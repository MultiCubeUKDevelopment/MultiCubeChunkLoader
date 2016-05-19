package io.github.multicubeuk.multicubechunkloader.events;

import io.github.multicubeuk.multicubechunkloader.ChunkLoaderConfiguration;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.util.UUIDUtils;
import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class MultiCubeChunkLoaderEvents implements Listener {

    private MultiCubeChunkLoader plugin;

    public MultiCubeChunkLoaderEvents(MultiCubeChunkLoader plugin) {
        this.plugin = plugin;
    }

    // http://jd.bukkit.org/apidocs/ provide a complete list of events that can be registered.

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.loadPersonalChunks(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.unloadPersonalChunks(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || !event.hasItem() || event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();

        ItemStack item = player.getItemInHand();
        if (item == null)
            return;

        if (
                item.getType() == plugin.getConfiguration().getChunkloaderActivator() &&
                        item.getItemMeta().getLore() != null &&
                        item.getItemMeta().getLore().size() > 0 &&
                        item.getItemMeta().getLore().get(0).equalsIgnoreCase(ChunkLoaderConfiguration.WAND_LORE)) {

            if (!plugin.isAllowedWorld(player))
                return;

            Block block = event.getClickedBlock();

            if (plugin.isChunkLoaderMaterial(block.getType())) {
                ChunkLoader cl = plugin.getChunkLoaders()
                        .stream()
                        .filter(c -> c.getLocationString().equalsIgnoreCase(ChunkLoader.getLocationString(block)))
                        .findFirst().orElse(null);

                if (cl == null)
                    cl = plugin.getChunkLoaderAtLocation(block.getLocation());

                if (cl != null && cl.getChunkType() == plugin.getChunkTypeFromMaterial(block.getType())) {
                    plugin.setActiveChunkLoader(player.getUniqueId(), cl);
                    plugin.getChunkLoaderIconMenu(cl).open(player);
                }
                return;
            }

            int x = event.getClickedBlock().getX() + event.getBlockFace().getModX();
            int y = event.getClickedBlock().getY() + event.getBlockFace().getModY();
            int z = event.getClickedBlock().getZ() + event.getBlockFace().getModZ();

            Location loc = new Location(event.getClickedBlock().getWorld(), x, y, z);

            plugin.setActiveLocation(player.getUniqueId(), loc);
            plugin.openMainMenu(player);
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (plugin.isChunkLoaderMaterial(block.getType())) {
            String locationString = ChunkLoader.getLocationString(block);
            ChunkLoader cl = plugin.getChunkLoaders().stream().filter(
                    c -> c.getLocationString().equalsIgnoreCase(locationString)
            ).findFirst().orElse(null);

            if (cl != null) {
                OfflinePlayer op = UUIDUtils.getPlayer(cl.getOwner());
                Player owner = op.getPlayer();

                if (owner != null && owner.isOnline()) {
                    if (!cl.getOwner().equals(player.getUniqueId())) {
                        owner.sendMessage(String.format("Your chunk loader at location: %s was deleted by player %s", cl.getLocationString(), player.getName()));
                    } else {
                        owner.sendMessage(String.format("Removing chunk loader at location: %s", cl.getLocationString()));
                    }
                }

                cl.delete();
                event.setCancelled(true);
            }
        }
    }
}
