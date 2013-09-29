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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public class Idle extends Event {
    public Idle(org.bukkit.World realWorld) {
        this(realWorld, 0);
    }
    
    public Idle(org.bukkit.World realWorld, int afterTicks) {
        this.realWorld = realWorld;
        this.afterTicks = afterTicks;
    }

    @Override
    public boolean register() {
        return EVENT_LISTENER.register(this);
    }

    @Override
    public boolean unregister() {
        return EVENT_LISTENER.unregister(this);
    }
    
    public Idle get(String time) {
        return new Idle(realWorld, afterTicks + parseTime(time));
    }
    
    void fire(Context context) {
        eventFired(context);
    }
    
    private int parseTime(String timeStr) {
        final int STATE_WAITING_FOR_NUMBER = 1;
        final int STATE_PARSING_NUMBER     = 2;
        final int STATE_WAITING_FOR_UNIT   = 3;
        final int STATE_PARSING_UNIT       = 4;
        int state = STATE_WAITING_FOR_NUMBER;
        int number = 0;
        StringBuilder unitBuilder = new StringBuilder();
sm:     for (int i = 0; i < timeStr.length(); i++) {
            char c = timeStr.charAt(i);
            switch (state) {
                case STATE_WAITING_FOR_NUMBER:
                    if (Character.isDigit(c)) {
                        number = Character.digit(c, 10);
                        state = STATE_PARSING_NUMBER;
                    }
                    break;
                case STATE_PARSING_NUMBER:
                    if (Character.isDigit(c)) {
                        number = number * 10 + Character.digit(c, 10);
                    } else if (Character.isLetter(c)) {
                        unitBuilder.append(c);
                        state = STATE_PARSING_UNIT;
                    } else {
                        state = STATE_WAITING_FOR_UNIT;
                    }
                    break;
                case STATE_WAITING_FOR_UNIT:
                    if (Character.isLetter(c)) {
                        unitBuilder.append(c);
                        state = STATE_PARSING_UNIT;
                    }
                    break;
                case STATE_PARSING_UNIT:
                    if (Character.isLetter(c)) {
                        unitBuilder.append(c);
                    } else {
                        break sm;
                    }
                    break;
            }
        }
        if (state == STATE_WAITING_FOR_NUMBER) {
            throw new IllegalArgumentException("Missing number in time definition \"" + timeStr + "\"");
        }
        if (state == STATE_WAITING_FOR_UNIT) {
            throw new IllegalArgumentException("Missing unit in time definition \"" + timeStr + "\"");
        }
        if (number < 1) {
            throw new IllegalArgumentException("Invalid number " + number + " in time definition \"" + timeStr + "\"");
        }
        String unitStr = unitBuilder.toString().trim().toLowerCase();
        TimeUnit unit = null;
        for (TimeUnit timeUnit: TimeUnit.values()) {
            if (timeUnit.name().toLowerCase().startsWith(unitStr)) {
                unit = timeUnit;
                break;
            }
        }
        if (unit == null) {
            throw new IllegalArgumentException("Invalid unit name \"" + unitStr + "\" in time definition \"" + timeStr + "\"");
        }
        return number * unit.ticks;
    }
    
    final org.bukkit.World realWorld;
    final int afterTicks;
    int taskId = -1;
    
    private static final EventListener EVENT_LISTENER = new EventListener();
    
    enum TimeUnit {
        TICKS(1), SECONDS(20), MINUTES(1200);
    
        private TimeUnit(int ticks) {
            this.ticks = ticks;
        }
        
        final int ticks;
    }
    
    static class EventListener implements org.bukkit.event.Listener {
        boolean register(Idle idleEvent) {
            String worldName = idleEvent.realWorld.getName();
            Set<Idle> events = eventsByWorld.get(worldName);
            if (events == null) {
                events = new HashSet<Idle>();
                eventsByWorld.put(worldName, events);
            }
            if (! events.contains(idleEvent)) {
                events.add(idleEvent);
                if (! registered) {
                    if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                        BukkitScriptPlugin.logger.fine("[BukkitScript] Registering player join and player quit event handlers");
                    }
                    BukkitScriptPlugin plugin = BukkitScriptPlugin.getInstance();
                    plugin.getServer().getPluginManager().registerEvents(this, plugin);
                    registered = true;
                }
                return true;
            } else {
                return false;
            }
        }

        boolean unregister(Idle idleEvent) {
            String worldName = idleEvent.realWorld.getName();
            Set<Idle> events = eventsByWorld.get(worldName);
            if (events == null) {
                return false;
            }
            if (events.contains(idleEvent)) {
                events.remove(idleEvent);
                if (idleEvent.taskId != -1) {
                    BukkitScriptPlugin.getInstance().getServer().getScheduler().cancelTask(idleEvent.taskId);
                }
                if (events.isEmpty()) {
                    eventsByWorld.remove(worldName);
                    if (eventsByWorld.isEmpty()) {
                        if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                            BukkitScriptPlugin.logger.fine("[BukkitScript] Unregistering player join and player quit event handlers");
                        }
                        HandlerList.unregisterAll(this);
                        registered = false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        
        @EventHandler(priority= EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            String worldName = event.getPlayer().getWorld().getName();
            if (idleWorlds.contains(worldName)) {
                BukkitScheduler scheduler = BukkitScriptPlugin.getInstance().getServer().getScheduler();
                for (Map.Entry<String, Set<Idle>> entry: eventsByWorld.entrySet()) {
                    for (Idle idleEvent: entry.getValue()) {
                        if (idleEvent.taskId != -1) {
                            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                                BukkitScriptPlugin.logger.fine("[BukkitScript] Cancelling task " + idleEvent.taskId + " for idle event");
                            }
                            scheduler.cancelTask(idleEvent.taskId);
                            idleEvent.taskId = -1;
                        }
                    }
                }
                idleWorlds.remove(worldName);
            }
        }
        
        @EventHandler(priority= EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            org.bukkit.World realWorld = event.getPlayer().getWorld();
            String worldName = realWorld.getName();
            if (realWorld.getPlayers().size() == 1) {
                // Last player logged out of world (the player is removed from
                // the world *after* this event; hence we test for one
                Set<Idle> events = eventsByWorld.get(worldName);
                if (events != null) {
                    BukkitScriptPlugin plugin = BukkitScriptPlugin.getInstance();
                    BukkitScheduler scheduler = plugin.getServer().getScheduler();
                    final Context context = new Context(realWorld, null, BukkitScriptPlugin.getInstance().getServer().getConsoleSender(), null);
                    for (Idle idleEvent: events) {
                        if (idleEvent.afterTicks == 0) {
                            idleEvent.fire(context);
                        } else {
                            final Idle finalEvent = idleEvent;
                            idleEvent.taskId = scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    finalEvent.fire(context);
                                    finalEvent.taskId = -1;
                                }
                            }, idleEvent.afterTicks);
                            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                                BukkitScriptPlugin.logger.fine("[BukkitScript] Task " + idleEvent.taskId + " scheduled for idle event");
                            }
                        }
                    }
                }
                idleWorlds.add(worldName);
            }
        }
        
        private final Map<String, Set<Idle>> eventsByWorld = new HashMap<String, Set<Idle>>();
        private boolean registered;
        private Set<String> idleWorlds = new HashSet<String>();
    }
}