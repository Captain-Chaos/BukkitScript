/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.Location;
import org.pepsoft.bukkit.bukkitscript.NamedBlock;

/**
 *
 * @author pepijn
 */
public class World {
    public World(org.bukkit.entity.Player realPlayer) {
        this(realPlayer.getWorld(), realPlayer);
    }

    public World(org.bukkit.World realWorld, OfflinePlayer realPlayer) {
        this.realWorld = realWorld;
        this.realPlayer = realPlayer;
        time = (realWorld != null) ? new TimeNode(realWorld) : null;
        idle = (realWorld != null) ? new Idle(realWorld) : null;
    }
    
    public String getName() {
        if (realWorld == null) {
            throw new IllegalStateException("No world in context");
        }
        return realWorld.getName();
    }
    
    public World get(String name) {
        Server server = BukkitScriptPlugin.getInstance().getServer();
        org.bukkit.World world = server.getWorld(name);
        if (world != null) {
            return new World(world, realPlayer);
        } else {
            throw new IllegalArgumentException("There is no world named \"" + name + "\"");
        }
    }
    
    public Block block(String name) {
        if (realWorld == null) {
            throw new IllegalStateException("No world in context");
        }
        NamedBlock namedBlock = BukkitScriptPlugin.getInstance().getNamedBlockManager().get(realWorld.getName(), name);
        if (namedBlock != null) {
            Location location = namedBlock.getLocation();
            return new Block(realWorld, location.x, location.y, location.z, realPlayer);
        } else {
            throw new IllegalArgumentException("There is no block named \"" + name + "\" in world " + realWorld.getName());
        }
    }
    
    public Block block(int x, int y, int z) {
        if (realWorld == null) {
            throw new IllegalStateException("No world in context");
        }
        if ((y < 0) || (y > realWorld.getMaxHeight())) {
            throw new IllegalArgumentException("Y coordinate " + y + " out of range");
        }
        return new Block(realWorld, x, y, z, realPlayer);
    }
    
    public Column column(Block corner1, Block corner2) {
        if (realWorld == null) {
            throw new IllegalStateException("No world in context");
        }
        return new Column(realWorld, new Location(realWorld.getName(), corner1.x, corner1.y, corner1.z), new Location(realWorld.getName(), corner2.x, corner2.y, corner2.z), realPlayer);
    }
    
    public Box box(Block corner1, Block corner2) {
        if (realWorld == null) {
            throw new IllegalStateException("No world in context");
        }
        return new Box(realWorld, new Location(realWorld.getName(), corner1.x, corner1.y, corner1.z), new Location(realWorld.getName(), corner2.x, corner2.y, corner2.z), realPlayer);
    }
    
    public boolean isIsIdle() {
        if (realWorld == null) {
            throw new IllegalStateException("No world in context");
        }
        return realWorld.getPlayers().isEmpty();
    }
     
    org.bukkit.World getRealWorld() {
        return realWorld;
    }

    public final TimeNode time;
    public final Idle idle;
    private final org.bukkit.World realWorld;
    private final OfflinePlayer realPlayer;
}