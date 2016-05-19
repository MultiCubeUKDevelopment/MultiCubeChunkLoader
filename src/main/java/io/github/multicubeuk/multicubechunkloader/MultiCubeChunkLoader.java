package io.github.multicubeuk.multicubechunkloader;

import io.github.multicubeuk.multicubechunkloader.commands.MultiCubeChunkLoaderAdminCommand;
import io.github.multicubeuk.multicubechunkloader.commands.MultiCubeChunkLoaderCommand;
import io.github.multicubeuk.multicubechunkloader.database.DataProvider;
import io.github.multicubeuk.multicubechunkloader.database.MySqlDataProvider;
import io.github.multicubeuk.multicubechunkloader.events.MultiCubeChunkLoaderEvents;
import io.github.multicubeuk.multicubechunkloader.gui.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MultiCubeChunkLoader extends JavaPlugin {

    public static MultiCubeChunkLoader instance;
    private ChunkLoaderConfiguration configuration;
    private DataProvider data;
    private List<ChunkLoader> chunkLoaders;
    private IconMenu menuMain;
    private IconMenu menuMainAdmin;
    private IconMenu menuPersonal;
    private IconMenu menuWorld;
    private IconMenu menuCreative;
    private Map<UUID, ChunkLoader> activeChunkLoader;
    private Map<UUID, Location> activeLocation;

    public MultiCubeChunkLoader() {
        super();
    }

    public ChunkLoaderConfiguration getConfiguration() {
        if (configuration == null)
            configuration = new ChunkLoaderConfiguration(this);
        return configuration;
    }

    public List<ChunkLoader> getChunkLoaders() {
        return chunkLoaders;
    }

    public IconMenu getMenuMain() {
        return menuMain;
    }

    public IconMenu getMenuMainAdmin() {
        return menuMainAdmin;
    }

    public IconMenu getMenuPersonal() {
        return menuPersonal;
    }

    public IconMenu getMenuWorld() {
        return menuWorld;
    }

    public IconMenu getMenuCreative() {
        return menuCreative;
    }

    public ChunkLoader getActiveChunkLoader(UUID uuid) {
        return activeChunkLoader.get(uuid);
    }

    public PlayerInfo getPlayerInfo(UUID uuid) {
        return data.getPlayerInfo(uuid);
    }

    public Location getActiveLocation(UUID uuid) {
        return activeLocation.get(uuid);
    }

    public void setActiveChunkLoader(UUID uuid, ChunkLoader chunkLoader) {
        this.activeChunkLoader.put(uuid, chunkLoader);
    }

    public void setActiveLocation(UUID uuid, Location location) {
        this.activeLocation.put(uuid, location);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling MultiCubeChunkLoader");

        if (menuMain != null)
            menuMain.destroy();

        if (menuMainAdmin != null)
            menuMainAdmin.destroy();

        if (menuPersonal != null)
            menuPersonal.destroy();

        if (menuWorld != null)
            menuWorld.destroy();

        if (menuCreative != null)
            menuCreative.destroy();

        if (chunkLoaders != null)
            chunkLoaders
                    .stream()
                    .forEach(c -> c.unload());

        configuration = null;

        activeChunkLoader = null;
        activeLocation = null;
        chunkLoaders = null;

        instance = null;
    }

    @Override
    public void onEnable() {
        try {
            Class.forName("net.kaikk.mc.bcl.forge.BCLForge");
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.SEVERE, "BCLForge not found. This plugin depends on BCLForge which can be downloaded from http://kaikk.net/mc/", ex);
            onDisable();
            return;
        }

        instance = this;
        chunkLoaders = new ArrayList<>();
        activeChunkLoader = new HashMap<>();
        activeLocation = new HashMap<>();

        configuration = new ChunkLoaderConfiguration(this);
        if (configuration.useMySql())
            this.data = new MySqlDataProvider(this);

        initializeMenus();
        loadWorldChunks();

        for (Player p : getServer().getOnlinePlayers()) {
            loadPersonalChunks(p.getUniqueId());
        }

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new MultiCubeChunkLoaderEvents(this), this);

        getCommand("multicubechunkloader").setExecutor(new MultiCubeChunkLoaderCommand(this));
        getCommand("multicubechunkloaderadmin").setExecutor(new MultiCubeChunkLoaderAdminCommand(this));

        getLogger().info("Successfully enabled MultiCubeChunkLoader");
    }

    public void reload()
    {
        getLogger().info("I tried but couldn't be bothered <3 Sweeck");
    }

    public void loadWorldChunks() {
        List<ChunkLoader> chunks = data.getWorldChunks();
        int chunkCount = 0;

        for (ChunkLoader cl : chunks) {
            if (!cl.load())
                continue;

            chunkCount += cl.getSize();
            chunkLoaders.add(cl);
        }

        getLogger().info(String.format("Loaded %s world chunk loaders, totalling %s chunks", chunks.size(), chunkCount));
    }

    public void loadPersonalChunks(UUID uuid) {
        List<ChunkLoader> chunks = data.getPersonalChunks(uuid);
        int chunkCount = 0;

        for (ChunkLoader cl : chunks) {
            if (!cl.load())
                continue;

            chunkCount += cl.getSize();
            getChunkLoaders().add(cl);
        }

        getLogger().info(String.format("Loaded %s personal chunk loaders, totalling %s chunks", chunks.size(), chunkCount));
    }

    public void unloadPersonalChunks(UUID uuid) {
        List<ChunkLoader> chunks = chunkLoaders.stream().filter(
                c -> c.getOwner().equals(uuid) &&
                        c.getChunkType() == ChunkLoader.ChunkType.PERSONAL
        ).collect(Collectors.toList());

        int chunkCount = 0;

        for (ChunkLoader c : chunks) {
            chunkCount += c.getSize();
            c.unload();
        }

        getChunkLoaders().removeAll(chunks);

        getLogger().info(String.format("Unloaded %s personal chunk loaders, totalling %s chunks", chunks.size(), chunkCount));
    }

    public void addChunkLoader(ChunkLoader chunkLoader) {
        Block block = getServer().getWorld(chunkLoader.getWorld()).getBlockAt(chunkLoader.getX(), chunkLoader.getY(), chunkLoader.getZ());

        ChunkLoader existing = chunkLoaders.stream().filter(
                c -> c.getBlock().equals(block)
        ).findFirst().orElse(null);

        if (existing != null)
            existing.delete();

        chunkLoader.setBlock(block);

        block.setType(getChunkLoaderMaterial(chunkLoader.getChunkType()));

        getChunkLoaders().add(chunkLoader);

        chunkLoader.load();

        data.saveChunkLoader(chunkLoader);

        Player player = getServer().getPlayer(chunkLoader.getOwner());
        if (player != null && player.isOnline())
            player.sendMessage(String.format("%sSuccessfully placed %s chunk loader with and ID of %s and %s chunks", ChatColor.GOLD, chunkLoader.getChunkType(), chunkLoader.getId(), chunkLoader.getSize()));
    }

    public void updateChunkLoader(ChunkLoader chunkLoader) {
        data.saveChunkLoader(chunkLoader);

        Player player = getServer().getPlayer(chunkLoader.getOwner());
        if (player != null && player.isOnline())
            player.sendMessage(String.format("%sSuccessfully updated your chunk loader. Chunks loaded are now %s", ChatColor.GOLD, chunkLoader.getSize()));
    }

    public void updatePlayerInfo(PlayerInfo playerInfo) {
        data.updatePlayerInfo(playerInfo);
    }

    public void deleteChunkLoader(ChunkLoader chunkLoader) {
        if (chunkLoader.isLoaded())
            chunkLoader.delete();

        chunkLoader.getBlock().setType(Material.AIR);

        chunkLoaders.remove(chunkLoader);
        data.deleteChunkLoader(chunkLoader);
    }

    public List<ChunkLoader> getPersonalChunks(UUID uuid) {
        return data.getPersonalChunks(uuid);
    }

    public List<ChunkLoader> getPersonalChunks(String world) {
        return data.getPersonalChunks(world);
    }

    public List<ChunkLoader> getPersonalChunks() {
        return data.getPersonalChunks();
    }

    public ChunkLoader getChunkLoader(int id) {
        return data.getChunkLoader(id);
    }

    public ChunkLoader.ChunkType getChunkTypeFromMaterial(Material material) {
        if (material == configuration.getPersonalLoaderBlock())
            return ChunkLoader.ChunkType.PERSONAL;
        if (material == configuration.getWorldLoaderBlock())
            return ChunkLoader.ChunkType.WORLD;
        if (material == configuration.getCreativeLoaderBlock())
            return ChunkLoader.ChunkType.CREATIVE;

        return null;
    }

    public IconMenu getChunkLoaderIconMenu(ChunkLoader.ChunkType type) {
        switch (type) {
            case PERSONAL:
                return getMenuPersonal();
            case WORLD:
                return getMenuWorld();
            case CREATIVE:
                return getMenuCreative();
            default:
                return null;
        }
    }

    public IconMenu getChunkLoaderIconMenu(ChunkLoader chunkLoader) {
        return getChunkLoaderIconMenu(chunkLoader.getChunkType());
    }

    public IconMenu getChunkLoaderIconMenu(Material material) {
        ChunkLoader.ChunkType type = getChunkTypeFromMaterial(material);
        return getChunkLoaderIconMenu(type);
    }

    public void openMainMenu(Player player) {
        if (player.hasPermission("multicubechunkloader.admin.place") || player.isOp())
            this.getMenuMainAdmin().open(player);
        else
            this.getMenuMain().open(player);

    }

    public boolean isChunkLoaderMaterial(Material material) {
        return getChunkTypeFromMaterial(material) != null;
    }

    public ChunkLoader getChunkLoaderAtLocation(Location location) {
        String locationString = String.format("%s:%s:%s:%s", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return data.getChunkLoaderAtLocation(locationString);
    }

    public UUID getUUID(String name) {
        return data.getUUID(name);
    }

    public Material getChunkLoaderMaterial(ChunkLoader.ChunkType type) {
        switch (type) {
            case PERSONAL:
                return configuration.getPersonalLoaderBlock();
            case WORLD:
                return configuration.getWorldLoaderBlock();
            case CREATIVE:
                return configuration.getCreativeLoaderBlock();
            default:
                return null;
        }
    }

    public boolean isAllowedWorld(Player player) {
        if (getConfiguration().limitDimensions()) {
            String world = player.getWorld().getName();
            String allowedWorld = getConfiguration().getAllowedDimensions()
                    .stream()
                    .filter(w -> w.equalsIgnoreCase(world))
                    .findFirst().orElse(null);

            if (allowedWorld == null) {
                player.sendMessage(ChatColor.RED + "Chunk Loaders are not allowed to be placed in this dimension!");
                return false;
            }
        }

        return true;
    }

    private void initializeMenus() {
        menuMain = new IconMenu("Chunk Loader - Main Menu", 9, new MainMenuOptionClickEventHandler(), this)
                .setOption(3, new ItemStack(configuration.getPersonalLoaderBlock(), 1), "Personal chunk loader", "Active when you are online.")
                .setOption(4, new ItemStack(configuration.getWorldLoaderBlock(), 1), "World chunk loader", "Always active.");

        menuMainAdmin = new IconMenu("Chunk Loader - Main Menu", 9, new MainMenuOptionClickEventHandler(), this)
                .setOption(3, new ItemStack(configuration.getPersonalLoaderBlock(), 1), "Personal chunk loader", "Active when you are online.")
                .setOption(4, new ItemStack(configuration.getWorldLoaderBlock(), 1), "World chunk loader", "Always active.")
                .setOption(5, new ItemStack(configuration.getCreativeLoaderBlock(), 1), "Creative chunk loader", "Always active and never expires.");

        menuPersonal = new IconMenu("Chunk Loader - Personal", 9, new PersonalOptionClickEventHandler(), this)
                .setOption(2, new ItemStack(configuration.getPersonalLoaderBlock(), 1), "1x1 chunks", "Loads 1 chunk.")
                .setOption(3, new ItemStack(configuration.getPersonalLoaderBlock(), 3), "3x3 chunks", "Loads 9 chunks.")
                .setOption(4, new ItemStack(configuration.getPersonalLoaderBlock(), 5), "5x5 chunks", "Loads 25 chunks.")
                .setOption(5, new ItemStack(configuration.getPersonalLoaderBlock(), 7), "7x7 chunks", "Loads 49 chunks.")
                .setOption(6, new ItemStack(configuration.getPersonalLoaderBlock(), 9), "9x9 chunks", "Loads 81 chunks.");

        menuWorld = new IconMenu("Chunk Loader - World", 9, new WorldOptionClickEventHandler(), this)
                .setOption(2, new ItemStack(configuration.getWorldLoaderBlock(), 1), "1x1 chunks", "Loads 1 chunk.")
                .setOption(3, new ItemStack(configuration.getWorldLoaderBlock(), 3), "3x3 chunks", "Loads 9 chunks.")
                .setOption(4, new ItemStack(configuration.getWorldLoaderBlock(), 5), "5x5 chunks", "Loads 25 chunks.")
                .setOption(5, new ItemStack(configuration.getWorldLoaderBlock(), 7), "7x7 chunks", "Loads 49 chunks.")
                .setOption(6, new ItemStack(configuration.getWorldLoaderBlock(), 9), "9x9 chunks", "Loads 81 chunks.");

        menuCreative = new IconMenu("Chunk Loader - Creative", 9, new CreativeOptionClickEventHandler(), this)
                .setOption(2, new ItemStack(configuration.getCreativeLoaderBlock(), 1), "1x1 chunks", "Loads 1 chunk.")
                .setOption(3, new ItemStack(configuration.getCreativeLoaderBlock(), 3), "3x3 chunks", "Loads 9 chunks.")
                .setOption(4, new ItemStack(configuration.getCreativeLoaderBlock(), 5), "5x5 chunks", "Loads 25 chunks.")
                .setOption(5, new ItemStack(configuration.getCreativeLoaderBlock(), 7), "7x7 chunks", "Loads 49 chunks.")
                .setOption(6, new ItemStack(configuration.getCreativeLoaderBlock(), 9), "9x9 chunks", "Loads 81 chunks.");
    }

}

