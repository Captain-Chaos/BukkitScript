/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import javax.script.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.pepsoft.bukkit.bukkitscript.context.Context;
import org.pepsoft.bukkit.bukkitscript.event.Event;
import org.pepsoft.bukkit.bukkitscript.event.Event.Listener;
import org.pepsoft.bukkit.bukkitscript.event.EventManager;
import org.pepsoft.bukkit.bukkitscript.event.EventSpec;
import org.pepsoft.util.FileUtils;

/**
 *
 * @author pepijn
 */
public class ScriptManager {
    public ScriptManager(BukkitScriptPlugin plugin) {
        this.plugin = plugin;
        eventManager = new EventManager();
    }
    
    public Set<String> getScriptNames() {
        return Collections.unmodifiableSet(scripts.keySet());
    }
    
    public Set<Script> getScripts() {
        return new HashSet<Script>(scripts.values());
    }
    
    public Script getScript(String name) {
        return scripts.get(name);
    }
    
    public boolean bindEvent(String eventDescriptor, final Script script, Context context) {
        EventSpec spec = new EventSpec(eventDescriptor, script.getName(), context.toProperties());
        if (boundEvents.containsKey(spec)) {
            throw new IllegalStateException("Script " + script.getName() + " already bound to event " + eventDescriptor);
        }
        Event event = eventManager.createEvent(eventDescriptor, context);
        if (event.register()) {
            boundEvents.put(spec, event);
            event.addListener(new Listener() {
                @Override
                public boolean eventFired(Event event, Context context) {
                    script.execute(context);
                    return true;
                }
            });
            return true;
        } else {
            return false;
        }
    }
    
    public boolean unbindEvent(String eventDescriptor, Script script) {
        EventSpec spec = new EventSpec(eventDescriptor, script.getName(), null);
        if (boundEvents.containsKey(spec)) {
            Event event = boundEvents.get(spec);
            if (! event.unregister()) {
                throw new RuntimeException("Could not unbind event " + event);
            }
            boundEvents.remove(spec);
            return true;
        } else {
            throw new IllegalStateException("Script " + script.getName() + " not bound to event " + eventDescriptor);
        }
    }
    
    public boolean unbindEvent(String eventDescriptor) {
        boolean eventsUnbound = false;
        for (Iterator<Map.Entry<EventSpec, Event>> i = boundEvents.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<EventSpec, Event> entry = i.next();
            if (entry.getKey().getEventDescriptor().equals(eventDescriptor)) {
                Event event = entry.getValue();
                if (! event.unregister()) {
                    throw new RuntimeException("Could not unbind event " + event);
                }
                i.remove();
                eventsUnbound = true;
            }
        }
        return eventsUnbound;
    }
    
    public int reloadScripts() {
        // TODO: this doesn't pick up new scripts
        sourceCache.clear();
        codeCache.clear();
        return scripts.size();
    }
    
    public Set<EventSpec> getBoundEvents() {
        return Collections.unmodifiableSet(boundEvents.keySet());
    }

    public void load() {
        // Load scripts
        scripts.clear();
        sourceCache.clear();
        codeCache.clear();
        File scriptDir = new File(plugin.getDataFolder(), "scripts");
        if (! scriptDir.isDirectory()) {
            // Create if it doesn't exist, so it is there for users after the
            // first run even though there are no scripts in it yet
            scriptDir.mkdirs();
        }
        File[] scriptFiles = scriptDir.listFiles();
        for (File file: scriptFiles) {
            if (file.isFile()) {
                if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                    BukkitScriptPlugin.logger.fine("[BukkitScript] Loading script " + file);
                }
                String name = file.getName();
                int p = name.lastIndexOf('.');
                if (p != -1) {
                    String lang = name.substring(p + 1);
                    if (scriptEngineManager.getEngineByExtension(lang) == null) {
                        BukkitScriptPlugin.logger.warning("[BukkitScript] Skipping " + file + " because the extension " + lang + " is not supported");
                        continue;
                    }
                    name = name.substring(0, p);
                    Script script = new Script(this, name, lang, file);
                    scripts.put(name, script);
                } else {
                    BukkitScriptPlugin.logger.warning("[BukkitScript] Skipping " + file + " because it has no recognized extension");
                }
            } else {
                BukkitScriptPlugin.logger.warning("[BukkitScript] Skipping " + file + " because it is not a regular file");
            }
        }
        BukkitScriptPlugin.logger.info("[BukkitScript] Loaded " + scripts.size() + " script(s)");
        
        // Load events
        boundEvents.clear();
        File eventsFile = getEventsFile();
        if (eventsFile.isFile()) {
            YamlConfiguration eventsConfig = new YamlConfiguration();
            eventsConfig.options().pathSeparator('/');
            try {
                eventsConfig.load(eventsFile);
                Map<String, Object> sections = eventsConfig.getValues(false);
                for (Map.Entry<String, Object> entry: sections.entrySet()) {
                    String eventDescriptor = entry.getKey();
                    ConfigurationSection section = (ConfigurationSection) entry.getValue();
                    String scriptName = section.getString("script");
                    if (! scripts.containsKey(scriptName)) {
                        BukkitScriptPlugin.logger.severe("[BukkitScript] Skipping event " + eventDescriptor + " because the specified script \"" + scriptName + "\" does not exist");
                        continue;
                    }
                    Properties context = new Properties();
                    for (Map.Entry<String, Object> subEntry: section.getConfigurationSection("context").getValues(false).entrySet()) {
                        String key = subEntry.getKey();
                        String value = (String) subEntry.getValue();
                        context.put(key, value);
                    }
                    try {
                        if (! bindEvent(eventDescriptor, scripts.get(scriptName), Context.fromProperties(context))) {
                            BukkitScriptPlugin.logger.severe("[BukkitScript] Event " + eventDescriptor + " could not be registered for an unknown reason");
                        }
                    } catch (IllegalArgumentException e) {
                        BukkitScriptPlugin.logger.severe("[BukkitScript] Event " + eventDescriptor + " could not be registered because the descriptor is not valid (message: " + e.getMessage() + ")");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error loading events from " + eventsFile, e);
            } catch (InvalidConfigurationException e) {
                throw new RuntimeException("Invalid configuration loading events from " + eventsFile, e);
            }
            BukkitScriptPlugin.logger.info("[BukkitScript] Loaded " + boundEvents.size() + " event(s)");
        }
    }
    
    public void save() {
        // Save events
        YamlConfiguration eventsConfig = new YamlConfiguration();
        eventsConfig.options().pathSeparator('/');
        for (EventSpec eventSpec: boundEvents.keySet()) {
            eventsConfig.set(eventSpec.getEventDescriptor() + "/script", eventSpec.getScriptName());
            for (Map.Entry<Object, Object> contextEntry: eventSpec.getContext().entrySet()) {
                eventsConfig.set(eventSpec.getEventDescriptor() + "/context/" + contextEntry.getKey(), contextEntry.getValue());
            }
        }
        File dataDir = plugin.getDataFolder();
        try {
            if (! dataDir.isDirectory()) {
                dataDir.mkdirs();
            }
            eventsConfig.options().header("This file is automatically generated. Editing it is not recommended. If you\nmake a mistake, you may lose *all* your settings! Always make a backup of the\nfile before editing it!");
            eventsConfig.save(getEventsFile());
        } catch (IOException e) {
            throw new RuntimeException("I/O error saving events to " + getEventsFile(), e);
        }
    }
    
    void execute(Script script, org.pepsoft.bukkit.bukkitscript.context.Context context) {
        long start = System.currentTimeMillis();
        ScriptEngine engine = null;
        CompiledScript code = codeCache.get(script);
        if (code == null) {
            try {
                engine = scriptEngineManager.getEngineByExtension(script.getLang());
                engine.put(ScriptEngine.FILENAME, script.getFile().getName());
                if (engine instanceof Compilable) {
                    if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                        BukkitScriptPlugin.logger.fine("[BukkitScript] Compiling script " + script.getName());
                    }
                    code = ((Compilable) engine).compile(getSource(script));
                    long now = System.currentTimeMillis();
                    long duration = now - start;
                    start = now;
                    if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                        BukkitScriptPlugin.logger.fine("[BukkitScript] Compiling script " + script.getName() + " took " + duration + " ms");
                    }
                } else {
                    code = UNCOMPILABLE;
                }
                codeCache.put(script, code);
            } catch (IOException e) {
                throw new RuntimeException("I/O error while reading script " + script.getFile(), e);
            } catch (ScriptException e) {
                throw new RuntimeException("Script error while compiling script " + script.getName(), e);
            }
        }
        Bindings bindings = new SimpleBindings();
        bindings.put("world", context.world);
        bindings.put("player", context.player);
        bindings.put("block", context.block);
        bindings.put("command", context.command);
        bindings.put("item", context.item);
        bindings.put("material", context.material);
        bindings.put("idle", context.idle);
        if (context.time != null) {
            bindings.put("time", context.time);
        }
        if (context.args != null) {
            bindings.put("args", context.args);
        }
        try {
            if (code == UNCOMPILABLE) {
                if (engine == null) {
                    engine = scriptEngineManager.getEngineByExtension(script.getLang());
                    engine.put(ScriptEngine.FILENAME, script.getFile().getName());
                }
                engine.eval(getSource(script), bindings);
            } else {
                code.eval(bindings);
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while executing script " + script.getName(), e);
        } catch (ScriptException e) {
            throw new RuntimeException("Script error while executing script " + script.getName(), e);
        }
        long duration = System.currentTimeMillis() - start;
        if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
            BukkitScriptPlugin.logger.fine("[BukkitScript] Executing script " + script.getName() + " took " + duration + " ms");
        }
    }
    
    private File getEventsFile() {
        return new File(plugin.getDataFolder(), "events.yml");
    }
    
    private String getSource(Script script) throws IOException {
        String source = sourceCache.get(script);
        if (source == null) {
            if (BukkitScriptPlugin.logger.isLoggable(Level.FINE)) {
                BukkitScriptPlugin.logger.fine("[BukkitScript] Loading source code of script " + script.getName());
            }
            source = FileUtils.read(script.getFile(), Charset.forName("UTF-8"));
            sourceCache.put(script, source);
        }
        return source;
    }
    
    public static void main(String[] args) {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        for (ScriptEngineFactory factory: scriptEngineManager.getEngineFactories()) {
            System.out.println(factory.getEngineName());
            System.out.println("    Version: " + factory.getEngineVersion());
            System.out.println("    Language: " + factory.getLanguageName());
            System.out.println("    Language version: " + factory.getLanguageVersion());
            System.out.println("    Extensions: " + factory.getExtensions());
        }
    }
    
    private final BukkitScriptPlugin plugin;
    private final EventManager eventManager;
    private final Map<String, Script> scripts = new HashMap<String, Script>();
    private final Map<Script, String> sourceCache = new HashMap<Script, String>();
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private final Map<Script, CompiledScript> codeCache = new HashMap<Script, CompiledScript>();
    private final Map<EventSpec, Event> boundEvents = new HashMap<EventSpec, Event>();
    
    private static final CompiledScript UNCOMPILABLE = new CompiledScript() {
        @Override public Object eval(ScriptContext context) throws ScriptException {return null;}
        @Override public ScriptEngine getEngine() {return null;}
    };
}