# Projection Consolidation Archive

## Overview

This directory contains documentation related to the migration from 6 individual projection modules to 3 consolidated projections.

## Migration Status

**Current Phase:** ✅ Removal Complete

All old individual projection modules have been removed from the codebase.

## Documents

- **[Client Migration Guide](client-migration-guide.md)** - Guide for migrating clients to new consolidated projections
- **[API Endpoint Mapping](api-endpoint-mapping.md)** - Complete mapping of old → new API endpoints
- **[Deprecation Timeline](deprecation-timeline.md)** - Timeline and status of deprecation process
- **[Removal Summary](removal-summary.md)** - Summary of what will be removed when old modules are deleted

## Git Archive

The old projection modules have been removed. The codebase state before removal is preserved in git history.

## Old Projection Modules (Removed)

The following modules have been removed:

1. **officer-projection** → Use `resource-projection`
2. **incident-projection** → Use `operational-projection`
3. **call-projection** → Use `operational-projection`
4. **dispatch-projection** → Use `operational-projection`
5. **activity-projection** → Use `operational-projection`
6. **assignment-projection** → Use `operational-projection`

## New Consolidated Projections

1. **operational-projection** (Port 8081)
   - Handles: incidents, calls, dispatches, activities, assignments, involved parties, resource assignments

2. **resource-projection** (Port 8082)
   - Handles: officers, vehicles, units, persons, locations

3. **workforce-projection** (Port 8083)
   - Handles: shifts, officer shifts, shift changes

## Migration Completion Status

✅ **All criteria met:**
- ✅ All clients migrated to new projections
- ✅ Data validation shows consistency
- ✅ All tests pass with new projections
- ✅ Build succeeds without old modules
- ✅ Documentation updated
- ✅ Old modules removed from codebase

## Questions

For questions about the migration:
- See migration guides in this directory
- Contact development team
- Check architecture documentation
