package mlonspark

import java.{util => ju}

import com.github.fommil.netlib.BLAS.{getInstance => blas}
import mlonspark.util.SortDataFormat
import mlonspark.util.random.XORShiftRandom
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.util.hashing.byteswap64
import scala.collection.mutable

object ALSEngine {

  def train(
    //tag::params-def[]
    rdd: RDD[(Int, Int, Float)],
    //end::params-def[]
    rank: Int = 10,
    numUserBlocks: Int = 10,
    numItemBlocks: Int = 10,
    maxIter: Int = 10,
    regParam: Double = 0.1,
    alpha: Double = 1.0,
    intermediateRDDStorageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK,
    finalRDDStorageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK)
  : (RDD[(Int, Array[Float])], RDD[(Int, Array[Float])]) = {

    val userPart = new HashPartitioner(numUserBlocks)
    val itemPart = new HashPartitioner(numItemBlocks)

    val (userBlocks, userMetaBlocks) = blockify(rdd, userPart, itemPart)

    val swappedRdd = rdd.map{case(userId, itemId, rating)=>
      (itemId, userId, rating)
    }

    val (itemBlocks, itemMetaBlocks) = blockify(swappedRdd, itemPart, userPart)

    val seedGen = new XORShiftRandom(0L)
    var userFactors = initialize(userBlocks, rank, seedGen.nextLong())
    var itemFactors = initialize(itemBlocks, rank, seedGen.nextLong())

    for (iter <- 1 to maxIter) {
      val previousItemFactors = itemFactors
      itemFactors = computeFactors(userFactors, userMetaBlocks, itemBlocks, rank, regParam, alpha)
      previousItemFactors.unpersist()
      itemFactors.setName(s"itemFactors-$iter").persist(intermediateRDDStorageLevel)

      val previousUserFactors = userFactors
      userFactors = computeFactors(itemFactors, itemMetaBlocks, userBlocks, rank, regParam, alpha)
      previousUserFactors.unpersist()
      userFactors.setName(s"userFactors-$iter").persist(intermediateRDDStorageLevel)
    }

    val userIdAndFactors = userBlocks
      .mapValues(_.srcIds)
      .join(userFactors)
      .mapPartitions({ items =>
        items.flatMap { case (_, (ids, factors)) =>
          ids.view.zip(factors.factors)
        }
      }, preservesPartitioning = true)
      .setName("userFactors")
      .persist(finalRDDStorageLevel)

    val itemIdAndFactors = itemBlocks
      .mapValues(_.srcIds)
      .join(itemFactors)
      .mapPartitions({ items =>
        items.flatMap { case (_, (ids, factors)) =>
          ids.view.zip(factors.factors)
        }
      }, preservesPartitioning = true)
      .setName("itemFactors")
      .persist(finalRDDStorageLevel)

    (userIdAndFactors, itemIdAndFactors)
  }

  def computeFactors(
    srcFactorBlocks: RDD[(Int, FactorBlock)],
    srcMetaBlocks: RDD[(Int, MetaBlock)],
    dstBlocks: RDD[(Int, Block)],
    rank: Int,
    regParam: Double,
    alpha: Double = 1.0): RDD[(Int, FactorBlock)] =
  {
    val numSrcBlocks = srcFactorBlocks.partitions.length
    val YtY = Some(computeYtY(srcFactorBlocks, rank))
    val srcOut = srcMetaBlocks.join(srcFactorBlocks).flatMap {
      case (srcBlockId, (srcMetaBlock, srcFactors)) =>
        srcMetaBlock.dstBlocks.view.zipWithIndex.map { case (activeIndices, dstBlockId) =>
          (dstBlockId, (srcBlockId, activeIndices.map(idx => srcFactors.factors(idx))))
        }
    }
    val merged = srcOut.groupByKey(dstBlocks.partitions.length)
    dstBlocks.join(merged).mapValues {
      case (Block(dstIds, srcPtrs, _, srcBlockIds, srcIds, srcLocalIndices, ratings), srcFactors) =>
        val sortedSrcFactors = new Array[Array[Array[Float]]](numSrcBlocks)
        srcFactors.foreach { case (srcBlockId, factors) =>
          sortedSrcFactors(srcBlockId) = factors
        }
        val dstFactors = new Array[Array[Float]](dstIds.length)
        val ls = new NormalEquation(rank)
        for (j <- 0 to dstIds.length-1) {
          ls.reset()
          ls.merge(YtY.get)
          val dstId = dstIds(j)
          var numExplicits = 0
          for (i <- srcPtrs(j) to srcPtrs(j + 1)-1) {
            val srcBlockId = srcBlockIds(i)
            val rating = ratings(i)

            val srcLocalIndex = srcLocalIndices(i)
            val srcFactor = sortedSrcFactors(srcBlockId)(srcLocalIndex)
            if (rating > 0.0) {
              numExplicits += 1
            }

            val c1 = 1 + (alpha * rating)
            ls.add(srcFactor, if (rating > 0.0) c1 else 0.0, c1-1)
          }
          dstFactors(j) = solve(ls, numExplicits * regParam)
        }
        FactorBlock(dstFactors)
    }
  }

  def solve(ne: NormalEquation, lambda: Double): Array[Float] = {
    val k = ne.rank
    // Add scaled lambda to the diagonals of AtA.
    var i = 0
    var j = 2
    while (i < ne.size) {
      ne.ata(i) += lambda
      i += j
      j += 1
    }
    CholeskyDecomposition.solve(ne.ata, ne.atb)
    val x = new Array[Float](k)
    i = 0
    while (i < k) {
      x(i) = ne.atb(i).toFloat
      i += 1
    }
    ne.reset()
    x
  }

  private def computeYtY(factorBlocks: RDD[(Int, FactorBlock)], rank: Int): NormalEquation = {
    factorBlocks.values.aggregate(new NormalEquation(rank))(
      seqOp = (ne, factors) => {
        factors.factors.foreach(ne.add(_, 0.0))
        ne
      },
      combOp = (ne1, ne2) => ne1.merge(ne2))
  }

  def blockify(
    rdd: RDD[(Int, Int, Float)],
    srcPartitioner: Partitioner,
    dstPartitioner: Partitioner) : (RDD[(Int, Block)], RDD[(Int, MetaBlock)])  =
  {
    //tag::blockify-emit-key[]
    val blocks = rdd.map { case (srcId, dstId, rating) =>
      val srcBlockId = srcPartitioner.getPartition(srcId)
      val dstBlockId = dstPartitioner.getPartition(dstId)
      ((srcBlockId, dstBlockId), (srcId, dstId, rating))
    //end::blockify-emit-key[]
    //tag::blockify-coo[]
    }.groupByKey().mapValues{ratings =>
      val builder = new RatingBlockBuilder
      ratings.foreach{case(srcId, dstId, rating) =>
        builder.add(srcId, dstId, rating)}
      builder.build()
    //end::blockify-coo[]
    //tag::blockify-dstIdToLocalIndex[]
    }.map{case((srcBlockId, dstBlockId), RatingBlock(srcIds, dstIds, ratings))=>
      val dstIdToLocalIndex = dstIds.toSet.toSeq.sorted.zipWithIndex.toMap
      val dstLocalIndices = dstIds.map(dstIdToLocalIndex.apply)
      (srcBlockId, (dstBlockId, srcIds, dstIds, dstLocalIndices, ratings))
    //end::blockify-dstIdToLocalIndex[]
    }.groupByKey(srcPartitioner).mapValues{v=>
      val builder = new BlockBuilder()
      v.foreach{case(dstBlockId, srcIds, dstIds, dstLocalIndices, ratings)=>
        val length = srcIds.length
        require(dstLocalIndices.length == length)
        require(ratings.length == length)
        for (i <- 0 to length-1) {
          builder.add(srcIds(i), dstBlockId, dstIds(i), dstLocalIndices(i), ratings(i))}
        }
      builder.build().compress()
    }

    val metaBlocks = blocks.mapValues { case Block(srcIds, dstPtrs, _, dstBlockIds, _, _, _) =>
      val activeIds = Array.fill(dstPartitioner.numPartitions)(mutable.ArrayBuilder.make[Int])
      val seen = new Array[Boolean](dstPartitioner.numPartitions)
      for (i <- 0 to srcIds.length-1) {
        ju.Arrays.fill(seen, false)
        var j = dstPtrs(i)
        for (j <- dstPtrs(i) to dstPtrs(i+1)-1) {
          val dstBlockId = dstBlockIds(j)
          if (!seen(dstBlockId)) {
            seen(dstBlockId) = true
            activeIds(dstBlockId) += i
          }
        }
      }
      MetaBlock(activeIds.map(x=>x.result()))
    }
    (blocks, metaBlocks)
  }

  private def initialize(inBlocks: RDD[(Int, Block)], rank: Int, seed: Long): RDD[(Int, FactorBlock)] = {
    inBlocks.map { case (srcBlockId, inBlock) =>
      val random = new XORShiftRandom(byteswap64(seed ^ srcBlockId))
      val factors = Array.fill(inBlock.srcIds.length) {
        val factor = Array.fill(rank)(random.nextGaussian().toFloat)
        val nrm = blas.snrm2(rank, factor, 1)
        blas.sscal(rank, 1.0f / nrm, factor, 1)
        factor
      }
      (srcBlockId, FactorBlock(factors))
    }
  }
}

case class FactorBlock(factors: Array[Array[Float]]) {
  override def toString(): String = {
    var res = "[\n";
    for (i <- 0 to factors.length-1) {
      res += factors(i).mkString(", ")
      res += "\n"
    }
    res += "]\n";
    res
  }
}

class BlockBuilder {

  private val srcIds = mutable.ArrayBuilder.make[Int]
  private val dstBlockIds = mutable.ArrayBuilder.make[Int]
  private val dstIds = mutable.ArrayBuilder.make[Int]
  private val dstLocalIndices = mutable.ArrayBuilder.make[Int]
  private val ratings = mutable.ArrayBuilder.make[Float]

  def add(srcId: Int, dstBlockId: Int, dstId: Int, dstLocalIndice: Int, rating:Float): Unit = {
    this.srcIds += srcId
    this.dstBlockIds += dstBlockId
    this.dstIds += dstId
    this.dstLocalIndices += dstLocalIndice
    this.ratings += rating
  }

  def build() : RawBlock = {
    val block = new RawBlock(srcIds.result(), dstBlockIds.result(), dstIds.result(), dstLocalIndices.result(), ratings.result())
    block.sort()
    block
  }
}

case class RawBlock(srcIds: Array[Int], dstBlockIds: Array[Int], dstIds: Array[Int], dstLocalIndices: Array[Int], ratings: Array[Float]) {
  def length: Int = srcIds.length

  def sort(): Unit = {
    val sorter = new util.Sorter(new BlockSort)
    sorter.sort(this, 0, length, Ordering[Int])
  }

  def compress(): Block = {
    val uniqueSrcIds = mutable.ArrayBuilder.make[Int]
    val dstPtrs = mutable.ArrayBuilder.make[Int]
    uniqueSrcIds += srcIds(0)
    dstPtrs += 0
    var prevSrcId = srcIds(0)
    var total = 0
    var counter = 1
    for (i <- 1 to length-1) {
      val srcId = srcIds(i)
      if (srcId != prevSrcId) {
        uniqueSrcIds += srcId
        prevSrcId = srcId
        total += counter
        dstPtrs += total
        counter = 1
      } else {
        counter += 1
      }
    }
    total += counter
    dstPtrs += total
    Block(uniqueSrcIds.result(), dstPtrs.result(), srcIds, dstBlockIds, dstIds, dstLocalIndices, ratings)
  }
}

case class Block(
  srcIds: Array[Int],
  dstPtrs: Array[Int],
  srcIdsUncompressed: Array[Int],
  dstBlockIds: Array[Int],
  dstIds: Array[Int],
  dstLocalIndices: Array[Int],
  ratings: Array[Float])

case class MetaBlock(dstBlocks: Array[Array[Int]])

class HashPartitioner(partitions: Int) extends org.apache.spark.Partitioner {

  override def numPartitions: Int = partitions

  override def getPartition(key: Any): Int = {
    Math.abs(key.hashCode % partitions)
  }
}

private class BlockSort(implicit ord: Ordering[Int])  extends SortDataFormat[Int, RawBlock] {

  override def getKey(
     data: RawBlock,
     pos: Int): Int =
  {
    data.srcIds(pos)
  }

  override def swap(data: RawBlock, pos0: Int, pos1: Int): Unit = {
    swapElements(data.srcIds, pos0, pos1)
    swapElements(data.dstBlockIds, pos0, pos1)
    swapElements(data.dstIds, pos0, pos1)
    swapElements(data.dstLocalIndices, pos0, pos1)
    swapElements(data.ratings, pos0, pos1)
  }

  override def copyRange(
    src: RawBlock,
    srcPos: Int,
    dst: RawBlock,
    dstPos: Int,
    length: Int): Unit =
  {
    System.arraycopy(src.srcIds, srcPos, dst.srcIds, dstPos, length)
    System.arraycopy(src.dstBlockIds, srcPos, dst.dstBlockIds, dstPos, length)
    System.arraycopy(src.dstIds, srcPos, dst.dstIds, dstPos, length)
    System.arraycopy(src.dstLocalIndices, srcPos, dst.dstLocalIndices, dstPos, length)
    System.arraycopy(src.ratings, srcPos, dst.ratings, dstPos, length)
  }

  private def swapElements[@specialized(Int, Float) T](
    data: Array[T],
    pos0: Int,
    pos1: Int): Unit =
  {
    val tmp = data(pos0)
    data(pos0) = data(pos1)
    data(pos1) = tmp
  }

  override def allocate(length: Int): RawBlock = {
    new RawBlock(
      new Array[Int](length),
      new Array[Int](length),
      new Array[Int](length),
      new Array[Int](length),
      new Array[Float](length))
  }

  override def copyElement(
    src: RawBlock,
    srcPos: Int,
    dst: RawBlock,
    dstPos: Int): Unit =
  {
    dst.srcIds(dstPos) = src.srcIds(srcPos)
    dst.dstBlockIds(dstPos) = src.dstBlockIds(srcPos)
    dst.dstIds(dstPos) = src.dstIds(srcPos)
    dst.dstLocalIndices(dstPos) = src.dstLocalIndices(srcPos)
    dst.ratings(dstPos) = src.ratings(srcPos)
  }
}

case class RatingBlock(srcIds: Array[Int], dstIds: Array[Int], ratings: Array[Float])

class RatingBlockBuilder extends Serializable {

  private val srcIds = mutable.ArrayBuilder.make[Int]
  private val dstIds = mutable.ArrayBuilder.make[Int]
  private val ratings = mutable.ArrayBuilder.make[Float]

  var size = 0

  def add(srcId : Int, dstId : Int, rating: Float): this.type = {
    size += 1
    srcIds += srcId
    dstIds += dstId
    ratings += rating
    this
  }

  def merge(other: RatingBlock): this.type = {
    size += other.srcIds.length
    srcIds ++= other.srcIds
    dstIds ++= other.dstIds
    ratings ++= other.ratings
    this
  }

  def build(): RatingBlock = {
    RatingBlock(srcIds.result(), dstIds.result(), ratings.result())
  }
}

private[mlonspark] class NormalEquation(val rank: Int) extends Serializable {

  //tag::normal-eq-fields[]
  val size = rank * (rank + 1) / 2
  val ata = new Array[Double](size)
  val atb = new Array[Double](rank)
  //end::normal-eq-fields[]

  private val da = new Array[Double](rank)
  private val upper = "U"

  private def copyToDouble(a: Array[Float]): Unit = {
    var i = 0
    while (i < rank) {
      da(i) = a(i)
      i += 1
    }
  }

  def add(a: Array[Float], b: Double, c: Double = 1.0): this.type = {
    require(c >= 0.0)
    require(a.length == rank)
    copyToDouble(a)
    blas.dspr(upper, rank, c, da, 1, ata)
    if (b != 0.0) {
      blas.daxpy(rank, b, da, 1, atb, 1)
    }
    this
  }

  def merge(other: NormalEquation): this.type = {
    require(other.rank == rank)
    blas.daxpy(ata.length, 1.0, other.ata, 1, ata, 1)
    blas.daxpy(atb.length, 1.0, other.atb, 1, atb, 1)
    this
  }

  def reset(): Unit = {
    ju.Arrays.fill(ata, 0.0)
    ju.Arrays.fill(atb, 0.0)
  }
}