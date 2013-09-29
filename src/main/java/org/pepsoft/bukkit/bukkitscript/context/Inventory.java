/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author pepijn
 */
public class Inventory {
    public Inventory(org.bukkit.inventory.Inventory inventory) {
        this.inventory = inventory;
    }
    
    public int getSize() {
        return inventory.getSize();
    }
    
    public boolean contains(Object item) {
        return inventory.contains(createItemStack(item).getType());
    }
    
    public void set(Object[] items) {
        ItemStack[] contents = new ItemStack[inventory.getSize()];
        for (int i = 0; i < items.length; i++) {
            contents[i] = createItemStack(items[i]);
        }
        inventory.setContents(contents);
    }
    
    public void set(int slot, Object item) {
        inventory.setItem(slot, createItemStack(item));
    }
    
    public void clear(int slot) {
        inventory.clear(slot);
    }
    
    public void clear() {
        inventory.clear();
    }
    
    public void add(Object item) {
        inventory.addItem(createItemStack(item));
    }
    
    public void remove(Object item) {
        inventory.removeItem(createItemStack(item));
    }
    
    public void removeAll(Object item) {
        inventory.remove(createItemStack(item).getType());
    }
    
    protected final ItemStack createItemStack(Object object) {
        if (object instanceof Item) {
            Item item = (Item) object;
            return new ItemStack(item.material, item.amount, item.damage, item.data);
        } else if (object instanceof Number) {
            int itemId = ((Number) object).intValue();
            Material material = Material.getMaterial(itemId);
            if (material == null) {
                throw new IllegalArgumentException("Invalid item ID " + itemId);
            } else {
                return new ItemStack(material);
            }
        } else if (object instanceof String) {
            Material material = Material.matchMaterial((String) object);
            if (material == null) {
                throw new IllegalArgumentException("Invalid item name \"" + object + "\"");
            } else {
                return new ItemStack(material);
            }
        } else if (object != null) {
            throw new IllegalArgumentException("Invalid argument type: " + object.getClass().getSimpleName() + ")");
        } else {
            return null;
        }
    }
    
    private final org.bukkit.inventory.Inventory inventory;
}