package com.knowit.policesystem.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Call projection application.
 * 
 * @deprecated This module is deprecated. Use {@link com.knowit.policesystem.projection.OperationalProjectionApplication}
 *             instead, which handles calls along with incidents, dispatches, activities, and assignments.
 *             See migration guide: doc/migration/client-migration-guide.md
 */
@Deprecated
@SpringBootApplication
public class CallProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallProjectionApplication.class, args);
    }
}

