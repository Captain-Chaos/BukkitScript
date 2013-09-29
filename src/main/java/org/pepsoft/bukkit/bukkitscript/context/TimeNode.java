/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

/**
 *
 * @author pepijn
 */
public class TimeNode {
    public TimeNode(org.bukkit.World realWorld) {
        this.realWorld = realWorld;
        this.timeOfDay = (int) realWorld.getTime();
    }
    
    // Properties
    
    public int now() {
        return timeOfDay;
    }
    
    public boolean isIsDawn() {
        return timeOfDay >= 22200;
    }
    
    public boolean isIsDay() {
        return timeOfDay < 12000;
    }
    
    public boolean isIsDusk() {
        return (timeOfDay >= 12000) && (timeOfDay < 13800);
    }
    
    public boolean isIsNight() {
        return (timeOfDay >= 13800) && (timeOfDay < 22200);
    }
    
    // Events
    
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
    
    public TimeEvent get(String description) {
        try {
            int time = Integer.parseInt(description);
            if ((time < 0) || (time >= 24000)) {
                throw new IllegalArgumentException("Time " + time + " out of range (must be between 0 (inclusive) and 24000 (exclusive))");
            }
        } catch (NumberFormatException e) {
            // Continue
        }
        throw new IllegalArgumentException("Not a recognisable time description: \"" + description + "\" (must be a number between 0 (inclusive) and 24000 (exclusive))");
    }
    
    private final org.bukkit.World realWorld;
    private final int timeOfDay;
}