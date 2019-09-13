package com.songoda.epicfarming.farming;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.compatibility.CompatibleSound;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.epicfarming.EpicFarming;
import com.songoda.epicfarming.boost.BoostData;
import com.songoda.epicfarming.player.PlayerData;
import com.songoda.epicfarming.settings.Settings;
import com.songoda.epicfarming.utils.Methods;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Farm {

    private static final Random random = new Random();
    private final List<Block> cachedCrops = new ArrayList<>();
    private Location location;
    private Level level;
    private Inventory inventory;
    private UUID placedBy;
    private UUID viewing = null;
    private long lastCached = 0;

    public Farm(Location location, Level level, UUID placedBy) {
        this.location = location;
        this.level = level;
        this.placedBy = placedBy;
        this.inventory = Bukkit.createInventory(null, 54, Methods.formatName(level.getLevel(), false));
    }

    public void view(Player player) {
        if (!player.hasPermission("epicfarming.view"))
            return;

        if (viewing != null) return;

        setupOverview(player);

        player.openInventory(inventory);
        this.viewing = player.getUniqueId();

        PlayerData playerData = EpicFarming.getInstance().getPlayerActionManager().getPlayerAction(player);

        playerData.setFarm(this);

        getInventory();
    }

    private void setupOverview(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Methods.formatName(level.getLevel(), false));
        inventory.setContents(this.inventory.getContents());
        this.inventory = inventory;

        EpicFarming instance = EpicFarming.getInstance();

        Level nextLevel = instance.getLevelManager().getHighestLevel().getLevel() > level.getLevel() ? instance.getLevelManager().getLevel(level.getLevel() + 1) : null;

        int level = this.level.getLevel();

        ItemStack item = new ItemStack(Material.valueOf(instance.getConfig().getString("Main.Farm Block Material")), 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(instance.getLocale().getMessage("general.nametag.farm")
                .processPlaceholder("level", level).getMessage());
        List<String> lore = this.level.getDescription();
        lore.add("");
        if (nextLevel == null) lore.add(instance.getLocale().getMessage("event.upgrade.maxed").getMessage());
        else {
            lore.add(instance.getLocale().getMessage("interface.button.level")
                    .processPlaceholder("level", nextLevel.getLevel()).getMessage());
            lore.addAll(nextLevel.getDescription());
        }

        BoostData boostData = instance.getBoostManager().getBoost(placedBy);
        if (boostData != null) {
            String[] parts = instance.getLocale().getMessage("interface.button.boostedstats")
                    .processPlaceholder("amount", Integer.toString(boostData.getMultiplier()))
                    .processPlaceholder("time", Methods.makeReadable(boostData.getEndTime() - System.currentTimeMillis()))
                    .getMessage().split("\\|");
            lore.add("");
            for (String line : parts)
                lore.add(Methods.formatText(line));
        }

        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);

        ItemStack itemXP = Settings.XP_ICON.getMaterial().getItem();
        ItemMeta itemmetaXP = itemXP.getItemMeta();
        itemmetaXP.setDisplayName(instance.getLocale().getMessage("interface.button.upgradewithxp").getMessage());
        ArrayList<String> loreXP = new ArrayList<>();
        if (nextLevel != null)
            loreXP.add(instance.getLocale().getMessage("interface.button.upgradewithxplore")
                    .processPlaceholder("cost", nextLevel.getCostExperiance()).getMessage());
        else
            loreXP.add(instance.getLocale().getMessage("event.upgrade.maxed").getMessage());
        itemmetaXP.setLore(loreXP);
        itemXP.setItemMeta(itemmetaXP);

        ItemStack itemECO = Settings.ECO_ICON.getMaterial().getItem();
        ItemMeta itemmetaECO = itemECO.getItemMeta();
        itemmetaECO.setDisplayName(instance.getLocale().getMessage("interface.button.upgradewitheconomy").getMessage());
        ArrayList<String> loreECO = new ArrayList<>();
        if (nextLevel != null)
            loreECO.add(instance.getLocale().getMessage("interface.button.upgradewitheconomylore")
                    .processPlaceholder("cost", Methods.formatEconomy(nextLevel.getCostEconomy()))
                    .getMessage());
        else
            loreECO.add(instance.getLocale().getMessage("event.upgrade.maxed").getMessage());
        itemmetaECO.setLore(loreECO);
        itemECO.setItemMeta(itemmetaECO);

        if (instance.getConfig().getBoolean("Main.Upgrade With XP") && player != null && player.hasPermission("EpicFarming.Upgrade.XP")) {
            inventory.setItem(11, itemXP);
        }

        inventory.setItem(13, item);

        if (instance.getConfig().getBoolean("Main.Upgrade With Economy") && player != null && player.hasPermission("EpicFarming.Upgrade.ECO")) {
            inventory.setItem(15, itemECO);
        }
/*
        inventory.setItem(0, Methods.getBackgroundGlass(true));
        inventory.setItem(1, Methods.getBackgroundGlass(true));
        inventory.setItem(2, Methods.getBackgroundGlass(false));
        inventory.setItem(6, Methods.getBackgroundGlass(false));
        inventory.setItem(7, Methods.getBackgroundGlass(true));
        inventory.setItem(8, Methods.getBackgroundGlass(true));
        inventory.setItem(9, Methods.getBackgroundGlass(true));
        inventory.setItem(10, Methods.getBackgroundGlass(false));
        inventory.setItem(16, Methods.getBackgroundGlass(false));
        inventory.setItem(17, Methods.getBackgroundGlass(true));
        inventory.setItem(18, Methods.getBackgroundGlass(true));
        inventory.setItem(19, Methods.getBackgroundGlass(true));
        inventory.setItem(20, Methods.getBackgroundGlass(false));
        inventory.setItem(24, Methods.getBackgroundGlass(false));
        inventory.setItem(25, Methods.getBackgroundGlass(true));
        inventory.setItem(26, Methods.getBackgroundGlass(true)); */
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void loadInventory(List<ItemStack> items) {
        setupOverview(null);
        int i = 27;
        for (ItemStack item : items) {
            inventory.setItem(i++, item);
        }
    }

    public List<ItemStack> dumpInventory() {
        List<ItemStack> items = new ArrayList<>();

        for (int i = 27; i < inventory.getSize(); i++) {
            items.add(inventory.getItem(i));
        }

        return items;
    }

    public void upgrade(UpgradeType type, Player player) {
        EpicFarming instance = EpicFarming.getInstance();
        if (instance.getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) {
            Level level = instance.getLevelManager().getLevel(this.level.getLevel() + 1);
            int cost;
            if (type == UpgradeType.EXPERIENCE) {
                cost = level.getCostExperiance();
            } else {
                cost = level.getCostEconomy();
            }

            if (type == UpgradeType.ECONOMY) {
                if (EconomyManager.isEnabled()) {
                    if (EconomyManager.hasBalance(player, cost)) {
                        EconomyManager.withdrawBalance(player, cost);
                        upgradeFinal(level, player);
                    } else {
                        instance.getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
                    }
                } else {
                    player.sendMessage("Vault is not installed.");
                }
            } else if (type == UpgradeType.EXPERIENCE) {
                if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        player.setLevel(player.getLevel() - cost);
                    }
                    upgradeFinal(level, player);
                } else {
                    instance.getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
                }
            }
        }
    }

    private void upgradeFinal(Level level, Player player) {
        EpicFarming instance = EpicFarming.getInstance();
        this.level = level;
        if (instance.getLevelManager().getHighestLevel() != level) {
            instance.getLocale().getMessage("event.upgrade.success")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        } else {
            instance.getLocale().getMessage("event.upgrade.successmaxed")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        }
        Location loc = location.clone().add(.5, .5, .5);
        tillLand(location);
        if (!ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) return;

        player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), loc, 200, .5, .5, .5);

        if (instance.getLevelManager().getHighestLevel() != level) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 15.0F);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2F, 25.0F);

            if (!ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) return;

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 2F, 25.0F);
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2F, 35.0F), 5L);
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.8F, 35.0F), 10L);
        }
    }

    public boolean tillLand(Location location) {
        if (Settings.DISABLE_AUTO_TIL_LAND.getBoolean()) return true;
        Block block = location.getBlock();
        int radius = level.getRadius();
        int bx = block.getX();
        int by = block.getY();
        int bz = block.getZ();
        for (int fx = -radius; fx <= radius; fx++) {
            for (int fy = -2; fy <= 1; fy++) {
                for (int fz = -radius; fz <= radius; fz++) {
                    Block b2 = block.getWorld().getBlockAt(bx + fx, by + fy, bz + fz);

                    // ToDo: enum for all flowers.
                    if (b2.getType() == CompatibleMaterial.TALL_GRASS.getMaterial()
                            || b2.getType() == CompatibleMaterial.GRASS.getMaterial()
                            || b2.getType().name().contains("TULIP")
                            || b2.getType().name().contains("ORCHID")
                            || b2.getType() == CompatibleMaterial.AZURE_BLUET.getMaterial()
                            || b2.getType() == CompatibleMaterial.ALLIUM.getMaterial()
                            || b2.getType() == CompatibleMaterial.POPPY.getMaterial()
                            || b2.getType() == CompatibleMaterial.DANDELION.getMaterial()) {
                        Bukkit.getScheduler().runTaskLater(EpicFarming.getInstance(), () -> {
                            b2.getRelative(BlockFace.DOWN).setType(CompatibleMaterial.FARMLAND.getMaterial());
                            b2.breakNaturally();
                            b2.getWorld().playSound(b2.getLocation(), CompatibleSound.BLOCK_GRASS_BREAK.getSound(), 10, 15);
                        }, random.nextInt(30) + 1);
                    }
                    if ((b2.getType() == CompatibleMaterial.GRASS_BLOCK.getMaterial()
                            || b2.getType() == Material.DIRT) && b2.getRelative(BlockFace.UP).getType() == Material.AIR) {
                        Bukkit.getScheduler().runTaskLater(EpicFarming.getInstance(), () -> {
                            b2.setType(CompatibleMaterial.FARMLAND.getMaterial());
                            b2.getWorld().playSound(b2.getLocation(), CompatibleSound.BLOCK_GRASS_BREAK.getSound(), 10, 15);
                        }, random.nextInt(30) + 1);
                    }

                }
            }
        }
        return false;
    }

    public UUID getViewing() {
        return viewing;
    }

    public void setViewing(UUID viewing) {
        this.viewing = viewing;
    }

    public void addCachedCrop(Block block) {
        cachedCrops.add(block);
    }

    public void removeCachedCrop(Block block) {
        cachedCrops.remove(block);
    }

    public List<Block> getCachedCrops() {
        return new ArrayList<>(cachedCrops);
    }

    public void clearCache() {
        cachedCrops.clear();
    }

    public long getLastCached() {
        return lastCached;
    }

    public void setLastCached(long lastCached) {
        this.lastCached = lastCached;
    }

    public boolean isInLoadedChunk() {
        return location != null && location.getWorld() != null && location.getWorld().isChunkLoaded(((int) location.getX()) >> 4, ((int) location.getZ()) >> 4);
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public UUID getPlacedBy() {
        return placedBy;
    }

    public Level getLevel() {
        return level;
    }
}