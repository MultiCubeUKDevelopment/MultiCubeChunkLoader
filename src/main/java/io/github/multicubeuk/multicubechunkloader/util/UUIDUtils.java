package io.github.multicubeuk.multicubechunkloader.util;

import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;
import net.kaikk.mc.uuidprovider.*;
import org.bukkit.OfflinePlayer;
import java.util.*;


public class UUIDUtils 
{
    public static UUID getUUID(String name)
	{
        if (MultiCubeChunkLoader.instance.getServer().getPluginManager().getPlugin("UUIDProvider") != null)
           return UUIDProvider.get(name);

        UUID uuid = MultiCubeChunkLoader.instance.getUUID(name);

        if (uuid == null)
		{
            try
            {
                Map<String, UUID> maps = new UUIDFetcher(Collections.singletonList(name)).call();
                for (String key : maps.keySet())
                    if (key.equalsIgnoreCase(name))
                        uuid = maps.get(key);
            } catch (Exception ex)
            {
                MultiCubeChunkLoader.instance.getLogger().severe(ex.getMessage());
                return null;
            }
        }

        return uuid;
    }

    public static String getName(UUID uuid)
    {

        if (MultiCubeChunkLoader.instance.getServer().getPluginManager().getPlugin("UUIDProvider") != null)
            return UUIDProvider.get(uuid);

        String name = null;

       PlayerInfo pi = MultiCubeChunkLoader.instance.getPlayerInfo(uuid);
        if (pi != null)
            name = pi.getName();

        if (name == null)
        {
            try
            {
                Map<UUID, String> maps = new NameFetcher(Collections.singletonList(uuid)).call();
                name = maps.get(uuid);
            } catch (Exception ex)
            {
                return null;
            }
        }

        return name;
    }

    public static OfflinePlayer getPlayer(UUID uuid)
    {
        if (MultiCubeChunkLoader.instance.getServer().getPluginManager().getPlugin("UUIDProvider") != null)
            return MultiCubeChunkLoader.instance.getServer().getOfflinePlayer(UUIDProvider.get(uuid));

        OfflinePlayer player = MultiCubeChunkLoader.instance.getServer().getOfflinePlayer(uuid);

        if (player == null || player.getName() == null)
        {
            try
            {
                String name = new NameFetcher(Collections.singletonList(uuid))
                        .call()
                        .get(uuid);

                if (name != null)
                {
                    player = MultiCubeChunkLoader.instance.getServer().getOfflinePlayer(name);
                }
            } catch (Exception ex)
            {
                MultiCubeChunkLoader.instance.getLogger().severe(ex.getMessage());
            }
        }

        return player;
    }
}
