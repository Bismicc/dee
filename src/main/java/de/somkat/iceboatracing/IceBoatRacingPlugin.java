package de.somkat.iceboatracing;

import de.somkat.iceboatracing.commands.IceBoatRacingCommand;
import de.somkat.iceboatracing.commands.RaceCommand;
import de.somkat.iceboatracing.config.ConfigManager;
import de.somkat.iceboatracing.database.DatabaseManager;
import de.somkat.iceboatracing.listeners.*;
import de.somkat.iceboatracing.managers.*;
import de.somkat.iceboatracing.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class IceBoatRacingPlugin extends JavaPlugin {

    private static IceBoatRacingPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ArenaManager arenaManager;
    private RaceManager raceManager;
    private PowerUpManager powerUpManager;
    private LeaderboardManager leaderboardManager;
    private GuiManager guiManager;
    private MessageUtil messageUtil;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration files first
        configManager = new ConfigManager(this);
        configManager.loadConfig(); // Load config.yml, messages.yml, arenas.yml

        // Initialize utility classes that depend on loaded configs
        messageUtil = new MessageUtil(this);

        // Initialize all core managers
        databaseManager = new DatabaseManager(this);
        arenaManager = new ArenaManager(this);
        raceManager = new RaceManager(this);
        powerUpManager = new PowerUpManager(this);
        leaderboardManager = new LeaderboardManager(this);
        guiManager = new GuiManager(this);

        // Load arenas from arenas.yml
        arenaManager.loadArenas();

        // Register commands
        getCommand("iceboatracing").setExecutor(new IceBoatRacingCommand(this));
        getCommand("race").setExecutor(new RaceCommand(this));

        // Register event listeners
        registerListeners();

        // Start GUI update loop
        guiManager.startUpdateTask();

        getLogger().info("IceBoatRacing Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanly stop all races
        if (raceManager != null) {
            raceManager.stopAllRaces();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("IceBoatRacing Plugin has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BoatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new RaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PowerUpListener(this), this);
        getServer().getPluginManager().registerEvents(new SetupListener(this), this);
    }

    // Getters
    public static IceBoatRacingPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public PowerUpManager getPowerUpManager() {
        return powerUpManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
}
