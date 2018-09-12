/**
 * Created by maaz on 8/26/18.
 */
import com.google.common.io.Closeables;

import com.google.common.primitives.Bytes;
import org.apache.commons.lang.ArrayUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.receiver.Receiver;
import scala.Byte;
import scala.Tuple2;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Custom Receiver that receives data over a socket. Received bytes is interpreted as
 * text and \n delimited lines are considered as records. They are then counted and printed.
 *
 * Usage: JavaCustomReceiver <master> <hostname> <port>
 *   <master> is the Spark master URL. In local mode, <master> should be 'local[n]' with n > 1.
 *   <hostname> and <port> of the TCP server that Spark Streaming would connect to receive data.
 *
 * To run this on your local machine, you need to first run a Netcat server
 *    `$ nc -lk 9999`
 * and then run the example
 *    `$ bin/run-example org.apache.spark.examples.streaming.JavaCustomReceiver localhost 9999`
 */

public class JavaCustomReceiver extends Receiver<String> {
    private static final Pattern SPACE = Pattern.compile(" ");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: JavaCustomReceiver <FilePath> ");
            System.exit(1);
        }

        //StreamingExamples.setStreamingLogLevels();

        // Create the context with a 1 second batch size
        SparkConf sparkConf = new SparkConf().setMaster("local[2]").setAppName("JavaCustomReceiver");
        JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(1000));

        // Create an input stream with the custom receiver on target ip:port and count the
        // words in input stream of \n delimited text (eg. generated by 'nc')
        JavaReceiverInputDStream<String> lines = ssc.receiverStream(
                new JavaCustomReceiver(args[0]));
        //JavaDStream<String> filteredPkts = lines.filter(x->x.contains("8"));
        JavaPairDStream<String, Integer> pktCoutns = lines.mapToPair(s -> new Tuple2<>(s, 1))
                .reduceByKey((i1, i2) -> i1 + i2);
        /*JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
        JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s, 1))
                .reduceByKey((i1, i2) -> i1 + i2);
        wordCounts.print();*/
        pktCoutns.print();
        ssc.start();
        ssc.awaitTermination();
    }

    // ============= Receiver code that receives data over a socket ==============

    String filePath = null;
    byte [] bFile = null;
    String fileString = null;

    public JavaCustomReceiver(String fp) {
        super(StorageLevel.MEMORY_AND_DISK_2());
        filePath = fp;

        FileInputStream fileInputStream = null;
        try {

            File file = new File(filePath);
            bFile = new byte[(int) file.length()];
            //System.out.println("[dbg] the length of the bfile is: " + bFile.length);
            //pktFile = ByteBuffer.allocate(bFile.length);
            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileString = new String(bFile, Charset.forName("UTF-8"));
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onStart() {
        // Start the thread that receives data over a connection
        new Thread(this::receive).start();
    }

    @Override
    public void onStop() {
        // There is nothing much to do as the thread calling receive()
        // is designed to stop by itself isStopped() returns false
    }
    /** Create a socket connection and receive data until receiver is stopped */
    private void receive() {
        /*try {
            *//*Socket socket = null;
            BufferedReader reader = null;*//*
            RandomAccessFile inputFile = new RandomAccessFile(filePath, "r");
            FileChannel inChannel = inputFile.getChannel();
            try {

                ByteBuffer buffer = ByteBuffer.allocate(52);
                // Until stopped or connection broken continue reading
                while(!isStopped() && inChannel.read(buffer) > 0)
                {
                    buffer.flip();
                    *//*store(buffer);*//*
                    byte[] temp = buffer.array();
                    //String str = new String(temp, StandardCharsets.UTF_8);

                    //System.out.println("[dbg] This is what we read: " + asciiToHex(str));
                    String data = new String(temp, Charset.forName("UTF-8"));
                    store(data);
                    buffer.clear(); // do something with the data and clear/compact it.
                }

            } finally {
                inChannel.close();
                inputFile.close();
            }
        } catch(Throwable t) {
            restart("Error receiving data", t);
        }*/
        int counter = 0;
        try {
            while(!isStopped()){
                int beg = counter, end = (counter + 51);
                byte[] temp = ArrayUtils.subarray(bFile, beg,end + 1);
                String data = new String(temp, Charset.forName("UTF-8"));
                //bbtemp = ByteBuffer.wrap(temp);
                //data = fileString.substring(beg, end + 1);
                store(data);
                counter = counter + 52;
                if (counter >= bFile.length) {
                    counter = 0;
                    //System.out.println("[dbg] the length of the bfile is: " + bFile.length );
                }
            }
        } catch (Throwable t){
            restart("Error receiving data!!!", t);
        }

    }
}