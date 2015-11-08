package mllib

import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, MatrixEntry, RowMatrix}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Created by liujikuan on 2015/10/13.
 */
object DIMSUM {
  var sc:SparkContext = null
  /**
   *
   * @param args args(0)代表输入源的地址
   */
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)

    val conf = new SparkConf().setAppName("liujikuan's Application")//.setMaster("local[2]")
    sc = new SparkContext(conf)

    var rowMat: RowMatrix = COOmatrix(args(0))//"F:\\房多统一日志平台\\算法\\second_user_house_score_tr2.txt"
    val simsPerfect = rowMat.columnSimilarities() // Compute similar columns perfectly, with brute force.
    println("the result is: ")
    var startTime = System.currentTimeMillis()
/*    simsPerfect.entries.foreach(entry => {
      println("i=" + entry.i + " j=" + entry.j + " entry=" + entry.value)
    })*/
    outputToHDFS(simsPerfect,args(1))
    var stopTime = System.currentTimeMillis()
    println("the calculation's elapsing time is " + (stopTime - startTime ))


    // Compute similar columns with estimation using DIMSUM
    /*  val threshold = 1.0.toDouble
        val simsEstimate = rowMat.columnSimilarities(threshold)
        println("the result with threshold is: ")
        simsEstimate.entries.foreach(entry => {
          println("i=" + entry.i + " j=" + entry.j + " entry=" + entry.value)
        })*/
  }

  def outputToHDFS(simsPerfect: CoordinateMatrix,desc:String): Unit = {
    val rdd: RDD[(Long, Long, Double)] = simsPerfect.entries.map(entry=>(entry.i,entry.j,entry.value))
    println("rdd.first.toString() = " + rdd.first.toString())
    rdd.saveAsTextFile(desc)
   /* val rdd1: RDD[(String, Int)] = sc.makeRDD(Array(("A", 2), ("A", 1), ("B", 6), ("B", 3), ("B", 7)))
    val rdd2: RDD[(String, Double)] = simsPerfect.entries.map(entry=>("",entry.value))*///只有这种格式才能调用saveAsHadoopFile方法
  }

  /**
   *
   * @param filename  文件，文件里的第一列是user_id，第二列是house_id，第三列是评分
   * @return 一个矩阵，其中矩阵的行是文件里的第一列，矩阵的列是文件里的第二列，矩阵里的每一个元素是评分
   */
  def COOmatrix( filename: String): RowMatrix = {
    // Load and parse the data file.
    val rows = sc.textFile(filename).map { line =>
      val values = line.split(',').map(_.toDouble)
      new MatrixEntry(values(0).toLong, values(1).toLong, values(2))
    }
    var mat = new CoordinateMatrix(rows)
    var rowMat = mat.toRowMatrix()
    println("rowMat.numRows() = " + rowMat.numRows())
    println("rowMat.numCols() = " + rowMat.numCols())
   //  rowMat.rows.foreach(vector => vector.apply(0))
    rowMat
  }

  /**
   *
   * @param filename  文件,必须只有三列,列与列之间用空格隔开
   * @return  一个矩阵.其中矩阵的列对应的是文件里的列
   */
  def vectorToMatrix( filename: String): RowMatrix = {
    val rows = sc.textFile(filename).map { line =>
      val values = line.split(',').map(_.toDouble)
      Vectors.dense(values)
    }
    rows.collect().foreach(vector => (println("========== "), vector.toArray.foreach(cell => println("cell = " + cell))))
    new RowMatrix(rows)

  }
}
