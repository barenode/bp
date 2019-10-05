package samples

import org.apache.spark.ml.recommendation.ALS.Rating
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel
import org.apache.spark.{SparkConf, SparkFunSuite}
import org.apache.spark.mllib.util.LocalClusterSparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.scalatest.FunSuite

import scala.reflect.ClassTag

class FooTest extends SparkFunSuite {

  test("foo") {

    val conf = new SparkConf()
      .setMaster("local-cluster[2, 1, 1024]")
      .setAppName("test-cluster")
      .set("spark.rpc.message.maxSize", "1")
    val spark = SparkSession.builder().config(conf).getOrCreate()
    import org.apache.spark.sql.Dataset
    val rawArtistData = spark.read.textFile("/filmy/data/user_artist_data.txt")
    val count = rawArtistData.count()
    System.out.println("===============================")
    System.out.println(count)
    System.out.println("===============================")

    rawArtistData.toDF().write.parquet("/filmy/data/user_artist_data/")
  }

}
