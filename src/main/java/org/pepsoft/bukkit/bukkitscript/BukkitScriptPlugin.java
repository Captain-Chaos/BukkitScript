/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.pepsoft.bukkit.bukkitscript.context.Context;
import org.pepsoft.bukkit.bukkitscript.event.EventSpec;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author pepijn
 */
public class BukkitScriptPlugin extends JavaPlugin {
    public BukkitScriptPlugin() {
        instance = this;
    }
    
    @Override
    public void onEnable() {
//        logger.setLevel(Level.FINE);
        namedBlockManager = new NamedBlockManager(getDataFolder());
        namedBlockManager.load();
        scriptManager = new ScriptManager(this);
        scriptManager.load();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((sender instanceof Player) && (! sender.isOp())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to execute that command");
            return true;
        }
        String name = command.getName().toLowerCase();
        if (name.equals("setname")) {
            return setName(sender, args);
        } else if (name.equals("getname")) {
            return getName(sender, args);
        } else if (name.equals("clearname")) {
            return clearName(sender, args);
        } else if (name.equals("listnames")) {
            return listNames(sender, args);
        } else if (name.equals("bindscript")) {
            return bindScript(sender, args);
        } else if (name.equals("reloadscripts")) {
            return reloadScripts(sender);
        } else if (name.equals("unbindscript")) {
            return unbindScript(sender, args);
        } else if (name.equals("listscripts")) {
            return listscripts(sender);
        } else if (name.equals("listbindings")) {
            return listboundevents(sender);
        } else {
            return false;
        }
    }
    
    public NamedBlockManager getNamedBlockManager() {
        return namedBlockManager;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }
    
    public static BukkitScriptPlugin getInstance() {
        return instance;
    }
    
    private boolean setName(CommandSender sender, String[] args) {
        if ((args.length < 1) || (args.length > 3)) {
            return false;
        }
        if (args.length == 1) {
            if (! (sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to specify a world name and block coordinates");
                return true;
            }
            String name = args[0].trim();
            Player player = (Player) sender;
            List<Block> lineOfSight = player.getLineOfSight((Set<Material>) null, 10);
            for (Block block: lineOfSight) {
                if (! block.isEmpty()) {
                    World world = player.getWorld();
                    Location location = new Location(world.getName(), block.getX(), block.getY(), block.getZ());
                    setName(sender, location, name);
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "You need to look at the block to name and/or get closer to it");
            return true;
        } else if (args.length == 2) {
            if (! (sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to specify a world name");
                return true;
            }
            Player player = (Player) sender;
            String[] coordStrs = args[0].trim().split(",");
            if (coordStrs.length != 3) {
                return false;
            }
            try {
                int x = Integer.parseInt(coordStrs[0]);
                int y = Integer.parseInt(coordStrs[1]);
                int z = Integer.parseInt(coordStrs[2]);
                World world = player.getWorld();
                if ((y < 0) || (y >= world.getMaxHeight())) {
                    sender.sendMessage(ChatColor.RED + "Not a valid y coordinate: " + y);
                    return true;
                }
                Location location = new Location(world.getName(), x, y, z);
                String name = args[1].trim();
                setName(sender, location, name);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            World world = getServer().getWorld(args[0]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "There is no world named \"" + args[0] + "\"");
                return true;
            }
            String[] coordStrs = args[1].trim().split(",");
            if (coordStrs.length != 3) {
                return false;
            }
            try {
                int x = Integer.parseInt(coordStrs[0]);
                int y = Integer.parseInt(coordStrs[1]);
                int z = Integer.parseInt(coordStrs[2]);
                if ((y < 0) || (y >= world.getMaxHeight())) {
                    sender.sendMessage(ChatColor.RED + "Not a valid y coordinate: " + y);
                    return true;
                }
                Location location = new Location(world.getName(), x, y, z);
                String name = args[2].trim();
                setName(sender, location, name);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
    
    private void setName(CommandSender sender, Location location, String name) {
        if (namedBlockManager.exists(location.worldName, name)) {
            sender.sendMessage(ChatColor.RED + "There is already a block named \"" + name + "\" @ " + namedBlockManager.get(location.worldName, name).getLocation());
            return;
        }
        Block block = getServer().getWorld(location.worldName).getBlockAt(location.x, location.y, location.z);
        if (namedBlockManager.exists(location)) {
            sender.sendMessage(ChatColor.RED + "That block (type " + block.getType() + " @ " + location + ") is already named \"" + namedBlockManager.get(location).getName() + "\"");
            return;
        }
        namedBlockManager.create(location, name);
        namedBlockManager.save();
        sender.sendMessage(ChatColor.GREEN + "Block of type " + block.getType() + " @ " + location + " named \"" + name + "\"");
    }

    private boolean getName(CommandSender sender, String[] args) {
        if (args.length > 2) {
            return false;
        }
        if (args.length == 0) {
            if (! (sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to specify a world name and block coordinates");
                return true;
            }
            Player player = (Player) sender;
            List<Block> lineOfSight = player.getLineOfSight((Set<Material>) null, 10);
            for (Block block: lineOfSight) {
                if (! block.isEmpty()) {
                    World world = player.getWorld();
                    Location location = new Location(world.getName(), block.getX(), block.getY(), block.getZ());
                    getName(sender, location);
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "You need to look at the block to identify and/or get closer to it");
            return true;
        } else if (args.length == 1) {
            if (! (sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to specify a world name");
                return true;
            }
            Player player = (Player) sender;
            String[] coordStrs = args[0].trim().split(",");
            if (coordStrs.length != 3) {
                return false;
            }
            try {
                int x = Integer.parseInt(coordStrs[0]);
                int y = Integer.parseInt(coordStrs[1]);
                int z = Integer.parseInt(coordStrs[2]);
                World world = player.getWorld();
                if ((y < 0) || (y >= world.getMaxHeight())) {
                    sender.sendMessage(ChatColor.RED + "Not a valid y coordinate: " + y);
                    return true;
                }
                Location location = new Location(world.getName(), x, y, z);
                getName(sender, location);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            World world = getServer().getWorld(args[0]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "There is no world named \"" + args[0] + "\"");
                return true;
            }
            String[] coordStrs = args[1].trim().split(",");
            if (coordStrs.length != 3) {
                return false;
            }
            try {
                int x = Integer.parseInt(coordStrs[0]);
                int y = Integer.parseInt(coordStrs[1]);
                int z = Integer.parseInt(coordStrs[2]);
                if ((y < 0) || (y >= world.getMaxHeight())) {
                    sender.sendMessage(ChatColor.RED + "Not a valid y coordinate: " + y);
                    return true;
                }
                Location location = new Location(world.getName(), x, y, z);
                getName(sender, location);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
    
    private void getName(CommandSender sender, Location location) {
        NamedBlock namedBlock = namedBlockManager.get(location);
        Block block = getServer().getWorld(location.worldName).getBlockAt(location.x, location.y, location.z);
        if (namedBlock != null) {
            sender.sendMessage(ChatColor.GREEN + "The block of type " + block.getType() + " @ " + location + " is named \"" + namedBlock.getName() + "\"");
        } else {
            sender.sendMessage(ChatColor.RED + "The block of type " + block.getType() + " @ " + location + " does not have a name");
        }
    }
    
    private boolean clearName(CommandSender sender, String[] args) {
        if (args.length > 2) {
            return false;
        }
        World world;
        String identifier;
        if (args.length == 0) {
            if (! (sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to specify a world name and block coordinates or name");
                return true;
            } else {
                world = ((Player) sender).getWorld();
                identifier = null;
            }
        } else if (args.length == 1) {
            if (! (sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to specify a world name");
                return true;
            } else {
                world = ((Player) sender).getWorld();
                identifier = args[0].trim();
            }
        } else {
            world = getServer().getWorld(args[0].trim());
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "There is no world named \"" + args[0] + "\"");
                return true;
            }
            identifier = args[1].trim();
        }
        if (identifier == null) {
            Player player = (Player) sender;
            List<Block> lineOfSight = player.getLineOfSight((Set<Material>) null, 10);
            for (Block block: lineOfSight) {
                if (! block.isEmpty()) {
                    Location location = new Location(player.getWorld().getName(), block.getX(), block.getY(), block.getZ());
                    clearName(sender, location, null, null);
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "You need to look at the block of which to clear the name and/or get closer to it");
            return true;
        } else {
            String[] coordStrs = identifier.split(",");
            if (coordStrs.length == 3) {
                try {
                    int x = Integer.parseInt(coordStrs[0]);
                    int y = Integer.parseInt(coordStrs[1]);
                    int z = Integer.parseInt(coordStrs[2]);
                    if ((y < 0) || (y >= world.getMaxHeight())) {
                        sender.sendMessage(ChatColor.RED + "Not a valid y coordinate: " + y);
                        return true;
                    }
                    Location location = new Location(world.getName(), x, y, z);
                    clearName(sender, location, null, null);
                    return true;
                } catch (NumberFormatException e) {
                    // Continue
                }
            }
            // If we reach here the identifier was not a valid set of coordinates,
            // so assume it is supposed to be a name
            clearName(sender, null, world.getName(), identifier);
            return true;
        }
    }
    
    private void clearName(CommandSender sender, Location location, String worldName, String name) {
        if (location != null) {
            NamedBlock namedBlock = namedBlockManager.get(location);
            if (namedBlock == null) {
                sender.sendMessage(ChatColor.RED + "There is no named block @ " + location);
                return;
            }
            namedBlockManager.remove(namedBlock);
            namedBlockManager.save();
            Block block = getServer().getWorld(location.worldName).getBlockAt(location.x, location.y, location.z);
            sender.sendMessage(ChatColor.GREEN + "Name \"" + namedBlock.getName() + "\" removed from block of type " + block.getType() + " @ " + location);
        } else {
            NamedBlock namedBlock = namedBlockManager.get(worldName, name);
            if (namedBlock == null) {
                sender.sendMessage(ChatColor.RED + "There is no block named \"" + name + "\"");
                return;
            }
            namedBlockManager.remove(namedBlock);
            namedBlockManager.save();
            Block block = getServer().getWorld(namedBlock.getLocation().worldName).getBlockAt(namedBlock.getLocation().x, namedBlock.getLocation().y, namedBlock.getLocation().z);
            sender.sendMessage(ChatColor.GREEN + "Name \"" + namedBlock.getName() + "\" removed from block of type " + block.getType() + " @ " + namedBlock.getLocation());
        }
    }

    private boolean listNames(CommandSender sender, String[] args) {
        if (args.length > 1) {
            return false;
        }
        World world = null;
        if (args.length == 0) {
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            }
        } else {
            world = getServer().getWorld(args[0].trim().toLowerCase());
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "There is no world named \"" + args[0].trim() + "\"");
                return true;
            }
        }
        List<NamedBlock> blocks;
        if (world == null) {
            blocks = namedBlockManager.getAll();
            sender.sendMessage(ChatColor.AQUA + "Named blocks in all worlds:");
        } else {
            blocks = namedBlockManager.find(world.getName());
            sender.sendMessage(ChatColor.AQUA + "Named blocks in world " + world.getName() + ":");
        }
        for (NamedBlock block: blocks) {
            sender.sendMessage(ChatColor.AQUA + block.getName() + ": " + block.getLocation());
        }
        return true;
    }

    private boolean bindScript(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return false;
        }
        String eventDescriptor = args[0];
        String scriptName = args[1];
        Script script = scriptManager.getScript(scriptName);
        if (script == null) {
            sender.sendMessage(ChatColor.RED + "There is no script named \"" + scriptName + "\"");
            return true;
        }
        Context context;
        if (sender instanceof Player) {
            context = new Context((Player) sender);
        } else {
            context = new Context(sender);
        }
        try {
            if (! scriptManager.bindEvent(eventDescriptor, script, context)) {
                sender.sendMessage(ChatColor.RED + "Could not bind script \"" + scriptName + "\" to event \"" + eventDescriptor + "\" for unknown reasons");
                return true;
            }
            scriptManager.save();
            sender.sendMessage(ChatColor.GREEN + "Script \"" + scriptName + "\" bound to event \"" + eventDescriptor + "\"");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid event descriptor (message: \"" + e.getMessage() + "\")");
        } catch (IllegalStateException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private boolean unbindScript(CommandSender sender, String[] args) {
        if ((args.length < 1) || (args.length > 2)) {
            return false;
        }
        String eventDescriptor = args[0];
        Script script = null;
        if (args.length == 2) {
            String scriptName = args[1];
            script = scriptManager.getScript(scriptName);
            if (script == null) {
                sender.sendMessage(ChatColor.RED + "There is no script named \"" + scriptName + "\"");
                return true;
            }
        }
        try {
            if (script != null) {
                if (! scriptManager.unbindEvent(eventDescriptor, script)) {
                    sender.sendMessage(ChatColor.RED + "Could not unbind script \"" + script.getName() + "\" from event \"" + eventDescriptor + "\" for unknown reasons");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "Script \"" + script.getName() + "\" unbound from event \"" + eventDescriptor + "\"");
            } else {
                if (! scriptManager.unbindEvent(eventDescriptor)) {
                    sender.sendMessage(ChatColor.RED + "No scripts are bound to event \"" + eventDescriptor + "\"");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "All scripts unbound from event \"" + eventDescriptor + "\"");
            }
            scriptManager.save();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid event descriptor (message: \"" + e.getMessage() + "\")");
        } catch (IllegalStateException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private boolean listscripts(CommandSender sender) {
        Set<String> scriptNames = scriptManager.getScriptNames();
        if (scriptNames.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "No scripts loaded");
        } else {
            sender.sendMessage(ChatColor.AQUA + Integer.toString(scriptNames.size()) + " scripts loaded:");
            for (String scriptName: scriptNames) {
                sender.sendMessage(ChatColor.AQUA + scriptName);
            }
        }
        return true;
    }

    private boolean listboundevents(CommandSender sender) {
        Set<EventSpec> boundEvents = scriptManager.getBoundEvents();
        if (boundEvents.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "No events bound");
        } else {
            sender.sendMessage(ChatColor.AQUA + Integer.toString(boundEvents.size()) + " events bound:");
            for (EventSpec boundEvent: boundEvents) {
                sender.sendMessage(ChatColor.AQUA + boundEvent.getEventDescriptor() + ": script " + boundEvent.getScriptName());
            }
        }
        return true;
    }
    
    private boolean reloadScripts(CommandSender sender) {
        int count = scriptManager.reloadScripts();
        sender.sendMessage(ChatColor.GREEN.toString() + count + " scripts reloaded.");
        return true;
    }
    
    private NamedBlockManager namedBlockManager;
    private ScriptManager scriptManager;
    
    private static BukkitScriptPlugin instance;
    
    public static final Logger logger = Logger.getLogger("Minecraft.org.pepsoft.bukkitscript");
}