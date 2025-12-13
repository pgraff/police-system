package com.knowit.policesystem.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Activity projection application.
 * 
 * @deprecated This module is deprecated. Use {@link com.knowit.policesystem.projection.OperationalProjectionApplication}
 *             instead, which handles activities along with incidents, calls, dispatches, and assignments.
 *             See migration guide: doc/migration/client-migration-guide.md
 */
@Deprecated
@SpringBootApplication
public class ActivityProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityProjectionApplication.class, args);
    }
}

