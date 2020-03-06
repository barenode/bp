package mlonspark

import org.apache.spark.SparkFunSuite
import org.apache.spark.internal.Logging
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.rdd.RDD

class SCSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest with Logging {

  private type ALSPartitioner = org.apache.spark.HashPartitioner

  test("sc") {
    val spark = this.spark
    //tag::spark-example-1[]
    val rdd = spark.sparkContext.parallelize(Seq(1, 2, 3, 4, 5, 6, 7, 8, 9))
      .map(v => (v%2, v))
      .groupByKey()
      .mapValues(v=>v.size)
    //end::spark-example-1[]
    System.out.println(rdd.toDebugString)
    rdd.foreach(f => {
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

  test("group") {
    val seq = Seq(
      (1, 1),
      (1, 2),
      (2, 2),
      (3, 2),
      (3, 1),
      (4, 5)
    )

    val seq2 = Seq(
      (1, 1),
      (1, 2),
      (2, 2),
      (3, 2),
      (3, 1),
      (4, 5)
    )
    val rdd1 = spark.sparkContext.parallelize(seq)
    val rdd2 = spark.sparkContext.parallelize(seq2)

    var rdd = rdd1.join(rdd2)

    print(rdd.collect().mkString("\n"))
    println("---------------------------------------------")
    print(
      rdd.groupByKey()
        .flatMap{
          case(k, v)=>v
        }
        .collect()
        .mkString("\n"))

//    rdd.map {
//      case (key, (v1, v2)) =>
//        (key, v2)
//    }
  }
}
