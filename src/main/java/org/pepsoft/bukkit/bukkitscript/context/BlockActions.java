/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

/**
 *
 * @author pepijn
 */
public interface BlockActions {
    /**
     * Interact with the block. It depends on the block what this does. A door
     * will open or close, a lever will switch, a push button will be pushed,
     * etc.
     */
    void interact();

    /**
     * Synonym for {@link #interact()}.
     */
    void push();
    
    /**
     * Switch the block off. Currently only works on levers.
     */
    void switchOff();

    /**
     * Switch the block on. Currently only works on levers.
     */
    void switchOn();
    
    /**
     * Open the block. Currently only works on doors.
     */
    void open();
    
    /**
     * Close the block. Currently only works on doors.
     */
    void close();
}