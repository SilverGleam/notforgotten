package net.gleam.markers.mixin;

import net.gleam.markers.Markers;
import net.gleam.markers.block.entity.MarkerBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Explosion.class)
public class ExplosionMixin {
  @Shadow
  @Final
  World world;
  private BlockPos lastPos;
  private Set<Block> markerBlocks = new HashSet<Block>() {
    {
      add(Markers.MARKER);
      add(Markers.MARKER_OLD);
      add(Markers.MARKER_WEATHERED);
      add(Markers.MARKER_FORGOTTEN);
    }
  };

  @ModifyVariable(method = "affectWorld", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
  private BlockPos modifyAffectedBlocks(BlockPos old) {
    lastPos = old;
    return old;
  }

  @ModifyVariable(method = "affectWorld", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
  private BlockState modifyAffectedBlocks(BlockState old) {
    if (markerBlocks.contains(old.getBlock())) {
      BlockEntity blockEntity = world.getBlockEntity(lastPos);

      if (blockEntity instanceof MarkerBlockEntity) {
        MarkerBlockEntity markerBlockEntity = (MarkerBlockEntity) blockEntity;

        if (markerBlockEntity.getMarkerOwner() != null)
          return Blocks.AIR.getDefaultState();
      }
    }

    return old;
  }
}