package cn.mcarl.miars.practiceffa.manager;

import cn.mcarl.miars.core.MiarsCore;
import cn.mcarl.miars.practiceffa.ui.SelectFKitTypeGUI;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * @Author: carl0
 * @DATE: 2023/1/5 21:06
 */
public class ItemInteractManager {
    private static final ItemInteractManager instance = new ItemInteractManager();
    public static ItemInteractManager getInstance() {
        return instance;
    }

    public void init(ItemStack i, Player p){

        NBTItem nbtItem = new NBTItem(i);

        //GUI
        switch (nbtItem.getString("gui")){
            case "SelectFKitTypeGUI" -> {
                SelectFKitTypeGUI.open(p);
            }
        }

        //Server
        if (nbtItem.getString("server")!=null && !Objects.equals(nbtItem.getString("server"), "")){
            MiarsCore.getBungeeApi().connect(p,nbtItem.getString("server"));
        }
    }
}