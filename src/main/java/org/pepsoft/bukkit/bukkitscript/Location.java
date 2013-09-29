/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript;

import java.io.Serializable;
import org.bukkit.block.Block;

/**
 *
 * @author pepijn
 */
public final class Location implements Serializable {
    public Location(String worldName, int x, int y, int z) {
        if (worldName == null) {
            throw new NullPointerException();
        }
        this.worldName = worldName.trim().toLowerCase().intern();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Location)
            && (worldName == ((Location) obj).worldName)
            && (x == ((Location) obj).x)
            && (y == ((Location) obj).y)
            && (z == ((Location) obj).z);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + worldName.hashCode();
        hash = 97 * hash + x;
        hash = 97 * hash + y;
        hash = 97 * hash + z;
        return hash;
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
    
    public static Location of(Block block) {
        return new Location(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
 
    public final String worldName;
    public final int x, y, z;
    
    private static final long serialVersionUID = 1L;
}