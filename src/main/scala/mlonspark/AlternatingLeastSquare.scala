package mlonspark

import java.{util => ju}

import com.github.fommil.netlib.BLAS.{getInstance => blas}
import mlonspark.util.{OpenHashMap, OpenHashSet, SortDataFormat}
import mlonspark.util.random.XORShiftRandom
import org.apache.hadoop.fs.Path
import org.apache.spark.Partitioner
import org.apache.spark.annotation.Since
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.param._
import org.apache.spark.ml.param.shared._
import org.apache.spark.ml.util.{DefaultParamsWritable, DefaultParamsWriter, Identifiable, MLReadable, MLReader, MLWritable, MLWriter}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType
import org.apache.spark.storage.StorageLevel
import org.json4s.DefaultFormats
import org.json4s.JsonDSL._

import scala.collection.mutable
import scala.util.{Sorting, Try}
import scala.util.hashing.byteswap64

/**
 * AlternatingLeastSquareModelParams
 */
//tag::params-def[]
trait AlternatingLeastSquareModelParams
  extends Params
    with HasPredictionCol
    //end::params-def[]
{
  /**
   * Param for the column name for user ids. Ids must be integers. Other
   * numeric types are supported for this column, but will be cast to integers as long as they
   * fall within the integer value range.
   * Default: "user"
   * @group param
   */
  val userCol = new Param[String](this, "userCol", "column name for user ids. Ids must be within " +
    "the integer value range.")
  def getUserCol: String = $(userCol)

  /**
   * Param for the column name for item ids. Ids must be integers. Other
   * numeric types are supported for this column, but will be cast to integers as long as they
   * fall within the integer value range.
   * Default: "item"
   * @group param
   */
  val itemCol = new Param[String](this, "itemCol", "column name for item ids. Ids must be within " +
    "the integer value range.")
  def getItemCol: String = $(itemCol)
}

trait AlternatingLeastSquareParams
  extends AlternatingLeastSquareModelParams
    with HasPredictionCol
    with HasMaxIter
    with HasRegParam
    with HasSeed
    //end::params-def[]
{
  /**
   * Param for rank of the matrix factorization (positive).
   * Default: 10
   * @group param
   */
  //tag::params-rank[]
    val rank = new IntParam(this, "rank", "rank of the factorization", ParamValidators.gtEq(1))
    def getRank: Int = $(rank)
  //end::params-rank[]

  /**
   * Param for the column name for ratings.
   * Default: "rating"
   * @group param
   */
  val ratingCol = new Param[String](this, "ratingCol", "column name for ratings")
  def getRatingCol: String = $(ratingCol)

  /**
   * Param for the alpha parameter in the implicit preference formulation (nonnegative).
   * Default: 1.0
   * @group param
   */
  val alpha = new DoubleParam(this, "alpha", "alpha for implicit preference",
    ParamValidators.gtEq(0))
  def getAlpha: Double = $(alpha)

  /**
   * Param for StorageLevel for intermediate datasets. Pass in a string representation of
   * `StorageLevel`. Cannot be "NONE".
   * Default: "MEMORY_AND_DISK".
   *
   * @group expertParam
   */
  val intermediateStorageLevel = new Param[String](this, "intermediateStorageLevel",
    "StorageLevel for intermediate datasets. Cannot be 'NONE'.",
    (s: String) => Try(StorageLevel.fromString(s)).isSuccess && s != "NONE")
  def getIntermediateStorageLevel: String = $(intermediateStorageLevel)

  /**
   * Param for StorageLevel for ALS model factors. Pass in a string representation of
   * `StorageLevel`.
   * Default: "MEMORY_AND_DISK".
   *
   * @group expertParam
   */
  val finalStorageLevel = new Param[String](this, "finalStorageLevel",
    "StorageLevel for ALS model factors.",
    (s: String) => Try(StorageLevel.fromString(s)).isSuccess)
  def getFinalStorageLevel: String = $(finalStorageLevel)

  /**
   * Param for number of user blocks (positive).
   * Default: 10
   * @group param
   */
  val numUserBlocks = new IntParam(this, "numUserBlocks", "number of user blocks",
    ParamValidators.gtEq(1))
  def getNumUserBlocks: Int = $(numUserBlocks)

  /**
   * Param for number of item blocks (positive).
   * Default: 10
   * @group param
   */
  val numItemBlocks = new IntParam(this, "numItemBlocks", "number of item blocks",
    ParamValidators.gtEq(1))
  def getNumItemBlocks: Int = $(numItemBlocks)

  setDefault(
    rank -> 10,
    maxIter -> 10,
    regParam -> 0.1,
    numUserBlocks -> 10,
    numItemBlocks -> 10,
    alpha -> 1.0,
    userCol -> "user",
    itemCol -> "item",
    ratingCol -> "rating",
    intermediateStorageLevel -> "MEMORY_AND_DISK",
    finalStorageLevel -> "MEMORY_AND_DISK")
}
/**
 * AlternatingLeastSquareModel
 *
 * @param uid
 */
//tag::model-def[]
class AlternatingLeastSquareModel(
    override val uid: String,
    val rank: Int,
    @transient val userFactors: DataFrame,
    @transient val itemFactors: DataFrame)
  extends Model[AlternatingLeastSquareModel]
    with AlternatingLeastSquareModelParams
    with MLWritable
    //end::model-def[]
{
  import AlternatingLeastSquareModel._

  override def copy(extra: ParamMap): AlternatingLeastSquareModel = {
    val copied = new AlternatingLeastSquareModel(uid, rank, userFactors, itemFactors)
    copyValues(copied, extra).setParent(parent)
  }

  override def write: MLWriter = new AlternatingLeastSquareModelWriter(this)

  private val predict = udf { (featuresA: Seq[Float], featuresB: Seq[Float]) =>
    if (featuresA != null && featuresB != null) {
      var dotProduct = 0.0f
      var i = 0
      while (i < rank) {
        dotProduct += featuresA(i) * featuresB(i)
        i += 1
      }
      dotProduct
    } else {
      Float.NaN
    }
  }

  override def transform(dataset: Dataset[_]): DataFrame = {
    val predictions = dataset
      .join(userFactors, dataset($(userCol)) === userFactors("id"), "left")
      .join(itemFactors, dataset($(itemCol)) === itemFactors("id"), "left")
      .select(dataset("*"), predict(userFactors("features"), itemFactors("features")).as($(predictionCol)))
    predictions.na.drop("all", Seq($(predictionCol)))
  }

  override def transformSchema(schema: StructType): StructType = {
    schema
  }

  def recommendForAllUsers(numItems: Int): DataFrame = {
    recommend(userFactors, itemFactors, numItems)
  }

  private def recommend(
     srcFactors: DataFrame,
     dstFactors: DataFrame,
     num: Int): DataFrame =
  {
    import srcFactors.sparkSession.implicits._

    srcFactors
  }
}

/**
 * AlternatingLeastSquareModel
 */
object AlternatingLeastSquareModel
  extends MLReadable[AlternatingLeastSquareModel]
{

  class AlternatingLeastSquareModelWriter(instance: AlternatingLeastSquareModel) extends MLWriter {
    override protected def saveImpl(path: String): Unit = {
      val extraMetadata = "rank" -> instance.rank
      DefaultParamsWriter.saveMetadata(instance, path, sc, Some(extraMetadata))
      val userPath = new Path(path, "userFactors").toString
      instance.userFactors.write.format("parquet").save(userPath)
      val itemPath = new Path(path, "itemFactors").toString
      instance.itemFactors.write.format("parquet").save(itemPath)
    }
  }

  private class AlternatingLeastSquareModelReader extends MLReader[AlternatingLeastSquareModel] {

    private val className = classOf[AlternatingLeastSquareModel].getName

    override def load(path: String): AlternatingLeastSquareModel = {
      val metadata = DefaultParamsReader.loadMetadata(path, sc, className)
      implicit val format = DefaultFormats
      val rank = (metadata.metadata \ "rank").extract[Int]
      val userPath = new Path(path, "userFactors").toString
      val userFactors = sparkSession.read.format("parquet").load(userPath)
      val itemPath = new Path(path, "itemFactors").toString
      val itemFactors = sparkSession.read.format("parquet").load(itemPath)
      val model = new AlternatingLeastSquareModel(metadata.uid, rank, userFactors, itemFactors)
      DefaultParamsReader.getAndSetParams(model, metadata)
      model
    }
  }

  override def read: MLReader[AlternatingLeastSquareModel] = new AlternatingLeastSquareModelReader
}

/**
 * AlternatingLeastSquare
 *
 * @param uid
 */
class AlternatingLeastSquare(override val uid: String)
  extends Estimator[AlternatingLeastSquareModel]
    with AlternatingLeastSquareParams
    with DefaultParamsWritable
{
  import mlonspark.AlternatingLeastSquare.Rating

  def setUserCol(value: String):                    this.type = set(userCol, value)
  def setItemCol(value: String):                    this.type = set(itemCol, value)
  def setRatingCol(value: String):                  this.type = set(ratingCol, value)
  def setRank(value: Int):                          this.type = set(rank, value)
  def setNumUserBlocks(value: Int):                 this.type = set(numUserBlocks, value)
  def setNumItemBlocks(value: Int):                 this.type = set(numItemBlocks, value)
  def setAlpha(value: Double):                      this.type = set(alpha, value)
  def setPredictionCol(value: String):              this.type = set(predictionCol, value)
  def setMaxIter(value: Int):                       this.type = set(maxIter, value)
  def setRegParam(value: Double):                   this.type = set(regParam, value)
  def setSeed(value: Long):                         this.type = set(seed, value)
  def setIntermediateStorageLevel(value: String):   this.type = set(intermediateStorageLevel, value)
  def setFinalStorageLevel(value: String):          this.type = set(finalStorageLevel, value)

  override def fit(dataset: Dataset[_]): AlternatingLeastSquareModel = {
    import dataset.sparkSession.implicits._

    val ratings = dataset
      .select(col($(userCol)), col($(itemCol)), col($(ratingCol))).rdd
      .map { row =>
        Rating(row.getInt(0), row.getInt(1), row.getFloat(2))
      }

    val (userFactors, itemFactors) = AlternatingLeastSquare.train(
      ratings,
      rank = $(rank),
      numUserBlocks = $(numUserBlocks),
      numItemBlocks = $(numItemBlocks),
      maxIter = $(maxIter),
      regParam = $(regParam),
      alpha = $(alpha),
      intermediateRDDStorageLevel = StorageLevel.fromString($(intermediateStorageLevel)),
      finalRDDStorageLevel = StorageLevel.fromString($(finalStorageLevel)),
      seed = $(seed))

    val userDF = userFactors.toDF("id", "features")
    val itemDF = itemFactors.toDF("id", "features")

    new AlternatingLeastSquareModel(uid, $(rank), userDF, itemDF)
  }

  override def copy(extra: ParamMap): AlternatingLeastSquare = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    schema
  }
}

object AlternatingLeastSquare {

  /**
   * Representing a normal equation to solve the following weighted least squares problem:
   *
   * minimize \sum,,i,, c,,i,, (a,,i,,^T^ x - d,,i,,)^2^ + lambda * x^T^ x.
   *
   * Its normal equation is given by
   *
   * \sum,,i,, c,,i,, (a,,i,, a,,i,,^T^ x - d,,i,, a,,i,,) + lambda * x = 0.
   *
   * Distributing and letting b,,i,, = c,,i,, * d,,i,,
   *
   * \sum,,i,, c,,i,, a,,i,, a,,i,,^T^ x - b,,i,, a,,i,, + lambda * x = 0.
   */
  //tag::normal-eq-constructor[]
  private[mlonspark] class NormalEquation(val k: Int) extends Serializable {
  //end::normal-eq-constructor[]

    /** Number of entries in the upper triangular part of a k-by-k matrix. */
    /** ata: A^T^ * A */
    /** atb: A^T^ * A */

    //tag::normal-eq-fields[]
    val triK = k * (k + 1) / 2
    val ata = new Array[Double](triK)
    val atb = new Array[Double](k)
    //end::normal-eq-fields[]

    private val da = new Array[Double](k)
    private val upper = "U"

    private def copyToDouble(a: Array[Float]): Unit = {
      var i = 0
      while (i < k) {
        da(i) = a(i)
        i += 1
      }
    }

    /** Adds an observation. */
    def add(a: Array[Float], b: Double, c: Double = 1.0): this.type = {
      require(c >= 0.0)
      require(a.length == k)
      copyToDouble(a)
      blas.dspr(upper, k, c, da, 1, ata)
      if (b != 0.0) {
        blas.daxpy(k, b, da, 1, atb, 1)
      }
      this
    }

    /** Merges another normal equation object. */
    def merge(other: NormalEquation): this.type = {
      require(other.k == k)
      blas.daxpy(ata.length, 1.0, other.ata, 1, ata, 1)
      blas.daxpy(atb.length, 1.0, other.atb, 1, atb, 1)
      this
    }

    /** Resets everything to zero, which should be called after each solve. */
    def reset(): Unit = {
      ju.Arrays.fill(ata, 0.0)
      ju.Arrays.fill(atb, 0.0)
    }
  }

  /** Trait for least squares solvers applied to the normal equation. */
  private[mlonspark] trait LeastSquaresNESolver extends Serializable {
    /** Solves a least squares problem with regularization (possibly with other constraints). */
    def solve(ne: NormalEquation, lambda: Double): Array[Float]
  }

  private[mlonspark] class CholeskySolver extends LeastSquaresNESolver {

    /**
     * Solves a least squares problem with L2 regularization:
     *
     *   min norm(A x - b)^2^ + lambda * norm(x)^2^
     *
     * @param ne a [[NormalEquation]] instance that contains AtA, Atb, and n (number of instances)
     * @param lambda regularization constant
     * @return the solution x
     */
    override def solve(ne: NormalEquation, lambda: Double): Array[Float] = {
      val k = ne.k
      // Add scaled lambda to the diagonals of AtA.
      var i = 0
      var j = 2
      while (i < ne.triK) {
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
  }

  def train(
       ratings: RDD[Rating],
       rank: Int = 10,
       numUserBlocks: Int = 10,
       numItemBlocks: Int = 10,
       maxIter: Int = 10,
       regParam: Double = 0.1,
       alpha: Double = 1.0,
       intermediateRDDStorageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK,
       finalRDDStorageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK,
       seed: Long = 0L) : (RDD[(Long, Array[Float])], RDD[(Long, Array[Float])]) =
  {
    val userPart = new ALSPartitioner(numUserBlocks)
    val itemPart = new ALSPartitioner(numItemBlocks)
    val blockRatings = partitionRatings(ratings, userPart, itemPart)
    blockRatings.persist(intermediateRDDStorageLevel)
    System.out.println("blockRatings: " + blockRatings.count())

    val (userInBlocks, userOutBlocks) =
      makeBlocks("user", blockRatings, userPart, itemPart, intermediateRDDStorageLevel)
    userOutBlocks.count()

    val swappedBlockRatings = blockRatings.map {
      case ((userBlockId, itemBlockId), RatingBlock(userIds, itemIds, localRatings)) =>
        ((itemBlockId, userBlockId), RatingBlock(itemIds, userIds, localRatings))
    }
    val (itemInBlocks, itemOutBlocks) =
      makeBlocks("item", swappedBlockRatings, itemPart, userPart, intermediateRDDStorageLevel)
    itemOutBlocks.count()    // materialize item blocks

    // Encoders for storing each user/item's partition ID and index within its partition using a
    // single integer; used as an optimization
    val userLocalIndexEncoder = new LocalIndexEncoder(userPart.numPartitions)
    val itemLocalIndexEncoder = new LocalIndexEncoder(itemPart.numPartitions)

    // These are the user and item factor matrices that, once trained, are multiplied together to
    // estimate the rating matrix.  The two matrices are stored in RDDs, partitioned by column such
    // that each factor column resides on the same Spark worker as its corresponding user or item.
    val seedGen = new XORShiftRandom(seed)
    var userFactors = initialize(userInBlocks, rank, seedGen.nextLong())
    var itemFactors = initialize(itemInBlocks, rank, seedGen.nextLong())

    val solver = new CholeskySolver

    for (iter <- 1 to maxIter) {
      userFactors.setName(s"userFactors-$iter").persist(intermediateRDDStorageLevel)
      val previousItemFactors = itemFactors
      itemFactors = computeFactors(userFactors, userOutBlocks, itemInBlocks, rank, regParam,
        userLocalIndexEncoder, alpha, solver)
      previousItemFactors.unpersist()
      itemFactors.setName(s"itemFactors-$iter").persist(intermediateRDDStorageLevel)
      // TODO: Generalize PeriodicGraphCheckpointer and use it here.
      val deps = itemFactors.dependencies
      //      if (shouldCheckpoint(iter)) {
      ////        itemFactors.checkpoint() // itemFactors gets materialized in computeFactors
      ////      }
      val previousUserFactors = userFactors
      userFactors = computeFactors(itemFactors, itemOutBlocks, userInBlocks, rank, regParam,
        itemLocalIndexEncoder, alpha, solver)
      //      if (shouldCheckpoint(iter)) {
      //        ALS.cleanShuffleDependencies(sc, deps)
      //        deletePreviousCheckpointFile()
      //        previousCheckpointFile = itemFactors.getCheckpointFile
      //      }
      previousUserFactors.unpersist()
    }

    val userIdAndFactors = userInBlocks
      .mapValues(_.srcIds)
      .join(userFactors)
      .mapPartitions({ items =>
        items.flatMap { case (_, (ids, factors)) =>
          ids.view.zip(factors)
        }
        // Preserve the partitioning because IDs are consistent with the partitioners in userInBlocks
        // and userFactors.
      }, preservesPartitioning = true)
      .setName("userFactors")
      .persist(finalRDDStorageLevel)
    System.out.println("==============================================")
    System.out.println(userIdAndFactors.toDebugString)
    System.out.println("==============================================")
    val itemIdAndFactors = itemInBlocks
      .mapValues(_.srcIds)
      .join(itemFactors)
      .mapPartitions({ items =>
        items.flatMap { case (_, (ids, factors)) =>
          ids.view.zip(factors)
        }
      }, preservesPartitioning = true)
      .setName("itemFactors")
      .persist(finalRDDStorageLevel)
    System.out.println(itemIdAndFactors.toDebugString)
    (userIdAndFactors, itemIdAndFactors)
  }

  private def computeFactors[ID](
    srcFactorBlocks: RDD[(Int, FactorBlock)],
    srcOutBlocks: RDD[(Int, OutBlock)],
    dstInBlocks: RDD[(Int, InBlock)],
    rank: Int,
    regParam: Double,
    srcEncoder: LocalIndexEncoder,
    alpha: Double = 1.0,
    solver: LeastSquaresNESolver): RDD[(Int, FactorBlock)] =
  {
    val numSrcBlocks = srcFactorBlocks.partitions.length
    val YtY = Some(computeYtY(srcFactorBlocks, rank))
    val srcOut = srcOutBlocks.join(srcFactorBlocks).flatMap {
      case (srcBlockId, (srcOutBlock, srcFactors)) =>
        srcOutBlock.view.zipWithIndex.map { case (activeIndices, dstBlockId) =>
          (dstBlockId, (srcBlockId, activeIndices.map(idx => srcFactors(idx))))
        }
    }
    val merged = srcOut.groupByKey(new ALSPartitioner(dstInBlocks.partitions.length))
    dstInBlocks.join(merged).mapValues {
      case (InBlock(dstIds, srcPtrs, srcEncodedIndices, ratings), srcFactors) =>
        val sortedSrcFactors = new Array[FactorBlock](numSrcBlocks)
        srcFactors.foreach { case (srcBlockId, factors) =>
          sortedSrcFactors(srcBlockId) = factors
        }
        val dstFactors = new Array[Array[Float]](dstIds.length)
        var j = 0
        val ls = new NormalEquation(rank)
        while (j < dstIds.length) {
          ls.reset()
          ls.merge(YtY.get)
          var i = srcPtrs(j)
          while (i < srcPtrs(j + 1)) {
            val encoded = srcEncodedIndices(i)
            val blockId = srcEncoder.blockId(encoded)
            val localIndex = srcEncoder.localIndex(encoded)
            val srcFactor = sortedSrcFactors(blockId)(localIndex)
            val rating = ratings(i)
            // Extension to the original paper to handle rating < 0. confidence is a function
            // of |rating| instead so that it is never negative. c1 is confidence - 1.
            val c1 = alpha * math.abs(rating)
            // For rating <= 0, the corresponding preference is 0. So the second argument of add
            // is only there for rating > 0.
            ls.add(srcFactor, if (rating > 0.0) 1.0 + c1 else 0.0, c1)
            i += 1
          }
          // Weight lambda by the number of explicit ratings based on the ALS-WR paper.
          dstFactors(j) = solver.solve(ls, regParam)
          j += 1
        }
        dstFactors
    }
  }

  /**
   * Computes the Gramian matrix of user or item factors, which is only used in implicit preference.
   * Caching of the input factors is handled in [[ALS#train]].
   */
  private def computeYtY(factorBlocks: RDD[(Int, FactorBlock)], rank: Int): NormalEquation = {
    factorBlocks.values.aggregate(new NormalEquation(rank))(
      seqOp = (ne, factors) => {
        factors.foreach(ne.add(_, 0.0))
        ne
      },
      combOp = (ne1, ne2) => ne1.merge(ne2))
  }

  private def initialize(
                          inBlocks: RDD[(Int, InBlock)],
                          rank: Int,
                          seed: Long): RDD[(Int, FactorBlock)] =
  {
    // Choose a unit vector uniformly at random from the unit sphere, but from the
    // "first quadrant" where all elements are nonnegative. This can be done by choosing
    // elements distributed as Normal(0,1) and taking the absolute value, and then normalizing.
    // This appears to create factorizations that have a slightly better reconstruction
    // (<1%) compared picking elements uniformly at random in [0,1].
    inBlocks.map { case (srcBlockId, inBlock) =>
      val random = new XORShiftRandom(byteswap64(seed ^ srcBlockId))
      val factors = Array.fill(inBlock.srcIds.length) {
        val factor = Array.fill(rank)(random.nextGaussian().toFloat)
        val nrm = blas.snrm2(rank, factor, 1)
        blas.sscal(rank, 1.0f / nrm, factor, 1)
        factor
      }
      (srcBlockId, factors)
    }
  }

  private def partitionRatingsOrig(
    ratings: RDD[Rating],
    srcPart: Partitioner,
    dstPart: Partitioner): RDD[((Int, Int), RatingBlock)] =
  {
    val numPartitions = srcPart.numPartitions * dstPart.numPartitions
    val result = ratings.mapPartitions { iter =>
      val builders = Array.fill(numPartitions)(new RatingBlockBuilder)
      iter.flatMap { r =>
        val srcBlockId = srcPart.getPartition(r.user)
        val dstBlockId = dstPart.getPartition(r.item)
        val idx = srcBlockId + srcPart.numPartitions * dstBlockId
        val builder = builders(idx)
        builder.add(r)
        if (builder.size >= 2048) { // 2048 * (3 * 4) = 24k
          builders(idx) = new RatingBlockBuilder
          Iterator.single(((srcBlockId, dstBlockId), builder.build()))
        } else {
          Iterator.empty
        }
      } ++ {
        builders.view.zipWithIndex.filter(_._1.size > 0).map { case (block, idx) =>
          val srcBlockId = idx % srcPart.numPartitions
          val dstBlockId = idx / srcPart.numPartitions
          ((srcBlockId, dstBlockId), block.build())
        }
      }
    }.groupByKey().mapValues { blocks =>
      val builder = new RatingBlockBuilder
      blocks.foreach(builder.merge)
      builder.build()
    }.setName("ratingBlocks")
    System.out.println(result.toDebugString)
    result
  }

  //tag::partitionRatings[]
  private def partitionRatings(
      ratings: RDD[Rating],
      srcPart: Partitioner,
      dstPart: Partitioner): RDD[((Int, Int), RatingBlock)] =
  //end::partitionRatings[]
  {
    ratings.map{r =>
      val srcBlockId = srcPart.getPartition(r.user)
      val dstBlockId = dstPart.getPartition(r.item)
      ((srcBlockId, dstBlockId), r)
    }.groupByKey().mapValues { blockRatings =>
      val builder = new RatingBlockBuilder
      blockRatings.foreach(builder.add)
      builder.build()
    }.setName("ratingBlocks")
  }

  private def makeBlocks(
                          prefix: String,
                          ratingBlocks: RDD[((Int, Int), RatingBlock)],
                          srcPart: Partitioner,
                          dstPart: Partitioner,
                          storageLevel: StorageLevel)(
                          implicit srcOrd: Ordering[Long]): (RDD[(Int, InBlock)], RDD[(Int, OutBlock)]) =
  {
    val inBlocks = ratingBlocks.map {
      case ((srcBlockId, dstBlockId), RatingBlock(srcIds, dstIds, ratings)) =>
        val tid = Thread.currentThread().getId
        System.out.println(tid + " BLOCK " + srcBlockId + "|" + dstBlockId)
        // The implementation is a faster version of
        // val dstIdToLocalIndex = dstIds.toSet.toSeq.sorted.zipWithIndex.toMap
        val start = System.nanoTime()
        val dstIdSet = new OpenHashSet[Long](1 << 20)
        dstIds.foreach(dstIdSet.add)
        val sortedDstIds = new Array[Long](dstIdSet.size)
        var i = 0
        var pos = dstIdSet.nextPos(0)
        while (pos != -1) {
          sortedDstIds(i) = dstIdSet.getValue(pos)
          pos = dstIdSet.nextPos(pos + 1)
          i += 1
        }
        assert(i == dstIdSet.size)
        Sorting.quickSort(sortedDstIds)
        val dstIdToLocalIndex = new OpenHashMap[Long, Int](sortedDstIds.length)
        i = 0
        while (i < sortedDstIds.length) {
          dstIdToLocalIndex.update(sortedDstIds(i), i)
          i += 1
        }
        System.out.println(
          "Converting to local indices took " + (System.nanoTime() - start) / 1e9 + " seconds.")
        val dstLocalIndices = dstIds.map(dstIdToLocalIndex.apply)
        System.out.println(" " + tid + " dstIdSet " + dstIdSet.iterator.mkString(" "))
        System.out.println(" " + tid + " srcIds " + srcIds.deep.mkString(" "))
        System.out.println(" " + tid + " dstLocalIndices " + dstLocalIndices.deep.mkString(" "))
        (srcBlockId, (dstBlockId, srcIds, dstLocalIndices, ratings))
    }.groupByKey(new ALSPartitioner(srcPart.numPartitions))
      .mapValues { iter =>
        val tid = Thread.currentThread().getId
        val builder =
          new UncompressedInBlockBuilder(new LocalIndexEncoder(dstPart.numPartitions))
        iter.foreach { case (dstBlockId, srcIds, dstLocalIndices, ratings) =>
          builder.add(dstBlockId, srcIds, dstLocalIndices, ratings)
        }
        val uncInBlick = builder.build()
        System.out.println(" " + tid + " srcIds " + uncInBlick.srcIds.deep.mkString(" "))
        System.out.println(" " + tid + " dstBlockIds " + uncInBlick.dstBlockIds.deep.mkString(" "))
        System.out.println(" " + tid + " dstLocalIndices " + uncInBlick.dstLocalIndices.deep.mkString(" "))
        System.out.println(" " + tid + " dstEncodedIndices " + uncInBlick.dstEncodedIndices.deep.mkString(" "))
        uncInBlick.compress()
      }.setName(prefix + "InBlocks")
      .persist(storageLevel)
    val outBlocks = inBlocks.mapValues { case InBlock(srcIds, dstPtrs, dstEncodedIndices, _) =>
      val tid = Thread.currentThread().getId
      System.out.println(" " + tid + " srcIds " + srcIds.deep.mkString(" "))
      System.out.println(" " + tid + " dstPtrs " + dstPtrs.deep.mkString(" "))
      System.out.println(" " + tid + " dstEncodedIndices " + dstEncodedIndices.deep.mkString(" "))
      val encoder = new LocalIndexEncoder(dstPart.numPartitions)
      val activeIds = Array.fill(dstPart.numPartitions)(mutable.ArrayBuilder.make[Int])
      var i = 0
      val seen = new Array[Boolean](dstPart.numPartitions)
      while (i < srcIds.length) {
        var j = dstPtrs(i)
        ju.Arrays.fill(seen, false)
        while (j < dstPtrs(i + 1)) {
          val dstBlockId = encoder.blockId(dstEncodedIndices(j))
          if (!seen(dstBlockId)) {
            activeIds(dstBlockId) += i // add the local index in this out-block
            seen(dstBlockId) = true
          }
          j += 1
        }
        i += 1
      }
      val result = activeIds.map { x =>
        x.result()
      }
      System.out.println(" " + tid + " result " + result.deep.mkString(" "))
      result
    }.setName(prefix + "OutBlocks")
      .persist(storageLevel)
    (inBlocks, outBlocks)
  }

  private type ALSPartitioner = org.apache.spark.HashPartitioner

  private type OutBlock = Array[Array[Int]]

  private type FactorBlock = Array[Array[Float]]

  //tag::Rating[]
  case class Rating(user: Long, item: Long, rating: Float)
  //end::Rating[]

  //tag::RatingBlock[]
  case class RatingBlock(srcIds: Array[Long], dstIds: Array[Long], ratings: Array[Float])
  //end::RatingBlock[]
  {
    /** Size of the block. */
    def size: Int = srcIds.length

    require(dstIds.length == srcIds.length)
    require(ratings.length == srcIds.length)
  }

  case class InBlock (
                       srcIds: Array[Long],
                       dstPtrs: Array[Int],
                       dstEncodedIndices: Array[Int],
                       ratings: Array[Float])
  {
    /** Size of the block. */
    def size: Int = ratings.length
    require(dstEncodedIndices.length == size)
    require(dstPtrs.length == srcIds.length + 1)
  }

  class RatingBlockBuilder extends Serializable {

    private val srcIds = mutable.ArrayBuilder.make[Long]
    private val dstIds = mutable.ArrayBuilder.make[Long]
    private val ratings = mutable.ArrayBuilder.make[Float]

    var size = 0

    /** Adds a rating. */
    def add(r: Rating): this.type = {
      size += 1
      srcIds += r.user
      dstIds += r.item
      ratings += r.rating
      this
    }

    /** Merges another [[RatingBlockBuilder]]. */
    def merge(other: RatingBlock): this.type = {
      size += other.srcIds.length
      srcIds ++= other.srcIds
      dstIds ++= other.dstIds
      ratings ++= other.ratings
      this
    }

    /** Builds a [[RatingBlock]]. */
    def build(): RatingBlock = {
      RatingBlock(srcIds.result(), dstIds.result(), ratings.result())
    }
  }

  class LocalIndexEncoder(numBlocks: Int) extends Serializable {

    require(numBlocks > 0, s"numBlocks must be positive but found $numBlocks.")

    private[this] final val numLocalIndexBits =
      math.min(java.lang.Integer.numberOfLeadingZeros(numBlocks - 1), 31)
    private[this] final val localIndexMask = (1 << numLocalIndexBits) - 1

    /** Encodes a (blockId, localIndex) into a single integer. */
    def encode(blockId: Int, localIndex: Int): Int = {
      require(blockId < numBlocks)
      require((localIndex & ~localIndexMask) == 0)
      (blockId << numLocalIndexBits) | localIndex
    }

    /** Gets the block id from an encoded index. */
    @inline
    def blockId(encoded: Int): Int = {
      encoded >>> numLocalIndexBits
    }

    /** Gets the local index from an encoded index. */
    @inline
    def localIndex(encoded: Int): Int = {
      encoded & localIndexMask
    }
  }

  class UncompressedInBlockBuilder(
                                    encoder: LocalIndexEncoder)(
                                    implicit ord: Ordering[Long])
  {
    private val srcIds = mutable.ArrayBuilder.make[Long]
    private val dstLocalIndices = mutable.ArrayBuilder.make[Int]
    private val dstEncodedIndices = mutable.ArrayBuilder.make[Int]
    private val dstBlockIds = mutable.ArrayBuilder.make[Int]
    private val ratings = mutable.ArrayBuilder.make[Float]

    /**
     * Adds a dst block of (srcId, dstLocalIndex, rating) tuples.
     *
     * @param dstBlockId dst block ID
     * @param srcIds original src IDs
     * @param dstLocalIndices dst local indices
     * @param ratings ratings
     */
    def add(
             dstBlockId: Int,
             srcIds: Array[Long],
             dstLocalIndices: Array[Int],
             ratings: Array[Float]): this.type =
    {
      val sz = srcIds.length
      require(dstLocalIndices.length == sz)
      require(ratings.length == sz)
      this.srcIds ++= srcIds
      this.dstLocalIndices ++= dstLocalIndices
      this.ratings ++= ratings
      var j = 0
      while (j < sz) {
        this.dstBlockIds += dstBlockId
        this.dstEncodedIndices += encoder.encode(dstBlockId, dstLocalIndices(j))
        j += 1
      }
      this
    }

    /** Builds a [[UncompressedInBlock]]. */
    def build(): UncompressedInBlock = {
      new UncompressedInBlock(srcIds.result(), dstLocalIndices.result(), dstEncodedIndices.result(), dstBlockIds.result(), ratings.result())
    }
  }

  /**
   * A block of (srcId, dstEncodedIndex, rating) tuples stored in primitive arrays.
   */
  class UncompressedInBlock(
                             val srcIds: Array[Long],
                             val dstLocalIndices: Array[Int],
                             val dstEncodedIndices: Array[Int],
                             val dstBlockIds: Array[Int],
                             val ratings: Array[Float])(
                             implicit ord: Ordering[Long])
  {
    /** Size the of block. */
    def length: Int = srcIds.length

    /**
     * Compresses the block into an `InBlock`. The algorithm is the same as converting a sparse
     * matrix from coordinate list (COO) format into compressed sparse column (CSC) format.
     * Sorting is done using Spark's built-in Timsort to avoid generating too many objects.
     */
    def compress(): InBlock = {
      val sz = length
      assert(sz > 0, "Empty in-link block should not exist.")
      sort()
      val uniqueSrcIdsBuilder = mutable.ArrayBuilder.make[Long]
      val dstCountsBuilder = mutable.ArrayBuilder.make[Int]
      var preSrcId = srcIds(0)
      uniqueSrcIdsBuilder += preSrcId
      var curCount = 1
      var i = 1
      while (i < sz) {
        val srcId = srcIds(i)
        if (srcId != preSrcId) {
          uniqueSrcIdsBuilder += srcId
          dstCountsBuilder += curCount
          preSrcId = srcId
          curCount = 0
        }
        curCount += 1
        i += 1
      }
      dstCountsBuilder += curCount
      val uniqueSrcIds = uniqueSrcIdsBuilder.result()
      val numUniqueSrdIds = uniqueSrcIds.length
      val dstCounts = dstCountsBuilder.result()
      val dstPtrs = new Array[Int](numUniqueSrdIds + 1)
      var sum = 0
      i = 0
      while (i < numUniqueSrdIds) {
        sum += dstCounts(i)
        i += 1
        dstPtrs(i) = sum
      }
      InBlock(uniqueSrcIds, dstPtrs, dstEncodedIndices, ratings)
    }

    private def sort(): Unit = {
      val sz = length
      // Since there might be interleaved log messages, we insert a unique id for easy pairing.
      val sortId = util.Utils.random.nextInt()
      System.out.println(s"Start sorting an uncompressed in-block of size $sz. (sortId = $sortId)")
      val start = System.nanoTime()
      val sorter = new util.Sorter(new UncompressedInBlockSort)
      sorter.sort(this, 0, length, Ordering[KeyWrapper])
      val duration = (System.nanoTime() - start) / 1e9
      System.out.println(s"Sorting took $duration seconds. (sortId = $sortId)")
    }
  }




  private class KeyWrapper(implicit ord: Ordering[Long]) extends Ordered[KeyWrapper] {

    var key: Long = _

    override def compare(that: KeyWrapper): Int = {
      ord.compare(key, that.key)
    }

    def setKey(key: Long): this.type = {
      this.key = key
      this
    }
  }




  /**
   * [[SortDataFormat]] of [[UncompressedInBlock]] used by [[util.Sorter]].
   */
  private class UncompressedInBlockSort(implicit ord: Ordering[Long])  extends SortDataFormat[KeyWrapper, UncompressedInBlock] {

    override def newKey(): KeyWrapper = new KeyWrapper()

    override def getKey(
                         data: UncompressedInBlock,
                         pos: Int,
                         reuse: KeyWrapper): KeyWrapper =
    {
      if (reuse == null) {
        new KeyWrapper().setKey(data.srcIds(pos))
      } else {
        reuse.setKey(data.srcIds(pos))
      }
    }

    override def getKey(
                         data: UncompressedInBlock,
                         pos: Int): KeyWrapper =
    {
      getKey(data, pos, null)
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

    override def swap(data: UncompressedInBlock, pos0: Int, pos1: Int): Unit = {
      swapElements(data.srcIds, pos0, pos1)
      swapElements(data.dstEncodedIndices, pos0, pos1)
      swapElements(data.ratings, pos0, pos1)
    }

    override def copyRange(
                            src: UncompressedInBlock,
                            srcPos: Int,
                            dst: UncompressedInBlock,
                            dstPos: Int,
                            length: Int): Unit =
    {
      System.arraycopy(src.srcIds, srcPos, dst.srcIds, dstPos, length)
      System.arraycopy(src.dstEncodedIndices, srcPos, dst.dstEncodedIndices, dstPos, length)
      System.arraycopy(src.ratings, srcPos, dst.ratings, dstPos, length)
    }

    override def allocate(length: Int): UncompressedInBlock = {
      new UncompressedInBlock(
        new Array[Long](length), new Array[Int](length), new Array[Int](length), new Array[Int](length), new Array[Float](length))
    }

    override def copyElement(
                              src: UncompressedInBlock,
                              srcPos: Int,
                              dst: UncompressedInBlock,
                              dstPos: Int): Unit =
    {
      dst.srcIds(dstPos) = src.srcIds(srcPos)
      dst.dstEncodedIndices(dstPos) = src.dstEncodedIndices(srcPos)
      dst.ratings(dstPos) = src.ratings(srcPos)
    }
  }
}