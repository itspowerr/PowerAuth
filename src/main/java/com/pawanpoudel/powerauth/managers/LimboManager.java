package com.pawanpoudel.powerauth.managers;

import com.pawanpoudel.powerauth.PowerAuth;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LimboManager {

    private final PowerAuth plugin;
    private World limboWorld;
    private Location limboSpawn;
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public LimboManager(PowerAuth plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        if (!plugin.getConfig().getBoolean("limbo.enabled", true)) {
            plugin.getLogger().info("Limbo world is disabled in config.");
            return;
        }

        String worldName = plugin.getConfig().getString("limbo.world-name", "limbo");
        boolean autoCreate = plugin.getConfig().getBoolean("limbo.auto-create", true);

        // Try to load existing world
        limboWorld = Bukkit.getWorld(worldName);

        // Create world if it doesn't exist and auto-create is enabled
        if (limboWorld == null && autoCreate) {
            plugin.getLogger().info("Creating limbo world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generator(new VoidWorldGenerator());

            limboWorld = creator.createWorld();

            if (limboWorld != null) {
                limboWorld.setDifficulty(Difficulty.PEACEFUL);
                limboWorld.setSpawnFlags(false, false);
                limboWorld.setPVP(false);
                limboWorld.setStorm(false);
                limboWorld.setThundering(false);
                limboWorld.setWeatherDuration(Integer.MAX_VALUE);
                limboWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                limboWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                limboWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                limboWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                limboWorld.setTime(6000); // Noon

                // Create spawn platform
                createSpawnPlatform();
            }
        }

        if (limboWorld != null) {
            // Set limbo spawn location from config
            double x = plugin.getConfig().getDouble("limbo.spawn-location.x", 0.5);
            double y = plugin.getConfig().getDouble("limbo.spawn-location.y", 64.0);
            double z = plugin.getConfig().getDouble("limbo.spawn-location.z", 0.5);
            float yaw = (float) plugin.getConfig().getDouble("limbo.spawn-location.yaw", 0.0);
            float pitch = (float) plugin.getConfig().getDouble("limbo.spawn-location.pitch", 0.0);

            limboSpawn = new Location(limboWorld, x, y, z, yaw, pitch);
            plugin.getLogger().info("Limbo world initialized successfully!");
        } else {
            plugin.getLogger().warning("Failed to initialize limbo world!");
        }
    }

    private void createSpawnPlatform() {
        if (limboWorld == null)
            return;

        // Create a 5x5 platform at spawn
        int platformY = 63;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                limboWorld.getBlockAt(x, platformY, z).setType(Material.STONE);
            }
        }

        plugin.getLogger().info("Created spawn platform in limbo world.");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("limbo.enabled", true) && limboWorld != null;
    }

    public void sendToLimbo(Player player) {
        if (!isEnabled())
            return;

        // Store previous location
        previousLocations.put(player.getUniqueId(), player.getLocation());

        // Teleport to limbo
        player.teleport(limboSpawn);
        player.sendMessage(ChatColor.YELLOW + "Please authenticate to continue.");
    }

    public void sendToMainWorld(Player player) {
        if (!isEnabled())
            return;

        String teleportMode = plugin.getConfig().getString("limbo.after-login.teleport-to", "spawn");
        Location destination;

        switch (teleportMode.toLowerCase()) {
            case "last-location":
                destination = previousLocations.remove(player.getUniqueId());
                if (destination == null || destination.getWorld() == limboWorld) {
                    destination = getMainWorldSpawn();
                }
                break;
            case "bed":
                destination = player.getBedSpawnLocation();
                if (destination == null) {
                    destination = getMainWorldSpawn();
                }
                break;
            case "spawn":
            default:
                destination = getMainWorldSpawn();
                break;
        }

        player.teleport(destination);
    }

    private Location getMainWorldSpawn() {
        World mainWorld = Bukkit.getWorlds().get(0); // First world is usually the main world
        return mainWorld.getSpawnLocation();
    }

    public void cleanup(UUID uuid) {
        previousLocations.remove(uuid);
    }

    public boolean isInLimbo(Player player) {
        return isEnabled() && player.getWorld().equals(limboWorld);
    }

    // Void world generator for limbo
    private static class VoidWorldGenerator extends org.bukkit.generator.ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, java.util.Random random, int x, int z, BiomeGrid biome) {
            return createChunkData(world);
        }
    }
}
