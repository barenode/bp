package samples

import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.util.{LocalClusterSparkContext, MLlibTestSparkContext}

class SparkTest extends SparkFunSuite with LocalClusterSparkContext {

  test("sc") {

    assert(sc!=null)
    var rdd = sc.parallelize(
      List(
        (1, 2), (1, 2), (1, 2), (1, 2)
      )
    )

    var count = rdd.count()
    System.out.println("===============================")
    System.out.println(count)
    System.out.println("===============================")
  }
}
