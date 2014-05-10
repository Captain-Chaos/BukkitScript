/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import java.util.logging.Level;
import net.minecraft.server.v1_7_R3.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.MaterialData;
import org.pepsoft.bukkit.Constants;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.Location;
import org.pepsoft.bukkit.bukkitscript.context.Players.PlayerVisitor;
import org.pepsoft.bukkit.bukkitscript.context.Players.PlayerProvider;
import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public class Box {
    public Box(Location corner1, Location corner2, org.bukkit.entity.Player realPlayer) {
        this(realPlayer.getWorld(), corner1, corner2, realPlayer);
    }

    public Box(org.bukkit.World realWorld, Location corner1, Location corner2, OfflinePlayer realPlayer) {
        if ((realWorld == null) || (corner1 == null) || (corner2 == null)) {
            throw new NullPointerException();
        }
        if ((!corner1.worldName.equalsIgnoreCase(realWorld.getName())) || (!corner2.worldName.equalsIgnoreCase(realWorld.getName()))) {
            throw new IllegalArgumentException();
        }
        this.realWorld = realWorld;
        this.realPlayer = realPlayer;
        // Normalise corners so that corner1 contains the low coordinates and
        // corner 2 the high coordinates
        this.corner1 = new Location(corner1.worldName,
                Math.min(corner1.x, corner2.x),
                Math.min(corner1.y, corner2.y),
                Math.min(corner1.z, corner2.z));
        this.corner2 = new Location(corner1.worldName,
                Math.max(corner1.x, corner2.x),
                Math.max(corner1.y, corner2.y),
                Math.max(corner1.z, corner2.z));
    }

    public TypedBlocks blocksOfType(int typeId) {
        if ((typeId == Constants.BLK_WOODEN_DOOR) || (typeId == Constants.BLK_IRON_DOOR)) {
            // Limit to bottom halves; doors are two blocks high and users are
            // unlikely to want to apply any action twice to each door
            return new TypedBlocks(this, typeId, 0x8, 0, realPlayer);
        } else {
            return new TypedBlocks(this, typeId, realPlayer);
        }
    }

    public TypedBlocks blocksOfType(String typeName) {
        Material material = Material.matchMaterial(typeName);
        if (material != null) {
            return blocksOfType(material.getId());
        } else {
            throw new IllegalArgumentException("Not a valid block type name: \"" + typeName + "\"");
        }
    }

    /**
     * Checks whether the volume contains the specified item anywhere.
     *
     * @param item The item to check for. Damage and amount are ignored.
     * @return <code>true</code> if the volume contains at least one item with
     * the specified ID and data value.
     */
    public boolean contains(final Item item) {
        if (item instanceof org.pepsoft.bukkit.bukkitscript.context.Material) {
            return contains((org.pepsoft.bukkit.bukkitscript.context.Material) item);
        } else {
            final boolean[] result = new boolean[1];
            visitEntities(org.bukkit.entity.Item.class, new EntityVisitor() {
                @Override
                public boolean visitEntity(World world, Entity entity) {
                    MaterialData materialData = ((org.bukkit.entity.Item) entity).getItemStack().getData();
                    if ((materialData.getItemType() == item.material) && (materialData.getData() == item.data)) {
                        result[0] = true;
                        return false;
                    } else {
                        return true;
                    }
                }
            });
            return result[0];
        }
    }

    /**
     * Checks whether the volume contains a block of the specified material
     * anywhere.
     *
     * @param material The material to check for. If the data value is zero any
     * data value will be matched; if it is non-zero it has to match precisely.
     * @return <code>true</code> if the volume contains at least one block with
     * the specified material.
     */
    public boolean contains(org.pepsoft.bukkit.bukkitscript.context.Material material) {
        final boolean[] result = new boolean[1];
        if (material.data == 0) {
            visitBlocks(material.material.getId(), new BlockVisitor() {
                @Override
                public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int blockData) {
                    result[0] = true;
                    return false;
                }
            });
        } else {
            visitBlocks(material.material.getId(), 0xf, material.data, new BlockVisitor() {
                @Override
                public boolean visitBlock(World world, int x, int y, int z, int blockTypeId, int blockData) {
                    result[0] = true;
                    return false;
                }
            });
        }
        return result[0];
    }

    public void clear() {
        set(0, (byte) 0);
    }

    public void set(org.pepsoft.bukkit.bukkitscript.context.Material material) {
        set(material.material.getId(), material.data);
    }

    /**
     * An event that fires whenever a player enters the volume.
     *
     * @return An event that fires whenever a player enters the volume.
     */
    public Event playerEnters() {
        return new PlayerEnters();
    }

    /**
     * An event that fires whenever a player enters the volume.
     *
     * @return An event that fires whenever a player enters the volume.
     */
    public Event playerLeaves() {
        return new PlayerLeaves();
    }

    public Players getPlayers() {
        return new Players(new PlayerProvider() {
            @Override
            public void visitPlayers(final PlayerVisitor visitor) {
                visitEntities(org.bukkit.entity.Player.class, new EntityVisitor() {
                    @Override
                    public boolean visitEntity(World world, Entity entity) {
                        visitor.visitPlayer((org.bukkit.entity.Player) entity);
                        return true;
                    }
                });
            }
        });
    }

    void set(int typeId, byte data) {
        // Go chunk by chunk to try to be as efficient as possible
        int chunkX1 = corner1.x >> 4;
        int chunkX2 = corner2.x >> 4;
        int chunkZ1 = corner1.z >> 4;
        int chunkZ2 = corner2.z >> 4;
        for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
            for (int chunkZ = chunkZ1; chunkZ <= chunkZ2; chunkZ++) {
                net.minecraft.server.v1_7_R3.World mcWorld = ((CraftWorld) realWorld).getHandle();
                for (int y = corner1.y; y <= corner2.y; y++) {
                    for (int dx = 0; dx < 16; dx++) {
                        for (int dz = 0; dz < 16; dz++) {
                            int x = (chunkX << 4) | dx;
                            int z = (chunkZ << 4) | dz;
                            if ((x >= corner1.x) && (x <= corner2.x) && (z >= corner1.z) && (z <= corner2.z)) {
                                mcWorld.setTypeAndData(x, y, z, net.minecraft.server.v1_7_R3.Block.e(typeId), data, FLAG_UPDATE | FLAG_MARK_CHUNK_DIRTY | FLAG_ONLY_IF_NOT_STATIC);
                            }
                        }
                    }
                }
            }
        }
    }

    boolean contains(org.bukkit.Location location) {
        int blockX = location.getBlockX(), blockY = location.getBlockY(), blockZ = location.getBlockZ();
        return (blockX >= corner1.x)
                && (blockX <= corner2.x)
                && (blockY >= corner1.y)
                && (blockY <= corner2.y)
                && (blockZ >= corner1.z)
                && (blockZ <= corner2.z);
    }

    void visitBlocks(int blockTypeId, BlockVisitor visitor) {
        // Go chunk by chunk to try to be as efficient as possible
        int chunkX1 = corner1.x >> 4;
        int chunkX2 = corner2.x >> 4;
        int chunkZ1 = corner1.z >> 4;
        int chunkZ2 = corner2.z >> 4;
        for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
            for (int chunkZ = chunkZ1; chunkZ <= chunkZ2; chunkZ++) {
                Chunk chunk = ((CraftChunk) this.realWorld.getChunkAt(chunkX, chunkZ)).getHandle();
                for (int y = corner1.y; y <= corner2.y; y++) {
                    if (chunk.i()[(y >> 4)] == null) {
                        if (blockTypeId == 0) {
                            for (int dx = 0; dx < 16; dx++) {
                                for (int dz = 0; dz < 16; dz++) {
                                    int x = chunkX << 4 | dx;
                                    int z = chunkZ << 4 | dz;
                                    if ((x >= this.corner1.x) && (x <= this.corner2.x) && (z >= this.corner1.z) && (z <= this.corner2.z)) {
                                        if (! visitor.visitBlock(this.realWorld, x, y, z, 0, 0)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (int dx = 0; dx < 16; dx++) {
                            for (int dz = 0; dz < 16; dz++) {
                                int x = (chunkX << 4) | dx;
                                int z = (chunkZ << 4) | dz;
                                if ((realWorld.getBlockTypeIdAt(x, y, z) == blockTypeId) && (x >= corner1.x) && (x <= corner2.x) && (z >= corner1.z) && (z <= corner2.z)) {
                                    if (! visitor.visitBlock(realWorld, x, y, z, blockTypeId, realWorld.getBlockAt(x, y, z).getData())) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void visitBlocks(int blockTypeId, int blockDataMask, int blockDataValue, BlockVisitor visitor) {
        if ((~blockDataValue | blockDataMask) != 0xFFFFFFFF) {
            throw new IllegalArgumentException("blockDataValue has bits set that are not set in blockDataMask");
        }
        // Go chunk by chunk to try to be as efficient as possible
        int chunkX1 = corner1.x >> 4;
        int chunkX2 = corner2.x >> 4;
        int chunkZ1 = corner1.z >> 4;
        int chunkZ2 = corner2.z >> 4;
        for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
            for (int chunkZ = chunkZ1; chunkZ <= chunkZ2; chunkZ++) {
                Chunk chunk = ((CraftChunk) this.realWorld.getChunkAt(chunkX, chunkZ)).getHandle();
                for (int y = corner1.y; y <= corner2.y; y++) {
                    if (chunk.i()[(y >> 4)] == null) {
                        if ((blockTypeId == 0) && (blockDataValue == 0)) {
                            for (int dx = 0; dx < 16; dx++) {
                                for (int dz = 0; dz < 16; dz++) {
                                    int x = chunkX << 4 | dx;
                                    int z = chunkZ << 4 | dz;
                                    if ((x >= this.corner1.x) && (x <= this.corner2.x) && (z >= this.corner1.z) && (z <= this.corner2.z)) {
                                        if (! visitor.visitBlock(this.realWorld, x, y, z, 0, 0)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        for (int dx = 0; dx < 16; dx++) {
                            for (int dz = 0; dz < 16; dz++) {
                                int x = (chunkX << 4) | dx;
                                int z = (chunkZ << 4) | dz;
                                if ((realWorld.getBlockTypeIdAt(x, y, z) == blockTypeId) && (x >= corner1.x) && (x <= corner2.x) && (z >= corner1.z) && (z <= corner2.z)) {
                                    int data = realWorld.getBlockAt(x, y, z).getData();
                                    if ((data & blockDataMask) == blockDataValue) {
                                        if (! visitor.visitBlock(realWorld, x, y, z, blockTypeId, data)) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void visitEntities(Class<? extends Entity> entityType, EntityVisitor visitor) {
        int chunkX1 = corner1.x >> 4;
        int chunkX2 = corner2.x >> 4;
        int chunkZ1 = corner1.z >> 4;
        int chunkZ2 = corner2.z >> 4;
        for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
            for (int chunkZ = chunkZ1; chunkZ <= chunkZ2; chunkZ++) {
                org.bukkit.Chunk chunk = realWorld.getChunkAt(chunkX, chunkZ);
                Entity[] entities = chunk.getEntities();
                for (Entity entity : entities) {
                    if (entityType.isAssignableFrom(entity.getClass())) {
                        org.bukkit.Location itemLocation = entity.getLocation();
                        if (contains(itemLocation)) {
                            if (! visitor.visitEntity(realWorld, entity)) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    private final org.bukkit.World realWorld;
    private final OfflinePlayer realPlayer;
    private final Location corner1, corner2;
    static final EventListener EVENT_LISTENER = new EventListener();
    private static final int FLAG_UPDATE             = 0x1;
    private static final int FLAG_MARK_CHUNK_DIRTY   = 0x2;
    private static final int FLAG_ONLY_IF_NOT_STATIC = 0x4;

    public interface BlockVisitor {
        boolean visitBlock(org.bukkit.World world, int x, int y, int z, int blockTypeId, int blockData);
    }

    public interface EntityVisitor {
        boolean visitEntity(org.bukkit.World world, Entity entity);
    }

    static class PlayerEnters extends Event {
        @Override
        public boolean register() {
            return EVENT_LISTENER.register(this);
        }

        @Override
        public boolean unregister() {
            return EVENT_LISTENER.unregister(this);
        }
    }

    static class PlayerLeaves extends Event {
        @Override
        public boolean register() {
            return EVENT_LISTENER.register(this);
        }

        @Override
        public boolean unregister() {
            return EVENT_LISTENER.unregister(this);
        }
    }

    static class EventListener implements Listener {
        boolean register(Event event) {
            if (!registered) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("Registering player join, move and quit event listeners for box entry and exit events");
                }
                BukkitScriptPlugin plugin = BukkitScriptPlugin.getInstance();
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                registered = true;
            }
            throw new UnsupportedOperationException("Not supported yet.");
        }

        boolean unregister(Event event) {
            if (registered) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("Unregistering player join, move and quit event listeners for box entry and exit events");
                }
                HandlerList.unregisterAll(this);
                registered = false;
            }
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerMove(PlayerMoveEvent event) {
            if (event.isCancelled()) {
                return;
            }
        }
        private boolean registered;
    }
}