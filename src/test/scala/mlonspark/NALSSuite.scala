package mlonspark

import java.{util => ju}

import org.apache.spark.{Partitioner, SparkFunSuite}
import org.apache.spark.internal.Logging
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Sorting
import org.apache.spark.util.{BoundedPriorityQueue, Utils}

class NALSSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest with Logging {

  test("nals") {
    val spark = this.spark
    val seq = Seq(
      NALS.Rating(0, 1, 0.1f),
      NALS.Rating(0, 4, 0.4f),
      NALS.Rating(0, 7, 0.7f),
      NALS.Rating(3, 1, 3.1f),
      NALS.Rating(3, 4, 3.4f),
      NALS.Rating(3, 7, 3.7f),
      NALS.Rating(6, 1, 6.1f),
      NALS.Rating(6, 4, 6.4f),
      NALS.Rating(6, 7, 6.7f)
    )
    val rdd = spark.sparkContext.parallelize(seq)
    NALS.train(rdd, 2, 2)
  }

}

