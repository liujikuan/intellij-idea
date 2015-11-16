import org.apache.spark.{SparkConf, SparkContext}
/* SimpleApp.scala */

object SparkContextTest {
  def main(args: Array[String]) {
    val logFile = "/data0/spark/spark-1.4.1-bin-hadoop2.5.2/README.md" // Should be some file on your system
    val conf = new SparkConf().setAppName("liujikuan's Application").setMaster("spark://vm-spider-57:7077")
    val sc = new SparkContext(conf)
    val logData = sc.textFile(logFile, 2).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))



  }
}