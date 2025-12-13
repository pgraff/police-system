FROM confluentinc/cp-kafka-connect:7.6.0

USER root

# Create plugin directories
RUN mkdir -p /kafka/connect/plugins/opensearch-sink/lib

# Copy OpenSearch connector from host directory if it exists
# This allows the connector to be downloaded once on the host and reused
COPY plugins/opensearch-sink/lib/ /kafka/connect/plugins/opensearch-sink/lib/

# Set permissions
RUN chown -R appuser:appuser /kafka/connect/plugins

USER appuser

