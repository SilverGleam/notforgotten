package net.gleam.markers.block.entity;

import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.gleam.markers.Markers;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class MarkerBlockEntity extends BlockEntity implements BlockEntityClientSerializable {
    private DefaultedList<ItemStack> items;
    private int xp;
    private GameProfile markerOwner;
    private String customName;
    private BlockState state;

    public MarkerBlockEntity(BlockPos pos, BlockState blockState) {
        super(Markers.MARKER_BLOCK_ENTITY, pos, blockState);
        this.customName = "";
        this.markerOwner = null;
        this.xp = 0;
        this.items = DefaultedList.ofSize(41, ItemStack.EMPTY);
        setState(blockState);
    }
    
    public void setItems(DefaultedList<ItemStack> items) {
        this.items = items;
        this.markDirty();
    }
    
    public DefaultedList<ItemStack> getItems() {
        return items;
    }
    
    public boolean hasItems() {
        return items.isEmpty();
    }
    
    public void setMarkerOwner(GameProfile gameProfile) {
        this.markerOwner = gameProfile;
        this.markDirty();
    }
    
    public GameProfile getMarkerOwner() {
        return markerOwner;
    }
    
    public boolean isMarkerOwner(GameProfile profile) {
        return markerOwner == profile;
    }
    
    public void setCustomNametag(String text) {
        this.customName = text;
        this.markDirty();
    }
    
    public boolean hasCustomNametag() {
        return customName == "";
    }
    
    public String getCustomNametag() {
        return customName;
    }
    
    public int getXp() {
        return xp;
    }
    
    public void setXp(int xp) {
        this.xp = xp;
        this.markDirty();
    }
    
    @Override
    public void readNbt(NbtCompound tag) {
        super.writeNbt(tag);

        this.items = DefaultedList.ofSize(tag.getInt("ItemCount"), ItemStack.EMPTY);

        Inventories.readNbt(tag.getCompound("Items"), this.items);

        this.xp = tag.getInt("XP");

        if(tag.contains("MarkerOwner"))
            this.markerOwner = NbtHelper.toGameProfile(tag.getCompound("MarkerOwner"));

        if(tag.contains("CustomName"))
            this.customName = tag.getString("CustomName");
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        tag.putInt("ItemCount", this.items.size());

        tag.put("Items", Inventories.writeNbt(new NbtCompound(), this.items, true));

        tag.putInt("XP", xp);

        if(markerOwner != null)
            tag.put("MarkerOwner", NbtHelper.writeGameProfile(new NbtCompound(), markerOwner));
        if(customName != null && !customName.isEmpty())
            tag.putString("CustomName", customName);

        return tag;
    }
    
    @Override
    public void fromClientTag(NbtCompound compoundTag) {
        if(compoundTag.contains("MarkerOwner"))
            this.markerOwner = NbtHelper.toGameProfile(compoundTag.getCompound("MarkerOwner"));
        if(compoundTag.contains("CustomName"))
            this.customName = compoundTag.getString("CustomName");
    }
    
    @Override
    public NbtCompound toClientTag(NbtCompound compoundTag) {
        if(markerOwner != null)
            compoundTag.put("MarkerOwner", NbtHelper.writeGameProfile(new NbtCompound(), this.markerOwner));
        if(customName != null && !customName.isEmpty())
            compoundTag.putString("CustomName", customName);

        return compoundTag;
    }
    
	public BlockState getState() {
		return state;
	}
	
	public void setState(BlockState state) {
		this.state = state;
	}
}
