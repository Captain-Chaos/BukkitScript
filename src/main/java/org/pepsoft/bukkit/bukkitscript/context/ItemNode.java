/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.Material;

/**
 *
 * @author pepijn
 */
public class ItemNode {
    public Item get(String name) {
        try {
            int itemId = Integer.parseInt(name);
            Material material = Material.getMaterial(itemId);
            if (material != null) {
                return new Item(material);
            }
        } catch (NumberFormatException e) {
            // Continue
        }
        
        Material material = Material.matchMaterial(name);
        if (material != null) {
            return new Item(material);
        }
        
        throw new IllegalArgumentException("Invalid item name or ID \"" + name + "\"");
    }
}