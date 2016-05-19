package io.github.multicubeuk.multicubechunkloader.gui;

import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import io.github.multicubeuk.multicubechunkloader.ChunkLoaderConfiguration;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MainAdminMenuOptionClickEventHandler implements IconMenu.OptionClickEventHandler {
    @Override
    public void onOptionClick(IconMenu.OptionClickEvent event) {
        ChunkLoaderConfiguration config = MultiCubeChunkLoader.instance.getConfiguration();

        Map<Integer, Material> type = new HashMap<>(3);
        type.put(3, config.getPersonalLoaderBlock());
        type.put(4, config.getWorldLoaderBlock());
        type.put(5, config.getCreativeLoaderBlock());

        Bukkit.getScheduler().scheduleSyncDelayedTask(MultiCubeChunkLoader.instance, new Runnable() {
            private Player player;
            private Material material;

            @Override
            public void run() {
                MultiCubeChunkLoader plugin = MultiCubeChunkLoader.instance;
                ChunkLoader.ChunkType type = plugin.getChunkTypeFromMaterial(material);

                plugin.getChunkLoaderIconMenu(type).open(player);
            }

            public Runnable init(Player player, Material material) {
                this.player = player;
                this.material = material;
                return (this);
            }
        }.init(event.getPlayer(), type.get(event.getPosition())), 2L);


        event.setWillClose(true);
    }
}
