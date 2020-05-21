package mlonspark

class Test {

  def f() : Int = {
    test(2, b = 2)
  }

  def test(x : Int, test : Int = 0, b : Int) : Int = {
    0
  }

  def test2() : F = {
    val p = Seq((1, 2), (1, 2), (1, 2))
    p.foreach{case (a, b)=>{

    }}
    F(1, 2)
  }

  case class F (b : Int, c : Int)
}

trait T {

}

class X {

}

class Y {

}

class Z extends X with T {

  def x : X = {
    new X
  }
}