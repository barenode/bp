package mlonspark

import org.apache.spark.SparkFunSuite
import org.apache.spark.internal.Logging
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.linalg.{DenseVector, SparseVector, Vector}
import org.apache.spark.mllib.linalg.distributed.{IndexedRow, IndexedRowMatrix, RowMatrix}
import org.apache.spark.mllib.util.MLlibTestSparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType
import org.apache.spark.util.collection.OpenHashSet

class SimilarityTestSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest with Logging {

  test("sparse") {
    val spark = this.spark
    val seq = Seq[Vector](
      new SparseVector(5, Array(1, 2), Array(2, 2)),
      new SparseVector(5, Array(1), Array(2)),
      new SparseVector(5, Array(1, 2), Array(2, 2)),
      new SparseVector(5, Array(1), Array(2)),
      new SparseVector(5, Array(4), Array(2))
    )
    val rows = spark.sparkContext.parallelize(seq)
    val matrix = new RowMatrix(rows, 5 , 5)
    val indexedMatrix = new IndexedRowMatrix(matrix.rows.zipWithIndex().map{ case (v, id) => IndexedRow(id, v)})
    val indexedLocalMatrix = indexedMatrix.toBlockMatrix().toLocalMatrix()
    println("--------------------------------------------")
    println(indexedLocalMatrix.toString(Int.MaxValue, Int.MaxValue))
    println("--------------------------------------------")
    val similarity = matrix.columnSimilarities()
    val localMatrix =   similarity.toBlockMatrix().toLocalMatrix()
    println("--------------------------------------------")
    println(localMatrix.toString(Int.MaxValue, Int.MaxValue))
    println("--------------------------------------------")
  }

  test("dense") {
    val spark = this.spark
    val seq = Seq[Vector](
      new DenseVector(Array(1, 2, 3, 4, 5)),
      new DenseVector(Array(1, 2, 3, 4, 5)),
      new DenseVector(Array(1, 2, 3, 4, 5)),
      new DenseVector(Array(1, 2, 3, 4, 5)),
      new DenseVector(Array(1, 2, 3, 4, 5))
    )
    val rows = spark.sparkContext.parallelize(seq)
    val matrix = new RowMatrix(rows, 5 , 5)
    val similarity = matrix.columnSimilarities()
    val localMatrix =   similarity.toBlockMatrix().toLocalMatrix()
    println("--------------------------------------------")
    println(localMatrix.toString(Int.MaxValue, Int.MaxValue))
    println("--------------------------------------------")
  }

  test("zipWithIndex") {
    val seq = Seq((5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10))
    val rdd = spark.sparkContext.parallelize(seq).zipWithIndex()

    val seq2 = seq.flatMap{i=>
      Iterator.empty
    } ++ {
      Iterator.single(1)
    }
    println("seq2: " + seq2.mkString(", "));

    val rdx = rdd.mapPartitions {iter =>
      println(iter.mkString(", "))
      iter.map{case ((src, dest), index) =>
        (index, src, dest)
      }
    }
    println(rdx.collect().mkString(", "));

    val rde = rdd.mapPartitions {iter =>
      iter.map{i =>
        (i._2, i._1._1, i._1._2)
      }
    }
    println(rde.collect().mkString(", "));

    val rdf = rdd.map {i =>
      (i._2, i._1._1, i._1._2)
    }
    println(rdf.collect().mkString(", "));


//    val sqlContext = spark.sqlContext
//    import sqlContext.implicits._
//
//    println("partitions: " + rdd.getNumPartitions);
//    rdd.repartition(5);
//
//    println(rdd.toDebugString)
//    println(rdd.collect().mkString(", "))
//    println("partitions: " + rdd.getNumPartitions);
//
//    val res = rdd.map {
//      case ((src, dest), index) =>
//        (index)
//    }
//
//    println(res.toDebugString)
//    println(res.collect().mkString(", "))
//
//    val df = res.toDF("index")
//    df.printSchema()
  }



}
