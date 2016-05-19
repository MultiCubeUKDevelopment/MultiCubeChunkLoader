package io.github.multicubeuk.multicubechunkloader;

import io.github.multicubeuk.multicubechunkloader.util.UUIDUtils;
import net.kaikk.mc.bcl.forge.BCLForge;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Sweeck on 26/06/2015.
 */
public class ChunkLoader {
    private final UUID owner;
    private final String ownerName;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final Date createdDate;
    private final MultiCubeChunkLoader plugin;
    private int id;
    private ChunkType chunkType;
    private int size;
    private boolean loaded = false;
    private Block block;
    private List<Chunk> chunks = new ArrayList<>();

    public ChunkLoader(MultiCubeChunkLoader plugin, int id, UUID owner, ChunkType chunkType, String world, int x, int y, int z, int size, Date createdDate) {
        this(plugin, id, owner, chunkType, String.format("%s:%s:%s:%s", world, x, y, z), size, createdDate);
    }

    public ChunkLoader(MultiCubeChunkLoader plugin, int id, UUID owner, ChunkType chunkType, String locationString, int size, Date createdDate) {
        String[] location = locationString.split(":");

        if (location.length != 4) {
            plugin.getLogger().severe("ChunkLoader has an invalid location string and will not be loaded!");
            throw new IllegalArgumentException(String.format("Invalid location format (%s)!", locationString));
        }

        this.world = location[0];
        this.x = Integer.parseInt(location[1]);
        this.y = Integer.parseInt(location[2]);
        this.z = Integer.parseInt(location[3]);

        this.plugin = plugin;
        this.id = id;
        this.owner = owner;
        this.chunkType = chunkType;
        this.size = size;
        this.createdDate = createdDate;

        this.ownerName = plugin.getPlayerInfo(owner).getName();
        this.block = plugin.getServer().createWorld(new WorldCreator(this.world)).getBlockAt(this.x, this.y, this.z);

    }

    public ChunkLoader(Player player, ChunkType type, int size) {
        this(MultiCubeChunkLoader.instance, -1, player.getUniqueId(), type, player.getLocation().getWorld().getName(), player.getLocation().getBlockX(), player.getLocation().getBlockY() - 1, player.getLocation().getBlockZ(), size, new Date());

    }

    public static String getLocationString(Block block) {
        return getLocationString(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static String getLocationString(String world, int x, int y, int z) {
        return String.format("%s:%s:%s:%s", world, x, y, z);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getOwner() {
        return owner;
    }

    public ChunkType getChunkType() {
        return chunkType;
    }

    public void setChunkType(ChunkType chunkType) {
        this.chunkType = chunkType;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isExpired() {

        if (plugin.getConfiguration().expireChunks()) {
            Date compareDate;
            if (plugin.getConfiguration().isAbsoluteChunkExpiry()) {
                compareDate = this.createdDate;
            } else {
                compareDate = new Date(UUIDUtils.getPlayer(owner).getLastPlayed() * 1000);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, -(plugin.getConfiguration().getChunkExpiryDays()));
            if (compareDate.compareTo(cal.getTime()) == -1)
                return true;
        }

        return false;
    }

    public boolean unload() {

        if (!loaded)
            return true;

        List<ChunkLoader> overlaps = plugin.getChunkLoaders()
                .stream()
                .filter(c -> !c.equals(this))
                .filter(c -> c.getRegion().intersects(this.getRegion()))
                .collect(Collectors.toList());

        Chunk chunk = getChunk();
        if (chunk == null)
            return false;

        int left = chunk.getX() - ((int) (Math.sqrt(this.size) - 1) / 2);
        int bottom = chunk.getZ() - ((int) (Math.sqrt(this.size) - 1) / 2);
        int right = left + (int) Math.sqrt(this.size) - 1;
        int top = bottom + (int) Math.sqrt(this.size) - 1;

        for (int x = left; x <= right; x++) {
            for (int z = bottom; z <= top; z++) {
                boolean remove = true;
                for (ChunkLoader cl : overlaps) {
                    if (cl.getRegion().contains(x, z)) {
                        plugin.getLogger().info(String.format("Chunk intersects with chunk loader id:%s and will not be removed.", cl.getId()));
                        remove = false;
                        break;
                    }
                }

                if (remove) {
                    plugin.getLogger().info(String.format("Unloading chunk at location %s:%s,%s", this.world, x, z));
                    BCLForge.instance.unloadChunk(this.world, x, z);
                }
            }
        }

        loaded = false;

//        plugin.getServer().getWorld(this.world).playEffect(this.block.getLocation(), Effect.SMOKE, 10);

        plugin.getLogger().info(String.format("Unloaded %s %s chunks at block location %s", size, chunkType, getLocationString()));
        return true;
    }

    public boolean load() {
        this.block = plugin.getServer().getWorld(this.world).getBlockAt(this.x, this.y, this.z);

        if (!plugin.isChunkLoaderMaterial(this.block.getType())) {
            plugin.getLogger().severe("Chunk Loader location is at an invalid block and will be deleted!");
            delete();
            return false;
        }

        if (getChunkType() == ChunkType.WORLD && isExpired()) {
            plugin.getLogger().info("Chunk Loader has expired and will be deleted!");
            plugin.deleteChunkLoader(this);
            return false;
        }

        Chunk chunk = getChunk();

        if (chunk == null)
            return false;

        int left = chunk.getX() - ((int) (Math.sqrt(this.size) - 1) / 2);
        int bottom = chunk.getZ() - ((int) (Math.sqrt(this.size) - 1) / 2);
        int right = left + (int) Math.sqrt(this.size) - 1;
        int top = bottom + (int) Math.sqrt(this.size) - 1;

        for (int x = left; x <= right; x++) {
            for (int z = bottom; z <= top; z++) {
                BCLForge.instance.loadChunk(this.world, x, z);
            }
        }

        plugin.getServer().getWorld(this.world).playEffect(this.block.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
        loaded = true;

        plugin.getLogger().info(String.format("Loaded %s chunks at block location %s", size, getLocationString()));
        return true;
    }

    public void delete() {
        if (loaded) {
            unload();
        }

        plugin.deleteChunkLoader(this);

    }

    public void resize(int newSize) {
        this.unload();

        Block block = this.getBlock();
        block.setType(plugin.getChunkLoaderMaterial(this.getChunkType()));

        this.setSize(newSize);
        this.load();

        plugin.updateChunkLoader(this);
    }

    public boolean equals(ChunkLoader cl) {
        return cl.getLocationString().equalsIgnoreCase(this.getLocationString());
    }

    private Chunk getChunk() {
        World world = plugin.getServer().getWorld(this.world);

        if (world == null) {
            plugin.getLogger().warning("ChunkLoader world does not exist. Deleting entry!");
            delete();
            return null;
        }

        Block block = getBlock();
        if (
                block == null || (
                        block.getType() != plugin.getConfiguration().getPersonalLoaderBlock() &&
                        block.getType() != plugin.getConfiguration().getWorldLoaderBlock() &&
                        block.getType() != plugin.getConfiguration().getCreativeLoaderBlock()
                )) {
            plugin.getLogger().warning("ChunkLoader block no longer exist. Deleting entry!");
            delete();
            return null;
        }

        return block.getChunk();
    }

    public String getLocationString() {
        return ChunkLoader.getLocationString(this.world, this.x, this.y, this.z);
    }

    private Rectangle getRegion() {
        Chunk chunk = getChunk();
        if (chunk == null)
            return new Rectangle(0, 0, 0, 0);

        int x = chunk.getX() - ((int) (Math.sqrt(this.size) - 1) / 2);
        int y = chunk.getZ() - ((int) (Math.sqrt(this.size) - 1) / 2);

        return new Rectangle(x, y, (int) Math.sqrt(this.size), (int) Math.sqrt(this.size));
    }

    public enum ChunkType {
        PERSONAL,
        WORLD,
        CREATIVE
    }
}

