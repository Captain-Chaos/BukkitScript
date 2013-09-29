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
public class Item {
    public Item(Material material) {
        if (material == null) {
            throw new NullPointerException("material");
        }
        this.material = material;
        data = 0;
        damage = 0;
        amount = 1;
    }
    
    public Item(Material material, byte data, int amount, short damage) {
        if (material == null) {
            throw new NullPointerException("material");
        }
        if ((data < 0) || (data > 15)) {
            throw new IllegalArgumentException("data " + data);
        }
        if ((amount < 0) || (amount > 15)) {
            throw new IllegalArgumentException("data " + data);
        }
        if (damage < 0) {
            throw new IllegalArgumentException("damage " + damage);
        }
        this.material = material;
        this.data = data;
        this.damage = damage;
        this.amount = amount;
    }
    
    public Item data(byte data) {
        return new Item(material, data, amount, damage);
    }
    
    public Item amount(int amount) {
        return new Item(material, data, amount, damage);
    }

    public Item damage(short damage) {
        return new Item(material, data, amount, damage);
    }

    public final Material material;
    public final int amount;
    public final short damage;
    public final byte data;
}