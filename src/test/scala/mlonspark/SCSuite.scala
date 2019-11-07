package mlonspark

import org.apache.spark.SparkFunSuite
import org.apache.spark.internal.Logging
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.util.MLlibTestSparkContext

class SCSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest with Logging {

  private type ALSPartitioner = org.apache.spark.HashPartitioner

  test("sc") {
    val spark = this.spark
    val seq = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val rdd = spark.sparkContext.parallelize(seq)
    val rmd = rdd.map(v=> {
      (v%2, v)
    }).groupByKey(new ALSPartitioner(2)).mapValues(v=>{v})
    System.out.println(rmd.toDebugString)
    rmd.foreach(f => {
      System.out.println(f);
    })
  }

  test("scPartitions") {
    val spark = this.spark
    val seq = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9)
    val rdd = spark.sparkContext.parallelize(seq)
    val rmd = rdd.mapPartitions(iter=> {
      iter.flatMap(i=> {
        System.out.println(i);
        Iterator.empty
      } ++ {
        System.out.println("Iterator.empty");
        Iterator.empty
      })
    })
    System.out.println(rmd.toDebugString)
    rmd.foreach(f => {
      System.out.println("" + f);
    })
  }
}
