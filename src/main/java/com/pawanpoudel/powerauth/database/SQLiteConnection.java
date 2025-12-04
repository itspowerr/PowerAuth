package com.pawanpoudel.powerauth.database;

import com.pawanpoudel.powerauth.PowerAuth;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnection implements DatabaseConnection {

    private final PowerAuth plugin;
    private Connection connection;

    public SQLiteConnection(PowerAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            File dataFolder = new File(plugin.getDataFolder(),
                    plugin.getConfig().getString("database.sqlite.file", "database.db"));

            if (!dataFolder.getParentFile().exists()) {
                dataFolder.getParentFile().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());

            plugin.getLogger().info("SQLite database connected successfully!");
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to initialize SQLite database!");
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initialize();
        }
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQLite database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
