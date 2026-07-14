package dev.zelo.renderscale.legacy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;

@Mod(
    modid = "renderscale",
    name = "RenderScale Legacy",
    version = "1.0.1",
    acceptableRemoteVersions = "*"
)
public final class RenderScaleLegacyMod {
    private static double scaleFactor = 2.0D;
    private static boolean linearFilter = true;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        Configuration configuration =
                new Configuration(event.getSuggestedConfigurationFile());
        try {
            configuration.load();
            scaleFactor = configuration.get("render", "scaleFactor", 2.0D,
                    "3D-world resolution multiplier. 2.0 renders twice the width "
                    + "and twice the height. Range: 1.0 to 4.0.").getDouble(2.0D);
            if (scaleFactor < 1.0D) scaleFactor = 1.0D;
            if (scaleFactor > 4.0D) scaleFactor = 4.0D;
            linearFilter = configuration.get("render", "linearFilter", true,
                    "Use linear filtering when resolving the scaled world.").getBoolean(true);
        } finally {
            if (configuration.hasChanged()) configuration.save();
        }
        System.out.println("[RenderScale Legacy] Configured for " + scaleFactor
                + "x world rendering (linearFilter=" + linearFilter + ").");
    }

    public static double getScaleFactor() {
        return scaleFactor;
    }

    public static boolean useLinearFilter() {
        return linearFilter;
    }
}
