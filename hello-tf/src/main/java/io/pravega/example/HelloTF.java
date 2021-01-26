package io.pravega.example;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;

import io.pravega.connectors.flink.FlinkPravegaReader;
import io.pravega.connectors.flink.PravegaConfig;
import io.pravega.client.stream.Stream;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

public class HelloTF {
    // Logger initialization
    private static Logger log = LoggerFactory.getLogger(HelloTF.class);

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        PravegaAppConfiguration config = new PravegaAppConfiguration(args);

        ParameterTool params = ParameterTool.fromArgs(args);
        String inputStreamName = params.get("input-stream", null);
        Stream inputStream;
        if (inputStreamName != null) {
            inputStream = Stream.of(config.getDefaultScope(), inputStreamName);
        }
        else {
            inputStream = config.getInputStreamConfig().getStream();
        }

        PravegaConfig pravegaConfig = PravegaConfig.fromDefaults()
                .withControllerURI(config.getClientConfig().getControllerURI())
                .withDefaultScope(config.getDefaultScope());
        // create the Pravega source to read a stream of text
        FlinkPravegaReader<String> source = FlinkPravegaReader.<String>builder()
                .withPravegaConfig(pravegaConfig)
                .forStream(inputStream)
                .withDeserializationSchema(new DeserializationSchema<String>() {
                    @Override
                    public String deserialize(byte[] message) throws IOException {
                        return "";
                    }

                    @Override
                    public boolean isEndOfStream(String nextElement) {
                        return false;
                    }

                    @Override
                    public TypeInformation<String> getProducedType() {
                        return TypeInformation.of(String.class);
                    }
                })
                .build();

        DataStream<String> dataStream = env.addSource(source).uid("pravega-reader")
                .setParallelism(1)
                .timeWindowAll(Time.milliseconds(3000))
                .aggregate(new AggregateFunction<String, Integer, String>() {
                    @Override
                    public Integer createAccumulator() {
                        return 0;
                    }

                    @Override
                    public Integer add(String value, Integer acc) {
                        return acc+1;
                    }

                    @Override
                    public String getResult(Integer acc) {
                        TFObject obj = new TFObject();
                        return String.format("%s, %d", obj.run(), acc);
                    }

                    @Override
                    public Integer merge(Integer a, Integer b) {
                        return a+b;
                    }
                });

        dataStream.print();

        env.execute("Hello Tensorflow");
    }

    private static class TFObject implements Serializable, Closeable {
        private Session session;

        TFObject() {
            try {
                final Graph g = new Graph();
                final String value = "Hello from " + TensorFlow.version();
                // Construct the computation graph with a single operation, a constant
                // named "MyConst" with a value "value".
                Tensor t = Tensor.create(value.getBytes("UTF-8"));
                // The Java API doesn't yet include convenience functions for adding operations.
                g.opBuilder("Const", "MyConst").setAttr("dtype", t.dataType()).setAttr("value", t).build();

                session = new Session(g);
            }
            catch (Exception e) {
                session = null;
            }
        }

        String run() {
            if (session != null) {
                // Generally, there may be multiple output tensors,
                // all of them must be closed to prevent resource leaks.
                try {
                    Tensor output = session.runner().fetch("MyConst").run().get(0);
                    return new String(output.bytesValue(), "UTF-8");
                }
                catch (Exception e) {
                }

            }
            return "";
        }

        @Override
        public void close() {
            if (session != null) {
                session.close();
            }
        }
    }
}
