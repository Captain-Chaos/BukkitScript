/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript;

import java.io.File;
import org.pepsoft.bukkit.bukkitscript.context.Context;

/**
 *
 * @author pepijn
 */
public class Script {
    public Script(ScriptManager scriptManager, String name, String lang, File file) {
        this.scriptManager = scriptManager;
        this.name = name;
        this.lang = lang;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public String getLang() {
        return lang;
    }

    public File getFile() {
        return file;
    }
    
    public void execute(Context context) {
        scriptManager.execute(this, context);
    }
    
    private final ScriptManager scriptManager;
    private final String name, lang;
    private final File file;
}