import java.nio.ByteBuffer
import java.sql.Connection
import java.util

import org.apache.log4j.{Level, Logger}

//import hbase.transform.TransformHandler
import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.flume.{FlumeUtils, SparkFlumeEvent}
import org.apache.spark.streaming.{Seconds, StreamingContext}
object SparkStreamingTest {
  def fromFlume() = {
    val conf = new SparkConf().setMaster("local[2]").setAppName("connectHBase")
    val ssc = new StreamingContext(conf, Seconds(1))
    //    ssc.checkpoint("checkpoint")
    //    val topicMap = "topic1,topic2".split(",").map((_, 3.toInt)).toMap
    //    val lines = ssc.socketTextStream("localhost", 9999)
    //    val lines = KafkaUtils.createStream(ssc, "zk54", "spark", topicMap).map(_._2)
    val port = conf.getInt("spark.flume.listener.port",44446)
    val stream = FlumeUtils.createStream(ssc, "0.0.0.0", port, StorageLevel.MEMORY_ONLY_SER_2)
    //    FlumeUtils.createPollingStream()
    stream.count().map(cnt => "Received " + cnt + " flume events.").print()
    stream.foreachRDD(flumeRDD => flumeRDD.foreach(event => connectKafka(event,conf)))
    ssc.start() // Start the computation
    ssc.awaitTermination() // Wait for the computation to terminate


  }
  var conn: Connection = null

  def connectKafka(event: SparkFlumeEvent,conf:SparkConf): Unit = {
    System.out.println("---------------connectKafka(event)---------------- ")
    val brokerList = conf.get("spark.kafka.metadata.broker.list")
    val topic = conf.get("spark.kafka.metadata.topic")
    new KafkaProducer().setBody(new String(event.event.getBody.array, "UTF-8")).setTopic(topic).sendData(brokerList)
  }


  def createEvent: SparkFlumeEvent = {
    val event: SparkFlumeEvent = new SparkFlumeEvent()
    val map = new util.HashMap[CharSequence, CharSequence]()
    map.put("source", "1")
    map.put("businessType", "1")
    //    map += ("source" -> "1")
    //    map += ("businessType" -> "1")
    event.event.setHeaders(map)
    var body = "9002\0079002\0079002\0079002\007102.1\007101.2\0072002-12-22 23:22:22.123456\0071\0075001\007Success\007RichMan"
    event.event.setBody(ByteBuffer.wrap(body.getBytes()))
    event
  }


  def connectHBase(event: SparkFlumeEvent) = {
    val it = event.event.getHeaders.entrySet().iterator()
    //      val myHbase =TransformHandler.transform(event)
    while (it.hasNext) {
      var next = it.next()
      println("event.header.getValue = " + next.getValue)
    }
  }

  def main(args: Array[String]) {
//    initMysql()
      // connectHBase(createEvent)
  //  new KafkaProducer().setBody("liujikuan").setTopic("test").sendData("rs54:9092,rs55:9092")
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
 //   fromFlume()
  }
}
/*
  def connectKafka(event: SparkFlumeEvent,ssc: StreamingContext): Unit = {
    // Set up the input DStream to read from Kafka (in parallel)
    val kafkaStream = {
      val sparkStreamingConsumerGroup = "spark-streaming-consumer-group"
      val kafkaParams = Map(
        "zookeeper.connect" -> "zookeeper1:2181",
        "group.id" -> "spark-streaming-test",
        "zookeeper.connection.timeout.ms" -> "1000")
      val inputTopic = "input-topic"
      val numPartitionsOfInputTopic = 5
      val streams = (1 to numPartitionsOfInputTopic) map { _ =>
        KafkaUtils.createStream(ssc, kafkaParams, Map(inputTopic -> 1), StorageLevel.MEMORY_ONLY_SER).map(_._2)
      }
      val unifiedStream = ssc.union(streams)
      val sparkProcessingParallelism = 1 // You'd probably pick a higher value than 1 in production.
      unifiedStream.repartition(sparkProcessingParallelism)
    }

    // We use accumulators to track global "counters" across the tasks of our streaming app
    val numInputMessages = ssc.sparkContext.accumulator(0L, "Kafka messages consumed")
    val numOutputMessages = ssc.sparkContext.accumulator(0L, "Kafka messages produced")
    // We use a broadcast variable to share a pool of Kafka producers, which we use to write data from Spark to Kafka.
    val producerPool = {
      val pool = createKafkaProducerPool(kafkaZkCluster.kafka.brokerList, outputTopic.name)
      ssc.sparkContext.broadcast(pool)
    }
    // We also use a broadcast variable for our Avro Injection (Twitter Bijection)
    val converter = ssc.sparkContext.broadcast(SpecificAvroCodecs.toBinary[Tweet])

    // Define the actual data flow of the streaming job
    kafkaStream.map { case bytes =>
      numInputMessages += 1
      // Convert Avro binary data to pojo
      converter.value.invert(bytes) match {
        case Success(tweet) => tweet
        case Failure(e) => // ignore if the conversion failed
      }
    }.foreachRDD(rdd => {
      rdd.foreachPartition(partitionOfRecords => {
        val p = producerPool.value.borrowObject()
        partitionOfRecords.foreach { case tweet: Tweet =>
          // Convert pojo back into Avro binary format
          val bytes = converter.value.apply(tweet)
          // Send the bytes to Kafka
          p.send(bytes)
          numOutputMessages += 1
        }
        producerPool.value.returnObject(p)
      })
    })

  }
*/

/*  def connectMySQL(event: SparkFlumeEvent): Unit = {
    var body = new String(event.event.getBody.array(), "UTF-8")

    try {
      var preparedStatement = conn.prepareStatement("insert into " + "tablename" + " values (?,?,?,?,?,?,?,?,?,?)")
      preparedStatement.clearBatch

      val strs: Array[String] = body.split("\7")
      var i: Int = 0
      while (i < strs.length) {
        System.out.println("str[" + i + "] = " + strs(i) + " by liujikuan")
        preparedStatement.setString(i + 1, strs(i))
        i += 1
      }
      preparedStatement.addBatch
//      for (temp <- Array("a\7b", "", "")) {}
      preparedStatement.executeBatch
      conn.commit
    }
    catch {
      case e: SQLException => {
        e.printStackTrace
        System.exit(1)
      }
    }
  }*/
/*  def initMysql() = {
    Class.forName("com.mysql.jdbc.Driver")
    val url: String = "jdbc:mysql://" + "10.0.4.54" + ":" + 3306 + "/" + "flume?useUnicode=true&characterEncoding=UTF-8"
    conn = DriverManager.getConnection(url, "liujikuan", "ljkAdmin123")
    conn.setAutoCommit(false)
  }*/