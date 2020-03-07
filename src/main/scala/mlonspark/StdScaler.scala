package mlonspark

import java.lang.Math

import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType

trait StdScalerParams extends Params with HasInputCol with HasOutputCol {

  protected def validateAndTransformSchema(schema: StructType): StructType = {
    schema
  }
}

class StdScaler(override val uid: String) extends Estimator[StdScalerModel] with StdScalerParams {

  /** @group setParam */
  def setInputCol(value: String): this.type = set(inputCol, value)

  /** @group setParam */
  def setOutputCol(value: String): this.type = set(outputCol, value)

  override def fit(dataset: Dataset[_]): StdScalerModel = {
    import dataset.sparkSession.implicits._

    val avgRdd = dataset.select(col($(inputCol))).rdd
      .map {row=>
        row.getFloat(0)
      }.mapPartitions {iter =>
        Iterator.single(1, (iter.size, iter.sum))
      }.groupByKey().mapValues {values=>
        var totalCount = 0.0
        var totalSum = 0.0
        values.foreach{case(size, sum)=>
          totalCount += size
          totalSum += sum
        }
        totalSum/totalCount
      }
    val avg = avgRdd.collect()(0)._2

    val stdDevRdd = dataset.select(col($(inputCol))).rdd
      .map {row=>
        row.getFloat(0)
      }.mapPartitions { iter =>
        var tmp = 0.0
        iter.foreach{value=>
          tmp += (value-avg)*(value-avg)
        }
        Iterator.single(1, (iter.size, tmp))
    }.groupByKey().mapValues {values=>
      var totalCount = 0.0
      var totalSum = 0.0
      values.foreach{case(size, sum)=>
        totalCount += size
        totalSum += sum
      }
      totalSum/totalCount
    }
    val stdDev = Math.sqrt(stdDevRdd.collect()(0)._2)

    copyValues(new StdScalerModel(uid, avg, stdDev).setParent(this))
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  override def copy(extra: ParamMap): StdScaler = defaultCopy(extra)
}

class StdScalerModel(override val uid: String, val avg: Double, val stdDev: Double) extends Model[StdScalerModel] with StdScalerParams {

  private val predict = udf { (value: Double) =>
    value/stdDev
  }

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset.toDF()
  }

  override def transformSchema(schema: StructType): StructType = {
    validateAndTransformSchema(schema)
  }

  override def copy(extra: ParamMap): ScalerModel = {
    val copied = new ScalerModel(uid, df)
    copyValues(copied, extra).setParent(parent)
  }
}
