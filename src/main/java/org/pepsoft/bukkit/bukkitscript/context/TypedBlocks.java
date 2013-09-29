/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.pepsoft.bukkit.bukkitscript.context.Box.BlockVisitor;

/**
 *
 * @author pepijn
 */
public class TypedBlocks implements BlockActions {
    public TypedBlocks(Box volume, int typeId, OfflinePlayer realPlayer) {
        this(volume, typeId, 0, 0, realPlayer);
    }
    
    public TypedBlocks(Box volume, int typeId, int dataMask, int dataValue, OfflinePlayer realPlayer) {
        this.volume = volume;
        this.typeId = typeId;
        this.dataMask = dataMask;
        this.dataValue = dataValue;
        this.realPlayer = realPlayer;
    }
    
    @Override
    public void interact() {
        visitBlocks(new BlockVisitor() {
            @Override
            public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int data) {
                BlockHelper.interact(world, x, y, z, realPlayer);
                return true;
            }
        });
    }

    @Override
    public void push() {
        interact();
    }

    @Override
    public void switchOff() {
        visitBlocks(new BlockVisitor() {
            @Override
            public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int data) {
                BlockHelper.switchOff(world, x, y, z, realPlayer);
                return true;
            }
        });
    }

    @Override
    public void switchOn() {
        visitBlocks(new BlockVisitor() {
            @Override
            public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int data) {
                BlockHelper.switchOn(world, x, y, z, realPlayer);
                return true;
            }
        });
    }

    @Override
    public void close() {
        visitBlocks(new BlockVisitor() {
            @Override
            public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int data) {
                BlockHelper.close(world, x, y, z, realPlayer);
                return true;
            }
        });
    }

    @Override
    public void open() {
        visitBlocks(new BlockVisitor() {
            @Override
            public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int data) {
                BlockHelper.open(world, x, y, z, realPlayer);
                return true;
            }
        });
    }
    
    private void visitBlocks(BlockVisitor visitor) {
        if (dataMask == 0) {
            volume.visitBlocks(typeId, visitor);
        } else {
            volume.visitBlocks(typeId, dataMask, dataValue, visitor);
        }
    }
    
    private final OfflinePlayer realPlayer;
    private final Box volume;
    private final int typeId, dataMask, dataValue;
}