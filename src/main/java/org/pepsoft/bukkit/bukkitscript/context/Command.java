/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import java.util.*;
import java.util.logging.Level;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;
import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;
import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public class Command extends Event {
    public Command(String name) {
        this.name = name.trim().toLowerCase();
    }

    public String[] execute() {
        return execute(null);
    }
    
    public String[] execute(String args) {
        BukkitScriptCommandSender myCommandSender = new BukkitScriptCommandSender();
        myCommandSender.setOp(true);
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (args != null) {
            sb.append(' ');
            sb.append(args.trim());
        }
        BukkitScriptPlugin.getInstance().getServer().dispatchCommand(myCommandSender, sb.toString());
        return myCommandSender.messages.toArray(new String[myCommandSender.messages.size()]);
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

    final String name;
    
    static final EventListener EVENT_LISTENER = new EventListener();
    
    static class EventListener implements org.bukkit.event.Listener {
        boolean register(Command command) {
            List<Command> events = eventsByCommandName.get(command.name);
            if (events == null) {
                events = new ArrayList<Command>();
                eventsByCommandName.put(command.name, events);
            }
            events.add(command);
            if (! registered) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Registering player command pre process event and server command event listeners");
                }
                BukkitScriptPlugin plugin = BukkitScriptPlugin.getInstance();
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                registered = true;
            }
            return true;
        }
        
        boolean unregister(Command command) {
            List<Command> events = eventsByCommandName.get(command.name);
            if ((events != null) && events.contains(command)) {
                events.remove(command);
                if (events.isEmpty()) {
                    eventsByCommandName.remove(command.name);
                    if (eventsByCommandName.isEmpty()) {
                        if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                            BukkitScriptPlugin.logger.fine("[BukkitScript] Unregistering player command pre process event and server command event listeners");
                        }
                        HandlerList.unregisterAll(this);
                        registered = false;
                    }
                }
                return true;
            }
            return false;
        }
        
        @EventHandler(priority= EventPriority.MONITOR)
        public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event) {
            if (event.isCancelled()) {
                return;
            }
//            long start = System.currentTimeMillis();
            String[] args = parseCommandLine(event.getMessage().substring(1));
            List<Command> commandEvents = eventsByCommandName.get(args[0]);
            if (commandEvents != null) {
                Context context = new Context(event.getPlayer(), args);
                for (Command commandEvent: commandEvents) {
                    commandEvent.fire(context);
                }
                if ((! DEFAULT_COMMANDS.contains(args[0])) && (BukkitScriptPlugin.getInstance().getServer().getPluginCommand(args[0]) == null)) {
                    // It's not an existing command. Cancel the event, so the user does
                    // not get an error message
                    event.setCancelled(true);
                }
            }
//            long duration = System.currentTimeMillis() - start;
//            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
//                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling player command pre process event took " + duration + " ms");
//            }
        }

        @EventHandler(priority= EventPriority.MONITOR)
        public void onServerCommand(ServerCommandEvent event) {
            long start = System.currentTimeMillis();
            String[] args = parseCommandLine(event.getCommand());
            List<Command> commandEvents = eventsByCommandName.get(args[0]);
            if (commandEvents != null) {
                Context context = new Context(event.getSender(), args);
                for (Command commandEvent: commandEvents) {
                    commandEvent.fire(context);
                }
            }
            long duration = System.currentTimeMillis() - start;
            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] Handling server command event took " + duration + " ms");
            }
        }
        
        private String[] parseCommandLine(String commandLine) {
            final int STATE_WAITING_FOR_ARGUMENT    = 1;
            final int STATE_PARSING_ARGUMENT        = 2;
            final int STATE_PARSING_QUOTED_ARGUMENT = 3;
            final int STATE_QUOTE_READ              = 4;
            int state = STATE_WAITING_FOR_ARGUMENT;
            List<String> args = new ArrayList<String>();
            StringBuilder argBuilder = new StringBuilder();
            for (int i = 0; i < commandLine.length(); i++) {
                char c = commandLine.charAt(i);
                switch (state) {
                    case STATE_WAITING_FOR_ARGUMENT:
                        if (c == '"') {
                            state = STATE_PARSING_QUOTED_ARGUMENT;
                        } else if (! Character.isWhitespace(c)) {
                            argBuilder.append(c);
                            state = STATE_PARSING_ARGUMENT;
                        }
                        break;
                    case STATE_PARSING_ARGUMENT:
                        if (Character.isWhitespace(c)) {
                            args.add(argBuilder.toString());
                            argBuilder.setLength(0);
                            state = STATE_WAITING_FOR_ARGUMENT;
                        } else {
                            argBuilder.append(c);
                        }
                        break;
                    case STATE_PARSING_QUOTED_ARGUMENT:
                        if (c == '"') {
                            state = STATE_QUOTE_READ;
                        } else {
                            argBuilder.append(c);
                        }
                        break;
                    case STATE_QUOTE_READ:
                        if (c == '"') {
                            argBuilder.append('"');
                            state = STATE_PARSING_QUOTED_ARGUMENT;
                        } else {
                            args.add(argBuilder.toString());
                            argBuilder.setLength(0);
                            state = STATE_WAITING_FOR_ARGUMENT;
                        }
                        break;
                }
            }
            if (argBuilder.length() > 0) {
                args.add(argBuilder.toString());
            }
            return args.toArray(new String[args.size()]);
        }
        
        private boolean registered;
        private final Map<String, List<Command>> eventsByCommandName = new HashMap<String, List<Command>>();

        private static final Set<String> DEFAULT_COMMANDS = new HashSet<String>(Arrays.asList(
            "list", "stop", "save", "save-on", "save-off", "op", "deop", "ban-ip", 
            "pardon-ip", "ban", "pardon", "kick", "tp", "give", "time", "say", 
            "whitelist", "tell", "me", "kill", "gamemode", "help", "xp", 
            "toggledownfall", "banlist"));
    }
    
    static class BukkitScriptCommandSender implements CommandSender, ServerOperator {
        public BukkitScriptCommandSender() {
            permissibleBase = new PermissibleBase(this);
        }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }

        @Override
        public void sendMessage(String[] messages) {
            for (String message: messages) {
                sendMessage(message);
            }
        }

        @Override
        public Server getServer() {
            return BukkitScriptPlugin.getInstance().getServer();
        }

        @Override
        public String getName() {
            return "BukkitScript";
        }

        @Override
        public boolean isPermissionSet(String name) {
            return permissibleBase.isPermissionSet(name);
        }

        @Override
        public boolean isPermissionSet(Permission perm) {
            return permissibleBase.isPermissionSet(perm);
        }

        @Override
        public boolean hasPermission(String name) {
            return permissibleBase.hasPermission(name);
        }

        @Override
        public boolean hasPermission(Permission perm) {
            return permissibleBase.hasPermission(perm);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
            return permissibleBase.addAttachment(plugin, name, value);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin) {
            return permissibleBase.addAttachment(plugin);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
            return permissibleBase.addAttachment(plugin, name, value, ticks);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
            return permissibleBase.addAttachment(plugin, ticks);
        }

        @Override
        public void removeAttachment(PermissionAttachment attachment) {
            permissibleBase.removeAttachment(attachment);
        }

        @Override
        public void recalculatePermissions() {
            permissibleBase.recalculatePermissions();
        }

        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            return permissibleBase.getEffectivePermissions();
        }

        @Override
        public boolean isOp() {
            return op;
        }

        @Override
        public void setOp(boolean op) {
            this.op = op;
        }

        final List<String> messages = new ArrayList<String>();
        private final PermissibleBase permissibleBase;
        private boolean op;
    }
}