package me.petterim1.seeinv;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.Listener;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.inventory.transaction.action.SlotChangeAction;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.service.RegisteredServiceProvider;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeInventories;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;

public class Main extends PluginBase {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("seeinv")) {
            if (sender instanceof Player) {
                if (args.length == 0) {
                    return false;
                }

                Player target = getServer().getPlayer(args[0]);

                if (target == null) {
                    sender.sendMessage("\u00A7cUnknown player");
                    return true;
                }

                if (target.equals(sender)) {
                    sender.sendMessage("\u00A7cCannot use this command for own inventory");
                    return true;
                }

                DoubleChestFakeInventory inv = new DoubleChestFakeInventory();
                inv.addListener(this::onSlotChange);

                if (args.length == 2 && args[1].equalsIgnoreCase("echest")) {
                    inv.setContents(target.getEnderChestInventory().getContents());
                    inv.setName(target.getName() + "'s ender chest inventory");
                } else {
                    inv.setContents(target.getInventory().getContents());
                    inv.setName(target.getName() + "'s inventory");
                }

                ((Player) sender).addWindow(inv);
                return true;
            }

            sender.sendMessage("\u00A7cYou can run this command only as a player");
        }

        return true;
    }

    private void onSlotChange(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof DoubleChestFakeInventory) {
            if (e.getInventory().getName().contains("'s ender chest inventory") || e.getInventory().getName().contains("'s inventory")) {
                e.setCancelled(true);
            }
        }
    }
}
