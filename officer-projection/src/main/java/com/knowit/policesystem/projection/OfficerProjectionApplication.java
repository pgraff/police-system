package com.knowit.policesystem.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Officer projection application.
 * 
 * @deprecated This module is deprecated. Use {@link com.knowit.policesystem.projection.ResourceProjectionApplication}
 *             instead, which handles officers along with vehicles, units, persons, and locations.
 *             See migration guide: doc/migration/client-migration-guide.md
 */
@Deprecated
@SpringBootApplication
public class OfficerProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficerProjectionApplication.class, args);
    }
}
