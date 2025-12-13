# Projection Module Removal Summary

## Overview

This document summarizes what was removed when old individual projection modules were removed from the codebase.

**Status:** ✅ All modules have been removed.

## Modules Removed

### 1. officer-projection

**Replacement:** `resource-projection`

**Files/Directories:**
- `officer-projection/` (entire directory)
- All Java source files
- `schema.sql`
- `application.yml`
- Test files

**What it handled:**
- Officer entities
- Officer status history

**Migration:** Officers are now handled by `resource-projection` along with vehicles, units, persons, and locations.

### 2. incident-projection

**Replacement:** `operational-projection`

**Files/Directories:**
- `incident-projection/` (entire directory)
- All Java source files
- `schema.sql`
- `application.yml`
- Test files

**What it handled:**
- Incident entities
- Incident status history

**Migration:** Incidents are now handled by `operational-projection` along with calls, dispatches, activities, and assignments.

### 3. call-projection

**Replacement:** `operational-projection`

**Files/Directories:**
- `call-projection/` (entire directory)
- All Java source files
- `schema.sql`
- `application.yml`
- Test files

**What it handled:**
- Call entities
- Call status history

**Migration:** Calls are now handled by `operational-projection` along with incidents, dispatches, activities, and assignments.

### 4. dispatch-projection

**Replacement:** `operational-projection`

**Files/Directories:**
- `dispatch-projection/` (entire directory)
- All Java source files
- `schema.sql`
- `application.yml`
- Test files

**What it handled:**
- Dispatch entities
- Dispatch status history

**Migration:** Dispatches are now handled by `operational-projection` along with incidents, calls, activities, and assignments.

### 5. activity-projection

**Replacement:** `operational-projection`

**Files/Directories:**
- `activity-projection/` (entire directory)
- All Java source files
- `schema.sql`
- `application.yml`
- Test files

**What it handled:**
- Activity entities
- Activity status history

**Migration:** Activities are now handled by `operational-projection` along with incidents, calls, dispatches, and assignments.

### 6. assignment-projection

**Replacement:** `operational-projection`

**Files/Directories:**
- `assignment-projection/` (entire directory)
- All Java source files
- `schema.sql`
- `application.yml`
- Test files

**What it handled:**
- Assignment entities
- Assignment status history

**Migration:** Assignments are now handled by `operational-projection` along with incidents, calls, dispatches, and activities.

## Build Configuration Changes

### pom.xml (Root)

**Remove:**
```xml
<module>officer-projection</module>
<module>incident-projection</module>
<module>call-projection</module>
<module>dispatch-projection</module>
<module>activity-projection</module>
<module>assignment-projection</module>
```

**Keep:**
```xml
<module>operational-projection</module>
<module>resource-projection</module>
<module>workforce-projection</module>
```

### edge/pom.xml

**Remove test dependencies:**
```xml
<!-- Old individual projections (DEPRECATED) -->
<dependency>
    <groupId>com.knowit.policesystem</groupId>
    <artifactId>officer-projection</artifactId>
    ...
</dependency>
<!-- (similar for all 6 old projections) -->
```

**Keep:**
```xml
<!-- New consolidated projections -->
<dependency>
    <groupId>com.knowit.policesystem</groupId>
    <artifactId>operational-projection</artifactId>
    ...
</dependency>
<!-- (similar for resource-projection and workforce-projection) -->
```

### ProjectionTestContext.java

**Remove:**
- Old individual projection filtering logic
- References to old projection application classes
- `getOtherModulePatterns()` method (or mark as deprecated/remove)

**Keep:**
- Consolidated projection support
- `getConsolidatedProjectionApplication()` method
- Consolidated projection filtering logic

## Code Statistics

### Files Removed

**Actual counts:**
- 6 module directories (removed)
- ~120 Java source files
- 6 schema.sql files
- 6 application.yml files
- ~30 test files
- 6 README files

### Lines of Code

**Approximate:**
- ~15,000-20,000 lines of Java code
- ~500 lines of SQL schema
- ~300 lines of configuration
- ~5,000 lines of test code

**Total:** ~20,000-25,000 lines of code to be removed

## Database Tables

**Note:** Database tables are NOT removed. Both old and new projections use the same table names:
- `officer_projection` (used by both old and new)
- `incident_projection` (used by both old and new)
- `call_projection` (used by both old and new)
- etc.

After migration, only new consolidated projections write to these tables, but table names remain the same.

## API Endpoints

**No Changes:** API endpoint paths remain the same. Only service URLs/ports change:
- Old: `http://officer-projection:8081/api/projections/officers/{id}`
- New: `http://resource-projection:8082/api/projections/officers/{id}`

## Migration Verification Checklist

Before removing old modules, verify:

- [ ] All clients migrated to new projections
- [ ] Data validation shows consistency
- [ ] No production issues for 2+ weeks
- [ ] All tests pass with new projections
- [ ] Build succeeds without old modules
- [ ] Documentation updated
- [ ] Team approval obtained

## Archive Strategy

### Git Tag

Create a git tag before removal:
```bash
git tag -a "pre-consolidation-projections" -m "State before removing old individual projection modules"
```

### Archive Location

Archive old projection code in:
- Git history (always available)
- Optional: Separate archive branch or repository

### Documentation Archive

Archive old projection documentation:
- README files
- Architecture documentation references
- API documentation

## Removal Steps (Future)

When ready to remove:

1. **Create Git Tag:**
   ```bash
   git tag -a "pre-consolidation-projections-v1.0.0" -m "State before removing old projection modules"
   ```

2. **Remove from pom.xml:**
   - Remove old module entries from root `pom.xml`
   - Remove test dependencies from `edge/pom.xml`

3. **Remove Directories:**
   ```bash
   rm -rf officer-projection/
   rm -rf incident-projection/
   rm -rf call-projection/
   rm -rf dispatch-projection/
   rm -rf activity-projection/
   rm -rf assignment-projection/
   ```

4. **Update ProjectionTestContext:**
   - Remove old projection filtering logic
   - Clean up deprecated methods

5. **Update Documentation:**
   - Remove references to old projections
   - Update architecture diagrams
   - Archive old documentation

6. **Verify Build:**
   ```bash
   mvn clean compile
   mvn clean test
   ```

7. **Commit:**
   ```bash
   git add -A
   git commit -m "chore: remove deprecated individual projection modules

   - Remove 6 old individual projection modules
   - Replaced by 3 consolidated projections:
     - operational-projection (incidents, calls, dispatches, activities, assignments)
     - resource-projection (officers, vehicles, units, persons, locations)
     - workforce-projection (shifts, officer shifts, shift changes)
   - See doc/migration/removal-summary.md for details"
   ```

## Benefits of Removal

1. **Reduced Complexity:**
   - 3 services instead of 6
   - Fewer modules to maintain
   - Simpler build process

2. **Faster Builds:**
   - Fewer modules to compile
   - Faster test execution

3. **Easier Deployment:**
   - 3 services instead of 6
   - Simpler configuration
   - Reduced resource usage

4. **Better Organization:**
   - Logical grouping by use case
   - Clearer architecture
   - Easier to understand

## Risks and Mitigation

### Risk: Breaking Changes

**Mitigation:**
- Thorough testing before removal
- Gradual migration period
- Rollback plan available

### Risk: Lost Code

**Mitigation:**
- Git history preserves all code
- Git tag created before removal
- Documentation archived

### Risk: Missing Functionality

**Mitigation:**
- Comprehensive testing
- Data validation
- Feature parity verified

## Timeline

**Current:** Deprecation phase (modules marked as deprecated)

**Next:** Removal phase (after successful migration and validation period)

**Target:** After 2+ weeks of successful operation with new projections
