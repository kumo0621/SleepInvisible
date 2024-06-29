package com.kumo0621.github.sleepinvisible;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public final class SleepInvisible extends JavaPlugin implements Listener {

    private HashMap<UUID, Long> lastActionTime = new HashMap<>();
    private HashMap<UUID, Boolean> protectedPlayers = new HashMap<>();
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
        lastActionTime.put(player.getUniqueId(), System.currentTimeMillis());
        if (protectedPlayers.containsKey(player.getUniqueId())) {
            protectedPlayers.remove(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL); // サバイバルモードに戻す
            Bukkit.broadcastMessage(player.getName() + " ←こいつ帰ってきた");
        }
    }

    // プレイヤーがゲームを退出した時のイベントハンドラ
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastActionTime.remove(playerId);
        protectedPlayers.remove(playerId);
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
                            player.setGameMode(GameMode.SPECTATOR); // スペクテーターモードに変更
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 1秒ごと（20ティック）に実行
    }
}
