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
     * Helper method to get regex pattern for other module class names.
     */
    private static String getOtherModulePatterns(String currentDomain) {
        String[] allModules = {"call", "incident", "dispatch", "activity", "assignment", "officer"};
        StringBuilder pattern = new StringBuilder();
        for (String module : allModules) {
            if (!module.equals(currentDomain)) {
                if (pattern.length() > 0) {
                    pattern.append("|");
                }
                String modulePrefix = module.substring(0, 1).toUpperCase() + module.substring(1);
                pattern.append(modulePrefix);
            }
        }
        return pattern.toString();
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
            String currentModulePrefix = domain.substring(0, 1).toUpperCase() + domain.substring(1);
            String[] otherModulePrefixes = {"Call", "Incident", "Dispatch", "Activity", "Assignment", "Officer"};
            
            int removedCount = 0;
            for (String beanName : allBeanNames) {
                try {
                    org.springframework.beans.factory.config.BeanDefinition bd = 
                        registry.getBeanDefinition(beanName);
                    String beanClassName = bd.getBeanClassName();
                    
                    if (beanClassName != null && 
                        beanClassName.startsWith("com.knowit.policesystem.projection.")) {
                        // Check if this is a component from another module
                        // Components can be in .consumer., .nats., or directly in .projection. package
                        boolean isOtherModule = false;
                        for (String otherPrefix : otherModulePrefixes) {
                            if (!otherPrefix.equals(currentModulePrefix)) {
                                // Check for various component patterns
                                if (beanClassName.contains(otherPrefix + "KafkaListener") ||
                                    beanClassName.contains(otherPrefix + "NatsListener") ||
                                    beanClassName.contains(otherPrefix + "NatsQueryHandler") ||
                                    beanClassName.contains(otherPrefix + "EventParser") ||
                                    beanClassName.equals("com.knowit.policesystem.projection." + otherPrefix + "NatsListener")) {
                                    isOtherModule = true;
                                    break;
                                }
                            }
                        }
                        
                        if (isOtherModule) {
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
        
        @org.springframework.context.annotation.Bean(name = "callKafkaListenerContainerFactory")
        @org.springframework.context.annotation.Primary
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> callFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
        
        @org.springframework.context.annotation.Bean(name = "incidentKafkaListenerContainerFactory")
        @org.springframework.context.annotation.Primary
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> incidentFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
        
        @org.springframework.context.annotation.Bean(name = "dispatchKafkaListenerContainerFactory")
        @org.springframework.context.annotation.Primary
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> dispatchFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
        
        @org.springframework.context.annotation.Bean(name = "activityKafkaListenerContainerFactory")
        @org.springframework.context.annotation.Primary
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> activityFactory(
                org.springframework.kafka.core.ConsumerFactory<?, ?> consumerFactory) {
            return createFactory(consumerFactory);
        }
        
        @org.springframework.context.annotation.Bean(name = "assignmentKafkaListenerContainerFactory")
        @org.springframework.context.annotation.Primary
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<?, ?> assignmentFactory(
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

        log.info("Starting projection service for domain: {}", domain);

        try {
            // Create a new thread to run the Spring Boot application
            CountDownLatch startupLatch = new CountDownLatch(1);
            AtomicReference<ConfigurableApplicationContext> contextRef = new AtomicReference<>();
            AtomicReference<Exception> startupException = new AtomicReference<>();

            Thread appThread = new Thread(() -> {
                try {
                    // Load the application class dynamically
                    Class<?> applicationClass = Class.forName(applicationClassName);
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
                    properties.put("spring.datasource.url", postgresJdbcUrl);
                    properties.put("spring.datasource.username", postgresUsername);
                    properties.put("spring.datasource.password", postgresPassword);
                    // Enable SQL initialization (projections use schema.sql for table creation)
                    properties.put("spring.sql.init.mode", "always");
                    properties.put("spring.sql.init.schema-locations", "classpath:schema.sql");
                    properties.put("spring.sql.init.continue-on-error", "false");
                    properties.put("spring.jpa.hibernate.ddl-auto", "none");
                    // Disable Flyway - projections use schema.sql instead
                    properties.put("spring.flyway.enabled", "false");
                    properties.put("projection.nats.enabled", "true");
                    properties.put("projection.nats.url", natsUrl);
                    properties.put("projection.nats.query.enabled", "true");
                    properties.put("server.port", "0"); // Random port
                    properties.put("spring.jmx.enabled", "false"); // Disable JMX for tests
                    properties.put("spring.main.allow-bean-definition-overriding", "true");
                    properties.put("logging.level.root", "WARN"); // Reduce log noise
                    properties.put("logging.level.com.knowit.policesystem", "INFO");
                    
                    // Store domain in a property so the filter config can access it
                    String finalDomain = domain;
                    properties.put("projection.test.domain", finalDomain);
                    
                    // Add configurations: filter config first (runs early), then stubs, then application
                    // The ProjectionFilterConfiguration is a static inner class that reads the domain from properties
                    SpringApplication app = builder
                        .properties(properties)
                        .web(org.springframework.boot.WebApplicationType.NONE)
                        .sources(ProjectionFilterConfiguration.class, StubKafkaListenerFactories.class, applicationClass)
                        .build();
                    
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

