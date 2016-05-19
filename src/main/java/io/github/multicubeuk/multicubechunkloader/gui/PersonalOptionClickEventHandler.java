package io.github.multicubeuk.multicubechunkloader.gui;

import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;
import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PersonalOptionClickEventHandler implements IconMenu.OptionClickEventHandler {
    private MultiCubeChunkLoader plugin;

    public PersonalOptionClickEventHandler() {
        super();

        this.plugin = MultiCubeChunkLoader.instance;
    }

    @Override
    public void onOptionClick(IconMenu.OptionClickEvent event) {
        int size;
        Player player = event.getPlayer();
        UUID uuid = event.getPlayer().getUniqueId();

        switch (event.getPosition()) {
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
            return;
        }

        PlayerInfo pi = plugin.getPlayerInfo(uuid);
//        List<ChunkLoader> chunks = plugin.getPersonalChunks(uuid);
        List<ChunkLoader> chunks = plugin.getChunkLoaders().stream().filter(
                c -> c.getChunkType() == ChunkLoader.ChunkType.PERSONAL &&
                        c.getOwner() == uuid
        ).collect(Collectors.toList());

        int existingChunks = 0;
        if (chunks != null) {
            for (ChunkLoader c : chunks)
                existingChunks += c.getSize();
        }

        int activeChunkSize = 0;
        ChunkLoader cl = plugin.getActiveChunkLoader(uuid);
        if (cl != null)
            activeChunkSize = cl.getSize();

        int balance = pi.getPersonalChunks() - existingChunks + activeChunkSize;
        if (size > balance && balance != -1) {
            player.sendMessage(String.format(
                            "%sYou do not have enough personal chunks available. Available chunks: %s, Required chunks: %s.",
                            ChatColor.RED,
                            balance,
                            size)
            );
            return;
        }


        if (cl == null) {
            Location loc = plugin.getActiveLocation(player.getUniqueId());

            cl = new ChunkLoader(
                    plugin,
                    -1,
                    player.getUniqueId(),
                    ChunkLoader.ChunkType.PERSONAL,
                    player.getWorld().getName(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    size,
                    new Date()
            );

            plugin.addChunkLoader(cl);
        } else {
            cl.resize(size);
        }
        event.setWillClose(true);
    }
}
