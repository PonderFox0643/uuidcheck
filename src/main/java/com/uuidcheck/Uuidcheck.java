package com.uuidcheck;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Uuidcheck extends JavaPlugin implements Listener {

    private static final String CREATE_DATABASE_QUERY = "CREATE DATABASE IF NOT EXISTS %s";
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS players (" +
            "uuid VARCHAR(36) PRIMARY KEY, " +
            "name VARCHAR(16), " +
            "first_login_ip VARCHAR(45), " +
            "last_login_ip VARCHAR(45)" +
            ")";
    private static final String SELECT_PLAYER_QUERY = "SELECT uuid, first_login_ip FROM players WHERE name = ?";
    private static final String UPDATE_PLAYER_QUERY = "UPDATE players SET name = ?, last_login_ip = ? WHERE uuid = ?";
    private static final String INSERT_PLAYER_QUERY = "INSERT INTO players (uuid, name, first_login_ip, last_login_ip) VALUES (?, ?, ?, ?)";

    private HikariDataSource dataSource;
    private Logger logger;

    @Override
    public void onEnable() {
        // Load configuration from config.yml
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Initialize logger
        getLogger().info("防相同名称不同uuid插件已启用，想法来自PonderFox0643，编写来自ChatGPT，开源地址：https://github.com/PonderFox0643/uuidcheck");
        logger = getLogger();

        // Initialize data source
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");
        String database = getConfig().getString("mysql.database");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);

        // Create database if not exists
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(String.format(CREATE_DATABASE_QUERY, database))) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Unable to create database: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Create players table if not exists
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_QUERY)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Unable to create table: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Close data source
        getLogger().info("防相同名称不同uuid插件已卸载成功");
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_QUERY)) {
            statement.setString(1, playerName);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    UUID dbUuid = UUID.fromString(result.getString("uuid"));
                    String firstLoginIp = result.getString("first_login_ip");
                    if (dbUuid.equals(player.getUniqueId())) {
                        // Player is allowed to join
                        try (PreparedStatement updateIpStatement = connection.prepareStatement("UPDATE players SET last_login_ip = ? WHERE uuid = ?")) {
                            updateIpStatement.setString(1, ip);
                            updateIpStatement.setString(2, uuid);
                            updateIpStatement.executeUpdate();
                        }
                        return;
                    } else {
                        // Player with same name but different UUID is already logged in
                        event.setJoinMessage(null);
                        player.kickPlayer(ChatColor.RED + "登录方式不对：这里出现了与你名字一样但uuid不同的人");
                        return;
                    }
                }
            }

            // Update player name and IP in database
            try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE_PLAYER_QUERY)) {
                updateStatement.setString(1, playerName);
                updateStatement.setString(2, ip);
                updateStatement.setString(3, uuid);
                int updatedRows = updateStatement.executeUpdate();
                if (updatedRows == 0) {
                    // Player does not exist in database, insert new row
                    try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_PLAYER_QUERY)) {
                        insertStatement.setString(1, uuid);
                        insertStatement.setString(2, playerName);
                        insertStatement.setString(3, ip);
                        insertStatement.setString(4, ip);
                        insertStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error while checking player login: " + e.getMessage(), e);
        }
    }
}
