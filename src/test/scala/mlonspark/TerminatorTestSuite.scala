package mlonspark

import mlonspark.AlternatingLeastSquare.{KeyWrapper, UncompressedInBlock}
import mlonspark.util.SortDataFormat
import org.apache.spark.SparkFunSuite
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.util.MLlibTestSparkContext

class TerminatorTestSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest {

  test("test") {
    val rdd = spark.sparkContext.parallelize(Seq(
      (1, 1, 2f),
      (4, 3, 4f),
      (3, 4, 1f),
      (3, 1, 1f),
      (1, 5, 1f),
      (2, 3, 2f)
    ))
    Terminator.process(rdd)

  }
}
