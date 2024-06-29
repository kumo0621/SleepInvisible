package com.kumo0621.github.sleepinvisible;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public final class SleepInvisible extends JavaPlugin implements Listener {

    private HashMap<UUID, Long> lastActionTime = new HashMap<>();
    private HashMap<UUID, Boolean> protectedPlayers = new HashMap<>();
    private HashMap<UUID, Long> movementTime = new HashMap<>();
    private long idleThreshold;
    private long moveThreshold;
    private String protectionEnabledMessage;
    private String protectionDisabledMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        idleThreshold = config.getLong("idle_time") * 1000; // 秒をミリ秒に変換
        moveThreshold = config.getLong("move_time") * 1000; // 秒をミリ秒に変換
        Bukkit.getPluginManager().registerEvents(this, this);
        startIdleChecker();
    }

    @Override
    public void onDisable() {
        // プラグインが無効化された時のロジック（特に何もなし）
    }

    // プレイヤーが動いた時のイベントハンドラ
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (!lastActionTime.containsKey(playerId)) {
            lastActionTime.put(playerId, currentTime);
        }

        if (protectedPlayers.containsKey(playerId)) {
            if (!movementTime.containsKey(playerId)) {
                movementTime.put(playerId, currentTime);
            } else {
                long moveStart = movementTime.get(playerId);
                if (currentTime - moveStart >= moveThreshold) {
                    protectedPlayers.remove(playerId);
                    movementTime.remove(playerId);
                    Bukkit.broadcastMessage(player.getName() + " ←こいつ帰ってきた");
                    removeProtection(player);
                }
            }
        } else {
            lastActionTime.put(playerId, currentTime);
            movementTime.remove(playerId);
        }
    }

    // プレイヤーがゲームを退出した時のイベントハンドラ
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastActionTime.remove(playerId);
        protectedPlayers.remove(playerId);
        movementTime.remove(playerId);
    }

    // 放置状態をチェックするタスクを開始
    private void startIdleChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    if (!lastActionTime.containsKey(playerId)) {
                        lastActionTime.put(playerId, currentTime);
                    }

                    long lastAction = lastActionTime.get(playerId);
                    if (currentTime - lastAction >= idleThreshold) {
                        if (!protectedPlayers.containsKey(playerId)) {
                            protectedPlayers.put(playerId, true);
                            Bukkit.broadcastMessage(player.getName() + " ←こいついなくなったお");
                            applyProtection(player);
                        }
                    } else if (protectedPlayers.containsKey(playerId)) {
                        // moveThresholdを過ぎて動いていない場合の処理は必要ない
                        // プレイヤーが動き続けた場合の処理はonPlayerMoveで行われる
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 1秒ごと（20ティック）に実行
    }

    // 無敵のエフェクトをプレイヤーに適用
    private void applyProtection(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 255));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 255));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 255));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 255));
    }

    // 無敵のエフェクトをプレイヤーから解除
    private void removeProtection(Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }
}
