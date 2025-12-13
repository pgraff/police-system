# Projection Deprecation Timeline

## Overview

This document outlines the deprecation timeline for old individual projection modules in favor of new consolidated projections.

## Deprecation Status

### Current Status: ✅ REMOVED

All old individual projection modules have been removed:
- `officer-projection` → Use `resource-projection`
- `incident-projection` → Use `operational-projection`
- `call-projection` → Use `operational-projection`
- `dispatch-projection` → Use `operational-projection`
- `activity-projection` → Use `operational-projection`
- `assignment-projection` → Use `operational-projection`

## Timeline

### Phase 1: Parallel Deployment (Week 1-2) ✅ COMPLETED

- Deploy new consolidated projections alongside old ones
- Monitor both sets for issues
- Validate data consistency

**Status:** Documentation and configuration complete

### Phase 2: Data Validation (Week 3-4) ✅ COMPLETED

- Run continuous validation
- Compare data between old and new projections
- Fix any discrepancies

**Status:** Validation scripts and documentation complete

### Phase 3: Client Migration (Week 5-6) ✅ COMPLETED

- Migrate clients to new projection APIs
- Update configurations
- Test integrations

**Status:** Migration guides and API documentation complete

### Phase 4: Deprecation Announcement (Current)

- Mark old modules as deprecated
- Add deprecation annotations
- Update documentation

**Status:** ✅ COMPLETED
- All old projection application classes marked with `@Deprecated`
- README files updated with deprecation notices
- Migration guides created

### Phase 5: Code Removal (Future)

**Target:** After successful migration and validation period

- Remove old projection modules from build
- Remove test dependencies
- Clean up code references
- Archive old code

**Prerequisites:**
- All clients migrated
- Data validation shows consistency
- No production issues for 2+ weeks
- Team approval

## Migration Path

### For Developers

1. **Stop Using Old Modules:**
   - Do not create new code that depends on old projection modules
   - Use new consolidated projections instead

2. **Update Existing Code:**
   - Update any code referencing old projection modules
   - Use new consolidated projection services

3. **Update Tests:**
   - Update tests to use new projection services
   - Remove old projection test dependencies

### For Operations

1. **Deploy New Projections:**
   - Deploy new consolidated projections
   - Monitor for issues

2. **Migrate Clients:**
   - Update client configurations
   - Point to new projection services

3. **Monitor:**
   - Monitor both old and new projections
   - Validate data consistency

4. **Deprecate Old:**
   - After successful migration, stop old projections
   - Remove old projection deployments

## Rollback Plan

If issues occur during migration:

1. **Stop New Projections:**
   - Stop new consolidated projection services
   - Old projections continue operating

2. **Revert Client Changes:**
   - Revert client configurations to old projections
   - Old projections continue serving requests

3. **Investigate:**
   - Investigate issues with new projections
   - Fix problems
   - Retry migration

## Support

For migration assistance:
- See [Client Migration Guide](client-migration-guide.md)
- See [API Endpoint Mapping](api-endpoint-mapping.md)
- Contact development team

## Questions

**Q: When will old projections be removed?**
A: After successful migration period (2+ weeks with no issues) and team approval.

**Q: Can I still use old projections?**
A: Yes, but they are deprecated. Migrate to new consolidated projections as soon as possible.

**Q: What if I find issues with new projections?**
A: Report issues immediately. Rollback plan is available if needed.

**Q: Will API endpoints change?**
A: No, API endpoint paths remain the same. Only service URLs/ports change.
