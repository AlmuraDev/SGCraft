//------------------------------------------------------------------------------------------------
//
//   SG Craft - DHD tile entity renderer
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.client.renderer;

import gcewing.sg.BaseModClient.IModel;
import gcewing.sg.BaseModClient.IRenderTarget;
import gcewing.sg.BaseModClient.ITexture;
import gcewing.sg.BaseModClient.ITiledTexture;
import gcewing.sg.BaseModel;
import gcewing.sg.BaseTexture.Image;
import gcewing.sg.BaseTileEntity;
import gcewing.sg.BaseTileEntityRenderer;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.SGCraft;
import gcewing.sg.Trans3;
import net.minecraft.util.ResourceLocation;

public class DHDTERenderer extends BaseTileEntityRenderer {

    IModel model;
    ITexture mainTexture;
    ITexture[] milkywayButtonTextures, pegasusButtonTextures;
    ITexture[] textures;
    
    final static int buttonTextureIndex = 3;
    
    public DHDTERenderer() {
        SGCraft mod = SGCraft.mod;
        ResourceLocation ttLoc = mod.textureLocation("tileentity/dhd_top.png");
        ResourceLocation stLoc = mod.textureLocation("tileentity/dhd_side.png");
        ResourceLocation dtLoc = mod.textureLocation("tileentity/dhd_detail.png");
        ITiledTexture detail = new Image(dtLoc).tiled(2, 2);
        textures = new ITexture[] {
            new Image(ttLoc),
            new Image(stLoc),
            detail.tile(1, 1),
            null, // button texture inserted here
        };
        ITexture button = detail.tile(0, 0);
        milkywayButtonTextures = new ITexture[] {
            button.colored(0.5, 0.5,  0.5),
            button.colored(0.5, 0.25, 0.0),
            button.colored(1.0, 0.5, 0.0).emissive(),
        };
        pegasusButtonTextures = new ITexture[] {
            button.colored(0.0, 0.5,  0.5),
            button.colored(0.0, 0.25, 0.75),
            button.colored(0.0, 0.5, 1.0).emissive(),
        };
        model = BaseModel.fromResource(mod.resourceLocation("models/block/dhd.smeg"));
        DHDTE.bounds = model.getBounds();
    }
    
    public void render(BaseTileEntity te, float dt, int destroyStage, Trans3 t, IRenderTarget target) {
        DHDTE dte = (DHDTE)te;
        SGBaseTE gte = dte.getLinkedStargateTE();
        int i;
        if (gte == null)
            i = 0;
        else if (gte.isActive())
            i = 2;
        else
            i = 1;
        if (gte == null) {
            textures[buttonTextureIndex] = milkywayButtonTextures[i];
        } else {
            textures[buttonTextureIndex] = gte.getGateType().select(milkywayButtonTextures, pegasusButtonTextures)[i];
        }
        model.render(t.translate(0, -0.5, 0), target, textures);
    }
}
