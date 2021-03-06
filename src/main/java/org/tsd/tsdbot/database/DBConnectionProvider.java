package org.tsd.tsdbot.database;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

@Singleton
public class DBConnectionProvider implements Provider<Connection> {

    private static final Logger log = LoggerFactory.getLogger(DBConnectionProvider.class);

    private static String testQuery = "select 1";

    private Connection connection;
    private String connectionString;

    @Inject
    public DBConnectionProvider(@DBConnectionString String connectionString) {
        log.info("Initializing ConnectionProvider with connectionString={}", connectionString);
        this.connectionString = connectionString;
    }

    @Override
    public Connection get() {
        try {
            if(connection == null || connection.isClosed()) {
                log.info("Connection is null or closed, retrying with connectionString={}", connectionString);
                connection = DriverManager.getConnection(connectionString);
                log.info("Connection created, properties: {}", connection.getClientInfo().toString());
            }
            try(PreparedStatement ps = connection.prepareStatement(testQuery);ResultSet result = ps.executeQuery()) {}
        } catch (SQLException sqle) {
            log.error("DB TEST QUERY FAILED: " + sqle.getMessage(), sqle);
            return null;
        }

        return connection;
    }
}
