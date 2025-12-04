package com.pawanpoudel.powerauth.database;

import com.pawanpoudel.powerauth.PowerAuth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLConnection implements DatabaseConnection {

    private final PowerAuth plugin;
    private HikariDataSource dataSource;

    public MySQLConnection(PowerAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();

            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "powerauth");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "password");
            int poolSize = plugin.getConfig().getInt("database.mysql.pool-size", 10);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true");
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
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);

            plugin.getLogger().info("MySQL database connected successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL database!");
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
            plugin.getLogger().info("MySQL database connection pool closed.");
        }
    }

    @Override
    public boolean isValid() {
        return dataSource != null && !dataSource.isClosed();
    }
}
