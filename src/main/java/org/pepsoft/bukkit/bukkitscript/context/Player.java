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
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public class Player implements PlayerActions, PlayerEvents {
    public Player(org.bukkit.World realWorld, OfflinePlayer realPlayer, CommandSender commandSender) {
        if (realWorld != null) {
            all = new OnlinePlayers(realWorld);
        } else if (realPlayer != null) {
            org.bukkit.entity.Player onlinePlayer = realPlayer.getPlayer();
            if (onlinePlayer != null) {
                all = new OnlinePlayers(onlinePlayer.getWorld());
            } else {
                all = new OnlinePlayers();
            }
        } else {
            all = new OnlinePlayers();
        }
        this.realPlayer = realPlayer;
        if ((commandSender == null) && (realPlayer instanceof CommandSender)) {
            this.commandSender = (CommandSender) realPlayer;
        } else {
            this.commandSender = commandSender;
        }
    }
    
    // Properties
    
    public Player get(String name) {
        return new Player(null, BukkitScriptPlugin.getInstance().getServer().getOfflinePlayer(name), commandSender);
    }
    
    public String getName() {
        return (realPlayer != null) ? realPlayer.getName() : ((commandSender != null) ? commandSender.getName() : null);
    }
    
    public Inventory getInventory() {
        return ((realPlayer != null) && (realPlayer.getPlayer() != null)) ? new Inventory(realPlayer.getPlayer().getInventory()) : null;
    }
    
    public boolean isIsOnline() {
        return (realPlayer != null) ? realPlayer.isOnline() : false;
    }
    
    public boolean isIsOp() {
        return (realPlayer != null) ? realPlayer.isOp() : false;
    }
    
    public boolean hasPermission(String permission) {
        if (realPlayer != null) {
            org.bukkit.entity.Player onlinePlayer = realPlayer.getPlayer();
            if (onlinePlayer != null) {
                return onlinePlayer.hasPermission(permission);
            }
        }
        return false;
    }
    
    /**
     * Check whether the player is in the specified volume.
     * 
     * @param volume
     * @return 
     */
    public boolean isIn(Box volume) {
        if (realPlayer != null) {
            org.bukkit.entity.Player onlinePlayer = realPlayer.getPlayer();
            if (onlinePlayer != null) {
                return volume.contains(onlinePlayer.getEyeLocation());
            }
        }
        return false;
    }

    // Actions
    
    @Override
    public void sendMessage(String message) {
        if (realPlayer != null) {
            org.bukkit.entity.Player onlinePlayer = realPlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(message);
            }
        } else if (commandSender != null) {
            commandSender.sendMessage(message);
        }
    }

    @Override
    public void kick(String message) {
        if (realPlayer != null) {
            org.bukkit.entity.Player onlinePlayer = realPlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.kickPlayer(message);
            }
        }
    }

    @Override
    public void ban(String message) {
        if (realPlayer != null) {
            org.bukkit.entity.Player onlinePlayer = realPlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.kickPlayer(message);
            }
            realPlayer.setBanned(true);
        }
    }

    @Override
    public void pardon() {
        if (realPlayer != null) {
            realPlayer.setBanned(false);
        }
    }
    
    // Events

    @Override
    public Event getLogin() {
        return (realPlayer != null) ? new Login(realPlayer) : null;
    }

    @Override
    public Event getLogout() {
        return (realPlayer != null) ? new Logout(realPlayer) : null;
    }
    
    OfflinePlayer getRealPlayer() {
        return realPlayer;
    }
    
    public final Players all;
    private final OfflinePlayer realPlayer;
    private final CommandSender commandSender;
    
    static final EventListener EVENT_LISTENER = new EventListener();
    
    static class Login extends Event {
        public Login(OfflinePlayer realPlayer) {
            this.realPlayer = realPlayer;
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
        
        final OfflinePlayer realPlayer;
    }
    
    static class Logout extends Event {
        public Logout(OfflinePlayer realPlayer) {
            this.realPlayer = realPlayer;
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
        
        final OfflinePlayer realPlayer;
    }
    
    static class EventListener implements Listener {
        boolean register(Event event) {
            if (event instanceof Login) {
                String playerName = ((Login) event).realPlayer.getName();
                Set<Event> events = loginEvents.get(playerName);
                if (events == null) {
                    events = new HashSet<Event>();
                    loginEvents.put(playerName, events);
                }
                if (events.contains(event)) {
                    return false;
                }
                events.add(event);
            } else if (event instanceof Logout) {
                String playerName = ((Login) event).realPlayer.getName();
                Set<Event> events = logoutEvents.get(playerName);
                if (events == null) {
                    events = new HashSet<Event>();
                    logoutEvents.put(playerName, events);
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
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Registering player join and player quit event handlers");
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
            if (event instanceof Login) {
                String playerName = ((Login) event).realPlayer.getName();
                Set<Event> events = loginEvents.get(playerName);
                if ((events != null) && events.contains(event)) {
                    events.remove(event);
                    if (events.isEmpty()) {
                        loginEvents.remove(playerName);
                        if (loginEvents.isEmpty() && logoutEvents.isEmpty()) {
                            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                                BukkitScriptPlugin.logger.fine("[BukkitScript] Unregistering player join and player quit event handlers");
                            }
                            HandlerList.unregisterAll(this);
                        }
                    }
                    return true;
                }
            } else if (event instanceof Logout) {
                String playerName = ((Logout) event).realPlayer.getName();
                Set<Event> events = logoutEvents.get(playerName);
                if ((events != null) && events.contains(event)) {
                    events.remove(event);
                    if (events.isEmpty()) {
                        loginEvents.remove(playerName);
                        if (loginEvents.isEmpty() && logoutEvents.isEmpty()) {
                            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                                BukkitScriptPlugin.logger.fine("[BukkitScript] Unregistering player join and player quit event handlers");
                            }
                            HandlerList.unregisterAll(this);
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
        public void onPlayerJoin(PlayerJoinEvent event) {
//            long start = System.currentTimeMillis();
            Set<Event> events = loginEvents.get(event.getPlayer().getName());
            if (events != null) {
                Context context = new Context(event.getPlayer());
                for (Event loginEvent: events) {
                    ((Login) loginEvent).fire(context);
                }
            }
//            long duration = System.currentTimeMillis() - start;
//            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
//                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling player join event took " + duration + " ms");
//            }
        }

        @EventHandler(priority= EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
//            long start = System.currentTimeMillis();
            Set<Event> events = logoutEvents.get(event.getPlayer().getName());
            if (events != null) {
                Context context = new Context(event.getPlayer());
                for (Event loginEvent: events) {
                    ((Login) loginEvent).fire(context);
                }
            }
//            long duration = System.currentTimeMillis() - start;
//            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
//                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling player quit event took " + duration + " ms");
//            }
        }
        
        private final Map<String, Set<Event>> loginEvents = new HashMap<String, Set<Event>>();
        private final Map<String, Set<Event>> logoutEvents = new HashMap<String, Set<Event>>();
        private boolean registered;
    }
}