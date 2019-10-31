package world.bentobox.warps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

public class WarpPanelManager {

    private static final int PANEL_MAX_SIZE = 52;
    private Warp addon;
    // This is a cache of signs
    private Map<World, Map<UUID, SignCache>> cachedSigns = new HashMap<>();



    public WarpPanelManager(Warp addon) {
        this.addon = addon;
    }

    private PanelItem getPanelItem(World world, UUID warpOwner) {
        PanelItemBuilder pib = new PanelItemBuilder()
                .name(addon.getSettings().getNameFormat() + addon.getPlugin().getPlayers().getName(warpOwner))
                .description(getSign(world, warpOwner))
                .clickHandler((panel, clicker, click, slot) -> hander(world, clicker, warpOwner));
        Material icon = getSignIcon(world, warpOwner);
        if (icon.equals(Material.PLAYER_HEAD)) {
            return pib.icon(addon.getPlayers().getName(warpOwner)).build();
        } else {
            return pib.icon(icon).build();
        }
    }

    private boolean hander(World world, User clicker, UUID warpOwner) {
        clicker.closeInventory();
        addon.getWarpSignsManager().warpPlayer(world, clicker, warpOwner);
        return true;
    }

    private PanelItem getRandomButton(World world, User user, UUID warpOwner) {
        ///give @p minecraft:player_head{display:{Name:"{\"text\":\"Question Mark\"}"},SkullOwner:"MHF_Question"} 1
        return new PanelItemBuilder()
                .name(addon.getSettings().getNameFormat() + user.getTranslation("warps.random"))
                .clickHandler((panel, clicker, click, slot) -> hander(world, clicker, warpOwner))
                .icon(Material.END_CRYSTAL).build();
    }

    private Material getSignIcon(World world, UUID warpOwner) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        if (cachedSigns.get(world).containsKey(warpOwner)) {
            return cachedSigns.get(world).get(warpOwner).getType();
        }
        // Not in cache
        SignCache sc = addon.getWarpSignsManager().getSignInfo(world, warpOwner);
        cachedSigns.get(world).put(warpOwner, sc);
        return sc.getType();
    }


    /**
     * Gets sign text and cache it
     * @param playerUUID
     * @return sign text in a list
     */
    private List<String> getSign(World world, UUID playerUUID) {
        // Add the worlds if we haven't seen this before
        cachedSigns.putIfAbsent(world, new HashMap<>());
        if (cachedSigns.get(world).containsKey(playerUUID)) {
            return cachedSigns.get(world).get(playerUUID).getSignText();
        }
        SignCache result = addon.getWarpSignsManager().getSignInfo(world, playerUUID);
        cachedSigns.get(world).put(playerUUID, result);
        return result.getSignText();
    }

    /**
     * Show the warp panel for the user
     * @param world - world
     * @param user - user
     * @param index - page to show - 0 is first
     */
    public void showWarpPanel(World world, User user, int index) {
        List<UUID> warps = new ArrayList<>(addon.getWarpSignsManager().getSortedWarps(world));
        UUID randomWarp = null;
        // Add random UUID
        if (!warps.isEmpty() && addon.getSettings().isRandomAllowed()) {
            randomWarp = warps.get(new Random().nextInt(warps.size()));
            warps.add(0, randomWarp);
        }
        if (index < 0) {
            index = 0;
        } else if (index > (warps.size() / PANEL_MAX_SIZE)) {
            index = warps.size() / PANEL_MAX_SIZE;
        }
        PanelBuilder panelBuilder = new PanelBuilder()
                .user(user)
                .name(user.getTranslation("warps.title") + " " + (index + 1));

        int i = index * PANEL_MAX_SIZE;
        for (; i < (index * PANEL_MAX_SIZE + PANEL_MAX_SIZE) && i < warps.size(); i++) {
            if (i == 0 && randomWarp != null) {
                panelBuilder.item(getRandomButton(world, user, randomWarp));
            } else {
                panelBuilder.item(getPanelItem(world, warps.get(i)));
            }
        }
        final int panelNum = index;
        // Add signs
        if (i < warps.size()) {
            // Next
            panelBuilder.item(new PanelItemBuilder()
                    .name(user.getTranslation("warps.next"))
                    .icon(new ItemStack(Material.STONE))
                    .clickHandler((panel, clicker, click, slot) -> {
                        user.closeInventory();
                        showWarpPanel(world, user, panelNum+1);
                        return true;
                    }).build());
        }
        if (i > PANEL_MAX_SIZE) {
            // Previous
            panelBuilder.item(new PanelItemBuilder()
                    .name(user.getTranslation("warps.previous"))
                    .icon(new ItemStack(Material.COBBLESTONE))
                    .clickHandler((panel, clicker, click, slot) -> {
                        user.closeInventory();
                        showWarpPanel(world, user, panelNum-1);
                        return true;
                    }).build());
        }
        panelBuilder.build();
    }

    /**
     * Removes sign text from the cache
     * @param key
     */
    public void removeWarp(World world, UUID key) {
        cachedSigns.putIfAbsent(world, new HashMap<>());
        cachedSigns.get(world).remove(key);
    }

}
