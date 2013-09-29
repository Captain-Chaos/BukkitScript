/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.Location;
import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public class Block implements BlockActions, BlockEvents {
    public Block(org.bukkit.World realWorld, int x, int y, int z, OfflinePlayer realPlayer) {
        this.realWorld = realWorld;
        this.realPlayer = realPlayer;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // Actions
    
    @Override
    public void interact() {
        BlockHelper.interact(realWorld, x, y, z, realPlayer);
    }
    
    @Override
    public void push() {
        interact();
    }
    
    @Override
    public void switchOff() {
        BlockHelper.switchOff(realWorld, x, y, z, realPlayer);
    }

    @Override
    public void switchOn() {
        BlockHelper.switchOn(realWorld, x, y, z, realPlayer);
    }

    @Override
    public void close() {
        BlockHelper.close(realWorld, x, y, z, realPlayer);
    }

    @Override
    public void open() {
        BlockHelper.open(realWorld, x, y, z, realPlayer);
    }
 
    // Properties
    
    public boolean isEmpty() {
        return BlockHelper.isEmpty(realWorld, x, y, z);
    }
    
    public boolean isIsOn() {
        return BlockHelper.isOn(realWorld, x, y, z);
    }

    public boolean isIsOff() {
        return BlockHelper.isOff(realWorld, x, y, z);
    }
    
    public World getWorld() {
        return new World(realWorld, realPlayer);
    }
    
    public Inventory getInventory() {
        BlockState realBlock = realWorld.getBlockAt(x, y, z).getState();
        if (realBlock instanceof InventoryHolder) {
            return new Inventory(((InventoryHolder) realBlock).getInventory());
        } else {
            throw new IllegalArgumentException("Block of type " + realBlock.getType() + " @ " + x + "," + y + "," + z + " has no inventory");
        }
    }

    // Events
    
    @Override
    public Event getInteracted() {
        return new Interacted(new Location(realWorld.getName(), x, y, z));
    }

    private final org.bukkit.World realWorld;
    public final int x, y, z;
    private final OfflinePlayer realPlayer;
    
    private static final EventListener EVENT_LISTENER = new EventListener();
    
    static class Interacted extends Event implements org.bukkit.event.Listener {
        Interacted(Location location) {
            this.location = location;
        }
        
        @Override
        public boolean register() {
            return EVENT_LISTENER.register(this);
        }

        @Override
        public boolean unregister() {
            return EVENT_LISTENER.unregister(this);
        }

        void fire(Context context) {
            eventFired(context);
        }

        final Location location;
    }
    
    static class EventListener implements Listener {
        boolean register(Event event) {
            if (event instanceof Interacted) {
                Location location = ((Interacted) event).location;
                Set<Event> events = interactedEvents.get(location);
                if (events == null) {
                    events = new HashSet<Event>();
                    interactedEvents.put(location, events);
                }
                if (events.contains(event)) {
                    return false;
                }
                events.add(event);
            } else {
                throw new IllegalArgumentException();
            }
            if (! registered) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Registering player interact event handler");
                }
                BukkitScriptPlugin plugin = BukkitScriptPlugin.getInstance();
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                registered = true;
            }
            return true;
        }
        
        boolean unregister(Event event) {
            // TODO: this won't work because Events get recreated every time the
            // event descriptor is parsed, and they use default equals() and
            // hashCode(), so they won't be recognized as the same event. Fix by
            // creating a better Event identity mechanism
            if (event instanceof Interacted) {
                Location location = ((Interacted) event).location;
                Set<Event> events = interactedEvents.get(location);
                if ((events != null) && events.contains(event)) {
                    events.remove(event);
                    if (events.isEmpty()) {
                        interactedEvents.remove(location);
                        if (interactedEvents.isEmpty()) {
                            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                                BukkitScriptPlugin.logger.fine("[BukkitScript] Unregistering player interact event handler");
                            }
                            HandlerList.unregisterAll(this);
                            registered = false;
                        }
                    }
                    return true;
                }
            } else {
                throw new IllegalArgumentException();
            }
            return false;
        }
        
        @EventHandler(priority= EventPriority.MONITOR)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.isCancelled()) {
                return;
            }
//            long start = System.currentTimeMillis();
            if (event.hasBlock()) {
                org.bukkit.block.Block block = event.getClickedBlock();
                Location location = Location.of(block);
                Set<Event> events = interactedEvents.get(location);
                if (events != null) {
                    Context context = new Context(event.getPlayer());
                    for (Event interactedEvent: events) {
                        ((Interacted) interactedEvent).fire(context);
                    }
                }
            }
//            long duration = System.currentTimeMillis() - start;
//            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
//                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling player interact event took " + duration + " ms");
//            }
        }
        
        private final Map<Location, Set<Event>> interactedEvents = new HashMap<Location, Set<Event>>();
        private boolean registered;
    }
}