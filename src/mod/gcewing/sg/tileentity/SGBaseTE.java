//------------------------------------------------------------------------------------------------
//
//   SG Craft - Stargate base tile entity
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.tileentity;

import static gcewing.sg.BaseBlockUtils.getWorldTileEntity;
import static gcewing.sg.BaseMod.isModLoaded;
import static gcewing.sg.BaseUtils.max;
import static gcewing.sg.BaseUtils.min;

import com.google.common.collect.Sets;
import gcewing.sg.BaseBlockUtils;
import gcewing.sg.BaseConfiguration;
import gcewing.sg.BaseTileInventory;
import gcewing.sg.BaseUtils;
import gcewing.sg.event.SGMergeEvent;
import gcewing.sg.features.ic2.zpm.modulehub.ZpmHubTE;
import gcewing.sg.features.zpm.console.ZpmConsoleTE;
import gcewing.sg.features.edd.ExplorerData;
import gcewing.sg.generator.GeneratorAddressRegistry;
import gcewing.sg.tileentity.data.GateAccessData;
import gcewing.sg.tileentity.data.PlayerAccessData;
import gcewing.sg.util.GateShape;
import gcewing.sg.util.GateType;
import gcewing.sg.util.GeneralAddressRegistry;
import gcewing.sg.util.SGAddressing;
import gcewing.sg.SGCraft;
import gcewing.sg.util.SGLocation;
import gcewing.sg.util.SGState;
import gcewing.sg.Trans3;
import gcewing.sg.Vector3;
import gcewing.sg.block.DHDBlock;
import gcewing.sg.block.EventHorizonLightBlock;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.block.SGRingBlock;
import gcewing.sg.entity.EntityStargateIris;
import gcewing.sg.features.ic2.IC2PowerTE;
import gcewing.sg.features.zpm.ZpmAddon;
import gcewing.sg.features.zpm.ZpmSlotSource;
import gcewing.sg.features.zpm.link.ILinkableZpm;
import gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCartTE;
import gcewing.sg.features.oc.OCIntegration;
import gcewing.sg.features.oc.OCInterfaceTE;
import gcewing.sg.features.oc.OCWirelessEndpoint;
import gcewing.sg.interfaces.IComputerInterface;
import gcewing.sg.interfaces.ISGEnergySource;
import gcewing.sg.interfaces.IGatePowerSource;
import gcewing.sg.util.IrisState;
import gcewing.sg.util.OriGate;
import gcewing.sg.interfaces.LoopingSoundSource;
import gcewing.sg.util.FakeTeleporter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.server.management.PlayerChunkMap;
import gcewing.sg.network.SGChannel;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class SGBaseTE extends BaseTileInventory implements ITickable, LoopingSoundSource {

    static boolean debugState = false;
    public static boolean debugEnergyUse = false;
    static boolean debugConnect = false;
    // Logs each step of a dial on both sides (debug.dialSync)
    public static boolean debugDialSync = false;
    // Through log4j, so it reaches the log files: printf output only shows in the console
    public static final org.apache.logging.log4j.Logger dialLog = org.apache.logging.log4j.LogManager.getLogger("sgcraft");

    // A gate's dial state, for debugDialSync
    public String dialSyncText() {
        return String.format("gate %s tick %d: state %s timeout %d/%d, locked %d of '%s', initiator %s, ring %.1f -> %.1f",
            pos, world == null ? -1 : world.getTotalWorldTime(), state, timeout, maxTimeout, numEngagedChevrons, dialledAddress,
            isInitiator, ringAngle, targetRingAngle);
    }
    static boolean debugTransientDamage = false;
    static boolean debugTeleport = false;
    static boolean debugZPM = false;
    final static DecimalFormat dFormat = new DecimalFormat("###,###,###,##0");
    // An amount of the gate's energy for a player to read: in Forge Energy, what they feed a Stargate Power Unit (at its
    // fpPerSGEnergyUnit), whole
    static String energyText(double energy) {
        return dFormat.format(Math.ceil(energy * SGCraft.FPPerSGEnergyUnit));
    }

    public static SoundEvent
            m_dialFailSound,
            p_dialFailSound,
            m_connectSound,
            p_connectSound,
            disconnectSound,
            irisOpenSound,
            irisCloseSound,
            irisHitSound,
            shieldActivationSound,
            shieldDeactivationSound,
            oriOpenSound,
            m_dhdPressSound,
            p_dhdPressSound,
            m_dhdDialSound,
            p_dhdDialSound,
            m_chevronOutgoingSound,
            m_chevronIncomingSound,
            p_chevronOutgoingSound,
            p_chevronIncomingSound,
            lockOutgoingSound,
            lockIncomingSound,
            m_gateRollSound,
            p_gateRollSound,
            eventHorizonSound,
            teleportSound,
            malfunctionSound;

    public static void registerSounds(SGCraft mod) {
        m_dialFailSound = mod.newSound("m_dial_fail");
        p_dialFailSound = mod.newSound("p_dial_fail");
        m_connectSound = mod.newSound("m_gate_open");
        p_connectSound = mod.newSound("p_gate_open");
        disconnectSound = mod.newSound("gate_close");
        irisOpenSound = mod.newSound("iris_open");
        irisCloseSound = mod.newSound("iris_close");
        irisHitSound = mod.newSound("iris_hit");
        shieldActivationSound = mod.newSound("shield_activation_atlantis");
        shieldDeactivationSound = mod.newSound("shield_deactivation");
        oriOpenSound = mod.newSound("ori_open");
        m_dhdPressSound = mod.newSound("m_dhd_press");
        p_dhdPressSound = mod.newSound("p_dhd_press");
        m_dhdDialSound = mod.newSound("m_dhd_dial");
        p_dhdDialSound = mod.newSound("p_dhd_dial");
        m_chevronOutgoingSound = mod.newSound("m_chevron_outgoing");
        m_chevronIncomingSound = mod.newSound("m_chevron_incoming");
        p_chevronOutgoingSound = mod.newSound("p_chevron_outgoing");
        p_chevronIncomingSound = mod.newSound("p_chevron_incoming");
        lockOutgoingSound = mod.newSound("lock_outgoing");
        lockIncomingSound = mod.newSound("lock_incoming");
        m_gateRollSound = mod.newSound("m_gate_roll");
        p_gateRollSound = mod.newSound("p_gate_roll");
        eventHorizonSound = mod.newSound("event_horizon");
        teleportSound = mod.newSound("teleport");
        malfunctionSound = mod.newSound("malfunction");
    }

    public final static String symbolChars = SGAddressing.symbolChars;
    public final static int numRingSymbols = SGAddressing.numSymbols;
    public final static double ringSymbolAngle = 360.0 / numRingSymbols;
    // A Milky Way gate's symbol ring carries the show's 39 glyphs, evenly spaced: the point of
    // origin, then the glyphs of the address symbols in order, then the rest. Dialling a symbol
    // turns the ring to bring its glyph to the top, ringGlyphAngle for each glyph.
    public final static int numRingGlyphs = 39;
    public final static double ringGlyphAngle = 360.0 / numRingGlyphs;
    // A Universe gate's ring is, as on the show, nine sections, one under each chevron: the
    // chevron's panel, then four symbols clockwise from it, each in its own slot. Dialling a symbol
    // turns the whole gate to bring the symbol to the top.
    public final static double universeGroupAngle = 360.0 / 9;
    public final static double universeChevronSectorAngle = 8;
    public final static double universeSlotAngle = (universeGroupAngle - universeChevronSectorAngle) / 4;

    // How far clockwise from the top chevron a Universe gate's symbol is centred, which is also the
    // angle the gate turns to to bring it to the top
    public static double universeSymbolOffset(int symbol) {
        return symbol / 4 * universeGroupAngle + universeChevronSectorAngle / 2
                + (symbol % 4 + 0.5) * universeSlotAngle;
    }

    public static int ringGlyphForSymbol(int symbol) {
        return symbol + 1;
    }
    public final static double irisZPosition = 0.1;
    public final static double irisThickness = 0.2; //0.1;
    public final static DamageSource irisDamageSource = new DamageSource("sgcraft:iris");
    public final static float irisDamageAmount = 1000000;
    public static int minutesOpenPerFuelItem = 80;
    // Energy a raised shield takes each tick (see tickShieldEnergy)
    public static double shieldEnergyPerTick = 1.0;
    // How far a Shield Console reaches for a gate with a shield (to the nearest part of its ring)
    public static int shieldConsoleRange = 40;
    // Ticks a shield takes to rise or fall, against the iris blades' maxIrisPhase
    final static int shieldRiseTicks = 10;


    final static int interDiallingTime = 10; // ticks
    // A Milky Way gate pauses longer between symbols, long enough to see its top chevron lock each
    // one (see GateRenderer.chevronLockTicks)
    final static int milkyWayInterDiallingTime = 24; // ticks
    public int syncAwaitTime = 30; // ticks  // 10 - withinWorld, 30 - dimensional
    final static int transientDuration = 20; // ticks
    // An Ori gate's event horizon first fills in from the ring to the middle, taking this long, before the kawoosh
    public final static int oriFillTicks = 30;
    final static int disconnectTime = 40; // ticks

    final static double openingTransientIntensity = 1.3;
    final static double openingTransientRandomness = 0.25;
    final static double closingTransientRandomness = 0.25;

    final static int maxIrisPhase = 70; // ticks

    public static int firstCamouflageSlot = 0;
    public static int numCamouflageSlots = 5;
    // A supergate's base also holds ZPMs (see zpmSources)
    public static int firstZpmSlot = numCamouflageSlots, numZpmSlots = 3;
    public static int numInventorySlots = numCamouflageSlots + numZpmSlots;

    static float defaultChevronAngle = 40f;
    static float chevronAngles[][] = {
            //     0    1    2    <-- Base camouflage level
            { 45f, 45f, 40f }, // 7 chevrons
            { 36f, 33f, 30f }  // 9 chevrons
    };

    // Static options
    static int chunkLoadingRange = 1;
    static boolean logStargateEvents = false;
    public static float soundVolume = 1F;
    static boolean variableChevronPositions = true;
    static Random random = new Random();
    static DamageSource transientDamageSource = new DamageSource("sgcraft:transient");
    public static BaseConfiguration cfg;
    static double transientDamageRate = 5000;
    static boolean resetIsGenerated = true;
    static double supergateEnergyMultiplier = 8.0;
    // Forge's default limit on the chunks one ticket can keep loaded
    static final int MAX_CHUNKS_PER_TICKET = 25;
    // Most blocks a kawoosh clears in one tick; a big supergate's takes a few ticks to clear
    static final int KAWOOSH_BLOCKS_PER_TICK = 2048;

    // Instanced options
    public boolean isMerged;
    public SGState state = SGState.Idle;
    public double startRingAngle, ringAngle, lastRingAngle, targetRingAngle; // degrees
    public int numEngagedChevrons;
    public String dialledAddress = "";
    public boolean isLinkedToController;
    public BlockPos linkedPos = new BlockPos(0, 0, 0);
    public boolean hasChevronUpgrade;
    public boolean hasIrisUpgrade;
    // The gate's iris upgrade is an energy shield (the Shield Upgrade) rather than metal blades. Both close and open the
    // same way, so whatever works the iris works the shield; it only looks and sounds different, and takes energy.
    public boolean hasShield;
    public IrisState irisState = IrisState.Open;
    public int irisPhase = maxIrisPhase; // 0 = fully closed, maxIrisPhase = fully open
    public int lastIrisPhase = maxIrisPhase;
    public OCWirelessEndpoint ocWirelessEndpoint; //[OC]
    public boolean debugCCInterface = false;
    public int dialedDigit = 0;
    public String enteredAddress = "";
    public String immediateDialAddress = "";
    public boolean errorState = false;
    public boolean checkForMalfunction = false;
    private boolean performBlockDamage = true;

    public SGLocation connectedLocation;
    public boolean isInitiator, redstoneInput, loaded;
    int timeout, maxTimeout;
    public double energyInBuffer, distanceFactor; // all energy use is multiplied by this
    public String homeAddress, addressError;
    private int updated = 0;

    IInventory inventory = new InventoryBasic("Stargate", false, numInventorySlots);

    // ZPM Implementation
    public boolean zpmPowered = false;
    public boolean destinationRequiresZPM = false;

    // Retain current irisState prior to opening
    public boolean wasIrisClosed = false;

    // Unique Gate Configurator Options
    public int gateType = 1; // GateType.id: 1 = Milky Way, 2 = Pegasus, 3 = Universe, 4 = Ori
    public int gateOrientation = 1;
    public int secondsToStayOpen = 5 * 60;
    public int ticksToStayOpen = 20 * secondsToStayOpen;
    public boolean oneWayTravel = true;
    public double ringRotationSpeed = 2.0;
    public double maxEnergyBuffer = 1000;
    public double energyPerFuelItem = 96000;
    public double distanceFactorMultiplier = 1.0;
    public double interDimensionMultiplier = 4.0;
    public int gateOpeningsPerFuelItem = 24;
    public double energyToOpen;
    double energyUsePerTick;
    public boolean reverseWormholeKills = false;
    public boolean closeFromEitherEnd = true;
    public boolean preserveInventory = false;
    public boolean allowIncomingConnections = true;
    public boolean allowOutgoingConnections = true;
    public boolean chevronsLockOnDial = false;
    public boolean returnToPreviousIrisState = false;
    public int facingDirectionOfBase = 0;
    public boolean requiresNoPower = false;
    public boolean transientDamage = true;
    public boolean transparency = true;
    public boolean defaultAllowIncoming = true;
    public boolean defaultAllowOutgoing = true;
    public boolean defaultAllowGateAccess = true;
    public boolean defaultAllowIrisAccess = true;
    public boolean defaultAllowAdminAccess = true;
    public boolean useDHDFuelSource = true;
    public boolean allowRedstoneOutput = true;
    public boolean allowRedstoneInput = true;
    public boolean canPlayerBreakPlayerGate = false;
    public boolean canPlayerBreakGeneratedGate = false;
    // Can't be broken at all, but by players in creative: not by survival players, explosions, withers or the dragon
    public boolean invulnerable = false;
    public boolean displayGateAddress = true;
    public boolean isGenerated = false;

    // Access Control Lists
    private List<PlayerAccessData> playerAccessData;
    private List<GateAccessData> gateAccessData;

    double ehGrid[][][];
    private GateShape shape;
    // Blocks from a supergate's centre to its base, or 0 for a stargate or a supergate whose
    // ring has not merged yet
    private int gateRadius;
    private static Set<UUID> messagesQueue = Sets.newHashSet();

    public SGBaseTE() {
        this.hasIrisUpgrade = cfg.getBoolean("stargate", "irisUpgrade", this.hasIrisUpgrade);
        this.hasChevronUpgrade = cfg.getBoolean("stargate", "chevronUpgrade", this.hasChevronUpgrade);
        this.gateType = cfg.getInteger("stargate", "gateType", this.gateType);
        this.secondsToStayOpen = cfg.getInteger("stargate", "secondsToStayOpen", this.secondsToStayOpen);
        this.oneWayTravel = cfg.getBoolean("stargate", "oneWayTravel", this.oneWayTravel);
        this.ringRotationSpeed = cfg.getDouble("stargate", "ringRotationSpeed", this.ringRotationSpeed);
        this.maxEnergyBuffer = cfg.getDouble("stargate", "maxEnergyBuffer", this.maxEnergyBuffer);
        this.energyPerFuelItem = cfg.getDouble("stargate", "energyPerFuelItem", this.energyPerFuelItem);
        this.gateOpeningsPerFuelItem = cfg.getInteger("stargate", "gateOpeningsPerFuelItem", this.gateOpeningsPerFuelItem);
        this.distanceFactorMultiplier = cfg.getDouble("stargate", "distanceFactorMultiplier", this.distanceFactorMultiplier);
        this.interDimensionMultiplier = cfg.getDouble("stargate", "interDimensionMultiplier", this.interDimensionMultiplier);
        this.reverseWormholeKills = cfg.getBoolean("stargate", "reverseWormholeKills", this.reverseWormholeKills);
        this.closeFromEitherEnd = cfg.getBoolean("stargate", "closeFromEitherEnd", this.closeFromEitherEnd);
        this.preserveInventory = cfg.getBoolean("iris", "preserveInventory", this.preserveInventory);
        this.chevronsLockOnDial = cfg.getBoolean("stargate", "chevronsLockOnDial", this.chevronsLockOnDial);
        this.returnToPreviousIrisState = cfg.getBoolean("stargate", "returnToPreviousIrisState", this.returnToPreviousIrisState);
        this.requiresNoPower = cfg.getBoolean("stargate", "requiresNoPower", this.requiresNoPower);
        this.transientDamage = cfg.getBoolean("stargate", "transientDamage", this.transientDamage); // was never read for a new gate
        this.transparency = cfg.getBoolean("stargate", "transparency", this.transparency);
        this.defaultAllowIncoming = cfg.getBoolean("gate-access", "defaultAllowIncoming", this.defaultAllowIncoming);
        this.defaultAllowOutgoing = cfg.getBoolean("gate-access", "defaultAllowOutgoing", this.defaultAllowOutgoing);
        this.defaultAllowGateAccess = cfg.getBoolean("player-access", "defaultAllowGateAccess", this.defaultAllowGateAccess);
        this.defaultAllowIrisAccess = cfg.getBoolean("player-access", "defaultAllowIrisAccess", this.defaultAllowIrisAccess);
        this.defaultAllowAdminAccess = cfg.getBoolean("player-access", "defaultAllowAdminAccess", this.defaultAllowAdminAccess);
        this.useDHDFuelSource = cfg.getBoolean("dhd", "useDHDFuelSource", this.useDHDFuelSource);
        this.allowRedstoneOutput = cfg.getBoolean("stargate", "allowRedstoneOutput", this.allowRedstoneOutput);
        this.allowRedstoneInput = cfg.getBoolean("iris", "allowRedstoneInput", this.allowRedstoneInput);
        this.canPlayerBreakPlayerGate = cfg.getBoolean("stargate", "canPlayerBreakGate", this.canPlayerBreakPlayerGate);
        this.canPlayerBreakGeneratedGate = cfg.getBoolean("stargate", "canPlayerBreakGate", this.canPlayerBreakGeneratedGate);
        this.displayGateAddress = cfg.getBoolean("stargate", "displayGateAddress", this.displayGateAddress);
        updateEnergyCosts();
    }

    private void updateEnergyCosts() {
        this.energyToOpen = this.energyPerFuelItem / this.gateOpeningsPerFuelItem;
        this.energyUsePerTick = this.energyPerFuelItem / (this.minutesOpenPerFuelItem * 60 * 20);
    }

    // The kind of gate comes from the base block, and a supergate's size from gateRadius, which is
    // set when its ring merges. It is looked up the first time it is needed, once the block is in
    // the world.
    public GateShape getShape() {
        if (shape == null) {
            if (world == null) {
                return GateShape.STANDARD;
            }
            Block block = world.getBlockState(pos).getBlock();
            if (!(block instanceof SGBaseBlock)) {
                return GateShape.STANDARD;
            }
            GateShape blockShape = ((SGBaseBlock) block).getShape();
            if (blockShape.isSupergate() && gateRadius >= GateShape.MIN_SUPERGATE_RADIUS
                    && gateRadius <= GateShape.MAX_SUPERGATE_RADIUS) {
                shape = GateShape.supergate(gateRadius);
            } else {
                shape = blockShape;
            }
        }
        return shape;
    }

    public void setShape(GateShape newShape) {
        if (newShape != shape) {
            shape = newShape;
            gateRadius = newShape.isSupergate() ? newShape.halfWidth : 0;
            ehGrid = null; // Its size depends on the shape
            markChanged();
        }
    }

    public boolean isSupergate() {
        return getShape().isSupergate();
    }

    // How much more energy than a stargate this gate takes to open and keep open
    public double sizeEnergyMultiplier() {
        return isSupergate() ? supergateEnergyMultiplier : 1.0;
    }

    public GateType getGateType() {
        return GateType.fromId(gateType);
    }

    // Makes this a gate of the given type, as a base of that type builds: a Pegasus gate's ring
    // turns faster, as the Pegasus upgrade sets it
    public void setGateType(GateType type) {
        gateType = type.id;
        if (type == GateType.PEGASUS) {
            ringRotationSpeed = 6.0D;
        }
        markChanged();
    }

    public static void configure(BaseConfiguration cfg) {
        SGBaseTE.cfg = cfg;
        net.minecraftforge.common.config.Property dialSync = cfg.get("debug", "dialSync", false);
        dialSync.setComment("Log each step of a dial: the server its state changes and locks, the client every gate update it gets (and "
            + "how) and when it starts the top chevron's lock animation. For finding why a dial looks wrong.");
        debugDialSync = dialSync.getBoolean(false);
        // Instanced config values
        // Note: allowing these methods to be hit creates the config in the file.
        cfg.getDouble("stargate", "energyPerFuelItem", 96000);
        cfg.getInteger("stargate", "gateOpeningsPerFuelItem", 24);
        cfg.getInteger("stargate", "secondsToStayOpen", 5 * 60);
        cfg.getBoolean("stargate", "oneWayTravel", true);
        cfg.getBoolean("stargate", "reverseWormholeKills", false);
        cfg.getBoolean("stargate", "closeFromEitherEnd", true);
        cfg.getDouble("stargate", "maxEnergyBuffer", 1000);
        cfg.getDouble("stargate", "distanceFactorMultiplier", 1.0);
        cfg.getDouble("stargate", "interDimensionMultiplier", 4.0);
        cfg.getBoolean("iris", "preserveInventory", false);

        // New instance config values
        cfg.getInteger("stargate", "gateType", 1);
        cfg.getBoolean("stargate", "chevronsLockOnDial", false);
        cfg.getBoolean("stargate", "returnToPreviousIrisState", false);
        cfg.getDouble("stargate", "ringRotationSpeed", 2.0);
        cfg.getBoolean("stargate", "irisUpgrade", false);
        cfg.getBoolean("stargate", "chevronUpgrade", false);
        cfg.getBoolean("stargate", "requiresNoPower", false);
        cfg.getBoolean("stargate", "transientDamage", true);
        cfg.getBoolean("stargate", "transparency", true);
        cfg.getBoolean("gate-access", "defaultAllowIncoming", true);
        cfg.getBoolean("gate-access", "defaultAllowOutgoing", true);
        cfg.getBoolean("player-access", "defaultAllowGateAccess", true);
        cfg.getBoolean("player-access", "defaultAllowIrisAccess", true);
        cfg.getBoolean("player-access", "defaultAllowAdminAccess", true);
        cfg.getBoolean("dhd", "useDHDFuelSource", true);
        cfg.getBoolean("stargate", "allowRedstoneOutput", true);
        cfg.getBoolean("iris", "allowRedstoneInput", true);
        cfg.getBoolean("stargate", "canPlayerBreakPlayerGate", true);
        cfg.getBoolean("stargate", "canPlayerBreakGeneratedGate", false);
        cfg.getBoolean("stargate", "displayGateAddress", true);

        // Global static config values
        minutesOpenPerFuelItem = cfg.getInteger("stargate", "minutesOpenPerFuelItem", minutesOpenPerFuelItem);
        shieldEnergyPerTick = cfg.getDouble("shield", "energyPerTick", shieldEnergyPerTick);
        shieldConsoleRange = cfg.getInteger("shield", "consoleRange", shieldConsoleRange);
        chunkLoadingRange = cfg.getInteger("options", "chunkLoadingRange", chunkLoadingRange);
        logStargateEvents = cfg.getBoolean("options", "logStargateEvents", logStargateEvents);
        soundVolume = (float)cfg.getDouble("stargate", "soundVolume", soundVolume);
        variableChevronPositions = cfg.getBoolean("stargate", "variableChevronPositions", variableChevronPositions);
        transientDamageRate = cfg.getDouble("stargate", "transientDamageRate", transientDamageRate);
        resetIsGenerated = cfg.getBoolean("stargate", "resetIsGenerated", resetIsGenerated);
        supergateEnergyMultiplier = cfg.getDouble("supergate", "energyMultiplier", supergateEnergyMultiplier);
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
        // Big enough for either orientation: a horizontal gate extends height - 1 blocks out
        // from its base in any horizontal direction, and its kawoosh bursts up or down out of it
        GateShape shape = getShape();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ(), r = Math.max(shape.halfWidth, shape.height - 1);
        int k = isFlat() ? (int) Math.ceil(Math.max(shape.kawooshDamageMax.z, shape.kawooshBlockMax.z)) + 1 : 1;
        // The Ori ring is drawn bigger than its blocks (see OriGate.outerScale)
        double ori = getGateType() == GateType.ORI ? Math.ceil(shape.ringOuterRadius * (OriGate.outerScale - 1)) : 0;
        return new AxisAlignedBB(x - r, y - k, z - r, x + r + 1, y + Math.max(shape.height, k), z + r + 1).grow(ori);
    }

    // A gate with a shield, or an Ori gate, is drawn again in Forge's second pass, after translucent blocks (water,
    // glass, some leaves), which then show through its see-through shield or light instead of being drawn over it
    // (see SGBaseTERenderer)
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || (pass == 1 && ((hasIrisUpgrade && hasShield) || getGateType() == GateType.ORI));
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 32768.0;
    }

    @Override
    public void onAddedToWorld() {
        if (SGBaseBlock.debugMerge) {
        System.out.print("SGBaseTE.onAddedToWorld\n");
        }
        updateChunkLoadingStatus();
    }

    // The space taken up by the gate's ring and the region just in front of and behind it
    AxisAlignedBB gateBounds() {
        GateShape shape = getShape();
        double w = shape.halfWidth + 0.5, d = GateShape.PORTAL_TRACKING_DEPTH;
        Vector3 p0, p1;
        if (gateOrientation == 1) {
            p0 = new Vector3(-w, -0.5, -d);
            p1 = new Vector3(w, shape.height - 0.5, d);
        } else {
            p0 = new Vector3(-w, -d, 0.5);
            p1 = new Vector3(w, d, -(shape.height - 0.5));
        }
        return localToGlobalTransformation().box(p0, p1);
    }

    public void updateChunkLoadingStatus() {
        if (state != SGState.Idle) {
            int n = chunkLoadingRange;
            if (n < 0) {
                n = 0;
            }
            if (debugState) {
                System.out.println("updateChunkLoadingStatus w/ Gatestate: " + state);
            }
            // Also cover every chunk the gate itself stands in, so that all of a big gate stays
            // loaded. This adds just the chunks it occupies rather than a square around the base,
            // keeping even the biggest supergate within Forge's 25 chunks per ticket.
            AxisAlignedBB box = gateBounds();
            int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
            int minX = Math.min(-n, ((int) Math.floor(box.minX) >> 4) - cx);
            int minZ = Math.min(-n, ((int) Math.floor(box.minZ) >> 4) - cz);
            int maxX = Math.max(n, ((int) Math.floor(box.maxX) >> 4) - cx);
            int maxZ = Math.max(n, ((int) Math.floor(box.maxZ) >> 4) - cz);
            if ((maxX - minX + 1) * (maxZ - minZ + 1) > MAX_CHUNKS_PER_TICKET) {
                // Too many with the square added; the gate's own chunks matter most
                minX = ((int) Math.floor(box.minX) >> 4) - cx;
                minZ = ((int) Math.floor(box.minZ) >> 4) - cz;
                maxX = ((int) Math.floor(box.maxX) >> 4) - cx;
                maxZ = ((int) Math.floor(box.maxZ) >> 4) - cz;
            }
            SGCraft.chunkManager.setForcedChunkRange(this, minX, minZ, maxX, maxZ);
        } else {
            SGCraft.chunkManager.clearForcedChunkRange(this);
        }
    }

    public static SGBaseTE at(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof SGBaseTE ? (SGBaseTE) te : null;
    }

    // Why the gate this one is dialling can't go on with the dial (found as target), or null if it can: it isn't there,
    // or no longer counts itself connected to this gate
    private String farGateProblem(SGBaseTE target) {
        if (connectedLocation == null) {
            return "it no longer knows which gate it was dialling";
        }
        String where = String.format("%s in dimension %d", connectedLocation.pos, connectedLocation.dimension);
        if (target == null) {
            World farWorld = SGAddressing.getWorld(connectedLocation.dimension);
            if (farWorld == null) {
                return "the gate it was dialling, at " + where + ", is in a dimension that isn't registered";
            }
            TileEntity te = farWorld.getTileEntity(connectedLocation.pos);
            return "the gate it was dialling, at " + where + ", isn't there (found "
                + (te == null ? "no tile entity" : te.getClass().getSimpleName()) + ")";
        }
        if (target.state == SGState.Idle || target.connectedLocation == null || !target.connectedLocation.pos.equals(pos)
                || target.connectedLocation.dimension != world.provider.getDimension()) {
            return "the gate it was dialling, at " + where + ", is no longer connected to it (it's " + target.state
                + (target.connectedLocation == null ? ", connected to nothing" : ", connected to " + target.connectedLocation.pos
                + " in dimension " + target.connectedLocation.dimension) + ")";
        }
        return null;
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

    @Override
    protected void setWorldCreate(World world) {
        this.world = world;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.isMerged = nbt.getBoolean("isMerged");
        int radius = nbt.getInteger("gateRadius");
        if (radius != gateRadius) {
            gateRadius = radius;
            shape = null; // Looked up again with the new size
            ehGrid = null;
        }
        // Out of range (bad data) as idle: thrown, the gate wasn't loaded at all, losing its upgrades and ZPMs
        int stateIndex = nbt.getInteger("state");
        this.state = stateIndex >= 0 && stateIndex < SGState.values().length ? SGState.values()[stateIndex] : SGState.Idle;
        SGState oldState = state;
        this.ringAngle = nbt.getDouble("ringAngle");
        this.startRingAngle = nbt.getDouble("startRingAngle");
        this.targetRingAngle = nbt.getDouble("targetRingAngle");
        this.numEngagedChevrons = nbt.getInteger("numEngagedChevrons");
        this.dialledAddress = nbt.getString("dialledAddress");
        this.pegasusFillStart = nbt.getLong("pegasusFillStart");
        this.pegasusFillEnd = nbt.getLong("pegasusFillEnd");
        this.isLinkedToController = nbt.getBoolean("isLinkedToController");
        int x = nbt.getInteger("linkedX");
        int y = nbt.getInteger("linkedY");
        int z = nbt.getInteger("linkedZ");
        this.linkedPos = new BlockPos(x, y, z);
        if (nbt.hasKey("connectedLocation")) {
            connectedLocation = new SGLocation(nbt.getCompoundTag("connectedLocation"));
        }
        linkedZpms.clear();
        NBTTagList zpmList = nbt.getTagList("linkedZpms", Constants.NBT.TAG_LONG);
        for (int i = 0; i < zpmList.tagCount(); i++) {
            linkedZpms.add(BlockPos.fromLong(((NBTTagLong) zpmList.get(i)).getLong()));
        }
        this.isInitiator = nbt.getBoolean("isInitiator");
        this.timeout = nbt.getInteger("timeout");
        this.maxTimeout = nbt.getInteger("maxTimeout");
        this.energyInBuffer = nbt.hasKey("energyInBuffer") ? nbt.getDouble("energyInBuffer") : nbt.getInteger("fuelBuffer");
        this.distanceFactor = nbt.getDouble("distanceFactor");
        int irisIndex = nbt.getInteger("irisState");
        this.irisState = irisIndex >= 0 && irisIndex < IrisState.values().length ? IrisState.values()[irisIndex] : IrisState.Open;
        this.irisPhase = nbt.getInteger("irisPhase");
        this.redstoneInput = nbt.getBoolean("redstoneInput");
        this.homeAddress = getStringOrNull(nbt, "address");
        this.addressError = nbt.getString("addressError");

        if (oldState != this.state && this.state == SGState.Connected && this.world.isRemote && !this.isRemoteView) {
            SGCraft.playSound(this, eventHorizonSound);
        }

        if (nbt.hasKey("hasIrisUpgrade") && !SGCraft.forceSGBaseTEUpdate) {
            this.hasIrisUpgrade = nbt.getBoolean("hasIrisUpgrade");
        } else {
            this.hasIrisUpgrade = cfg.getBoolean("stargate", "irisUpgrade", this.hasIrisUpgrade);
        }
        this.hasShield = this.hasIrisUpgrade && nbt.getBoolean("hasShield");

        if (nbt.hasKey("hasChevronUpgrade") && !SGCraft.forceSGBaseTEUpdate) {
            this.hasChevronUpgrade = nbt.getBoolean("hasChevronUpgrade");
        } else {
            this.hasChevronUpgrade = cfg.getBoolean("stargate", "chevronUpgrade", this.hasChevronUpgrade);
        }

        if (nbt.hasKey("gateType") && !SGCraft.forceSGBaseTEUpdate) {
            this.gateType = nbt.getInteger("gateType");
        } else {
            this.gateType = cfg.getInteger("stargate", "gateType", this.gateType);
        }

        if (nbt.hasKey("secondsToStayOpen") && !SGCraft.forceSGBaseTEUpdate) {
            this.secondsToStayOpen = nbt.getInteger("secondsToStayOpen");
        } else {
            this.secondsToStayOpen = cfg.getInteger("stargate", "secondsToStayOpen", this.secondsToStayOpen);
        }

        if (nbt.hasKey("oneWayTravel") && !SGCraft.forceSGBaseTEUpdate) {
            this.oneWayTravel = nbt.getBoolean("oneWayTravel");
        } else {
            this.oneWayTravel = cfg.getBoolean("stargate", "oneWayTravel", this.oneWayTravel);
        }

        if (nbt.hasKey("ringRotationSpeed") && !SGCraft.forceSGBaseTEUpdate) {
            this.ringRotationSpeed = nbt.getDouble("ringRotationSpeed");
        } else {
            this.ringRotationSpeed = cfg.getDouble("stargate", "ringRotationSpeed", this.ringRotationSpeed);
        }

        if (nbt.hasKey("maxEnergyBuffer") && !SGCraft.forceSGBaseTEUpdate) {
            this.maxEnergyBuffer = nbt.getDouble("maxEnergyBuffer");
        } else {
            this.maxEnergyBuffer = cfg.getDouble("stargate", "maxEnergyBuffer", this.maxEnergyBuffer);
        }

        if (nbt.hasKey("energyPerFuelItem") && !SGCraft.forceSGBaseTEUpdate) {
            this.energyPerFuelItem = nbt.getDouble("energyPerFuelItem");
        } else {
            this.energyPerFuelItem = cfg.getDouble("stargate", "energyPerFuelItem", this.energyPerFuelItem);
        }

        if (nbt.hasKey("gateOpeningsPerFuelItem") && !SGCraft.forceSGBaseTEUpdate) {
            this.gateOpeningsPerFuelItem = nbt.getInteger("gateOpeningsPerFuelItem");
        } else {
            this.gateOpeningsPerFuelItem = cfg.getInteger("stargate", "gateOpeningsPerFuelItem", this.gateOpeningsPerFuelItem);
        }

        if (nbt.hasKey("distanceFactorMultiplier") && !SGCraft.forceSGBaseTEUpdate) {
            this.distanceFactorMultiplier = nbt.getDouble("distanceFactorMultiplier");
        } else {
            this.distanceFactorMultiplier = cfg.getDouble("stargate", "distanceFactorMultiplier", this.distanceFactorMultiplier);
        }

        if (nbt.hasKey("interDimensionalMultiplier") && !SGCraft.forceSGBaseTEUpdate) {
            this.interDimensionMultiplier = nbt.getDouble("interDimensionalMultiplier");
        } else {
            this.interDimensionMultiplier = cfg.getDouble("stargate", "interDimensionMultiplier", this.interDimensionMultiplier);
        }

        if (nbt.hasKey("reverseWormholeKills") && !SGCraft.forceSGBaseTEUpdate) {
            this.reverseWormholeKills = nbt.getBoolean("reverseWormholeKills");
        } else {
            reverseWormholeKills = cfg.getBoolean("stargate", "reverseWormholeKills", this.reverseWormholeKills);
        }

        if (nbt.hasKey("closeFromEitherEnd") && !SGCraft.forceSGBaseTEUpdate) {
            this.closeFromEitherEnd = nbt.getBoolean("closeFromEitherEnd");
        } else {
            this.closeFromEitherEnd = cfg.getBoolean("stargate", "closeFromEitherEnd", this.closeFromEitherEnd);
        }

        if (nbt.hasKey("preserveInventory") && !SGCraft.forceSGBaseTEUpdate) {
            this.preserveInventory = nbt.getBoolean("preserveInventory");
        } else {
            this.preserveInventory = cfg.getBoolean("iris", "preserveInventory", this.preserveInventory);
        }

        if (nbt.hasKey("allowIncomingConnections") && !SGCraft.forceSGBaseTEUpdate) {
            this.allowIncomingConnections = nbt.getBoolean("allowIncomingConnections");
        } else {
            this.allowIncomingConnections = true;
        }

        if (nbt.hasKey("allowOutgoingConnections") && !SGCraft.forceSGBaseTEUpdate) {
            this.allowOutgoingConnections = nbt.getBoolean("allowOutgoingConnections");
        } else {
            this.allowOutgoingConnections = true;
        }

        if (nbt.hasKey("chevronsLockOnDial") && !SGCraft.forceSGBaseTEUpdate) {
            this.chevronsLockOnDial = nbt.getBoolean("chevronsLockOnDial");
        } else {
            this.chevronsLockOnDial = cfg.getBoolean("stargate", "chevronsLockOnDial", this.chevronsLockOnDial);
        }

        if (nbt.hasKey("returnToPreviousIrisState") && !SGCraft.forceSGBaseTEUpdate) {
            this.returnToPreviousIrisState = nbt.getBoolean("returnToPreviousIrisState");
        } else {
            this.returnToPreviousIrisState = cfg.getBoolean("stargate", "returnToPreviousIrisState", this.returnToPreviousIrisState);
        }

        if (nbt.hasKey("gateOrientation")) {
            this.gateOrientation = nbt.getInteger("gateOrientation");
        } else {
            this.gateOrientation = 1;
        }

        if (nbt.hasKey("requiresNoPower") && !SGCraft.forceSGBaseTEUpdate) {
            this.requiresNoPower = nbt.getBoolean("requiresNoPower");
        } else {
            this.requiresNoPower = cfg.getBoolean("stargate", "requiresNoPower", this.requiresNoPower);
        }

        if (nbt.hasKey("transientDamage") && !SGCraft.forceSGBaseTEUpdate) {
            this.transientDamage = nbt.getBoolean("transientDamage");
        } else {
            this.transientDamage = cfg.getBoolean("stargate", "transientDamage", this.requiresNoPower);
        }

        if (nbt.hasKey("transparency") && !SGCraft.forceSGBaseTEUpdate) {
            this.transparency = nbt.getBoolean("transparency");
        } else {
            this.transparency = cfg.getBoolean("stargate", "transparency", this.transparency);
        }

        if (nbt.hasKey("useDHDFuelSource") && !SGCraft.forceDHDCfgUpdate) {
            this.useDHDFuelSource = nbt.getBoolean("useDHDFuelSource");
        } else {
            this.useDHDFuelSource = cfg.getBoolean("dhd", "useDHDFuelSource", this.useDHDFuelSource);
        }

        this.facingDirectionOfBase = nbt.getInteger("facingDirectionOfBase");
        this.errorState = nbt.getBoolean("errorState");

        // Set values after NBT load
        this.ticksToStayOpen = 20 * this.secondsToStayOpen;
        updateEnergyCosts();

        // Access lists are always kept, whether or not the screens that edit them (MalisisCore) are installed, so a
        // restart without them doesn't lose the lists. Clients aren't sent them with the gate's other data (see
        // getUpdateTag), so a client keeps whatever an admin's Configurator was last sent.
        if (nbt.hasKey(PlayerAccessData.PLAYERACCESSLIST) || this.playerAccessData == null) {
            this.playerAccessData = PlayerAccessData.getPlayerAccessList(nbt);
        }
        if (nbt.hasKey(GateAccessData.GATEACCESSLIST) || this.gateAccessData == null) {
            this.gateAccessData = GateAccessData.getGateAccessList(nbt);
        }

        if (nbt.hasKey("defaultAllowIncoming") && !SGCraft.forceGateAccessSystemReset) {
            this.defaultAllowIncoming = nbt.getBoolean("defaultAllowIncoming");
        } else {
            this.defaultAllowIncoming = cfg.getBoolean("gate-access", "defaultAllowIncoming", this.defaultAllowIncoming);
        }

        if (nbt.hasKey("defaultAllowOutgoing") && !SGCraft.forceGateAccessSystemReset) {
            this.defaultAllowOutgoing = nbt.getBoolean("defaultAllowOutgoing");
        } else {
            this.defaultAllowOutgoing = cfg.getBoolean("gate-access", "defaultAllowOutgoing", this.defaultAllowOutgoing);
        }

        if (nbt.hasKey("defaultAllowGateAccess") && !SGCraft.forcePlayerAccessSystemReset) {
            this.defaultAllowGateAccess = nbt.getBoolean("defaultAllowGateAccess");
        } else {
            this.defaultAllowGateAccess = cfg.getBoolean("player-access", "defaultAllowGateAccess", this.defaultAllowGateAccess);
        }

        if (nbt.hasKey("defaultAllowIrisAccess") && !SGCraft.forcePlayerAccessSystemReset) {
            this.defaultAllowIrisAccess = nbt.getBoolean("defaultAllowIrisAccess");
        } else {
            this.defaultAllowIrisAccess = cfg.getBoolean("player-access", "defaultAllowIrisAccess", this.defaultAllowIrisAccess);
        }

        if (nbt.hasKey("defaultAllowAdminAccess") && !SGCraft.forcePlayerAccessSystemReset) {
            this.defaultAllowAdminAccess = nbt.getBoolean("defaultAllowAdminAccess");
        } else {
            this.defaultAllowAdminAccess = cfg.getBoolean("player-access", "defaultAllowAdminAccess", this.defaultAllowAdminAccess);
        }

        if (nbt.hasKey("allowRedstoneOutput") && !SGCraft.forceSGBaseTEUpdate) {
            this.allowRedstoneOutput = nbt.getBoolean("allowRedstoneOutput");
        } else {
            this.allowRedstoneOutput = cfg.getBoolean("stargate", "allowRedstoneOutput", this.allowRedstoneOutput);
        }

        if (nbt.hasKey("allowRedstoneInput") && !SGCraft.forceSGBaseTEUpdate) {
            this.allowRedstoneInput = nbt.getBoolean("allowRedstoneInput");
        } else {
            this.allowRedstoneInput = cfg.getBoolean("iris", "allowRedstoneInput", this.allowRedstoneInput);
        }

        this.invulnerable = nbt.getBoolean("invulnerable");
        if (nbt.hasKey("canPlayerBreakPlayerGate") && !SGCraft.forceSGBaseTEUpdate) {
            this.canPlayerBreakPlayerGate = nbt.getBoolean("canPlayerBreakPlayerGate");
        } else {
            this.canPlayerBreakPlayerGate = cfg.getBoolean("stargate", "canPlayerBreakPlayerGate", this.canPlayerBreakPlayerGate);
        }

        if (nbt.hasKey("canPlayerBreakGeneratedGate") && !SGCraft.forceSGBaseTEUpdate) {
            this.canPlayerBreakGeneratedGate = nbt.getBoolean("canPlayerBreakGeneratedGate");
        } else {
            this.canPlayerBreakGeneratedGate = cfg.getBoolean("stargate", "canPlayerBreakGeneratedGate", this.canPlayerBreakGeneratedGate);
        }

        if (nbt.hasKey("displayGateAddress") && !SGCraft.forceSGBaseTEUpdate) {
            this.displayGateAddress = nbt.getBoolean("displayGateAddress");
        } else {
            this.displayGateAddress = cfg.getBoolean("stargate", "displayGateAddress", this.displayGateAddress);
        }

        if (nbt.hasKey("isGenerated")) {
            this.isGenerated = nbt.getBoolean("isGenerated");
        } else {
            if (!world.isRemote) { // This lookup happens on the server first, then the client once it receives the packet.
                if (this.homeAddress != null && GeneratorAddressRegistry.hasAddress(this.world, this.homeAddress)) { // none for a gate out of the address range
                    this.isGenerated = true;
                }
            }
        }

        enforceOriRules();

        // Special Hotfix for forcing reset of isGenerated on a stargate IF the address is in the generators address registry
        if (!world.isRemote) {
            if (SGBaseTE.resetIsGenerated) {
                if (this.homeAddress != null && GeneratorAddressRegistry.hasAddress(this.world, this.homeAddress)) { // none for a gate out of the address range
                    this.isGenerated = true;
                }
            }
        }
    }

    protected String getStringOrNull(NBTTagCompound nbt, String name) {
        return nbt.hasKey(name) ? nbt.getString(name) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("isMerged", isMerged);
        nbt.setInteger("gateRadius", gateRadius);
        nbt.setInteger("state", state.ordinal());
        nbt.setDouble("ringAngle", ringAngle);
        nbt.setDouble("startRingAngle", startRingAngle);
        nbt.setDouble("targetRingAngle", targetRingAngle);
        nbt.setInteger("numEngagedChevrons", numEngagedChevrons);
        nbt.setString("dialledAddress", dialledAddress);
        nbt.setLong("pegasusFillStart", pegasusFillStart);
        nbt.setLong("pegasusFillEnd", pegasusFillEnd);
        nbt.setBoolean("isLinkedToController", isLinkedToController);
        NBTTagList zpmList = new NBTTagList();
        for (BlockPos p : linkedZpms) {
            zpmList.appendTag(new NBTTagLong(p.toLong()));
        }
        nbt.setTag("linkedZpms", zpmList);
        nbt.setInteger("linkedX", linkedPos.getX());
        nbt.setInteger("linkedY", linkedPos.getY());
        nbt.setInteger("linkedZ", linkedPos.getZ());
        nbt.setBoolean("isInitiator", isInitiator);
        nbt.setInteger("timeout", timeout);
        nbt.setInteger("maxTimeout", maxTimeout);
        nbt.setDouble("energyInBuffer", energyInBuffer);
        nbt.setDouble("distanceFactor", distanceFactor);
        nbt.setInteger("irisState", irisState.ordinal());
        nbt.setInteger("irisPhase", irisPhase);
        nbt.setBoolean("redstoneInput", redstoneInput);

        // Configurator Options
        nbt.setBoolean("hasIrisUpgrade", this.hasIrisUpgrade);
        nbt.setBoolean("hasShield", this.hasShield);
        nbt.setBoolean("hasChevronUpgrade", this.hasChevronUpgrade);
        nbt.setInteger("gateType", this.gateType);
        nbt.setInteger("secondsToStayOpen", this.secondsToStayOpen);
        nbt.setBoolean("oneWayTravel", this.oneWayTravel);
        nbt.setDouble("ringRotationSpeed", this.ringRotationSpeed);
        nbt.setDouble("maxEnergyBuffer", this.maxEnergyBuffer);
        nbt.setDouble("energyPerFuelItem", this.energyPerFuelItem);
        nbt.setInteger("gateOpeningsPerFuelItem", this.gateOpeningsPerFuelItem);
        nbt.setDouble("distanceFactorMultiplier", this.distanceFactorMultiplier);
        nbt.setDouble("interDimensionMultiplier", this.interDimensionMultiplier);
        nbt.setBoolean("reverseWormholeKills", this.reverseWormholeKills);
        nbt.setBoolean("closeFromEitherEnd", this.closeFromEitherEnd);
        nbt.setBoolean("preserveInventory", this.preserveInventory);
        nbt.setBoolean("allowIncomingConnections", this.allowIncomingConnections);
        nbt.setBoolean("allowOutgoingConnections", this.allowOutgoingConnections);
        nbt.setBoolean("chevronsLockOnDial", this.chevronsLockOnDial);
        nbt.setBoolean("returnToPreviousIrisState", this.returnToPreviousIrisState);
        nbt.setInteger("gateOrientation", this.gateOrientation);
        nbt.setInteger("facingDirectionOfBase", this.facingDirectionOfBase);
        nbt.setBoolean("requiresNoPower", this.requiresNoPower);
        nbt.setBoolean("transientDamage", this.transientDamage);
        nbt.setBoolean("transparency", this.transparency);
        nbt.setBoolean("errorState", this.errorState);
        nbt.setBoolean("useDHDFuelSource", this.useDHDFuelSource);

        if (connectedLocation != null) {
            nbt.setTag("connectedLocation", connectedLocation.toNBT());
        }
        if (homeAddress != null) {
            nbt.setString("address", homeAddress);
        }
        if (addressError != null) {
            nbt.setString("addressError", addressError);
        }

        // Access Lists...
        if (this.playerAccessData != null)
            PlayerAccessData.writeAddresses(nbt, this.playerAccessData);

        if (this.gateAccessData != null)
            GateAccessData.writeAddresses(nbt, this.gateAccessData);

        nbt.setBoolean("defaultAllowIncoming", this.defaultAllowIncoming);
        nbt.setBoolean("defaultAllowOutgoing", this.defaultAllowOutgoing);
        nbt.setBoolean("defaultAllowGateAccess", this.defaultAllowGateAccess);
        nbt.setBoolean("defaultAllowIrisAccess", this.defaultAllowIrisAccess);
        nbt.setBoolean("defaultAllowAdminAccess", this.defaultAllowAdminAccess);
        nbt.setBoolean("allowRedstoneOutput", this.allowRedstoneOutput);
        nbt.setBoolean("allowRedstoneInput", this.allowRedstoneInput);
        nbt.setBoolean("canPlayerBreakPlayerGate", this.canPlayerBreakPlayerGate);
        nbt.setBoolean("canPlayerBreakGeneratedGate", this.canPlayerBreakGeneratedGate);
        nbt.setBoolean("invulnerable", this.invulnerable);
        nbt.setBoolean("displayGateAddress", this.displayGateAddress);
        nbt.setBoolean("isGenerated", isGenerated);

        return nbt;
    }

    // *********************************************************************************************
    // Core Stargate Control Systems
    // *********************************************************************************************

    private String tryToGetHomeAddress() {
        try {
            return getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            return null;
        }
    }

    //public int dimension() {
    //    return this.world != null ? world.provider.getDimension() : -999;
    //}

    public boolean isActive() {
        return state != SGState.Idle && state != SGState.Disconnecting;
    }

    //static boolean isValidSymbolChar(String c) {
    //    return SGAddressing.isValidSymbolChar(c);
    //}

    public static char symbolToChar(int i) {
        return SGAddressing.symbolToChar(i);
    }

    public static int charToSymbol(char c) {
        return SGAddressing.charToSymbol(c);
    }

    //static int charToSymbol(String c) {
    //    return SGAddressing.charToSymbol(c);
    //}

    // Seven, or nine with the chevron upgrade, whatever the type of gate
    public int getNumChevrons() {
        return hasChevronUpgrade || getGateType() == GateType.ORI ? 9 : 7; // an Ori gate always reaches other worlds
    }

    // How many ZPMs a supergate (Ori or not) needs near its base to dial or be dialled, and which it runs on alone
    public static final int supergateZpmsNeeded = 3;

    // The ZPMs this gate runs on, with energy left: in its base's slots, if a supergate, and nearby (in ZPM consoles,
    // and with IC2 carts and hubs)
    public List<ISGEnergySource> zpmSources() {
        List<ISGEnergySource> sources = new ArrayList<>();
        if (isSupergate()) {
            for (int i = 0; i < numZpmSlots; i++) {
                ZpmSlotSource source = new ZpmSlotSource(this, firstZpmSlot + i);
                if (!source.isEmpty()) {
                    sources.add(source);
                }
            }
        }
        sources.addAll(ZpmAddon.findZpmSources(world, pos, SGCraft.zpmSearchRange));
        addLinkedZpms(sources);
        return sources;
    }

    // ---- ZPM devices linked to this gate (see ZpmLinks), which it draws on wherever they stand ----

    private final List<BlockPos> linkedZpms = new ArrayList<>();

    public void addLinkedZpm(BlockPos devicePos) {
        if (!linkedZpms.contains(devicePos)) {
            linkedZpms.add(devicePos.toImmutable());
            markChanged(); // clients too: the Configurator lists them
        }
    }

    public void removeLinkedZpm(BlockPos devicePos) {
        if (linkedZpms.remove(devicePos)) {
            markChanged();
        }
    }

    // The devices still linked to it; any that are gone, or now linked elsewhere, are let go
    public List<ILinkableZpm> linkedZpmDevices() {
        List<ILinkableZpm> devices = new ArrayList<>();
        for (BlockPos p : new ArrayList<>(linkedZpms)) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof ILinkableZpm && ((ILinkableZpm) te).getZpmLink().isLinkedTo(pos)) {
                devices.add((ILinkableZpm) te);
            } else {
                removeLinkedZpm(p);
            }
        }
        return devices;
    }

    // Where the devices linked to it are, as last saved or sent (on a client, for the Configurator)
    public List<BlockPos> linkedZpmPositions() {
        return new ArrayList<>(linkedZpms);
    }

    // Adds the linked devices with energy to sources, leaving out any already there (nearby ones are found anyway)
    private void addLinkedZpms(List<ISGEnergySource> sources) {
        if (linkedZpms.isEmpty() || world == null || world.isRemote) {
            return;
        }
        for (ILinkableZpm device : linkedZpmDevices()) {
            if (!device.hasNoZpm() && !sources.contains(device)) {
                sources.add(device);
            }
        }
    }

    // The energy in ZPMs it can draw on: near its base, linked to it, and in a supergate's own slots (left out before, so
    // a supergate running on those alone couldn't dial a route that needs a ZPM)
    public double zpmPowerAvailable() {
        double power = 0;
        for (ISGEnergySource source : zpmSources()) {
            power += source.availableEnergy();
        }
        return power;
    }

    public int zpmCount() {
        return zpmSources().size();
    }

    // The energy those ZPMs hold
    public double zpmEnergy() {
        double energy = 0;
        for (ISGEnergySource source : zpmSources()) {
            energy += source.availableEnergy();
        }
        return energy;
    }

    // What this gate takes to open a wormhole to a gate close by, and to hold it open a tick; further away, more
    // (see distanceFactorForCoordDifference)
    public double nearbyOpenCost() {
        return requiresNoPower ? 0 : energyPerFuelItem / gateOpeningsPerFuelItem * sizeEnergyMultiplier();
    }

    public double nearbyTickCost() {
        return requiresNoPower ? 0 : energyPerFuelItem / (minutesOpenPerFuelItem * 60 * 20) * sizeEnergyMultiplier();
    }

    // An Ori supergate has no iris, shield or chevron upgrade (it has all nine), never locks its chevrons as it's
    // dialled, and runs on its ZPMs, not a DHD
    public void applyOriRules() {
        if (enforceOriRules() && world != null && !world.isRemote) {
            updateIrisEntity();
        }
    }

    // The same, without touching the world (as when loading); true if it's an Ori gate
    public boolean enforceOriRules() {
        if (getGateType() != GateType.ORI) {
            return false;
        }
        hasIrisUpgrade = false;
        hasShield = false;
        hasChevronUpgrade = true;
        chevronsLockOnDial = false;
        useDHDFuelSource = false;
        return true;
    }

    // How far (0 to 1) the gate has got through dialling the symbol it's on, if dialling one
    public double symbolDialProgress(float partialTicks) {
        if (state != SGState.Dialling || maxTimeout <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, (maxTimeout - timeout + partialTicks) / maxTimeout));
    }

    public boolean chevronIsEngaged(int i) {
        return i < numEngagedChevrons;
    }

    public float angleBetweenChevrons() {
        if (getGateType() == GateType.UNIVERSE) {
            // Evenly round the ring, one over each section of its symbols, as on the show
            return (float) universeGroupAngle;
        } else if (getGateType() == GateType.MILKY_WAY || getGateType() == GateType.PEGASUS) {
            // Evenly round the ring, where its model has places for them, as on the show; without
            // the chevron upgrade, the bottom two are left out
            return 360f / 9;
        } else if (variableChevronPositions) {
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

    public void setMerged(boolean state) {
        if (isMerged != state) {
            isMerged = state;
            markBlockChanged();

            String address = tryToGetHomeAddress();
            if (address != null) {
                Logger log = LogManager.getLogger();
                String action = isMerged ? "ADDED" : "REMOVED";
                String name = getWorld().getWorldInfo().getWorldName();
                if (isMerged) {
                    this.homeAddress = address;
                }
                if (logStargateEvents) {
                    log.info(String.format("STARGATE %s %s %s %s", action, name, pos, address));
                }
                SGMergeEvent.fireEvent(this.homeAddress, isMerged, this.pos, getWorld());
                if (state) {
                    GeneralAddressRegistry.addAddress(world, this.homeAddress);
                } else {
                    GeneratorAddressRegistry.removeAddress(world, this.homeAddress); // This will remove the address from the generation registry.
                    GeneralAddressRegistry.removeAddress(world, this.homeAddress);
                }
            }

            updateIrisEntity();
        }
    }

    @Override
    public void update() {
        if (world.isRemote) {
            clientUpdate();
        } else {
            lockIncomingChevrons();
            tickLockDial();
            serverUpdate();
            checkForEntitiesInPortal();
            clearKawooshBlocks();
            tickShieldEnergy();
        }
        irisUpdate();
        updateEventHorizonLight();
    }

    // *********************************************************************************************
    // Event horizon light: while the event horizon is up and the iris isn't closed over it, the
    // gate's blocks (its base and ring) give off light, lighting the world round the gate. Server
    // and client each relight their own world as the gate's state changes.
    // *********************************************************************************************

    // -1 until the gate's first tick, which always relights round it: a gate whose chunk was unloaded while lit (it is
    // disconnected then, but no longer ticks to put its light out) was saved with the light still in the world
    private int eventHorizonLight = -1;

    // The light the gate's blocks give off now (see SGBaseBlock and SGRingBlock getLightValue)
    public int getEventHorizonLight() {
        return Math.max(0, eventHorizonLight);
    }

    private int targetEventHorizonLight() {
        if (!isMerged || SGCraft.eventHorizonLightLevel <= 0) {
            return 0;
        }
        // Block light shines every way from the gate's blocks, so with the iris closed over the front they go dark,
        // and hidden light blocks behind the gate light the back instead (see irisLightPositions)
        return eventHorizonUp() && !irisIsClosed() ? Math.min(15, SGCraft.eventHorizonLightLevel) : 0;
    }

    private boolean eventHorizonUp() {
        return state == SGState.Transient || state == SGState.Connected || state == SGState.Disconnecting;
    }

    // Where the light behind a closed iris goes: just behind the middle of the opening, and for a gate big enough,
    // halfway out from there each way. Empty while the iris isn't closed over a live event horizon.
    public List<BlockPos> irisLightPositions() {
        List<BlockPos> positions = new ArrayList<>();
        if (!isMerged || SGCraft.eventHorizonLightLevel <= 0 || !eventHorizonUp() || !irisIsClosed()) {
            return positions;
        }
        GateShape shape = getShape();
        Trans3 t = localToGlobalTransformation();
        int cy = (int) Math.floor(shape.centreY + 0.5);
        int k = (int) Math.floor(shape.ringInnerRadius / 2);
        int[][] offsets = k > 0 ? new int[][] {{0, 0}, {k, 0}, {-k, 0}, {0, k}, {0, -k}} : new int[][] {{0, 0}};
        for (int[] o : offsets) {
            positions.add(t.p(fromGateFrame(new Vector3(o[0], cy + o[1], -1))).blockPos()); // z -1: behind the gate
        }
        return positions;
    }

    // The light blocks this gate has placed, to remove once no longer wanted
    private final List<BlockPos> placedIrisLights = new ArrayList<>();

    // Server only: puts the light behind a closed iris where it should be, into empty air only, and takes away any no
    // longer wanted. Run when the iris or the connection changes, and every second to replace any broken.
    private void updateIrisLights() {
        List<BlockPos> wanted = irisLightPositions();
        if (wanted.equals(placedIrisLights) && world.getTotalWorldTime() % 20 != 0) {
            return;
        }
        Block lightBlock = SGCraft.eventHorizonLightBlock;
        for (BlockPos p : placedIrisLights) {
            if (!wanted.contains(p) && world.isBlockLoaded(p) && world.getBlockState(p).getBlock() == lightBlock) {
                world.setBlockToAir(p);
            }
        }
        placedIrisLights.clear();
        if (lightBlock == null) {
            return;
        }
        IBlockState light = lightBlock.getDefaultState().withProperty(EventHorizonLightBlock.LIGHT, Math.min(15, SGCraft.eventHorizonLightLevel));
        for (BlockPos p : wanted) {
            if (!world.isBlockLoaded(p)) {
                continue;
            }
            IBlockState current = world.getBlockState(p);
            if (current.getBlock() == lightBlock || world.isAirBlock(p)) {
                if (current != light) {
                    world.setBlockState(p, light, 3);
                }
                placedIrisLights.add(p);
            }
        }
    }

    // Whether any gate near pos wants an iris light there (see EventHorizonLightBlock, which removes itself if not).
    // Looks only in loaded chunks, without loading any.
    public static boolean wantsIrisLightAt(World world, BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                if (!world.isBlockLoaded(new BlockPos(x << 4, 0, z << 4))) {
                    continue;
                }
                for (TileEntity te : world.getChunk(x, z).getTileEntityMap().values()) {
                    if (te instanceof SGBaseTE && ((SGBaseTE) te).irisLightPositions().contains(pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // When the gate last connected, disconnected or its iris opened or closed, and when to sweep the light round it
    private boolean lastHorizonUp, lastIrisClosed;
    private long areaRelightAt = -1, areaRelightAgainAt = -1;

    // How far round the gate (blocks, each way from the middle of its ring) leftover light is cleared
    private static final int AREA_RELIGHT_RANGE = 30;

    // Clears leftover light round the gate. Lighting is worked out a block at a time, so when the gate's light sources
    // change together some light can be left with nothing to give it. Every block in range still holding block light is
    // checked: one whose light has no source loses it, along with all it lit. Server and client each sweep their world.
    private void relightArea() {
        Vector3 c = localToGlobalTransformation().p(ringCentre());
        int cx = (int) Math.floor(c.x), cy = (int) Math.floor(c.y), cz = (int) Math.floor(c.z), r = AREA_RELIGHT_RANGE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                if (!world.isBlockLoaded(p.setPos(x, 64, z))) {
                    continue;
                }
                for (int y = Math.max(0, cy - r); y <= Math.min(255, cy + r); y++) {
                    p.setPos(x, y, z);
                    if (world.getLightFor(EnumSkyBlock.BLOCK, p) > 0) {
                        world.checkLightFor(EnumSkyBlock.BLOCK, p.toImmutable());
                    }
                }
            }
        }
        if (world.isRemote) {
            world.markBlockRangeForRenderUpdate(cx - r, cy - r, cz - r, cx + r, cy + r, cz + r);
        }
    }

    private void updateEventHorizonLight() {
        if (!world.isRemote && !isRemoteView) {
            updateIrisLights();
        }
        if (!isRemoteView) {
            // Sweep a moment after the gate connects, disconnects or its iris opens or closes, once the light sources
            // have changed (the iris lights reach clients a little later), and again a second later
            boolean horizonUp = isMerged && eventHorizonUp(), irisClosed = irisIsClosed();
            long now = world.getTotalWorldTime();
            if (horizonUp != lastHorizonUp || irisClosed != lastIrisClosed) {
                lastHorizonUp = horizonUp;
                lastIrisClosed = irisClosed;
                areaRelightAt = now + 3;
                areaRelightAgainAt = now + 20;
            }
            if (now == areaRelightAt || now == areaRelightAgainAt) {
                relightArea();
            }
        }
        int light = targetEventHorizonLight();
        if (light != eventHorizonLight && !isRemoteView) {
            boolean dimming = eventHorizonLight < 0 || light < eventHorizonLight;
            eventHorizonLight = light;
            relightGate();
            if (dimming) {
                // All the gate's blocks go dark at once, but are relit one at a time, so the first ones pick light
                // back up from the rest; a second pass clears that, rather than leaving it for Minecraft's slow
                // background light checks (which took tens of seconds)
                relightGate();
            }
            if (world.isRemote) {
                // Redraw everything the light reaches straight away
                int r = 16 + Math.max(getShape().halfWidth, getShape().height);
                world.markBlockRangeForRenderUpdate(pos.add(-r, -r, -r), pos.add(r, r, r));
            }
        }
    }

    // Has the world work out the light round the gate again: at its base and each block of its ring
    private void relightGate() {
        world.checkLight(pos);
        GateShape shape = getShape();
        Trans3 t = new Trans3(Vector3.blockCenter(pos)).turn(facingDirectionOfBase);
        for (int x = -shape.halfWidth; x <= shape.halfWidth; x++) {
            for (int y = 0; y < shape.height; y++) {
                if (shape.cellAt(x, y) != GateShape.EMPTY && !(x == 0 && y == 0)) {
                    // Standing up or lying flat, as SGBaseBlock.unmergeRing visits the ring
                    relightRingBlock(t.p(x, y, 0).blockPos());
                    relightRingBlock(t.p(x, 0, -y).blockPos());
                }
            }
        }
    }

    private void relightRingBlock(BlockPos ringPos) {
        TileEntity te = world.isBlockLoaded(ringPos) ? world.getTileEntity(ringPos) : null;
        if (te instanceof SGRingTE && ((SGRingTE) te).isMerged && pos.equals(((SGRingTE) te).basePos)) {
            world.checkLight(ringPos);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (!world.isRemote && ocWirelessEndpoint != null) {  //[OC]
            ocWirelessEndpoint.remove();
        }
    }

    // A chunk unloading doesn't invalidate its tile entities, and the chunk brings a new one (with an endpoint of its
    // own) when it loads again: the old one's endpoint, holding the old gate, stayed in OpenComputers' wireless network
    // for as long as the world was loaded
    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (!world.isRemote && ocWirelessEndpoint != null) {  //[OC]
            ocWirelessEndpoint.remove();
            ocWirelessEndpoint = null;
        }
    }

    public void onNeighborBlockChange() {
        if (!world.isRemote) {
            boolean newInput = BaseBlockUtils.blockIsGettingExternallyPowered(world, pos);
            if (redstoneInput != newInput) {
                redstoneInput = newInput;
                markDirty();
                if (this.allowRedstoneInput) {
                    if (redstoneInput) {
                        closeIris();
                    } else {
                        openIris();
                    }
                }
            }
        }
    }

    private String side() {
        return world.isRemote ? "Client" : "Server";
    }

    private void enterState(SGState newState, int newTimeout) {
        if (debugState) {
            System.out.printf("SGBaseTE: at %s in dimension %s entering state %s with timeout %s\n", pos, world.provider.getDimension(), newState, newTimeout);
        }
        SGState oldState = state;
        state = newState;
        startRingAngle = ringAngle;
        maxTimeout = newTimeout;
        timeout = newTimeout;
        markChanged();
        if (debugDialSync && !world.isRemote) {
            dialLog.info(String.format("[SGCraft dial] server %s (was %s)", dialSyncText(), oldState));
        }
        if (((oldState == SGState.Idle) != (newState == SGState.Idle)) || (oldState == SGState.attemptToDial != (newState == SGState.attemptToDial))) {
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

    public DHDTE getLinkedControllerTE() {
        //System.out.printf("SGBaseTE.getLinkedControllerTE: isLinkedToController = %s, linkedPos = %s\n",
        //    isLinkedToController, linkedPos);
        if (isLinkedToController) {
            TileEntity cte = world.getTileEntity(linkedPos);
            if (cte instanceof DHDTE)
                return (DHDTE)cte;
        }
        return null;
    }

    public void checkForLink() {
        int rangeXY = max(DHDTE.linkRangeX, DHDTE.linkRangeY);
        int rangeZ = DHDTE.linkRangeZ;
        if (SGBaseBlock.debugMerge) {
            System.out.printf("SGBaseTE.checkForLink: in range +/-(%d,%d,%d) of %s\n", rangeXY, rangeZ, rangeXY, pos);
        }
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
        if (SGBaseBlock.debugMerge) {
            System.out.printf("SGBaseTE: Unlinking stargate at %d from controller\n", pos);
        }
        isLinkedToController = false;
        markDirty();
    }

    //------------------------------------   Server   --------------------------------------------

    public void connectOrDisconnect(String address, EntityPlayer player) {
        if (debugConnect) {
            System.out.printf("SGBaseTE: %s: connectOrDisconnect('%s') in state %s by %s\n", side(), address, state, player);
        }
        if (address.length() > 0) {
            DHDTE te = getLinkedControllerTE();
            if (te != null) {
                if (connect(address, player, false, false) != null) {
                    numEngagedChevrons = 0;
                    markChanged();
                }
            }
        } else {
            disconnect(player);
        }

    }

    public String disconnect(EntityPlayer player) {
        boolean canDisconnect = disconnectionAllowed();
        SGBaseTE dte = getConnectedStargateTE();
        // Todo: remove this if result is intended after test.
        //boolean validConnection = dte != null && !dte.isInvalid() && dte.getConnectedStargateTE() == this;
        //if (canDisconnect || !validConnection) {
        if (canDisconnect) {
            if (state != SGState.Disconnecting)
                disconnect();
            return null;
        } else {
            return operationFailure(player, "incomingConnection");
        }
    }

    public boolean disconnectionAllowed() {
        if (this.isInitiator) {
            return true;
        } else {
            if (closeFromEitherEnd) {
                return true;
            }
        }
        return false;
    }

    // *************************************
    // Connect Here!!!
    // *************************************

    public String connect(String address, EntityPlayer player, boolean ccInterface, boolean pending) {
        // Note:  if player is null when introduced to this method, then it usually indicates a CI attempting to dial.
        // Note:  ccInterface == true so it stops the immediate chevron lock if a ccInterface is the one doing the dialing.
        debugEnergyUse = false;
        if (state != SGState.Idle) {
            return diallingFailure(player, "selfBusy");
        }
        // A base without its ring: it can't dial (its state never moves on), and would leave the gate it dialled waiting
        // for good, its chunks loaded
        if (!isMerged) {
            return diallingFailure(player, "selfIncomplete");
        }
        String homeAddress = findHomeAddress();
        if (homeAddress.equals("")) {
            return diallingFailure(player, "selfOutOfRange");
        }

        boolean isPermissionsAdmin = false;

        if (player != null) {
            isPermissionsAdmin = SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin"); // Fallback for a full permissions system override to the Access System

            // Player Access Control System
            if (!this.allowGateAccess(player)) {
                if (!isPermissionsAdmin) return diallingFailure(player, "accessFromDenied");
            }
        }

        SGBaseTE targetGate;
        try {
            targetGate = SGAddressing.findAddressedStargate(address, world, pending);
        } catch (SGAddressing.AddressingError e) {
            return diallingFailure(player, e.getMessage());
        }
        String fullAddress = SGAddressing.fullAddress(address, world.provider.getDimension()); // a local one is in this world
        if (targetGate != null && !targetGate.isMerged && GeneratorAddressRegistry.isGenerated(fullAddress)) {
            // A generated gate whose ring is whole can still have been left unmerged: its blocks are placed as the world is
            // made, which needn't let each one look for the rest. Look again now.
            World targetWorld = targetGate.getWorld();
            BlockPos targetPos = targetGate.getPos();
            Block targetBlock = targetWorld.getBlockState(targetPos).getBlock();
            if (targetBlock instanceof SGBaseBlock) {
                ((SGBaseBlock) targetBlock).checkForMerge(targetWorld, targetPos);
                SGBaseTE merged = SGBaseTE.at(targetWorld, targetPos);
                if (merged != null) {
                    targetGate = merged;
                }
                if (targetGate.isMerged) {
                    System.out.printf("SGCraft: generated stargate %s at %s in %s wasn't merged; merged it\n", fullAddress,
                        targetPos, targetWorld.getWorldInfo().getWorldName());
                } else if (SGBaseBlock.repairGeneratedGates) {
                    // Its ring is broken or was only half built (earlier versions could leave one so): put it back
                    int placed = ((SGBaseBlock) targetBlock).repairRing(targetWorld, targetPos);
                    merged = SGBaseTE.at(targetWorld, targetPos);
                    if (merged != null) {
                        targetGate = merged;
                    }
                    if (placed >= 0 && targetGate.isMerged) {
                        System.out.printf("SGCraft: repaired generated stargate %s at %s in %s: put back %d ring blocks\n",
                            fullAddress, targetPos, targetWorld.getWorldInfo().getWorldName(), placed);
                    } else {
                        System.out.printf("SGCraft: couldn't repair generated stargate %s at %s in %s: a ring position is in "
                            + "a chunk never generated or holds something that can't be replaced\n",
                            fullAddress, targetPos, targetWorld.getWorldInfo().getWorldName());
                    }
                }
            }
        }
        if (targetGate != null && !targetGate.isMerged && GeneratorAddressRegistry.isGenerated(fullAddress)) {
            System.out.printf("SGCraft: generated stargate %s at %s in %s isn't complete (its ring isn't whole), so it "
                + "can't be dialled\n", fullAddress, targetGate.getPos(), targetGate.getWorld().getWorldInfo().getWorldName());
        }
        if (targetGate == null) {
            // A generated gate that's gone (broken some way that didn't take it off the list, or its world reset) is
            // taken off the list (generator.yml, and so the EDD's), so it isn't offered again
            if (GeneratorAddressRegistry.isGenerated(fullAddress) && SGAddressing.noGateAt(fullAddress)
                    && GeneratorAddressRegistry.removeEverywhere(fullAddress)) {
                System.out.printf("SGCraft: generated stargate %s is gone; removed it from generator.yml\n", fullAddress);
                return diallingFailure(player, "generatedGateGone", address);
            }
        }
        if (targetGate == null || !targetGate.isMerged) {
            return diallingFailure(player, "unknownAddress", address);
        }
        if (getWorld() == targetGate.getWorld()) {
            address = SGAddressing.localAddress(address);
            homeAddress = SGAddressing.localAddress(homeAddress);
        }
        if (!targetGate.allowIncomingConnections) {
            return diallingFailure(player, "cannotDialToThisGate", address);
        }
        if (!this.allowOutgoingConnections) {
            return diallingFailure(player, "cannotDialFromThisGate", address);
        }
        if (address.length() > getNumChevrons()) {
            return diallingFailure(player, "selfLackChevrons", address);
        }
        if (targetGate == this) {
            return diallingFailure(player, "diallingItself");
        }

        // Player Access Control System
        if (player != null && !targetGate.allowGateAccess(player)) {
            if (!isPermissionsAdmin) return diallingFailure(player, "playerAccessToDenied");
        }

        // Gate Address Control System. Both gates' full addresses are checked, not what was dialled: within one
        // dimension that is only the 7-symbol local address, which would never match a list entry.
        if (!this.allowOutgoingAddress(targetGate.homeAddress, this.defaultAllowOutgoing)) {
            if (!isPermissionsAdmin)
                return diallingFailure(player, "accessSystemDeniedOrigin");
        }

        // Whether the gate being dialled accepts this one is up to its own list
        if (!targetGate.allowIncomingAddress(this.homeAddress, targetGate.defaultAllowIncoming)) {
            if (!isPermissionsAdmin)
                return diallingFailure(player, "accessSystemDeniedDestination");
        }
        // End Access Control System

        if (debugConnect) {
            System.out.printf("SGBaseTE.connect: to %s in dimension %d with state %s\n", targetGate.getPos(), targetGate.getWorld().provider.getDimension(), targetGate.state);
        }
        if (targetGate.getNumChevrons() < homeAddress.length()) {
            return diallingFailure(player, "targetLackChevrons");
        }
        if (targetGate.state != SGState.Idle) {
            return diallingFailure(player, "targetBusy", address);
        }
        // An Ori supergate only links with another; no other gate can reach one
        if ((getGateType() == GateType.ORI) != (targetGate.getGateType() == GateType.ORI)) {
            return diallingFailure(player, "oriOnlyOri", address);
        }
        // A supergate needs its ZPMs, at either end
        if (isSupergate() && !requiresNoPower && zpmCount() < supergateZpmsNeeded) {
            return diallingFailure(player, "supergateNeedsZpms", supergateZpmsNeeded);
        }
        if (targetGate.isSupergate() && !targetGate.requiresNoPower && targetGate.zpmCount() < supergateZpmsNeeded) {
            return diallingFailure(player, "targetSupergateNeedsZpms", supergateZpmsNeeded);
        }
        distanceFactor = distanceFactorForCoordDifference(this, targetGate);
        if (debugEnergyUse) {
            System.out.printf("SGBaseTE: distanceFactor = %s\n", distanceFactor);
        }

        //Reset this value:
        if (!this.requiresNoPower) {
            energyToOpen = energyPerFuelItem / gateOpeningsPerFuelItem;
            if (debugEnergyUse) {
                System.out.println("EnergyToOpen: " + energyToOpen);
            }
        } else {
            energyToOpen = 0;
        }

        if (!this.requiresNoPower) {
            // Zpm
            String originName = this.getWorld().getWorldInfo().getWorldName().toLowerCase();
            String destinationName = targetGate.getWorld().getWorldInfo().getWorldName().toLowerCase();

            if (ZpmAddon.routeRequiresZpm(originName, destinationName)) {
                long power = (long) zpmPowerAvailable();
                if (!(power > 0)) {
                    return diallingFailure(player, "zpmNotFound");
                } else {
                    if (!(power > energyToOpen * distanceFactor)) {
                        return diallingFailure(player, "zpmLowPower", energyText(energyToOpen * distanceFactor),
                            energyText(energyUsePerTick * distanceFactor * 20), energyText(power));
                    }
                }
                this.syncAwaitTime = 30; // Sets longer connect delay due to distance
                this.destinationRequiresZPM = true;

            } else {
                this.syncAwaitTime = 10; // Sets short connect delay for in-world Stargates
                this.destinationRequiresZPM = false;
            }

            if (debugEnergyUse || debugZPM) {
                System.out.println("-------------------   Power Usage Debug   --------------------------");
                System.out.println("EnergyPerFuelItem: " + dFormat.format(energyPerFuelItem));
                System.out.println("Gate Openings Per Fuel: " + gateOpeningsPerFuelItem);
                System.out.println("SGPU Energy to Open with Distance Factor: " + dFormat.format(energyToOpen * distanceFactor));
                System.out.println("--------------------------------------------------------------------");
                System.out.println("IC2 Energy to Open with Distance Factor: " + dFormat.format((energyToOpen * distanceFactor) * SGCraft.Ic2euPerSGEnergyUnit));
                System.out.println("--------------------------------------------------------------------");
                System.out.println("ZPM Required: " + this.destinationRequiresZPM);
                System.out.println("ZPM Multiplier: " + ZpmAddon.routeZpmMultiplier(originName, destinationName));
                System.out.println("--------------------------------------------------------------------");
                System.out.println("Energy to Open: " + energyToOpen);
                System.out.println("Distance Factor: " + distanceFactor);
                System.out.println("--------------------------------------------------------------------");
                System.out.println("Energy Available: " + availableEnergy());
                System.out.println("Energy Required: " + (energyToOpen * distanceFactor));
                System.out.println("Energy per Tick: " + energyUsePerTick);
                System.out.println("Energy Used per Tick: " + (energyUsePerTick * distanceFactor));
                System.out.println("--------------------------------------------------------------------");
            }
        }

        // Final Power check before dial
        // Saying what it would take: to dial, and to stay open each second (in Forge Energy, as the power units show)
        if (!this.requiresNoPower && !energyIsAvailable(energyToOpen * distanceFactor)) {
            return diallingFailure(player, "insufficientEnergy", energyText(energyToOpen * distanceFactor),
                energyText(energyUsePerTick * distanceFactor * 20), energyText(availableEnergy()));
        }

        if (this.hasIrisUpgrade) {
            this.wasIrisClosed = this.irisIsClosed();
        }

        if (targetGate.hasIrisUpgrade) { // was hasChevronUpgrade
            targetGate.wasIrisClosed = targetGate.irisIsClosed();
        }

        if (!GeneralAddressRegistry.addressExists(world, this.homeAddress)) {
            GeneralAddressRegistry.addAddress(world, this.homeAddress);
        }
        // The following will keep the target gate from invalidating itself.
        startDiallingStargate(address, targetGate, true, (this.chevronsLockOnDial && !ccInterface));

        targetGate.isPending = true;
        targetGate.enterState(SGState.attemptToDial, 5); // Force remote gate immediate change state to help chunk stay loaded
        // Disabled this on 7/21/2020 - Doc
        // Note to Dockter: added a check in update the dialing status; which is now causing a double chunk ticket load; because for some reason
        // the Computer interface is causing the TE's to double load?  Makes no sense, I know....
        targetGate.startDiallingStargate(homeAddress, this, false, (this.chevronsLockOnDial && !ccInterface));
        // This gate started dialling its first symbol before the one it's dialling was told of the connection, which has
        // just reset it: an Ori gate being dialled takes up linking where this one has got to
        if (state == SGState.Dialling) {
            syncOriSymbol(timeout, maxTimeout);
        }

        if (this.isInitiator) {
            checkForMalfunction = true;
        }

        // Transient Block Damage
        this.performBlockDamage = true;
        targetGate.performBlockDamage = true;

        return null;
    }

    public double distanceFactorForCoordDifference(TileEntity te1, TileEntity te2) {
        double dx = te1.getPos().getX() - te2.getPos().getX();
        double dz = te1.getPos().getZ() - te2.getPos().getZ();
        double d = Math.sqrt(dx * dx + dz * dz);

        if (debugEnergyUse) {
            System.out.printf("SGBaseTE: Connection distance = %s\n", d);
        }

        double ld = Math.log(0.05 * d + 1);
        double lm = Math.log(0.05 * 16 * SGAddressing.coordRange);
        double lr = ld / lm;
        double f = 1 + 14 * distanceFactorMultiplier * lr * lr;
        if (te1.getWorld() != te2.getWorld()) {
            f *= distanceFactorMultiplier;
            String originName = te1.getWorld().getWorldInfo().getWorldName().toLowerCase();
            String destinationName = te2.getWorld().getWorldInfo().getWorldName().toLowerCase();

            if (ZpmAddon.routeRequiresZpm(te1.getWorld().getWorldInfo().getWorldName().toLowerCase(), te2.getWorld().getWorldInfo().getWorldName().toLowerCase())) {
                f += ZpmAddon.routeZpmMultiplier(originName, destinationName);
            }
        }
        // A wormhole to or from a supergate costs more, whichever end dials
        if (te1 instanceof SGBaseTE && te2 instanceof SGBaseTE) {
            f *= Math.max(((SGBaseTE) te1).sizeEnergyMultiplier(), ((SGBaseTE) te2).sizeEnergyMultiplier());
        }
        return f;
    }

    public String diallingFailure(EntityPlayer player, String msg, Object... args) {
        if (player != null) {
            if (state == SGState.Idle) {
                playSGSoundEffect(getGateType().select(m_dialFailSound, p_dialFailSound), 1F, 1F);
            }
        }
        errorState = true;
        return operationFailure(player, msg, args);
    }

    public String operationFailure(EntityPlayer player, String msg, Object... args) {
        if (player != null)
            sendErrorMsg(player, msg, args);
        return msg;
    }

    public void resetStargate() { // Specifically for usage with PDD.
        disconnect();
        markChanged();
    }

    public static void sendErrorMsg(EntityPlayer player, String msg, Object... args) {
        ITextComponent component = new TextComponentTranslation("message.sgcraft:" + msg, args);
        component.getStyle().setColor(TextFormatting.RED);
        player.sendMessage(component);
    }

    public static void sendBasicMsg(EntityPlayer player, String msg, Object... args) {
        ITextComponent component = new TextComponentTranslation("message.sgcraft:" + msg, args);
        player.sendMessage(component);
    }

    //Todo: get rid of this and add all translations.
    public static void sendGenericErrorMsg(EntityPlayer player, String message) {
        TextComponentString component = new TextComponentString(message);
        component.getStyle().setColor(TextFormatting.RED);
        player.sendMessage(component);
    }

    // *********************************************************************************************
    // Access Control Subsystem
    // *********************************************************************************************

    // Addresses: entries are matched whatever the dashes or case, and always against a full 9-symbol address, so
    // pass the other gate's homeAddress rather than whatever was dialled (which may be the 7-symbol local form).

    public boolean allowOutgoingAddress(String address, boolean defaultValue) {
        GateAccessData entry = findGateAccessEntry(address);
        return entry != null ? entry.hasOutgoingAccess() : defaultValue;
    }

    public void setAllowOutgoingAddress(String address, boolean value) {
        GateAccessData entry = findGateAccessEntry(address);
        if (entry != null) {
            entry.setOutgoingAccess(value);
        }
    }

    public boolean allowIncomingAddress(String address, boolean defaultValue) {
        GateAccessData entry = findGateAccessEntry(address);
        return entry != null ? entry.hasIncomingAccess() : defaultValue;
    }

    public void setAllowIncomingAddress(String address, boolean value) {
        GateAccessData entry = findGateAccessEntry(address);
        if (entry != null) {
            entry.setIncomingAccess(value);
        }
    }

    @Nullable
    public GateAccessData findGateAccessEntry(String address) {
        if (this.gateAccessData == null || address == null) {
            return null;
        }
        return this.gateAccessData.stream().filter(g -> g.matches(address)).findFirst().orElse(null);
    }

    // Players: entries are tied to a player's UUID, so renaming keeps a player's access and a new player taking an
    // old name doesn't inherit it. A player with the sgcraft.admin permission may do anything.
    //
    // On a client, the local player's answers come from the server (setLocalPlayerAccess), since only the server
    // knows the lists and the player's permissions; until they arrive the client falls back to the gate's defaults.

    // This client's own access to this gate, as last sent by the server; null until then
    @Nullable private Boolean localGateAccess, localIrisAccess, localAdminAccess;

    public void setLocalPlayerAccess(boolean gateAccess, boolean irisAccess, boolean adminAccess) {
        this.localGateAccess = gateAccess;
        this.localIrisAccess = irisAccess;
        this.localAdminAccess = adminAccess;
    }

    public boolean allowGateAccess(EntityPlayer player) {
        if (world != null && world.isRemote && localGateAccess != null) {
            return localGateAccess;
        }
        if (isPermissionsAdmin(player)) {
            return true;
        }
        PlayerAccessData entry = findPlayerAccessEntry(player);
        return entry != null ? entry.hasGateAccess() : this.defaultAllowGateAccess;
    }

    public boolean allowAccessToIrisController(EntityPlayer player) {
        if (world != null && world.isRemote && localIrisAccess != null) {
            return localIrisAccess;
        }
        if (isPermissionsAdmin(player)) {
            return true;
        }
        PlayerAccessData entry = findPlayerAccessEntry(player);
        return entry != null ? entry.hasIrisAccess() : this.defaultAllowIrisAccess;
    }

    public boolean allowAdminAccess(EntityPlayer player) {
        if (world != null && world.isRemote && localAdminAccess != null) {
            return localAdminAccess;
        }
        if (isPermissionsAdmin(player)) {
            return true;
        }
        PlayerAccessData entry = findPlayerAccessEntry(player);
        return entry != null ? entry.isAdmin() : this.defaultAllowAdminAccess;
    }

    // The permissions system's fallback override of the access lists; only the server can check it
    private boolean isPermissionsAdmin(EntityPlayer player) {
        return world != null && !world.isRemote && SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin");
    }

    // The player's entry: theirs by UUID, or one made for their name before they were known. On the server that
    // ties a name-only entry to the player, and keeps an entry's name up to date when the player renames.
    @Nullable
    private PlayerAccessData findPlayerAccessEntry(EntityPlayer player) {
        if (this.playerAccessData == null || player == null) {
            return null;
        }
        PlayerAccessData entry = this.playerAccessData.stream()
            .filter(p -> player.getUniqueID().equals(p.getUuid())).findFirst()
            .orElseGet(() -> this.playerAccessData.stream().filter(p -> p.getUuid() == null && p.isFor(player)).findFirst().orElse(null));
        if (entry != null && world != null && !world.isRemote
                && (entry.getUuid() == null || !entry.getPlayerName().equals(player.getName()))) {
            entry.setUuid(player.getUniqueID());
            entry.setPlayerName(player.getName());
            markDirty();
        }
        return entry;
    }

    // Entries are edited by the name shown in the Configurator
    @Nullable
    public PlayerAccessData findPlayerAccessEntry(String playerName) {
        if (this.playerAccessData == null || playerName == null) {
            return null;
        }
        return this.playerAccessData.stream().filter(p -> p.getPlayerName().equalsIgnoreCase(playerName)).findFirst().orElse(null);
    }

    public void setAllowAccessToIrisController(String playerName, boolean value) {
        PlayerAccessData entry = findPlayerAccessEntry(playerName);
        if (entry != null) {
            entry.setIrisAccess(value);
        }
    }

    public void setAllowGateAccessAccess(String playerName, boolean value) {
        PlayerAccessData entry = findPlayerAccessEntry(playerName);
        if (entry != null) {
            entry.setGateAccess(value);
        }
    }

    public void setAllowAccessAdmin(String playerName, boolean value) {
        PlayerAccessData entry = findPlayerAccessEntry(playerName);
        if (entry != null) {
            entry.setAdmin(value);
        }
    }

    // Tells a player what they may do at this gate
    public void sendPlayerAccessTo(EntityPlayerMP player) {
        SGChannel.sendPlayerAccessToClient(player, this, allowGateAccess(player), allowAccessToIrisController(player), allowAdminAccess(player));
    }

    // Tells every player who can see this gate what they may now do at it, after its access settings change
    public void sendPlayerAccessToWatchers() {
        if (world instanceof WorldServer) {
            PlayerChunkMap chunks = ((WorldServer) world).getPlayerChunkMap();
            for (EntityPlayer player : world.playerEntities) {
                if (player instanceof EntityPlayerMP && chunks.isPlayerWatchingChunk((EntityPlayerMP) player, pos.getX() >> 4, pos.getZ() >> 4)) {
                    sendPlayerAccessTo((EntityPlayerMP) player);
                }
            }
        }
    }

    // The client's copy of the lists, sent only to an admin using the Configurator
    public void setClientAccessLists(List<GateAccessData> gateAccessData, List<PlayerAccessData> playerAccessData) {
        this.gateAccessData = gateAccessData;
        this.playerAccessData = playerAccessData;
    }

    public List<GateAccessData> getGateAccessData() {
        if (this.gateAccessData == null) {
            this.gateAccessData = new ArrayList<>();
        }
        return this.gateAccessData;
    }

    public List<PlayerAccessData> getPlayerAccessData() {
        if (this.playerAccessData == null) {
            this.playerAccessData = new ArrayList<>();
        }
        return this.playerAccessData;
    }

    // *********************************************************************************************
    // Dialing Subsystem
    // *********************************************************************************************

    String findHomeAddress() {
        try {
            return getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            //System.out.printf("SGBaseTE.findHomeAddress: %s\n", e);
            return "";
        }
    }

    public void disconnect() {
        if (debugConnect) {
            System.out.printf("SGBaseTE: %s: disconnect()\n", side());
        }
        SGBaseTE dte = SGBaseTE.at(connectedLocation);

        if (dte != null)
            dte.clearConnection();
        clearConnection();
    }

    public void clearIdleConnection() {
        if (state == SGState.Idle) {
            dialledAddress = "";
            pegasusFillStart = pegasusFillEnd = 0;
            dialedDigit = 0;
            enteredAddress = "";
            immediateDialAddress = "";
            connectedLocation = null;
            numEngagedChevrons = 0;
            isInitiator = false;
            performBlockDamage = false;
            this.checkForMalfunction = false;
            isPending = false;
            markChanged();
        }
    }

    public void clearConnection() {
        if (state != SGState.Idle || connectedLocation != null) {
            dialledAddress = "";
            pegasusFillStart = pegasusFillEnd = 0;
            immediateDialAddress = "";
            dialedDigit = 0;
            enteredAddress = "";
            connectedLocation = null;
            checkForMalfunction = false;
            performBlockDamage = false;
            isPending = false;
            markChanged();
            if (state == SGState.Connected) {
                enterState(SGState.Disconnecting, disconnectTime);
                playSGSoundEffect(disconnectSound, 1F, 1F);

            } else {
                numEngagedChevrons = 0;
                if (state != SGState.Idle && state != SGState.Disconnecting) {
                    playSGSoundEffect(getGateType().select(m_dialFailSound, p_dialFailSound), 1F, 1F);
                } else {
                    playSGSoundEffect(m_chevronOutgoingSound, 1F, 1F);
                }
                enterState(SGState.Idle, 0);
            }

            isInitiator = false; // Note: moved this to poso the enterState still contained the isInitiator value on postEvent.
            markChanged();
        }
    }

    public void immediateDialSymbol(String address, EntityPlayer player, int digit) {
        // This method is ony used for the PDD's progressive dialling when this.chevronsLockOnDial=true.
        // Only an idle gate, and only the next symbol of its dial: one given to a gate in the middle of another dial wrote
        // over that dial's address and chevrons, and the gate then crashed the server reading past the end of its address
        if (state != SGState.Idle || digit < 0 || digit >= address.length()) {
            return;
        }
        if (digit == 0) {
            enteredAddress = ""; // dialling from the start again
        }
        if (digit != enteredAddress.length()) {
            return;
        }
        char currentSymbol = address.charAt(digit);
        String check = String.valueOf(currentSymbol);

        if (!SGAddressing.isValidSymbolChar(check)) {
            diallingFailure(player, "malformedAddress", address);
            return;
        }

        if (address.length()> this.getNumChevrons()) {
            diallingFailure(player, "selfLackChevrons", address);
            return;
        }

        if (address.length() <= this.getNumChevrons()) {
            enteredAddress += currentSymbol;
            this.dialedDigit = enteredAddress.length();
            boolean last = address.length() == this.dialedDigit;
            this.finishDiallingSymbol(Character.toString(currentSymbol), true, false, last);
            this.dialledAddress = enteredAddress;
            this.numEngagedChevrons = enteredAddress.length();

            if (last) {
                String value = connect(address, player, false, false);

                if (value != null) {
                    this.clearIdleConnection();
                }
            } else {
                this.markChanged(); // need current state of server TE sent to client so thing stay in sync.
            }
        }
    }

    public void malfunction() throws SGAddressing.AddressingError {
        String randomAddress = GeneratorAddressRegistry.randomAddress(world, this.homeAddress, new Random());
        if (randomAddress != null) {
            if (this.allowOutgoingAddress(randomAddress, this.defaultAllowOutgoing)) {
                SGBaseTE malfunctionDestinationGate = SGAddressing.findAddressedStargate(randomAddress, world, false);
                // Only to a complete, idle gate that takes incoming wormholes: one already connected was taken over, its
                // own other end left connected to it for good
                if (malfunctionDestinationGate != null && malfunctionDestinationGate != this && malfunctionDestinationGate.isMerged
                        && malfunctionDestinationGate.state == SGState.Idle && malfunctionDestinationGate.allowIncomingConnections) {
                    if (malfunctionDestinationGate.allowIncomingAddress(this.homeAddress, malfunctionDestinationGate.defaultAllowIncoming)) {
                        SGBaseTE dialled = this.getConnectedStargateTE();
                        if (dialled != null) {
                            dialled.clearConnection();
                        }
                        this.connectedLocation = new SGLocation(malfunctionDestinationGate);
                        malfunctionDestinationGate.numEngagedChevrons = 7;
                        malfunctionDestinationGate.connectedLocation = new SGLocation(this);
                        playSGSoundEffect(malfunctionSound, 1F, 1F);
                        malfunctionDestinationGate.enterState(SGState.Connected, 0);
                    }
                }
            }
        }
    }

    void startDiallingStargate(String address, SGBaseTE dte, boolean initiator, boolean immediate) {
        dialledAddress = address;
        connectedLocation = new SGLocation(dte);
        isInitiator = initiator;
        // Wait as long as the dialling gate before opening, so that both ends open together
        if (!initiator) {
            syncAwaitTime = dte.syncAwaitTime;
        }
        markDirty();
        if (!immediate) {
            // From its first symbol, whatever chevrons a DHD had locked as they were entered: the first symbol dialled
            // used to be the one after them
            numEngagedChevrons = 0;
        }
        if (isInitiator && !immediate) {
            startDiallingNextSymbol();
        }
        postEvent(initiator ? "sgDialOut" : "sgDialIn", address);

        if (immediate) {
            // Its chevrons locked as each symbol went in, but the gate it dials can't be found until the last one is in:
            // that gate locks its own then, one after another, each with its sound (and its glyph, on a Pegasus gate),
            // and both wait for it before opening. It used to be given them all at once.
            int locking = dialledAddress.length() * incomingLockGap;
            if (initiator) {
                numEngagedChevrons = dialledAddress.length();
            } else {
                numEngagedChevrons = 0;
                incomingLockAt = world.getTotalWorldTime() + incomingLockGap;
            }
            enterState(SGState.SyncAwait, syncAwaitTime + locking);
        }
    }

    // A gate dialled by one whose chevrons lock as they're entered: how many ticks apart it locks its own, and when
    // (world time) it locks the next, or 0 (server only)
    static final int incomingLockGap = 4;
    private long incomingLockAt = 0;

    // A dial whose symbols the server enters one at a time, for a gate whose chevrons lock as they're entered
    // (chevronsLockOnDial), started from a PDD or EDD, which closes rather than staying open to send each one: the address,
    // who dialled, how many symbols are in, and when (world time) the next goes in (server only)
    private String lockDialAddress;
    private java.util.UUID lockDialPlayer;
    private int lockDialDigit;
    private long lockDialNextAt;
    // Ticks between symbols, as the PDD and EDD screens sent them
    static final int lockDialTicks = 15;

    public void startLockDial(String address, EntityPlayer player) {
        if (state != SGState.Idle || address == null || address.isEmpty()) {
            return;
        }
        lockDialAddress = address;
        lockDialPlayer = player.getUniqueID();
        lockDialDigit = 0;
        lockDialNextAt = world.getTotalWorldTime();
    }

    // The next symbol of a lock dial, when it's due; it stops once the address is in, a symbol is refused (see
    // immediateDialSymbol), the gate is busy, or its player has gone
    private void tickLockDial() {
        if (lockDialAddress == null || world.getTotalWorldTime() < lockDialNextAt) {
            return;
        }
        EntityPlayer player = world.getMinecraftServer() == null ? null
            : world.getMinecraftServer().getPlayerList().getPlayerByUUID(lockDialPlayer);
        if (player == null || state != SGState.Idle) {
            lockDialAddress = null;
            return;
        }
        String address = lockDialAddress;
        int digit = lockDialDigit++;
        immediateDialSymbol(address, player, digit);
        if (lockDialDigit >= address.length() || enteredAddress == null || enteredAddress.length() != lockDialDigit) {
            lockDialAddress = null; // done, or that symbol didn't go in
        } else {
            lockDialNextAt += lockDialTicks;
        }
    }

    private void lockIncomingChevrons() {
        if (incomingLockAt <= 0) {
            return;
        }
        if (isInitiator || state != SGState.SyncAwait) {
            incomingLockAt = 0; // the dial was called off
            return;
        }
        if (world.getTotalWorldTime() < incomingLockAt) {
            return;
        }
        int n = dialledAddress.length();
        if (numEngagedChevrons < n) {
            finishDiallingSymbol(String.valueOf(dialledAddress.charAt(numEngagedChevrons)), false, false,
                numEngagedChevrons + 1 >= n);
            markChanged();
        }
        incomingLockAt = numEngagedChevrons < n ? incomingLockAt + incomingLockGap : 0;
    }

    // For each chevron position (0 to 8, 4 being the top), which symbol of the address it locks
    // for (9 = never), for 7 and 9 symbol addresses. They lock clockwise from the top.
    public static final int[][] chevronEngagementSequences = {
        {9, 3, 4, 5, 6, 0, 1, 2, 9},
        {7, 3, 4, 5, 8, 0, 1, 2, 6}
    };

    // The position (0 to 8) of the chevron that locks e-th, or -1 if none does
    public int chevronPosition(int row, int e) {
        int numChevrons = getNumChevrons();
        int i0 = numChevrons > 7 ? 0 : 1;
        for (int i = i0; i < i0 + numChevrons; i++) {
            if (chevronEngagementSequences[row][i] == e) {
                return i;
            }
        }
        return -1;
    }

    // A Pegasus gate's light steps a glyph every 0.07 seconds, as the Atlantis gate does, whatever
    // the ring speed; a whole dial takes about 20 seconds, as long as its rolling sound. A gate
    // being dialled lights its ring from the top over the last pegasusFillTicks of the dial.
    final static double pegasusTicksPerGlyph = 1.4;
    final static int pegasusFillTicks = 90;
    // A Pegasus gate opens about a second after its last chevron locks, as the Atlantis gate does
    final static int pegasusEstablishTicks = 10;

    // The type of the gate that dialled this connection, whose timing both ends follow so that
    // they open together
    GateType diallingGateType() {
        SGBaseTE other = connectedLocation != null ? connectedLocation.getStargateTE() : null;
        return isInitiator || other == null ? getGateType() : other.getGateType();
    }

    // When (world time) a Pegasus gate being dialled starts and finishes lighting its ring, or 0
    public long pegasusFillStart, pegasusFillEnd;

    // Client-side only, kept by the renderer: how many chevrons it last saw locked, and when
    // (world time) it saw the latest one lock as this gate dialled out, to move the top chevron
    public int chevronsSeenLocked;
    public double chevronLockSeenTime = -1e9;

    int pegasusDialTicks(int e) {
        return (int) Math.round(Math.abs(pegasusLightTravel(e)) / ringSymbolAngle * pegasusTicksPerGlyph);
    }

    // Tells a Pegasus gate being dialled by another Pegasus gate when the last chevron will lock, so it can light its ring
    // in time. Called as each symbol starts dialling, taking the given number of ticks. Dialled by any other kind, whose
    // ring's timing isn't known until its last symbol, it lights each chevron as it locks instead (see
    // PegasusScreens.fill): filling then lit the whole ring in one sweep at the end, its chevrons dark until then.
    void schedulePegasusFill(int delay) {
        SGBaseTE target = connectedLocation != null ? connectedLocation.getStargateTE() : null;
        if (target == null || target.getGateType() != GateType.PEGASUS || getGateType() != GateType.PEGASUS) {
            return;
        }
        int n = dialledAddress.length();
        // A state lasts a tick longer than its timeout
        long ticks = delay + 1;
        if (numEngagedChevrons < n - 1) {
            for (int e = numEngagedChevrons + 1; e < n; e++) {
                ticks += interDiallingTime + 1 + pegasusDialTicks(e) + 1;
            }
        }
        long now = world.getTotalWorldTime();
        target.pegasusFillEnd = now + ticks;
        // Once the ring has started lighting, it carries on from where it is
        if (target.pegasusFillStart == 0 || target.pegasusFillStart > now) {
            target.pegasusFillStart = Math.max(now, target.pegasusFillEnd - pegasusFillTicks);
        }
        target.markChanged();
    }

    // How far (degrees, anticlockwise) a Pegasus gate's glyph light runs to lock the e-th symbol,
    // as on the show (following the Carter Addon Pack's Atlantis gate). It sets off from the
    // chevron locked before (the top one for the first symbol), anticlockwise for the first
    // symbol and alternating after that, and always runs at least three quarters of the way
    // round, taking an extra turn rather than a short hop. It moves a whole glyph at a time.
    double pegasusLightTravel(int e) {
        int row = dialledAddress.length() > 7 ? 1 : 0;
        int to = chevronPosition(row, e);
        int from = e == 0 ? 4 : chevronPosition(row, e - 1);
        if (to < 0 || from < 0) {
            return 360;
        }
        // Chevron angles fall as positions rise
        double between = (from - to) * angleBetweenChevrons();
        int direction = e % 2 == 0 ? 1 : -1;
        double distance = ((direction * between) % 360 + 360) % 360;
        if (distance <= 240) {
            distance += 360;
        }
        return direction * Math.round(distance / ringSymbolAngle) * ringSymbolAngle;
    }

    // An Ori supergate takes about this long (ticks) from starting to dial until it opens, as in The Ark of Truth: the
    // symbols, the short pauses between them, then waiting for the far gate and establishing the connection
    final static int oriInterDiallingTime = 6, oriEstablishTicks = 5;
    // Its dialling sound (ori_open, 22.7 seconds) plays once from the first symbol, swelling as the whoosh of its opening
    // starts, a little way into the opening, and running on half a second past it: so the dial to the whoosh takes this long
    static final int oriDialTicks = 444, oriWhooshTicks = 5;

    // The Ori gate being dialled links its segments in step with the one dialling it: each symbol takes it as long
    void syncOriSymbol(int delay) {
        syncOriSymbol(delay, delay);
    }

    // Part way through: the symbol takes length ticks, with remaining left
    void syncOriSymbol(int remaining, int length) {
        SGBaseTE target = connectedLocation != null ? connectedLocation.getStargateTE() : null;
        if (getGateType() == GateType.ORI && target != null && target.getGateType() == GateType.ORI && !target.isInitiator) {
            target.enterState(SGState.Dialling, remaining);
            target.maxTimeout = length;
        }
    }

    // How long (ticks) the symbol being dialled takes: the dial, from its first symbol to the whoosh, takes exactly
    // oriDialTicks, whatever the address (each state lasts a tick more than its timeout)
    int oriSymbolTicks() {
        int length = Math.max(1, dialledAddress.length());
        int budget = oriDialTicks - (length - 1) * (oriInterDiallingTime + 1) - (syncAwaitTime + 1)
            - (oriEstablishTicks + 1) - oriWhooshTicks;
        int each = budget / length;
        int spare = numEngagedChevrons == 0 ? budget - each * length : 0; // what doesn't divide evenly, on the first
        return Math.max(10, each - 1 + spare);
    }

    private void startDiallingSymbol(char c) {
        int i = SGAddressing.charToSymbol(c);
        if (debugState) {
            System.out.printf("SGBaseTE.startDiallingSymbol: %s\n", i);
        }
        if (i >= 0 && i < numRingSymbols && getGateType() == GateType.PEGASUS) {
            // A Pegasus gate's ring doesn't turn; ringAngle instead tracks how far its glyph light
            // has run, and the renderer places the light from it
            int delay = pegasusDialTicks(numEngagedChevrons);
            targetRingAngle = ringAngle + pegasusLightTravel(numEngagedChevrons);
            enterState(SGState.Dialling, delay);
            schedulePegasusFill(delay);
        } else if (i >= 0 && i < numRingSymbols) {
            // Back within a turn or two, after running a Pegasus gate's light
            if (Math.abs(ringAngle) >= 720) {
                ringAngle %= 360;
            }
            // A Universe gate's symbols come in fours between its chevrons
            double symbolAngle = getGateType() == GateType.UNIVERSE
                    ? universeSymbolOffset(i) : ringGlyphForSymbol(i) * ringGlyphAngle;
            // The ring turns one way and then the other, symbol by symbol, as on the show: left (anticlockwise, facing the
            // gate) for the first, right for the next, and so on, however far round the symbol is that way. It used to
            // reverse only when the symbol was within half a turn, so it could go the same way twice.
            boolean left = numEngagedChevrons % 2 == 0;
            double ahead = ((symbolAngle - ringAngle) % 360 + 360) % 360; // how far round it is, turning left
            if (ahead < 1e-6) {
                ahead = 360; // a whole turn rather than none
            }
            double diff = left ? ahead : ahead - 360;
            if (diff == 0) {
                diff = -360;
            }
            double targetAngle = ringAngle + diff;

            int delay = (int)Math.abs(diff / ringRotationSpeed);
            if (getGateType() == GateType.ORI) {
                delay = oriSymbolTicks(); // it has no ring to turn; it takes its time linking up
                if (numEngagedChevrons == 0 && !world.isRemote) {
                    startOriDialSound();
                }
            }
            targetRingAngle = targetAngle;
            //System.out.println(homeAddress + " -> Delay: " + delay + " (From angle " + ringAngle + " to angle " + targetAngle + ")");
            enterState(SGState.Dialling, delay);
            syncOriSymbol(delay);
            schedulePegasusFill(delay);
        } else {
            System.out.printf("SGCraft: Stargate jammed trying to dial symbol %s\n", c);
            dialledAddress = "";
            enterState(SGState.Idle, 0);
        }
    }

    public void unsetSymbol(char symbol) {
        --numEngagedChevrons; // Needs to be pre-event
        postEvent("sgChevronUnset", numEngagedChevrons, symbol);
    }

    public void finishDiallingSymbol(String symbol, boolean outgoing, boolean changeState, boolean lastOne) {
        ++numEngagedChevrons;
        if (debugDialSync && !world.isRemote) {
            dialLog.info(String.format("[SGCraft dial] server lock %s: %s", symbol, dialSyncText()));
        }
        postEvent("sgChevronEngaged", numEngagedChevrons, symbol);

        if (lastOne) {
            if (changeState) {
                enterState(SGState.SyncAwait, syncAwaitTime);
            }
            if (!world.isRemote) {
                //playSGSoundEffect(outgoing ? lockOutgoingSound : lockIncomingSound, 1F, 1F);
                playChevronSound(outgoing);
            }
        } else {
            if (changeState) {
                enterState(SGState.InterDialling, getGateType() == GateType.ORI ? oriInterDiallingTime
                    : getGateType().select(milkyWayInterDiallingTime, interDiallingTime));
            }
            if (!world.isRemote) {
                playChevronSound(outgoing);
            }
        }
    }

    private void attemptToLockStargate() {
        if (!isInitiator || useEnergy(energyToOpen * distanceFactor)) {
            // An Ori gate goes straight on to open once its ring has linked up (see OriGate)
            enterState(SGState.EstablishingConnection, getGateType() == GateType.ORI ? oriEstablishTicks
                : diallingGateType().select(30, pegasusEstablishTicks));
        } else {
            disconnect();
        }
    }

    private void openStargate() {
        if (debugConnect) {
            System.out.printf("SGBaseTE: Connecting to '%s'\n", dialledAddress);
        }
        enterState(SGState.Transient, getGateType() == GateType.ORI ? oriFillTicks + transientDuration : transientDuration);
    }

    public boolean canTravelFromThisEnd() {
        return isInitiator || !oneWayTravel;
    }

    private boolean symbolsRemaining(boolean before) {
        int n = numEngagedChevrons;
        return n < dialledAddress.length() - (before ? 1 : 0);
    }

    private void startDiallingNextSymbol() {
        if (debugState) {
            System.out.printf("SGBaseTE.startDiallingNextSymbol: %s of %s\n", numEngagedChevrons, dialledAddress);
        }
        // Past the end of the address, if something changed it under the dial: the dial stops, rather than the gate
        // throwing every tick (a "Ticking block entity" crash)
        if (numEngagedChevrons < 0 || numEngagedChevrons >= dialledAddress.length()) {
            System.out.printf("SGCraft: gate at %s in dimension %d stopped dialling %s: it has no symbol %d\n",
                pos, world.provider.getDimension(), dialledAddress, numEngagedChevrons + 1);
            this.resetStargate(); // both ends
            return;
        }

        startDiallingSymbol(dialledAddress.charAt(numEngagedChevrons));
    }

    // *********************************************************************************************
    // Energy Sub System
    // *********************************************************************************************

    // An Ori gate dialling out plays its dialling sound (ori_open) at both ends from its first symbol: the dial is timed so
    // it swells as the whoosh starts (see oriSymbolTicks)
    void startOriDialSound() {
        if (getGateType() != GateType.ORI || !isInitiator) {
            return;
        }
        playSGSoundEffect(oriOpenSound, 1F, 1F);
        SGBaseTE target = connectedLocation != null ? connectedLocation.getStargateTE() : null;
        if (target != null && target.getGateType() == GateType.ORI) {
            target.playSGSoundEffect(oriOpenSound, 1F, 1F);
        }
    }

    // A supergate that loses its ZPMs while dialling or open shuts down
    void checkSupergateZpms() {
        if (isSupergate() && state != SGState.Idle && !requiresNoPower && world.getTotalWorldTime() % 20 == 0
                && zpmCount() < supergateZpmsNeeded) {
            disconnect();
        }
    }

    void tickEnergyUsage() {
        if (state == SGState.Connected && isInitiator) {
            if (!this.requiresNoPower) {
                if (!useEnergy(energyUsePerTick * distanceFactor)) {
                    disconnect();
                }
            }
        }
    }

    public void validateZPM() {
        if (this.destinationRequiresZPM && this.isConnected()) {
            long power = (long) zpmPowerAvailable();
            if (power <= 0) {
                // Force disconnect if a zpm was required but suddenly missing or dead.
                this.disconnect();
            }
        }
    }

    // The sources it would draw on to dial now, as availableEnergy counts them (for /sgpower)
    public List<ISGEnergySource> energySources() {
        return findEnergySources(this.destinationRequiresZPM);
    }

    public double availableEnergy() {
        List<ISGEnergySource> sources = findEnergySources(this.destinationRequiresZPM);
        return energyInBuffer + energyAvailableFrom(sources);
    }

    public boolean energyIsAvailable(double amount) {
        double energy = availableEnergy();
        if (debugEnergyUse) {
            System.out.printf("SGBaseTE.energyIsAvailable: need %s, have %s\n", amount, energy);
        }
        return energy >= amount;
    }

    public boolean useEnergy(double amount) {
        debugEnergyUse = false;
        if (this.requiresNoPower) {
            return true;
        }

        if (debugEnergyUse) {
            System.out.printf("SGBaseTE.useEnergy: %s; buffered: %s\n", amount, energyInBuffer);
        }

        if (amount <= energyInBuffer) {
            energyInBuffer -= amount;

            if (updated++ > 10) {
                // Send energy update to client for diag/gui purposes
                markChanged();
                updated = 0;
            }
            return true;
        }

        if (this.destinationRequiresZPM) {
            long power = (long) zpmPowerAvailable();
            if (power == 0) {
                this.disconnect();
            }
        }

        List<ISGEnergySource> sources = findEnergySources(this.destinationRequiresZPM);
        double energyAvailable = energyInBuffer + energyAvailableFrom(sources);

        if (debugEnergyUse) {
            System.out.printf("SGBaseTE.useEnergy: %s available\n", energyAvailable);
        }

        if (amount > energyAvailable) {
            if (debugEnergyUse) {
                System.out.print("SGBaseTE: Not enough energy available\n");
            }
            return false;
        }

        double desiredEnergy = max(amount, maxEnergyBuffer);
        double targetEnergy = min(desiredEnergy, energyAvailable);
        double energyRequired = targetEnergy - energyInBuffer;

        if (debugEnergyUse) {
            System.out.printf("SGBaseTE.useEnergy: another %s required\n", energyRequired);
        }

        double energyOnHand = energyInBuffer + drawEnergyFrom(sources, energyRequired);

        if (debugEnergyUse) {
            System.out.printf("SGBaseTE.useEnergy: %s now on hand, need %s\n", energyOnHand, amount);
        }

        if (amount - 0.0001 > energyOnHand) {
            if (debugEnergyUse) {
                System.out.printf("SGBaseTE: Energy sources only delivered %s of promised %s\n", energyOnHand - energyInBuffer, energyAvailable);
            }
            return false;
        }

        setEnergyInBuffer(energyOnHand - amount);

        if (debugEnergyUse) {
            System.out.printf("SGBaseTE.useEnergy: %s left over in buffer\n", energyInBuffer);
        }

        return true;
    }

    private List<ISGEnergySource> findEnergySources(boolean requireZPM) {
        if (isSupergate()) {
            return zpmSources(); // a supergate runs on its ZPMs alone
        }

        boolean ccLoaded = isModLoaded("computercraft");
        boolean ocLoaded = isModLoaded("opencomputers");
        boolean ic2Loaded = isModLoaded("ic2");
        DHDTE te = getLinkedControllerTE();

        if (debugEnergyUse) {
            System.out.printf("SGBaseTe.findEnergySources: for %s\n", getSoundPos());
        }

        List<ISGEnergySource> result = new ArrayList<>();
        int radius = 4;

        for (final BlockPos.MutableBlockPos nearPos : BlockPos.getAllInBoxMutable(
                pos.add(-radius, -radius, -radius),
                pos.add(radius, radius, radius)
        )) {

            TileEntity nte = world.getTileEntity(nearPos);
            if (debugEnergyUse) {
                //System.out.printf("SGBaseTE.findEnergySources: %s at %s\n", nte, nearPos);
            }

            if (nte instanceof ISGEnergySource) { // Specifically exclude the ZPM Interface.
                if (ic2Loaded && nte instanceof IC2PowerTE) {
                    result.add((ISGEnergySource) nte);
                    if (debugEnergyUse) {
                        System.out.println("Found IC2PowerTE at: " + nte.getPos());
                    }
                }

                if(ic2Loaded && nte instanceof ZpmInterfaceCartTE) {
                    if (!((ZpmInterfaceCartTE) nte).isEmpty()){
                        result.add((ISGEnergySource) nte);
                    }
                    if (debugEnergyUse)
                        System.out.println("Found ZpmInterfaceCartTE at: " + nte.getPos());
                }

                if(ic2Loaded && nte instanceof ZpmHubTE) {
                    if (!((ZpmHubTE) nte).isEmpty()){
                        result.add((ISGEnergySource) nte);
                    }
                    if (debugEnergyUse)
                        System.out.println("Found ZpmHubTE at: " + nte.getPos());
                }

                if (nte instanceof ZpmConsoleTE) {
                    if (!((ZpmConsoleTE) nte).isEmpty()) {
                        result.add((ISGEnergySource) nte);
                    }
                    if (debugEnergyUse)
                        System.out.println("Added the ZpmConsoleTE to a Source");
                }

                if (nte instanceof SGPowerTE) {
                    result.add((ISGEnergySource) nte);
                    if (debugEnergyUse) {
                        System.out.println("Found SGPowerTE at: " + nte.getPos());
                    }
                }

                // Anything else made to power gates, such as a Naquadah generator
                if (nte instanceof IGatePowerSource) {
                    result.add((ISGEnergySource) nte);
                    if (debugEnergyUse) {
                        System.out.println("Found a gate power source at: " + nte.getPos());
                    }
                }

                if (ocLoaded && nte instanceof OCInterfaceTE) {
                    result.add((ISGEnergySource) nte);
                    if (debugEnergyUse) {
                        System.out.println("Found OCInterfaceTE at: " + nte.getPos());
                    }
                }
            }
        }

        // ZPM devices linked to the gate, wherever they are
        addLinkedZpms(result);

        if (te != null) {
            if (this.useDHDFuelSource) {
                if (!requireZPM) {
                    result.add(te);
                    if (debugEnergyUse) {
                        System.out.println("Found DHDTE at: " + te.getPos());
                    }
                } else {
                    if (debugEnergyUse) {
                        System.out.println("Found DHDTE at: " + te.getPos() + " but was not added because destination requires ZPM");
                    }
                }
            } else {
                if (debugEnergyUse) {
                    System.out.println("Found DHDTE at: " + te.getPos() + " but was not added because it is disabled in the config.");
                }
            }
        }

        return result;
    }

    private double energyAvailableFrom(List<ISGEnergySource> sources) {
        double energy = 0;
        for (ISGEnergySource source : sources) {
            double e = source.availableEnergy();
            if (debugEnergyUse) {
                System.out.printf("SGBaseTe.energyAvailableFrom: %s can supply %s\n", source, e);
            }
            energy += e;
        }

        if (debugEnergyUse) {
            System.out.println("Energy from Sources: " + energy);
        }

        return energy;
    }

    // In equal shares, so ZPMs (and other sources) run down together rather than one after another
    private double drawEnergyFrom(List<ISGEnergySource> sources, double amount) {
        double total = ZpmAddon.drawEvenly(sources, amount);
        if (debugEnergyUse) {
            System.out.printf("SGBaseTe.drawEnergyFrom: %s sources supplied %s\n", sources.size(), total);
        }
        if (total < amount) {
            if (debugEnergyUse) {
                System.out.printf("SGCraft: Warning: Energy sources did not deliver promised energy (%s requested, %s delivered)\n", amount, total);
            }
        }

        return total;
    }

    private void setEnergyInBuffer(double amount) {
        if (energyInBuffer != amount) {
            energyInBuffer = amount;
            markDirty();
        }
    }

    // The kawoosh: hurting what is in front of the gate, and clearing the blocks in its way. Its
    // regions (see GateShape) are in the gate's own frame, so it bursts out of the gate's front
    // however it stands: up out of a gate facing up, down out of one facing down.
    private void performTransientDamage() {
        GateShape shape = getShape();
        Trans3 t = localToGlobalTransformation();
        Vector3 p0 = t.p(fromGateFrame(shape.kawooshDamageMin));
        Vector3 p1 = t.p(fromGateFrame(shape.kawooshDamageMax));
        Vector3 q0 = p0.min(p1);
        Vector3 q1 = p0.max(p1);
        AxisAlignedBB box = new AxisAlignedBB(q0.x, q0.y, q0.z, q1.x, q1.y, q1.z);
        if (debugTransientDamage) {
            System.out.print("SGBaseTE.performTransientDamage: players in world:\n");
            for (Entity ent : world.loadedEntityList)
                if (ent instanceof EntityPlayer) {
                    System.out.printf("--- %s\n", ent);
                }
            System.out.printf("SGBaseTE.performTransientDamage: box = %s\n", box);
        }

        List<EntityLivingBase> ents = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (EntityLivingBase ent : ents) {
            Vector3 ep = new Vector3(ent.posX, ent.posY, ent.posZ);
            Vector3 gp = t.p(fromGateFrame(new Vector3(0, shape.centreY, 0.5)));
            double dist = ep.distance(gp);
            if (debugTransientDamage) {
                System.out.printf("SGBaseTE.performTransientDamage: found %s\n", ent);
            }
            if (dist > 1.0) {
                dist = 1.0;
            }
            int damage = (int) Math.ceil(dist * transientDamageRate);
            if (debugTransientDamage) {
                System.out.printf("SGBaseTE.performTransientDamage: distance = %s, damage = %s\n", dist, damage);
            }
            ent.attackEntityFrom(transientDamageSource, damage);
        }

        // Block Damage Area: the round bore in front of the opening, within the kawoosh block
        // region (left to right, up from base, forward from base). It is queued nearest the
        // gate first and cleared a slice at a time by clearKawooshBlocks, so that a big
        // supergate's kawoosh doesn't stall the server for one long tick.
        if (this.performBlockDamage && !this.irisIsClosed()) {
            t = localToGlobalTransformation();
            kawooshBlocks.clear();
            int x0 = (int) Math.floor(shape.kawooshBlockMin.x + 0.5), x1 = (int) Math.floor(shape.kawooshBlockMax.x + 0.5);
            int y0 = (int) Math.floor(shape.kawooshBlockMin.y + 0.5), y1 = (int) Math.floor(shape.kawooshBlockMax.y + 0.5);
            int z0 = (int) Math.floor(shape.kawooshBlockMin.z + 0.5), z1 = (int) Math.floor(shape.kawooshBlockMax.z + 0.5);
            for (int z = z0; z <= z1; z++)
                for (int y = y0; y <= y1; y++)
                    for (int x = x0; x <= x1; x++)
                        if (shape.inKawooshBore(x, y)) {
                            kawooshBlocks.add(t.p(fromGateFrame(new Vector3(x, y, z))).blockPos());
                        }
            this.performBlockDamage = false;
            clearKawooshBlocks();
        }
    }

    // Blocks still to be cleared by the current kawoosh, nearest the gate first
    private final java.util.ArrayDeque<BlockPos> kawooshBlocks = new java.util.ArrayDeque<>();

    // Clears the next lot of blocks in the kawoosh's path; called each server tick until done
    private void clearKawooshBlocks() {
        if (kawooshBlocks.isEmpty()) {
            return;
        }
        if (irisIsClosed()) {
            kawooshBlocks.clear();
            return;
        }
        for (int i = 0; i < KAWOOSH_BLOCKS_PER_TICK && !kawooshBlocks.isEmpty(); i++) {
            BlockPos p = kawooshBlocks.poll();
            IBlockState s = world.getBlockState(p);
            // Never a gate or DHD, of this gate or any other
            if (s.getBlock() == Blocks.AIR || isGateBlock(s.getBlock())) {
                continue;
            }
            if (SGCraft.wormholeCanDestroyUnbreakableBlocks || s.getBlockHardness(world, p) >= 0) {
                world.setBlockState(p, Blocks.AIR.getDefaultState());
            }
        }
    }

    static boolean isGateBlock(Block block) {
        return block instanceof SGBaseBlock || block instanceof SGRingBlock || block instanceof DHDBlock;
    }

    // *********************************************************************************************
    // Inventory Subsystem of TE.
    // *********************************************************************************************

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    public ItemStack getCamouflageStack(BlockPos cpos) {
        // Only the row of a gate standing up either side of its base is camouflaged
        if (isFlat()) {
            return null;
        }
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

    boolean isZpmSlot(int slot) {
        return slot >= firstZpmSlot && slot < firstZpmSlot + numZpmSlots;
    }

    // A supergate's ZPM slots take only ZPMs, from players or hoppers alike, and other gates' nothing
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (isZpmSlot(slot)) {
            return isSupergate() && stack.getItem() == SGCraft.zpm;
        }
        return super.isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return isItemValidForSlot(slot, stack) && super.canInsertItem(slot, stack, side);
    }

    // Hoppers and pipes may fill its slots but not empty them: its screen, the way out, is for its admins only, and a
    // hopper under any gate could otherwise take a supergate's ZPMs or a base's camouflage
    @Override
    public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) {
        return false;
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

    @SuppressWarnings("deprecation")
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

    // *********************************************************************************************
    // Entity Subsystem & Teleportation
    // *********************************************************************************************

    String repr(Entity entity) {
        if (entity != null) {
            String s = String.format("%s#%s", entity.getClass().getSimpleName(), entity.getEntityId());
            if (entity.isDead) {
                s += "(dead)";
            }
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

    // The region in front of and behind the opening in which entities are tracked for teleporting
    // ---- The gate's own frame ------------------------------------------------------------------
    //
    // A gate stands up (gateOrientation 1) or lies flat, its ring running out from its base along
    // the base's -z, facing up (2) or down (3). Whichever, in its own frame its base is at the
    // origin, x runs across its face, y up its ring from its base, and z out of its front, as for
    // a gate standing up; so that going through a gate, crossing its event horizon and coming out
    // of one are the same for all of them. These turn the base block's frame (its
    // localToGlobalTransformation) into the gate's and back.

    public boolean isFlat() {
        return gateOrientation == 2 || gateOrientation == 3;
    }

    // A point or direction in the base's frame, in the gate's
    public Vector3 toGateFrame(Vector3 l) {
        switch (gateOrientation) {
            case 2: return new Vector3(l.x, -l.z, l.y);
            case 3: return new Vector3(-l.x, -l.z, -l.y);
            default: return new Vector3(l.x, l.y, l.z);
        }
    }

    // A point or direction in the gate's frame, in the base's
    public Vector3 fromGateFrame(Vector3 g) {
        switch (gateOrientation) {
            case 2: return new Vector3(g.x, g.z, -g.y);
            case 3: return new Vector3(-g.x, -g.z, -g.y);
            default: return new Vector3(g.x, g.y, g.z);
        }
    }

    // The middle of the gate's ring, in the base's frame
    public Vector3 ringCentre() {
        return fromGateFrame(new Vector3(0, getShape().centreY, 0));
    }

    // The light round the gate, to draw it by, as World.getCombinedLight gives it: the brightest of the light in the
    // middle of its opening and a block in front of and behind that. Minecraft would use the light at the base, but
    // the base is a solid block, which light never gets into, so the gate would always be drawn in the dark.
    @SideOnly(Side.CLIENT) // World.getCombinedLight is client-only; only the gate renderer uses this
    public int lightAroundGate() {
        Trans3 t = localToGlobalTransformation();
        Vector3 centre = ringCentre(), front = fromGateFrame(new Vector3(0, 0, 1));
        int sky = 0, block = 0;
        for (int k = -1; k <= 1; k++) {
            int light = world.getCombinedLight(t.p(centre.add(front.mul(k))).blockPos(), 0);
            sky = Math.max(sky, light >> 20 & 15);
            block = Math.max(block, light >> 4 & 15);
        }
        return sky << 20 | block << 4;
    }

    // Where entities are watched for going through the gate: its opening, either side of its
    // event horizon
    AxisAlignedBB portalTrackingBox() {
        GateShape shape = getShape();
        double h = shape.apertureHalfWidth;
        double d = GateShape.PORTAL_TRACKING_DEPTH;
        Vector3 p0 = fromGateFrame(new Vector3(-h, shape.apertureBottom, -d));
        Vector3 p1 = fromGateFrame(new Vector3(h, shape.apertureTop, d));
        return localToGlobalTransformation().box(p0, p1);
    }

    // How far up an entity, from its feet, the point of it that goes through a gate first is: its
    // feet, but its head going up into a gate facing down
    double leadingHeight(Entity entity) {
        return gateOrientation == 3 ? entity.height : 0;
    }

    // How far an entity can move in a tick and still be teleported: further than anything but the
    // fastest projectiles go
    final static double maxPortalApproach = 4;

    void checkForEntitiesInPortal() {
        if (state == SGState.Connected) {
            for (TrackedEntity trk : trackedEntities) {
                entityInPortal(trk.entity, trk.lastPos);
            }
            trackedEntities.clear();
            List<Entity> ents = world.getEntitiesWithinAABB(Entity.class, portalTrackingBox());
            for (Entity entity : ents) {
                if (entity instanceof EntityFishHook || entity instanceof EntityStargateIris) {
                    continue;
                }
                // An entity filed under the wrong chunk can be found twice; tracking it twice would
                // teleport it twice
                boolean tracked = false;
                for (TrackedEntity trk : trackedEntities) {
                    tracked |= trk.entity == entity;
                }
                if (!tracked && !entity.isDead && entity.getRidingEntity() == null) {
                    if (entity instanceof EntityPlayer) {
                        //System.out.println("Entity:" + entity);
                    }
                    trackedEntities.add(new TrackedEntity(entity));
                }
            }
        } else {
            trackedEntities.clear();
        }
    }

    public void entityInPortal(Entity entity, Vector3 prevPos) {
        // Only an entity still at the gate goes through it; one already sent elsewhere doesn't
        boolean atGate = entity.world == world
                && portalTrackingBox().grow(maxPortalApproach).intersects(entity.getEntityBoundingBox());
        if (!entity.isDead && state == SGState.Connected && atGate) {
            Trans3 t = localToGlobalTransformation();
            double vx = entity.posX - prevPos.x;
            double vy = entity.posY - prevPos.y;
            double vz = entity.posZ - prevPos.z;
            // Where its leading point was and is, in the gate's frame: it has gone through if that
            // has crossed the event horizon (z = 0) from in front, by no more than the gate is deep
            double lead = leadingHeight(entity);
            Vector3 p1 = toGateFrame(t.ip(entity.posX, entity.posY + lead, entity.posZ));
            Vector3 p0 = toGateFrame(t.ip(2 * prevPos.x - entity.posX, 2 * prevPos.y - entity.posY + lead, 2 * prevPos.z - entity.posZ));
            GateShape shape = getShape();
            double depth = shape.maxCrossingDepth;

            // Its middle, across the gate's face and up its ring
            Vector3 mid = toGateFrame(t.ip(entity.posX, entity.posY + entity.height / 2, entity.posZ));
            boolean inOpening = shape.inOpening(mid.x, mid.y);

            if (inOpening && p0.z >= 0 && p1.z < 0 && p1.z > -depth) {
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

    private boolean playerHasTeleportAccess(Entity entity) {
        // Access Control System
        SGBaseTE dte = getConnectedStargateTE();
        boolean allowTeleport = true;

        if (entity instanceof EntityPlayer) {
            if (!this.allowGateAccess((EntityPlayer) entity) || !dte.allowGateAccess((EntityPlayer) entity)) {
                allowTeleport = false;
            }
        }

        if (!allowTeleport) {
            if (entity instanceof EntityPlayer)
                entity.sendMessage(new TextComponentString("Transport Failed.  Gate Access Denied!"));
            if (entity.getRidingEntity()!= null && (entity.getRidingEntity() instanceof EntityPlayer))
                entity.sendMessage(new TextComponentString("Rider Transport failed.  Gate Access Denied!"));
            return false;
        }
        return true;
    }

    // Whether an entity, and all that rides it however far up, may go through: none of them a player without access to
    // both gates, nor an ender pearl thrown by one (it takes its thrower where it lands). One denied anywhere keeps the
    // whole stack here, as one riding the entity itself always did. A stack with one further up used to go through
    // without them, and they were then put back on their mount on the far side: pulled through anyway, or, to another
    // world, left riding in both worlds at once.
    private boolean mayGoThrough(Entity entity) {
        if (entity instanceof EntityPlayer && !playerHasTeleportAccess(entity)) {
            return false;
        }
        if (entity instanceof EntityEnderPearl && ((EntityEnderPearl) entity).getThrower() instanceof EntityPlayer
                && !playerHasTeleportAccess(((EntityEnderPearl) entity).getThrower())) {
            return false;
        }
        for (Entity rider : entity.getPassengers()) {
            if (!mayGoThrough(rider)) {
                return false;
            }
        }
        return true;
    }

    Entity teleportEntityAndRiders(Entity entity, Trans3 t1, Trans3 t2, int dimension, boolean destBlocked) {
        if (debugTeleport) {
            System.out.printf("SGBaseTE.teleportEntityAndRiders: destBlocked = %s\n", destBlocked);
        }
        List<Entity> riders = entity.getPassengers();
        boolean canProceed = mayGoThrough(entity);

        if (canProceed) {
            for (int i = 0; i < riders.size(); i++) {
                Entity rider = riders.get(i);
                rider.dismountRidingEntity();
                rider = teleportEntityAndRiders(rider, t1, t2, dimension, destBlocked);
                riders.set(i, rider);
            }

            unleashEntity(entity);
            entity = teleportEntity(entity, t1, t2, dimension, destBlocked);
            // A player going through has been at both gates, if they reached the far one (an iris there kills them)
            if (entity instanceof EntityPlayer) {
                ExplorerData.markVisited((EntityPlayer) entity, this.homeAddress);
                SGBaseTE dte = getConnectedStargateTE();
                if (!destBlocked && dte != null) {
                    ExplorerData.markVisited((EntityPlayer) entity, dte.homeAddress);
                }
            }

            if (entity != null && !entity.isDead) {
                for (Entity rider : riders) {
                    if (rider != null && !rider.isDead) {
                        rider.startRiding(entity, true);
                    }
                }
            }
        }
        return entity;
    }

    // Break any leash connections to or from the given entity. That happens anyway
    // when the entity is teleported, but without this it drops an extra leash item.
    protected void unleashEntity(Entity entity) {
        if (entity instanceof EntityLiving) {
            ((EntityLiving) entity).clearLeashed(true, false);
        }
        for (EntityLiving entity2 : entitiesWithinLeashRange(entity)) {
            if (entity2.getLeashed() && entity2.getLeashHolder() == entity) {
                entity2.clearLeashed(true, false);
            }
        }
    }

    protected List<EntityLiving> entitiesWithinLeashRange(Entity entity) {
        AxisAlignedBB box = new AxisAlignedBB(
                entity.posX - 7.0D, entity.posY - 7.0D, entity.posZ - 7.0D,
                entity.posX + 7.0D, entity.posY + 7.0D, entity.posZ + 7.0D);
        return entity.world.getEntitiesWithinAABB(EntityLiving.class, box);
    }

    Entity teleportEntity(Entity entity, Trans3 t1, Trans3 t2, int dimension, boolean destBlocked) {
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

        // Through the gate, in the gates' own frames (see toGateFrame): what goes in at the front
        // of one comes out of the front of the other, turned half round about its y, so that
        // going in at its left it comes out on its left, as seen by someone going through
        SGBaseTE dest = getConnectedStargateTE();
        // Where its leading point (see leadingHeight) is, going in, and is to be, coming out
        Vector3 p = toGateFrame(t1.ip(entity.posX, entity.posY + leadingHeight(entity), entity.posZ));
        Vector3 v = toGateFrame(t1.iv(entity.motionX, entity.motionY, entity.motionZ));
        double x = -p.x, y = p.y, z = -p.z;

        // Into a supergate, between gates of different sizes, or between a gate standing up and
        // one lying flat, put the entity inside the destination's opening as it looks. Coming out
        // of a gate standing up, a player on foot, or anything at all out of a gate lying flat,
        // comes out standing on its floor; anything else at the same place in the opening, scaled
        // to its size.
        GateShape srcShape = getShape();
        GateShape destShape = dest.getShape();
        boolean turned = isFlat() != dest.isFlat();
        if (destShape != srcShape || destShape.isSupergate() || turned) {
            boolean onFoot = !dest.isFlat() && (isFlat() || entity instanceof EntityPlayer && !((EntityPlayer) entity).capabilities.isFlying);
            // How far it reaches up the destination's ring, and its middle that way
            double size = dest.isFlat() ? entity.width : entity.height, half = size / 2;
            double mid = isFlat() ? p.y : p.y + entity.height / 2;
            double scale = destShape.openingRadius() / srcShape.openingRadius();
            double floor = destShape.openingFloor();
            y = onFoot ? floor : destShape.centreY + (mid - srcShape.centreY) * scale - half;
            y = clamp(y, floor, Math.max(floor, destShape.openingTop() - size));
            double hw = Math.max(0, destShape.openingHalfWidthAt(y + half) - entity.width / 2);
            x = clamp(x * (onFoot ? 1 : scale), -hw, hw);
            if (dest.isFlat()) {
                // Its feet are its edge; its middle is where it goes in the opening
                y += half;
            }
        }
        // Out of a gate lying flat, clear of its event horizon, so as not to fall back through it:
        // its feet above it facing up, its head below it facing down
        if (dest.isFlat()) {
            z = Math.max(z, 0.1);
        }
        Vector3 q = t2.p(dest.fromGateFrame(new Vector3(x, y, z)).sub(0, dest.leadingHeight(entity), 0));
        Vector3 u = t2.v(dest.fromGateFrame(new Vector3(-v.x, v.y, -v.z)));

        // Looking the same way out of it as it was into it: turned about the vertical as the bases
        // are turned to each other, its pitch kept, whether either gate stands or lies flat
        Vector3 r = t1.iv(yawVector(entity));
        Vector3 s = t2.v(new Vector3(-r.x, 0, -r.z));
        double a = yawAngle(s, entity);
        double pitch = entity.rotationPitch;

        if (debugTeleport) {
            System.out.printf("SGBaseTE.teleportEntity: from gate %s orientation %d to gate %s orientation %d, gate frame %s velocity %s, arriving at %s velocity %s yaw %.2f pitch %.2f\n",
                    pos, gateOrientation, dest.pos, dest.gateOrientation, p, v, q, u, a, pitch);
        }

        if (!canTravelFromThisEnd() && !isInitiator) {
            if (reverseWormholeKills) { // Player attempting to reverse travel through one-way wormhole.
                terminateEntityByReverseWormhole(entity);
                return entity;
            } else {
                // Player attempted to reverse travel but config option for reverseWormholleKills is disabled, return entity, display nothing.
                return entity;
            }
        }

        // Tollana Phase Shift Device == Bypass destBlocked (isIrisClosed)
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player != null && !player.getHeldItemOffhand().isEmpty() && player.getHeldItemOffhand().getItem().equals(SGCraft.tollan_phase_shift_device)) {
                destBlocked = false;
            }
        }

        if (!destBlocked) { // destBlocked == closed iris.
            // Play sound from point of origin gate.
            playTeleportSound(entity.getEntityWorld(), new Vector3(entity.getPositionVector()), entity);
            if (entity.dimension == dimension) {
                newEntity = teleportWithinDimension(entity, q, u, a, pitch, destBlocked);
            } else {
                newEntity = teleportToOtherDimension(entity, q, u, a, pitch, dimension, destBlocked);
            }
        } else {
            terminateEntityByIrisImpact(entity);
            playIrisHitSound(worldForDimension(dimension), q, entity);
        }
        // Play sound at destination gate.
        playTeleportSound(entity.getEntityWorld(), new Vector3(entity.getPositionVector()), entity);
        return newEntity;
    }

    public void terminateEntityByIrisImpact(Entity entity) {
        if (entity instanceof EntityPlayer) {
            terminatePlayerByIrisImpact((EntityPlayer)entity);
        } else {
            entity.setDead();
        }
    }

    public void terminatePlayerByIrisImpact(EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            sendErrorMsg(player, "irisAtDestination");
        } else {
            if (!(preserveInventory || player.world.getGameRules().getBoolean("keepInventory")))
                player.inventory.clear();
            player.attackEntityFrom(irisDamageSource, irisDamageAmount);
        }
    }

    public void terminateEntityByReverseWormhole(Entity entity) {
        if (entity instanceof EntityPlayer) {
            terminatePlayerByReverseWormhole((EntityPlayer)entity);
        } else {
            entity.setDead();
        }
    }

    public void terminatePlayerByReverseWormhole(EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            sendErrorMsg(player, "reverseWormhole");
        } else {
            if (!(preserveInventory || player.world.getGameRules().getBoolean("keepInventory")))
                player.inventory.clear();
            player.attackEntityFrom(transientDamageSource, irisDamageAmount);
            if (!player.isDead) {
                player.setHealth(0.0F); // Backup player death to prevent grief prevention from saving player.
            }
        }
    }

    static WorldServer worldForDimension(int dimension) {
        return SGAddressing.getWorld(dimension);
    }

    Entity teleportWithinDimension(Entity entity, Vector3 p, Vector3 v, double a, double pitch, boolean destBlocked) {
        if (entity instanceof EntityPlayerMP) {
            return teleportPlayerWithinDimension((EntityPlayerMP) entity, p, v, a, pitch);
        } else {
            return teleportEntityToWorld(entity, p, v, a, pitch, (WorldServer) entity.world, destBlocked);
        }
    }

    Entity teleportPlayerWithinDimension(EntityPlayerMP entity, Vector3 p, Vector3 v, double a, double pitch) {
        entity.rotationYaw = (float)a;
        //entity.setPositionAndUpdate(p.x, p.y, p.z);
        // Todo:
        setEntityLocationAndPitch(entity, p, a, pitch);
        setVelocity(entity, v);
        entity.world.updateEntityWithOptionalForce(entity, false);
        entity.velocityChanged = true; // Have to mark entity velocity changed.

        return entity;
    }

    Entity teleportToOtherDimension(Entity entity, Vector3 p, Vector3 v, double a, double pitch, int dimension, boolean destBlocked) {
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP)entity;
            // A step further out of a gate standing up; one lying flat already puts it clear
            SGBaseTE dest = getConnectedStargateTE();
            Vector3 q = dest != null && dest.isFlat() ? p : p.add(yawVector(a));
            transferPlayerToDimension(player, dimension, q, v, a, pitch);
            return player;
        } else {
            setVelocity(entity, v);
            return teleportEntityToDimension(entity, p, v, a, pitch, dimension, destBlocked);
        }
    }

    void transferPlayerToDimension(EntityPlayerMP player, int newDimension, Vector3 p, Vector3 v, double a, double pitch) {
        FakeTeleporter fakeTeleporter = new FakeTeleporter();
        player.changeDimension(newDimension, fakeTeleporter);
        // Now check to see if the player made it through the above server method, if it did, then update their location.
        if (player.dimension == newDimension) {
            //player.connection.setPlayerLocation(p.x, p.y, p.z, (float) a, player.rotationPitch);
            // Todo:
            setEntityLocationAndPitch(player, p, a, pitch);
            setVelocity(player, v);
            player.velocityChanged = true;
        }
    }

    Entity teleportEntityToDimension(Entity entity, Vector3 p, Vector3 v, double a, double pitch, int dimension, boolean destBlocked) {
        MinecraftServer server = BaseUtils.getMinecraftServer();
        WorldServer world = server.getWorld(dimension);
        return teleportEntityToWorld(entity, p, v, a, pitch, world, destBlocked);
    }

    Entity teleportEntityToWorld(Entity entity, Vector3 p, Vector3 v, double a, double pitch, WorldServer newWorld, boolean destBlocked) {
        if (destBlocked) {
            if (!(entity instanceof EntityLivingBase))
                return null;
        }

        if (this.world == newWorld) {
            entity.rotationYaw = (float) a;
            //entity.setPositionAndUpdate(p.x, p.y, p.z);
            // Todo:
            setEntityLocationAndPitch(entity, p, a, pitch);
            setVelocity(entity, v);
            entity.world.updateEntityWithOptionalForce(entity, false);
            entity.velocityChanged = true; // Have to mark entity velocity changed.
            return entity;
        } else {
            //Todo: the following was modified because the below should only fire if changing permissions, else an "entity already exists" is thrown.
            FakeTeleporter fakeTeleporter = new FakeTeleporter();
            Entity newEntity = entity.changeDimension(newWorld.provider.getDimension(), fakeTeleporter);
            if (newEntity == null) {
                return entity; // refused (by a plugin, say), or couldn't be made there: it stays where it was
            }
            if (newEntity.dimension == newWorld.provider.getDimension()) {
                //newEntity.setLocationAndAngles(p.x, p.y, p.z, (float) a, entity.rotationPitch);
                // Todo:
                setEntityLocationAndPitch(newEntity, p, a, pitch);
                setVelocity(newEntity, v); //Set velocity so that items exist at the same rate they did when they entered the event horizon.
                newEntity.velocityChanged = true;
            }

            return newEntity;
        }
    }

    // Puts the entity where it comes out of the gate, facing the way it does (see teleportEntity)
    private void setEntityLocationAndPitch(Entity entity, Vector3 p, double yaw, double pitch) {
        if (entity instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entity).connection.setPlayerLocation(p.x, p.y, p.z, (float) yaw, (float) pitch);
        } else {
            entity.setLocationAndAngles(p.x, p.y, p.z, (float) yaw, (float) pitch);
        }
    }

    static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    static void setVelocity(Entity entity, Vector3 v) {
        entity.motionX = v.x;
        entity.motionY = v.y;
        entity.motionZ = v.z;
    }

    protected static int yawSign(Entity entity) {
        return entity instanceof EntityArrow ? -1 : 1;
    }

    Vector3 yawVector(Entity entity) {
        return yawVector(yawSign(entity) * entity.rotationYaw);
    }

    Vector3 yawVector(double yaw) {
        double a = Math.toRadians(yaw);
        Vector3 v = new Vector3(-Math.sin(a), 0, Math.cos(a));
        //System.out.printf("SGBaseTE.yawVector: %.2f --> (%.3f, %.3f)\n", yaw, v.x, v.z);
        return v;
    }

    double yawAngle(Vector3 v, Entity entity) {
        double a = Math.atan2(-v.x, v.z);
        double d = Math.toDegrees(a);
        //System.out.printf("SGBaseTE.yawAngle: (%.3f, %.3f) --> %.2f\n", v.x, v.z, d);
        return yawSign(entity) * d;
    }

    public SGBaseTE getConnectedStargateTE() {
        return isConnected() && connectedLocation != null ? connectedLocation.getStargateTE() : null;
    }

    static void copyMoreEntityData(EntityLiving oldEntity, EntityLiving newEntity) {
        float s = oldEntity.getAIMoveSpeed();
        if (s != 0)
            newEntity.setAIMoveSpeed(s);
    }

    // The iris's solid body, made afresh each time, as it lies across the gate as it now stands
    // (merging, turning it with the configurator, or fitting the iris upgrade)
    public void updateIrisEntity() {
        for (EntityStargateIris ent : findIrisEntities()) {
            world.removeEntity(ent);
        }
        if (isMerged && hasIrisUpgrade) {
            world.spawnEntity(new EntityStargateIris(this));
        }
    }

    boolean hasIrisEntity() {
        return findIrisEntities().size() != 0;
    }

    // The iris bodies of this gate, round the middle of its ring however it stands
    List<EntityStargateIris> findIrisEntities() {
        Vector3 c = localToGlobalTransformation().p(ringCentre());
        double r = getShape().halfWidth + 1;
        AxisAlignedBB box = new AxisAlignedBB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
        //System.out.printf("SGBaseTE.findIrisEntities: in %s\n", box);
        return world.getEntitiesWithinAABB(EntityStargateIris.class, box, ent -> pos.equals(ent.getGatePos()));
    }

    // *********************************************************************************************
    // Sound Subsystem
    // *********************************************************************************************

    @Override
    public float getSoundVolume(SoundEvent sound) {
        return soundVolume * soundRange();
    }

    // The gate is heard from its middle rather than its base block, which is a long way down on a
    // supergate.
    @Override
    public Vec3d getSoundOrigin() {
        Vector3 c = localToGlobalTransformation().p(ringCentre());
        return new Vec3d(c.x, c.y, c.z);
    }

    // Minecraft plays a sound to players up to 16 blocks away, times its volume when that is more
    // than 1. A supergate is heard as far beyond its rim as a standard gate is.
    float soundRange() {
        double r = getShape().ringOuterRadius - GateShape.STANDARD.ringOuterRadius;
        return (float) Math.max(1.0, (16.0 + r) / 16.0);
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
        if (this.isInvalid() || !this.world.isBlockLoaded(this.pos) || this.world.getTileEntity(this.pos) != this) {
            return false;
        }
        if (sound == m_gateRollSound || sound == p_gateRollSound) {
            return state == SGState.Dialling;
        } else if (sound == irisOpenSound) {
            return irisState == IrisState.Opening;
        } else if (sound == irisCloseSound) {
            return irisState == IrisState.Closing;
        } else if (sound == eventHorizonSound || sound == malfunctionSound) {
            return state == SGState.Connected;
        } else {
            return false;
        }
    }

    void playIrisHitSound(World world, Vector3 pos, Entity entity) {
        float volume = (float) min(entity.width * entity.height, 1.0);
        float pitch = 2F - volume;
        if (debugTeleport) {
            System.out.printf("SGBaseTE.playIrisHitSound: at (%.3f,%.3f,%.3f) volume %.3f pitch %.3f\n", pos.x, pos.y, pos.z, volume, pitch);
        }
        playSoundEffect(world, pos.x, pos.y, pos.z, irisHitSound, volume * soundVolume, pitch);
    }

    void playTeleportSound(World world, Vector3 pos, Entity entity) {
        float volume = (float) min(entity.width * entity.height, 1.0);
        float pitch = 2F - volume;
        if (debugTeleport) {
            System.out.printf("SGBaseTE.playTeleportSound: at (%.3f,%.3f,%.3f) volume %.3f pitch %.3f\n", pos.x, pos.y, pos.z, volume, pitch);
        }
        playSoundEffect(world, pos.x, pos.y, pos.z, teleportSound, volume * soundVolume, pitch);
    }

    public void playSGSoundEffect(SoundEvent se, float volume, float pitch) {
        Vec3d c = getSoundOrigin();
        playSoundEffect(world, c.x, c.y, c.z, se, volume * soundVolume * soundRange(), pitch);
    }

    void playChevronSound(boolean outgoing) {
        GateType type = getGateType();
        if (type == GateType.ORI) {
            return; // its dialling sound plays throughout instead (see startOriDialSound)
        }
        if (outgoing) {
            playSGSoundEffect(type.select(m_chevronOutgoingSound, p_chevronOutgoingSound), 1F, 1F);
        } else {
            playSGSoundEffect(type.select(m_chevronIncomingSound, p_chevronIncomingSound), 1F, 1F);
        }
    }

    // *********************************************************************************************
    // OnTick for server thread
    // *********************************************************************************************

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
                ((OCIntegration)SGCraft.ocIntegration).onSGBaseTEAdded(this);
            }
        }
        if (isMerged) {
            if (debugState && state != SGState.Connected && timeout > 0) {
                int dimension = world.provider.getDimension();
                System.out.printf("SGBaseTE.serverUpdate at %s in dimension %d: state %s, timeout %s\n", pos, dimension, state, timeout);
            }
            tickEnergyUsage();
            checkSupergateZpms();
            if (timeout > 0) {
                switch (state) {
                    case Transient:
                        // An Ori gate's kawoosh comes once its horizon has filled in; its whoosh starts just after the opening does
                        int sinceOpening = maxTimeout - timeout;
                        boolean ori = getGateType() == GateType.ORI;
                        if (ori && sinceOpening == oriWhooshTicks) {
                            playSGSoundEffect(m_connectSound, 1F, 1F);
                        }
                        if (!irisIsClosed() && (!ori || sinceOpening >= oriFillTicks)) {
                            if (this.transientDamage) {
                                performTransientDamage();
                            }
                        }
                        break;
                    case Dialling:
                        // Where it will be at the end of this tick, so that it reaches the target on the last one
                        double step = (double) (maxTimeout - timeout + 1) / (double) maxTimeout;
                        ringAngle = startRingAngle + (targetRingAngle - startRingAngle) * step;
                        break;

                    case EstablishingConnection:
                        if (timeout == diallingGateType().select(25, pegasusEstablishTicks) && getGateType() != GateType.ORI) {
                            // Play sound before gate actually opens; a connection dialled by a
                            // Pegasus gate opens sooner, so plays it straight away.
                            playSGSoundEffect(getGateType().select(m_connectSound, p_connectSound), 1F, 1F);
                        }
                        break;

                    case Connected:
                        if (this.checkForMalfunction && isInitiator) {
                            BlockPos rainCheckPos;
                            if (this.gateOrientation == 1) {
                                rainCheckPos = new BlockPos(this.getPos().getX(), this.getY() + getShape().height, this.getZ()); // Top of a vertical gate.
                            } else {
                                rainCheckPos = new BlockPos(this.getPos().getX(), this.getY(), this.getZ()); // Base block since its horizontal.
                            }
                            if (rainCheckPos != null && this.world.isRaining() && this.world.isRainingAt(rainCheckPos)) {
                                Random rand = new Random();
                                if(rand.nextInt(100) <= 5) {
                                    try {
                                        world.addWeatherEffect(new EntityLightningBolt(this.world, (double)this.pos.getX(), (double)this.pos.getY(), (double)this.pos.getZ(), true));
                                        malfunction();
                                    } catch (SGAddressing.AddressingError addressingError) {
                                        addressingError.printStackTrace();
                                    }
                                }
                            }
                            checkForMalfunction = false;
                        }
                        break;
                }
                --timeout;
            } else {
                switch(state) { // Next Stage
                    case Idle:
                        if (symbolsRemaining(false) && isInitiator) {
                            startDiallingNextSymbol();
                        }
                        break;
                    case Dialling:
                        if (isInitiator) {
                            try {
                                // The gate being dialled, which locks its symbol with this one's: if it's gone, or has
                                // lost its side of the connection, say why (it used to stop without a word) and stop
                                SGBaseTE targetGate = SGBaseTE.at(connectedLocation);
                                String lost = farGateProblem(targetGate);
                                if (lost != null) {
                                    System.out.printf("SGCraft: gate at %s in dimension %d stopped dialling %s: %s\n",
                                        pos, world.provider.getDimension(), dialledAddress, lost);
                                    this.clearConnection();
                                    this.resetStargate();
                                    break;
                                }
                                char charTargetSymbol = dialledAddress.charAt(numEngagedChevrons);
                                char charOwnSymbol = homeAddress.charAt(numEngagedChevrons);
                                String targetSymbol = Character.toString(charTargetSymbol);
                                String ownSymbol = Character.toString(charOwnSymbol);
                                // Note:  CC interfaces can't use CHAR!
                                finishDiallingSymbol(targetSymbol, true, true, !symbolsRemaining(true));
                                targetGate.finishDiallingSymbol(ownSymbol, false, true, !targetGate.symbolsRemaining(true));
                            }
                            catch (Exception e) {  // Fall back, but say what went wrong
                                System.out.printf("SGCraft: gate at %s in dimension %d stopped dialling %s after %d symbols: %s\n",
                                    pos, world.provider.getDimension(), dialledAddress, numEngagedChevrons, e);
                                e.printStackTrace();
                                // Both ends: clearing this one first lost where the other was, leaving it waiting
                                // (and its chunks loaded) for a dial that would never come
                                this.resetStargate();
                            }
                        }
                        break;
                    case InterDialling:
                        if (isInitiator) {
                            startDiallingNextSymbol();
                        }
                        break;
                    case SyncAwait:
                        attemptToLockStargate();
                        break;
                    case EstablishingConnection:
                        openStargate();
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
                        if (this.hasIrisUpgrade && this.returnToPreviousIrisState && this.wasIrisClosed) {
                            this.closeIris();
                            // Note: this is fired at both the origin and destination gates.
                        }

                        break;
                }
            }
        }
    }

    // *********************************************************************************************
    // Client Handlers
    // *********************************************************************************************

    // The gate's data as sent to every player near it: without the access lists, which name who may use the gate.
    // Each player is sent only their own access instead (see sendPlayerAccessTo).
    @Override
    protected NBTTagCompound writeToClientNBT(NBTTagCompound nbt) {
        super.writeToClientNBT(nbt);
        nbt.removeTag(PlayerAccessData.PLAYERACCESSLIST);
        nbt.removeTag(GateAccessData.GATEACCESSLIST);
        return nbt;
    }

    // A gate's data arriving with its chunk (rather than as its own packet), which skips onDataPacket
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (debugDialSync) {
            dialLog.info(String.format("[SGCraft dial] client got gate with chunk data: %s", dialSyncText()));
        }
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        //System.out.printf("SGBaseTE.onDataPacket: with state %s numEngagedChevrons %s\n",
        //  SGState.valueOf(pkt.customParam1.getInteger("state")),
        //  pkt.customParam1.getInteger("numEngagedChevrons"));
        IrisState oldIrisState = irisState;
        SGState oldState = state;
        super.onDataPacket(net, pkt);
        if (debugDialSync) {
            dialLog.info(String.format("[SGCraft dial] client got gate packet: %s", dialSyncText()));
        }
        if (isMerged) {
            if (state != oldState) {
                switch (state) {
                    case Transient:
                        startOpening();
                        break;
                    case Disconnecting:
                        initiateClosingTransient();
                        break;
                    case Dialling:
                        if (isInitiator) {
                            if (timeout > 0 && getGateType() != GateType.ORI) { // an Ori gate has no ring to roll
                                SGCraft.playSound(this, getGateType().select(m_gateRollSound, p_gateRollSound));
                            }
                        }
                        break;
                    case Connected:
                        SGCraft.playSound(this, eventHorizonSound);
                        break;
                }
            }
            if (irisState != oldIrisState) {
                switch (irisState) {
                    case Opening:
                        if (!hasShield) {
                            SGCraft.playSound(this, irisOpenSound);
                        } else if (oldIrisState == IrisState.Closed || oldIrisState == IrisState.Closing) {
                            playShieldSound(shieldDeactivationSound);
                        }
                        break;
                    case Closing:
                        if (!hasShield) {
                            SGCraft.playSound(this, irisCloseSound);
                        } else if (oldIrisState == IrisState.Open || oldIrisState == IrisState.Opening) {
                            playShieldSound(shieldActivationSound);
                        }
                        break;
                }
            }
        }
    }

    // The shield sound last started, and when (world time), so a shield only seen rising or falling again by a late
    // update from the server doesn't start it over
    private SoundEvent lastShieldSound;
    private long lastShieldSoundTime = -1000;
    private static final int shieldSoundTicks = 38, shieldActivationTicks = 82; // the sounds' lengths

    @SideOnly(Side.CLIENT)
    private void playShieldSound(SoundEvent sound) {
        long now = world.getTotalWorldTime();
        int length = sound == shieldActivationSound ? shieldActivationTicks : shieldSoundTicks;
        if (sound != lastShieldSound || now - lastShieldSoundTime >= length) {
            lastShieldSound = sound;
            lastShieldSoundTime = now;
            // A plain one-off sound, not the gate's tracked sounds (SGCraft.playSound), which kept this one looping
            Vec3d c = getSoundOrigin();
            world.playSound(c.x, c.y, c.z, sound, SoundCategory.AMBIENT, soundVolume * soundRange(), 1F, false);
        }
    }

    // A client's copy of a gate it can't see (the far end of a GDO's connection, usually unloaded or in another
    // dimension), kept only to draw it on the GDO's screen: never in the world, silent, updated from the server's
    // data for the real gate and otherwise animated by ticking it as the client does its own gates.
    private boolean isRemoteView = false;

    public static SGBaseTE remoteView(World clientWorld, BlockPos pos) {
        SGBaseTE view = new SGBaseTE();
        view.setWorld(clientWorld);
        view.setPos(pos);
        view.isRemoteView = true;
        return view;
    }

    // Takes on the real gate's latest data, starting the event horizon's opening or closing surge as onDataPacket does
    public void applyRemoteState(NBTTagCompound nbt) {
        SGState oldState = state;
        readFromNBT(nbt);
        if (isMerged && state != oldState) {
            if (state == SGState.Transient) {
                startOpening();
            } else if (state == SGState.Disconnecting) {
                initiateClosingTransient();
            }
        }
    }

    void clientUpdate() {
        updateOriFill();
        lastRingAngle = ringAngle;
        switch (state) {
            case Dialling:
                if (timeout > 0) {
                    // As on the server, where it will be at the end of this tick
                    double step = (double) (maxTimeout - timeout + 1) / (double) maxTimeout;
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

    // *********************************************************************************************
    // Event Horizon and Iris Controllers
    // *********************************************************************************************

    public EnumActionResult applyChevronUpgrade(ItemStack stack, EntityPlayer player) {
        if (getGateType() == GateType.ORI) {
            if (!getWorld().isRemote) {
                sendErrorMsg(player, "oriNoUpgrades");
            }
            return EnumActionResult.SUCCESS;
        }
        if (!getWorld().isRemote && !hasChevronUpgrade && stack.getCount() > 0) {
            //System.out.printf("SGBaseTE.applyChevronUpgrade: Installing chevron upgrade\n");
            hasChevronUpgrade = true;
            stack.shrink(1);
            markChanged();
        }
        return EnumActionResult.SUCCESS;
    }

    public EnumActionResult applyIrisUpgrade(ItemStack stack, EntityPlayer player) {
        return applyBarrierUpgrade(stack, player, false);
    }

    public EnumActionResult applyShieldUpgrade(ItemStack stack, EntityPlayer player) {
        return applyBarrierUpgrade(stack, player, true);
    }

    // Fits an iris (shield false) or a shield. A gate has one or the other: fitting one in place of the other, while
    // it's open, hands the player back the one taken out.
    EnumActionResult applyBarrierUpgrade(ItemStack stack, EntityPlayer player, boolean shield) {
        if (getGateType() == GateType.ORI) {
            if (!getWorld().isRemote) {
                sendErrorMsg(player, "oriNoUpgrades");
            }
            return EnumActionResult.SUCCESS;
        }
        if (!getWorld().isRemote && stack.getCount() > 0 && !(hasIrisUpgrade && hasShield == shield)) {
            if (hasIrisUpgrade) {
                if (irisState != IrisState.Open) {
                    sendErrorMsg(player, "openBeforeReplacing");
                    return EnumActionResult.SUCCESS;
                }
                ItemStack removed = new ItemStack(hasShield ? SGCraft.sgShieldUpgrade : SGCraft.sgIrisUpgrade);
                if (!player.inventory.addItemStackToInventory(removed)) {
                    player.dropItem(removed, false);
                }
            }
            hasIrisUpgrade = true;
            hasShield = shield;
            stack.shrink(1);
            markChanged();
            updateIrisEntity();
        }
        return EnumActionResult.SUCCESS;
    }

    public EnumActionResult applyPegasusUpgrade(ItemStack stack, EntityPlayer player) {
        if (!getWorld().isRemote && this.gateType != GateType.PEGASUS.id) {
            this.gateType = GateType.PEGASUS.id; //Set type to pegasus style
            this.ringRotationSpeed = 6.0D; //Set rotation speed to 6 as per configurator default
            stack.shrink(1);
            markChanged();
        }

        return EnumActionResult.SUCCESS;
    }

    public EnumActionResult applyUniverseUpgrade(ItemStack stack, EntityPlayer player) {
        if (!getWorld().isRemote && this.gateType != GateType.UNIVERSE.id) {
            this.gateType = GateType.UNIVERSE.id;
            stack.shrink(1);
            markChanged();
        }

        return EnumActionResult.SUCCESS;
    }

    public double[][][] getEventHorizonGrid() {
        if (ehGrid == null) {
            int m = getShape().ehGridRadialSize;
            int n = getShape().ehGridPolarSize;
            ehGrid = new double[2][n + 2][m + 1];
            for (int i = 0; i < 2; i++) {
                ehGrid[i][0] = ehGrid[i][n];
                ehGrid[i][n + 1] = ehGrid[i][1];
            }
        }
        return ehGrid;
    }

    // On the client: when an Ori gate's horizon started filling in (world time), or -1 if it isn't
    private long oriFillStart = -1;

    // The event horizon opens: an Ori gate's first fills in from the ring, and the kawoosh follows (see clientUpdate)
    void startOpening() {
        if (getGateType() == GateType.ORI) {
            oriFillStart = world.getTotalWorldTime();
        } else {
            initiateOpeningTransient();
        }
    }

    // How far (0 to 1) an Ori gate's horizon has filled in from the ring towards the middle; any other gate's is full
    public double horizonFill(float partialTicks) {
        if (oriFillStart < 0 || world == null) {
            return 1;
        }
        return Math.max(0, Math.min(1, (world.getTotalWorldTime() - oriFillStart + (double) partialTicks) / oriFillTicks));
    }

    // Once an Ori gate's horizon has filled in, its kawoosh bursts out
    void updateOriFill() {
        if (oriFillStart >= 0 && (state != SGState.Transient || world.getTotalWorldTime() - oriFillStart >= oriFillTicks)) {
            oriFillStart = -1;
            if (state == SGState.Transient) {
                initiateOpeningTransient();
            }
        }
    }

    void initiateOpeningTransient() {
        double v[][] = getEventHorizonGrid()[1];
        int n = getShape().ehGridPolarSize;
        for (int j = 0; j <= n+1; j++) {
            v[j][0] = openingTransientIntensity;
            v[j][1] = v[j][0] + openingTransientRandomness * random.nextGaussian();
        }
    }

    void initiateClosingTransient() {
        //numEngagedChevrons = 0;
        double v[][] = getEventHorizonGrid()[1];
        int m = getShape().ehGridRadialSize;
        int n = getShape().ehGridPolarSize;
        for (int i = 1; i < m; i++)
            for (int j = 1; j <= n; j++)
                v[j][i] += closingTransientRandomness * random.nextGaussian();
    }

    void applyRandomImpulse() {
        double v[][] = getEventHorizonGrid()[1];
        int m = getShape().ehGridRadialSize;
        int n = getShape().ehGridPolarSize;
        int i = random.nextInt(m - 1) + 1;
        int j = random.nextInt(n) + 1;
        v[j][i] += 0.05 * random.nextGaussian();
    }

    void updateEventHorizon() {
        double grid[][][] = getEventHorizonGrid();
        double u[][] = grid[0];
        double v[][] = grid[1];
        int m = getShape().ehGridRadialSize;
        int n = getShape().ehGridPolarSize;
        double ka = getShape().ehAngularCoupling; // same ripple speed around for any slice count
        // A fine grid takes several smaller steps per tick to stay stable, with the damping
        // split between them so that a tick damps the same overall
        int steps = getShape().ehSubsteps;
        double dt = 1.0 / steps;
        double asq = GateShape.EH_WAVE_SPEED_SQ;
        double d = Math.pow(0.95, dt);
        for (int step = 0; step < steps; step++) {
            for (int i = 1; i < m; i++)
                for (int j = 1; j <= n; j++) {
                    double du_dr = 0.5 * (u[j][i+1] - u[j][i-1]);
                    double d2u_drsq = u[j][i+1] - 2 * u[j][i] + u[j][i-1];
                    double d2u_dthsq = u[j+1][i] - 2 * u[j][i] + u[j-1][i];
                    v[j][i] = d * v[j][i] + (asq * dt) * (d2u_drsq + du_dr / i + ka * d2u_dthsq / (i * i));
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
        }
        //dumpGrid("u", u);
        //dumpGrid("v", v);
    }

    void dumpGrid(String label, double g[][]) {
        System.out.printf("SGBaseTE: %s:\n", label);
        int m = getShape().ehGridRadialSize;
        int n = getShape().ehGridPolarSize;
        for (int j = 0; j <= n+1; j++) {
            for (int i = 0; i <= m; i++)
                System.out.printf(" %6.3f", g[j][i]);
            System.out.print("\n");
        }
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
                    irisPhase = min(irisPhase + irisStep(), maxIrisPhase);
                else
                    enterIrisState(IrisState.Open);
                break;
            case Closing:
                if (irisPhase > 0)
                    irisPhase = max(irisPhase - irisStep(), 0);
                else
                    enterIrisState(IrisState.Closed);
                break;
        }
    }

    void enterIrisState(IrisState newState) {
        if (irisState != newState) {
            if (isRemoteView) { // only animating a copy of a gate elsewhere: nothing here to tell
                irisState = newState;
                return;
            }
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

    // How far the iris moves each tick: the blades take maxIrisPhase ticks, a shield only shieldRiseTicks
    int irisStep() {
        return hasShield ? Math.max(1, maxIrisPhase / shieldRiseTicks) : 1;
    }

    // A raised shield takes energy every tick, and falls when the gate has none left to give it
    void tickShieldEnergy() {
        if (hasIrisUpgrade && hasShield && isMerged && (irisState == IrisState.Closing || irisState == IrisState.Closed)) {
            if (!useEnergy(shieldEnergyPerTick)) {
                enterIrisState(IrisState.Opening);
            }
        }
    }

    public void closeIris() {
        if (hasShield && !world.isRemote && !requiresNoPower && !energyIsAvailable(shieldEnergyPerTick)) {
            return; // no energy to raise the shield
        }
        if (isMerged && hasIrisUpgrade && irisState != IrisState.Closed) {
            enterIrisState(IrisState.Closing);
        }
    }

    static int rdx[] = {1, 0, -1, 0};
    static int rdz[] = {0, -1, 0, 1};

    // Find locations of tile entities that could connect to the stargate ring.
    public Collection<TileEntity> adjacentTiles() {
        Collection<TileEntity> result = new ArrayList<>();
        Trans3 t = localToGlobalTransformation();
        for (int i = -2; i <= 2; i++) {
            BlockPos bp = t.p(i, -1, 0).blockPos();
            TileEntity te = getWorldTileEntity(world, bp);
            if (te != null)
                result.add(te);
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
        for (TileEntity te : adjacentTiles()) {
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
        for (TileEntity te : adjacentTiles()) {
            if (te instanceof IComputerInterface) {
                ((IComputerInterface)te).postEvent(this, name, args);
            }
        }
        this.debugCCInterface = false;
    }

    public String sgStateDescription() {
        return sgStateDescription(state);
    }

    String sgStateDescription(SGState state) {
        switch (state) {
            case Idle: return "Idle";
            case Dialling:
            case attemptToDial:
            case InterDialling: return "Dialling";
            case SyncAwait:
            case EstablishingConnection:
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

    public double getMaxEnergyBuffer() {
        return this.maxEnergyBuffer;
    }

    public double getBaseMaxEnergyBuffer() {
        return this.maxEnergyBuffer;
    }
}
