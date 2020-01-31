package mlonspark

import java.{util => ju}
import com.github.fommil.netlib.BLAS.{getInstance => blas}
import mlonspark.util.SortDataFormat
import mlonspark.util.random.XORShiftRandom
import org.apache.spark.rdd.RDD
import scala.util.hashing.byteswap64
import scala.collection.mutable

object Terminator {

  def process(rdd: RDD[(Int, Int, Float)]): Unit = {
    val rank = 3

    val (userBlocks, userMetaBlocks) = blockify(rdd)
    println(userBlocks.collect().mkString(""))
    println(userMetaBlocks.collect().mkString(""))

    val swappedRdd = rdd.map{case(userId, itemId, rating)=>
      (itemId, userId, rating)
    }

    val (itemBlocks, itemMetaBlocks) = blockify(swappedRdd)
    println(itemBlocks.collect().mkString(""))
    println(itemMetaBlocks.collect().mkString(""))

    val seedGen = new XORShiftRandom(0L)
    var userFactors = initialize(userBlocks, rank, seedGen.nextLong())
    val itemFactors = initialize(itemBlocks, rank, seedGen.nextLong())

    userFactors.collect().foreach(v=>println(v._1 + " " + v._2))
    itemFactors.collect().foreach(v=>println(v._1 + " " + v._2))

    computeFactors(userFactors, userMetaBlocks, itemBlocks, rank)

//    , regParam,
//      userLocalIndexEncoder, alpha, solver)
  }

  def computeFactors(
    srcFactorBlocks: RDD[(Int, FactorBlock)],
    srcMetaBlocks: RDD[(Int, MetaBlock)],
    dstBlocks: RDD[(Int, Block)],
    rank: Int): Unit =
  {
    val numSrcBlocks = srcFactorBlocks.partitions.length
    val srcOut = srcMetaBlocks.join(srcFactorBlocks).flatMap {
      case (srcBlockId, (srcMetaBlock, srcFactors)) =>
        srcMetaBlock.dstBlocks.view.zipWithIndex.map { case (activeIndices, dstBlockId) =>
          (dstBlockId, (srcBlockId, activeIndices.map(idx => srcFactors.factors(idx))))
        }
    }
    val merged = srcOut.groupByKey(dstBlocks.partitions.length)
    dstBlocks.join(merged).map {
      case (dstBlockId, (Block(dstIds, srcPtrs, _, srcBlockIds, srcIds, ratings), srcFactors)) =>
        println("============================================")
        val sortedSrcFactors = new Array[Array[Array[Float]]](numSrcBlocks)
        srcFactors.foreach { case (srcBlockId, factors) =>
          sortedSrcFactors(srcBlockId) = factors
        }
        for (j <- 0 to dstIds.length-1) {
          println("---------------------------------------------")
          val dstId = dstIds(j)
          for (i <- srcPtrs(j) to srcPtrs(j + 1)-1) {
            val srcBlockId = srcBlockIds(i)
            val rating = ratings(i)
            val srcId = srcIds(i)
            println("dstBlockId: " + dstBlockId + ", dstId: " + dstId + ", srcBlockId: " + srcBlockId + ", srcId: " + srcId + ", rating: " + rating)
          }
        }
    }.collect()
  }

  def blockify(rdd: RDD[(Int, Int, Float)] ) : (RDD[(Int, Block)], RDD[(Int, MetaBlock)])  = {
    val partitioner = new HashPartitioner(2)
    val blocks = rdd.map{case (srcId, dstId, rating) =>
      (partitioner.getPartition(srcId), (srcId, partitioner.getPartition(dstId), dstId, rating))
    }.groupByKey(partitioner).mapValues{v=>
      val builder = new BlockBuilder()
      v.foreach(i=>builder.add(i._1, i._2, i._3, i._4))
      builder.build().compress()
    }

    val metaBlocks = blocks.mapValues { case Block(srcIds, dstPtrs, _, dstBlockIds, _, _) =>
      val activeIds = Array.fill(partitioner.numPartitions)(mutable.ArrayBuilder.make[Int])
      val seen = new Array[Boolean](partitioner.numPartitions)
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
  private val ratings = mutable.ArrayBuilder.make[Float]

  def add(srcId: Int, dstBlockId: Int, dstId: Int, rating:Float): Unit = {
    this.srcIds += srcId
    this.dstBlockIds += dstBlockId
    this.dstIds += dstId
    this.ratings += rating
  }

  def build() : RawBlock = {
    val block = new RawBlock(srcIds.result(), dstBlockIds.result(), dstIds.result(), ratings.result())
    block.sort()
    block
  }
}

case class RawBlock(srcIds: Array[Int], dstBlockIds: Array[Int], dstIds: Array[Int], ratings: Array[Float]) {
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
    Block(uniqueSrcIds.result(), dstPtrs.result(), srcIds, dstBlockIds, dstIds, ratings)
  }

  override def toString(): String = {
    var res = "[\n";
    for (i <- 0 to length-1) {
      res += "% 7d".format(srcIds(i))
      res += "% 7d".format(dstBlockIds(i))
      res += "% 7d".format(dstIds(i))
      res += "    % 7f".format(ratings(i))
      res += "\n"
    }
    res += "]";
    res
  }
}

case class Block(srcIds: Array[Int], dstPtrs: Array[Int], srcIdsUncompressed: Array[Int], dstBlockIds: Array[Int], dstIds: Array[Int], ratings: Array[Float]) {

  override def toString(): String = {
    var res = "\n[\n";
    res += "   " + srcIds.mkString(", ")
    res += "\n"
    res += dstPtrs.mkString(", ")
    res += "\n"
    res += "---------------------------------------------"
    res += "\n"
    for (i <- 0 to srcIdsUncompressed.length-1) {
      res += "% 7d".format(srcIdsUncompressed(i))
      res += "% 7d".format(dstBlockIds(i))
      res += "% 7d".format(dstIds(i))
      res += "    % 7f".format(ratings(i))
      res += "\n"
    }
    res += "]\n";
    res
  }
}

case class MetaBlock(dstBlocks: Array[Array[Int]]) {
  override def toString(): String = {
    var res = "\n[\n";
    for (i <- 0 to dstBlocks.length-1) {
      res += i
      res += " ["
      res += dstBlocks(i).mkString(", ")
      res += "]\n"
    }
    res += "]\n";
    res
  }
}

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
    dst.ratings(dstPos) = src.ratings(srcPos)
  }
}
