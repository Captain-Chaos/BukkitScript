/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript;

import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public final class NamedBlock implements Serializable, Comparable<NamedBlock> {
    NamedBlock(Location location, String name) {
        this.location = location;
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    // Comparable
    
    @Override
    public int compareTo(NamedBlock o) {
        return name.compareTo(o.name);
    }
    
    // Object

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NamedBlock other = (NamedBlock) obj;
        if (this.location != other.location && (this.location == null || !this.location.equals(other.location))) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.location != null ? this.location.hashCode() : 0);
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
    
    private final Location location;
    private final String name;
    
    private static final long serialVersionUID = 1L;
}