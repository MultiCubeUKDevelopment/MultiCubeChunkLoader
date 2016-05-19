package io.github.multicubeuk.multicubechunkloader.commands;

import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;
import io.github.multicubeuk.multicubechunkloader.util.StringUtils;
import io.github.multicubeuk.multicubechunkloader.util.UUIDUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MultiCubeChunkLoaderAdminCommand implements CommandExecutor
{
    private MultiCubeChunkLoader plugin;
    private List<Command> commands;


    public MultiCubeChunkLoaderAdminCommand(MultiCubeChunkLoader plugin)
    {
        this.plugin = plugin;

        commands = new ArrayList<>();
        commands.add(new Command("add", "/mcla add <player> <personal/world> <# chunks> - Adds available chunks of the given type to players balance. This can be a negative number.", "multicubechunkloader.admin.add"));
        commands.add(new Command("balance", "/mcla balance <player> - Lists the balance of the specified player.", "multicubechunkloader.admin.balance"));
        commands.add(new Command("list", "/mcla list [player:<player>] [world:<world>] [type:<personal/world/creative>] [page #] - Lists all chunk loaders.", "multicubechunkloader.admin.list"));
        commands.add(new Command("delete", "/mcla delete [id:<id>] [player:<player>] [world:<world>] [all] - Deletes specified or all chunk loaders", "multicubechunkloader.admin.delete"));
        commands.add(new Command("reload", "/mcla reload - Reloads the plugin, including configuration and loaded chunks", "multicubechunkloader.admin.reload"));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) 
    {

        /**
         * HELP
         */
        if (args.length == 0 || args[0].equalsIgnoreCase("?")) 
        {
            commands
                    .stream()
                    .filter(c -> sender.hasPermission(c.permissions) || sender.isOp())
                    .forEach(c -> sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, c.commandText)));
            return true;
        }

        /**
         * ADD
         */
        if (args[0].equalsIgnoreCase("add"))
        {
            if (args.length < 4)
            {
                sender.sendMessage(String.format(commands.stream().filter(c -> c.command.equalsIgnoreCase("add")).findFirst().get().commandText, ChatColor.RED));
                return false;
            }

            try
            {
                UUID player = null;
                if (args[1].length() == 36)
                {
                    try
                    {
                        player = UUID.fromString(args[1]);
                    } 
                    catch (Exception ex)
                    {
                        // Not a uuid
                    }
                }

                if (player == null)
                    player = UUIDUtils.getUUID(args[1]);

                ChunkLoader.ChunkType type = ChunkLoader.ChunkType.valueOf(args[2].toUpperCase());
                int chunks = Integer.parseInt(args[3]);

                PlayerInfo pi = plugin.getPlayerInfo(player);

                if (type == ChunkLoader.ChunkType.PERSONAL)
                {
                    pi.addPersonalChunks(chunks);
                    sender.sendMessage(ChatColor.GREEN + "Successfully added " + chunks + " personal chunkloaders to " + pi.getName() + "'s balance!");
                }
                else
                {
                    pi.addWorldChunks(chunks);
                    sender.sendMessage(ChatColor.GREEN + "Successfully added " + chunks + " world chunkloaders to " + pi.getName() + "'s balance!");
                }

                plugin.updatePlayerInfo(pi);


                return true;
            } 
            catch (Exception ex)
            {
                sender.sendMessage(ChatColor.RED + "Error while adjusting balance! See console for more information.");
                plugin.getLogger().log(Level.SEVERE, "Error while updating player chunk balance!", ex);
                return false;
            }
        }

        /**
         * BALANCE
         */
        if (args[0].equalsIgnoreCase("balance")) 
        {
            if (args.length != 2) 
            {
                sender.sendMessage(ChatColor.RED + "Invalid arguments to balance command. Format must be /mcl balance <uuid|player name>");
                return true;
            }

            UUID uuid = null;
            if (args[1].length() == 36) 
            {
                try 
                {
                    uuid = UUID.fromString(args[1]);
                } 
                catch (Exception ex) 
                {
                    // No a uuid
                }
            }

            if (uuid == null) 
            {
                uuid = UUIDUtils.getUUID(args[1]);
            }

            if (uuid == null) 
            {
                sender.sendMessage(ChatColor.RED + "The specified player can not be found! Specify a valid UUID or player name.");
                return true;
            }

            PlayerInfo pi = plugin.getPlayerInfo(uuid);
            int usedPersonal = 0;
            int usedWorld = 0;

            for (ChunkLoader c : plugin.getChunkLoaders()
                    .stream()
                    .filter(cl -> cl.getOwner().equals(pi.getUuid()))
                    .collect(Collectors.toList())) 
                    {
                if (c.getChunkType() == ChunkLoader.ChunkType.WORLD)
                    usedWorld += c.getSize();
                else if (c.getChunkType() == ChunkLoader.ChunkType.PERSONAL)
                    usedPersonal += c.getSize();
            }

            if (!UUIDUtils.getPlayer(uuid).isOnline()) 
            {
                List<ChunkLoader> chunks = plugin.getPersonalChunks(uuid);

                for (ChunkLoader c : chunks)
                    usedPersonal += c.getSize();
            }

            sender.sendMessage(String.format("%sBalance of %s is:", ChatColor.GREEN, pi.getName()));
            sender.sendMessage(String.format("%s%s of %s free personal chunks", ChatColor.GREEN, pi.getPersonalChunks() - usedPersonal, pi.getPersonalChunks()));
            sender.sendMessage(String.format("%s%s of %s free world chunks", ChatColor.GREEN, pi.getWorldChunks() - usedWorld, pi.getWorldChunks()));

            return true;
        }

        else //{ return false; }

        /**
         * LIST
         */
        if (args[0].equalsIgnoreCase("list"))
        {
            int listType = 0;
            String ownerList = "";
            ChunkLoader.ChunkType typeList = null;
            String worldList = "";
            String error = "";

            int page = 0;

            page = page < 0 ? 0 : page * 10;
            int totalPages = (int) Math.ceil(plugin.getChunkLoaders().size() / 10.0);

            sender.sendMessage(String.format("%sListing chunk loaders (page %s/%s)", ChatColor.YELLOW, page + 1, totalPages));

            // this is me rewriting dun push this yet since it might be rubbish
            // @Sam can you check this part and see if you can find another way to incorporate a more accurate error throwing?
            if (args.length == 4)
            {
                for (ChunkLoader c : plugin.getChunkLoaders().stream().skip(page).limit(10).collect(Collectors.toList()))
                {
                    //OwnerList
                    if (args[2].equalsIgnoreCase("owner"))
                    {
                        try
                        {
                            ownerList = args[3];
                            if (ownerList != null)
                            {
                                listType = 1;
                            }

                            else { return false; }
                        }
                        catch (Exception ex)
                        {
                            //not a valid player
                            error = "The specified player does not exist please check your spelling!";
                        }
                    }

                    //TypeList
                    if (args[2].equalsIgnoreCase("type"))
                    {
                        try
                        {
                            typeList = ChunkLoader.ChunkType.valueOf(args[3].toUpperCase());
                            listType = 2;
                        }
                        catch (Exception ex)
                        {
                            // Not a valid type of chunkloader
                            error = "The specified type of ChunkLoader is invalid!";
                        }
                    }
                    else { return false; }

                    //WorldList
                    if (args[2].equalsIgnoreCase("world"))
                    {
                        World w1 = plugin.getServer().getWorld(args[3]);

                        if (w1 != null)
                        {
                            try
                            {
                                worldList = args[3];
                                listType = 3;
                            }
                            catch (Exception ex)
                            {
                                //invalid world name
                                error = "This world does not exist!";
                            }
                        }
                        else
                        {
                            error = "The specified argument is not valid to go with world!";
                        }
                    }

                    switch (listType)
                    {
                        case 1:
                            //OwnerList
                            if (args[3].length() == 36)
                            {
                                try
                                {
                                    if (!c.getOwner().equals(UUID.fromString(ownerList)))
                                        continue;
                                } catch (Exception ex)
                                {
                                    // Not a UUID
                                }
                            }
                            else
                            {
                                if (!c.getOwnerName().equalsIgnoreCase(ownerList))
                                    continue;
                            }
                            break;
                        case 2:
                            //TypeList
                            if (typeList != null)
                            {
                                if (c.getChunkType() != typeList)
                                    continue;
                            }
                            else { return false; }

                            break;
                        case 3:
                            //WorldList
                            if (!c.getWorld().equalsIgnoreCase(worldList))
                                continue;
                            break;
                        default:
                            sender.sendMessage(ChatColor.RED + error);
                            return true;
                    }

                    sender.sendMessage(
                            String.format(
                                    "%s#%s, %s, %s(%s) %s(%s,%s,%s)",
                                    ChatColor.GREEN,
                                    c.getId(),
                                    c.getOwnerName(),
                                    c.getChunkType(),
                                    c.getSize(),
                                    c.getWorld(),
                                    c.getX(),
                                    c.getY(),
                                    c.getZ()
                            )
                    );
                    return true;
                }
            }
        }
        else //{ return false; }

            /**
             * DELETE
             */

            if (args[0].equalsIgnoreCase("delete"))
            {
                if (!sender.hasPermission("multicubechunkloader.admin.delete") || !sender.isOp())
                {
                    sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
                }

                else { return false; }

                if (args.length < 4 || args.length > 4)
                {
                    sender.sendMessage(ChatColor.RED + "Invalid arguments. At least one filter must be specified");
                    sender.sendMessage(ChatColor.YELLOW + commands.stream().filter(c -> c.command.equalsIgnoreCase("delete")).findFirst().orElse(null).commandText);
                }

                else { return false; }

                int loadersAll = 0;
                int chunksAll = 0;

                int id = -1;
                UUID ownerDelete = null;
                String worldDelete = null;
                ChunkLoader.ChunkType typeDelete = null;

                List<ChunkLoader> clsType = null;
                List<ChunkLoader> clsOwner = null;
                List<ChunkLoader> clsWorld = null;


                if (args.length == 4)
                {
                    if (args[2].equalsIgnoreCase("owner"))
                    {
                        if (args[3].length() == 36)
                        {
                            try
                            {
                                clsOwner = new ArrayList<>();
                                UUID uuid = UUID.fromString(args[3]);
                                List<ChunkLoader> tOwner = new ArrayList<>();

                                if (clsType != null)
                                {
                                    clsType.stream()
                                            .filter(c -> c.getOwner().equals(uuid))
                                            .forEach(c -> tOwner.add(c));
                                    clsOwner.addAll(tOwner);
                                }
                                else
                                {
                                    List<ChunkLoader> personal = plugin.getPersonalChunks(ownerDelete);

                                    for (ChunkLoader c : personal)
                                    {
                                        ChunkLoader lpc = plugin.getChunkLoaders()
                                                .stream()
                                                .filter(cl -> cl.getId() == c.getId())
                                                .findFirst().orElse(null);

                                        clsOwner.add(lpc == null ? c : lpc);
                                    }

                                    plugin.getChunkLoaders()
                                            .stream()
                                            .filter(c -> c.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                                            .forEach(c -> tOwner.add(c));

                                    clsOwner.addAll(tOwner);
                                }
                            }
                            catch (Exception ex)
                            {
                                sender.sendMessage("Invalid UUID argument for the delete command! Format must be /mcla delete owner <UUID|Player>");
                                return true;
                            }
                        }
                        else
                        {
                            clsOwner = new ArrayList<>();
                            UUID uuid = UUIDUtils.getUUID(args[3]);
                            List<ChunkLoader> tOwner = new ArrayList<>();

                            if (clsType != null)
                            {
                                clsType.stream()
                                        .filter(c -> c.getOwner().equals(uuid))
                                        .forEach(c -> tOwner.add(c));
                                clsOwner.addAll(tOwner);
                            }
                            else
                            {
                                List<ChunkLoader> personal = plugin.getPersonalChunks(ownerDelete);

                                for (ChunkLoader c : personal)
                                {
                                    ChunkLoader lpc = plugin.getChunkLoaders()
                                            .stream()
                                            .filter(cl -> cl.getId() == c.getId())
                                            .findFirst().orElse(null);

                                    clsOwner.add(lpc == null ? c : lpc);
                                }

                                plugin.getChunkLoaders()
                                        .stream()
                                        .filter(c -> c.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                                        .forEach(c -> tOwner.add(c));

                                clsOwner.addAll(tOwner);
                            }
                        }

                        if (ownerDelete == null)
                        {
                            sender.sendMessage("Invalid Player Name argument for the delete command! Format must be /mcla delete owner <UUID|Player>");
                            return true;
                        }
                    }

                    if (args[2].equalsIgnoreCase("type"))
                    {
                        try
                        {
                            typeDelete = ChunkLoader.ChunkType.valueOf(args[3].toUpperCase());
                            clsType = new ArrayList<>();
                            if (typeDelete == ChunkLoader.ChunkType.PERSONAL)
                            {
                                List<ChunkLoader> personal = plugin.getPersonalChunks();

                                for (ChunkLoader c : personal)
                                {
                                    ChunkLoader lpc = plugin.getChunkLoaders().stream().filter(cl -> cl.getId() == c.getId()).findFirst().orElse(null);

                                    clsType.add(lpc == null ? c : lpc);
                                }
                            }
                            else
                            {
                                List<ChunkLoader> tType = new ArrayList<>();
                                plugin.getChunkLoaders()
                                        .stream()
                                        .filter(cl -> cl.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                                        .forEach(cl -> tType.add(cl));

                                clsType.addAll(tType);
                            }
                        }
                        catch (Exception ex)
                        {
                            sender.sendMessage("Invalid type argument for the delete command! Format must be /mcla delete type <personal|world|creative>");
                            return true;
                        }
                    }

                    if (args[2].equalsIgnoreCase("world"))
                    {
                        World w1 = plugin.getServer().getWorld(args[3]);

                        if (w1 == null)
                        {
                            sender.sendMessage("Specified World Name does not exist! Format must be /mcla delete world <world name>");
                            return true;
                        }

                        worldDelete = w1.getName();

                        clsWorld = new ArrayList<>();
                        String world2 = worldDelete;
                        List<ChunkLoader> tworld = new ArrayList<>();

                        if (clsOwner != null)
                        {
                            clsOwner.stream()
                                    .filter(c -> c.getWorld().equalsIgnoreCase(world2))
                                    .forEach(c -> tworld.add(c));
                            clsWorld.addAll(tworld);
                        }
                        else
                        {
                            List<ChunkLoader> personal = plugin.getPersonalChunks();

                            for (ChunkLoader c : personal)
                            {
                                if (!c.getWorld().equalsIgnoreCase(world2))
                                    continue;

                                ChunkLoader lpc = plugin.getChunkLoaders()
                                        .stream()
                                        .filter(cl -> cl.getWorld().equalsIgnoreCase(c.getWorld()))
                                        .findFirst().orElse(null);

                                clsWorld.add(lpc == null ? c : lpc);
                            }

                            plugin.getChunkLoaders()
                                    .stream()
                                    .filter(c -> c.getWorld().equalsIgnoreCase(world2))
                                    .forEach(c -> tworld.add(c));

                            clsWorld.addAll(tworld);
                        }
                    }

                    List<ChunkLoader> deleteList = clsWorld == null ? clsOwner == null ? clsType : clsOwner : clsWorld;

                    int loadersDelete = 0;
                    int chunksDelete = 0;

                    for (ChunkLoader c : deleteList)
                    {
                        loadersDelete++;
                        chunksDelete += c.getSize();

                        c.delete();
                    }

                    sender.sendMessage(String.format("%sSuccessfully deleted %s chunk loaders, totalling %s chunks", ChatColor.GREEN, loadersDelete, chunksDelete));
                    //return true;

                    if (args[2].equalsIgnoreCase("id"))
                    {
                        if (StringUtils.isInteger(args[3]))
                        {
                            int cid = id;
                            ChunkLoader c = plugin.getChunkLoaders().stream().filter(cl -> cl.getId() == cid).findFirst().orElse(null);

                            if (c == null)
                            {
                                c = plugin.getChunkLoader(id);

                                if (c == null)
                                {
                                    sender.sendMessage(ChatColor.RED + "The specified ID is invalid. Command aborted, please try again!");
                                    return true;
                                }

                                int chunks = c.getSize();
                                c.delete();

                                sender.sendMessage(String.format("%sSuccessfully deleted 1 chunk loader with id: %s, totalling %s chunks", ChatColor.GREEN, cid, chunks));
                                return true;
                            }
                        }
                        else
                        {
                            sender.sendMessage("Invalid ID argument for the delete command! Format must be /mcla delete id <#>!");
                            return true;
                        }
                    }

                    if (args[2].equalsIgnoreCase("all"))
                    {
                        if (typeDelete == ChunkLoader.ChunkType.PERSONAL)
                        {
                            List<ChunkLoader> cl = plugin.getPersonalChunks();

                            for (ChunkLoader c : cl)
                            {
                                loadersAll++;
                                chunksAll += c.getSize();
                                ChunkLoader lpc = plugin.getChunkLoaders().stream().filter(cls -> cls.getId() == c.getId()).findFirst().orElse(null);
                                if (lpc != null)
                                    lpc.delete();
                                else
                                    c.delete();
                            }
                        }
                        else
                        {
                            ChunkLoader.ChunkType ct = typeDelete;
                            for (ChunkLoader c : plugin.getChunkLoaders()
                                    .stream()
                                    .filter(cl -> cl.getChunkType() == ct)
                                    .collect(Collectors.toList())) {
                                loadersAll++;
                                chunksAll += c.getSize();
                                c.delete();
                            }
                        }
                    }
                    else
                    {
                        List<ChunkLoader> pChunks = plugin.getPersonalChunks();

                        for (ChunkLoader c : pChunks)
                        {
                            loadersAll++;
                            chunksAll += c.getSize();
                            // Check if it is already loaded
                            ChunkLoader lpc = plugin.getChunkLoaders().stream().filter(cl -> c.getId() == c.getId()).findFirst().orElse(null);
                            if (lpc != null)
                                lpc.delete();
                            else
                                c.delete();
                        }

                        for (ChunkLoader c : plugin.getChunkLoaders()
                                .stream()
                                .filter(cl -> cl.getChunkType() != ChunkLoader.ChunkType.PERSONAL)
                                .collect(Collectors.toList())) {
                            loadersAll++;
                            chunksAll += c.getSize();
                        }

                    }
                    sender.sendMessage(String.format("%sSuccessfully deleted %s chunk loaders, totalling %s chunks", ChatColor.GREEN, loadersAll, chunksAll));
                    return true;
                }
                else
                {
                    sender.sendMessage(String.format("%sInvalid argument (%s) used with the delete command!", ChatColor.RED, args));
                    sender.sendMessage(ChatColor.YELLOW + commands.stream().filter(c -> c.command.equalsIgnoreCase("delete")).findFirst().orElse(null).commandText);
                    return true;
                }
            }
            else { return false; }

        /**
         * RELOAD
         */
        if (args[0].equalsIgnoreCase("reload")) 
        {
            if (!sender.hasPermission("multicubechunkloader.admin.reload") || !sender.isOp()) 
            {
                sender.sendMessage(ChatColor.RED + "You do not have access to this command");
                return true;
            }

            plugin.onDisable();
            plugin.onEnable();
            return true;
        }
        return false;
    }

    private class Command 
    {
        public String command;
        public String commandText;
        public String permissions;

        public Command() 
        {
            
        }

        public Command(String command, String commandText, String permissions) 
        {
            this.command = command;
            this.commandText = commandText;
            this.permissions = permissions;
        }
    }
}
