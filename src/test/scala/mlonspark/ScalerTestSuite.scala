package mlonspark

import org.apache.spark.SparkFunSuite
import org.apache.spark.internal.Logging
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.util.MLlibTestSparkContext

class ScalerTestSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest with Logging {

  test("scaler") {
    val spark = this.spark
    import spark.implicits._

    val dataset = Seq(
      (1, 3, 1.0f),
      (1, 3, 2.0f),
      (1, 3, 10.0f),
      (2, 3, 3.0f),
      (2, 3, 0.5f)
    ).toDF("id", "otherId", "value")

    val model = new Scaler("uid")
      .setGroupCol("id")
      .setInputCol("value")
      .setOutputCol("scaled")
      .fit(dataset)

    val result = model.transform(dataset)
    result.collect().foreach(println)
  }
}
