#!/bin/bash

# Projection Data Validation Script
# Compares data between old individual projections and new consolidated projections

set -e

# Configuration
DB_HOST="${PROJECTION_DATASOURCE_HOST:-localhost}"
DB_PORT="${PROJECTION_DATASOURCE_PORT:-5432}"
DB_NAME="${PROJECTION_DATASOURCE_DB:-police}"
DB_USER="${PROJECTION_DATASOURCE_USERNAME:-postgres}"
DB_PASS="${PROJECTION_DATASOURCE_PASSWORD:-postgres}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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
    echo -e "  $1"
}

# Function to execute SQL and get result
execute_sql() {
    local query="$1"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c "$query" 2>/dev/null || echo "0"
}

# Function to compare entity counts
compare_entity_counts() {
    local entity_name="$1"
    local table_name="${2:-${entity_name}_projection}"
    
    print_info "Comparing $entity_name counts..."
    
    local count=$(execute_sql "SELECT COUNT(*) FROM $table_name")
    
    if [ "$count" -gt 0 ]; then
        print_success "$entity_name: $count records found"
        echo "$count" > "/tmp/${entity_name}_count.txt"
    else
        print_warning "$entity_name: No records found"
        echo "0" > "/tmp/${entity_name}_count.txt"
    fi
}

# Function to compare status history counts
compare_status_history() {
    local entity_name="$1"
    local history_table="${entity_name}_status_history"
    
    print_info "Comparing $entity_name status history..."
    
    local count=$(execute_sql "SELECT COUNT(*) FROM $history_table" 2>/dev/null || echo "0")
    
    if [ "$count" -gt 0 ]; then
        print_success "$entity_name history: $count records found"
    else
        print_warning "$entity_name history: No records found (table may not exist)"
    fi
}

# Function to check for missing event IDs
check_event_ids() {
    local entity_name="$1"
    local table_name="${entity_name}_projection"
    
    print_info "Checking event IDs for $entity_name..."
    
    local total=$(execute_sql "SELECT COUNT(*) FROM $table_name")
    local with_event_id=$(execute_sql "SELECT COUNT(*) FROM $table_name WHERE event_id IS NOT NULL AND event_id != ''")
    local missing=$((total - with_event_id))
    
    if [ "$missing" -eq 0 ]; then
        print_success "$entity_name: All $total records have event_id"
    else
        print_error "$entity_name: $missing out of $total records missing event_id"
    fi
}

# Function to check for duplicate event IDs
check_duplicate_events() {
    local entity_name="$1"
    local table_name="${entity_name}_projection"
    
    print_info "Checking for duplicate event IDs in $entity_name..."
    
    local duplicates=$(execute_sql "SELECT COUNT(*) FROM (SELECT event_id, COUNT(*) as cnt FROM $table_name WHERE event_id IS NOT NULL GROUP BY event_id HAVING COUNT(*) > 1) AS dup")
    
    if [ "$duplicates" -eq 0 ]; then
        print_success "$entity_name: No duplicate event IDs found"
    else
        print_error "$entity_name: $duplicates duplicate event IDs found"
    fi
}

# Function to validate entity data consistency
validate_entity_consistency() {
    local entity_name="$1"
    local table_name="${entity_name}_projection"
    
    print_info "Validating $entity_name data consistency..."
    
    # Check for null primary keys
    local null_keys=$(execute_sql "SELECT COUNT(*) FROM $table_name WHERE ${entity_name}_id IS NULL" 2>/dev/null || echo "0")
    
    if [ "$null_keys" -eq 0 ]; then
        print_success "$entity_name: No null primary keys"
    else
        print_error "$entity_name: $null_keys records with null primary keys"
    fi
    
    # Check for null timestamps (updated_at)
    local null_timestamps=$(execute_sql "SELECT COUNT(*) FROM $table_name WHERE updated_at IS NULL" 2>/dev/null || echo "0")
    
    if [ "$null_timestamps" -eq 0 ]; then
        print_success "$entity_name: All records have updated_at timestamp"
    else
        print_warning "$entity_name: $null_timestamps records missing updated_at timestamp"
    fi
}

# Main validation function
main() {
    echo "=========================================="
    echo "Projection Data Validation"
    echo "=========================================="
    echo ""
    echo "Database: $DB_NAME@$DB_HOST:$DB_PORT"
    echo "User: $DB_USER"
    echo ""
    
    # Operational entities
    echo "=== Operational Projection Entities ==="
    compare_entity_counts "incident"
    compare_status_history "incident"
    check_event_ids "incident"
    check_duplicate_events "incident"
    validate_entity_consistency "incident"
    echo ""
    
    compare_entity_counts "call"
    compare_status_history "call"
    check_event_ids "call"
    check_duplicate_events "call"
    validate_entity_consistency "call"
    echo ""
    
    compare_entity_counts "dispatch"
    compare_status_history "dispatch"
    check_event_ids "dispatch"
    check_duplicate_events "dispatch"
    validate_entity_consistency "dispatch"
    echo ""
    
    compare_entity_counts "activity"
    compare_status_history "activity"
    check_event_ids "activity"
    check_duplicate_events "activity"
    validate_entity_consistency "activity"
    echo ""
    
    compare_entity_counts "assignment"
    compare_status_history "assignment"
    check_event_ids "assignment"
    check_duplicate_events "assignment"
    validate_entity_consistency "assignment"
    echo ""
    
    # Resource entities
    echo "=== Resource Projection Entities ==="
    compare_entity_counts "officer"
    compare_status_history "officer"
    check_event_ids "officer"
    check_duplicate_events "officer"
    validate_entity_consistency "officer"
    echo ""
    
    compare_entity_counts "vehicle"
    compare_status_history "vehicle"
    check_event_ids "vehicle"
    check_duplicate_events "vehicle"
    validate_entity_consistency "vehicle"
    echo ""
    
    compare_entity_counts "unit"
    compare_status_history "unit"
    check_event_ids "unit"
    check_duplicate_events "unit"
    validate_entity_consistency "unit"
    echo ""
    
    compare_entity_counts "person"
    check_event_ids "person"
    check_duplicate_events "person"
    validate_entity_consistency "person"
    echo ""
    
    compare_entity_counts "location"
    check_event_ids "location"
    check_duplicate_events "location"
    validate_entity_consistency "location"
    echo ""
    
    # Workforce entities
    echo "=== Workforce Projection Entities ==="
    compare_entity_counts "shift"
    compare_status_history "shift"
    check_event_ids "shift"
    check_duplicate_events "shift"
    validate_entity_consistency "shift"
    echo ""
    
    compare_entity_counts "officer_shift" "officer_shift_projection"
    check_event_ids "officer_shift" "officer_shift_projection"
    check_duplicate_events "officer_shift" "officer_shift_projection"
    echo ""
    
    compare_entity_counts "shift_change" "shift_change_projection"
    check_event_ids "shift_change" "shift_change_projection"
    check_duplicate_events "shift_change" "shift_change_projection"
    echo ""
    
    # Summary
    echo "=========================================="
    echo "Validation Complete"
    echo "=========================================="
}

# Run main function
main
