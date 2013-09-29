/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.bukkit.bukkitscript.context;

import org.pepsoft.bukkit.bukkitscript.context.Inventories.InventoryProvider;
import org.pepsoft.bukkit.bukkitscript.context.Inventories.InventoryVisitor;

/**
 *
 * @author pepijn
 */
public class Players implements PlayerActions {
    public Players(PlayerProvider playerProvider) {
        this.playerProvider = playerProvider;
    }
    
    @Override
    public void sendMessage(final String message) {
        playerProvider.visitPlayers(new PlayerVisitor() {
            @Override
            public void visitPlayer(org.bukkit.entity.Player player) {
                player.sendMessage(message);
            }
        });
    }

    @Override
    public void kick(final String message) {
        playerProvider.visitPlayers(new PlayerVisitor() {
            @Override
            public void visitPlayer(org.bukkit.entity.Player player) {
                player.kickPlayer(message);
            }
        });
    }

    @Override
    public void ban(final String message) {
        playerProvider.visitPlayers(new PlayerVisitor() {
            @Override
            public void visitPlayer(org.bukkit.entity.Player player) {
                player.kickPlayer(message);
                player.setBanned(true);
            }
        });
    }

    @Override
    public void pardon() {
        playerProvider.visitPlayers(new PlayerVisitor() {
            @Override
            public void visitPlayer(org.bukkit.entity.Player player) {
                player.setBanned(false);
            }
        });
    }
    
    public Inventories getInventory() {
        return new Inventories(new InventoryProvider() {
            @Override
            public void visitInventories(final InventoryVisitor inventoryVisitor) {
                playerProvider.visitPlayers(new PlayerVisitor() {
                    @Override
                    public void visitPlayer(org.bukkit.entity.Player player) {
                        inventoryVisitor.visit(player.getInventory());
                    }
                });
            }

            @Override
            public int getSize() {
                return 32;
            }
        });
    }
    
    private final PlayerProvider playerProvider;
    
    public interface PlayerProvider {
        void visitPlayers(PlayerVisitor visitor);
    }
    
    public interface PlayerVisitor {
        void visitPlayer(org.bukkit.entity.Player player);
    }
}