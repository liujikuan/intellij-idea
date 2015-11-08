/**
 * Created by lenovo on 2015/8/7.
 */

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming.flume.SparkFlumeEvent;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * 详细可以参考：https://cwiki.apache.org/confluence/display/KAFKA/0.8.0+Producer+Example
 */
public class KafkaProducer {
    private String topic;

    public KafkaProducer setEvent(SparkFlumeEvent event) {
        this.event = event;
        return this;
    }
    public KafkaProducer setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public KafkaProducer setBody(String body) {
        this.body = body;
        return this;
    }

    private String body;
    private SparkFlumeEvent event;
    public void sendData(String brokerList) throws UnsupportedEncodingException {
        int events=100;
//        SparkConf conf = ssc.sc().getConf();
//conf.get("spark.kafka.metadata.broker.list")
        Properties props = new Properties();
        props.put("metadata.broker.list",brokerList);//broker之间以逗号分隔
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        // key.serializer.class默认为serializer.class
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        // 可选配置，如果不配置，则使用默认的partitioner
        props.put("partitioner.class", "PartitionerDemo");
        // 触发acknowledgement机制，否则是fire and forget，可能会引起数据丢失
        // 值为0,1,-1,可以参考
        // http://kafka.apache.org/08/configuration.html
        //Random rnd = new Random();
        props.put("request.required.acks", "1");
        ProducerConfig config = new ProducerConfig(props);
        Producer<String, String> producer = new Producer<String, String>(config);
        // 产生并发送消息
        long start=System.currentTimeMillis();
        for (long i = 0; i < events; i++) {
            long runtime = new Date().getTime();
            String ip = "192.168.2." + i;//rnd.nextInt(255);
            String msg = runtime + ",www.example.com," + ip;
            //如果topic不存在，则会自动创建，默认replication-factor为1，partitions为0
            KeyedMessage<String, String> data = new KeyedMessage<String, String>(
                    topic, ip, body);/*例子中的topic为page_visits*/
            System.out.println("---------------producer.send(data)---------------- ");
            producer.send(data);
        }
        System.out.println("耗时:" + (System.currentTimeMillis() - start));

        producer.close();
    }
    public static void main(String[] args) {
    }
}