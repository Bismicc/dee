package de.somkat.iceboatracing.database;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private final IceBoatRacingPlugin plugin;
    private Connection connection;
    private String host, database, username, password;
    private int port;
    private boolean useMySQL;

    public DatabaseManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration config = plugin.getConfig();

        String databaseType = config.getString("database.type", "sqlite").toLowerCase();
        useMySQL = databaseType.equals("mysql") || databaseType.equals("mariadb");

        if (useMySQL) {
            host = config.getString("database.host", "localhost");
            port = config.getInt("database.port", 3306);
            database = config.getString("database.database", "iceboatracing");
            username = config.getString("database.username", "root");
            password = config.getString("database.password", "");
        }

        connect();
        createTables();
    }

    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            if (useMySQL) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
                connection = DriverManager.getConnection(url, username, password);
            } else {
                Class.forName("org.sqlite.JDBC");
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "database.db";
                connection = DriverManager.getConnection(url);
            }

            plugin.getLogger().info("Database connected successfully!");

        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() {
        String[] tables = {
                // Player statistics table
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "id " + (useMySQL ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT") + "," +
                        "player_uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "arena_name VARCHAR(32) NOT NULL," +
                        "races_played INT DEFAULT 0," +
                        "wins INT DEFAULT 0," +
                        "best_time BIGINT DEFAULT 0," +
                        "total_time BIGINT DEFAULT 0," +
                        "average_position DOUBLE DEFAULT 0," +
                        "UNIQUE(player_uuid, arena_name)" +
                        ")",

                // Individual race results table
                "CREATE TABLE IF NOT EXISTS player_race_stats (" +
                        "id " + (useMySQL ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT") + "," +
                        "player_uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "arena_name VARCHAR(32) NOT NULL," +
                        "position INT NOT NULL," +
                        "finish_time BIGINT NOT NULL," +
                        "checkpoints_reached INT NOT NULL," +
                        "race_date BIGINT NOT NULL" +
                        ")"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
            plugin.getLogger().info("Database tables created successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check connection status: " + e.getMessage());
            connect();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}