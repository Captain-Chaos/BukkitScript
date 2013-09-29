/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author pepijn
 */
public class NamedBlockManager {
    public NamedBlockManager(File dataDir) {
        this.dataDir = dataDir;
    }
    
    public NamedBlock create(Location location, String name) {
        if (location == null) {
            throw new NullPointerException("location");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be empty");
        }
        if (namedBlocksByLocation.containsKey(location)) {
            throw new IllegalStateException("There already is a named block at that location (name \"" + namedBlocksByLocation.get(location).getName() + "\")");
        }
        String normalisedName = (location.worldName + "." + name).toLowerCase();
        if (namedBlocksByName.containsKey(normalisedName)) {
            Location existingBlockLocation = namedBlocksByName.get(normalisedName).getLocation();
            throw new IllegalStateException("There already is a block named \"" + name + "\" (@ " + existingBlockLocation + ")");
        }
        NamedBlock block = new NamedBlock(location, name);
        namedBlocksByLocation.put(location, block);
        namedBlocksByName.put(normalisedName, block);
        allBlocks = null;
        allBlocksByWorld.remove(location.worldName);
        return block;
    }

    public void remove(NamedBlock namedBlock) {
        Location location = namedBlock.getLocation();
        namedBlocksByLocation.remove(location);
        namedBlocksByName.remove((location.worldName + "." + namedBlock.getName().trim()).toLowerCase());
        allBlocks = null;
        allBlocksByWorld.remove(location.worldName);
    }

    public NamedBlock get(String worldName, String name) {
        return namedBlocksByName.get((worldName.trim() + "." + name.trim()).toLowerCase());
    }
    
    public NamedBlock get(Location location) {
        return namedBlocksByLocation.get(location);
    }

    public boolean exists(String worldName, String name) {
        return namedBlocksByName.containsKey((worldName.trim() + "." + name.trim()).toLowerCase());
    }

    public boolean exists(Location location) {
        return namedBlocksByLocation.containsKey(location);
    }

    List<NamedBlock> getAll() {
        if (allBlocks == null) {
            List<NamedBlock> blocks = new ArrayList<NamedBlock>(namedBlocksByLocation.values());
            Collections.sort(blocks);
            allBlocks = Collections.unmodifiableList(blocks);
        }
        return allBlocks;
    }

    List<NamedBlock> find(String worldName) {
        String key = worldName.trim().toLowerCase().intern();
        List<NamedBlock> blocks = allBlocksByWorld.get(key);
        if (blocks == null) {
            blocks = new ArrayList<NamedBlock>();
            for (NamedBlock block: namedBlocksByLocation.values()) {
                // Allowed because of intern() above and in Location():
                if (block.getLocation().worldName == key) {
                    blocks.add(block);
                }
            }
            Collections.sort(blocks);
            blocks = Collections.unmodifiableList(blocks);
            allBlocksByWorld.put(key, blocks);
        }
        return blocks;
    }
    
    public void save() {
        // Split the named blocks by world and sort them by name
        SortedMap<String, SortedSet<NamedBlock>> sortedBlocks = new TreeMap<String, SortedSet<NamedBlock>>();
        for (NamedBlock block: namedBlocksByLocation.values()) {
            String worldName = block.getLocation().worldName;
            SortedSet<NamedBlock> set = sortedBlocks.get(worldName);
            if (set == null) {
                set = new TreeSet<NamedBlock>();
                sortedBlocks.put(worldName, set);
            }
            set.add(block);
        }
        
        // Write them out
        YamlConfiguration namedBlocksConfig = new YamlConfiguration();
        for (Map.Entry<String, SortedSet<NamedBlock>> entry: sortedBlocks.entrySet()) {
            String worldName = entry.getKey();
            for (NamedBlock block: entry.getValue()) {
                namedBlocksConfig.set(worldName + "." + block.getName() + ".x", block.getLocation().x);
                namedBlocksConfig.set(worldName + "." + block.getName() + ".y", block.getLocation().y);
                namedBlocksConfig.set(worldName + "." + block.getName() + ".z", block.getLocation().z);
            }
        }
        try {
            if (! dataDir.isDirectory()) {
                dataDir.mkdirs();
            }
            namedBlocksConfig.options().header("This file is automatically generated. Editing it is not recommended. If you\nmake a mistake, you may lose *all* your settings! Always make a backup of the\nfile before editing it!");
            namedBlocksConfig.save(getNamedBlocksFile());
        } catch (IOException e) {
            throw new RuntimeException("I/O error saving named blocks to " + getNamedBlocksFile(), e);
        }
    }
     
    public void load() {
        File namedBlocksFile = getNamedBlocksFile();
        if (namedBlocksFile.isFile()) {
            YamlConfiguration namedBlocksConfig = new YamlConfiguration();
            try {
                namedBlocksConfig.load(namedBlocksFile);
                Map<String, Object> sections = namedBlocksConfig.getValues(false);
                namedBlocksByLocation.clear();
                namedBlocksByName.clear();
                for (Map.Entry<String, Object> entry: sections.entrySet()) {
                    String worldName = entry.getKey().trim().toLowerCase();
                    ConfigurationSection section = (ConfigurationSection) entry.getValue();
                    for (Map.Entry<String, Object> subEntry: section.getValues(false).entrySet()) {
                        String name = subEntry.getKey();
                        ConfigurationSection subSection = (ConfigurationSection) subEntry.getValue();
                        Location location = new Location(worldName, subSection.getInt("x"), subSection.getInt("y"), subSection.getInt("z"));
                        NamedBlock block = new NamedBlock(location, name);
                        namedBlocksByLocation.put(location, block);
                        namedBlocksByName.put(worldName + "." + name.trim().toLowerCase(), block);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error loading named blocks from " + namedBlocksFile, e);
            } catch (InvalidConfigurationException e) {
                throw new RuntimeException("Invalid configuration loading named blocks from " + namedBlocksFile, e);
            }
            allBlocks = null;
            allBlocksByWorld.clear();
            BukkitScriptPlugin.logger.info("[BukkitScript] Loaded " + namedBlocksByLocation.size() + " named block(s)");
        }
    }

    private File getNamedBlocksFile() {
        return new File(dataDir, "namedBlocks.yml");
    }
    
    private final File dataDir;
    private final Map<Location, NamedBlock> namedBlocksByLocation = new HashMap<Location, NamedBlock>();
    private final Map<String, NamedBlock> namedBlocksByName = new HashMap<String, NamedBlock>();
    private List<NamedBlock> allBlocks;
    private final Map<String, List<NamedBlock>> allBlocksByWorld = new HashMap<String, List<NamedBlock>>();
}