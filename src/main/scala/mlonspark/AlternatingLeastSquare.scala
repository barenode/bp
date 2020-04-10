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
    val predictions =
    //tag::model-transform[]
      dataset
      .join(userFactors, dataset($(userCol)) === userFactors("id"), "left")
      .join(itemFactors, dataset($(itemCol)) === itemFactors("id"), "left")
      .select(
        dataset("*"),
        predict(userFactors("features"), itemFactors("features")).as($(predictionCol)))
    //end::model-transform[]
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

    //tag::dataset-to-rdd[]
    val ratings = dataset
      .select(col($(userCol)), col($(itemCol)), col($(ratingCol))).rdd
      .map { row =>
        (row.getInt(0), row.getInt(1), row.getFloat(2))}
    //end::dataset-to-rdd[]

    val (userFactors, itemFactors) = ALSEngine.train(
      ratings,
      rank = $(rank),
      numUserBlocks = $(numUserBlocks),
      numItemBlocks = $(numItemBlocks),
      maxIter = $(maxIter),
      regParam = $(regParam),
      alpha = $(alpha),
      finalRDDStorageLevel = StorageLevel.fromString($(finalStorageLevel))
    )

    val userDF = userFactors.toDF("id", "features")
    val itemDF = itemFactors.toDF("id", "features")

    new AlternatingLeastSquareModel(uid, $(rank), userDF, itemDF)
  }

  override def copy(extra: ParamMap): AlternatingLeastSquare = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    schema
  }
}

