package me.petterim1.seeinv;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.plugin.PluginBase;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;

public class Main extends PluginBase {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("seeinv")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou can run this command only as a player");
                return true;
            }

            if (args.length == 0) {
                return false;
            }

            Player target = getServer().getPlayer(args[0]);
            CompoundTag offlineData = null;

            if (target == null) {
                offlineData = getServer().getOfflinePlayerData(args[0], false);
                if (offlineData == null) {
                    sender.sendMessage("§cUnknown player: " + args[0]);
                    return true;
                }
            }

            if (sender.equals(target) && !sender.hasPermission("seeinv.self")) {
                sender.sendMessage("§cYou don't have permission to use this command for own inventory");
                return true;
            }

            ChestFakeInventory inv;

            if (args.length == 2 && args[1].equalsIgnoreCase("echest")) {
                inv = new ChestFakeInventory();
                if (offlineData != null) {
                    setOfflineInventory(inv, offlineData, true);
                    inv.setName(offlineData.getString("NameTag") + "'s ender chest");
                } else {
                    inv.setName(target.getName() + "'s ender chest");
                    inv.setContents(target.getEnderChestInventory().getContents());
                }
            } else {
                inv = new DoubleChestFakeInventory();
                if (offlineData != null) {
                    setOfflineInventory(inv, offlineData, false);
                    inv.setName(offlineData.getString("NameTag") + "'s inventory");
                } else {
                    inv.setName(target.getName() + "'s inventory");
                    inv.setContents(target.getInventory().getContents());
                }
            }

            inv.addListener(this::onSlotChange);
            ((Player) sender).addWindow(inv);
        } else if (cmd.getName().equals("echest")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou can run this command only as a player");
                return true;
            }

            Player target;
            CompoundTag offlineData = null;
            ChestFakeInventory inv = new ChestFakeInventory();

            if (args.length == 0) {
                if (sender.hasPermission("seeinv.self")) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage("§cYou don't have permission to use this command for own ender chest inventory");
                    return true;
                }
            } else {
                target = getServer().getPlayer(args[0]);

                if (target == null) {
                    offlineData = getServer().getOfflinePlayerData(args[0], false);
                    if (offlineData == null) {
                        sender.sendMessage("§cUnknown player: " + args[0]);
                        return true;
                    }
                }
            }

            if (offlineData != null) {
                setOfflineInventory(inv, offlineData, true);
                inv.setName(offlineData.getString("NameTag") + "'s ender chest");
            } else {
                inv.setContents(target.getEnderChestInventory().getContents());
                inv.setName(target.getName() + "'s ender chest");
            }

            inv.addListener(this::onSlotChange);
            ((Player) sender).addWindow(inv);
        }

        return true;
    }

    private void onSlotChange(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory) {
            boolean inv = e.getInventory() instanceof DoubleChestFakeInventory && e.getInventory().getName().contains("'s inventory");
            boolean ec = e.getInventory() instanceof ChestFakeInventory && e.getInventory().getName().contains("'s ender chest");
            if (inv || ec) {
                if (e.getPlayer().hasPermission("seeinv.takeitems")) {
                    Player target = getServer().getPlayerExact(e.getInventory().getName().replace("'s ender chest", "").replace("'s inventory", ""));
                    if (target == null) {
                        e.getPlayer().sendMessage("§cOperation failed: target player is not online");
                        e.setCancelled(true);
                    } else if (inv && target.equals(e.getPlayer())) {
                        e.getPlayer().sendMessage("§cOperation failed: can't edit own player inventory trough fake inventory");
                        e.setCancelled(true);
                    } else {
                        getServer().getScheduler().scheduleTask(this, () -> {
                            if (inv) {
                                target.getInventory().setContents(e.getInventory().getContents());
                            } else {
                                target.getEnderChestInventory().setContents(e.getInventory().getContents());
                            }
                        });
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }
    
    private static void setOfflineInventory(ChestFakeInventory inv, CompoundTag offlineData, boolean ec) {
        if (ec) {
            if (offlineData.contains("EnderItems") && offlineData.get("EnderItems") instanceof ListTag) {
                ListTag<CompoundTag> inventoryList = offlineData.getList("EnderItems", CompoundTag.class);
                for (CompoundTag item : inventoryList.getAll()) {
                    inv.setItem(item.getByte("Slot"), NBTIO.getItemHelper(item));
                }
            }
        } else {
            if (offlineData.contains("Inventory") && offlineData.get("Inventory") instanceof ListTag) {
                ListTag<CompoundTag> inventoryList = offlineData.getList("Inventory", CompoundTag.class);
                for (CompoundTag item : inventoryList.getAll()) {
                    int slot = item.getByte("Slot");
                    if (slot >= 0 && slot < 9) {
                        inventoryList.remove(item);
                    } else if (slot >= 100 && slot < 104) {
                        inv.setItem(inv.getSize() + slot - 100, NBTIO.getItemHelper(item));
                    } else if (slot == -106) {
                        inv.setItem(0, NBTIO.getItemHelper(item));
                    } else {
                        inv.setItem(slot - 9, NBTIO.getItemHelper(item));
                    }
                }
            }
        }
    }
}
