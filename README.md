```markdown
# Flink Trade Aggregation Job

Standalone Apache Flink job that consumes trade messages from Kafka `trades` topic, performs windowed aggregation by `(groupId, price, direction)`, and writes results to `trades-aggregated` topic.

## Features

- **Kafka source**: Reads JSON `TradeMessage` from `trades` topic
- **Windowed aggregation**: Sums `volume` per key in configurable tumbling windows
- **Kafka sink**: Writes `AggregatedTrade` JSON to `trades-aggregated`
- **Configurable**: Window size via CLI parameter

## Architecture

```
Kafka (trades)
↓ JSON TradeMessage
Flink Job
↓ keyBy(groupId|price|direction)
↓ tumbling window (N minutes)
↓ aggregate(sum(volume))
Kafka (trades-aggregated)
↓ JSON AggregatedTrade
```

## Quick Start

### 1. Prerequisites

- Java 17+
- Maven
- Kafka at `localhost:9092`
- `trades` topic populated (from Camel producer)

### 2. Build the JAR

```
mvn clean package
```

### 3. Run the job

```
java -cp target/trade-aggregation-job-1.0-SNAPSHOT.jar \
com.example.flink.TradeAggregationJob \
--bootstrapServers localhost:9092 \
--inputTopic trades \
--outputTopic trades-aggregated \
--windowMinutes 1
```

**Parameters**:
- `--windowMinutes 1`: 1-minute tumbling windows (use 15 for production)
- `--inputTopic trades`: Source topic
- `--outputTopic trades-aggregated`: Aggregated results

### 4. Verify output

```
kafka-console-consumer --bootstrap-server localhost:9092 \
--topic trades-aggregated \
--from-beginning
```

Expected output:
```
{"groupId":"GRP1","price":100.5,"direction":"BUY","totalVolume":1500}
```

## Data Models

### Input: `TradeMessage`
```
public class TradeMessage {
String groupId;
double price;
long volume;
String direction;  // "BUY"/"SELL"
}
```

### Output: `AggregatedTrade`
```
public class AggregatedTrade {
String groupId;
double price;
String direction;
long totalVolume;  // sum of volumes in window
}
```

## Keying Logic

Records are grouped by composite key:
```
key = groupId + "|" + price + "|" + direction
Example: "GRP1|100.5|BUY"
```

All trades with same key in same window → summed `totalVolume`.

## Configuration Options

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--bootstrapServers` | `localhost:9092` | Kafka brokers |
| `--inputTopic` | `trades` | Source topic |
| `--outputTopic` | `trades-aggregated` | Sink topic |
| `--windowMinutes` | `15` | Tumbling window size |

## Complete End-to-End Flow

```
1. Camel Producer → POST /trades or trades.json → Kafka(trades)
2. Flink Job → reads trades → aggregates → Kafka(trades-aggregated)
3. Consumer → kafka-console-consumer trades-aggregated
```

## IntelliJ Run Configuration

**Application** config:
- Main class: `com.example.flink.TradeAggregationJob`
- Program arguments: `--bootstrapServers localhost:9092 --inputTopic trades --outputTopic trades-aggregated --windowMinutes 1`
- Use classpath of module: your Flink module


## Troubleshooting

| Issue | Solution |
|-------|----------|
| `NoClassDefFoundError: DataStream` | Remove `<scope>provided</scope>` from Flink deps |
| Jackson `NoSuchMethodError` | Align all Jackson versions to `2.17.2` |
| No output in `trades-aggregated` | Check window size, send multiple same-key records, use `--windowMinutes 1` |
| Array deserialization error | Ensure Camel sends single JSON objects, not arrays |

## Production Notes

- **Event time**: Add timestamps to `TradeMessage` and use `TumblingEventTimeWindows`
- **Checkpoints**: `env.enableCheckpointing(60_000)` for fault tolerance
- **Parallelism**: `env.setParallelism(4)`
- **State backend**: RocksDB for large state

## License

Apache 2.0
```

Save this as `README.md` in your Flink job project/module root. It provides complete setup, usage, and troubleshooting for your trade aggregation job!