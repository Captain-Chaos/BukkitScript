/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import java.util.Properties;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;

/**
 * This is the root node of the tree of objects which provides access to events,
 * properties and actions of the Minecraft world. The context is used both for
 * specifying events using an event descriptor, and to access the Minecraft
 * world from within scripts. The available events, properties and actions
 * depend on what information is available at the time and place where the
 * context is created.
 *
 * @author pepijn
 */
public class Context {
    public Context(org.bukkit.entity.Player realPlayer) {
        this(realPlayer, null);
    }

    public Context(CommandSender commandSender) {
        this(commandSender, null);
    }

    public Context(org.bukkit.entity.Player realPlayer, String[] args) {
        this(realPlayer.getWorld(), realPlayer, realPlayer, args);
    }

    public Context(CommandSender commandSender, String[] args) {
        this((commandSender instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) commandSender).getWorld() : null,
            (commandSender instanceof OfflinePlayer) ? (OfflinePlayer) commandSender : null,
            commandSender,
            args);
    }
    
    public Context(org.bukkit.World realWorld, OfflinePlayer realPlayer, CommandSender commandSender, String[] args) {
        world = new World(realWorld, realPlayer);
        player = new Player(realWorld, realPlayer, commandSender);
        block = new BlockNode(realWorld, realPlayer);
        time = (realWorld != null) ? new TimeNode(realWorld) : null;
        command = new CommandNode();
        item = material = new ItemNode();
        idle = new Idle(realWorld);
        this.args = args;
    }
    
    public Properties toProperties() {
        Properties properties = new Properties();
        if (world.getRealWorld() != null) {
            properties.setProperty(PROPERTY_WORLD, world.getRealWorld().getName());
        }
        if (player.getRealPlayer() != null) {
            properties.setProperty(PROPERTY_PLAYER, player.getRealPlayer().getName());
        }
        return properties;
    }
    
    public static Context fromProperties(Properties properties) {
        org.bukkit.World realWorld = null;
        String worldName = properties.getProperty(PROPERTY_WORLD);
        Server server = BukkitScriptPlugin.getInstance().getServer();
        if (worldName != null) {
            realWorld = server.getWorld(worldName);
        }
        OfflinePlayer realPlayer = null;
        String playerName = properties.getProperty(PROPERTY_PLAYER);
        if (playerName != null) {
            realPlayer = server.getOfflinePlayer(playerName);
        }
        return new Context(realWorld, realPlayer, null, null);
    }
    
    public final World world;
    public final Player player;
    public final BlockNode block;
    public final TimeNode time;
    public final CommandNode command;
    public final ItemNode item, material;
    public final Idle idle;
    public final String[] args;
    
    private static final String PROPERTY_WORLD  = "worldName";
    private static final String PROPERTY_PLAYER = "playerName";
}