package net.gleam.markers.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface MarkersApi {
    List<ItemStack> getInventory(PlayerEntity entity);

    void setInventory(List<ItemStack> inventory, PlayerEntity entity);

    int getInventorySize(PlayerEntity entity);
}
