package mllib

import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.linalg.distributed._
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
/**
 * Created by lenovo on 2015/10/13.
 */
object Matrix {
  def main(args: Array[String]) {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
    val conf = new SparkConf().setAppName("liujikuan's Application").setMaster("local[2]")
    val sc = new SparkContext(conf)

    var arr=Array(1.0,2,3,4)
    var vec=Vectors.dense(arr)
    val rdd=sc.makeRDD(Seq(Vectors.dense(arr),Vectors.dense(arr.map(_*10)),Vectors.dense(arr.map(_*100))))

    var dm = new DenseMatrix(2,2,arr)
    println("dm.toString() = \n" + dm.toString())

    var sparseM =  Matrices.sparse(3,2,Array(3,2,6),Array(2,3,4,5,6,7),Array(1,2,3,4,5,6))
    println("sparseM.toString() = \n" + sparseM.toString())

    others(sc)

    val mat: RowMatrix = new RowMatrix(rdd)
    println("mat.rows.count() = " + mat.rows.count())
    println("mat.numRows() = " + mat.numRows())
    println("mat.numCols() = " + mat.numCols())
    rdd.collect().foreach(vector=>( println("========== " ), vector.toArray.foreach(cell=> println("cell = " + cell))))
    var sum=Statistics.colStats(rdd)
    println("sum.mean = " + sum.mean)
  }

  def others(sc: SparkContext): SQLContext = {
    SQLContext.getOrCreate(sc)
  }
}
