package mlonspark

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.sql.types.{ArrayType, IntegerType, StructField, StructType}

class SimpleSuite extends SparkFunSuite with MLlibTestSparkContext {

  private type ALSPartitioner = org.apache.spark.HashPartitioner
  test("partitioner") {
    val partitioner = new ALSPartitioner(2)
    assert(partitioner.numPartitions==2)
    assert(1==partitioner.getPartition(1))
    assert(0==partitioner.getPartition(2))
    assert(1==partitioner.getPartition(3))
  }

  test("test with index") {
    val x = Seq(1, 2, 3, 4).zipWithIndex.zipWithIndex
//    x.foreach(v => {
//      System.out.println(v._1 + " - " + v._2)
//    })

    x.zipWithIndex.map{case (v, idx) => v}.filter{_ => true}

  }

  def gtw(vl : Int) : Int = vl

  test("dataFrame") {
    val spark = this.spark
    import spark.implicits._
    val dataset = Seq(
      (1, .1),
      (2, .2)
    ).toDF("id", "rating")
    val userFactors = Seq(
      (1, Seq(1, 2, 3)),
      (2, Seq(2, 3, 4)),
      (3, Seq(3, 4, 5)),
      (4, Seq(4, 5, 6))
    ).toDF("id", "features")

    val t = gtw(1)
//    dataset.join(userFactors, dataset("id") === userFactors("id"))
//      .select(met(dataset("id")))
//      .show()

    dataset.crossJoin(userFactors)
      .as[(Int, Double, Int, Seq[Int])]
      .map{ r => {
        r._2
    }}.show()
  }

  import org.apache.spark.sql.functions._
  val met = udf{(x: Int) => {
    print("predict " + x)
    x
  }}
}

