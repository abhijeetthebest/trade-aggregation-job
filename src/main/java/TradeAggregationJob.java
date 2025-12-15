// TradeAggregationJob.java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import models.AggregatedTrade;
import models.TradeMessage;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;

public class TradeAggregationJob {

    public static void main(String[] args) throws Exception {
        // Read CLI params, e.g. --bootstrapServers localhost:9092 --inputTopic trades --outputTopic trades-agg --windowMinutes 15
        ParameterTool params = ParameterTool.fromArgs(args);

        String bootstrapServers = params.get("bootstrapServers", "localhost:9092");
        String inputTopic      = params.get("inputTopic", "trades-new");
        String outputTopic     = params.get("outputTopic", "trades-aggregated");
        int windowSeconds      = params.getInt("windowMinutes", 20);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);

        ObjectMapper mapper = new ObjectMapper();

        // ---- Kafka source: JSON string messages ----
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(inputTopic)
                .setGroupId("trade-flink-consumer-2")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> raw = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(), // processing time windows
                "kafka-trades-source"
        );
        System.out.println("Flink received: " + raw);
        // ---- Parse JSON to TradeMessage ----
        DataStream<TradeMessage> trades = raw.map(json -> mapper.readValue(json, TradeMessage.class));

        // ---- Key by (groupId, price, direction) and aggregate volume over tumbling window ----
        DataStream<AggregatedTrade> aggregated = trades
                .keyBy(t -> t.getGroupId() + "|" + t.getPrice() + "|" + t.getDirection())
                .window(TumblingProcessingTimeWindows.of(Time.seconds(windowSeconds)))
                .reduce(
                        // Reduce function: sum volume
                        (t1, t2) -> {
                            long totalVol = t1.getVolume() + t2.getVolume();
                            return new TradeMessage(
                                    t1.getGroupId(),
                                    t1.getPrice(),
                                    totalVol,
                                    t1.getDirection()
                            );
                        }
                )
                // Map reduced TradeMessage to AggregatedTrade
                .map(t -> new AggregatedTrade(
                        t.getGroupId(),
                        t.getPrice(),
                        t.getDirection(),
                        t.getVolume()
                ));

        // ---- Kafka sink: write aggregated JSON to output topic ----
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic(outputTopic)
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .build();

        aggregated
                .map(agg -> mapper.writeValueAsString(agg))
                .sinkTo(sink)
                .name("kafka-trades-agg-sink");

        env.execute("Trade Volume Aggregation Job");
        System.out.println(("Flink job ran successsfully"));
    }
}
