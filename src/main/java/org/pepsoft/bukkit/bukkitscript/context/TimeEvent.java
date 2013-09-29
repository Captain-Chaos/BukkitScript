/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.bukkit.Server;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public class TimeEvent extends Event implements Comparable<TimeEvent> {
    /**
     * Create a new time event that fires once a day at a specific time
     * (measured in "ticks", of which there are 20 per second).
     * 
     * @param realWorld The world for which to create the event.
     * @param timeOfDay The time of day in ticks at which the event should fire
     *     (0 equals the start of the day; 12000 is the start of dusk; 13800 is
     *     the start of night and 22200 is the start of dawn).
     */
    public TimeEvent(org.bukkit.World realWorld, int timeOfDay) {
        this.realWorld = realWorld;
        this.timeOfDay = timeOfDay;
    }
    
    public TimeEvent getDay() {
        return new TimeEvent(realWorld, 0);
    }
    
    public TimeEvent getNoon() {
        return new TimeEvent(realWorld, 6000);
    }
    
    public TimeEvent getDusk() {
        return new TimeEvent(realWorld, 12000);
    }
    
    public TimeEvent getNight() {
        return new TimeEvent(realWorld, 13800);
    }
    
    public TimeEvent getMidnight() {
        return new TimeEvent(realWorld, 18000);
    }
    
    public TimeEvent getDawn() {
        return new TimeEvent(realWorld, 22200);
    }
    
    @Override
    public boolean register() {
        return EVENT_LISTENER.register(this);
    }

    @Override
    public boolean unregister() {
        return EVENT_LISTENER.unregister(this);
    }

    // Comparable
    
    @Override
    public int compareTo(TimeEvent o) {
        if (o.nextOccurrence > nextOccurrence) {
            return -1;
        } else if (o.nextOccurrence == nextOccurrence) {
            return 0;
        } else {
            return 1;
        }
    }
    
    boolean schedule(long currentTime) {
        long currentTimeOfDay = currentTime % 24000L;
        if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
            BukkitScriptPlugin.logger.fine("[BukkitScript] Calculating next occurrence of timed event; time of day: " + timeOfDay + ", current time: " + currentTime + ", current time of day: " + currentTimeOfDay);
        }
        if (timeOfDay <= currentTimeOfDay) {
            // Next day
            nextOccurrence = currentTime - currentTimeOfDay + 24000L + timeOfDay;
            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] Next occurrence is tomorrow: " + nextOccurrence);
            }
        } else {
            // Later today
            nextOccurrence = currentTime - currentTimeOfDay + timeOfDay;
            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] Next occurrence is later today: " + nextOccurrence);
            }
        }
        return true;
    }
    
    void fire(Context context) {
        eventFired(context);
    }
    
    private final org.bukkit.World realWorld;
    private final int timeOfDay;
    long nextOccurrence = 0;
    
    private static final EventListener EVENT_LISTENER = new EventListener();
    
    static class EventListener implements Runnable {
        boolean register(TimeEvent timeEvent) {
            String worldName = timeEvent.realWorld.getName();
            SortedSet<TimeEvent> events = eventsByWorld.get(worldName);
            if (events == null) {
                events = new TreeSet<TimeEvent>();
                eventsByWorld.put(worldName, events);
            }
            long now = timeEvent.realWorld.getFullTime();
            timeEvent.schedule(now);
            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] Scheduling timed event " + timeEvent + " for " + (timeEvent.nextOccurrence - now) + " ticks from now");
            }
            events.add(timeEvent);
            
            // (Re)schedule
            if (taskId != -1) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Cancelling previously scheduled event handling task");
                }
                BukkitScriptPlugin.getInstance().getServer().getScheduler().cancelTask(taskId);
                taskId = -1;
            }
            tick();
            return true;
        }
        
        boolean unregister(TimeEvent timeEvent) {
            String worldName = timeEvent.realWorld.getName();
            SortedSet<TimeEvent> events = eventsByWorld.get(worldName);
            if ((events != null) && events.contains(timeEvent)) {
                events.remove(timeEvent);
                if (events.isEmpty()) {
                    eventsByWorld.remove(worldName);
                }
                
                // (Re)schedule
                if (taskId != -1) {
                    BukkitScriptPlugin.getInstance().getServer().getScheduler().cancelTask(taskId);
                    taskId = -1;
                }
                tick();
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public void run() {
//            long start = System.currentTimeMillis();
            tick();
//            long duration = System.currentTimeMillis() - start;
//            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
//                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling timed events took " + duration + " ms");
//            }
        }
        
        /**
         * Handle all due events, reschedule them if applicable, and reschedule
         * the event handling task for the next soonest expiring event.
         */
        private void tick() {
            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling timed events");
            }
            Server server = BukkitScriptPlugin.getInstance().getServer();
            long timeUntilNextEvent = Long.MAX_VALUE;
            for (Iterator<Map.Entry<String, SortedSet<TimeEvent>>> i = eventsByWorld.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<String, SortedSet<TimeEvent>> entry = i.next();
                String worldName = entry.getKey();
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Handling timed events for world \"" + worldName + "\"");
                }
                org.bukkit.World world = server.getWorld(worldName);
                if (world != null) {
                    long currentTime = world.getFullTime();
                    SortedSet<TimeEvent> events = entry.getValue();
                    while ((! events.isEmpty()) && (events.first().nextOccurrence <= currentTime)) {
                        TimeEvent event = events.first();
                        events.remove(event);
                        if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                            BukkitScriptPlugin.logger.fine("[BukkitScript] Event " + event + " is due; firing event");
                        }
                        Context context = new Context(world, null, server.getConsoleSender(), null);
                        event.fire(context);
                        if (event.schedule(currentTime)) {
                            events.add(event);
                        }
                    }
                    if (events.isEmpty()) {
                        i.remove();
                    } else {
                        long timeUntilFirstEvent = events.first().nextOccurrence - currentTime;
                        if (timeUntilFirstEvent < timeUntilNextEvent) {
                            timeUntilNextEvent = timeUntilFirstEvent;
                        }
                    }
                } else {
                    BukkitScriptPlugin.logger.warning("[BukkitScript] Timed events scheduled for world \"" + worldName + "\", but world not loaded");
                }
            }
            if (timeUntilNextEvent < Long.MAX_VALUE) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Scheduling event handler for " + timeUntilNextEvent + " ticks from now");
                }
                BukkitScriptPlugin plugin = BukkitScriptPlugin.getInstance();
                taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this, Math.max(timeUntilNextEvent, 0L));
            } else if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] No timed events scheduled; not scheduling event handler");
            }
        }
        
        private final Map<String, SortedSet<TimeEvent>> eventsByWorld = new HashMap<String, SortedSet<TimeEvent>>();
        private int taskId = -1; // TODO: in theory the task ID eventually wraps around to -1. Probably would take a while though
    }
}