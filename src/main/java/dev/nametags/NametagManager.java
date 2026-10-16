package dev.nametags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NametagManager {
    private static final TextColor WHITE      = TextColor.color(0xFFFFFF);
    private static final TextColor DARK_GREEN = TextColor.color(0x007A00); // $
    private static final TextColor DARK_RED   = TextColor.color(0x8B0000); // heart
    private static final TextColor BRIGHT_RED = TextColor.color(0xFF1A1A); // sword
    private static final TextColor BRIGHT_ORG = TextColor.color(0xFF8C00); // skull

    private final NametagsPlugin plugin;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private Team hiddenTeam;

    public NametagManager(NametagsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        var board = Bukkit.getScoreboardManager().getMainScoreboard();
        hiddenTeam = board.getTeam("nt_hidden");
        if (hiddenTeam == null) hiddenTeam = board.registerNewTeam("nt_hidden");
        hiddenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 10L);
    }

    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) remove(p);
        displays.values().forEach(d -> { if (d.isValid()) d.remove(); });
        displays.clear();
        if (hiddenTeam != null) hiddenTeam.unregister();
    }

    public void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) refresh(p);
    }

    /** Rebuilds the tag right now. Called by the GUI on every toggle. */
    public void refresh(Player p) {
        PlayerData d = plugin.stats().get(p.getUniqueId());
        if (!d.enabled || p.isDead() || p.getGameMode() == GameMode.SPECTATOR) {
            remove(p);
            return;
        }
        hiddenTeam.addEntry(p.getName());

        TextDisplay display = displays.get(p.getUniqueId());
        if (display == null || !display.isValid()) {
            display = spawn(p);
            displays.put(p.getUniqueId(), display);
        } else if (!p.equals(display.getVehicle())) {
            display.teleport(p.getLocation());
            p.addPassenger(display);
        }
        display.text(build(p, d));
    }

    public void remove(Player p) {
        TextDisplay display = displays.remove(p.getUniqueId());
        if (display != null && display.isValid()) display.remove();
        if (hiddenTeam != null) {
            // Show the vanilla tag again when ours is off
            hiddenTeam.removeEntry(p.getName());
        }
    }

    private TextDisplay spawn(Player p) {
        TextDisplay display = p.getWorld().spawn(p.getLocation(), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setDefaultBackground(false);
            d.setShadowed(true);
            d.setPersistent(false);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.45f, 0f), new AxisAngle4f(),
                    new Vector3f(1f, 1f, 1f), new AxisAngle4f()));
        });
        p.addPassenger(display);
        p.hideEntity(plugin, display); // you don't see your own tag
        return display;
    }

    private Component build(Player p, PlayerData d) {
        List<Component> lines = new ArrayList<>();
        lines.add(plain(SmallCaps.convert(p.getName()), WHITE));

        if (d.showMoney) {
            Economy eco = plugin.economy();
            double bal = eco != null ? eco.getBalance(p) : 0;
            lines.add(row("$", DARK_GREEN, NumberFormatUtil.format(bal)));
        }
        if (d.showHearts) {
            double hearts = Math.round(p.getHealth() / 2.0 * 2) / 2.0; // half-heart steps
            lines.add(row("❤", DARK_RED, NumberFormatUtil.format(hearts)));
        }
        if (d.showKills)  lines.add(row("⚔", BRIGHT_RED, String.valueOf(d.kills)));
        if (d.showDeaths) lines.add(row("☠", BRIGHT_ORG, String.valueOf(d.deaths)));

        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private Component row(String icon, TextColor iconColor, String value) {
        return plain(icon, iconColor).append(plain(" " + value, WHITE));
    }

    private Component plain(String text, TextColor color) {
        return Component.text(text, color)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
    }
}
