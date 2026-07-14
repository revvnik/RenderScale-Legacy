package dev.zelo.renderscale.legacy;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("RenderScale Legacy Core")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1100)
@IFMLLoadingPlugin.TransformerExclusions({ "dev.zelo.renderscale.legacy" })
public final class RenderScaleLoadingPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() {
        return new String[] {
                "dev.zelo.renderscale.legacy.EntityRendererTransformer"
        };
    }

    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) { }
    public String getAccessTransformerClass() { return null; }
}
