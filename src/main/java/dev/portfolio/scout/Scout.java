package dev.portfolio.scout;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class Scout extends JavaPlugin {

    NamespacedKey scannerKey;

    NamespacedKey usesKey;
    private NamespacedKey recipeKey;
    int scanRadius;
    int maxUses;
    boolean glow;
    String itemName;
    List<String> itemLore;
    String msgSearching;
    String msgBlocked;
    String msgNotFound;
    String msgFound;

    List<String> regionPrefixes = List.of("ps_x");

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        this.scannerKey = new NamespacedKey(this, "private_scanner");
        this.usesKey = new NamespacedKey(this, "scanner_uses");
        this.recipeKey = new NamespacedKey(this, "private_scanner");
        this.addRecipe();
        this.getServer().getPluginManager().registerEvents(new Blocks(this), this);
        this.getLogger().info("Scout включен");
    }

    @Override
    public void onDisable() {
        if (this.recipeKey != null) {
            Bukkit.removeRecipe(this.recipeKey);
        }
    }

    void loadConfig() {
        this.reloadConfig();

        this.scanRadius = Math.min(500, Math.max(1, this.getConfig().getInt("scan-radius", 100)));
        this.maxUses = Math.max(1, this.getConfig().getInt("max-uses", 25));
        this.glow = this.getConfig().getBoolean("glow", true);
        this.itemName = this.getConfig().getString("item.name", "&5Сканер приватов");
        this.itemLore = this.getConfig().getStringList("item.lore");
        if (this.itemLore == null) {
            this.itemLore = List.of();
        }
        this.msgSearching = this.getConfig().getString("messages.searching", "&8▸ &5&lСКАНИРОВАНИЕ &8┃ &d%time% &7сек. &8┃ &d✦✦✦");
        this.msgBlocked = this.getConfig().getString("messages.blocked", "&8▸ &c&lНЕЛЬЗЯ &8┃ &fСканер нельзя ставить внутри привата");
        this.msgNotFound = this.getConfig().getString("messages.not-found", "&8▸ &7Тишина... &8┃ &fПохоже, здесь никто не живет");
        this.msgFound = this.getConfig().getString("messages.found", "&8▸ &5&lСИГНАЛ НАЙДЕН &8┃ &dЯ нашел чей-то приват!");
        List<String> prefixes = this.getConfig().getStringList("region-prefixes");
        if (prefixes == null || prefixes.isEmpty()) {
            this.regionPrefixes = List.of("ps_x");
        } else {
            this.regionPrefixes = List.copyOf(prefixes);
        }
    }

    private void addRecipe() {
        Bukkit.removeRecipe(this.recipeKey);
        ShapedRecipe r = new ShapedRecipe(this.recipeKey, this.makeScanner(0));
        r.shape("ORO", "RDR", "ORO");
        r.setIngredient('O', Material.OBSERVER);
        r.setIngredient('R', Material.REDSTONE);
        r.setIngredient('D', Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(r);
    }

    ItemStack makeScanner(int used) {
        ItemStack item = new ItemStack(Material.SCULK_SHRIEKER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(this.colorize(this.itemName));
        int left = Math.max(0, this.maxUses - used);
        ArrayList<String> lr = new ArrayList<>();
        for (String line : this.itemLore) {
            if (line == null) continue;
            lr.add(this.colorize(line.replace("%uses%", String.valueOf(left))));
        }
        meta.setLore(lr);
        meta.setMaxStackSize(1);
        if (this.glow) {
            meta.setEnchantmentGlintOverride(true);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (meta instanceof Damageable dmg) {
            dmg.setMaxDamage(this.maxUses);
            dmg.setDamage(Math.min(used, this.maxUses));
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(this.scannerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(this.usesKey, PersistentDataType.INTEGER, used);
        item.setItemMeta(meta);
        return item;
    }

    boolean isScanner(ItemStack item) {
        if (item == null || item.getType() != Material.SCULK_SHRIEKER) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(this.scannerKey, PersistentDataType.BYTE);
    }

    String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("scout") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("scout.admin")) {
                sender.sendMessage("§cНет права scout.admin");
                return true;
            }
            this.loadConfig();
            this.addRecipe();
            sender.sendMessage("§aScout перезагружен");
            return true;
        }
        return false;
    }
}
