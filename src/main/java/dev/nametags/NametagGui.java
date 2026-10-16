package dev.nametags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class NametagGui implements Listener, CommandExecutor {
    private static final int MONEY = 10, HEARTS = 11, KILLS = 12, DEATHS = 13, MASTER = 16;

    private final NametagsPlugin plugin;

    public NametagGui(NametagsPlugin plugin) {
        this.plugin = plugin;
    }

    private static class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player p) open(p);
        else sender.sendMessage("Players only.");
        return true;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(new Holder(), 27, Component.text("Nametag Settings")
                .decoration(TextDecoration.BOLD, false));
        PlayerData d = plugin.stats().get(p.getUniqueId());
        inv.setItem(MONEY,  item(Material.EMERALD,         "Money",      d.showMoney));
        inv.setItem(HEARTS, item(Material.GOLDEN_APPLE,    "Hearts",     d.showHearts));
        inv.setItem(KILLS,  item(Material.IRON_SWORD,      "Kills",      d.showKills));
        inv.setItem(DEATHS, item(Material.SKELETON_SKULL,  "Deaths",     d.showDeaths));
        inv.setItem(MASTER, item(Material.NAME_TAG,        "Nametag",    d.enabled));
        p.openInventory(inv);
    }

    private ItemStack item(Material m, String name, boolean on) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, false));
        meta.lore(List.of(
                (on ? Component.text("Enabled", NamedTextColor.GREEN) : Component.text("Disabled", NamedTextColor.RED))
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Click to toggle", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        PlayerData d = plugin.stats().get(p.getUniqueId());
        switch (e.getSlot()) {
            case MONEY  -> d.showMoney = !d.showMoney;
            case HEARTS -> d.showHearts = !d.showHearts;
            case KILLS  -> d.showKills = !d.showKills;
            case DEATHS -> d.showDeaths = !d.showDeaths;
            case MASTER -> d.enabled = !d.enabled;
            default -> { return; }
        }
        plugin.manager().refresh(p); // instant update on the head
        open(p);                     // redraw GUI with new state
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
