/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

/**
 *
 * @author pepijn
 */
public class CommandNode {
    public Command get(String name) {
        return new Command(name);
    }
    
    public String[] execute(String command) {
        return new Command(command).execute();
    }
    
    public String[] execute(String command, String args) {
        return new Command(command).execute(args);
    }
}