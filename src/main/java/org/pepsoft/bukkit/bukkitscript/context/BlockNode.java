/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.OfflinePlayer;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.Location;
import org.pepsoft.bukkit.bukkitscript.NamedBlock;

/**
 *
 * @author pepijn
 */
public class BlockNode {
    public BlockNode(org.bukkit.World realWorld, OfflinePlayer realPlayer) {
        this.realWorld = realWorld;
        this.realPlayer = realPlayer;
    }
    
    public Block get(String name) {
        if (realWorld == null) {
            throw new IllegalArgumentException("No world in context");
        }
        NamedBlock namedBlock = BukkitScriptPlugin.getInstance().getNamedBlockManager().get(realWorld.getName(), name);
        if (namedBlock != null) {
            Location location = namedBlock.getLocation();
            return new Block(realWorld, location.x, location.y, location.z, realPlayer);
        } else {
            throw new IllegalArgumentException("There is no block named \"" + name + "\" in world " + realWorld.getName());
        }
    }
    
    private final org.bukkit.World realWorld;
    private final OfflinePlayer realPlayer;
}