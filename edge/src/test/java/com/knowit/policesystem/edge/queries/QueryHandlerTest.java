package com.knowit.policesystem.edge.queries;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for query handler infrastructure including handler execution.
 * Note: QueryHandlerRegistry is optional for this increment, so we test
 * the handler interface directly.
 */
class QueryHandlerTest {

    private TestQueryHandler testHandler;

    @BeforeEach
    void setUp() {
        testHandler = new TestQueryHandler();
    }

    @Test
    void testQueryHandler_CanHandleQuery() {
        // Given
        TestQuery query = new TestQuery("search term", 10);

        // When
        String result = testHandler.handle(query);

        // Then
        assertThat(result).isEqualTo("Results for: search term (limit: 10)");
        assertThat(testHandler.getQueryType()).isEqualTo(TestQuery.class);
    }

    @Test
    void testQueryHandler_HandleQuery_ReturnsResult() {
        // Given
        TestQuery query = new TestQuery("test query", 5);

        // When
        String result = testHandler.handle(query);

        // Then
        assertThat(result).isEqualTo("Results for: test query (limit: 5)");
    }

    @Test
    void testQueryHandler_GetQueryType_ReturnsCorrectType() {
        // When
        Class<TestQuery> queryType = testHandler.getQueryType();

        // Then
        assertThat(queryType).isEqualTo(TestQuery.class);
    }

    /**
     * Test implementation of QueryHandler for TestQuery.
     */
    private static class TestQueryHandler implements QueryHandler<TestQuery, String> {
        @Override
        public String handle(TestQuery query) {
            return "Results for: " + query.getSearchTerm() + " (limit: " + query.getLimit() + ")";
        }

        @Override
        public Class<TestQuery> getQueryType() {
            return TestQuery.class;
        }
    }
}

