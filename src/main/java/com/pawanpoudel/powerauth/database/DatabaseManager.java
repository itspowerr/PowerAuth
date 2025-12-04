package com.pawanpoudel.powerauth.database;

import com.pawanpoudel.powerauth.PowerAuth;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final PowerAuth plugin;
    private Connection connection;

    public DatabaseManager(PowerAuth plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "database.db");
            if (!dataFolder.getParentFile().exists()) {
                dataFolder.getParentFile().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());

            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database!", e);
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16) NOT NULL," +
                "password_hash VARCHAR(255)," +
                "is_premium BOOLEAN DEFAULT 0," +
                "last_ip VARCHAR(45)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create tables!", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // Helper methods
    public boolean isRegistered(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void registerPlayer(UUID uuid, String username, String passwordHash, String ip) {
        try (PreparedStatement ps = connection
                .prepareStatement("INSERT INTO players (uuid, username, password_hash, last_ip) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPasswordHash(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT password_hash FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password_hash");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLastIp(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT last_ip FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("last_ip");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateIp(UUID uuid, String ip) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE players SET last_ip = ? WHERE uuid = ?")) {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPremium(UUID uuid, boolean premium) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE players SET is_premium = ? WHERE uuid = ?")) {
            ps.setBoolean(1, premium);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPremium(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT is_premium FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_premium");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
