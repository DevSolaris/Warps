package bskyblock.addon.warps;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import bskyblock.addon.level.Level;
import bskyblock.addon.warps.event.WarpRemoveEvent;
import world.bentobox.bbox.BentoBox;
import world.bentobox.bbox.api.user.User;

/**
 * Handles warping. Players can add one sign
 *
 * @author tastybento
 *
 */
public class WarpSignsListener implements Listener {
    private BentoBox plugin;

    private Warp addon;

    /**
     * @param addon - addon
     */
    public WarpSignsListener(Warp addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();
    }

    /**
     * Checks to see if a sign has been broken
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        // Signs only
        if (!b.getType().equals(Material.SIGN) && !b.getType().equals(Material.WALL_SIGN)) {
            return;
        }
        if (!addon.inRegisteredWorld(b.getWorld())) {
            return;
        }
        User user = User.getInstance(e.getPlayer());
        Sign s = (Sign) b.getState();
        if (s == null) {
            return;
        }
        if (s.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getConfig().getString("welcomeLine"))) {
            // Do a quick check to see if this sign location is in
            // the list of warp signs
            Map<UUID, Location> list = addon.getWarpSignsManager().getWarpList(b.getWorld());
            if (list.containsValue(s.getLocation())) {
                // Welcome sign detected - check to see if it is
                System.out.println("DEBIG:");
                // this player's sign
                if ((list.containsKey(user.getUniqueId()) && list.get(user.getUniqueId()).equals(s.getLocation())) 
                        || user.isOp()  || user.hasPermission(addon.getPermPrefix(e.getBlock().getWorld()) + "mod.removesign")) {
                    // Op or mod removed sign
                    user.sendMessage("warps.removed");
                    addon.getWarpSignsManager().removeWarp(s.getLocation());
                    Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(addon, s.getLocation(), user.getUniqueId()));
                } else {
                    // Someone else's sign - not allowed
                    user.sendMessage("warps.error.no-remove");
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Event handler for Sign Changes
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignWarpCreate(SignChangeEvent e) {
        Block b = e.getBlock();
        if (!addon.inRegisteredWorld(b.getWorld())) {
            return;
        }
        String title = e.getLine(0);
        User user = User.getInstance(e.getPlayer());
        // Check if someone is changing their own sign
        if (title.equalsIgnoreCase(addon.getConfig().getString("welcomeLine"))) {
            // Welcome sign detected - check permissions
            if (!(user.hasPermission(addon.getPermPrefix(b.getWorld()) + "island.addwarp"))) {
                user.sendMessage("warps.error.no-permission");
                user.sendMessage("general.errors.you-need", "[permission]", addon.getPermPrefix(b.getWorld()) + "island.addwarp");
                return;
            }
            if (addon.getLevelAddon().isPresent()) {
                Level lev = (Level) addon.getLevelAddon().get();
                if (lev.getIslandLevel(b.getWorld(), user.getUniqueId()) < addon.getSettings().getWarpLevelRestriction()) {
                    user.sendMessage("warps.error.not-enough-level");
                    user.sendMessage("warps.error.your-level-is",
                            "[level]", String.valueOf(lev.getIslandLevel(b.getWorld(), user.getUniqueId())),
                            "[required]", String.valueOf(addon.getSettings().getWarpLevelRestriction()));
                    return;
                }
            }


            // Check that the player is on their island
            if (!(plugin.getIslands().userIsOnIsland(b.getWorld(), user))) {
                user.sendMessage("warps.error.not-on-island");
                e.setLine(0, ChatColor.RED + addon.getConfig().getString("welcomeLine"));
                return;
            }
            // Check if the player already has a sign
            final Location oldSignLoc = addon.getWarpSignsManager().getWarp(b.getWorld(), user.getUniqueId());
            if (oldSignLoc == null) {
                // First time the sign has been placed or this is a new
                // sign
                addSign(e, user, b);
            } else {
                // A sign already exists. Check if it still there and if
                // so,
                // deactivate it
                Block oldSignBlock = oldSignLoc.getBlock();
                if (oldSignBlock.getType().equals(Material.SIGN) || oldSignBlock.getType().equals(Material.WALL_SIGN)) {
                    // The block is still a sign
                    Sign oldSign = (Sign) oldSignBlock.getState();
                    if (oldSign != null) {
                        if (oldSign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + addon.getConfig().getString("welcomeLine"))) {
                            oldSign.setLine(0, ChatColor.RED + addon.getConfig().getString("welcomeLine"));
                            oldSign.update(true, false);
                            user.sendMessage("warps.deactivate");
                            addon.getWarpSignsManager().removeWarp(oldSignBlock.getWorld(), user.getUniqueId());
                            Bukkit.getPluginManager().callEvent(new WarpRemoveEvent(addon, oldSign.getLocation(), user.getUniqueId()));
                        }
                    }
                }
                // Set up the new warp sign
                addSign(e, user, b);
            }
        }

    }

    private void addSign(SignChangeEvent e, User user, Block b) {
        if (addon.getWarpSignsManager().addWarp(user.getUniqueId(), b.getLocation())) {
            user.sendMessage("warps.success");
            e.setLine(0, ChatColor.GREEN + addon.getConfig().getString("welcomeLine"));
            for (int i = 1; i<4; i++) {
                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
            }
        } else {
            user.sendMessage("warps.error.duplicate");
            e.setLine(0, ChatColor.RED + addon.getConfig().getString("welcomeLine"));
            for (int i = 1; i<4; i++) {
                e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
            }
        }        
    }

}
