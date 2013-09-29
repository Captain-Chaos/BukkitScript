/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.OfflinePlayer;
import org.pepsoft.bukkit.bukkitscript.Location;

/**
 *
 * @author pepijn
 */
public class Column extends Box {
    public Column(Location corner1, Location corner2, org.bukkit.entity.Player realPlayer) {
        this(realPlayer.getWorld(), corner1, corner2, realPlayer);
    }
    
    public Column(org.bukkit.World realWorld, Location corner1, Location corner2, OfflinePlayer realPlayer) {
        super(realWorld, new Location(corner1.worldName, corner1.x, 0, corner1.z), new Location(corner2.worldName, corner2.x, realWorld.getMaxHeight() - 1, corner2.z), realPlayer);
    }
}