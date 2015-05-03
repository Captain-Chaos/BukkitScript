/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import net.minecraft.server.v1_8_R2.BlockPosition;
import net.minecraft.server.v1_8_R2.EntityHuman;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.material.Lever;

/**
 *
 * @author pepijn
 */
public final class BlockHelper {
    private BlockHelper() {
        // Prevent instantiation
    }
    
    public static void interact(org.bukkit.World realWorld, int x, int y, int z, OfflinePlayer realPlayer) {
        interact(realWorld.getBlockAt(x, y, z), realPlayer);
    }

    public static void switchOff(org.bukkit.World realWorld, int x, int y, int z, OfflinePlayer realPlayer) {
        org.bukkit.block.Block realBlock = realWorld.getBlockAt(x, y, z);
        if ((realBlock.getType() == Material.LEVER) && ((realBlock.getData() & 0x8) != 0)) {
            interact(realBlock, realPlayer);
        }
    }

    public static void switchOn(org.bukkit.World realWorld, int x, int y, int z, OfflinePlayer realPlayer) {
        org.bukkit.block.Block realBlock = realWorld.getBlockAt(x, y, z);
        if ((realBlock.getType() == Material.LEVER) && ((realBlock.getData() & 0x8) == 0)) {
            interact(realBlock, realPlayer);
        }
    }
    
    public static void open(org.bukkit.World realWorld, int x, int y, int z, OfflinePlayer realPlayer) {
        org.bukkit.block.Block realBlock = realWorld.getBlockAt(x, y, z);
        Material material = realBlock.getType();
        if ((material == Material.WOODEN_DOOR) || (material == Material.IRON_DOOR)) {
            int data = realBlock.getData();
            if ((data & 0x8) != 0) {
                // This is a top half of a door, redirect to the bottom half
                if (y > 0) {
                    open(realWorld, x, y - 1, z, realPlayer);
                }
            } else if ((realBlock.getData() & 0x4) == 0) {
                // Bottom half of a door and the door is closed
                interact(realBlock, realPlayer);
            }
        } else if ((material == Material.TRAP_DOOR) || (material == Material.FENCE_GATE)) {
            if ((realBlock.getData() & 0x4) == 0) {
                interact(realBlock, realPlayer);
            }
        }
    }

    public static void close(org.bukkit.World realWorld, int x, int y, int z, OfflinePlayer realPlayer) {
        org.bukkit.block.Block realBlock = realWorld.getBlockAt(x, y, z);
        Material material = realBlock.getType();
        if ((material == Material.WOODEN_DOOR) || (material == Material.IRON_DOOR)) {
            int data = realBlock.getData();
            if ((data & 0x8) != 0) {
                // This is a top half of a door, redirect to the bottom half
                if (y > 0) {
                    close(realWorld, x, y - 1, z, realPlayer);
                }
            } else if ((realBlock.getData() & 0x4) != 0) {
                // Bottom half of a door and the door is open
                interact(realBlock, realPlayer);
            }
        } else if ((material == Material.TRAP_DOOR) || (material == Material.FENCE_GATE)) {
            if ((realBlock.getData() & 0x4) != 0) {
                interact(realBlock, realPlayer);
            }
        }
    }

    public static boolean isEmpty(World realWorld, int x, int y, int z) {
        return realWorld.getBlockTypeIdAt(x, y, z) == 0;
    }
    
    public static boolean isOn(org.bukkit.World realWorld, int x, int y, int z) {
        org.bukkit.block.Block realBlock = realWorld.getBlockAt(x, y, z);
        return realBlock.getBlockPower() > 0;
    }
    
    public static boolean isOff(org.bukkit.World realWorld, int x, int y, int z) {
        org.bukkit.block.Block realBlock = realWorld.getBlockAt(x, y, z);
        return realBlock.getBlockPower() == 0;
    }
    
    private static void interact(org.bukkit.block.Block realBlock, OfflinePlayer realPlayer) {
        org.bukkit.entity.Player onlinePlayer = (realPlayer != null) ? realPlayer.getPlayer() : null;
        EntityHuman entityHuman = (onlinePlayer != null) ? ((CraftPlayer) onlinePlayer).getHandle() : null;
        // TODO no idea what the last four parameters are for; they are new in MC 1.3.1:
        net.minecraft.server.v1_8_R2.Block mcBlock = net.minecraft.server.v1_8_R2.Block.getById(realBlock.getTypeId());
        mcBlock.interact(
            ((CraftWorld) realBlock.getWorld()).getHandle(),
            new BlockPosition(realBlock.getX(), realBlock.getY(), realBlock.getZ()),
            mcBlock.fromLegacyData(realBlock.getData()),
            entityHuman,
            null,
            0.0f, 0.0f, 0.0f);
    }
}