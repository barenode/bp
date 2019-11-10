package mlonspark

import scala.util.{Sorting, Try}
import org.apache.spark.ml.param._
import org.apache.spark.ml.param.shared.{HasMaxIter, HasPredictionCol, HasRegParam, HasSeed}
import org.apache.spark.ml.util.{MLReadable, MLReader, MLWritable, MLWriter}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType
import org.apache.spark.storage.StorageLevel

trait FCXParams extends Params with HasMaxIter with HasRegParam with HasPredictionCol with HasSeed {

  val userCol = new Param[String](this, "userCol", "column name for user ids. Ids must be within " +
    "the integer value range.")

  /** @group getParam */
  def getUserCol: String = $(userCol)

  val itemCol = new Param[String](this, "itemCol", "column name for item ids. Ids must be within " +
    "the integer value range.")

  /** @group getParam */
  def getItemCol: String = $(itemCol)

  val rank = new IntParam(this, "rank", "rank of the factorization", ParamValidators.gtEq(1))

  /** @group getParam */
  def getRank: Int = $(rank)

  /**
   * Param for number of user blocks (positive).
   * Default: 10
   * @group param
   */
  val numUserBlocks = new IntParam(this, "numUserBlocks", "number of user blocks",
    ParamValidators.gtEq(1))

  /** @group getParam */
  def getNumUserBlocks: Int = $(numUserBlocks)

  /**
   * Param for number of item blocks (positive).
   * Default: 10
   * @group param
   */
  val numItemBlocks = new IntParam(this, "numItemBlocks", "number of item blocks",
    ParamValidators.gtEq(1))

  /** @group getParam */
  def getNumItemBlocks: Int = $(numItemBlocks)

  /**
   * Param for the alpha parameter in the implicit preference formulation (nonnegative).
   * Default: 1.0
   * @group param
   */
  val alpha = new DoubleParam(this, "alpha", "alpha for implicit preference",
    ParamValidators.gtEq(0))

  /** @group getParam */
  def getAlpha: Double = $(alpha)

  /**
   * Param for the column name for ratings.
   * Default: "rating"
   * @group param
   */
  val ratingCol = new Param[String](this, "ratingCol", "column name for ratings")

  /** @group getParam */
  def getRatingCol: String = $(ratingCol)

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

  /** @group expertGetParam */
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

  /** @group expertGetParam */
  def getFinalStorageLevel: String = $(finalStorageLevel)

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

class FCXModel(
    override val uid: String,
    val rank: Int,
    @transient val userFactors: DataFrame,
    @transient val itemFactors: DataFrame
  ) extends Model[FCXModel] with FCXParams with MLWritable {

  override def copy(extra: ParamMap): FCXModel = {
    val copied = new FCXModel(uid, rank, userFactors, itemFactors)
    copyValues(copied, extra).setParent(parent)
  }

  override def write: MLWriter = ???

  override def transform(dataset: Dataset[_]): DataFrame = ???

  override def transformSchema(schema: StructType): StructType = ???

  override val uid: String = _
}

object FCXModel extends MLReadable[FCXModel] {
  override def read: MLReader[FCXModel] = {

  }
}
