# Projection Consolidation Archive

## Overview

This directory contains documentation related to the migration from 6 individual projection modules to 3 consolidated projections.

## Migration Status

**Current Phase:** Deprecation Complete

All old individual projection modules are marked as deprecated and ready for removal after successful migration period.

## Documents

- **[Client Migration Guide](client-migration-guide.md)** - Guide for migrating clients to new consolidated projections
- **[API Endpoint Mapping](api-endpoint-mapping.md)** - Complete mapping of old → new API endpoints
- **[Deprecation Timeline](deprecation-timeline.md)** - Timeline and status of deprecation process
- **[Removal Summary](removal-summary.md)** - Summary of what will be removed when old modules are deleted

## Git Archive

Before removing old projection modules, a git tag will be created:

```bash
git tag -a "pre-consolidation-projections-v1.0.0" -m "State before removing old individual projection modules"
```

This tag preserves the state of the codebase with all old projection modules intact.

## Old Projection Modules (Deprecated)

The following modules are deprecated and will be removed:

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

## Migration Completion Criteria

Before removing old modules, verify:

- ✅ All clients migrated to new projections
- ✅ Data validation shows consistency
- ✅ No production issues for 2+ weeks
- ✅ All tests pass with new projections
- ✅ Build succeeds without old modules
- ✅ Documentation updated
- ✅ Team approval obtained

## Questions

For questions about the migration:
- See migration guides in this directory
- Contact development team
- Check architecture documentation
