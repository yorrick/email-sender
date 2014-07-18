//import scala.concurrent.{Await, Future, blocking}
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration._
//
////def f(item: Int): Future[Unit] = Future{
////  print("Waiting " + item + " seconds ...")
////  Console.flush
////  blocking{Thread.sleep((item seconds).toMillis)}
////  println("Done")
////}
////
////val fSerial = f(4) flatMap(res1 => f(16)) flatMap(res2 => f(2)) flatMap(res3 => f(8))
////
////fSerial.onComplete{case resTry => println("!!!! That's a wrap !!!! Success=" + resTry.isSuccess)}
//
//def calculator = {
//  Future {
//    val rand = math.random
//    if(rand < 0.5) rand else throw new Exception("Oh no")
//  }
//}
//
//val result = calculator map { calculated =>
//  calculated.toString
//} recover {
//  case e: Exception => "got exception"
//}
//
//println(Await.result(result, 1 second))
