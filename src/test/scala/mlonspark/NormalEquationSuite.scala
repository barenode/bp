package mlonspark

import org.apache.spark.SparkFunSuite
import org.apache.spark.internal.Logging
import org.apache.spark.ml.util.DefaultReadWriteTest
import org.apache.spark.mllib.util.MLlibTestSparkContext

class NormalEquationSuite extends SparkFunSuite with MLlibTestSparkContext with DefaultReadWriteTest with Logging {

  test("sc") {
    val ne = new AlternatingLeastSquare.NormalEquation(3)
    ne.add(Array(1, 2, 3), 1)
//    System.out.println("-------------------------------")
//    System.out.println(ne.ata.mkString(" "));
//    System.out.println("-------------------------------")
//    ne.add(Array(2, 3, 4), 1)
//    System.out.println(ne.ata.mkString(" "));
//    System.out.println("-------------------------------")
//    System.out.println(ne.atb.mkString(" "));
//    System.out.println("-------------------------------")


    val solver = new AlternatingLeastSquare.CholeskySolver
    val result = solver.solve(ne, 1.0)
    System.out.println(result.mkString(" "));
  }
}
