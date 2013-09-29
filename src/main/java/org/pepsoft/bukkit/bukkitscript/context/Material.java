/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

/**
 *
 * @author pepijn
 */
public class Material extends Item {
    public Material(org.bukkit.Material material) {
        super(material);
    }

    public Material(org.bukkit.Material material, byte data) {
        super(material, data, 1, (short) 0);
    }
}