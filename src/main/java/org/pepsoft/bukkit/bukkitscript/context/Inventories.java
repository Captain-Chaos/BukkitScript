/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.inventory.ItemStack;

/**
 *
 * @author pepijn
 */
public class Inventories extends Inventory {
    public Inventories(InventoryProvider inventoryProvider) {
        super(null);
        this.inventoryProvider = inventoryProvider;
    }

    @Override
    public void add(Object item) {
        final ItemStack itemStack = createItemStack(item);
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.addItem(itemStack);
                return true;
            }
        });
    }

    @Override
    public void clear(final int slot) {
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.clear(slot);
                return true;
            }
        });
    }

    @Override
    public void clear() {
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.clear();
                return true;
            }
        });
    }

    @Override
    public boolean contains(Object item) {
        final boolean[] result = new boolean[1];
        final org.bukkit.Material material = createItemStack(item).getType();
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                if (inventory.contains(material)) {
                    result[0] = true;
                    return false;
                } else {
                    return true;
                }
            }
        });
        return result[0];
    }

    @Override
    public int getSize() {
        return inventoryProvider.getSize();
    }

    @Override
    public void remove(Object item) {
        final ItemStack itemStack = createItemStack(item);
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.remove(itemStack);
                return true;
            }
        });
    }

    @Override
    public void removeAll(Object item) {
        final org.bukkit.Material material = createItemStack(item).getType();
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.remove(material);
                return true;
            }
        });
    }

    @Override
    public void set(Object[] items) {
        final ItemStack[] contents = new ItemStack[inventoryProvider.getSize()];
        for (int i = 0; i < items.length; i++) {
            contents[i] = createItemStack(items[i]);
        }
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.setContents(contents);
                return true;
            }
        });
    }

    @Override
    public void set(final int slot, Object item) {
        final ItemStack itemStack = createItemStack(item);
        inventoryProvider.visitInventories(new InventoryVisitor() {
            @Override
            public boolean visit(org.bukkit.inventory.Inventory inventory) {
                inventory.setItem(slot, itemStack);
                return true;
            }
        });
    }
    
    private final InventoryProvider inventoryProvider;
    
    public interface InventoryProvider {
        void visitInventories(InventoryVisitor visitor);
        int getSize();
    }
    
    public interface InventoryVisitor {
        /**
         * Visit the specified inventory, and indicate whether the iteration
         * over the inventories should continue.
         * 
         * @param inventory The inventory to visit.
         * @return <code>true</code> if the iteration should continue.
         */
        boolean visit(org.bukkit.inventory.Inventory inventory);
    }
}