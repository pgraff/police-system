# Installing OpenSearch Sink Connector

The connector plugin is now integrated into the Docker Compose setup. You have two options:

## Option 1: Download to Host Directory (Recommended)

The connector JAR can be placed in this directory and will be automatically available via bind mount:

```bash
# Create directory if it doesn't exist
mkdir -p docker/kafka-connect/plugins/opensearch-sink/lib

# Download the connector (replace VERSION with latest)
cd docker/kafka-connect/plugins/opensearch-sink/lib
wget https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v2.3.0.0/opensearch-sink-connector-2.3.0.0.jar

# Restart Kafka Connect (no rebuild needed - uses bind mount)
docker compose restart kafka-connect
```

## Option 2: Build into Image

If you want the connector baked into the image:

```bash
# 1. Download connector to the directory
mkdir -p docker/kafka-connect/plugins/opensearch-sink/lib
cd docker/kafka-connect/plugins/opensearch-sink/lib
wget https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v2.3.0.0/opensearch-sink-connector-2.3.0.0.jar

# 2. Rebuild the image
docker compose build kafka-connect

# 3. Restart
docker compose restart kafka-connect
```

## Quick Installation (Legacy - Manual Download)

1. **Download the connector JAR:**
   ```bash
   # Create the plugin directory
   mkdir -p docker/kafka-connect/plugins/opensearch-sink/lib
   
   # Download the latest release (replace VERSION with latest version number)
   cd docker/kafka-connect/plugins/opensearch-sink/lib
   wget https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v2.3.0.0/opensearch-sink-connector-2.3.0.0.jar
   ```

2. **Or use curl:**
   ```bash
   mkdir -p docker/kafka-connect/plugins/opensearch-sink/lib
   curl -L -o docker/kafka-connect/plugins/opensearch-sink/lib/opensearch-sink-connector-2.3.0.0.jar \
     https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v2.3.0.0/opensearch-sink-connector-2.3.0.0.jar
   ```

3. **Restart Kafka Connect:**
   ```bash
   docker compose restart kafka-connect
   ```

4. **Verify installation:**
   ```bash
   curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
   ```

### Option 2: Direct to Container Volume

If Kafka Connect is already running:

```bash
# Download to a temporary location
wget -O /tmp/opensearch-sink-connector.jar \
  https://github.com/opensearch-project/opensearch-kafka-connect/releases/download/v2.3.0.0/opensearch-sink-connector-2.3.0.0.jar

# Copy to container
docker cp /tmp/opensearch-sink-connector.jar kafka-connect:/kafka/connect/plugins/opensearch-sink/lib/

# Restart to load plugin
docker compose restart kafka-connect
```

## Finding the Latest Version

Check the releases page for the latest version:
https://github.com/opensearch-project/opensearch-kafka-connect/releases

The JAR file name format is: `opensearch-sink-connector-{VERSION}.jar`

## Verification

After installation, verify the plugin is loaded:

```bash
curl http://localhost:8083/connector-plugins | jq '.[] | select(.class | contains("Opensearch"))'
```

You should see:
```json
{
  "class": "org.opensearch.kafka.connect.OpensearchSinkConnector",
  "type": "sink",
  "version": "2.3.0.0"
}
```

## Troubleshooting

If the plugin doesn't appear:

1. **Check the plugin directory:**
   ```bash
   docker exec kafka-connect ls -la /kafka/connect/plugins/opensearch-sink/lib/
   ```

2. **Check Kafka Connect logs:**
   ```bash
   docker logs kafka-connect | grep -i opensearch
   ```

3. **Verify plugin path in docker-compose.yml:**
   The `CONNECT_PLUGIN_PATH` should include `/kafka/connect/plugins`

4. **Restart Kafka Connect:**
   ```bash
   docker compose restart kafka-connect
   ```

