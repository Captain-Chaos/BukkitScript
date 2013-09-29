/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.context.Context;

/**
 *
 * @author pepijn
 */
public abstract class Event {
    public abstract boolean register();
    
    public abstract boolean unregister();
    
    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
    
    protected final void eventFired(Context context) {
        for (Listener listener: listeners) {
            try {
                listener.eventFired(this, context);
            } catch (Throwable t) {
                BukkitScriptPlugin.logger.log(Level.SEVERE, "[BukkitScript] Exception thrown while handling event", t);
                if (context.player != null) {
                    Throwable cause = t;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    context.player.sendMessage(ChatColor.RED + "Error while handling event (type: " + cause.getClass().getSimpleName() + ", message: " + cause.getMessage() + "); check server log");
                }
            }
        }
    }
    
    private List<Listener> listeners = new ArrayList<Listener>();
    
    public interface Listener {
        boolean eventFired(Event event, Context context);
    }
}