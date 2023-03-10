package cn.mcarl.miars.lobby;

import cc.carm.lib.easyplugin.utils.ColorParser;
import cn.mcarl.miars.lobby.conf.PluginConfig;
import cn.mcarl.miars.lobby.listener.BlockListener;
import cn.mcarl.miars.lobby.listener.CitizensListener;
import cn.mcarl.miars.lobby.listener.PlayerListener;
import cn.mcarl.miars.lobby.manager.CitizensManager;
import cn.mcarl.miars.lobby.manager.ConfigManager;
import cn.mcarl.miars.lobby.manager.ScoreBoardManager;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MiarsLobby extends JavaPlugin {

    private static MiarsLobby instance;
    public static MiarsLobby getInstance() {
        return instance;
    }
    protected ConfigManager configManager;

    @SneakyThrows
    @Override
    public void onEnable() {
        instance = this;

        log(getName() + " " + getDescription().getVersion() + " &7开始加载...");

        long startTime = System.currentTimeMillis();

        log("正在初始化配置文件...");
        this.configManager = new ConfigManager(getDataFolder());

        log("正在注册监听器...");
        regListener(new PlayerListener());
        regListener(new BlockListener());
        regListener(new CitizensListener());

        log("正在初始化世界NPC...");
        CitizensManager.getInstance().init(PluginConfig.LOBBY_SITE.LOCATION.get().getWorld());

        log("正在计分板...");
        ScoreBoardManager.getInstance().init();

        log("加载完成 ,共耗时 " + (System.currentTimeMillis() - startTime) + " ms 。");

        showAD();
    }

    @SneakyThrows
    @Override
    public void onDisable() {
        log(getName() + " " + getDescription().getVersion() + " 开始卸载...");
        long startTime = System.currentTimeMillis();

        log("卸载监听器...");
        Bukkit.getServicesManager().unregisterAll(this);

        log("卸载完成 ,共耗时 " + (System.currentTimeMillis() - startTime) + " ms 。");

        showAD();
    }

    /**
     * 注册监听器
     *
     * @param listener 监听器
     */
    public static void regListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, getInstance());
    }

    /**
     * 注册指令
     *
     * @param name 指令名字
     * @param command 指令
     */
    public static void regCommand(String name, CommandExecutor command) {
        Bukkit.getPluginCommand(name).setExecutor(command);
    }

    /**
     * 日志
     * @param message 日志消息
     */
    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(ColorParser.parse("[" + getInstance().getName() + "] " + message));
    }

    /**
     * 作者信息
     */
    private void showAD() {
        log("&7感谢您使用 &c&l"+getDescription().getName()+" v" + getDescription().getVersion());
        log("&7本插件由 &c&lMCarl Studios &7提供长期支持与维护。");
    }
}
