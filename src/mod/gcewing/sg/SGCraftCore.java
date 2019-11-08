package gcewing.sg;

import java.util.Map;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class SGCraftCore implements IFMLLoadingPlugin {

	public SGCraftCore() {
		Launch.classLoader.addTransformerExclusion("org.spongepowered.launch.JavaVersionCheckUtils");
		
		
		Launch.classLoader.addTransformerExclusion("org.spongepowered.common.mixin.handler.TerminateVM");
		Launch.classLoader.addTransformerExclusion("scala.");
	}
	
	@Override
	public String[] getASMTransformerClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getModContainerClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSetupClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getAccessTransformerClass() {
		// TODO Auto-generated method stub
		return null;
	}

}
