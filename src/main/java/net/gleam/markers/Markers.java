package net.gleam.markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.gleam.markers.api.MarkersApi;
import net.gleam.markers.block.AgingMarker.BlockAge;
import net.gleam.markers.block.MarkerBase;
import net.gleam.markers.block.entity.MarkerBlockEntity;
import net.gleam.markers.util.MarkerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class Markers implements ModInitializer {
	
	public static final MarkerBase MARKER_FORGOTTEN = new MarkerBase(BlockAge.FORGOTTEN, FabricBlockSettings.of(Material.ORGANIC_PRODUCT).strength(0.8f, -1f));
	public static final MarkerBase MARKER_WEATHERED = new MarkerBase(BlockAge.WEATHERED, FabricBlockSettings.of(Material.ORGANIC_PRODUCT).strength(0.8f, -1f));
	public static final MarkerBase MARKER_OLD = new MarkerBase(BlockAge.OLD, FabricBlockSettings.of(Material.ORGANIC_PRODUCT).strength(0.8f, -1f));
	public static final MarkerBase MARKER = new MarkerBase(BlockAge.FRESH, FabricBlockSettings.of(Material.ORGANIC_PRODUCT).strength(0.8f, -1f));
	
	public static BlockEntityType<MarkerBlockEntity> MARKER_BLOCK_ENTITY;

	public static final ArrayList<MarkersApi> apiMods = new ArrayList<>();
	
	public static String MOD_ID = "notforgotten";
	public static String BRAND_BLOCK = "marker";//Maybe name can be changed in config? Also helps for diy quick and easy locale. Plus people who want to call it a gravestone can.
	//tbh I'm tired

	@Override
	public void onInitialize() {
		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, BRAND_BLOCK), MARKER);
		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, BRAND_BLOCK+"_old"), MARKER_OLD);
		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, BRAND_BLOCK+"_weathered"), MARKER_WEATHERED);
		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, BRAND_BLOCK+"_forgotten"), MARKER_FORGOTTEN);
		MARKER_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, MOD_ID+":"+BRAND_BLOCK, FabricBlockEntityTypeBuilder.create(MarkerBlockEntity::new, MARKER,MARKER_OLD,MARKER_WEATHERED,MARKER_FORGOTTEN).build(null));
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, BRAND_BLOCK), new BlockItem(MARKER, new Item.Settings().group(ItemGroup.DECORATIONS)));
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, BRAND_BLOCK+"_old"), new BlockItem(MARKER_OLD, new Item.Settings().group(ItemGroup.DECORATIONS)));
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, BRAND_BLOCK+"_weathered"), new BlockItem(MARKER_WEATHERED, new Item.Settings().group(ItemGroup.DECORATIONS)));
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, BRAND_BLOCK+"_forgotten"), new BlockItem(MARKER_FORGOTTEN, new Item.Settings().group(ItemGroup.DECORATIONS)));
		
		apiMods.addAll(FabricLoader.getInstance().getEntrypoints(MOD_ID, MarkersApi.class));
		
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			if(entity instanceof MarkerBlockEntity) {
				MarkerBlockEntity markerBlockEntity = (MarkerBlockEntity) entity;

				if(markerBlockEntity.getMarkerOwner() != null && !markerBlockEntity.getMarkerOwner().getId().equals(player.getGameProfile().getId())) return true;

				if(markerBlockEntity.getMarkerOwner() != null)
					if(!markerBlockEntity.getMarkerOwner().getId().equals(player.getGameProfile().getId()))
						return false;
			}
			return true;
		});
	}

	public static void placeMarker(World world, Vec3d pos, PlayerEntity player) {
		if (world.isClient) return;

		BlockPos blockPos = new BlockPos(pos.x, pos.y - 1, pos.z);

		DefaultedList<ItemStack> combinedInventory = DefaultedList.of();

		combinedInventory.addAll(player.getInventory().main);
		combinedInventory.addAll(player.getInventory().armor);
		/*if(){//Compat Inventories. when I don't need to sleep lol
		 * combinedInventory.addAll(player.getInventory().armor);
		 * }*/
		combinedInventory.addAll(player.getInventory().offHand);

		for (MarkersApi markersApi : Markers.apiMods) {
			combinedInventory.addAll(markersApi.getInventory(player));
		}

		if(blockPos.getY() < 0) {
			blockPos = new BlockPos(blockPos.getX(), 10, blockPos.getZ());
		}
		
		for (BlockPos markerPos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
			BlockState blockState = world.getBlockState(markerPos);
			Block block = blockState.getBlock();

			if(canPlaceMarker(world, block, markerPos)) {
				world.setBlockState(markerPos, Markers.MARKER.getDefaultState().with(Properties.HORIZONTAL_FACING, player.getHorizontalFacing()));
				
				MarkerBlockEntity markerBlockEntity = new MarkerBlockEntity(markerPos, world.getBlockState(markerPos));
				markerBlockEntity.setItems(combinedInventory);
				markerBlockEntity.setMarkerOwner(player.getGameProfile());
				int currentExperience = MarkerUtil.calculateTotalExperience(
						player.experienceLevel, 
						player.experienceProgress
				);
				markerBlockEntity.setXp(currentExperience);
				player.totalExperience = 0;
				player.experienceProgress = 0;
				player.experienceLevel = 0;
				world.addBlockEntity(markerBlockEntity);

				if(world.isClient()) {
					markerBlockEntity.sync();
				}
				block.onBreak(world, blockPos, blockState, player);

				player.sendMessage(new TranslatableText("text.notforgotten.mark_coords", markerPos.getX(), markerPos.getY(), markerPos.getZ()), false);
				System.out.println("[Markers] Marker spawned at: " + markerPos.getX() + ", " + markerPos.getY() + ", " + markerPos.getZ());
				break;
			}
		}
	}

	private static boolean canPlaceMarker(World world, Block block, BlockPos blockPos) {
		BlockEntity blockEntity = world.getBlockEntity(blockPos);

		if(blockEntity != null) return false;
		//Check if it's in a blacklisted/non-whitelisted dimension
		//Check if it's in a blacklisted/non-whitelisted biome

		Set<Block> blackListedBlocks = new HashSet<Block>() {{
			add(Blocks.BEDROCK);
		}};

		if(blackListedBlocks.contains(block)) return false;

		return !(blockPos.getY() < 0 || blockPos.getY() > 255);
	}
}

