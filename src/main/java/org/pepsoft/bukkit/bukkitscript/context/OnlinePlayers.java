/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.pepsoft.bukkit.bukkitscript.BukkitScriptPlugin;

/**
 *
 * @author pepijn
 */
public class OnlinePlayers extends Players {
    public OnlinePlayers() {
        super(new PlayerProvider() {
            @Override
            public void visitPlayers(PlayerVisitor visitor) {
                for (org.bukkit.entity.Player player: BukkitScriptPlugin.getInstance().getServer().getOnlinePlayers()) {
                    visitor.visitPlayer(player);
                }
            }
        });
    }
    
    public OnlinePlayers(final org.bukkit.World realWorld) {
        super(new PlayerProvider() {
            @Override
            public void visitPlayers(PlayerVisitor visitor) {
                for (org.bukkit.entity.Player player: realWorld.getPlayers()) {
                    visitor.visitPlayer(player);
                }
            }
        });
    }
}