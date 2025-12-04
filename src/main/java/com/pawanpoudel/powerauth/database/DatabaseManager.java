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
    private DatabaseConnection databaseConnection;

    public DatabaseManager(PowerAuth plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        plugin.getLogger().info("Initializing " + dbType.toUpperCase() + " database...");

        switch (dbType) {
            case "mysql":
                databaseConnection = new MySQLConnection(plugin);
                break;
            case "postgresql":
            case "postgres":
                databaseConnection = new PostgreSQLConnection(plugin);
                break;
            case "sqlite":
            default:
                databaseConnection = new SQLiteConnection(plugin);
                break;
        }

        databaseConnection.initialize();
        createTables();
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16) NOT NULL," +
                "password_hash VARCHAR(255)," +
                "is_premium BOOLEAN DEFAULT 0," +
                "last_ip VARCHAR(45)" +
                ");";

        try (Statement stmt = databaseConnection.getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create tables!", e);
        }
    }

    public void close() {
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }

    public Connection getConnection() {
        try {
            return databaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper methods
    public boolean isRegistered(UUID uuid) {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void registerPlayer(UUID uuid, String username, String passwordHash, String ip) {
        try (PreparedStatement ps = databaseConnection.getConnection()
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
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("SELECT password_hash FROM players WHERE uuid = ?")) {
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
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("SELECT last_ip FROM players WHERE uuid = ?")) {
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
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("UPDATE players SET last_ip = ? WHERE uuid = ?")) {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPremium(UUID uuid, boolean premium) {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("UPDATE players SET is_premium = ? WHERE uuid = ?")) {
            ps.setBoolean(1, premium);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPremium(UUID uuid) {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("SELECT is_premium FROM players WHERE uuid = ?")) {
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

    public void changePassword(UUID uuid, String newHash) {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("UPDATE players SET password_hash = ? WHERE uuid = ?")) {
            ps.setString(1, newHash);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unregisterPlayer(UUID uuid) {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("DELETE FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
