package cn.mcarl.miars.storage.storage;

import cn.mcarl.miars.storage.MiarsStorage;
import cn.mcarl.miars.storage.conf.PluginConfig;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.util.List;

public class RedisStorage {

    public Jedis jedis;
    private Gson gson = new Gson();

    public boolean initialize(){

        try {
            MiarsStorage.getInstance().log("	尝试连接到 Redis 数据库...");

            jedis = new Jedis(PluginConfig.REDIS.URL.get());
            jedis.auth(PluginConfig.REDIS.PASSWORD.get());
        } catch (Exception exception) {
            MiarsStorage.getInstance().log("无法连接到数据库，请检查配置文件。");
            exception.printStackTrace();
            return false;
        }

        return true;
    }

    public void shutdown() {
        MiarsStorage.getInstance().log("	关闭 Redis 数据库连接...");
        jedis.close();
    }

    public void setList(String key, List list){
        jedis.set(key,gson.toJson(list));
    }

    public List getList(String key){
        return gson.fromJson(jedis.get(key),List.class);
    }



}