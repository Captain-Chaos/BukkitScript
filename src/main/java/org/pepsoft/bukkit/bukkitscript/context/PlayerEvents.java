/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.pepsoft.bukkit.bukkitscript.event.Event;

/**
 *
 * @author pepijn
 */
public interface PlayerEvents {
    Event getLogin();
    Event getLogout();
}