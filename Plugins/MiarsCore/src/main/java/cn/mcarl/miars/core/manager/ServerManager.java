package cn.mcarl.miars.core.manager;

import cn.mcarl.miars.core.MiarsCore;
import cn.mcarl.miars.core.conf.PluginConfig;
import cn.mcarl.miars.storage.entity.MServerInfo;
import cn.mcarl.miars.core.utils.HttpClientHelper;
import com.alibaba.fastjson.JSON;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;

/**
 * @Author: carl0
 * @DATE: 2022/11/11 23:34
 */
public class ServerManager {
    private static final ServerManager instance = new ServerManager();
    public static ServerManager getInstance() {
        return instance;
    }
    BukkitTask bukkitRunnable;

    /**
     * 开启服务器,并创建Bungee服务器
     * @throws IOException
     */
    public void onStartServer() {
        String name = PluginConfig.SERVER_INFO.NAME.get();
        String url = PluginConfig.SERVER_INFO.URL.get();
        int port = MiarsCore.getInstance().getServer().getPort();
        try {
            HttpClientHelper.sendGet(url+"/add/server?name="+name+"&port="+port);
        } catch (IOException e) {
            MiarsCore.getInstance().log("启动代理模式失败,代理服务器可能没有正常运行...");
        }
        if (bukkitRunnable == null){
            tick();
        }
    }

    /**
     * 关闭服务器,并移除Bungee内的服务器
     * @throws IOException
     */
    public void onStopServer() {
        bukkitRunnable.cancel();
        String name = PluginConfig.SERVER_INFO.NAME.get();
        String url = PluginConfig.SERVER_INFO.URL.get();
        try {
            HttpClientHelper.sendGet(url+"/remove/server?name="+name);
        } catch (IOException e) {
            MiarsCore.getInstance().log("移除代理失败,代理服务器已经提前关闭...");
        }
    }


    /**
     * 获取服务器信息
     * @throws IOException
     */
    public MServerInfo getServerInfo(String name) {
        String state = "error";
        String url = PluginConfig.SERVER_INFO.URL.get();
        try {
            state = HttpClientHelper.sendGet(url+"/info?name="+name);
        } catch (IOException e){
            MiarsCore.getInstance().log("代理服务器链接已关闭,无法获取有效信息...");
        }
        if (state.contains("error")){
            return null;
        }
        return JSON.toJavaObject(JSON.parseObject(state),MServerInfo.class);
    }

    /**
     * 循环判断Bungee中是否正常的存在该服务器的数据
     */
    public void tick(){
        bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (getServerInfo(PluginConfig.SERVER_INFO.NAME.get())==null){
                    onStartServer();
                }
            }
        }.runTaskTimerAsynchronously(MiarsCore.getInstance(),0,300);
    }
}
