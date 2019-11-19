package mlonspark

import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.param.{IntParam, ParamMap, ParamValidators, Params}
import org.apache.spark.ml.param.shared.HasPredictionCol
import org.apache.spark.ml.util.{DefaultParamsWritable, Identifiable, MLReadable, MLReader, MLWritable, MLWriter}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType

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
   * Param for rank of the matrix factorization (positive).
   * Default: 10
   * @group param
   */
  //tag::params-rank[]
  val rank = new IntParam(this, "rank", "rank of the factorization", ParamValidators.gtEq(1))
  def getRank: Int = $(rank)
  //end::params-rank[]
}

/**
 * AlternatingLeastSquareModel
 *
 * @param uid
 */
//tag::model-def[]
class AlternatingLeastSquareModel(override val uid: String)
  extends Model[AlternatingLeastSquareModel]
    with AlternatingLeastSquareModelParams
    with MLWritable
    //end::model-def[]
{
  import AlternatingLeastSquareModel._

  def this() = this(Identifiable.randomUID("AlternatingLeastSquare"))

  override def copy(extra: ParamMap): AlternatingLeastSquareModel = defaultCopy(extra)

  override def write: MLWriter = new AlternatingLeastSquareModelWriter(this)

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset.select("*")
  }
  override def transformSchema(schema: StructType): StructType = {
    schema
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
    }
  }

  private class AlternatingLeastSquareModelReader extends MLReader[AlternatingLeastSquareModel] {
    override def load(path: String): AlternatingLeastSquareModel = {
      new AlternatingLeastSquareModel()
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
    with AlternatingLeastSquareModelParams
    with DefaultParamsWritable
{

  override def fit(dataset: Dataset[_]): AlternatingLeastSquareModel = {
    new AlternatingLeastSquareModel()
  }

  override def copy(extra: ParamMap): AlternatingLeastSquare = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    schema
  }
}
