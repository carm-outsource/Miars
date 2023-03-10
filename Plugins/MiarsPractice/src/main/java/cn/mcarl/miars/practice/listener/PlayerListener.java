package cn.mcarl.miars.practice.listener;

import cc.carm.lib.easyplugin.utils.ColorParser;
import cn.mcarl.miars.core.manager.ServerManager;
import cn.mcarl.miars.core.utils.ToolUtils;
import cn.mcarl.miars.core.utils.jsonmessage.JSONMessage;
import cn.mcarl.miars.practice.MiarsPractice;
import cn.mcarl.miars.practice.conf.PluginConfig;
import cn.mcarl.miars.practice.manager.ArenaManager;
import cn.mcarl.miars.practice.manager.ItemInteractManager;
import cn.mcarl.miars.practice.manager.PlayerInventoryManager;
import cn.mcarl.miars.practice.manager.ScoreBoardManager;
import cn.mcarl.miars.storage.entity.MPlayer;
import cn.mcarl.miars.storage.entity.MRank;
import cn.mcarl.miars.storage.entity.practice.Arena;
import cn.mcarl.miars.storage.entity.practice.ArenaState;
import cn.mcarl.miars.storage.enums.FKitType;
import cn.mcarl.miars.storage.storage.data.MPlayerDataStorage;
import cn.mcarl.miars.storage.storage.data.MRankDataStorage;
import cn.mcarl.miars.storage.storage.data.practice.PracticeArenaStateDataStorage;
import cn.mcarl.miars.storage.storage.data.practice.PracticeDailyStreakDataStorage;
import cn.mcarl.miars.storage.storage.data.practice.PracticeGameDataStorage;
import cn.mcarl.miars.storage.utils.BukkitUtils;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class PlayerListener implements Listener {

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e){
        Player player = e.getPlayer();
        ToolUtils.playerInitialize(player);
        player.getInventory().clear();

        ArenaState state = PracticeArenaStateDataStorage.getInstance().getArenaStateByPlayer(player);
        if (state==null){
            if (!player.hasPermission("miars.admin")){
                player.kickPlayer("&c???????????????");
            }
        }else {

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!(state.getState()>=2)){
                        ServerManager.getInstance().sendPlayerToServer(player.getName(),"practice");
                    }
                    cancel();
                }
            }.runTaskLaterAsynchronously(MiarsPractice.getInstance(),160);

            Arena arena = ArenaManager.getInstance().getArenaById(state.getArenaId());
            if (state.getPlayerA().equals(player.getName())){
                player.teleport(arena.getLoc1());
            }else {
                player.teleport(arena.getLoc2());
            }

            // ????????????????????????
            ScoreBoardManager.getInstance().joinPlayer(player);
            // ????????????
            PlayerInventoryManager.getInstance().init(player);

            // ??????????????????
            if ((Bukkit.getPlayer(state.getPlayerA()) !=null && Bukkit.getPlayer(state.getPlayerA()).isOnline()) && (Bukkit.getPlayer(state.getPlayerB()) != null && Bukkit.getPlayer(state.getPlayerB()).isOnline())){
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 5; i++) {
                            try {
                                JSONMessage.create(ColorParser.parse("&7???????????? &c"+(5-i)+" &7??????????????????..."))
                                        .send(Bukkit.getPlayer(state.getPlayerA()),Bukkit.getPlayer(state.getPlayerB()));
                                if (i == 4){
                                    ArenaManager.getInstance().startGame(state.getArenaId());

                                    JSONMessage.create(ColorParser.parse("&7??????????????????????????????"))
                                            .send(Bukkit.getPlayer(state.getPlayerA()),Bukkit.getPlayer(state.getPlayerB()));
                                    break;
                                }
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                        cancel();
                    }
                }.runTaskAsynchronously(MiarsPractice.getInstance());
            }

        }
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent e){
        Player player = e.getPlayer();

        // ?????????????????????
        ScoreBoardManager.getInstance().removePlayer(player);

        ArenaState state = PracticeArenaStateDataStorage.getInstance().getArenaStateByPlayer(player);

        if (state!=null){
            // ??????????????????
            if (state.getState() == 2){
                Player deathPlayer = e.getPlayer();
                Player attackPlayer = state.getPlayerA().equals(deathPlayer.getName()) ? Bukkit.getPlayer(state.getPlayerB()) : Bukkit.getPlayer(state.getPlayerA());



                state.setWin(attackPlayer.getName());
                state.setEndTime(System.currentTimeMillis());
                state.setFKitType(FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()));
                state.setState(3);


                if (deathPlayer.getName().equals(state.getPlayerA())){
                    state.setAFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(deathPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
                    state.setBFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(attackPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
                }else {
                    state.setAFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(attackPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
                    state.setBFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(deathPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
                }

                attackPlayer.sendTitle(ColorParser.parse("&a&lVICTORY!"),ColorParser.parse("&7"+attackPlayer.getName()+" &fwon the match"));
                PracticeDailyStreakDataStorage.getInstance().putDailyStreakData(attackPlayer,state.getQueueType(), state.getFKitType(),true);
                PracticeDailyStreakDataStorage.getInstance().putDailyStreakData(deathPlayer,state.getQueueType(), state.getFKitType(),false);


                PracticeGameDataStorage.getInstance().putArenaData(state); // ???????????????????????????
                ArenaState arenaState = PracticeGameDataStorage.getInstance().getArenaDataByEndTime(state.getEndTime()); // ???????????????????????????
                ArenaManager.getInstance().endGame(state.getArenaId()); // ??????????????????

                JSONMessage.create(ColorParser.parse("&r\n&6Post-Match Inventories &7(Click name to view)"))
                        .send(attackPlayer);
                JSONMessage.create(ColorParser.parse("&aWinner: "))
                        .then(ColorParser.parse("&e"+attackPlayer.getName()))
                        .tooltip(ColorParser.parse("&7Click view"))
                        .runCommand("/miars practice openInv "+attackPlayer.getName()+" "+arenaState.getId())
                        .then(ColorParser.parse("&7 - &r"))
                        .then(ColorParser.parse("&cLoser: "))
                        .then(ColorParser.parse("&e"+deathPlayer.getName()))
                        .tooltip(ColorParser.parse("&7Click view"))
                        .runCommand("/miars practice openInv "+deathPlayer.getName()+" "+arenaState.getId())
                        .then(ColorParser.parse("\n&r"))
                        .send(attackPlayer);

                // 5?????????????????????????????????
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ServerManager.getInstance().sendPlayerToServer(attackPlayer.getName(),"practice");
                    }
                }.runTaskLaterAsynchronously(MiarsPractice.getInstance(),100);
            }

            // ????????????
            if (state.getState() == 3){
                ArenaManager.getInstance().releaseArena(PracticeArenaStateDataStorage.getInstance().getArenaStateByPlayer(player).getArenaId());
            }
        }

    }


    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent e){
        Player player = e.getPlayer();
        ArenaState state = PracticeArenaStateDataStorage.getInstance().getArenaStateByPlayer(player);

        // ????????????????????????????????????
        if (!player.hasPermission("miars.admin") && player.getLocation().getY()<0){
            Arena arena = ArenaManager.getInstance().getArenaById(state.getArenaId());
            if (state.getPlayerA().equals(player.getName())){
                player.teleport(arena.getLoc1());
            }else {
                player.teleport(arena.getLoc2());
            }
        }
    }

    @EventHandler
    public void PlayerDeathEvent(PlayerDeathEvent e){

        e.setDeathMessage(null);
        e.setKeepInventory(true);

        Player deathPlayer = e.getEntity(); // ???????????????
        ArenaState state = PracticeArenaStateDataStorage.getInstance().getArenaStateByPlayer(deathPlayer);
        Location location = deathPlayer.getLocation();
        deathPlayer.spigot().respawn();
        deathPlayer.teleport(location);

        Player attackPlayer;
        if (e.getEntity().getKiller() != null) {
            attackPlayer = e.getEntity().getKiller(); // ???????????????
        }else {
            attackPlayer = deathPlayer.getName().equals(state.getPlayerA()) ? Bukkit.getPlayer(state.getPlayerB()):Bukkit.getPlayer(state.getPlayerA());
        }

        state.setWin(attackPlayer.getName());
        state.setEndTime(System.currentTimeMillis());
        state.setFKitType(FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()));
        state.setState(3);

        if (deathPlayer.getName().equals(state.getPlayerA())){
            state.setAFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(deathPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
            state.setBFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(attackPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
        }else {
            state.setAFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(attackPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
            state.setBFInventory(BukkitUtils.ItemStackConvertByte(ToolUtils.playerToFInv(deathPlayer, FKitType.valueOf(PluginConfig.PRACTICE_SITE.MODE.get()))));
        }


        attackPlayer.sendTitle(ColorParser.parse("&a&lVICTORY!"),ColorParser.parse("&7"+attackPlayer.getName()+" &fwon the match"));
        PracticeDailyStreakDataStorage.getInstance().putDailyStreakData(attackPlayer,state.getQueueType(), state.getFKitType(),true);
        deathPlayer.sendTitle(ColorParser.parse("&c&lDEFEAT!"),ColorParser.parse("&7"+attackPlayer.getName()+" &fwon the match"));
        PracticeDailyStreakDataStorage.getInstance().putDailyStreakData(deathPlayer,state.getQueueType(), state.getFKitType(),false);


        PracticeGameDataStorage.getInstance().putArenaData(state); // ???????????????????????????
        ArenaState arenaState = PracticeGameDataStorage.getInstance().getArenaDataByEndTime(state.getEndTime()); // ???????????????????????????
        ArenaManager.getInstance().endGame(state.getArenaId()); // ??????????????????

        JSONMessage.create(ColorParser.parse("&r\n&6Post-Match Inventories &7(Click name to view)"))
                .send(attackPlayer,deathPlayer);
        JSONMessage.create(ColorParser.parse("&aWinner: "))
                .then(ColorParser.parse("&e"+attackPlayer.getName()))
                .tooltip(ColorParser.parse("&7Click view"))
                .runCommand("/miars practice openInv "+attackPlayer.getName()+" "+arenaState.getId())
                .then(ColorParser.parse("&7 - &r"))
                .then(ColorParser.parse("&cLoser: "))
                .then(ColorParser.parse("&e"+deathPlayer.getName()))
                .tooltip(ColorParser.parse("&7Click view"))
                .runCommand("/miars practice openInv "+deathPlayer.getName()+" "+arenaState.getId())
                .then(ColorParser.parse("\n&r"))
                .send(attackPlayer,deathPlayer);

        // 5?????????????????????????????????
        new BukkitRunnable() {
            @Override
            public void run() {
                ServerManager.getInstance().sendPlayerToServer(deathPlayer.getName(),"practice");
                ServerManager.getInstance().sendPlayerToServer(attackPlayer.getName(),"practice");
            }
        }.runTaskLaterAsynchronously(MiarsPractice.getInstance(),100);

        deathPlayer.getInventory().clear();
    }

    @EventHandler
    public void PlayerRespawnEvent(PlayerRespawnEvent e){
        Player player = e.getPlayer();
        ToolUtils.playerInitialize(player);
    }

    /**
     * ????????????????????????
     */
    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent e) {
        Player player = e.getWhoClicked().getKiller();
        ItemStack itemStack = e.getCurrentItem();

        if (itemStack!=null && itemStack.getType()!= Material.AIR){
            NBTItem nbtItem = new NBTItem(itemStack);
            if (nbtItem.getBoolean("stopClick")){
                e.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent e){
        MPlayer mPlayer = MPlayerDataStorage.getInstance().getMPlayer(e.getPlayer());
        MRank mRank = MRankDataStorage.getInstance().getMRank(mPlayer.getRank());
        e.setFormat(ColorParser.parse(mRank.getPrefix()+mRank.getNameColor()+"%1$s&f: %2$s"));
    }

    HashMap<UUID,Long> hashMap = new HashMap<>();
    @EventHandler
    public void PlayerInteractEvent(PlayerInteractEvent e){
        Player player = e.getPlayer();
        ItemStack itemStack = e.getItem();
        Block block = e.getClickedBlock();
        Action action = e.getAction();

        if (itemStack!=null){
            // ???????????????
            ItemInteractManager.getInstance().init(itemStack,player);

            // ????????????????????? Todo ??????manager
            if (action.equals(Action.RIGHT_CLICK_AIR)||action.equals(Action.RIGHT_CLICK_BLOCK)){
                if (itemStack.getType().equals(Material.ENDER_PEARL)){
                    if (player.getGameMode().equals(GameMode.SURVIVAL)){
                        if (hashMap.containsKey(player.getUniqueId())){
                            if (!((System.currentTimeMillis()-hashMap.get(player.getUniqueId()))>=12000)){
                                //player.sendMessage(ColorParser.parse("&7????????????????????? &c???????????? &7??????????????? " + ToolUtils.getDate((15000-(System.currentTimeMillis()-hashMap.get(player.getUniqueId())))/1000) + " &7???????????????"));
                                player.playSound(player.getLocation(), Sound.VILLAGER_NO,1,1);
                                e.setCancelled(true);
                            }
                        }else {
                            hashMap.put(player.getUniqueId(),System.currentTimeMillis());
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (hashMap.containsKey(player.getUniqueId())){
                                        if (!((System.currentTimeMillis()-hashMap.get(player.getUniqueId()))>=12000)){
                                            player.setLevel((int) ((12000-(System.currentTimeMillis()-hashMap.get(player.getUniqueId())))/1000));
                                        }else {
                                            hashMap.remove(player.getUniqueId());
                                            cancel();
                                        }

                                    }
                                }
                            }.runTaskTimerAsynchronously(MiarsPractice.getInstance(),0,2);
                            ToolUtils.reduceXpBar(player,12*20);
                        }
                    }
                }
            }
        }

    }

    @EventHandler
    public void PlayerDropItemEvent(PlayerDropItemEvent event) {
        // ????????????????????????
        event.setCancelled(true);
    }

    @EventHandler
    public void PlayerItemConsumeEvent(PlayerItemConsumeEvent event) {
        // ????????????????????????????????????????????????
        if (event.getItem().getType().equals(Material.POTION)) {
            Bukkit.getScheduler().runTaskLater(MiarsPractice.getInstance(), () -> event.getPlayer().getInventory().remove(Material.GLASS_BOTTLE), 2);
        }
    }

}
