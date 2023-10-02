package chiefarug.mods.morethan50.mixin;

import chiefarug.mods.morethan50.MoreThan50;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
	)
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
	private static AABB morethan50$increaseRange(AABB _aabb, Level level, BlockPos pos, int tier) {
		// Because we are using ModifyVariable instead of some other way, we get no context except the AABB.
		// Thankfully, we can reconstruct literally everything we need from that.
		// Stay in school kids, Maths is cool.
		int normalRange = tier * 10 + 10;
		if (tier >= infiniteRangeThreshold.get()) return INFINITE_EXTENT_AABB;

		AABB aabb = new AABB(pos).inflate(normalRange * multiplier.get());
		if (verticalMode.get().up) aabb = aabb.expandTowards(0, level.getMaxBuildHeight(), 0);
		if (verticalMode.get().down) aabb = aabb.expandTowards(0, level.getMinBuildHeight(), 0);

		return aabb;
	}

	@WrapOperation(
			method = "applyEffects",
			at = @At(value = "INVOKE", target =  "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
	)
	private static List<? extends Player> morethan50$allowInfiniteAABB(Level level, Class<?> clazz, AABB aabb, Operation<List<Player>> defaultOp) {
		if (aabb.equals(INFINITE_EXTENT_AABB)) {
			if (clazz == Player.class) {
				return level.players();
			} else if (!morethan50$warnedAboutNonPlayers) {
				LGGR.warn("Some other mod is messing with beacons and trying to apply effects to non players (specifically {})! MoreThan50 is unable to apply effects infinitely.", clazz);
				morethan50$warnedAboutNonPlayers = true;
			}
		}
		return defaultOp.call(level, clazz, aabb);
	}
//	@ModifyArg(
//			method = "applyEffects",
//			at = @At(value = "NEW", target = "(Lnet/minecraft/world/effect/MobEffect;IIZZ)Lnet/minecraft/world/effect/MobEffectInstance;"),
//			index = 1,
//			expect = 2
//	)
	@ModifyArg(
		method = "applyEffects",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;<init>(Lnet/minecraft/world/effect/MobEffect;IIZZ)V"),
		index = 4,
		expect = 2
	)
	private static boolean morethan50$disableParticles(boolean showParticles) {
		// If the config option is disabled, rely on the default. If it is enabled, then cancel no matter what.
		return !hideParticles.get() && showParticles;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private static void morethan50$backupChunkloadOnTick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity be, CallbackInfo ci) {
		BeaconExtenderMixin accessor = ((BeaconExtenderMixin) ((BlockEntity) be));
		if (chunkLoad.get()) {
			if (!accessor.morethan50$haveWeChunkloaded && level instanceof ServerLevel sl) {
				ChunkPos chunkPos = new ChunkPos(be.getBlockPos());
				accessor.morethan50$haveWeChunkloaded = ForgeChunkManager.forceChunk(sl, MODID, pos, chunkPos.x, chunkPos.z, true, true);
			}
		} else if (accessor.morethan50$haveWeChunkloaded && level instanceof ServerLevel sl) {
			// if the config option is disabled, and we have chunkloaded, then stop chunkloading
			ChunkPos chunkPos = new ChunkPos(be.getBlockPos());
			accessor.morethan50$haveWeChunkloaded = !ForgeChunkManager.forceChunk(sl, MODID, be.getBlockPos(), chunkPos.x, chunkPos.z, false, false);
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
