package mlonspark

import org.apache.spark.SparkFunSuite

class SimpleSuite extends SparkFunSuite{

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
}
