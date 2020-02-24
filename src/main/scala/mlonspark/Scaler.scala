package mlonspark

import org.apache.spark.annotation.Since
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.feature.{MinMaxScaler, MinMaxScalerModel, MinMaxScalerParams}
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.ml.util.{DefaultParamsWritable, MLWritable}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

trait ScalerParams extends Params with HasInputCol with HasOutputCol {

  final val groupCol: Param[String] = new Param[String](this, "groupCol", "group column name")

  /** @group getParam */
  final def getGroupCol: String = $(groupCol)

  protected def validateAndTransformSchema(schema: StructType): StructType = {
    schema
  }
}

class Scaler(override val uid: String) extends Estimator[ScalerModel] with ScalerParams {

  /** @group setParam */
  def setInputCol(value: String): this.type = set(inputCol, value)

  /** @group setParam */
  def setOutputCol(value: String): this.type = set(outputCol, value)

  /** @group setParam */
  def setGroupCol(value: String): this.type = set(groupCol, value)

  override def fit(dataset: Dataset[_]): ScalerModel = {
    import dataset.sparkSession.implicits._

    val ds = dataset.select(col($(groupCol)), col($(inputCol))).rdd
    val rdd = ds.map {row=>
      (row.getInt(0), row.getFloat(1))
    }.groupByKey().map{case (id, values) =>
      var min = Float.MaxValue
      var max = Float.MinValue
      values.foreach{value=>
        min = Math.min(min, value)
        max = Math.max(max, value)
      }
      (id, min, max)
    }
    copyValues(new ScalerModel(uid, rdd.toDF("id", "min", "max")).setParent(this))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  override def copy(extra: ParamMap): Scaler = defaultCopy(extra)
}

class ScalerModel(override val uid: String, df: DataFrame) extends Model[ScalerModel] with ScalerParams {

  /** @group setParam */
  def setInputCol(value: String): this.type = set(inputCol, value)

  /** @group setParam */
  def setOutputCol(value: String): this.type = set(outputCol, value)

  /** @group setParam */
  def setGroupCol(value: String): this.type = set(groupCol, value)

  private val predict = udf { (value: Float, min: Float, max: Float) =>
    if (min == max) {
      if (min==0) {
        0f
      } else {
        1f
      }
    } else {
      (value - min) / (max - min) * 9f + 1.0f
    }
  }

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset
      .join(df, dataset($(groupCol)) === df("id"))
      .select(dataset("*"), predict(dataset($(inputCol)), df("min"), df("max")).as($(outputCol)))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  override def copy(extra: ParamMap): ScalerModel = {
    val copied = new ScalerModel(uid, df)
    copyValues(copied, extra).setParent(parent)
  }
}
