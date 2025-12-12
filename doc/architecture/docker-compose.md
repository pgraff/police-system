# Docker Compose Infrastructure

This document describes the infrastructure services provided by the `docker-compose.yml` file.

## Overview

The docker-compose setup provides all necessary infrastructure services for the Police System:
- **Event Buses**: Kafka (3 brokers) and NATS (3-node cluster)
- **Search Engine**: Elasticsearch for event indexing and search
- **Event Indexing**: Kafka Connect for Elasticsearch indexing
- **Databases**: PostgreSQL, MongoDB, InfluxDB
- **Admin UIs**: Kafka UI and NATS Tower
- **Development Tools**: pgAdmin, MongoDB Express

## Services

### Kafka Cluster

**3-Broker Kafka Cluster** (KRaft mode - no Zookeeper)

- **kafka-broker-1**: Port 9092
- **kafka-broker-2**: Port 9092 (internal)
- **kafka-broker-3**: Port 9092 (internal)
- **Replication Factor**: 3
- **Minimum In-Sync Replicas**: 2

**Kafka UI**: http://localhost:8080
- Web-based management interface
- View topics, consumers, messages
- Monitor broker health

### NATS Cluster

**3-Node NATS Cluster with JetStream**

- **nats-1**: 
  - Client: `localhost:4222`
  - Monitoring: `localhost:8222`
  - Cluster: Internal port 6222
- **nats-2**: 
  - Client: `localhost:4223`
  - Monitoring: `localhost:8223`
  - Cluster: Internal port 6222
- **nats-3**: 
  - Client: `localhost:4224`
  - Monitoring: `localhost:8224`
  - Cluster: Internal port 6222

**Cluster Configuration:**
- Cluster Name: `policesystem-nats-cluster`
- JetStream: Enabled
- High Availability: Automatic failover

**NATS Tower**: http://localhost:8099
- Web-based management interface
- Monitor cluster health
- View streams and subjects
- Manage connections

**Monitoring Endpoints:**
- Health: `http://localhost:8222/healthz`
- Server Info: `http://localhost:8222/varz`
- JetStream Info: `http://localhost:8222/jsz`
- Connections: `http://localhost:8222/connz`

### Kafka Connect

**Kafka Connect** (Port 8083)
- Distributed mode for high availability
- REST API: http://localhost:8083
- Connects to all 3 Kafka brokers
- Used for Elasticsearch indexing via OpenSearch Sink Connector

**Connectors**: 14 connectors, one per event topic
- Indexes events to Elasticsearch with domain-specific indices
- Dead letter topics for error handling
- See [Kafka Connect Elasticsearch](kafka-connect-elasticsearch.md) for details

### Databases

**PostgreSQL** (Port 5432)
- Database: `policesystem`
- User: `policesystem`
- Password: `policesystem`
- **pgAdmin**: http://localhost:5050
  - Email: `admin@example.com`
  - Password: `admin`

**MongoDB** (Port 27017)
- Database: `policesystem`
- User: `policesystem`
- Password: `policesystem`
- **MongoDB Express**: http://localhost:8081
  - Username: `admin`
  - Password: `admin`

**InfluxDB** (Port 8086)
- Organization: `policesystem`
- Bucket: `policesystem`
- User: `policesystem`
- Password: `policesystem`
- Admin Token: `policesystem-admin-token`

**Elasticsearch** (Port 9200)
- Single-node cluster (development mode)
- Security: Disabled (development only)
- Memory: 512MB heap
- Used for: Event indexing and search via Kafka Connect
- Health: `http://localhost:9200/_cluster/health`

## Starting Services

### Start All Services

```bash
docker compose up -d
```

### Start Specific Services

```bash
# Start only Kafka
docker compose up -d kafka-broker-1 kafka-broker-2 kafka-broker-3

# Start only NATS
docker compose up -d nats-1 nats-2 nats-3

# Start databases
docker compose up -d postgres mongodb influxdb
```

### Stop Services

```bash
# Stop all services
docker compose down

# Stop specific services
docker compose stop nats-1 nats-2 nats-3
```

## Health Checks

### Check Service Status

```bash
docker compose ps
```

### Check Kafka Health

```bash
# Check if brokers are running
docker compose ps kafka-broker-1 kafka-broker-2 kafka-broker-3

# View Kafka UI
open http://localhost:8080
```

### Check NATS Health

```bash
# Check if NATS servers are running
docker compose ps nats-1 nats-2 nats-3

# Check cluster status
curl http://localhost:8222/jsz | python3 -m json.tool

# Check individual server health
curl http://localhost:8222/healthz
curl http://localhost:8223/healthz
curl http://localhost:8224/healthz

# View NATS Tower
open http://localhost:8099
```

## Testing High Availability

### Test NATS Cluster Failover

```bash
# 1. Verify all servers are running
docker compose ps nats-1 nats-2 nats-3

# 2. Check cluster status
curl -s http://localhost:8222/jsz | python3 -c \
  "import sys, json; d=json.load(sys.stdin); \
   print(f\"Cluster Size: {d['meta_cluster']['cluster_size']}\"); \
   print(f\"Active Replicas: {len([r for r in d['meta_cluster']['replicas'] if r['current']])}\")"

# 3. Stop one server
docker compose stop nats-2

# 4. Verify cluster continues operating
curl -s http://localhost:8222/jsz | python3 -c \
  "import sys, json; d=json.load(sys.stdin); \
   print(f\"Cluster Size: {d['meta_cluster']['cluster_size']}\"); \
   print(f\"Active Replicas: {len([r for r in d['meta_cluster']['replicas'] if r['current']])}\")"

# 5. Restart the server
docker compose start nats-2

# 6. Wait a few seconds and verify it rejoined
sleep 5
curl -s http://localhost:8222/jsz | python3 -c \
  "import sys, json; d=json.load(sys.stdin); \
   print(f\"Cluster Size: {d['meta_cluster']['cluster_size']}\"); \
   print(f\"Active Replicas: {len([r for r in d['meta_cluster']['replicas'] if r['current']])}\")"
```

### Test Kafka Cluster

```bash
# Check broker status
docker compose ps kafka-broker-1 kafka-broker-2 kafka-broker-3

# Stop one broker
docker compose stop kafka-broker-2

# Verify cluster continues (should still have 2 brokers)
# Restart
docker compose start kafka-broker-2
```

## Viewing Logs

### View All Logs

```bash
docker compose logs -f
```

### View Specific Service Logs

```bash
# NATS logs
docker compose logs -f nats-1
docker compose logs -f nats-2
docker compose logs -f nats-3

# Kafka logs
docker compose logs -f kafka-broker-1

# NATS Tower logs
docker compose logs -f nats-tower
```

## Data Persistence

All services use Docker volumes for data persistence:

- `kafka-broker-1-data`, `kafka-broker-2-data`, `kafka-broker-3-data`: Kafka data
- `nats-1-data`, `nats-2-data`, `nats-3-data`: NATS JetStream data
- `postgres-data`: PostgreSQL data
- `mongodb-data`, `mongodb-config`: MongoDB data
- `influxdb-data`, `influxdb-config`: InfluxDB data
- `pgadmin-data`: pgAdmin data

### Backup Data

```bash
# Backup volumes (example for PostgreSQL)
docker run --rm -v policesystem_postgres-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres-backup.tar.gz /data
```

### Remove All Data

```bash
# Stop and remove containers and volumes
docker compose down -v
```

## Network

All services are connected to the `policesystem-network` bridge network, allowing them to communicate using service names:

- Kafka: `kafka-broker-1:9092`, `kafka-broker-2:9092`, `kafka-broker-3:9092`
- NATS: `nats-1:4222`, `nats-2:4222`, `nats-3:4222`
- PostgreSQL: `postgres:5432`
- MongoDB: `mongodb:27017`
- InfluxDB: `influxdb:8086`

## Configuration

### Application Configuration

The application connects to services using the following configuration:

**Kafka:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

**NATS:**
```yaml
nats:
  url: nats://localhost:4222,nats://localhost:4223,nats://localhost:4224
  enabled: true
```

**PostgreSQL:**
```
jdbc:postgresql://localhost:5432/policesystem
```

**MongoDB:**
```
mongodb://policesystem:policesystem@localhost:27017/policesystem
```

## Troubleshooting

### Services Won't Start

1. Check if ports are already in use:
   ```bash
   # Check for port conflicts
   netstat -tuln | grep -E '4222|4223|4224|9092|5432|27017|8080|8099'
   ```

2. Check Docker logs:
   ```bash
   docker compose logs <service-name>
   ```

3. Verify Docker has enough resources:
   ```bash
   docker system df
   ```

### NATS Cluster Issues

1. Check cluster status:
   ```bash
   curl http://localhost:8222/jsz
   ```

2. Check if all servers can see each other:
   ```bash
   docker compose exec nats-1 wget -qO- http://localhost:8222/varz | grep cluster
   ```

3. Restart the cluster:
   ```bash
   docker compose restart nats-1 nats-2 nats-3
   ```

### Kafka Issues

1. Check broker logs:
   ```bash
   docker compose logs kafka-broker-1
   ```

2. Verify brokers can communicate:
   ```bash
   docker compose exec kafka-broker-1 kafka-broker-api-versions \
     --bootstrap-server localhost:9092
   ```

## Production Considerations

For production deployments:

1. **Security**: Configure authentication and encryption
2. **Resource Limits**: Set appropriate CPU and memory limits
3. **Monitoring**: Set up comprehensive monitoring and alerting
4. **Backup**: Implement regular backup strategies
5. **High Availability**: Ensure proper replication and failover
6. **Network**: Use dedicated networks and proper firewall rules
7. **Secrets**: Use Docker secrets or external secret management
8. **Scaling**: Plan for horizontal scaling of services
