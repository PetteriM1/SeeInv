package me.petterim1.seeinv;

import cn.nukkit.Player;
import cn.nukkit.block.BlockEnderChest;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.plugin.PluginBase;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;

import java.util.ArrayList;
import java.util.List;

public class Main extends PluginBase {

    private final List<String> ecOpen = new ArrayList<>();
    private final List<String> invOpen = new ArrayList<>();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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

        ChestFakeInventory inv;

        if (cmd.getName().equals("seeinv")) {
            if (!sender.hasPermission("seeinv.command.use")) {
                throw new IllegalStateException("No permission");
            }

            if (args.length == 2 && args[1].equalsIgnoreCase("echest")) {
                inv = new ChestFakeInventory();
                if (offlineData != null) {
                    loadOfflineInventory(inv, offlineData, true);
                    inv.setName(offlineData.getString("NameTag") + "'s ender chest");
                } else {
                    inv.setName(target.getName() + "'s ender chest");
                    inv.setContents(target.getEnderChestInventory().getContents());
                }
            } else {
                inv = new DoubleChestFakeInventory();
                if (offlineData != null) {
                    loadOfflineInventory(inv, offlineData, false);
                    inv.setName(offlineData.getString("NameTag") + "'s inventory");
                } else {
                    inv.setName(target.getName() + "'s inventory");
                    inv.setContents(target.getInventory().getContents());
                }
            }

            inv.addListener(this::onSlotView);
            ((Player) sender).addWindow(inv);
        } else if (cmd.getName().equals("editinv")) {
            if (!sender.hasPermission("editinv.command.use")) {
                throw new IllegalStateException("No permission");
            }

            if (args.length == 2 && args[1].equalsIgnoreCase("echest")) {
                inv = new ChestFakeInventory();
                if (offlineData != null) {
                    loadOfflineInventory(inv, offlineData, true);
                    inv.setName(offlineData.getString("NameTag") + "'s ender chest");
                } else {
                    if (target.getWindowId(target.getEnderChestInventory()) != -1) {
                        target.removeWindow(target.getEnderChestInventory());
                        sender.sendMessage(target.getName() + "'s ender chest inventory was closed forcefully");
                    }

                    inv.setName(target.getName() + "'s ender chest");
                    inv.setContents(target.getEnderChestInventory().getContents());

                    ecOpen.add(target.getName());
                }
            } else {
                if (sender.equals(target)) {
                    sender.sendMessage("§cCan't edit own player inventory");
                    return true;
                }

                inv = new DoubleChestFakeInventory();
                if (offlineData != null) {
                    loadOfflineInventory(inv, offlineData, false);
                    inv.setName(offlineData.getString("NameTag") + "'s inventory");
                } else {
                    if (target.getWindowId(target.getInventory()) != -1) {
                        target.removeWindow(target.getInventory());
                        sender.sendMessage(target.getName() + "'s inventory was closed forcefully");
                    }

                    inv.setName(target.getName() + "'s inventory");
                    inv.setContents(target.getInventory().getContents());

                    invOpen.add(target.getName());
                }
            }

            inv.addListener(this::onSlotEdit);
            ((Player) sender).addWindow(inv);
        }

        return true;
    }

    private void onSlotView(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory) {
            boolean inv = e.getInventory() instanceof DoubleChestFakeInventory && e.getInventory().getName().contains("'s inventory");
            boolean ec = e.getInventory().getName().contains("'s ender chest");
            if (inv || ec) {
                e.setCancelled(true);
            }
        }
    }

    private void onSlotEdit(FakeSlotChangeEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory) {
            boolean inv = e.getInventory() instanceof DoubleChestFakeInventory && e.getInventory().getName().contains("'s inventory");
            boolean ec = e.getInventory().getName().contains("'s ender chest");
            if (inv || ec) {
                Player target = getServer().getPlayerExact(e.getInventory().getName().replace("'s ender chest", "").replace("'s inventory", ""));
                if (target == null) {
                    e.getPlayer().sendMessage("§cOperation failed: target player is not online");
                    e.setCancelled(true);
                } else if (inv && target.equals(e.getPlayer())) {
                    e.getPlayer().sendMessage("§cOperation failed: can't edit own player inventory");
                    e.setCancelled(true);
                } else {
                    getServer().getScheduler().scheduleTask(this, () -> {
                        if (inv) {
                            target.getInventory().setContents(e.getInventory().getContents());
                        } else {
                            target.getEnderChestInventory().setContents(e.getInventory().getContents());
                        }
                        getLogger().info(e.getPlayer().getName() + " modified " + e.getInventory().getName());
                    });
                }
            }
        }
    }
    
    private static void loadOfflineInventory(ChestFakeInventory inv, CompoundTag offlineData, boolean echest) {
        if (echest) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInteractEnderChest(PlayerInteractEvent e) {
        if (e.getBlock() instanceof BlockEnderChest && ecOpen.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onOpenPlayerInventory(InventoryOpenEvent e) {
        if (invOpen.contains(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFakeInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory() instanceof ChestFakeInventory) {
            if (e.getInventory().getName().contains("'s inventory")) {
                Player target = getServer().getPlayerExact(e.getInventory().getName().replace("'s inventory", ""));

                if (target != null) {
                    invOpen.remove(target.getName());
                }
            } else if (e.getInventory().getName().contains("'s ender chest")) {
                Player target = getServer().getPlayerExact(e.getInventory().getName().replace("'s ender chest", ""));

                if (target != null) {
                    ecOpen.remove(target.getName());
                }
            }
        }
    }
}
