#!/bin/bash

# Police System Demo Scenario Script
# This script demonstrates a realistic police incident management scenario:
# 1. Sets up initial data (officers, vehicles, units, locations, persons)
# 2. Runs through a complete incident workflow
# 3. Queries projections to verify data
# 4. Documents what was created
#
# Usage:
#   ./scripts/demo-scenario.sh
#
# Environment Variables:
#   EDGE_BASE_URL - Base URL for edge service (default: http://localhost:8080/api/v1)
#   OPERATIONAL_PROJECTION_URL - Operational projection URL (default: http://localhost:8081/api/projections)
#   RESOURCE_PROJECTION_URL - Resource projection URL (default: http://localhost:8082/api/projections)
#   WORKFORCE_PROJECTION_URL - Workforce projection URL (default: http://localhost:8083/api/projections)
#
# Prerequisites:
#   - Docker and docker-compose installed
#   - jq installed (optional, for better JSON formatting)
#   - curl installed
#
# Environment Variables:
#   START_SERVICES - Set to "true" to start docker-compose services (default: true)
#   STOP_SERVICES - Set to "true" to stop services after demo (default: false)
#   DOCKER_COMPOSE_FILE - Path to docker-compose file (default: docker-compose-integration.yml)
#
# Example:
#   EDGE_BASE_URL=http://localhost:8080/api/v1 ./scripts/demo-scenario.sh
#
# The script will:
#   - Create officers, vehicles, units, locations, and persons
#   - Start a shift and check in officers
#   - Receive a call and report an incident
#   - Dispatch units and create assignments
#   - Create activities and involve parties
#   - Complete activities and clear the incident
#   - Query projections to verify data
#   - Store all created resource IDs in /tmp/police-demo-data.json
#   - Log all output to /tmp/police-demo-YYYYMMDD-HHMMSS.log

# Don't exit on error - we want to see the full scenario even if some steps fail
set +e

# Configuration
EDGE_BASE_URL="${EDGE_BASE_URL:-http://localhost:8080/api/v1}"
OPERATIONAL_PROJECTION_URL="${OPERATIONAL_PROJECTION_URL:-http://localhost:8081/api/projections}"
RESOURCE_PROJECTION_URL="${RESOURCE_PROJECTION_URL:-http://localhost:8082/api/projections}"
WORKFORCE_PROJECTION_URL="${WORKFORCE_PROJECTION_URL:-http://localhost:8083/api/projections}"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FILE:-docker-compose-integration.yml}"
START_SERVICES="${START_SERVICES:-true}"
STOP_SERVICES="${STOP_SERVICES:-false}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Storage for created resource IDs
DEMO_DATA_FILE="/tmp/police-demo-data.json"
DEMO_LOG_FILE="/tmp/police-demo-$(date +%Y%m%d-%H%M%S).log"

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

print_section() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

# Function to make API calls and extract response data
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local description="$4"
    local continue_on_error="${5:-false}"  # Optional: continue even on error
    
    print_info "$description"
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            "$EDGE_BASE_URL$endpoint" 2>/dev/null || echo "ERROR\n000")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$EDGE_BASE_URL$endpoint" 2>/dev/null || echo "ERROR\n000")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        print_success "HTTP $http_code - $description"
        echo "$body"
        return 0
    else
        print_error "HTTP $http_code - $description"
        echo "$body" >&2
        if [ "$continue_on_error" = "true" ]; then
            print_warning "Continuing despite error..."
            return 1
        else
            return 1
        fi
    fi
}

# Function to extract JSON field value
extract_field() {
    local json="$1"
    local field="$2"
    echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | cut -d'"' -f4 || echo ""
}

# Function to extract nested field (e.g., data.badgeNumber)
extract_nested_field() {
    local json="$1"
    local field="$2"
    echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | cut -d'"' -f4 || echo ""
}

# Function to wait a bit for eventual consistency
wait_for_projection() {
    print_info "Waiting 2 seconds for event processing..."
    sleep 2
}

# Function to query projection
query_projection() {
    local projection_url="$1"
    local endpoint="$2"
    local description="$3"
    
    print_info "$description"
    response=$(curl -s -w "\n%{http_code}" "$projection_url$endpoint" 2>/dev/null || echo "ERROR\n000")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        print_success "HTTP $http_code - $description"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        print_warning "HTTP $http_code - $description (may not be available yet)"
    fi
}

# Initialize demo data storage
init_demo_data() {
    echo "{}" > "$DEMO_DATA_FILE"
}

# Store demo data
store_data() {
    local key="$1"
    local value="$2"
    local temp_file=$(mktemp)
    jq --arg key "$key" --arg value "$value" '. + {($key): $value}' "$DEMO_DATA_FILE" > "$temp_file"
    mv "$temp_file" "$DEMO_DATA_FILE"
}

# Get stored data
get_data() {
    local key="$1"
    jq -r ".[\"$key\"] // empty" "$DEMO_DATA_FILE"
}

# Function to start docker-compose services
start_services() {
    print_section "Starting Docker Services"
    
    if [ "$START_SERVICES" != "true" ]; then
        print_info "Skipping service startup (START_SERVICES=false)"
        print_info "Assuming services are already running..."
        return 0
    fi
    
    if ! command -v docker-compose &> /dev/null && ! command -v docker &> /dev/null; then
        print_error "Docker or docker-compose is not installed. Please install Docker to run this script."
        exit 1
    fi
    
    # Check if docker-compose or docker compose (v2) is available
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        print_error "Neither 'docker-compose' nor 'docker compose' is available."
        exit 1
    fi
    
    print_info "Using: $DOCKER_COMPOSE_CMD"
    print_info "Compose file: $DOCKER_COMPOSE_FILE"
    
    # Check if services are already running
    if $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" ps | grep -q "Up"; then
        print_warning "Some services are already running. Stopping them first..."
        $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" down
    fi
    
    # Remove any existing containers with conflicting names
    print_info "Cleaning up any existing containers..."
    docker rm -f postgres kafka-broker-1 kafka-broker-2 kafka-broker-3 nats-1 nats-2 nats-3 \
        edge-service operational-projection resource-projection workforce-projection 2>/dev/null || true
    
    print_info "Starting all services (this may take a few minutes)..."
    $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" up -d
    
    if [ $? -ne 0 ]; then
        print_error "Failed to start docker-compose services"
        exit 1
    fi
    
    print_info "Waiting for services to be healthy..."
    
    # Wait for infrastructure services
    print_info "Waiting for PostgreSQL..."
    timeout=60
    elapsed=0
    while ! $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" exec -T postgres pg_isready -U policesystem &> /dev/null; do
        sleep 2
        elapsed=$((elapsed + 2))
        if [ $elapsed -ge $timeout ]; then
            print_error "PostgreSQL did not become ready within $timeout seconds"
            exit 1
        fi
    done
    print_success "PostgreSQL is ready"
    
    # Wait for Kafka (Kafka can take longer to start, especially in KRaft mode)
    print_info "Waiting for Kafka (this may take up to 2 minutes)..."
    elapsed=0
    timeout=120  # Increase timeout for Kafka
    # Use the container hostname instead of localhost for the health check
    while ! $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" exec -T kafka-broker-1 kafka-broker-api-versions --bootstrap-server kafka-broker-1:9092 &> /dev/null 2>&1; do
        sleep 3
        elapsed=$((elapsed + 3))
        if [ $((elapsed % 15)) -eq 0 ]; then
            print_info "Still waiting for Kafka... (${elapsed}s elapsed)"
            # Show a brief status
            $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" ps kafka-broker-1 2>&1 | grep -q "Up" && print_info "Kafka container is running, waiting for API..." || print_warning "Kafka container may not be running"
        fi
        if [ $elapsed -ge $timeout ]; then
            print_error "Kafka did not become ready within $timeout seconds"
            print_info "Checking Kafka container status and logs..."
            $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" ps kafka-broker-1
            print_info "Last 30 lines of Kafka logs:"
            $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" logs --tail=30 kafka-broker-1
            print_warning "You can check the full logs with: docker compose -f $DOCKER_COMPOSE_FILE logs kafka-broker-1"
            exit 1
        fi
    done
    print_success "Kafka is ready"
    
    # Wait for NATS
    print_info "Waiting for NATS..."
    elapsed=0
    while ! curl -f http://localhost:8222/healthz &> /dev/null; do
        sleep 2
        elapsed=$((elapsed + 2))
        if [ $elapsed -ge $timeout ]; then
            print_error "NATS did not become ready within $timeout seconds"
            exit 1
        fi
    done
    print_success "NATS is ready"
    
    # Wait for application services
    print_info "Waiting for application services to start..."
    sleep 10
    
    # Wait for edge service (use the correct health endpoint)
    print_info "Waiting for Edge Service..."
    elapsed=0
    while ! curl -f http://localhost:8080/api/v1/health &> /dev/null; do
        sleep 3
        elapsed=$((elapsed + 3))
        if [ $((elapsed % 15)) -eq 0 ]; then
            print_info "Still waiting for Edge Service... (${elapsed}s elapsed)"
            # Show container status
            $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" ps edge-service 2>&1 | grep -q "Up" && print_info "Edge container is running..." || print_warning "Edge container may not be running"
        fi
        if [ $elapsed -ge 120 ]; then
            print_error "Edge Service did not become ready within 120 seconds"
            print_info "Checking Edge Service container status and logs..."
            $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" ps edge-service
            print_info "Last 30 lines of Edge Service logs:"
            $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" logs --tail=30 edge-service
            print_warning "Attempting to continue anyway - the service may still work for API calls"
            break
        fi
    done
    if [ $elapsed -lt 120 ]; then
        print_success "Edge Service is ready"
    fi
    
    # Wait for projections (with more lenient checks)
    print_info "Waiting for Projection Services..."
    for port in 8081 8082 8083; do
        elapsed=0
        projection_ready=false
        while [ $elapsed -lt 120 ]; do
            # Try health endpoint
            if curl -f http://localhost:$port/actuator/health &> /dev/null; then
                projection_ready=true
                break
            fi
            # If health fails, check if service responds at all
            if curl -s http://localhost:$port/actuator/info &> /dev/null || curl -s http://localhost:$port/api/projections &> /dev/null; then
                print_warning "Projection on port $port is responding but health endpoint may have issues - continuing"
                projection_ready=true
                break
            fi
            sleep 3
            elapsed=$((elapsed + 3))
        done
        if [ "$projection_ready" = true ]; then
            print_success "Projection on port $port is ready"
        else
            print_warning "Projection on port $port did not become ready within 120 seconds - may still work"
        fi
    done
    
    print_success "Service startup complete!"
    print_info "Note: Some services may show warnings but the demo will attempt to continue."
    echo ""
}

# Function to stop docker-compose services
stop_services() {
    if [ "$STOP_SERVICES" != "true" ]; then
        return 0
    fi
    
    print_section "Stopping Docker Services"
    
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        print_warning "Docker compose not available, skipping service shutdown"
        return 0
    fi
    
    print_info "Stopping services..."
    $DOCKER_COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" down
    
    if [ $? -eq 0 ]; then
        print_success "Services stopped successfully"
    else
        print_warning "Some services may not have stopped cleanly"
    fi
}

# Cleanup function
cleanup() {
    print_section "Demo Data Summary"
    if [ -f "$DEMO_DATA_FILE" ]; then
        print_info "Created resources (stored in $DEMO_DATA_FILE):"
        cat "$DEMO_DATA_FILE" | jq '.' 2>/dev/null || cat "$DEMO_DATA_FILE"
        echo ""
        print_warning "Note: In an event-driven system, resources cannot be deleted via API."
        print_warning "The above resources were created for demonstration purposes."
        print_info "To clean up, you would need to manually remove data from the database or reset the system."
    else
        print_warning "No demo data file found. The demo may not have completed successfully."
    fi
    
    print_info "Complete demo execution log: $DEMO_LOG_FILE"
    echo ""
    
    # Stop services if requested
    stop_services
}

# Trap to ensure cleanup runs
trap cleanup EXIT

# Main execution
main() {
    # Log all output to file
    exec > >(tee -a "$DEMO_LOG_FILE") 2>&1
    
    print_section "Police System Demo Scenario"
    print_info "Edge Service: $EDGE_BASE_URL"
    print_info "Operational Projection: $OPERATIONAL_PROJECTION_URL"
    print_info "Resource Projection: $RESOURCE_PROJECTION_URL"
    print_info "Workforce Projection: $WORKFORCE_PROJECTION_URL"
    print_info "Demo log file: $DEMO_LOG_FILE"
    echo ""
    
    # Start services if requested
    start_services
    
    init_demo_data
    
    # ============================================================================
    # PHASE 1: SETUP - Create Initial Resources
    # ============================================================================
    print_section "PHASE 1: Setup - Creating Initial Resources"
    
    # Create Officers (use timestamp to avoid conflicts from previous runs)
    print_info "Creating officers..."
    TIMESTAMP=$(date +%s)
    
    officer1_response=$(api_call "POST" "/officers" \
        '{
            "badgeNumber": "BADGE-'$TIMESTAMP'-001",
            "firstName": "John",
            "lastName": "Smith",
            "rank": "Sergeant",
            "email": "john.smith'$TIMESTAMP'@police.gov",
            "phoneNumber": "+1-555-0101",
            "hireDate": "2020-01-15",
            "status": "Active"
        }' \
        "Register Officer 1 (John Smith)")
    
    officer1_badge=$(extract_nested_field "$officer1_response" "badgeNumber" || echo "BADGE-$TIMESTAMP-001")
    store_data "officer1_badge" "$officer1_badge"
    
    officer2_response=$(api_call "POST" "/officers" \
        '{
            "badgeNumber": "BADGE-'$TIMESTAMP'-002",
            "firstName": "Jane",
            "lastName": "Doe",
            "rank": "Officer",
            "email": "jane.doe'$TIMESTAMP'@police.gov",
            "phoneNumber": "+1-555-0102",
            "hireDate": "2021-03-20",
            "status": "Active"
        }' \
        "Register Officer 2 (Jane Doe)")
    
    officer2_badge=$(extract_nested_field "$officer2_response" "badgeNumber" || echo "BADGE-$TIMESTAMP-002")
    store_data "officer2_badge" "$officer2_badge"
    
    # Create Units (must be created before vehicles, as vehicles require unitId)
    print_info "Creating units..."
    
    unit1_response=$(api_call "POST" "/units" \
        '{
            "unitId": "UNIT-'$TIMESTAMP'-001",
            "unitNumber": "U-'$TIMESTAMP'-001",
            "unitType": "Single",
            "status": "Available"
        }' \
        "Create Unit 1 (Patrol Unit)")
    
    unit1_id=$(extract_nested_field "$unit1_response" "unitId" || echo "UNIT-$TIMESTAMP-001")
    store_data "unit1_id" "$unit1_id"
    
    # Create Vehicles (requires unitId from unit created above)
    print_info "Creating vehicles..."
    
    vehicle1_response=$(api_call "POST" "/vehicles" \
        '{
            "unitId": "'"$unit1_id"'",
            "vehicleType": "Patrol",
            "licensePlate": "POL-001",
            "vin": "1HGBH41JXMN109186",
            "status": "Available"
        }' \
        "Register Vehicle 1 (Patrol Car)")
    
    # Vehicle ID is the same as unit ID (vehicle is identified by unitId)
    # Extract from response, fallback to unit1_id we already have
    vehicle1_id=$(extract_nested_field "$vehicle1_response" "vehicleId" || extract_nested_field "$vehicle1_response" "unitId" || echo "$unit1_id")
    store_data "vehicle1_id" "$vehicle1_id"
    
    # Create Location
    print_info "Creating location..."
    
    location1_response=$(api_call "POST" "/locations" \
        '{
            "locationId": "LOC-'$TIMESTAMP'-001",
            "address": "123 Main Street",
            "city": "Springfield",
            "state": "IL",
            "zipCode": "62701",
            "locationType": "Street",
            "latitude": 39.7817,
            "longitude": -89.6501
        }' \
        "Create Location (123 Main Street)")
    
    location1_id=$(extract_nested_field "$location1_response" "locationId" || echo "LOC-$TIMESTAMP-001")
    store_data "location1_id" "$location1_id"
    
    # Create Person
    print_info "Creating person..."
    
    person1_response=$(api_call "POST" "/persons" \
        '{
            "personId": "PERSON-'$TIMESTAMP'-001",
            "firstName": "Bob",
            "lastName": "Johnson",
            "dateOfBirth": "1985-05-15",
            "gender": "Male",
            "race": "White"
        }' \
        "Register Person (Bob Johnson)")
    
    person1_id=$(extract_nested_field "$person1_response" "personId" || echo "PERSON-$TIMESTAMP-001")
    store_data "person1_id" "$person1_id"
    
    wait_for_projection
    
    # ============================================================================
    # PHASE 2: SHIFT MANAGEMENT
    # ============================================================================
    print_section "PHASE 2: Shift Management"
    
    # Start Shift
    shift1_response=$(api_call "POST" "/shifts" \
        '{
            "shiftId": "SHIFT-'$TIMESTAMP'-001",
            "shiftType": "Day",
            "startTime": "2024-01-15T08:00:00.000+00:00",
            "status": "Started"
        }' \
        "Start Shift (Day Shift)")
    
    shift1_id=$(extract_nested_field "$shift1_response" "shiftId" || echo "SHIFT-$TIMESTAMP-001")
    store_data "shift1_id" "$shift1_id"
    
    # Check in officers to shift
    api_call "POST" "/shifts/$shift1_id/officers/$officer1_badge/check-in" \
        '{
            "checkInTime": "2024-01-15T08:00:00.000+00:00",
            "shiftRoleType": "Regular"
        }' \
        "Check in Officer 1 to shift"
    
    api_call "POST" "/shifts/$shift1_id/officers/$officer2_badge/check-in" \
        '{
            "checkInTime": "2024-01-15T08:00:00.000+00:00",
            "shiftRoleType": "Regular"
        }' \
        "Check in Officer 2 to shift"
    
    wait_for_projection
    
    # ============================================================================
    # PHASE 3: INCIDENT WORKFLOW
    # ============================================================================
    print_section "PHASE 3: Incident Workflow"
    
    # Receive a Call
    call1_response=$(api_call "POST" "/calls" \
        '{
            "callId": "CALL-'$TIMESTAMP'-001",
            "callNumber": "C-2024-'$TIMESTAMP'-001",
            "callType": "Emergency",
            "priority": "High",
            "status": "Received",
            "receivedTime": "2024-01-15T10:30:00.000+00:00",
            "callerName": "Bob Johnson",
            "callerPhone": "+1-555-9999",
            "description": "Report of suspicious activity at 123 Main Street"
        }' \
        "Receive Call (Suspicious Activity)")
    
    call1_id=$(extract_nested_field "$call1_response" "callId" || echo "CALL-$TIMESTAMP-001")
    store_data "call1_id" "$call1_id"
    
    # Report Incident
    incident1_response=$(api_call "POST" "/incidents" \
        '{
            "incidentId": "INC-'$TIMESTAMP'-001",
            "incidentNumber": "I-2024-'$TIMESTAMP'-001",
            "priority": "High",
            "status": "Reported",
            "reportedTime": "2024-01-15T10:35:00.000+00:00",
            "description": "Suspicious activity reported at 123 Main Street. Possible burglary in progress.",
            "incidentType": "Burglary"
        }' \
        "Report Incident (Burglary)")
    
    incident1_id=$(extract_nested_field "$incident1_response" "incidentId" || echo "INC-$TIMESTAMP-001")
    store_data "incident1_id" "$incident1_id"
    
    # Link call to incident
    api_call "POST" "/calls/$call1_id/incidents" \
        '{
            "incidentId": "'"$incident1_id"'"
        }' \
        "Link Call to Incident"
    
    # Link location to incident (use LocationController endpoint)
    api_call "POST" "/incidents/$incident1_id/locations" \
        '{
            "locationId": "'"$location1_id"'",
            "locationRoleType": "Primary"
        }' \
        "Link Location to Incident"
    
    wait_for_projection
    
    # Dispatch Incident
    dispatch1_response=$(api_call "POST" "/incidents/$incident1_id/dispatch" \
        '{
            "dispatchedTime": "2024-01-15T10:40:00.000+00:00"
        }' \
        "Dispatch Unit to Incident")
    
    # Extract dispatch ID from response (it's generated by the system)
    dispatch1_id=$(extract_nested_field "$dispatch1_response" "dispatchId" || echo "")
    if [ -z "$dispatch1_id" ]; then
        # If dispatch ID not in response, we'll need to query for it later
        print_info "Dispatch ID not in response, will query later"
    fi
    store_data "dispatch1_id" "$dispatch1_id"
    
    wait_for_projection
    
    # Create Assignment (with incidentId, then link to dispatch separately)
    assignment1_response=$(api_call "POST" "/assignments" \
        '{
            "assignmentId": "ASSIGN-'$TIMESTAMP'-001",
            "assignmentType": "Primary",
            "status": "Assigned",
            "assignedTime": "2024-01-15T10:40:00.000+00:00",
            "incidentId": "'"$incident1_id"'"
        }' \
        "Create Assignment (Officer 1 to Incident)")
    
    assignment1_id=$(extract_nested_field "$assignment1_response" "assignmentId" || echo "ASSIGN-$TIMESTAMP-001")
    store_data "assignment1_id" "$assignment1_id"
    
    # Link assignment to dispatch (if we have dispatch ID)
    if [ -n "$dispatch1_id" ]; then
        api_call "POST" "/assignments/$assignment1_id/dispatches" \
            '{
                "dispatchId": "'"$dispatch1_id"'"
            }' \
            "Link Assignment to Dispatch"
    fi
    
    # Assign resource (vehicle) to assignment
    api_call "POST" "/assignments/$assignment1_id/resources" \
        '{
            "resourceId": "'"$vehicle1_id"'",
            "resourceType": "Vehicle",
            "roleType": "Primary",
            "status": "Assigned"
        }' \
        "Assign Vehicle to Assignment"
    
    wait_for_projection
    
    # Arrive at Incident
    api_call "POST" "/incidents/$incident1_id/arrive" \
        '{
            "arrivedTime": "2024-01-15T10:45:00.000+00:00",
            "unitId": "'"$unit1_id"'"
        }' \
        "Arrive at Incident"
    
    # Update dispatch status (use valid DispatchStatus enum value)
    if [ -n "$dispatch1_id" ]; then
        api_call "PATCH" "/dispatches/$dispatch1_id/status" \
            '{
                "status": "Acknowledged"
            }' \
            "Update Dispatch Status to Acknowledged"
    fi
    
    wait_for_projection
    
    # Start Activity
    activity1_response=$(api_call "POST" "/activities" \
        '{
            "activityId": "ACT-'$TIMESTAMP'-001",
            "activityType": "Report",
            "status": "InProgress",
            "activityTime": "2024-01-15T10:46:00.000+00:00",
            "description": "Investigating suspicious activity",
            "incidentId": "'"$incident1_id"'",
            "badgeNumber": "'"$officer1_badge"'"
        }' \
        "Start Activity (Report)")
    
    activity1_id=$(extract_nested_field "$activity1_response" "activityId" || echo "ACT-$TIMESTAMP-001")
    store_data "activity1_id" "$activity1_id"
    
    wait_for_projection
    
    # Involve Party (involvementId is generated by the API)
    involved_party1_response=$(api_call "POST" "/involved-parties" \
        '{
            "personId": "'"$person1_id"'",
            "incidentId": "'"$incident1_id"'",
            "partyRoleType": "Witness",
            "involvementStartTime": "2024-01-15T10:50:00.000+00:00"
        }' \
        "Involve Party (Witness)")
    
    involved_party1_id=$(extract_nested_field "$involved_party1_response" "involvementId" || echo "INV-$TIMESTAMP-001")
    store_data "involved_party1_id" "$involved_party1_id"
    
    wait_for_projection
    
    # Complete Activity
    api_call "POST" "/activities/$activity1_id/complete" \
        '{
            "completedTime": "2024-01-15T11:00:00.000+00:00",
            "notes": "Investigation complete. No evidence of burglary found."
        }' \
        "Complete Activity"
    
    # Complete Assignment
    api_call "POST" "/assignments/$assignment1_id/complete" \
        '{
            "completedTime": "2024-01-15T11:01:00.000+00:00"
        }' \
        "Complete Assignment"
    
    wait_for_projection
    
    # Clear Incident
    api_call "POST" "/incidents/$incident1_id/clear" \
        '{
            "clearedTime": "2024-01-15T11:05:00.000+00:00",
            "clearedBy": "'"$officer1_badge"'",
            "notes": "Incident cleared. False alarm."
        }' \
        "Clear Incident"
    
    # Update call status
    api_call "PATCH" "/calls/$call1_id/status" \
        '{
            "status": "Cleared"
        }' \
        "Update Call Status to Cleared"
    
    wait_for_projection
    
    # ============================================================================
    # PHASE 4: QUERY PROJECTIONS - Verify Data
    # ============================================================================
    print_section "PHASE 4: Query Projections - Verifying Data"
    
    print_info "Querying projections to verify data was processed..."
    
    # Query Resource Projection - Officers
    query_projection "$RESOURCE_PROJECTION_URL" "/officers/$officer1_badge" \
        "Query Officer 1 from Resource Projection"
    
    query_projection "$RESOURCE_PROJECTION_URL" "/officers/$officer2_badge" \
        "Query Officer 2 from Resource Projection"
    
    # Query Resource Projection - Vehicle
    query_projection "$RESOURCE_PROJECTION_URL" "/vehicles/$vehicle1_id" \
        "Query Vehicle 1 from Resource Projection"
    
    # Query Operational Projection - Incident
    query_projection "$OPERATIONAL_PROJECTION_URL" "/incidents/$incident1_id" \
        "Query Incident 1 from Operational Projection"
    
    # Query Operational Projection - Call
    query_projection "$OPERATIONAL_PROJECTION_URL" "/calls/$call1_id" \
        "Query Call 1 from Operational Projection"
    
    # Query Operational Projection - Dispatch
    query_projection "$OPERATIONAL_PROJECTION_URL" "/dispatches/$dispatch1_id" \
        "Query Dispatch 1 from Operational Projection"
    
    # Query Operational Projection - Activity
    query_projection "$OPERATIONAL_PROJECTION_URL" "/activities/$activity1_id" \
        "Query Activity 1 from Operational Projection"
    
    # Query Operational Projection - Assignment
    query_projection "$OPERATIONAL_PROJECTION_URL" "/assignments/$assignment1_id" \
        "Query Assignment 1 from Operational Projection"
    
    # Query Workforce Projection - Shift
    query_projection "$WORKFORCE_PROJECTION_URL" "/shifts/$shift1_id" \
        "Query Shift 1 from Workforce Projection"
    
    # Query composite - Incident with all related data
    query_projection "$OPERATIONAL_PROJECTION_URL" "/incidents/$incident1_id/full" \
        "Query Incident 1 (Full) with all related data from Operational Projection"
    
    # ============================================================================
    # PHASE 5: SUMMARY
    # ============================================================================
    print_section "PHASE 5: Demo Complete"
    
    print_success "Demo scenario completed successfully!"
    print_info "All resources created during this demo are stored in: $DEMO_DATA_FILE"
    print_info "Complete demo log is available at: $DEMO_LOG_FILE"
    print_info ""
    print_info "Summary of created resources:"
    print_info "  - Officers: $officer1_badge, $officer2_badge"
    print_info "  - Vehicle: $vehicle1_id"
    print_info "  - Unit: $unit1_id"
    print_info "  - Location: $location1_id"
    print_info "  - Person: $person1_id"
    print_info "  - Shift: $shift1_id"
    print_info "  - Call: $call1_id"
    print_info "  - Incident: $incident1_id"
    print_info "  - Dispatch: $dispatch1_id"
    print_info "  - Assignment: $assignment1_id"
    print_info "  - Activity: $activity1_id"
    print_info "  - Involved Party: $involved_party1_id"
    echo ""
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    print_warning "jq is not installed. Some features may not work correctly."
    print_info "Install jq with: sudo apt-get install jq (or brew install jq on macOS)"
    echo ""
fi

# Check if curl is installed
if ! command -v curl &> /dev/null; then
    print_error "curl is not installed. Please install curl to run this script."
    exit 1
fi

# Run main function
main
