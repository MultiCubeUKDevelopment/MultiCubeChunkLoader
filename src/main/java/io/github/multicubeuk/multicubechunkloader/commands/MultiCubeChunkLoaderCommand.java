package io.github.multicubeuk.multicubechunkloader.commands;

import io.github.multicubeuk.multicubechunkloader.ChunkLoaderConfiguration;
import io.github.multicubeuk.multicubechunkloader.MultiCubeChunkLoader;
import io.github.multicubeuk.multicubechunkloader.PlayerInfo;
import io.github.multicubeuk.multicubechunkloader.util.StringUtils;
import io.github.multicubeuk.multicubechunkloader.ChunkLoader;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiCubeChunkLoaderCommand implements CommandExecutor
{
    private MultiCubeChunkLoader plugin;
    private List<Command> commands;

    public MultiCubeChunkLoaderCommand(MultiCubeChunkLoader plugin)
    {
        this.plugin = plugin;

        commands = new ArrayList<>();
        commands.add(new Command("", "/mcl - Gives an activator that is used to right click a block to place a chunk loader, or right click an existing chunk loader to edit.", "multicubechunkloader.place"));
        commands.add(new Command("place", "/mcl place - Opens the place chunk loader menu", "multicubechunkloader.place"));
        commands.add(new Command("balance", "/mcl balance - Prints your chunk loader balance,", "multicubechunkloader.balance"));
        commands.add(new Command("list", "/mcl list [page #] - Lists all chunk loaders.", "multicubechunkloader.list"));
        commands.add(new Command("delete", "/mcl delete <#|all> - Deletes specified or all chunk loaders", "multicubechunkloader.delete"));

    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args)
    {

        /**
         * GIVE ACTIVATOR
         */
        if (args.length == 0)
        {
            if (sender instanceof Player)
            {
                Player player = (Player) sender;

                ItemStack item = new ItemStack(plugin.getConfiguration().getChunkloaderActivator(), 1);
                ItemMeta im = item.getItemMeta();

                im.setDisplayName(ChunkLoaderConfiguration.WAND_DISPLAY_NAME);
                List<String> lore = new ArrayList<>();
                lore.add(ChunkLoaderConfiguration.WAND_LORE);
                im.setLore(lore);


                if (plugin.getConfiguration().getChunkloaderActivatorEnchant() != null)
                {
                    im.addEnchant(plugin.getConfiguration().getChunkloaderActivatorEnchant(), 1, true);
                }

                item.setItemMeta(im);

                player.getInventory().addItem(item);

            }
            else
            {
                sender.sendMessage("You must be in game to use the GUI!");
                commands
                        .stream()
                        .filter(c -> sender.hasPermission(c.permissions) || sender.isOp())
                        .forEach(c -> sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, c.commandText)));
            }

            return true;
        }

        /**
         * HELP
         */
        if (args[0].equalsIgnoreCase("?"))
        {
            commands
                    .stream()
                    .filter(c -> sender.hasPermission(c.permissions) || sender.isOp())
                    .forEach(c -> sender.sendMessage(String.format("%s%s", ChatColor.YELLOW, c.commandText)));
        }

        /**
         * BALANCE
         */

        if (args[0].equalsIgnoreCase("balance"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("This command can only be used in game. Use /mcla balance <player> from console.");
                return true;
            }

            Player player = (Player) sender;
            PlayerInfo pi = plugin.getPlayerInfo(player.getUniqueId());

            int loadedPersonalChunks = 0;
            int loadedWorldChunks = 0;

            for (ChunkLoader c : plugin.getChunkLoaders().stream().filter(cl -> cl.getOwner().equals(player.getUniqueId())).collect(Collectors.toList())) {
                if (c.getChunkType() == ChunkLoader.ChunkType.PERSONAL)
                    loadedPersonalChunks += c.getSize();
                else if (c.getChunkType() == ChunkLoader.ChunkType.WORLD)
                    loadedWorldChunks += c.getSize();
            }

            sender.sendMessage(ChatColor.GREEN + "Your chunk loader balance:");
            sender.sendMessage(String.format("%s%s of %s free personal chunk loaders", ChatColor.GREEN, pi.getPersonalChunks() - loadedPersonalChunks, pi.getPersonalChunks()));
            sender.sendMessage(String.format("%s%s of %s free world chunk loaders", ChatColor.GREEN, pi.getWorldChunks() - loadedWorldChunks, pi.getWorldChunks()));

            return true;
        }

        /**
         * LIST
         */
        if (args[0].equalsIgnoreCase("list"))
        {

            if (!(sender instanceof Player))
            {
                sender.sendMessage(String.format("%sThis command can only be used in game. Use /mcla list from console.", ChatColor.RED));
                return true;
            }

            Player player = (Player) sender;
            int page = 0;

            if (args.length > 1 && StringUtils.isInteger(args[1]))
                page = Integer.parseInt(args[1]) - 1;

            page = page < 0 ? 0 : page * 5;
            int totalPages = (int) Math.ceil(plugin.getChunkLoaders().size() / 5);

            sender.sendMessage(String.format("%sListing chunk loaders (page %s/%s)", ChatColor.YELLOW, page + 1, totalPages));

            plugin.getChunkLoaders()
                    .stream()
                    .filter(c -> c.getOwner().equals(player.getUniqueId()))
                    .skip(page)
                    .forEach(c -> sender.sendMessage(
                            String.format(
                                    "%s%s loader with size of %s chunks at block position %s:%s,%s,%s",
                                    ChatColor.GREEN,
                                    c.getChunkType(),
                                    c.getSize(),
                                    c.getWorld(),
                                    c.getX(),
                                    c.getY(),
                                    c.getZ()
                            )
                    ));

            return true;
        }

        /**
         * DELETE
         */
        if (args[0].equalsIgnoreCase("delete"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "This command can only be used in game. Use /mcla delete from the console.");
                return true;
            }

            if (!sender.hasPermission("multicubechunkloader.delete") || !sender.isOp())
            {
                sender.sendMessage(ChatColor.RED + "You do not have access to this command!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("The delete command must be provided either a valid Chunk Loader ID or an ALL flag!");
                return true;
            }

            int loaders = 0;
            int chunks = 0;

            if (args[1].equalsIgnoreCase("all"))
            {
                for (ChunkLoader c : plugin.getChunkLoaders()
                        .stream()
                        .filter(cl -> cl.getOwner().equals(((Player) sender).getUniqueId()))
                        .collect(Collectors.toList()))
                {
                    loaders++;
                    chunks += c.getSize();
                    c.delete();
                }

                sender.sendMessage(String.format("%sSuccessfully deleted %s chunk loaders, totalling %s chunks!", ChatColor.GREEN, loaders, chunks));
                return true;
            }
            else
            {
                if (!StringUtils.isInteger(args[1]))
                {
                    sender.sendMessage(ChatColor.RED + "Invalid ID passed to the delete command!");
                    return true;
                }

                int id = Integer.parseInt(args[1]);

                ChunkLoader lpc = plugin.getChunkLoaders()
                        .stream()
                        .filter(c -> c.getOwner().equals(((Player) sender).getUniqueId()))
                        .filter(c -> c.getId() == id)
                        .findFirst().orElse(null);

                if (lpc == null)
                {
                    sender.sendMessage(ChatColor.RED + "Invalid ID passed to the delete command");
                    return true;
                }

                chunks += lpc.getSize();
                lpc.delete();

                sender.sendMessage(String.format("%sSuccessfully deleted chunk loader, totalling %s chunks", ChatColor.GREEN, chunks));
                return true;
            }
        }
        /**
         * PLACE
         */
        if (args[0].equalsIgnoreCase("place"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("This command can only by used in game!");
                return true;
            }

            Player player = (Player) sender;

            if (!plugin.isAllowedWorld(player))
                return true;

            Location location = ((Player) sender).getLocation();

            plugin.setActiveLocation(((Player) sender).getUniqueId(), location);
            plugin.openMainMenu((Player) sender);
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
