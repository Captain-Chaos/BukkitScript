/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

/**
 *
 * @author pepijn
 */
public interface PlayerActions {
    void sendMessage(String message);
    void kick(String message);
    void ban(String message);
    void pardon();
//    void giveXP(int xp);
//    void takeXP(int xp);
}