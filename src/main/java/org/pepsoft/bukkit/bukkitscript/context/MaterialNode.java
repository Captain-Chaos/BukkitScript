/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

/**
 *
 * @author pepijn
 */
public class MaterialNode {
    public Material get(String name) {
        try {
            int itemId = Integer.parseInt(name);
            if ((itemId < 0) || (itemId > 255)) {
                throw new IllegalArgumentException("Invalid block type ID " + itemId);
            }
            org.bukkit.Material material = org.bukkit.Material.getMaterial(itemId);
            if (material != null) {
                return new Material(material);
            }
        } catch (NumberFormatException e) {
            // Continue
        }
        
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(name);
        if (material != null) {
            if (! material.isBlock()) {
                throw new IllegalArgumentException("Invalid block type name \"" + name + "\"");
            }
            return new Material(material);
        }
        
        throw new IllegalArgumentException("Invalid block type name or ID \"" + name + "\"");
    }
}