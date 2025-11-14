package com.knowit.policesystem.edge.exceptions;

/**
 * Exception thrown when no query handler is found for a query type.
 */
public class QueryHandlerNotFoundException extends RuntimeException {
    /**
     * Creates a new exception.
     *
     * @param queryType the query type that has no handler
     */
    public QueryHandlerNotFoundException(String queryType) {
        super("No query handler found for query type: " + queryType);
    }
}

