//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate base tile entity
//
//------------------------------------------------------------------------------------------------

package gcewing.sg;

import gcewing.sg.oc.OCWirelessEndpoint;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.network.play.server.SPacketSetExperience;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.network.ForgeMessage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static gcewing.sg.BaseBlockUtils.getWorldTileEntity;
import static gcewing.sg.BaseUtils.max;
import static gcewing.sg.BaseUtils.min;

public class SGBaseTE extends BaseTileInventory implements ITickable, LoopingSoundSource {

    static boolean debugState = false;
    static boolean debugEnergyUse = false;
    static boolean debugConnect = false;
    static boolean debugTransientDamage = false;
    static boolean debugTeleport = false;

    static SoundEvent
        dialFailSound,
        connectSound,
        disconnectSound,
        irisOpenSound,
        irisCloseSound,
        irisHitSound,
        dhdPressSound,
        dhdDialSound,
        chevronOutgoingSound,
        chevronIncomingSound,
        lockOutgoingSound,
        lockIncomingSound,
        gateRollSound;
    
    public static void registerSounds(SGCraft mod) {
        dialFailSound = mod.newSound("dial_fail");
        connectSound = mod.newSound("gate_open");
        disconnectSound = mod.newSound("gate_close");
        irisOpenSound = mod.newSound("iris_open");
        irisCloseSound = mod.newSound("iris_close");
        irisHitSound = mod.newSound("iris_hit");
        dhdPressSound = mod.newSound("dhd_press");
        dhdDialSound = mod.newSound("dhd_dial");
        chevronOutgoingSound = mod.newSound("chevron_outgoing");
        chevronIncomingSound = mod.newSound("chevron_incoming");
        lockOutgoingSound = mod.newSound("lock_outgoing");
        lockIncomingSound = mod.newSound("lock_incoming");
        gateRollSound = mod.newSound("gate_roll");
    }

    public final static String symbolChars = SGAddressing.symbolChars;
    public final static int numRingSymbols = SGAddressing.numSymbols;
    public final static double ringSymbolAngle = 360.0 / numRingSymbols;
    public final static double irisZPosition = 0.1;
    public final static double irisThickness = 0.2; //0.1;
    public final static DamageSource irisDamageSource = new DamageSource("sgcraft:iris");
    public final static float irisDamageAmount = 1000000;
    public final static double ringRotationSpeed = 2.0;

    final static int interDiallingTime = 10; // ticks
    final static int syncAwaitTime = 20; // ticks
    final static int transientDuration = 20; // ticks
    final static int disconnectTime = 40; // ticks
    
    final static double openingTransientIntensity = 1.3;
    final static double openingTransientRandomness = 0.25;
    final static double closingTransientRandomness = 0.25;
    final static double transientDamageRate = 50;
    
    final static int maxIrisPhase = 70; // ticks
    
    final static int firstCamouflageSlot = 0;
    final static int numCamouflageSlots = 5;
    final static int numInventorySlots = numCamouflageSlots;
    
    static float defaultChevronAngle = 40f;
    static float chevronAngles[][] = {
    //     0    1    2    <-- Base camouflage level
        { 45f, 45f, 40f }, // 7 chevrons
        { 36f, 33f, 30f }  // 9 chevrons
    };
    
    // Configuration options
    static double maxEnergyBuffer = 1000;
    static double energyPerFuelItem = 96000;
    static double distanceFactorMultiplier = 1.0;
    static double interDimensionMultiplier = 4.0;
    static int gateOpeningsPerFuelItem = 24;
    static int minutesOpenPerFuelItem = 80;
    static int secondsToStayOpen = 5 * 60;
    static boolean oneWayTravel = false;
    static boolean closeFromEitherEnd = true;
    static int chunkLoadingRange = 1;
    static boolean logStargateEvents = false;
    static boolean preserveInventory = false;
    static float soundVolume = 1F;
    static boolean variableChevronPositions = true;
    
    public static double energyToOpen;
    static double energyUsePerTick;
    static int ticksToStayOpen;
    public static boolean transparency = true;
    
    static Random random = new Random();
    static DamageSource transientDamage = new DamageSource("sgcraft:transient");
    
    public boolean isMerged;
    public SGState state = SGState.Idle;
    public double startRingAngle, ringAngle, lastRingAngle, targetRingAngle; // degrees
    public int numEngagedChevrons;
    public String dialledAddress = "";
    public boolean isLinkedToController;
    public BlockPos linkedPos = new BlockPos(0, 0, 0);
    public boolean hasChevronUpgrade;
    public boolean hasIrisUpgrade;
    public IrisState irisState = IrisState.Open;
    public int irisPhase = maxIrisPhase; // 0 = fully closed, maxIrisPhase = fully open
    public int lastIrisPhase = maxIrisPhase;
    public OCWirelessEndpoint ocWirelessEndpoint; //[OC]

    SGLocation connectedLocation;
    boolean isInitiator, redstoneInput, loaded;
    int timeout, maxTimeout;
    double energyInBuffer, distanceFactor; // all energy use is multiplied by this
    public String homeAddress, addressError;
    
//  public static final int firstFuelSlot = 0;
//  public static final int numFuelSlots = 4;
//  public static final int firstUpgradeSlot = 4;
//  public static final int numUpgradeSlots = 0;

    IInventory inventory = new InventoryBasic("Stargate", false, numInventorySlots);

    double ehGrid[][][];
    
    public static void configure(BaseConfiguration cfg) {
        energyPerFuelItem = cfg.getDouble("stargate", "energyPerFuelItem", energyPerFuelItem);
        gateOpeningsPerFuelItem = cfg.getInteger("stargate", "gateOpeningsPerFuelItem", gateOpeningsPerFuelItem);
        minutesOpenPerFuelItem = cfg.getInteger("stargate", "minutesOpenPerFuelItem", minutesOpenPerFuelItem);
        secondsToStayOpen = cfg.getInteger("stargate", "secondsToStayOpen", secondsToStayOpen);
        oneWayTravel = cfg.getBoolean("stargate", "oneWayTravel", oneWayTravel);
        closeFromEitherEnd = cfg.getBoolean("stargate", "closeFromEitherEnd", closeFromEitherEnd);
        //energyPerFuelItem = minutesOpenPerFuelItem * 60 * 20;
        //maxEnergyBuffer = 2 * energyPerFuelItem;
        maxEnergyBuffer = cfg.getDouble("stargate", "maxEnergyBuffer", maxEnergyBuffer);
        energyToOpen = energyPerFuelItem / gateOpeningsPerFuelItem;
        energyUsePerTick = energyPerFuelItem / (minutesOpenPerFuelItem * 60 * 20);
        distanceFactorMultiplier = cfg.getDouble("stargate", "distanceFactorMultiplier", distanceFactorMultiplier);
        interDimensionMultiplier = cfg.getDouble("stargate", "interDimensionMultiplier", interDimensionMultiplier);
        if (debugEnergyUse) {
            System.out.printf("SGBaseTE: energyPerFuelItem = %s\n", energyPerFuelItem);
            System.out.printf("SGBaseTE: energyToOpen = %s\n", energyToOpen);
            System.out.printf("SGBaseTE: energyUsePerTick = %s\n", energyUsePerTick);
        }
        ticksToStayOpen = 20 * secondsToStayOpen;
        chunkLoadingRange = cfg.getInteger("options", "chunkLoadingRange", chunkLoadingRange);
        //if (chunkLoadingRange < 0)
        //  chunkLoadingRange = 0;
        transparency = cfg.getBoolean("stargate", "transparency", transparency);
        logStargateEvents = cfg.getBoolean("options", "logStargateEvents", logStargateEvents);
        preserveInventory = cfg.getBoolean("iris", "preserveInventory", preserveInventory);
        soundVolume = (float)cfg.getDouble("stargate", "soundVolume", soundVolume);
        variableChevronPositions = cfg.getBoolean("stargate", "variableChevronPositions", variableChevronPositions);
    }
    
    public static SGBaseTE get(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SGBaseTE) {
            return (SGBaseTE)te;
        } else if (te instanceof SGRingTE) {
            return ((SGRingTE)te).getBaseTE();
        } else {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return String.format("SGBaseTE(%s,%s)", pos, world.provider.getDimension());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new AxisAlignedBB(x - 2, y, z - 2, x + 3, y + 5, z + 3);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 32768.0;
    }

    @Override
    public void onAddedToWorld() {
        if (SGBaseBlock.debugMerge)
            System.out.print("SGBaseTE.onAddedToWorld\n");
        updateChunkLoadingStatus();
    }

    @Override
    public float getSoundVolume(SoundEvent sound) {
        return soundVolume;
    }

    @Override
    public World getSoundWorld() {
        return this.world;
    }

    @Override
    public BlockPos getSoundPos() {
        return this.pos;
    }

    @Override
    public boolean isSoundActive(SoundEvent sound) {
        if (this.isInvalid()) {
            return false;
        }
        if (sound == gateRollSound) {
            return state == SGState.Dialling;
        } else if (sound == irisOpenSound) {
            return irisState == IrisState.Opening;
        } else if (sound == irisCloseSound) {
            return irisState == IrisState.Closing;
        } else {
            return false;
        }
    }
    
    void updateChunkLoadingStatus() {
        if (state != SGState.Idle) {
            int n = chunkLoadingRange;
            if (n >= 0) {
                SGCraft.chunkManager.setForcedChunkRange(this, -n, -n, n, n);
            }
        } else {
            SGCraft.chunkManager.clearForcedChunkRange(this);
        }
    }

    public static SGBaseTE at(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof SGBaseTE ? (SGBaseTE) te : null;
    }
    
    public static SGBaseTE at(SGLocation loc) {
        if (loc != null) {
            World world = SGAddressing.getWorld(loc.dimension);
            if (world != null) {
                return SGBaseTE.at(world, loc.pos);
            }
        }
        return null;
    }
    
    public static SGBaseTE at(IBlockAccess world, NBTTagCompound nbt) {
        BlockPos pos = new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
        return SGBaseTE.at(world, pos);
    }
    
    void setMerged(boolean state) {
        if (isMerged != state) {
            isMerged = state;
            markBlockChanged();
            if (logStargateEvents) {
                String address = tryToGetHomeAddress();
                if (address != null) {
                    Logger log = LogManager.getLogger();
                    String action = isMerged ? "ADDED" : "REMOVED";
                    String name = getWorld().getWorldInfo().getWorldName();
                    log.info(String.format("STARGATE %s %s %s %s", action, name, pos, address));
                }
            }
            updateIrisEntity();
        }
    }
    
    String tryToGetHomeAddress() {
        try {
            return getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            return null;
        }
    }

    public int dimension() {
        return world != null ? world.provider.getDimension() : -999;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        isMerged = nbt.getBoolean("isMerged");
        state = SGState.values()[nbt.getInteger("state")];
        ringAngle = nbt.getDouble("ringAngle");
        startRingAngle = nbt.getDouble("startRingAngle");
        targetRingAngle = nbt.getDouble("targetRingAngle");
        numEngagedChevrons = nbt.getInteger("numEngagedChevrons");
        dialledAddress = nbt.getString("dialledAddress");
        isLinkedToController = nbt.getBoolean("isLinkedToController");
        int x = nbt.getInteger("linkedX");
        int y = nbt.getInteger("linkedY");
        int z = nbt.getInteger("linkedZ");
        linkedPos = new BlockPos(x, y, z);
        hasChevronUpgrade = nbt.getBoolean("hasChevronUpgrade");
        if (nbt.hasKey("connectedLocation")) {
            connectedLocation = new SGLocation(nbt.getCompoundTag("connectedLocation"));
        }
        isInitiator = nbt.getBoolean("isInitiator");
        timeout = nbt.getInteger("timeout");
        maxTimeout = nbt.getInteger("maxTimeout");
        energyInBuffer = nbt.hasKey("energyInBuffer") ? nbt.getDouble("energyInBuffer") : nbt.getInteger("fuelBuffer");
        distanceFactor = nbt.getDouble("distanceFactor");
        hasIrisUpgrade = nbt.getBoolean("hasIrisUpgrade");
        irisState = IrisState.values()[nbt.getInteger("irisState")];
        irisPhase = nbt.getInteger("irisPhase");
        redstoneInput = nbt.getBoolean("redstoneInput");
        homeAddress = getStringOrNull(nbt, "address");
        addressError = nbt.getString("addressError");
    }
    
    protected String getStringOrNull(NBTTagCompound nbt, String name) {
        return nbt.hasKey(name) ? nbt.getString(name) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("isMerged", isMerged);
        nbt.setInteger("state", state.ordinal());
        nbt.setDouble("ringAngle", ringAngle);
        nbt.setDouble("startRingAngle", startRingAngle);
        nbt.setDouble("targetRingAngle", targetRingAngle);
        nbt.setInteger("numEngagedChevrons", numEngagedChevrons);
        //nbt.setString("homeAddress", homeAddress);
        nbt.setString("dialledAddress", dialledAddress);
        nbt.setBoolean("isLinkedToController", isLinkedToController);
        nbt.setInteger("linkedX", linkedPos.getX());
        nbt.setInteger("linkedY", linkedPos.getY());
        nbt.setInteger("linkedZ", linkedPos.getZ());
        nbt.setBoolean("hasChevronUpgrade", hasChevronUpgrade);
        if (connectedLocation != null)
            nbt.setTag("connectedLocation", connectedLocation.toNBT());
        nbt.setBoolean("isInitiator", isInitiator);
        nbt.setInteger("timeout", timeout);
        nbt.setInteger("maxTimeout", maxTimeout);
        nbt.setDouble("energyInBuffer", energyInBuffer);
        nbt.setDouble("distanceFactor", distanceFactor);
        nbt.setBoolean("hasIrisUpgrade", hasIrisUpgrade);
        nbt.setInteger("irisState", irisState.ordinal());
        nbt.setInteger("irisPhase", irisPhase);
        nbt.setBoolean("redstoneInput", redstoneInput);
        if (homeAddress != null)
            nbt.setString("address", homeAddress);
        if (addressError != null)
            nbt.setString("addressError", addressError);
        return nbt;
    }
    
    public boolean isActive() {
        return state != SGState.Idle && state != SGState.Disconnecting;
    }
    
    static boolean isValidSymbolChar(String c) {
        return SGAddressing.isValidSymbolChar(c);
    }
    
    static char symbolToChar(int i) {
        return SGAddressing.symbolToChar(i);
    }
    
    static int charToSymbol(char c) {
        return SGAddressing.charToSymbol(c);
    }

    static int charToSymbol(String c) {
        return SGAddressing.charToSymbol(c);
    }
    
    public EnumActionResult applyChevronUpgrade(ItemStack stack, EntityPlayer player) {
        if (!getWorld().isRemote && !hasChevronUpgrade && stack.getCount() > 0) {
            //System.out.printf("SGBaseTE.applyChevronUpgrade: Installing chevron upgrade\n");
            hasChevronUpgrade = true;
            stack.shrink(1);
            markChanged();
        }
        return EnumActionResult.SUCCESS;
    }
    
    public EnumActionResult applyIrisUpgrade(ItemStack stack, EntityPlayer player) {
        if (!getWorld().isRemote && !hasIrisUpgrade && stack.getCount() > 0) {
            //System.out.printf("SGBaseTE.applyIrisUpgrade: Installing iris upgrade\n");
            hasIrisUpgrade = true;
            stack.shrink(1);
            markChanged();
            updateIrisEntity();
        }
        return EnumActionResult.SUCCESS;
    }
    
    public int getNumChevrons() {
        //if (upgradePresent(SGCraft.sgChevronUpgrade))
        return hasChevronUpgrade ? 9 : 7;
    }
    
    public boolean chevronIsEngaged(int i) {
        return i < numEngagedChevrons;
    }

    public float angleBetweenChevrons() {
        if (variableChevronPositions) {
            int c9 = getNumChevrons() > 7 ? 1 : 0;
            int bc = baseCornerCamouflage();
            return chevronAngles[c9][bc];
        } else {
            return defaultChevronAngle;
        }
    }

    public String getHomeAddress() throws SGAddressing.AddressingError {
        return SGAddressing.addressForLocation(new SGLocation(this));
    }
    
    public SGBaseBlock getBlock() {
        return (SGBaseBlock)getBlockType();
    }
    
    public double interpolatedRingAngle(double partialTicks) {
        return isInitiator ? lastRingAngle + (ringAngle - lastRingAngle) * partialTicks : 0;
    }
    
    @Override
    public void update() {
        if (world.isRemote) {
            clientUpdate();
        } else {
            serverUpdate();
            checkForEntitiesInPortal();
        }
        irisUpdate();
    }
    
    @Override
    public void invalidate() {
        super.invalidate();
        if (!world.isRemote && ocWirelessEndpoint != null) {  //[OC]
            ocWirelessEndpoint.remove();
        }
    }
    
    String side() {
        return world.isRemote ? "Client" : "Server";
    }
    
    void enterState(SGState newState, int newTimeout) {
        if (debugState)
            System.out.printf("SGBaseTE: at %s in dimension %s entering state %s with timeout %s\n", 
                pos, world.provider.getDimension(), newState, newTimeout);
        SGState oldState = state;
        state = newState;
        startRingAngle = ringAngle;
        maxTimeout = newTimeout;
        timeout = newTimeout;
        markChanged();
        if ((oldState == SGState.Idle) != (newState == SGState.Idle)) {
            updateChunkLoadingStatus();
            world.notifyNeighborsOfStateChange(pos, getBlockType(), true);
            //Update: may not need observer update here.
        }
        String oldDesc = sgStateDescription(oldState);
        String newDesc = sgStateDescription(newState);
        if (!oldDesc.equals(newDesc))
            postEvent("sgStargateStateChange", newDesc, oldDesc);
    }
    
    public boolean isConnected() {
        return state == SGState.SyncAwait
            || state == SGState.Transient
            || state == SGState.Connected
            || state == SGState.Disconnecting;
    }
    
    DHDTE getLinkedControllerTE() {
        //System.out.printf("SGBaseTE.getLinkedControllerTE: isLinkedToController = %s, linkedPos = %s\n",
        //    isLinkedToController, linkedPos);
        if (isLinkedToController) {
            TileEntity cte = world.getTileEntity(linkedPos);
            if (cte instanceof DHDTE)
                return (DHDTE)cte;
        }
        return null;
    }
    
    void checkForLink() {
        int rangeXY = max(DHDTE.linkRangeX, DHDTE.linkRangeY);
        int rangeZ = DHDTE.linkRangeZ;
        if (SGBaseBlock.debugMerge)
            System.out.printf("SGBaseTE.checkForLink: in range +/-(%d,%d,%d) of %s\n",
                rangeXY, rangeZ, rangeXY, pos);
        for (int i = -rangeXY; i <= rangeXY; i++)
            for (int j = -rangeZ; j <= rangeZ; j++)
                for (int k = -rangeXY; k <= rangeXY; k++) {
                    TileEntity te = world.getTileEntity(pos.add(i, j, k));
                    if (te instanceof DHDTE)
                        ((DHDTE)te).checkForLink();
                }
    }

    public void unlinkFromController() {
        if (isLinkedToController) {
            DHDTE cte = getLinkedControllerTE();
            if (cte != null)
                cte.clearLinkToStargate();
            clearLinkToController();
        }
    }
    
    public void clearLinkToController() {
        if (SGBaseBlock.debugMerge)
            System.out.printf("SGBaseTE: Unlinking stargate at %d from controller\n", pos);
        isLinkedToController = false;
        markDirty();
    }

    //------------------------------------   Server   --------------------------------------------
    
    public void connectOrDisconnect(String address, EntityPlayer player) {
        if (debugConnect)
            System.out.printf("SGBaseTE: %s: connectOrDisconnect('%s') in state %s by %s\n",
                side(), address, state, player);
        if (address.length() > 0) {
            connect(address, player);
        } else {
            attemptToDisconnect(player);
        }
    }
    
    public String attemptToDisconnect(EntityPlayer player) {        
        boolean canDisconnect = disconnectionAllowed();
        SGBaseTE dte = getConnectedStargateTE();
        boolean validConnection =
            (dte != null) && !dte.isInvalid() && (dte.getConnectedStargateTE() == this);
        if (canDisconnect || !validConnection) {
            if (state != SGState.Disconnecting)
                disconnect();
                return null;
        } else {
            return operationFailure(player, "incomingConnection");
        }
    }
    
    public boolean disconnectionAllowed() {
        return isInitiator || closeFromEitherEnd;
    }
    
    String connect(String address, EntityPlayer player) {
        SGBaseTE dte;
        if (state != SGState.Idle)
            return diallingFailure(player, "selfBusy");
        String homeAddress = findHomeAddress();
        if (homeAddress.equals(""))
            return diallingFailure(player, "selfOutOfRange");
        try {
            dte = SGAddressing.findAddressedStargate(address, world);
        } catch (SGAddressing.AddressingError e) {
            return diallingFailure(player, e.getMessage());
        }
        if (dte == null || !dte.isMerged)
            return diallingFailure(player, "unknownAddress", address);
        if (getWorld() == dte.getWorld()) {
            address = SGAddressing.localAddress(address);
            homeAddress = SGAddressing.localAddress(homeAddress);
        }
        if (address.length() > getNumChevrons())
            return diallingFailure(player, "selfLackChevrons", address);
        if (dte == this)
            return diallingFailure(player, "diallingItself");
        if (debugConnect)
            System.out.printf("SGBaseTE.connect: to %s in dimension %d with state %s\n",
                dte.getPos(), dte.getWorld().provider.getDimension(), dte.state);
        if (dte.getNumChevrons() < homeAddress.length())
            return diallingFailure(player, "targetLackChevrons");
        if (dte.state != SGState.Idle)
            return diallingFailure(player, "targetBusy", address);
        distanceFactor = distanceFactorForCoordDifference(this, dte);
        if (debugEnergyUse)
            System.out.printf("SGBaseTE: distanceFactor = %s\n", distanceFactor);
        if (!energyIsAvailable(energyToOpen * distanceFactor))
            return diallingFailure(player, "insufficientEnergy");
        startDiallingStargate(address, dte, true);
        dte.startDiallingStargate(homeAddress, this, false);
        return null;
    }
    
    public static double distanceFactorForCoordDifference(TileEntity te1, TileEntity te2) {
        double dx = te1.getPos().getX() - te2.getPos().getX();
        double dz = te1.getPos().getZ() - te2.getPos().getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (debugEnergyUse)
            System.out.printf("SGBaseTE: Connection distance = %s\n", d);
        double ld = Math.log(0.05 * d + 1);
        double lm = Math.log(0.05 * 16 * SGAddressing.coordRange);
        double lr = ld / lm;
        double f = 1 + 14 * distanceFactorMultiplier * lr * lr;
        if (te1.getWorld() != te2.getWorld())
            f *= interDimensionMultiplier;
        return f;
    }
    
    public void playSGSoundEffect(SoundEvent se, float volume, float pitch) {
        playSoundEffect(se, volume * soundVolume, pitch);
    }

    public String diallingFailure(EntityPlayer player, String msg, Object... args) {
        if (player != null) {
            if (state == SGState.Idle)
                playSGSoundEffect(dialFailSound, 1F, 1F);
        }
        return operationFailure(player, msg, args);
    }

    public String operationFailure(EntityPlayer player, String msg, Object... args) {
        if (player != null)
            sendErrorMsg(player, msg, args);
        return msg;
    }

    public static void sendErrorMsg(EntityPlayer player, String msg, Object... args) {
        ITextComponent component = new TextComponentTranslation("message.sgcraft:" + msg, args);
        component.getStyle().setColor(TextFormatting.RED);
        player.sendMessage(component);
    }
    
    String findHomeAddress() {
        try {
            return getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            //System.out.printf("SGBaseTE.findHomeAddress: %s\n", e);
            return "";
        }
    }
    
    public void disconnect() {
        if (debugConnect)
            System.out.printf("SGBaseTE: %s: disconnect()\n", side());
        SGBaseTE dte = SGBaseTE.at(connectedLocation);
        if (dte != null)
            dte.clearConnection();
        clearConnection();
    }
    
    public void clearConnection() {
        if (state != SGState.Idle || connectedLocation != null) {
            dialledAddress = "";
            connectedLocation = null;
            isInitiator = false;
            markChanged();
            if (state == SGState.Connected) {
                enterState(SGState.Disconnecting, disconnectTime);
                //sendClientEvent(SGEvent.StartDisconnecting, 0);
                playSGSoundEffect(disconnectSound, 1F, 1F);
            } else {
                numEngagedChevrons = 0;
                if (state != SGState.Idle && state != SGState.Disconnecting)
                    playSGSoundEffect(dialFailSound, 1F, 1F);
                enterState(SGState.Idle, 0);
                //sendClientEvent(SGEvent.FinishDisconnecting, 0);
            }
        }
    }
    
    void startDiallingStargate(String address, SGBaseTE dte, boolean initiator) {
        //System.out.printf("SGBaseTE.startDiallingStargate %s, initiator = %s\n",
        //  dte, initiator);
        dialledAddress = address;
        connectedLocation = new SGLocation(dte);
        isInitiator = initiator;
        markDirty();
        if (isInitiator) {
            startDiallingNextSymbol();
        }
        postEvent(initiator ? "sgDialOut" : "sgDialIn", address);
    }

    void serverUpdate() {
        if (!loaded) {
            loaded = true;
            try {
                homeAddress = getHomeAddress();
                addressError = "";
            } catch (SGAddressing.AddressingError e) {
                homeAddress = null;
                addressError = e.getMessage();
            }
            if (SGCraft.ocIntegration != null) { //[OC]
                SGCraft.ocIntegration.onSGBaseTEAdded(this);
            }
        }
        if (isMerged) {
            if (debugState && state != SGState.Connected && timeout > 0) {
                int dimension = world.provider.getDimension();
                System.out.printf("SGBaseTE.serverUpdate at %s in dimension %d: state %s, timeout %s\n", pos, dimension, state, timeout);
            }
            tickEnergyUsage();
            if (timeout > 0) {
                switch (state) {
                    case Transient:
                        if (!irisIsClosed()) {
                            performTransientDamage();
                        }
                        break;
                    case Dialling:
                        double step = (double)(maxTimeout - timeout) / (double)maxTimeout;
                        ringAngle = startRingAngle + (targetRingAngle - startRingAngle) * step;
                        break;
                }
                --timeout;
            } else {
                switch(state) {
                    case Idle:
                        if (symbolsRemaining() && isInitiator) {
                            startDiallingNextSymbol();
                        }
                        break;
                    case Dialling:
                        if (isInitiator) {
                            finishDiallingSymbol();
                            SGBaseTE targetGate = SGBaseTE.at(connectedLocation);
                            targetGate.finishDiallingSymbol();
                        }
                        break;
                    case InterDialling:
                        if (isInitiator) {
                            startDiallingNextSymbol();
                        }
                        break;
                    case SyncAwait:
                        finishDiallingAddress();
                        break;
                    case Transient:
                        enterState(SGState.Connected, isInitiator ? ticksToStayOpen : 0);
                        break;
                    case Connected:
                        if (isInitiator && ticksToStayOpen > 0) {
                            disconnect();
                        }
                        break;
                    case Disconnecting:
                        numEngagedChevrons = 0;
                        enterState(SGState.Idle, 0);
                        break;
                }
            }
        }
    }
    
    void tickEnergyUsage() {
        if (state == SGState.Connected && isInitiator) {
            if (!useEnergy(energyUsePerTick * distanceFactor)) {
                disconnect();
            }
        }
    }
    
    double availableEnergy() {
        List<ISGEnergySource> sources = findEnergySources();
        return energyInBuffer + energyAvailableFrom(sources);
    }

    boolean energyIsAvailable(double amount) {
        double energy = availableEnergy();
        if (debugEnergyUse)
            System.out.printf("SGBaseTE.energyIsAvailable: need %s, have %s\n", amount, energy);
        return energy >= amount;
    }
    
    boolean useEnergy(double amount) {
        if (debugEnergyUse)
            System.out.printf("SGBaseTE.useEnergy: %s; buffered: %s\n", amount, energyInBuffer);
        if (amount <= energyInBuffer) {
            energyInBuffer -= amount;
            return true;
        }
        List<ISGEnergySource> sources = findEnergySources();
        double energyAvailable = energyInBuffer + energyAvailableFrom(sources);
        if (debugEnergyUse)
            System.out.printf("SGBaseTE.useEnergy: %s available\n", energyAvailable);
        if (amount > energyAvailable) {
            System.out.print("SGBaseTE: Not enough energy available\n");
            return false;
        }
        double desiredEnergy = max(amount, maxEnergyBuffer);
        double targetEnergy = min(desiredEnergy, energyAvailable);
        double energyRequired = targetEnergy - energyInBuffer;
        if (debugEnergyUse)
            System.out.printf("SGBaseTE.useEnergy: another %s required\n", energyRequired);
        double energyOnHand = energyInBuffer + drawEnergyFrom(sources, energyRequired);
        if (debugEnergyUse)
            System.out.printf("SGBaseTE.useEnergy: %s now on hand, need %s\n", energyOnHand, amount);
        if (amount - 0.0001 > energyOnHand) {
            System.out.printf("SGBaseTE: Energy sources only delivered %s of promised %s\n",
                energyOnHand - energyInBuffer, energyAvailable);
            return false;
        }
        setEnergyInBuffer(energyOnHand - amount);
        if (debugEnergyUse)
            System.out.printf("SGBaseTE.useEnergy: %s left over in buffer\n", energyInBuffer);
        return true;
    }
    
    List<ISGEnergySource> findEnergySources() {
//         System.out.printf("SGBaseTe.findEnergySources: for %s\n", getSoundPos());
        List<ISGEnergySource> result = new ArrayList<>();
        Trans3 t = localToGlobalTransformation();
        for (int i = -2; i <= 2; i++) {
            BlockPos bp = t.p(i, -1, 0).blockPos();
            TileEntity nte = world.getTileEntity(bp);
//             System.out.printf("SGBaseTE.findEnergySources: %s at %s\n", nte, bp);
            if (nte instanceof ISGEnergySource)
                result.add((ISGEnergySource)nte);
        }
        DHDTE te = getLinkedControllerTE();
        if (te != null)
            result.add(te);
        return result;
    }
    
    double energyAvailableFrom(List<ISGEnergySource> sources) {
        double energy = 0;
        for (ISGEnergySource source : sources) {
            double e = source.availableEnergy();
            if (debugEnergyUse)
                System.out.printf("SGBaseTe.energyAvailableFrom: %s can supply %s\n", source, e);
            energy += e;
        }
        return energy;
    }
    
    double drawEnergyFrom(List<ISGEnergySource> sources, double amount) {
        double total = 0;
        for (ISGEnergySource source : sources) {
            if (total >= amount)
                break;
            double e = source.drawEnergy(amount - total);
            if (debugEnergyUse)
                System.out.printf("SGBaseTe.drawEnergyFrom: %s supplied %s\n", source, e);
            total += e;
        }
        if (total < amount)
            System.out.printf("SGCraft: Warning: Energy sources did not deliver promised energy " +
                "(%s requested, %s delivered)\n", amount, total);
        return total;
    }
    
    void setEnergyInBuffer(double amount) {
        if (energyInBuffer != amount) {
            energyInBuffer = amount;
            markDirty();
        }
    }
    
    void performTransientDamage() {
        Trans3 t = localToGlobalTransformation();
        Vector3 p0 = t.p(-1.5, 0.5, 0.5);
        Vector3 p1 = t.p(1.5, 3.5, 5.5);
        Vector3 q0 = p0.min(p1);
        Vector3 q1 = p0.max(p1);
        AxisAlignedBB box = new AxisAlignedBB(q0.x, q0.y, q0.z, q1.x, q1.y, q1.z);
        if (debugTransientDamage) {
            System.out.print("SGBaseTE.performTransientDamage: players in world:\n");
            for (Entity ent : world.loadedEntityList)
                if (ent instanceof EntityPlayer)
                    System.out.printf("--- %s\n", ent);
            System.out.printf("SGBaseTE.performTransientDamage: box = %s\n", box);
        }
        List<EntityLivingBase> ents = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (EntityLivingBase ent : ents) {
            Vector3 ep = new Vector3(ent.posX, ent.posY, ent.posZ);
            Vector3 gp = t.p(0, 2, 0.5);
            double dist = ep.distance(gp);
            if (debugTransientDamage)
                System.out.printf("SGBaseTE.performTransientDamage: found %s\n", ent);
            if (dist > 1.0)
                dist = 1.0;
            int damage = (int)Math.ceil(dist * transientDamageRate);
            if (debugTransientDamage)
                System.out.printf("SGBaseTE.performTransientDamage: distance = %s, damage = %s\n",
                    dist, damage);
            ent.attackEntityFrom(transientDamage, damage);
        }
    }
    
    boolean symbolsRemaining() {
        int n = numEngagedChevrons;
        return n < dialledAddress.length();
    }
    
    void startDiallingNextSymbol() {
        if (debugState)
            System.out.printf("SGBaseTE.startDiallingNextSymbol: %s of %s\n", numEngagedChevrons, dialledAddress);
        startDiallingSymbol(dialledAddress.charAt(numEngagedChevrons));
    }
    
    void startDiallingSymbol(char c) {
        int i = SGAddressing.charToSymbol(c);
        if (debugState)
            System.out.printf("SGBaseTE.startDiallingSymbol: %s\n", i);
        if (i >= 0 && i < numRingSymbols) {
            double targetAngle = i * ringSymbolAngle;
            double diff = targetAngle - ringAngle;
            if (Math.abs(diff) < 180) {
                targetAngle -= Math.copySign(360.0, ringAngle);
                diff = targetAngle - ringAngle;
            }
            int delay = (int)Math.abs(diff / ringRotationSpeed);
            targetRingAngle = targetAngle;
            //System.out.println(homeAddress + " -> Delay: " + delay + " (From angle " + ringAngle + " to angle " + targetAngle + ")");
            enterState(SGState.Dialling, delay);
        } else {
            System.out.printf("SGCraft: Stargate jammed trying to dial symbol %s\n", c);
            dialledAddress = "";
            enterState(SGState.Idle, 0);
        }
    }
    
    void finishDiallingSymbol() {
        ++numEngagedChevrons;
        String symbol = dialledAddress.substring(numEngagedChevrons - 1, numEngagedChevrons);
        postEvent("sgChevronEngaged", numEngagedChevrons, symbol);
        if (symbolsRemaining()) {
            enterState(SGState.InterDialling, interDiallingTime);
            playSGSoundEffect(isInitiator ? chevronOutgoingSound : chevronIncomingSound, 1F, 1F);
        } else {
            enterState(SGState.SyncAwait, syncAwaitTime);
            playSGSoundEffect(isInitiator ? lockOutgoingSound : lockIncomingSound, 1F, 1F);
            playSGSoundEffect(connectSound, 1F, 1F);
        }
    }
    
    void finishDiallingAddress() {
        //System.out.printf("SGBaseTE: Connecting to '%s'\n", dialledAddress);
        if (!isInitiator || useEnergy(energyToOpen * distanceFactor)) {
            enterState(SGState.Transient, transientDuration);
        } else {
            disconnect();
        }
    }
    
    boolean canTravelFromThisEnd() {
        return isInitiator || !oneWayTravel;
    }
    
    static String repr(Entity entity) {
        if (entity != null) {
            String s = String.format("%s#%s", entity.getClass().getSimpleName(), entity.getEntityId());
            if (entity.isDead)
                s += "(dead)";
            return s;
        } else {
            return "null";
        }
    }
    
    class TrackedEntity {
        public Entity entity;
        public Vector3 lastPos;
        
        public TrackedEntity(Entity entity) {
            this.entity = entity;
            this.lastPos = new Vector3(entity.posX, entity.posY, entity.posZ);
        }
    }

    List<TrackedEntity> trackedEntities = new ArrayList<>();
    
    void checkForEntitiesInPortal() {
        if (state == SGState.Connected) {
            for (TrackedEntity trk : trackedEntities)
                entityInPortal(trk.entity, trk.lastPos);
            trackedEntities.clear();
            Vector3 p0 = new Vector3(-1.5, 0.5, -3.5);
            Vector3 p1 = new Vector3(1.5, 3.5, 3.5);
            Trans3 t = localToGlobalTransformation();
            AxisAlignedBB box = t.box(p0, p1);
            //System.out.printf("SGBaseTE.checkForEntitiesInPortal: %s\n", box);
            List<Entity> ents = world.getEntitiesWithinAABB(Entity.class, box);
            for (Entity entity : ents) {
                if (entity instanceof EntityFishHook)
                    continue;
                if (!entity.isDead && entity.getRidingEntity() == null) {
                    //if (!(entity instanceof EntityPlayer))
                    //  System.out.printf("SGBaseTE.checkForEntitiesInPortal: Tracking %s\n", repr(entity));
                    trackedEntities.add(new TrackedEntity(entity));
                }
            }
        } else {
            trackedEntities.clear();
        }
    }

    public void entityInPortal(Entity entity, Vector3 prevPos) {
        if (!entity.isDead && state == SGState.Connected && canTravelFromThisEnd()) {
            Trans3 t = localToGlobalTransformation();
            double vx = entity.posX - prevPos.x;
            double vy = entity.posY - prevPos.y;
            double vz = entity.posZ - prevPos.z;
            Vector3 p1 = t.ip(entity.posX, entity.posY, entity.posZ);
            Vector3 p0 = t.ip(2 * prevPos.x - entity.posX, 2 * prevPos.y - entity.posY, 2 * prevPos.z - entity.posZ);
            //if (!(entity instanceof EntityPlayer))
            //  System.out.printf("SGBaseTE.entityInPortal: z0 = %.3f z1 = %.3f\n", p0.z, p1.z);
            double z0 = 0.0;
            if (p0.z >= z0 && p1.z < z0 && p1.z > z0 - 5.0) {
                //System.out.printf("SGBaseTE.entityInPortal: %s passed through event horizon of stargate at (%d,%d,%d) in %s\n",
                //  repr(entity), xCoord, yCoord, zCoord, world);
                entity.motionX = vx;
                entity.motionY = vy;
                entity.motionZ = vz;
                //System.out.printf("SGBaseTE.entityInPortal: %s pos (%.2f, %.2f, %.2f) prev (%.2f, %.2f, %.2f) motion (%.2f, %.2f, %.2f)\n",
                //  repr(entity),
                //  entity.posX, entity.posY, entity.posZ,
                //  prevPos.x, prevPos.y, prevPos.z,
                //  entity.motionX, entity.motionY, entity.motionZ);
                SGBaseTE dte = getConnectedStargateTE();
                if (dte != null) {
                    Trans3 dt = dte.localToGlobalTransformation();
                    while (entity.getRidingEntity() != null)
                        entity = entity.getRidingEntity();
                    teleportEntityAndRiders(entity, t, dt, connectedLocation.dimension, dte.irisIsClosed());
                }
            }
        }
    }
    
    Entity teleportEntityAndRiders(Entity entity, Trans3 t1, Trans3 t2, int dimension, boolean destBlocked) {
        if (debugTeleport)
            System.out.printf("SGBaseTE.teleportEntityAndRiders: destBlocked = %s\n", destBlocked);
        //Entity rider = entity.riddenByEntity;
        List<Entity> riders = entity.getPassengers();
        for (int i = 0; i < riders.size(); i++) {
            Entity rider = riders.get(i);
            //System.out.printf("SGBaseTE.teleportEntityAndRider: Unmounting %s from %s\n",
            //  repr(rider), repr(entity));
            rider.dismountRidingEntity();
            rider = teleportEntityAndRiders(rider, t1, t2, dimension, destBlocked);
            riders.set(i, rider);
        }
        unleashEntity(entity);
        entity = teleportEntity(entity, t1, t2, dimension, destBlocked);
        if (entity != null && !entity.isDead) {
            for (Entity rider : riders) {
                if (rider != null && !rider.isDead) {
                    //System.out.printf("SGBaseTE.teleportEntityAndRider: Mounting %s on %s\n",
                    //  repr(rider), repr(entity));
                    rider.startRiding(entity, true);
                }
            }
        }
        return entity;
    }
    
    // Break any leash connections to or from the given entity. That happens anyway
    // when the entity is teleported, but without this it drops an extra leash item.
    protected static void unleashEntity(Entity entity) {
        if (entity instanceof EntityLiving)
            ((EntityLiving)entity).clearLeashed(true, false);
        for (EntityLiving entity2 : entitiesWithinLeashRange(entity))
            if (entity2.getLeashed() && entity2.getLeashHolder() == entity)
                entity2.clearLeashed(true, false);
    }
    
    protected static List<EntityLiving> entitiesWithinLeashRange(Entity entity) {
        AxisAlignedBB box = new AxisAlignedBB(
            entity.posX - 7.0D, entity.posY - 7.0D, entity.posZ - 7.0D,
            entity.posX + 7.0D, entity.posY + 7.0D, entity.posZ + 7.0D);
        return entity.world.getEntitiesWithinAABB(EntityLiving.class, box);
    }

    static Entity teleportEntity(Entity entity, Trans3 t1, Trans3 t2, int dimension, boolean destBlocked) {
        Entity newEntity = null;
        if (debugTeleport) {
            System.out.printf("SGBaseTE.teleportEntity: %s (in dimension %d)  to dimension %d\n",
              repr(entity), entity.dimension, dimension);
            System.out.printf("SGBaseTE.teleportEntity: pos (%.2f, %.2f, %.2f) prev (%.2f, %.2f, %.2f) last (%.2f, %.2f, %.2f) pitch %.2f yaw %.2f\n",
              entity.posX, entity.posY, entity.posZ,
              entity.prevPosX, entity.prevPosY, entity.prevPosZ,
              entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ,
              entity.rotationPitch, entity.rotationYaw);
        }
        Vector3 p = t1.ip(entity.posX, entity.posY, entity.posZ); // local position
        Vector3 v = t1.iv(entity.motionX, entity.motionY, entity.motionZ); // local velocity
        Vector3 r = t1.iv(yawVector(entity)); // local facing
        Vector3 q = t2.p(-p.x, p.y, -p.z); // new global position
        Vector3 u = t2.v(-v.x, v.y, -v.z); // new global velocity
        Vector3 s = t2.v(r.mul(-1)); // new global facing
        if (debugTeleport)
            System.out.printf("SGBaseTE.teleportEntity: Facing old %s new %s\n", r, s);
        double a = yawAngle(s, entity); // new global yaw angle
        if (debugTeleport)
            System.out.printf("SGBaseTE.teleportEntity: new yaw %.2f\n", a);
        if (!destBlocked) {
            if (entity.dimension == dimension)
                newEntity = teleportWithinDimension(entity, q, u, a, destBlocked);
            else {
                newEntity = teleportToOtherDimension(entity, q, u, a, dimension, destBlocked);
                if (newEntity != null)
                    newEntity.dimension = dimension;
                //else
                //  System.out.printf("SGBaseTE.teleportEntity: teleportToOtherDimension returned null for %s\n",
                //      entity);
            }
            //if (entity != newEntity)
            //  System.out.printf("SGBaseTE.teleportEntity: %s is now %s\n", repr(entity), repr(newEntity));
        }
        else {
            terminateEntityByIrisImpact(entity);
            playIrisHitSound(worldForDimension(dimension), q, entity);  
        }
        return newEntity;
    }
    
    static void terminateEntityByIrisImpact(Entity entity) {
        if (entity instanceof EntityPlayer) {
            terminatePlayerByIrisImpact((EntityPlayer)entity);
        } else {
            entity.setDead();
        }
    }
    
    static void terminatePlayerByIrisImpact(EntityPlayer player) {
        if (player.capabilities.isCreativeMode)
            sendErrorMsg(player, "irisAtDestination");
        else {
            if (!(preserveInventory || player.world.getGameRules().getBoolean("keepInventory")))
                player.inventory.clear();
            player.attackEntityFrom(irisDamageSource, irisDamageAmount);
        }
    }

    static WorldServer worldForDimension(int dimension) {
        return SGAddressing.getWorld(dimension);
    }

    static void playIrisHitSound(World world, Vector3 pos, Entity entity) {
        double volume = min(entity.width * entity.height, 1.0);
        double pitch = 2.0 - volume;
        if (debugTeleport)
            System.out.printf("SGBaseTE.playIrisHitSound: at (%.3f,%.3f,%.3f) volume %.3f pitch %.3f\n", pos.x, pos.y, pos.z, volume, pitch);
        world.playSound(pos.x, pos.y, pos.z, irisHitSound, SoundCategory.NEUTRAL, (float)volume, (float)pitch, false);
    }
    
    static Entity teleportWithinDimension(Entity entity, Vector3 p, Vector3 v, double a, boolean destBlocked) {
        if (entity instanceof EntityPlayerMP)
            return teleportPlayerWithinDimension((EntityPlayerMP)entity, p, v, a);
        else
            return teleportEntityToWorld(entity, p, v, a, (WorldServer)entity.world, destBlocked);
    }
    
    static Entity teleportPlayerWithinDimension(EntityPlayerMP entity, Vector3 p, Vector3 v, double a) {
        entity.rotationYaw = (float)a;
        entity.setPositionAndUpdate(p.x, p.y, p.z);
        entity.world.updateEntityWithOptionalForce(entity, false);
        return entity;
    }

    static Entity teleportToOtherDimension(Entity entity, Vector3 p, Vector3 v, double a, int dimension, boolean destBlocked) {
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP)entity;
            Vector3 q = p.add(yawVector(a));
            transferPlayerToDimension(player, dimension, q, a);
            return player;
        } else {
            return teleportEntityToDimension(entity, p, v, a, dimension, destBlocked);
        }
    }
    
    static void sendDimensionRegister(EntityPlayerMP player, int dimensionID) {
        DimensionType providerID = DimensionManager.getProviderType(dimensionID);
        ForgeMessage msg = new ForgeMessage.DimensionRegisterMessage(dimensionID, providerID.toString());
        FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel("FORGE", Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
    
    
    static void transferPlayerToDimension(EntityPlayerMP player, int newDimension, Vector3 p, double a) {
        //System.out.printf("SGBaseTE.transferPlayerToDimension: %s to dimension %d\n", repr(player), newDimension);
        MinecraftServer server = BaseUtils.getMinecraftServer();
        PlayerList scm = server.getPlayerList();
        int oldDimension = player.dimension;
        player.dimension = newDimension;
        WorldServer oldWorld = server.getWorld(oldDimension);
        WorldServer newWorld = server.getWorld(newDimension);
        //System.out.printf("SGBaseTE.transferPlayerToDimension: %s with %s\n", newWorld, newWorld.getEntityTracker());
        // <<< Fix for MCPC+
        // -- Is this still necessary now that we are calling firePlayerChangedDimensionEvent?
        // -- Yes, apparently it is.
        sendDimensionRegister(player, newDimension);
        // >>>
        player.closeScreen();
        player.connection.sendPacket(new SPacketRespawn(player.dimension,
            player.world.getDifficulty(), newWorld.getWorldInfo().getTerrainType(),
            player.interactionManager.getGameType()));
//         if (SGCraft.mystcraftIntegration != null) //[MYST]
//             SGCraft.mystcraftIntegration.sendAgeData(newWorld, player);
        oldWorld.removeEntityDangerously(player); // Removes player right now instead of waiting for next tick
        player.isDead = false;
        player.setLocationAndAngles(p.x, p.y, p.z, (float)a, player.rotationPitch);
        newWorld.spawnEntity(player);
        player.setWorld(newWorld);
        scm.preparePlayer(player, oldWorld);
        player.connection.setPlayerLocation(p.x, p.y, p.z, (float)a, player.rotationPitch);
        player.interactionManager.setWorld(newWorld);
        scm.updateTimeAndWeatherForPlayer(player, newWorld);
        scm.syncPlayerInventory(player);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), effect));
        }
        player.connection.sendPacket(new SPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDimension, newDimension);
        //System.out.printf("SGBaseTE.transferPlayerToDimension: Transferred %s\n", repr(player));
    }   
    
    static Entity teleportEntityToDimension(Entity entity, Vector3 p, Vector3 v, double a, int dimension, boolean destBlocked) {
        //System.out.printf("SGBaseTE.teleportEntityToDimension: %s to dimension %d\n", repr(entity), dimension);
        MinecraftServer server = BaseUtils.getMinecraftServer();
        WorldServer world = server.getWorld(dimension);
        return teleportEntityToWorld(entity, p, v, a, world, destBlocked);
    }
    
    static Entity teleportEntityToWorld(Entity oldEntity, Vector3 p, Vector3 v, double a, WorldServer newWorld, boolean destBlocked) {
        if (debugTeleport)
            System.out.printf("SGBaseTE.teleportEntityToWorld: %s to %s, destBlocked = %s\n", repr(oldEntity), newWorld, destBlocked);
        WorldServer oldWorld = (WorldServer)oldEntity.world;
        NBTTagCompound nbt = new NBTTagCompound();
        oldEntity.writeToNBT(nbt);
        extractEntityFromWorld(oldWorld, oldEntity);
        if (destBlocked) {
            if (!(oldEntity instanceof EntityLivingBase))
                return null;
        }
        Entity newEntity = instantiateEntityFromNBT(oldEntity.getClass(), nbt, newWorld);
        if (newEntity != null) {
            if (oldEntity instanceof EntityLiving)
                copyMoreEntityData((EntityLiving)oldEntity, (EntityLiving)newEntity);
            setVelocity(newEntity, v);
            newEntity.setLocationAndAngles(p.x, p.y, p.z, (float)a, oldEntity.rotationPitch);
            checkChunk(newWorld, newEntity);
            //System.out.printf("SGBaseTE.teleportEntityToWorld: Spawning %s in %s\n", repr(newEntity), newWorld);
            newEntity.forceSpawn = true; // Force spawn packet to be sent as soon as possible
            newWorld.spawnEntity(newEntity);
            newEntity.setWorld(newWorld);
            if (debugTeleport)
                System.out.printf(
                  "SGBaseTE.teleportEntityToWorld: Spawned %s pos (%.2f, %.2f, %.2f) vel (%.2f, %.2f, %.2f) pitch %.2f (%.2f) yaw %.2f (%.2f)\n",
                  repr(newEntity),
                  newEntity.posX, newEntity.posY, newEntity.posZ,
                  newEntity.motionX, newEntity.motionY, newEntity.motionZ,
                  newEntity.rotationPitch, newEntity.prevRotationPitch,
                  newEntity.rotationYaw, newEntity.prevRotationYaw);
        }
        oldWorld.resetUpdateEntityTick();
        if (oldWorld != newWorld)
            newWorld.resetUpdateEntityTick();
        return newEntity;
    }
    
    static Entity instantiateEntityFromNBT(Class cls, NBTTagCompound nbt, WorldServer world) {
        try {
            Entity entity = (Entity)cls.getConstructor(World.class).newInstance(world);
            entity.readFromNBT(nbt);
            return entity;
        } catch (Exception e) {
            System.out.printf("SGCraft: SGBaseTE.instantiateEntityFromNBT: Could not instantiate %s: %s\n", cls, e);
            e.printStackTrace();
            return null;
        }
    }

    static void copyMoreEntityData(EntityLiving oldEntity, EntityLiving newEntity) {
        float s = oldEntity.getAIMoveSpeed();
        if (s != 0)
            newEntity.setAIMoveSpeed(s);
    }

    static void setVelocity(Entity entity, Vector3 v) {
        entity.motionX = v.x;
        entity.motionY = v.y;
        entity.motionZ = v.z;
    }
    
    static void extractEntityFromWorld(World world, Entity entity) {
        // Immediately remove entity from world without calling setDead(), which has
        // undesirable side effects on some entities.
        if (entity instanceof EntityPlayer) {
            world.playerEntities.remove(entity);
            world.updateAllPlayersSleepingFlag();
        }
        int i = entity.chunkCoordX;
        int j = entity.chunkCoordZ;
        if (entity.addedToChunk && ((ChunkProviderServer)world.getChunkProvider()).chunkExists(i, j))
            world.getChunkFromChunkCoords(i, j).removeEntity(entity);
        world.loadedEntityList.remove(entity);
        //BaseReflectionUtils.call(world, onEntityRemoved, entity);
        world.onEntityRemoved(entity);
    }
    
    static void checkChunk(World world, Entity entity) {
        int cx = MathHelper.floor(entity.posX / 16.0D);
        int cy = MathHelper.floor(entity.posZ / 16.0D);
        Chunk chunk = world.getChunkFromChunkCoords(cx, cy);
    }
    
    protected static int yawSign(Entity entity) {
        return entity instanceof EntityArrow ? -1 : 1;
    }
    
    static Vector3 yawVector(Entity entity) {
        return yawVector(yawSign(entity) * entity.rotationYaw);
    }
    
    static Vector3 yawVector(double yaw) {
        double a = Math.toRadians(yaw);
        Vector3 v = new Vector3(-Math.sin(a), 0, Math.cos(a));
        //System.out.printf("SGBaseTE.yawVector: %.2f --> (%.3f, %.3f)\n", yaw, v.x, v.z);
        return v;
    }
    
    static double yawAngle(Vector3 v, Entity entity) {
        double a = Math.atan2(-v.x, v.z);
        double d = Math.toDegrees(a);
        //System.out.printf("SGBaseTE.yawAngle: (%.3f, %.3f) --> %.2f\n", v.x, v.z, d);
        return yawSign(entity) * d;
    }
    
    public SGBaseTE getConnectedStargateTE() {
        return isConnected() && connectedLocation != null ? connectedLocation.getStargateTE() : null;
    }
    
    //------------------------------------   Client   --------------------------------------------

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        //System.out.printf("SGBaseTE.onDataPacket: with state %s numEngagedChevrons %s\n",
        //  SGState.valueOf(pkt.customParam1.getInteger("state")),
        //  pkt.customParam1.getInteger("numEngagedChevrons"));
        IrisState oldIrisState = irisState;
        SGState oldState = state;
        super.onDataPacket(net, pkt);
        if (isMerged) {
            if (state != oldState) {
                switch (state) {
                    case Transient:
                        initiateOpeningTransient();
                        break;
                    case Disconnecting:
                        initiateClosingTransient();
                        break;
                    case Dialling:
                        if (isInitiator) {
                            if (timeout > 0) {
                                SGCraft.playSound(this, gateRollSound);
                            }
                        }
                        break;
                }
            }
            if (irisState != oldIrisState) {
                switch (irisState) {
                    case Opening:
                        SGCraft.playSound(this, irisOpenSound);
                        break;
                    case Closing:
                        SGCraft.playSound(this, irisCloseSound);
                        break;
                }
            }
        }
    }
    
    void clientUpdate() {
        lastRingAngle = ringAngle;
        switch (state) {
            case Dialling:
                if (timeout > 0) {
                    double step = (double)(maxTimeout - timeout) / (double)maxTimeout;
                    ringAngle = startRingAngle + (targetRingAngle - startRingAngle) * step;
                    --timeout;
                }
                break;
            case Transient:
            case Connected:
            case Disconnecting:
                applyRandomImpulse();
                updateEventHorizon();
                break;
        }
    }
    
    public double[][][] getEventHorizonGrid() {
        if (ehGrid == null) {
            int m = SGBaseTERenderer.ehGridRadialSize;
            int n = SGBaseTERenderer.ehGridPolarSize;
            ehGrid = new double[2][n + 2][m + 1];
            for (int i = 0; i < 2; i++) {
                ehGrid[i][0] = ehGrid[i][n];
                ehGrid[i][n + 1] = ehGrid[i][1];
            }
        }
        return ehGrid;
    }
    
    void initiateOpeningTransient() {
        double v[][] = getEventHorizonGrid()[1];
        int n = SGBaseTERenderer.ehGridPolarSize;
        for (int j = 0; j <= n+1; j++) {
            v[j][0] = openingTransientIntensity;
            v[j][1] = v[j][0] + openingTransientRandomness * random.nextGaussian();
        }
    }
    
    void initiateClosingTransient() {
        numEngagedChevrons = 0;
        double v[][] = getEventHorizonGrid()[1];
        int m = SGBaseTERenderer.ehGridRadialSize;
        int n = SGBaseTERenderer.ehGridPolarSize;
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++)
                v[j][i] += closingTransientRandomness * random.nextGaussian();
    }
    
    void applyRandomImpulse() {
        double v[][] = getEventHorizonGrid()[1];
        int m = SGBaseTERenderer.ehGridRadialSize;
        int n = SGBaseTERenderer.ehGridPolarSize;
        int i = random.nextInt(m - 1) + 1;
        int j = random.nextInt(n) + 1;
        v[j][i] += 0.05 * random.nextGaussian();
    }
    
    void updateEventHorizon() {
        double grid[][][] = getEventHorizonGrid();
        double u[][] = grid[0];
        double v[][] = grid[1];
        int m = SGBaseTERenderer.ehGridRadialSize;
        int n = SGBaseTERenderer.ehGridPolarSize;
        double dt = 1.0;
        double asq = 0.03;
        double d = 0.95;
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++) {
                double du_dr = 0.5 * (u[j][i+1] - u[j][i-1]);
                double d2u_drsq = u[j][i+1] - 2 * u[j][i] + u[j][i-1];
                double d2u_dthsq = u[j+1][i] - 2 * u[j][i] + u[j-1][i];
                v[j][i] = d * v[j][i] + (asq * dt) * (d2u_drsq + du_dr / i + d2u_dthsq / (i * i));
        }
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++)
                u[j][i] += v[j][i] * dt;
        double u0 = 0, v0 = 0;
        for (int j = 1; j <= n; j++) {
            u0 += u[j][1];
            v0 += v[j][1];
        }
        u0 /= n;
        v0 /= n;
        for (int j = 1; j <= n; j++) {
            u[j][0] = u0;
            v[j][0] = v0;
        }
        //dumpGrid("u", u);
        //dumpGrid("v", v);
    }
    
    void dumpGrid(String label, double g[][]) {
        System.out.printf("SGBaseTE: %s:\n", label);
        int m = SGBaseTERenderer.ehGridRadialSize;
        int n = SGBaseTERenderer.ehGridPolarSize;
        for (int j = 0; j <= n+1; j++) {
            for (int i = 0; i <= m; i++)
                System.out.printf(" %6.3f", g[j][i]);
            System.out.print("\n");
        }
    }
    
    @Override
    protected IInventory getInventory() {
        return inventory;
    }

    public boolean irisIsClosed() {
        //System.out.printf("SGBaseTE.irisIsClosed: irisPhase = %s\n", irisPhase);
        return hasIrisUpgrade && irisPhase <= maxIrisPhase / 2;
    }
    
    public double getIrisAperture(double partialTicks) {
        return (lastIrisPhase * (1 - partialTicks) + irisPhase * partialTicks) / maxIrisPhase;
    }
    
    void irisUpdate() {
        lastIrisPhase = irisPhase;
        switch (irisState) {
            case Opening:
                if (irisPhase < maxIrisPhase)
                    ++irisPhase;
                else
                    enterIrisState(IrisState.Open);
                break;
            case Closing:
                if (irisPhase > 0)
                    --irisPhase;
                else
                    enterIrisState(IrisState.Closed);
                break;
        }
    }
    
    void enterIrisState(IrisState newState) {
        if (irisState != newState) {
            String oldDesc = irisStateDescription(irisState);
            String newDesc = irisStateDescription(newState);
            irisState = newState;
            markChanged();
            if (!oldDesc.equals(newDesc))
                //postEvent("sgIrisStateChange", "oldState", oldDesc, "newState", newDesc);
                postEvent("sgIrisStateChange", newDesc, oldDesc);
        }
    }
    
    public void openIris() {
        if (isMerged && hasIrisUpgrade && irisState != IrisState.Open) {
            enterIrisState(IrisState.Opening);
        }
    }
    
    public void closeIris() {
        if (isMerged && hasIrisUpgrade && irisState != IrisState.Closed) {
            enterIrisState(IrisState.Closing);
        }
    }
    
    public void onNeighborBlockChange() {
        if (!world.isRemote) {
            boolean newInput = BaseBlockUtils.blockIsGettingExternallyPowered(world, pos);
            if (redstoneInput != newInput) {
                redstoneInput = newInput;
                markDirty();
                if (redstoneInput) {
                    closeIris();
                } else {
                    openIris();
                }
            }
        }
    }
    
    void updateIrisEntity() {
        if (isMerged && hasIrisUpgrade) {
            if (!hasIrisEntity()) {
                EntityStargateIris ent = new EntityStargateIris(this);
                world.spawnEntity(ent);
            }
        } else {
            for (EntityStargateIris ent : findIrisEntities()) {
                world.removeEntity(ent);
            }
        }
    }
    
    boolean hasIrisEntity() {
        return findIrisEntities().size() != 0;
    }
    
    List<EntityStargateIris> findIrisEntities() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 2, z + 1);
        //System.out.printf("SGBaseTE.findIrisEntities: in %s\n", box);
        return world.getEntitiesWithinAABB(EntityStargateIris.class, box);
    }
    
    ItemStack getCamouflageStack(BlockPos cpos) {
        //System.out.printf("SGBaseTE.getCamouflageStack: for %s from base at %s\n", cpos, pos);
        Trans3 t = localToGlobalTransformation();
        Vector3 p = t.ip(Vector3.blockCenter(cpos));
        //System.out.printf("SGBaseTE.getCamouflageStack: p = %s\n", p);
        if (p.y == 0) {
            int i = 2 + p.roundX();
            //System.out.printf("SGBaseTE.getCamouflageStack: i = %s\n", i);
            if (i >= 0 && i < 5)
                return getStackInSlot(firstCamouflageSlot + i);
        }
        return null;
    }
    
    boolean isCamouflageSlot(int slot) {
        return slot >= firstCamouflageSlot && slot < firstCamouflageSlot + numCamouflageSlots;
    }
    
    @Override
    protected void onInventoryChanged(int slot) {
        //System.out.printf("SGBaseTE.onInventoryChanged: %s\n", slot);
        super.onInventoryChanged(slot);
        if (isCamouflageSlot(slot)) {
            //System.out.printf("SGBaseTE.onInventoryChanged: Camouflage slot changed\n");
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    BaseBlockUtils.markBlockForUpdate(world, pos.add(dx, 0, dz));
        }
    }

    protected int baseCornerCamouflage() {
        return max(baseCamouflageAt(0), baseCamouflageAt(4));
    }
    
    protected int baseCamouflageAt(int i) {
        ItemStack stack = getStackInSlot(i);
        if (stack != null) {
            Item item = stack.getItem();
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block != Blocks.AIR) {
                if (block instanceof BlockSlab)
                    return 1;
                int meta = item.getMetadata(stack);
                IBlockState state = block.getStateFromMeta(meta);
                if (state.isFullCube()) {
                    return 2;
                }
            }
        }
        return 0;
    }

    static int rdx[] = {1, 0, -1, 0};
    static int rdz[] = {0, -1, 0, 1};
    
    // Find locations of tile entities that could connect to the stargate ring.
    // TODO: Cache this
    public Collection<BlockRef> adjacentTiles() {
        Collection<BlockRef> result = new ArrayList<>();
        Trans3 t = localToGlobalTransformation();
        for (int i = -2; i <= 2; i++) {
            BlockPos bp = t.p(i, -1, 0).blockPos();
            TileEntity te = getWorldTileEntity(world, bp);
            if (te != null)
                result.add(new BlockRef(te));
        }
        return result;
    }
    
//------------------------------------ Computer interface ----------------------------------

    public void forwardNetworkPacket(Object packet) {
        SGBaseTE dte = getConnectedStargateTE();
        if (dte != null)
            dte.rebroadcastNetworkPacket(packet);
    }
    
    void rebroadcastNetworkPacket(Object packet) {
        for (BlockRef ref : adjacentTiles()) {
            TileEntity te = ref.getTileEntity();
            if (te instanceof SGInterfaceTE)
                ((SGInterfaceTE)te).rebroadcastNetworkPacket(packet);
        }
    }
    
    public String sendMessage(Object[] args) {
        SGBaseTE dte = getConnectedStargateTE();
        if (dte != null) {
            dte.postEvent("sgMessageReceived", args);
            return null;
        }
        else
            return "Stargate not connected";
    }
    
    void postEvent(String name, Object... args) {
        //System.out.printf("SGBaseTE.postEvent: %s from (%s,%s,%s)\n", name,
        //  xCoord, yCoord, zCoord);
        for (BlockRef b : adjacentTiles()) {
            TileEntity te = b.getTileEntity();
            if (te instanceof IComputerInterface) {
                //System.out.printf("SGBaseTE.postEvent: to TE at (%s,%s,%s)\n",
                //  b.xCoord, b.yCoord, b.zCoord);
                ((IComputerInterface)te).postEvent(this, name, args);
            }
        }
    }
    
    public String sgStateDescription() {
        return sgStateDescription(state);
    }
    
    static String sgStateDescription(SGState state) {
        switch (state) {
            case Idle: return "Idle";
            case Dialling:
            case InterDialling: return "Dialling";
            case SyncAwait:
            case Transient: return "Opening";
            case Connected: return "Connected";
            case Disconnecting: return "Closing";
            default: return "Unknown";
        }
    }
    
    public String irisStateDescription() {
        return irisStateDescription(irisState);
    }
    
    static String irisStateDescription(IrisState state) {
        return state.toString();
    }
    
    public static SGBaseTE getBaseTE(SGInterfaceTE ite) {
        return SGBaseTE.get(ite.getWorld(), ite.getPos().add(0, 1, 0));
    }
}

//------------------------------------------------------------------------------------------------

class BlockRef {
    public IBlockAccess world;
    BlockPos pos;
    
    public BlockRef(TileEntity te) {
        this(te.getWorld(), te.getPos());
    }
    
    public BlockRef(IBlockAccess world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }
    
    public TileEntity getTileEntity() {
        if (world == null || pos == null)
            return null;
        return world.getTileEntity(pos);
    }
}
