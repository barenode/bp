package mlonspark

import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.param.{ParamMap, Params}
import org.apache.spark.ml.param.shared.HasPredictionCol
import org.apache.spark.ml.util.{DefaultParamsWritable, Identifiable, MLReadable, MLReader, MLWritable, MLWriter}
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.types.StructType

/**
 * AlternatingLeastSquareModelParams
 */
private trait AlternatingLeastSquareModelParams
  extends Params with HasPredictionCol
{
}

/**
 * AlternatingLeastSquareModel
 *
 * @param uid
 */
class AlternatingLeastSquareModel(override val uid: String)
  extends AlternatingLeastSquareModelParams with MLWritable
{
  import AlternatingLeastSquareModel._

  def this() = this(Identifiable.randomUID("AlternatingLeastSquare"))

  override def copy(extra: ParamMap): AlternatingLeastSquareModel = defaultCopy(extra)

  override def write: MLWriter = new AlternatingLeastSquareModelWriter(this)
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
