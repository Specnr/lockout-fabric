package me.marin.lockout;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.PlayerManager;

import java.util.*;

public class CompassItemHandler {

    public static boolean isCompass(ItemStack item) {
        return item != null &&
                item.getItem() == Items.COMPASS &&
                Optional.ofNullable(item.get(DataComponentTypes.CUSTOM_DATA)).filter(customData -> customData.copyNbt().contains("PlayerTracker")).isPresent();
    }

    public final List<UUID> players = new ArrayList<>();
    public final Map<UUID, String> playerNames = new HashMap<>();
    public final Map<UUID, Integer> currentSelection = new HashMap<>();
    public final Map<UUID, Integer> compassSlots = new HashMap<>();

    public CompassItemHandler(List<UUID> players, PlayerManager playerManager) {
        for (int i = 0; i < players.size(); i++) {
            UUID playerId = players.get(i);
            this.players.add(playerId);
            this.playerNames.put(playerId, playerManager.getPlayer(playerId).getName().getString());

            this.currentSelection.put(playerId, i == 0 ? 1 : 0);
        }
    }

    public void cycle(PlayerEntity player) {
        if (!currentSelection.containsKey(player.getUuid())) return;
        int cur = currentSelection.get(player.getUuid());
        int next = (cur + 1) % players.size();
        if (players.get(next).equals(player.getUuid())) {
            next = (next + 1) % players.size();
        }
        currentSelection.put(player.getUuid(), next);
    }

    public void removePlayer(UUID player) {
        if (!players.contains(player)) return;
        int index = players.indexOf(player);
        players.remove(player);
        playerNames.remove(player);

        // Update selections for other players
        for (UUID uuid : currentSelection.keySet()) {
            int selected = currentSelection.get(uuid);
            if (selected == index) {
                // If they were selecting the removed player, cycle to next
                if (players.isEmpty()) {
                    currentSelection.put(uuid, 0); // Should handle empty list gracefully elsewhere if needed
                } else {
                    int next = selected % players.size(); // Wrap around just in case
                    if (players.get(next).equals(uuid)) {
                         next = (next + 1) % players.size();
                    }
                    currentSelection.put(uuid, next);
                }
            } else if (selected > index) {
                // Shift down if above
                currentSelection.put(uuid, selected - 1);
            }
        }
        currentSelection.remove(player);
    }

    public ItemStack newCompass() {
        ItemStack compass = Items.COMPASS.getDefaultStack();
        NbtCompound compound = new NbtCompound();
        compound.putString("PlayerTracker", UUID.randomUUID().toString());
        compass.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(compound));
        return compass;
    }

}
