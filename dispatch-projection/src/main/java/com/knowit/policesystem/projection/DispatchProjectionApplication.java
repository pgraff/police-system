package com.knowit.policesystem.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dispatch projection application.
 * 
 * @deprecated This module is deprecated. Use {@link com.knowit.policesystem.projection.OperationalProjectionApplication}
 *             instead, which handles dispatches along with incidents, calls, activities, and assignments.
 *             See migration guide: doc/migration/client-migration-guide.md
 */
@Deprecated
@SpringBootApplication
public class DispatchProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchProjectionApplication.class, args);
    }
}

