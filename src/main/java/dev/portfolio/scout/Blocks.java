package dev.portfolio.scout;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class Blocks implements Listener {
    private final Scout plugin;

    Blocks(Scout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceValidate(BlockPlaceEvent e) {
        ItemStack hand = e.getItemInHand();
        if (!this.plugin.isScanner(hand)) {
            return;
        }
        Player p = e.getPlayer();
        if (!p.hasPermission("scout.use") && !p.hasPermission("scout.admin")) {
            e.setCancelled(true);
            this.actionBar(p, this.plugin.colorize("&cНет права scout.use"));
            return;
        }
        if (this.isInsidePrivate(e.getBlockPlaced().getLocation())) {
            e.setCancelled(true);
            this.actionBar(p, this.plugin.colorize(this.plugin.msgBlocked));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack hand = e.getItemInHand();
        if (!this.plugin.isScanner(hand)) {
            return;
        }
        Block placed = e.getBlockPlaced();
        Location loc = placed.getLocation();
        Player p = e.getPlayer();
        Integer raw = hand.hasItemMeta()
                ? hand.getItemMeta().getPersistentDataContainer().get(this.plugin.usesKey, PersistentDataType.INTEGER)
                : null;
        int used = (raw != null ? raw : 0) + 1;

        if (placed.getState() instanceof TileState tile) {
            PersistentDataContainer pdc = tile.getPersistentDataContainer();
            pdc.set(this.plugin.scannerKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(this.plugin.usesKey, PersistentDataType.INTEGER, used);
            tile.update(true, false);
        }
        final World w = loc.getWorld();
        if (w == null) {
            return;
        }
        w.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 1.0f);
        final Location base = loc.clone().add(0.5, 0.5, 0.5);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (this.tick >= 12) {
                    this.cancel();
                    return;
                }
                double r = 0.5 + (double) this.tick * 0.35;
                float size = Math.max(0.4f, 1.6f - (float) this.tick * 0.1f);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(140, 40, 220), size);
                double y = Math.sin((double) this.tick * 0.5) * 0.2;
                for (int i = 0; i < 10; ++i) {
                    double ang = Math.toRadians(i * 36 + this.tick * 8);
                    w.spawnParticle(Particle.DUST, base.getX() + Math.cos(ang) * r, base.getY() + y, base.getZ() + Math.sin(ang) * r, 1, dust);
                }
                ++this.tick;
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
        this.startScan(p, loc, used);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (block.getType() != Material.SCULK_SHRIEKER) {
            return;
        }
        if (!(block.getState() instanceof TileState tile)) {
            return;
        }
        PersistentDataContainer pdc = tile.getPersistentDataContainer();
        if (!pdc.has(this.plugin.scannerKey, PersistentDataType.BYTE)) {
            return;
        }
        Integer used = pdc.get(this.plugin.usesKey, PersistentDataType.INTEGER);

        e.setDropItems(false);
        e.setExpToDrop(0);
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (used != null && used < this.plugin.maxUses) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), this.plugin.makeScanner(used));
        }
    }

    private void startScan(final Player p, final Location loc, final int usedAtPlace) {
        final int[] timer = new int[]{3};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (loc.getWorld() == null) {
                    this.cancel();
                    return;
                }
                Block block = loc.getBlock();

                if (block.getType() != Material.SCULK_SHRIEKER) {
                    this.cancel();
                    return;
                }
                if (!(block.getState() instanceof TileState tile)) {
                    this.cancel();
                    return;
                }
                PersistentDataContainer pdc = tile.getPersistentDataContainer();
                if (!pdc.has(plugin.scannerKey, PersistentDataType.BYTE)) {
                    this.cancel();
                    return;
                }
                if (timer[0] > 0) {
                    if (p.isOnline()) {
                        actionBar(p, plugin.colorize(plugin.msgSearching.replace("%time%", String.valueOf(timer[0]))));
                    }
                    timer[0]--;
                    return;
                }
                boolean found = scanForPrivates(loc, plugin.scanRadius);
                if (p.isOnline()) {
                    actionBar(p, plugin.colorize(found ? plugin.msgFound : plugin.msgNotFound));
                }
                Integer tileUsed = pdc.get(plugin.usesKey, PersistentDataType.INTEGER);
                int used = tileUsed != null ? tileUsed : usedAtPlace;

                if (used >= plugin.maxUses) {
                    block.setType(Material.AIR, false);
                }
                this.cancel();
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    private boolean isInsidePrivate(Location loc) {
        RegionManager mgr = this.getRegionMgr(loc);
        if (mgr == null) {
            return false;
        }
        for (ProtectedRegion rg : mgr.getApplicableRegions(BukkitAdapter.asBlockVector(loc))) {
            if (this.isPrivateRegion(rg)) return true;
        }
        return false;
    }

    private boolean scanForPrivates(Location center, int radius) {
        RegionManager mgr = this.getRegionMgr(center);
        if (mgr == null) {
            return false;
        }
        int r = Math.max(1, radius);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        ProtectedCuboidRegion area = new ProtectedCuboidRegion(
                "scout_scan",
                BlockVector3.at(cx - r, cy - r, cz - r),
                BlockVector3.at(cx + r, cy + r, cz + r));
        for (ProtectedRegion rg : mgr.getApplicableRegions(area)) {
            if (this.isPrivateRegion(rg)) return true;
        }
        return false;
    }

    private boolean isPrivateRegion(ProtectedRegion rg) {
        String id = rg.getId();
        for (String prefix : this.plugin.regionPrefixes) {
            if (id.startsWith(prefix)) return true;
        }
        return false;
    }

    private RegionManager getRegionMgr(Location loc) {
        if (loc.getWorld() == null) return null;
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
    }

    private void actionBar(Player p, String msg) {
        if (p == null || !p.isOnline()) return;
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }
}
