#!/bin/bash

# Projection Comparison Script
# Compares data between old individual projections and new consolidated projections
# Assumes both are writing to the same database tables (parallel deployment scenario)

set -e

# Configuration
DB_HOST="${PROJECTION_DATASOURCE_HOST:-localhost}"
DB_PORT="${PROJECTION_DATASOURCE_PORT:-5432}"
DB_NAME="${PROJECTION_DATASOURCE_DB:-police}"
DB_USER="${PROJECTION_DATASOURCE_USERNAME:-postgres}"
DB_PASS="${PROJECTION_DATASOURCE_PASSWORD:-postgres}"

# API endpoints (if comparing via REST APIs)
OPERATIONAL_API="${OPERATIONAL_API_URL:-http://localhost:8081}"
RESOURCE_API="${RESOURCE_API_URL:-http://localhost:8082}"
WORKFORCE_API="${WORKFORCE_API_URL:-http://localhost:8083}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Export password for psql
export PGPASSWORD="$DB_PASS"

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
    echo -e "${BLUE}ℹ${NC} $1"
}

# Function to execute SQL and get result
execute_sql() {
    local query="$1"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c "$query" 2>/dev/null || echo "0"
}

# Function to compare entity counts between old and new
compare_entity_counts() {
    local entity_name="$1"
    local table_name="${2:-${entity_name}_projection}"
    
    print_info "Comparing $entity_name counts..."
    
    local count=$(execute_sql "SELECT COUNT(*) FROM $table_name")
    
    echo "  Total $entity_name records: $count"
    
    # Check if table exists and has data
    if [ "$count" -gt 0 ]; then
        print_success "$entity_name: $count records found"
        return 0
    else
        print_warning "$entity_name: No records found"
        return 1
    fi
}

# Function to compare specific entity by ID
compare_entity_by_id() {
    local entity_name="$1"
    local entity_id="$2"
    local id_column="${entity_name}_id"
    local table_name="${entity_name}_projection"
    
    print_info "Comparing $entity_name with ID: $entity_id"
    
    # Get data from database
    local data=$(execute_sql "SELECT * FROM $table_name WHERE $id_column = '$entity_id' LIMIT 1")
    
    if [ -z "$data" ]; then
        print_error "$entity_name $entity_id: Not found in database"
        return 1
    else
        print_success "$entity_name $entity_id: Found in database"
        echo "  Data: $data"
        return 0
    fi
}

# Function to compare status history
compare_status_history() {
    local entity_name="$1"
    local history_table="${entity_name}_status_history"
    
    print_info "Comparing $entity_name status history..."
    
    local count=$(execute_sql "SELECT COUNT(*) FROM $history_table" 2>/dev/null || echo "0")
    
    if [ "$count" -gt 0 ]; then
        print_success "$entity_name history: $count records"
        
        # Show sample history entries
        local sample=$(execute_sql "SELECT ${entity_name}_id, status, changed_at FROM $history_table ORDER BY changed_at DESC LIMIT 5" 2>/dev/null || echo "")
        if [ -n "$sample" ]; then
            echo "  Sample entries:"
            echo "$sample" | while IFS= read -r line; do
                echo "    $line"
            done
        fi
    else
        print_warning "$entity_name history: No records found"
    fi
}

# Function to compare via REST API (if both projections are running)
compare_via_api() {
    local entity_name="$1"
    local entity_id="$2"
    local old_api_url="$3"
    local new_api_url="$4"
    
    print_info "Comparing $entity_name $entity_id via REST APIs..."
    
    # Try to fetch from old projection (if running)
    local old_data=""
    if [ -n "$old_api_url" ]; then
        old_data=$(curl -s "${old_api_url}/api/projections/${entity_name}s/${entity_id}" 2>/dev/null || echo "")
    fi
    
    # Try to fetch from new projection
    local new_data=""
    if [ -n "$new_api_url" ]; then
        new_data=$(curl -s "${new_api_url}/api/projections/${entity_name}s/${entity_id}" 2>/dev/null || echo "")
    fi
    
    if [ -z "$old_data" ] && [ -z "$new_data" ]; then
        print_error "$entity_name $entity_id: Not found in either projection"
        return 1
    elif [ -z "$old_data" ]; then
        print_warning "$entity_name $entity_id: Found only in new projection"
        return 0
    elif [ -z "$new_data" ]; then
        print_warning "$entity_name $entity_id: Found only in old projection"
        return 0
    else
        # Compare data (simple string comparison)
        if [ "$old_data" = "$new_data" ]; then
            print_success "$entity_name $entity_id: Data matches between projections"
            return 0
        else
            print_warning "$entity_name $entity_id: Data differs between projections"
            echo "  Old: $old_data"
            echo "  New: $new_data"
            return 1
        fi
    fi
}

# Function to generate comparison report
generate_report() {
    local report_file="${1:-/tmp/projection-comparison-report.txt}"
    
    echo "Projection Comparison Report" > "$report_file"
    echo "Generated: $(date)" >> "$report_file"
    echo "Database: $DB_NAME@$DB_HOST:$DB_PORT" >> "$report_file"
    echo "" >> "$report_file"
    
    # Entity counts
    echo "=== Entity Counts ===" >> "$report_file"
    for entity in incident call dispatch activity assignment officer vehicle unit person location shift; do
        local count=$(execute_sql "SELECT COUNT(*) FROM ${entity}_projection" 2>/dev/null || echo "0")
        echo "$entity: $count" >> "$report_file"
    done
    
    echo "" >> "$report_file"
    echo "Report saved to: $report_file"
}

# Main comparison function
main() {
    echo "=========================================="
    echo "Projection Comparison Tool"
    echo "=========================================="
    echo ""
    echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
    echo "User: $DB_USER"
    echo ""
    
    # Compare entity counts
    echo "=== Entity Counts ==="
    compare_entity_counts "incident"
    compare_entity_counts "call"
    compare_entity_counts "dispatch"
    compare_entity_counts "activity"
    compare_entity_counts "assignment"
    compare_entity_counts "officer"
    compare_entity_counts "vehicle"
    compare_entity_counts "unit"
    compare_entity_counts "person"
    compare_entity_counts "location"
    compare_entity_counts "shift"
    echo ""
    
    # Compare status history
    echo "=== Status History ==="
    compare_status_history "incident"
    compare_status_history "call"
    compare_status_history "dispatch"
    compare_status_history "activity"
    compare_status_history "assignment"
    compare_status_history "officer"
    compare_status_history "vehicle"
    compare_status_history "unit"
    compare_status_history "shift"
    echo ""
    
    # Generate report
    generate_report
    
    echo ""
    echo "=========================================="
    echo "Comparison Complete"
    echo "=========================================="
}

# Check if specific entity ID provided
if [ -n "$1" ] && [ -n "$2" ]; then
    compare_entity_by_id "$1" "$2"
else
    main
fi
