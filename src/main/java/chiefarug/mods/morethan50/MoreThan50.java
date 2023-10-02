package chiefarug.mods.morethan50;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MoreThan50.MODID)
public class MoreThan50 {
	public static final String MODID = "morethan50";
	public static final Logger LGGR = LogUtils.getLogger();

	public static ForgeConfigSpec.DoubleValue multiplier;
	public static ForgeConfigSpec.BooleanValue chunkLoad;
	public static ForgeConfigSpec.IntValue infiniteRangeThreshold;
	public static ForgeConfigSpec.EnumValue<VerticalMode> verticalMode;
	public static ForgeConfigSpec.BooleanValue hideParticles;

	public MoreThan50() {
		ForgeConfigSpec.Builder specBuilder = new ForgeConfigSpec.Builder();
		specBuilder.comment("The multiplier to apply to the beacons range. A value of 1 will use vanilla behaviour. A value of 0 will stop beacons applying effects.");
		multiplier = specBuilder.defineInRange("size_multiplier", 3, 0, Double.POSITIVE_INFINITY);
		specBuilder.comment("Whether to forceload the chunk the beacon is in or not. ", "Recommended to be on when the multiplier * 50 is greater than your simulation distance * 16, or you have the range set to infinite at any tier");
		chunkLoad = specBuilder.define("forceload", true);
		specBuilder.comment("The tier at which the range becomes infinite.", "The default is 5, meaning that unless you have another mod that adds another tier, the range is never infinite.");
		infiniteRangeThreshold = specBuilder.defineInRange("infiniteRangeThreshold", 5, 0, Integer.MAX_VALUE);
		specBuilder.comment("How the range should be extended vertically. In vanilla this is UP, to the sky limit", "Can also be DOWN (to bedrock/the void), BOTH (extend both up and down) or NONE (only use the regular range to extend up and down)");
		verticalMode = specBuilder.defineEnum("verticalMode", VerticalMode.UP);
		specBuilder.comment("If particles from beacons effects should be visible. false will use existing behaviour, which in vanilla is to show particles, however mods may change that.");
		hideParticles = specBuilder.define("hideParticles", true);

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, specBuilder.build());
	}
	public enum VerticalMode {
		UP(true, false),
		DOWN(false, true),
		BOTH(true, true),
		NONE(false, false);
		public final boolean up;
		public final boolean down;
		VerticalMode(boolean up, boolean down) {
			this.up = up;
			this.down = down;
		}
	}

}
