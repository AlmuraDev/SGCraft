//------------------------------------------------------------------------------------------------
//
//   SG Craft - Iris Entity
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.entity;

import gcewing.sg.BaseBlockUtils;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.Trans3;
import gcewing.sg.util.GateShape;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.io.DataInput;
import java.io.DataOutput;

public class EntityStargateIris extends Entity implements IEntityAdditionalSpawnData {

    BlockPos blockPos;
    
    public EntityStargateIris(World world) {
        super(world);
    }

    public EntityStargateIris(SGBaseTE te) {
        this(te.getWorld());
        GateShape shape = te.getShape();
        double radius = shape.ringInnerRadius;
        double thickness = SGBaseTE.irisThickness;
        AxisAlignedBB localBox;
        if (te.gateOrientation == 1) { // Vertical
            double cx = 0;
            double cy = shape.centreY;
            double cz = SGBaseTE.irisZPosition;
            double hx = radius;
            double hy = radius;
            double hz = thickness;
            localBox = new AxisAlignedBB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
        } else {
            double cx = 0;
            double cy = SGBaseTE.irisZPosition;
            double cz = -shape.centreY;
            double hx = radius;
            double hy = thickness;
            double hz = radius;
            localBox = new AxisAlignedBB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
        }

        Trans3 t = te.localToGlobalTransformation();
        AxisAlignedBB globalBox = t.t(localBox);
        //System.out.printf("EntityStargateIris.init: local %s\n", localBox);
        init(te.getPos(), globalBox);
    }
    
    void init(BlockPos pos, AxisAlignedBB box) {
        //System.out.printf("EntityStargateIris.init: %s at %s box %s\n", this, pos, box);
        this.blockPos = pos;
        setPosition(box.minX, box.minY, box.minZ);
        setEntityBoundingBox(box);
    }
    
    @Override
    protected void entityInit() {}

    SGBaseTE getBaseTE() {
        TileEntity te = world.getTileEntity(blockPos);
        return te instanceof SGBaseTE ? (SGBaseTE) te : null;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        SGBaseTE te = getBaseTE();
        return te != null && te.irisIsClosed();
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return canBeCollidedWith() ? super.getEntityBoundingBox() : null;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
    
    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        //System.out.printf("EntityStargateIris.readEntityFromNBT\n");
        int blockX = nbt.getInteger("blockX");
        int blockY = nbt.getInteger("blockY");
        int blockZ = nbt.getInteger("blockZ");
        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        double minX = nbt.getDouble("minX");
        double minY = nbt.getDouble("minY");
        double minZ = nbt.getDouble("minZ");
        double maxX = nbt.getDouble("maxX");
        double maxY = nbt.getDouble("maxY");
        double maxZ = nbt.getDouble("maxZ");
        AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        init(pos, box);
    }
    
    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        //System.out.printf("EntityStargateIris.writeEntityToNBT\n");
        nbt.setInteger("blockX", blockPos.getX());
        nbt.setInteger("blockY", blockPos.getY());
        nbt.setInteger("blockZ", blockPos.getZ());
        AxisAlignedBB box = super.getEntityBoundingBox();
        nbt.setDouble("minX", box.minX);
        nbt.setDouble("minY", box.minY);
        nbt.setDouble("minZ", box.minZ);
        nbt.setDouble("maxX", box.maxX);
        nbt.setDouble("maxY", box.maxY);
        nbt.setDouble("maxZ", box.maxZ);
    }

    @Override
    protected boolean shouldSetPosAfterLoading() {
        return false; // To prevent bounding box being reset
    }

    // --------------------------- Spawn Data ---------------------------

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        //System.out.printf("EntityStargateIris.writeSpawnData\n");
        try {
            DataOutput data = new ByteBufOutputStream(buffer);
            BaseBlockUtils.writeBlockPos(data, blockPos);
            AxisAlignedBB box = super.getEntityBoundingBox();
            data.writeDouble(box.minX);
            data.writeDouble(box.minY);
            data.writeDouble(box.minZ);
            data.writeDouble(box.maxX);
            data.writeDouble(box.maxY);
            data.writeDouble(box.maxZ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void readSpawnData(ByteBuf buffer) {
        //System.out.printf("EntityStargateIris.readSpawnData\n");
        try {
            DataInput data = new ByteBufInputStream(buffer);
            BlockPos pos = BaseBlockUtils.readBlockPos(data);
            double minX = data.readDouble();
            double minY = data.readDouble();
            double minZ = data.readDouble();
            double maxX = data.readDouble();
            double maxY = data.readDouble();
            double maxZ = data.readDouble();
            AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
            init(pos, box);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
