package chiefarug.mods.morethan50.mixin;

import chiefarug.mods.morethan50.MoreThan50;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static chiefarug.mods.morethan50.MoreThan50.*;
import static chiefarug.mods.morethan50.MoreThan50.LGGR;

@Mixin(BeaconBlockEntity.class)
public class BeaconExtenderMixin extends BlockEntity {

	@Unique
	private boolean morethan50$haveWeChunkloaded = false;
	@Unique
	private static boolean morethan50$warnedAboutNonPlayers = false;

	public BeaconExtenderMixin(BlockEntityType<?> _t, BlockPos _p, BlockState _bs) {
		super(_t, _p, _bs);
	}

	@ModifyVariable(
			method = "applyEffects",
			at = @At("STORE")
	)//TODO: rewrite this bit to use WrapOperation and just use what is passed in
	/*
	  	@WrapOperation(
			method = "applyEffects",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;")
		)
	 */
	//***************************************************************************************************************
	//         Target Class : net.minecraft.world.level.block.entity.BeaconBlockEntity
	//        Target Method : applyEffects
	//        Callback Name : morethan50$increaseRange
	//         Capture Type : AABB
	//          Instruction : [58] LabelNode UNKNOWN
	//***************************************************************************************************************
	//           Match mode : IMPLICIT (match single) - VALID (exactly 1 match)
	//        Match ordinal : any
	//          Match index : any
	//        Match name(s) : any
	//            Args only : false
	//***************************************************************************************************************
	// INDEX  ORDINAL                            TYPE  NAME                                                CANDIDATE
	// [  0]    [  0]                           Level  pLevel                                              -
	// [  1]    [  0]                        BlockPos  pPos                                                -
	// [  2]    [  0]                             int  pLevels                                             -
	// [  3]    [  0]                       MobEffect  pPrimary                                            -
	// [  4]    [  1]                       MobEffect  pSecondary                                          -
	// [  5]    [  0]                          double  d0                                                  -
	// [  6]                                    <top>
	// [  7]    [  1]                             int  i                                                   -
	// [  8]    [  2]                             int  j                                                   -
	// [  9]    [  0]                            AABB  aabb                                                YES
	// [ 10]                                        -
	// [ 11]                                        -
	// [ 12]                                        -
	//***************************************************************************************************************
	private static AABB morethan50$increaseRange(AABB aabb) {
		// Because we are using ModifyVariable instead of some other way, we get no context except the AABB.
		// Thankfully, we can reconstruct literally everything we need from that.
		// Stay in school kids, Maths is cool.
		int normalRange = (int) ((aabb.getXsize() - 1) / 2);
		if (normalRange >= infiniteRangeThreshold.get()) return INFINITE_EXTENT_AABB;

		Vec3 vec3 = aabb.getCenter();
		BlockPos beaconPos = new BlockPos(vec3.x, aabb.minY + normalRange, vec3.z);
//		int extraUp = verticalMode.get().up ? (int) (aabb.maxY - normalRange - beaconPos.getY()) : 0;
//		int extraDown = verticalMode.get().down ? (int) (aabb.maxY - normalRange - beaconPos.getY()) : 0;

		AABB newBox = new AABB(beaconPos).inflate(normalRange * multiplier.get());

		return newBox;
	}

	@WrapOperation(
			method = "applyEffects",
			at = @At(value = "INVOKE", target =  "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
	)
	private static List<? extends Player> morethan50$allowInfiniteAABB(Level level, Class<?> clazz, AABB aabb, Operation<List<Player>> defautlOp) {
		if (aabb.equals(INFINITE_EXTENT_AABB)) {
			if (clazz == Player.class) {
				return level.players();
			} else {
				LGGR.warn("Some other mod is messing with beacons and trying to apply effects to non players (specifically {})! MoreThan50 is unable to apply effects infinitely.", clazz);
				morethan50$warnedAboutNonPlayers = true;
			}
		}
		return defautlOp.call(level, clazz, aabb);
	}

	// DO NOT TRY TO INJECT HERE TO LOAD CHUNKS
	// It causes a deadlock on world load for... reasons.
	// Even though level#isLoaded returns true
//	@Inject(method = "setLevel", at = @At("HEAD"))
//	private void morethan50$startChunkLoading(Level lvl, CallbackInfo ci) {}

	@Inject(method = "tick", at = @At("HEAD"))
	private static void morethan50$backupChunkloadOnTick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity be, CallbackInfo ci) {
		BeaconExtenderMixin accessor = ((BeaconExtenderMixin) ((BlockEntity) be));
		if (!accessor.morethan50$haveWeChunkloaded) {
			if (level instanceof ServerLevel sl/* && sl.isLoaded(pos)*/) {
				ChunkPos chunkPos = new ChunkPos(be.getBlockPos());
				accessor.morethan50$haveWeChunkloaded = ForgeChunkManager.forceChunk(sl, MODID, pos, chunkPos.x, chunkPos.z, true, true);
			}
		}
	}

	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void morethan50$stopChunkloading(CallbackInfo ci) {
		if (this.level instanceof ServerLevel sl) {
			ChunkPos chunkPos = new ChunkPos(this.getBlockPos());
			ForgeChunkManager.forceChunk(sl, MODID, this.getBlockPos(), chunkPos.x, chunkPos.z, false, false);
		}
	}
}
