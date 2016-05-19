package io.github.multicubeuk.multicubechunkloader;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ChunkLoaderConfiguration {

    public static String WAND_DISPLAY_NAME = "ChunkLoader Wand";
    public static String WAND_LORE = "Right click to place or edit a Chunk Loader";

    private final boolean _expireChunks;
    private final boolean absoluteChunkExpiry;
    private final int chunkExpiryDays;

    private final boolean _useMySql;
    private final String mySqlServer;
    private final String mySqlPort;
    private final String mySqlDatabase;
    private final String mySqlTablePrefix;
    private final String mySqlUsername;
    private final String mySqlPassword;

    private final boolean _limitDimensions;
    private final ArrayList<String> allowedDimensions;

    private final Material personalLoaderBlock;
    private final Material worldLoaderBlock;
    private final Material creativeLoaderBlock;
    private final Material chunkloaderActivator;
    private final Enchantment chunkloaderActivatorEnchant;

    private final HashMap<String, Integer> worldChunks;
    private final HashMap<String, Integer> personalChunks;

    private final List<Material> validBlocks;

    public ChunkLoaderConfiguration(MultiCubeChunkLoader plugin) {
        plugin.saveDefaultConfig();

        FileConfiguration config = plugin.getConfig();

        this._expireChunks = config.getBoolean("expire-chunks.enable", false);
        this.absoluteChunkExpiry = config.getBoolean("expire-chunks.absolute-expiry", false);
        this.chunkExpiryDays = config.getInt("expire-chunks.expiry-days", 7);

        this._useMySql = config.getBoolean("use-mysql", false);
        this.mySqlServer = config.getString("database.server", "");
        this.mySqlPort = config.getString("database.port", "");
        this.mySqlDatabase = config.getString("database.database", "");
        this.mySqlTablePrefix = config.getString("database.table-prefix", "");
        this.mySqlUsername = config.getString("database.username", "");
        this.mySqlPassword = config.getString("database.password", "");

        this.personalLoaderBlock = Material.valueOf(config.getString("chunkloader-materials.personal", "IRON_BLOCK"));
        this.worldLoaderBlock = Material.valueOf(config.getString("chunkloader-materials.world", "DIAMOND_BLOCK"));
        this.creativeLoaderBlock = Material.valueOf(config.getString("chunkloader-materials.creative", "EMERALD_BLOCK"));
        this.chunkloaderActivator = Material.valueOf(config.getString("chunkloader-materials.activator", "STICK"));

        String enchant = config.getString("chunkloader-materials.activator-enchant");
        if (enchant != null && !enchant.isEmpty())
            this.chunkloaderActivatorEnchant = Enchantment.getByName(enchant);
        else
            this.chunkloaderActivatorEnchant = null;

        this._limitDimensions = config.getBoolean("limit-dimensions.enable", false);
        this.allowedDimensions = new ArrayList<>();
        if (this._limitDimensions)
            for (Object dimension : config.getList("limit-dimensions.dimensions"))
                this.allowedDimensions.add(dimension.toString());

        // Personal limits
        personalChunks = new HashMap<>();
        boolean foundDefault = false;
        for (Map.Entry<String, Object> item : config.getConfigurationSection("initial-chunks.personal").getValues(false).entrySet()) {
            if (item.getKey().equalsIgnoreCase("default"))
                foundDefault = true;

            personalChunks.put(item.getKey(), (Integer) item.getValue());
        }

        if (!foundDefault)
            personalChunks.put("default", 0);

        // World limits
        worldChunks = new HashMap<>();
        foundDefault = false;
        for (Map.Entry<String, Object> item : config.getConfigurationSection("initial-chunks.world").getValues(false).entrySet()) {
            if (item.getKey().equalsIgnoreCase("default"))
                foundDefault = true;

            worldChunks.put(item.getKey(), (Integer) item.getValue());
        }

        if (!foundDefault)
            worldChunks.put("default", 0);

        // Valid blocks
        validBlocks = new ArrayList<>();
        for (String value : config.getStringList("chunkloader-materials.valid-blocks")) {
            try {
                validBlocks.add(Material.getMaterial(value));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Unknown material in valid blocks config list!", ex);
            }
        }
    }

    public boolean expireChunks() {
        return _expireChunks;
    }

    public boolean isAbsoluteChunkExpiry() {
        return absoluteChunkExpiry;
    }

    public int getChunkExpiryDays() {
        return chunkExpiryDays;
    }

    public boolean useMySql() {
        return _useMySql;
    }

    public String getMySqlServer() {
        return mySqlServer;
    }

    public String getMySqlPort() {
        return mySqlPort;
    }

    public String getMySqlDatabase() {
        return mySqlDatabase;
    }

    public String getMySqlTablePrefix() {
        return mySqlTablePrefix;
    }

    public String getMySqlUsername() {
        return mySqlUsername;
    }

    public String getMySqlPassword() {
        return mySqlPassword;
    }

    public boolean limitDimensions() {
        return _limitDimensions;
    }

    public ArrayList<String> getAllowedDimensions() {
        return allowedDimensions;
    }

    public Material getPersonalLoaderBlock() {
        return personalLoaderBlock;
    }

    public Material getWorldLoaderBlock() {
        return worldLoaderBlock;
    }

    public Material getCreativeLoaderBlock() {
        return creativeLoaderBlock;
    }

    public Material getChunkloaderActivator() {
        return chunkloaderActivator;
    }

    public HashMap<String, Integer> getWorldChunks() {
        return worldChunks;
    }

    public HashMap<String, Integer> getPersonalChunks() {
        return personalChunks;
    }

    public List<Material> getValidBlocks() {
        return validBlocks;
    }

    public Enchantment getChunkloaderActivatorEnchant() {
        return chunkloaderActivatorEnchant;
    }
}
