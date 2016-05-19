package io.github.multicubeuk.multicubechunkloader.database;

import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;

import java.util.List;
import java.util.UUID;

public abstract class DataProvider {
    public MultiCubeChunkLoader plugin;

    public DataProvider(MultiCubeChunkLoader plugin) {
        this.plugin = plugin;

    }

    public abstract List<ChunkLoader> getWorldChunks();

    public abstract List<ChunkLoader> getPersonalChunks(UUID uuid);

    public abstract List<ChunkLoader> getPersonalChunks(String world);

    public abstract List<ChunkLoader> getPersonalChunks();

    public abstract ChunkLoader getChunkLoader(int id);

    public abstract PlayerInfo getPlayerInfo(UUID uuid);

    public abstract UUID getUUID(String name);

    public abstract void saveChunkLoader(ChunkLoader chunkLoader);

    public abstract ChunkLoader getChunkLoaderAtLocation(String locationString);

    public abstract void deleteChunkLoader(ChunkLoader chunkLoader);

    public abstract void updatePlayerInfo(PlayerInfo playerInfo);
}
