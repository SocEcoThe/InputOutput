package org.cockshott.database;

import java.sql.Connection;

public interface DatabaseOperation<T> {
    T run(Connection connection) throws Exception;
}
