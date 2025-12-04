package com.pawanpoudel.powerauth.database;

import com.pawanpoudel.powerauth.PowerAuth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class PostgreSQLConnection implements DatabaseConnection {

    private final PowerAuth plugin;
    private HikariDataSource dataSource;

    public PostgreSQLConnection(PowerAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();

            String host = plugin.getConfig().getString("database.postgresql.host", "localhost");
            int port = plugin.getConfig().getInt("database.postgresql.port", 5432);
            String database = plugin.getConfig().getString("database.postgresql.database", "powerauth");
            String username = plugin.getConfig().getString("database.postgresql.username", "postgres");
            String password = plugin.getConfig().getString("database.postgresql.password", "password");
            int poolSize = plugin.getConfig().getInt("database.postgresql.pool-size", 10);

            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // Performance settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            plugin.getLogger().info("PostgreSQL database connected successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize PostgreSQL database!");
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("PostgreSQL database connection pool closed.");
        }
    }

    @Override
    public boolean isValid() {
        return dataSource != null && !dataSource.isClosed();
    }
}
