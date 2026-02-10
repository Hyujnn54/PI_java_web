package Interfaces;

import java.util.List;

/**
 * Generic service interface for CRUD operations
 * All service classes should implement this interface for consistency
 * This allows easy integration of new modules (User Service, Event Service, etc.)
 */
public interface IService<T> {
    /**
     * Create a new entity
     */
    void add(T entity);

    /**
     * Read all entities
     */
    List<T> getAll();

    /**
     * Read a specific entity by ID
     */
    T getById(int id);

    /**
     * Update an existing entity
     */
    void update(int id, T entity);

    /**
     * Delete an entity by ID
     */
    void delete(int id);
}

