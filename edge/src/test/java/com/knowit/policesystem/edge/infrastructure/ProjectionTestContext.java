package com.knowit.policesystem.edge.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to manage projection Spring Boot application contexts in tests.
 * Allows starting and stopping projection services programmatically for E2E testing.
 * 
 * This class manages the lifecycle of projection applications:
 * - Start projection services in separate threads
 * - Wait for services to be ready
 * - Stop services and clean up resources
 */
public class ProjectionTestContext {

    private static final Logger log = LoggerFactory.getLogger(ProjectionTestContext.class);

    private final Map<String, ConfigurableApplicationContext> contexts = new HashMap<>();
    private final Map<String, Thread> applicationThreads = new HashMap<>();
    private final String natsUrl;
    private final String kafkaBootstrapServers;
    private final String postgresJdbcUrl;
    private final String postgresUsername;
    private final String postgresPassword;


    /**
     * Maps a domain to its consolidated projection application class name.
     * 
     * @param domain the domain name (e.g., "incident", "officer", "shift")
     * @return the fully qualified application class name for the consolidated projection
     */
    private static String getConsolidatedProjectionApplication(String domain) {
        // Operational projection domains
        if (domain.equals("incident") || domain.equals("call") || domain.equals("dispatch") 
                || domain.equals("activity") || domain.equals("assignment") 
                || domain.equals("involved-party") || domain.equals("resource-assignment")) {
            return "com.knowit.policesystem.projection.OperationalProjectionApplication";
        }
        // Resource projection domains
        if (domain.equals("officer") || domain.equals("vehicle") || domain.equals("unit") 
                || domain.equals("person") || domain.equals("location")) {
            return "com.knowit.policesystem.projection.ResourceProjectionApplication";
        }
        // Workforce projection domains
        if (domain.equals("shift") || domain.equals("officer-shift") || domain.equals("shift-change")) {
            return "com.knowit.policesystem.projection.WorkforceProjectionApplication";
        }
        // Default to operational for unknown domains (backward compatibility)
        log.warn("Unknown domain {}, defaulting to operational-projection", domain);
        return "com.knowit.policesystem.projection.OperationalProjectionApplication";
    }

    /**
     * Starts a projection service for the given domain using the appropriate consolidated projection.
     * This is a convenience method that automatically selects the correct consolidated projection application.
     * 
     * @param domain the domain name (e.g., "incident", "officer", "shift")
     * @return true if the projection started successfully, false otherwise
     */
    public boolean startProjection(String domain) {
        String applicationClassName = getConsolidatedProjectionApplication(domain);
        return startProjection(domain, applicationClassName);
    }

    /**
     * Configuration class that filters out bean definitions from other projection modules.
     * This runs early in the Spring lifecycle via BeanDefinitionRegistryPostProcessor.
     */
    @Configuration
    static class ProjectionFilterConfiguration implements org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor,
            org.springframework.context.EnvironmentAware {
        
        private org.springframework.core.env.Environment environment;
        
        @Override
        public void setEnvironment(org.springframework.core.env.Environment environment) {
            this.environment = environment;
        }
        
        @Override
        public void postProcessBeanDefinitionRegistry(
                org.springframework.beans.factory.support.BeanDefinitionRegistry registry) {
                    // Read domain from environment (set via property)
                    String domain = environment.getProperty("projection.test.domain", "officer");
                    String[] allBeanNames = registry.getBeanDefinitionNames();
                    
                    // Determine which consolidated projection this is based on application class
                    String applicationClassName = environment.getProperty("projection.test.application.class");
                    boolean isOperational = applicationClassName != null && applicationClassName.contains("OperationalProjectionApplication");
                    boolean isResource = applicationClassName != null && applicationClassName.contains("ResourceProjectionApplication");
                    boolean isWorkforce = applicationClassName != null && applicationClassName.contains("WorkforceProjectionApplication");
                    
                    // Filter out components from other consolidated projections
                    int removedCount = 0;
                    for (String beanName : allBeanNames) {
                        try {
                            org.springframework.beans.factory.config.BeanDefinition bd = 
                                registry.getBeanDefinition(beanName);
                            String beanClassName = bd.getBeanClassName();
                            
                            if (beanClassName != null && 
                                beanClassName.startsWith("com.knowit.policesystem.projection.")) {
                                boolean shouldRemove = false;
                                
                                // Remove components from other consolidated projections
                                // Check for specific class name patterns to avoid false positives
                                if (isOperational) {
                                        // Remove Resource and Workforce components (but keep Operational)
                                        if ((beanClassName.contains("ResourceKafkaListener") ||
                                             beanClassName.contains("ResourceNatsListener") ||
                                             beanClassName.contains("ResourceNatsQueryHandler") ||
                                             beanClassName.contains("ResourceEventParser") ||
                                             beanClassName.contains("ResourceProjectionService") ||
                                             beanClassName.contains("ResourceProjectionController") ||
                                             beanClassName.contains("ResourceProjectionApplication")) ||
                                            (beanClassName.contains("WorkforceKafkaListener") ||
                                             beanClassName.contains("WorkforceNatsListener") ||
                                             beanClassName.contains("WorkforceNatsQueryHandler") ||
                                             beanClassName.contains("WorkforceEventParser") ||
                                             beanClassName.contains("WorkforceProjectionService") ||
                                             beanClassName.contains("WorkforceProjectionController") ||
                                             beanClassName.contains("WorkforceProjectionApplication"))) {
                                            shouldRemove = true;
                                        }
                                    } else if (isResource) {
                                        // Remove Operational and Workforce components (but keep Resource)
                                        if ((beanClassName.contains("OperationalKafkaListener") ||
                                             beanClassName.contains("OperationalNatsListener") ||
                                             beanClassName.contains("OperationalNatsQueryHandler") ||
                                             beanClassName.contains("OperationalEventParser") ||
                                             beanClassName.contains("OperationalProjectionService") ||
                                             beanClassName.contains("OperationalProjectionController") ||
                                             beanClassName.contains("OperationalProjectionApplication")) ||
                                            (beanClassName.contains("WorkforceKafkaListener") ||
                                             beanClassName.contains("WorkforceNatsListener") ||
                                             beanClassName.contains("WorkforceNatsQueryHandler") ||
                                             beanClassName.contains("WorkforceEventParser") ||
                                             beanClassName.contains("WorkforceProjectionService") ||
                                             beanClassName.contains("WorkforceProjectionController") ||
                                             beanClassName.contains("WorkforceProjectionApplication"))) {
                                            shouldRemove = true;
                                        }
                                    } else if (isWorkforce) {
                                        // Remove Operational and Resource components (but keep Workforce)
                                        if ((beanClassName.contains("OperationalKafkaListener") ||
                                             beanClassName.contains("OperationalNatsListener") ||
                                             beanClassName.contains("OperationalNatsQueryHandler") ||
                                             beanClassName.contains("OperationalEventParser") ||
                                             beanClassName.contains("OperationalProjectionService") ||
                                             beanClassName.contains("OperationalProjectionController") ||
                                             beanClassName.contains("OperationalProjectionApplication")) ||
                                            (beanClassName.contains("ResourceKafkaListener") ||
                                             beanClassName.contains("ResourceNatsListener") ||
                                             beanClassName.contains("ResourceNatsQueryHandler") ||
                                             beanClassName.contains("ResourceEventParser") ||
                                             beanClassName.contains("ResourceProjectionService") ||
                                             beanClassName.contains("ResourceProjectionController") ||
                                             beanClassName.contains("ResourceProjectionApplication"))) {
                                            shouldRemove = true;
                                        }
                                    }
                                
                                if (shouldRemove) {
                                    registry.removeBeanDefinition(beanName);
                                    removedCount++;
                                    log.debug("Removed bean {} from other projection module", beanName);
                                }
                            }
                        } catch (Exception e) {
                            // Ignore errors during filtering
                        }
                    }
                    if (removedCount > 0) {
                        log.info("Removed {} bean definitions from other projection modules for domain {}", removedCount, domain);
                    }
        }
        
        @Override
        public void postProcessBeanFactory(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
            // No-op
        }
    }

    /**
     * Configuration class that provides stub Kafka listener container factories
     * for other projection modules. This prevents errors when other modules'
     * listeners are picked up by component scanning.
     */
    @Configuration
    static class StubKafkaListenerFactories {
        private org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> createFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<Object, Object> factory = 
                new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory((org.springframework.kafka.core.ConsumerFactory<Object, Object>) consumerFactory);
            factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
            return factory;
        }
        
        // Consolidated projection factories
        @org.springframework.context.annotation.Bean(name = "resourceKafkaListenerContainerFactory")
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "resourceKafkaListenerContainerFactory")
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> resourceFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
        
        @org.springframework.context.annotation.Bean(name = "operationalKafkaListenerContainerFactory")
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "operationalKafkaListenerContainerFactory")
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> operationalFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
        
        @org.springframework.context.annotation.Bean(name = "workforceKafkaListenerContainerFactory")
        @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "workforceKafkaListenerContainerFactory")
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> workforceFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
    }

    public ProjectionTestContext(String natsUrl, String kafkaBootstrapServers,
                                 String postgresJdbcUrl, String postgresUsername, String postgresPassword) {
        this.natsUrl = natsUrl;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.postgresJdbcUrl = postgresJdbcUrl;
        this.postgresUsername = postgresUsername;
        this.postgresPassword = postgresPassword;
    }

    /**
     * Starts a projection service for the given domain.
     * 
     * @param domain the domain name (e.g., "officer", "call", "incident")
     * @param applicationClassName the fully qualified class name of the Spring Boot application class
     * @return true if the projection started successfully
     */
    public boolean startProjection(String domain, String applicationClassName) {
        if (contexts.containsKey(domain)) {
            log.warn("Projection {} is already started", domain);
            return true;
        }

        log.info("Starting projection service for domain: {} with PostgreSQL URL: {}", domain, postgresJdbcUrl);

        try {
            // Create a new thread to run the Spring Boot application
            CountDownLatch startupLatch = new CountDownLatch(1);
            AtomicReference<ConfigurableApplicationContext> contextRef = new AtomicReference<>();
            AtomicReference<Exception> startupException = new AtomicReference<>();
            AtomicReference<Class<?>> applicationClassRef = new AtomicReference<>();

            Thread appThread = new Thread(() -> {
                // Set environment variables that application.yml uses (these take precedence)
                String originalProjectionUrl = System.getProperty("PROJECTION_DATASOURCE_URL");
                String originalProjectionUser = System.getProperty("PROJECTION_DATASOURCE_USERNAME");
                String originalProjectionPass = System.getProperty("PROJECTION_DATASOURCE_PASSWORD");
                try {
                    System.setProperty("PROJECTION_DATASOURCE_URL", postgresJdbcUrl);
                    System.setProperty("PROJECTION_DATASOURCE_USERNAME", postgresUsername);
                    System.setProperty("PROJECTION_DATASOURCE_PASSWORD", postgresPassword);
                    
                    // Load the application class dynamically
                    Class<?> applicationClass = Class.forName(applicationClassName);
                    applicationClassRef.set(applicationClass);
                    String currentPackage = applicationClass.getPackage().getName();
                    
                    // Use SpringApplicationBuilder for more control
                    org.springframework.boot.builder.SpringApplicationBuilder builder = 
                        new org.springframework.boot.builder.SpringApplicationBuilder(applicationClass);
                    
                    // Configure properties for the projection
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("spring.kafka.bootstrap-servers", kafkaBootstrapServers);
                    properties.put("spring.kafka.consumer.bootstrap-servers", kafkaBootstrapServers);
                    properties.put("spring.kafka.consumer.auto-offset-reset", "earliest");
                    properties.put("spring.kafka.consumer.group-id", "projection-test-" + domain + "-" + System.currentTimeMillis());
                    properties.put("spring.kafka.producer.bootstrap-servers", kafkaBootstrapServers);
                    // Set datasource properties - these should override application.yml
                    // Use both spring.datasource.* and PROJECTION_DATASOURCE_* to ensure they're picked up
                    log.debug("Setting datasource URL for projection {}: {}", domain, postgresJdbcUrl);
                    properties.put("spring.datasource.url", postgresJdbcUrl);
                    properties.put("spring.datasource.username", postgresUsername);
                    properties.put("spring.datasource.password", postgresPassword);
                    properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                    properties.put("spring.datasource.driverClassName", "org.postgresql.Driver");
                    // Also set as environment variable names that application.yml uses (with ${VAR:default} syntax)
                    // Spring Boot resolves these from system properties/environment variables
                    System.setProperty("PROJECTION_DATASOURCE_URL", postgresJdbcUrl);
                    System.setProperty("PROJECTION_DATASOURCE_USERNAME", postgresUsername);
                    System.setProperty("PROJECTION_DATASOURCE_PASSWORD", postgresPassword);
                    // Enable Spring Boot's auto SQL initialization - it will load schema.sql from classpath
                    // This is more reliable than manual initialization
                    properties.put("spring.sql.init.mode", "always");
                    properties.put("spring.sql.init.schema-locations", "classpath:schema.sql");
                    properties.put("spring.jpa.hibernate.ddl-auto", "none");
                    // Disable Flyway - projections use schema.sql instead
                    properties.put("spring.flyway.enabled", "false");
                    // NATS properties - consolidated projections use "projection.nats." prefix
                    properties.put("projection.nats.enabled", "true");
                    properties.put("projection.nats.url", natsUrl);
                    properties.put("projection.nats.query-enabled", "true");
                    properties.put("server.port", "0"); // Random port
                    properties.put("spring.jmx.enabled", "false"); // Disable JMX for tests
                    properties.put("spring.main.allow-bean-definition-overriding", "true");
                    properties.put("logging.level.root", "WARN"); // Reduce log noise
                    properties.put("logging.level.com.knowit.policesystem", "INFO");
                    
                    // Store domain and application class in properties so the filter config can access them
                    String finalDomain = domain;
                    properties.put("projection.test.domain", finalDomain);
                    properties.put("projection.test.application.class", applicationClassName);
                    
                    // Create a custom ApplicationContextInitializer to set properties with highest precedence
                    org.springframework.context.ApplicationContextInitializer<org.springframework.context.ConfigurableApplicationContext> initializer = 
                        (context) -> {
                            org.springframework.core.env.ConfigurableEnvironment env = context.getEnvironment();
                            org.springframework.core.env.MutablePropertySources propertySources = env.getPropertySources();
                            // Add properties with highest precedence (before application.yml)
                            propertySources.addFirst(new org.springframework.core.env.MapPropertySource("test-properties", properties));
                        };
                    
                    // Add configurations: filter config first (runs early), then stubs, then application
                    // The ProjectionFilterConfiguration is a static inner class that reads the domain from properties
                    SpringApplication app = builder
                        .properties(properties)
                        .initializers(initializer)
                        .web(org.springframework.boot.WebApplicationType.NONE)
                        .sources(ProjectionFilterConfiguration.class, StubKafkaListenerFactories.class, applicationClass)
                        .build();
                    
                    // Set default properties with highest precedence
                    app.setDefaultProperties(properties);
                    
                    ConfigurableApplicationContext context = app.run();
                    contextRef.set(context);
                    startupLatch.countDown();
                    log.info("Projection {} started successfully", domain);
                    
                    // Keep the context running
                    // The context will be closed when stopProjection is called
                } catch (Exception e) {
                    startupException.set(e);
                    startupLatch.countDown();
                    log.error("Failed to start projection {}", domain, e);
                } finally {
                    // Restore original system properties
                    if (originalProjectionUrl != null) {
                        System.setProperty("PROJECTION_DATASOURCE_URL", originalProjectionUrl);
                    } else {
                        System.clearProperty("PROJECTION_DATASOURCE_URL");
                    }
                    if (originalProjectionUser != null) {
                        System.setProperty("PROJECTION_DATASOURCE_USERNAME", originalProjectionUser);
                    } else {
                        System.clearProperty("PROJECTION_DATASOURCE_USERNAME");
                    }
                    if (originalProjectionPass != null) {
                        System.setProperty("PROJECTION_DATASOURCE_PASSWORD", originalProjectionPass);
                    } else {
                        System.clearProperty("PROJECTION_DATASOURCE_PASSWORD");
                    }
                }
            }, "projection-" + domain);

            appThread.setDaemon(true);
            appThread.start();
            applicationThreads.put(domain, appThread);

            // Wait for startup (max 60 seconds - Spring Boot apps can take time)
            boolean started = startupLatch.await(60, TimeUnit.SECONDS);
            
            if (!started) {
                log.error("Projection {} failed to start within timeout (60 seconds)", domain);
                if (startupException.get() != null) {
                    log.error("Startup exception was: ", startupException.get());
                }
                return false;
            }

            if (startupException.get() != null) {
                log.error("Projection {} failed to start with exception", domain, startupException.get());
                // Print stack trace for debugging
                startupException.get().printStackTrace();
                return false;
            }

            ConfigurableApplicationContext context = contextRef.get();
            if (context == null) {
                log.error("Projection {} context is null after startup", domain);
                return false;
            }
            
            if (!context.isActive()) {
                log.error("Projection {} context is not active. State: {}", domain, 
                    context.getEnvironment().getProperty("spring.application.name", "unknown"));
                return false;
            }

            contexts.put(domain, context);
            
            // Manually initialize schema if Spring Boot's auto-init didn't work
            // This ensures tables exist before queries are executed
            // For consolidated projections, we need to check the correct table name based on domain
            try {
                org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
                // For consolidated projections, table names match domain names (e.g., "officer" -> "officer_projection")
                String tableName = domain + "_projection";
                
                // Always execute schema.sql manually to ensure tables exist
                // Spring Boot's auto-init may not run reliably when starting applications programmatically
                // We'll do it manually to guarantee schema is initialized
                log.info("Ensuring schema is initialized for projection {}", domain);
                
                // Check if table exists first
                boolean tableExists = false;
                try {
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
                    tableExists = true;
                    log.debug("Table {} already exists for projection {}", tableName, domain);
                } catch (Exception e) {
                    // Table doesn't exist - this is expected, we'll create it
                    log.debug("Table {} does not exist yet for projection {}, will create it", tableName, domain);
                }
                
                // If table doesn't exist, initialize schema
                if (!tableExists) {
                    
                    // Try to read and execute schema.sql manually
                    try {
                        Class<?> appClass = applicationClassRef.get();
                        if (appClass == null) {
                            log.warn("Application class not available for schema initialization");
                        } else {
                            // Use Spring's ResourceDatabasePopulator to execute schema.sql
                            // This is the proper way to execute SQL scripts in Spring
                            // IMPORTANT: We need to load schema.sql from the specific module's JAR, not from
                            // another module that might be on the classpath. We do this by:
                            // 1. Finding which JAR file contains the application class
                            // 2. Loading schema.sql from that same JAR file
                            try {
                                org.springframework.core.io.Resource schemaResource = null;
                                if (appClass != null) {
                                    // Find the JAR file that contains the application class
                                    String appClassPath = appClass.getName().replace('.', '/') + ".class";
                                    java.net.URL appClassUrl = appClass.getClassLoader().getResource(appClassPath);
                                    if (appClassUrl != null) {
                                        String appClassUrlString = appClassUrl.toString();
                                        // Extract JAR path from URL like "jar:file:/path/to/jar!/com/package/Class.class"
                                        if (appClassUrlString.startsWith("jar:file:")) {
                                            int jarEndIndex = appClassUrlString.indexOf("!/");
                                            if (jarEndIndex > 0) {
                                                String jarPath = appClassUrlString.substring(9, jarEndIndex); // Remove "jar:file:" prefix
                                                // Construct URL to schema.sql in the same JAR
                                                String schemaUrlString = "jar:file:" + jarPath + "!/schema.sql";
                                                try {
                                                    java.net.URL schemaUrl = new java.net.URL(schemaUrlString);
                                                    schemaResource = new org.springframework.core.io.UrlResource(schemaUrl);
                                                    log.info("Loading schema.sql for projection {} from JAR: exists={}, url={}", 
                                                            domain, schemaResource.exists(), schemaUrl);
                                                } catch (java.net.MalformedURLException urlEx) {
                                                    log.warn("Failed to construct schema.sql URL from JAR path for projection {}: {}", 
                                                            domain, urlEx.getMessage());
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Fallback: try getResource() if JAR-based approach didn't work
                                    if (schemaResource == null || !schemaResource.exists()) {
                                        java.net.URL schemaUrl = appClass.getResource("/schema.sql");
                                        if (schemaUrl != null) {
                                            schemaResource = new org.springframework.core.io.UrlResource(schemaUrl);
                                            log.info("Loading schema.sql for projection {} using getResource() fallback: exists={}, url={}", 
                                                    domain, schemaResource.exists(), schemaUrl);
                                        }
                                    }
                                    
                                    // Final fallback: ClassPathResource
                                    if (schemaResource == null || !schemaResource.exists()) {
                                        schemaResource = new org.springframework.core.io.ClassPathResource("schema.sql", appClass.getClassLoader());
                                        log.warn("Using ClassPathResource fallback for projection {}: exists={}", 
                                                domain, schemaResource.exists());
                                    }
                                } else {
                                    // Fallback to context resource loader if appClass is not available
                                    org.springframework.core.io.ResourceLoader resourceLoader = context;
                                    schemaResource = resourceLoader.getResource("classpath:schema.sql");
                                    log.info("Loading schema.sql for projection {} using context resource loader: exists={}, description={}", 
                                            domain, schemaResource.exists(), schemaResource.getDescription());
                                }
                                
                                if (schemaResource.exists()) {
                                    // Log database connection info for debugging
                                    javax.sql.DataSource dataSource = context.getBean(javax.sql.DataSource.class);
                                    try (java.sql.Connection testConn = dataSource.getConnection()) {
                                        String dbUrl = testConn.getMetaData().getURL();
                                        String dbUser = testConn.getMetaData().getUserName();
                                        log.info("Database connection for projection {}: url={}, user={}", domain, dbUrl, dbUser);
                                    }
                                    org.springframework.jdbc.datasource.init.ResourceDatabasePopulator populator = 
                                        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(schemaResource);
                                    populator.setContinueOnError(false); // Fail fast to see errors
                                    populator.setSeparator(";");
                                    
                                    try {
                                        // Use ScriptUtils directly for more control
                                        java.sql.Connection connection = dataSource.getConnection();
                                        try {
                                            // Ensure we can control transactions
                                            boolean originalAutoCommit = connection.getAutoCommit();
                                            try {
                                                // Disable auto-commit to ensure all statements execute in one transaction
                                                connection.setAutoCommit(false);
                                                
                                                org.springframework.core.io.support.EncodedResource encodedResource = 
                                                    new org.springframework.core.io.support.EncodedResource(schemaResource, "UTF-8");
                                                
                                                log.debug("Executing schema.sql for projection {} using ScriptUtils", domain);
                                                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                                                    connection,
                                                    encodedResource,
                                                    false,
                                                    false,
                                                    org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_COMMENT_PREFIX,
                                                    ";",
                                                    org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                                                    org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
                                                );
                                                
                                                // Commit the transaction
                                                connection.commit();
                                                log.info("Executed and committed schema.sql using Spring ScriptUtils for projection {}", domain);
                                                
                                                // List all tables to verify creation
                                                try {
                                                    java.sql.DatabaseMetaData metaData = connection.getMetaData();
                                                    java.sql.ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"});
                                                    java.util.List<String> tableNames = new java.util.ArrayList<>();
                                                    while (tables.next()) {
                                                        tableNames.add(tables.getString("TABLE_NAME"));
                                                    }
                                                    log.info("Tables in database after schema execution for projection {}: {}", domain, tableNames);
                                                } catch (Exception metaEx) {
                                                    log.debug("Could not list tables: {}", metaEx.getMessage());
                                                }
                                            } catch (Exception sqlEx) {
                                                // Rollback on error
                                                try {
                                                    connection.rollback();
                                                    log.warn("Rolled back schema.sql execution for projection {} due to error", domain);
                                                } catch (Exception rollbackEx) {
                                                    log.warn("Failed to rollback: {}", rollbackEx.getMessage());
                                                }
                                                throw sqlEx;
                                            } finally {
                                                // Restore original auto-commit setting
                                                connection.setAutoCommit(originalAutoCommit);
                                            }
                                        } finally {
                                            connection.close();
                                        }
                                    } catch (Exception popEx) {
                                        log.error("Error executing schema.sql using ScriptUtils for projection {}: {}", 
                                                domain, popEx.getMessage(), popEx);
                                        // Fallback to ResourceDatabasePopulator
                                        try {
                                            log.info("Attempting fallback to ResourceDatabasePopulator for projection {}", domain);
                                            populator.setContinueOnError(false); // Fail fast to see errors
                                            populator.execute(dataSource);
                                            log.info("Executed schema.sql with ResourceDatabasePopulator (fallback) for projection {}", domain);
                                        } catch (Exception retryEx) {
                                            log.error("Failed to execute schema.sql even with fallback for projection {}: {}", 
                                                    domain, retryEx.getMessage(), retryEx);
                                            // Try to read and log the schema.sql content for debugging
                                            try {
                                                java.io.InputStream is = schemaResource.getInputStream();
                                                java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
                                                String schemaContent = scanner.hasNext() ? scanner.next() : "";
                                                log.debug("Schema.sql content for {}:\n{}", domain, schemaContent);
                                            } catch (Exception readEx) {
                                                log.debug("Could not read schema.sql content: {}", readEx.getMessage());
                                            }
                                        }
                                    }
                                    
                                    // Verify schema was created - retry a few times with longer delays
                                    boolean schemaVerified = false;
                                    for (int i = 0; i < 15; i++) {
                                        try {
                                            // Try a simple query first
                                            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
                                            
                                            // Also verify we can actually query the table structure by trying to select from it
                                            // This ensures the table is fully created and accessible
                                            try {
                                                jdbcTemplate.queryForList("SELECT * FROM " + tableName + " LIMIT 1");
                                            } catch (Exception structEx) {
                                                // If structure query fails, table might not be fully ready
                                                if (i < 14) {
                                                    Thread.sleep(500);
                                                    continue;
                                                }
                                            }
                                            
                                            schemaVerified = true;
                                            log.info("Successfully initialized and verified schema for projection {}", domain);
                                            break;
                                        } catch (Exception verifyEx) {
                                            if (i < 14) {
                                                Thread.sleep(500); // Longer delay
                                            } else {
                                                log.error("Schema initialization completed but table {} still not accessible after {} retries: {}", 
                                                        tableName, i + 1, verifyEx.getMessage());
                                                // Try to list all tables to debug
                                                try {
                                                    java.util.List<java.util.Map<String, Object>> tables = jdbcTemplate.queryForList(
                                                        "SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
                                                    log.debug("Available tables in database: {}", tables);
                                                } catch (Exception listEx) {
                                                    log.debug("Could not list tables: {}", listEx.getMessage());
                                                }
                                                // Don't fail here - let the projection start and see if it works anyway
                                                // Some databases might have slight delays in table visibility
                                            }
                                        }
                                    }
                                    
                                    if (!schemaVerified) {
                                        log.error("Schema verification failed for projection {} after {} retries. Table {} may not be ready.", 
                                                domain, 15, tableName);
                                        // List all tables to help debug
                                        try {
                                            java.util.List<java.util.Map<String, Object>> tables = jdbcTemplate.queryForList(
                                                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
                                            log.error("Available tables in database: {}", tables);
                                        } catch (Exception listEx) {
                                            log.error("Could not list tables: {}", listEx.getMessage());
                                        }
                                        // Don't throw - let it continue and see if it works anyway
                                        // Some databases might have delays in table visibility
                                    }
                                } else {
                                    log.error("Could not find schema.sql resource at classpath:schema.sql for projection {}", domain);
                                }
                            } catch (Exception ex) {
                                log.error("Failed to execute schema.sql using ResourceDatabasePopulator for projection {}: {}", 
                                        domain, ex.getMessage(), ex);
                            }
                        }
                    } catch (Exception ex) {
                        log.error("Failed to manually initialize schema for projection {}: {}", domain, ex.getMessage(), ex);
                    }
                }
            } catch (Exception e) {
                log.error("Could not verify/initialize schema for projection {}: {}", domain, e.getMessage(), e);
            }
            
            // Final verification: ensure table is accessible before marking as ready
            // Retry multiple times to handle any timing issues
            boolean finalVerificationPassed = false;
            for (int i = 0; i < 10; i++) {
                try {
                    org.springframework.jdbc.core.JdbcTemplate finalJdbcTemplate = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
                    String finalTableName = domain + "_projection";
                    finalJdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + finalTableName, Integer.class);
                    finalVerificationPassed = true;
                    log.debug("Final schema verification passed for projection {} (attempt {})", domain, i + 1);
                    break;
                } catch (Exception finalVerifyEx) {
                    if (i < 9) {
                        log.debug("Final schema verification failed for projection {} (attempt {}): {}. Retrying...", 
                                domain, i + 1, finalVerifyEx.getMessage());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.error("Final schema verification failed for projection {} after {} attempts: {}. Projection may not be fully ready.", 
                                domain, 10, finalVerifyEx.getMessage());
                        // List tables to help debug
                        try {
                            org.springframework.jdbc.core.JdbcTemplate finalJdbcTemplate = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
                            java.util.List<java.util.Map<String, Object>> tables = finalJdbcTemplate.queryForList(
                                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
                            log.error("Available tables in database: {}", tables);
                        } catch (Exception listEx) {
                            log.error("Could not list tables: {}", listEx.getMessage());
                        }
                    }
                }
            }
            
            if (!finalVerificationPassed) {
                log.error("WARNING: Final schema verification failed for projection {}. Tables may not be ready for queries.", domain);
            }
            
            // Wait a bit more for NATS query handler to be ready
            Thread.sleep(2000);
            
            log.info("Projection {} is ready", domain);
            return true;
        } catch (Exception e) {
            log.error("Error starting projection {}", domain, e);
            return false;
        }
    }

    /**
     * Stops a projection service for the given domain.
     * 
     * @param domain the domain name
     */
    public void stopProjection(String domain) {
        ConfigurableApplicationContext context = contexts.remove(domain);
        if (context != null) {
            try {
                log.info("Stopping projection service for domain: {}", domain);
                context.close();
                log.info("Projection {} stopped", domain);
            } catch (Exception e) {
                log.warn("Error stopping projection {}", domain, e);
            }
        }

        Thread appThread = applicationThreads.remove(domain);
        if (appThread != null && appThread.isAlive()) {
            try {
                appThread.join(5000); // Wait up to 5 seconds for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for projection {} thread to finish", domain);
            }
        }
    }

    /**
     * Checks if a projection service is running.
     * 
     * @param domain the domain name
     * @return true if the projection is running
     */
    public boolean isProjectionRunning(String domain) {
        ConfigurableApplicationContext context = contexts.get(domain);
        return context != null && context.isActive();
    }

    /**
     * Waits for a projection service to be ready.
     * 
     * @param domain the domain name
     * @param timeout the maximum time to wait
     * @return true if the projection is ready within the timeout
     */
    public boolean waitForProjectionReady(String domain, Duration timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (isProjectionRunning(domain)) {
                // Additional check: verify NATS query handler is ready
                // This is a simple check - in a real scenario you might ping a health endpoint
                try {
                    Thread.sleep(1000); // Give it a moment to fully initialize
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * Stops all running projection services.
     */
    public void stopAll() {
        log.info("Stopping all projection services");
        for (String domain : contexts.keySet().toArray(new String[0])) {
            stopProjection(domain);
        }
    }

    /**
     * Gets the application context for a projection (for advanced use cases).
     * 
     * @param domain the domain name
     * @return the application context, or null if not running
     */
    public ConfigurableApplicationContext getContext(String domain) {
        return contexts.get(domain);
    }

}

