package me.petterim1.seeinv;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;

public class Main extends PluginBase {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("seeinv")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou can run this command only as a player");
                return true;
            }

            if (args.length == 0) {
                return false;
            }

            Player target = getServer().getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cUnknown player");
                return true;
            }

            if (target.equals(sender) && !sender.hasPermission("seeinv.self")) {
                sender.sendMessage("§cNo permission to use this command for own inventory");
                return true;
            }

            ChestFakeInventory inv;

            if (args.length == 2 && args[1].equalsIgnoreCase("echest")) {
                inv = new ChestFakeInventory();
                inv.setContents(target.getEnderChestInventory().getContents());
                inv.setName(target.getName() + "'s ender chest inventory");
            } else {
                inv = new DoubleChestFakeInventory();
                inv.setContents(target.getInventory().getContents());
                inv.setName(target.getName() + "'s inventory");
            }

            inv.addListener(this::onSlotChange);
            ((Player) sender).addWindow(inv);
        } else if (cmd.getName().equalsIgnoreCase("echest")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou can run this command only as a player");
                return true;
            }

            Player target;
            ChestFakeInventory inv = new ChestFakeInventory();

            if (args.length == 0) {
                if (sender.hasPermission("seeinv.self")) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage("§cNo permission to use this command for own enderchest inventory");
                    return true;
                }
            } else {
                target = getServer().getPlayer(args[0]);

                if (target == null) {
                    sender.sendMessage("§cUnknown player");
                    return true;
                }
            }

            inv.setContents(target.getEnderChestInventory().getContents());
            inv.setName(target.getName() + "'s ender chest inventory");
            inv.addListener(this::onSlotChange);
            ((Player) sender).addWindow(inv);
        }

        return true;
    }

    private void onSlotChange(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory && e.getPlayer().hasPermission("seeinv.takeitems")) {
            if (e.getInventory().getName().contains("'s ender chest inventory") || e.getInventory().getName().contains("'s inventory")) {
                Player target = getServer().getPlayer(e.getInventory().getName().replace("'s ender chest inventory", "").replace("'s inventory", ""));

                if (target == null) {
                    e.setCancelled(true);
                } else { // TODO: Check where the item is moved to
                    target.getInventory().remove(e.getAction().getSourceItem());
                }

                return;
            }
        }

        if (e.getInventory() instanceof DoubleChestFakeInventory) {
            if (e.getInventory().getName().contains("'s ender chest inventory") || e.getInventory().getName().contains("'s inventory")) {
                e.setCancelled(true);
            }
        }
    }
}
