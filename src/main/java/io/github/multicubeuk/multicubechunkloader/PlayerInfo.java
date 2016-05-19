package io.github.multicubeuk.multicubechunkloader;

import java.util.UUID;

/**
 * Created by Christian on 25/06/2015.
 */
public class PlayerInfo {
    private UUID uuid;
    private String name;
    private int worldChunks;
    private int personalChunks;

    public PlayerInfo(UUID uuid, String name, int worldChunks, int personalChunks) {
        this.uuid = uuid;
        this.name = name;
        this.worldChunks = worldChunks;
        this.personalChunks = personalChunks;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWorldChunks() {
        return worldChunks;
    }

    public void setWorldChunks(int worldChunks) {
        this.worldChunks = worldChunks;
    }

    public void addWorldChunks(int worldChunks) {
        this.worldChunks += worldChunks;
    }

    public int getPersonalChunks() {
        return personalChunks;
    }

    public void setPersonalChunks(int personalChunks) {
        this.personalChunks = personalChunks;
    }

    public void addPersonalChunks(int personalChunks) {
        this.personalChunks += personalChunks;
    }
}
