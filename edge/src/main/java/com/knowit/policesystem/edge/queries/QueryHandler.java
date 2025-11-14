package com.knowit.policesystem.edge.queries;

/**
 * Interface for handling queries.
 * Implementations should process queries and return results.
 *
 * @param <Q> the query type
 * @param <R> the result type
 */
public interface QueryHandler<Q extends Query, R> {
    /**
     * Handles the given query and returns a result.
     *
     * @param query the query to handle
     * @return the result of query execution
     */
    R handle(Q query);

    /**
     * Returns the query type this handler can process.
     *
     * @return the query class
     */
    Class<Q> getQueryType();
}

