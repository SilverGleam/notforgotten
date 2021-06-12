package net.gleam.markers.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.gleam.markers.Markers;
import net.gleam.markers.api.MarkersApi;
import net.gleam.markers.block.entity.MarkerBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MarkerBase extends HorizontalFacingBlock implements BlockEntityProvider, AgingMarker {
	
	private final BlockAge blockAge;
	
    public MarkerBase(BlockAge blockAge, Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        this.blockAge = blockAge;
    }

    FlowerBlock f;
    
    public int getWeathered() {
    	int stage;
    	switch(blockAge) {
    		default:
    			stage = 0;
    			break;
    		case OLD:
    			stage =  1;
        		break;
    		case WEATHERED:
    			stage =  2;
    			break;
    		case FORGOTTEN:
    			stage =  3;
    			break;
    	}
    	return stage;
    }
    
    public MarkerBase getAgedBlock() {
    	switch(blockAge) {
    		default:
    			return Markers.MARKER;
    		case OLD:
    			return Markers.MARKER_OLD;
    		case WEATHERED:
    			return Markers.MARKER_WEATHERED;
    		case FORGOTTEN:
    			return Markers.MARKER_FORGOTTEN;
    	}
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(Properties.HORIZONTAL_FACING);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(player.getStackInHand(hand).isEmpty() && ((MarkerBlockEntity)world.getBlockEntity(pos)).isMarkerOwner(player.getGameProfile())) {useMarker(player, world, pos);}

        return super.onUse(state, world, pos, player, hand, hit);
    }
    
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    	//if(((MarkerBlockEntity)world.getBlockEntity(pos)).isMarkerOwner(player.getGameProfile())) {useMarker(player, world, pos);}
    	useMarker(player, world, pos);
    	//Haven't been able to actually make this unbreakable if a non-owner breaks the marker.
    	//Pretty annoying but I need sleep so I'll work on this later.
        super.onBreak(world, pos, state, player);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ct) {
        return VoxelShapes.cuboid(0.062f, 0f, 0.062f, 0.938f, 0.07f, 0.938f);//return VoxelShapes.cuboid(0.062f, 0f, 0.0f, 0.938f, 0.07f, 1.0f);
    }
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0.062f, 0f, 0.062f, 0.938f, 0.02f, 0.938f);
    }
    
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.IGNORE;
    }
    
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerBlockEntity(pos, state);
    }
    
    private boolean useMarker(PlayerEntity playerEntity, World world, BlockPos pos) {
        if(world.isClient) return false;

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if(!(blockEntity instanceof MarkerBlockEntity)) {return false;}
        MarkerBlockEntity markerBlockEntity = (MarkerBlockEntity) blockEntity;

        markerBlockEntity.sync();

        if(markerBlockEntity.getItems() == null) {return false;}
        if(markerBlockEntity.getMarkerOwner() == null) {return false;}
        if (!playerEntity.getGameProfile().getId().equals(markerBlockEntity.getMarkerOwner().getId())) {return false;}

        DefaultedList<ItemStack> items = markerBlockEntity.getItems();

        DefaultedList<ItemStack> inventory = DefaultedList.of();

        inventory.addAll(playerEntity.getInventory().main);
        inventory.addAll(playerEntity.getInventory().armor);
        inventory.addAll(playerEntity.getInventory().offHand);

        playerEntity.getInventory().clear();

        List<ItemStack> armor = items.subList(36, 40);

        for (int i = 0; i < armor.size(); i++) {
        EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(armor.get(i));
        playerEntity.equipStack(equipmentSlot, armor.get(i));
        }
        playerEntity.equipStack(EquipmentSlot.OFFHAND, items.get(40));
        List<ItemStack> mainInventory = items.subList(0, 36);
        for (int i = 0; i < mainInventory.size(); i++) {
        	playerEntity.getInventory().setStack(i, mainInventory.get(i));
        }
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        List<Integer> openArmorSlots = getInventoryOpenSlots(playerEntity.getInventory().armor);
        for(int i = 0; i < 4; i++) {
        	if(openArmorSlots.contains(i)) {
        		playerEntity.equipStack(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i), inventory.subList(36, 40).get(i));
        	}else{
        		extraItems.add(inventory.subList(36, 40).get(i));
        	}
        }
        if(playerEntity.getInventory().offHand.get(0) == ItemStack.EMPTY) {
        	playerEntity.equipStack(EquipmentSlot.OFFHAND, inventory.get(40));
        }else{
        	extraItems.add(inventory.get(40));
        }
        extraItems.addAll(inventory.subList(0, 36));
        List<Integer> openSlots = getInventoryOpenSlots(playerEntity.getInventory().main);
        for(int i = 0; i < openSlots.size(); i++) {
        	playerEntity.getInventory().setStack(openSlots.get(i), extraItems.get(i));
        }
        DefaultedList<ItemStack> dropItems = DefaultedList.of();
        dropItems.addAll(extraItems.subList(openSlots.size(), extraItems.size()));
        int inventoryOffset = 41;
        for(MarkersApi markersApi : Markers.apiMods) {
        	markersApi.setInventory(items.subList(inventoryOffset, inventoryOffset + markersApi.getInventorySize(playerEntity)), playerEntity);
        	inventoryOffset += markersApi.getInventorySize(playerEntity);
        }
        ItemScatterer.spawn(world, pos, dropItems);
        playerEntity.addExperience((int) (1 * markerBlockEntity.getXp()));
        spawnBreakParticles(world, playerEntity, pos, getDefaultState());
        world.removeBlock(pos, false);
        return true;
    }
    
    private List<Integer> getInventoryOpenSlots(DefaultedList<ItemStack> inventory) {
        List<Integer> openSlots = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            if(inventory.get(i) == ItemStack.EMPTY)
                openSlots.add(i);
        }
        return openSlots;
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
    	BlockEntity blockEntity = world.getBlockEntity(pos);
    	
        if(!(blockEntity instanceof MarkerBlockEntity) || !itemStack.hasCustomName()) {
            super.onPlaced(world, pos, state, placer, itemStack);
            return;
        }
        
        MarkerBlockEntity markerBlockEntity = (MarkerBlockEntity) blockEntity;

        markerBlockEntity.setCustomNametag(itemStack.getOrCreateSubTag("display").getString("Name"));
    }
    
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }
    
    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.tickDegradation(state, world, pos, random);
    }
    
    @Override
    public boolean hasRandomTicks(BlockState state) {
        return AgingMarker.getIncreasedOxidationBlock(state.getBlock()).isPresent();
    }

    @Override
	public BlockAge getDegradationLevel() {
        return this.blockAge;
    }
}
