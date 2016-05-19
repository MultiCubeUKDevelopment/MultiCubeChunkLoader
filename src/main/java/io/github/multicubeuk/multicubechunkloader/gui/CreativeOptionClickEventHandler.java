package io.github.multicubeuk.multicubechunkloader.gui;

import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

public class CreativeOptionClickEventHandler implements IconMenu.OptionClickEventHandler {
    private MultiCubeChunkLoader plugin;

    public CreativeOptionClickEventHandler() {
        super();

        this.plugin = MultiCubeChunkLoader.instance;
    }

    @Override
    public void onOptionClick(IconMenu.OptionClickEvent event) {
        int size;
        Player player = event.getPlayer();
        UUID uuid = event.getPlayer().getUniqueId();

        switch (event.getPosition())
        {
            case 2:
                size = 1;
                break;
            case 3:
                size = 9;
                break;
            case 4:
                size = 25;
                break;
            case 5:
                size = 49;
                break;
            case 6:
                size = 81;
                break;
            default:
                size = 0;
        }

        if (size == 0) {
            plugin.getLogger().info("Chunk loading size resolved to 0 and will not be loaded.");
            event.setWillClose(true);
            return;
        }

        ChunkLoader cl = plugin.getActiveChunkLoader(uuid);

        if (cl == null) {
            Location loc = plugin.getActiveLocation(uuid);

            cl = new ChunkLoader(
                    plugin,
                    -1,
                    uuid,
                    ChunkLoader.ChunkType.CREATIVE,
                    player.getWorld().getName(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    size,
                    new Date()
            );

            plugin.addChunkLoader(cl);
        } 
		else
		{
            cl.resize(size);
        }
        event.setWillClose(true);
    }
}
