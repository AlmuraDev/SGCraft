//------------------------------------------------------------------------------------------------
//
//   SG Craft - Main Class
//
//------------------------------------------------------------------------------------------------

package gcewing.sg;

import gcewing.sg.block.DHDBlock;
import gcewing.sg.block.EventHorizonLightBlock;
import gcewing.sg.block.GateSpawnerBlock;
import gcewing.sg.block.NaquadahBlock;
import gcewing.sg.block.NaquadahOreBlock;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.block.SGPowerBlock;
import gcewing.sg.block.SGRingBlock;
import gcewing.sg.block.SupergateBaseBlock;
import gcewing.sg.block.SupergateRingBlock;
import gcewing.sg.client.gui.SGGui;
import gcewing.sg.command.SGPowerCommand;
import gcewing.sg.command.SGSpawnCommand;
import gcewing.sg.container.DHDFuelContainer;
import gcewing.sg.container.PowerContainer;
import gcewing.sg.container.SGBaseContainer;
import gcewing.sg.entity.EntityStargateIris;
import gcewing.sg.entity.SGEntity;
import gcewing.sg.features.configurator.ConfiguratorItem;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.features.gdo.GdoItem;
import gcewing.sg.features.gdo.network.GdoNetworkHandler;
import gcewing.sg.features.ic2.zpm.modulehub.ZpmHub;
import gcewing.sg.features.ic2.zpm.modulehub.ZpmHubContainer;
import gcewing.sg.features.ic2.zpm.modulehub.ZpmHubTE;
import gcewing.sg.features.pdd.AddressNameRegistry;
import gcewing.sg.features.zpm.ZPMItem;
import gcewing.sg.features.zpm.ZPMMultiplierRegistry;
import gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCartContainer;
import gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCart;
import gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCartTE;
import gcewing.sg.features.edd.EddItem;
import gcewing.sg.features.obelisk.AsgardObeliskBlock;
import gcewing.sg.features.obelisk.AsgardObeliskTE;
import gcewing.sg.features.obelisk.AsgardObelisks;
import gcewing.sg.features.obelisk.ObeliskNetworkHandler;
import gcewing.sg.features.rings.RingFuelContainer;
import gcewing.sg.features.rings.RingHolds;
import gcewing.sg.features.rings.RingPanelBlock;
import gcewing.sg.features.rings.RingPlatformMalisis;
import gcewing.sg.features.rings.RingPlatformBlock;
import gcewing.sg.features.rings.RingPlatformTE;
import gcewing.sg.features.rings.RingRemoteItem;
import gcewing.sg.features.rings.RingTransports;
import gcewing.sg.features.zat.Zat;
import gcewing.sg.features.zpm.link.ZpmLinkNetworkHandler;
import gcewing.sg.features.zpm.link.ZpmLinks;
import gcewing.sg.features.zat.ZatItem;
import gcewing.sg.features.zat.ZatNetworkHandler;
import gcewing.sg.features.cityshield.CityShieldBlock;
import gcewing.sg.features.cityshield.CityShieldTE;
import gcewing.sg.features.cityshield.network.CityShieldNetworkHandler;
import gcewing.sg.features.naquadahgenerator.NaquadahGeneratorBlock;
import gcewing.sg.features.naquadahgenerator.NaquadahGeneratorTE;
import gcewing.sg.features.naquadahgenerator.NaquadahGenerators;
import gcewing.sg.features.naquadahgenerator.network.NaquadahGeneratorNetworkHandler;
import gcewing.sg.features.shield.ShieldConsoleBlock;
import gcewing.sg.features.shield.network.ShieldConsoleNetworkHandler;
import gcewing.sg.features.pdd.PddItem;
import gcewing.sg.features.pdd.network.PddNetworkHandler;
import gcewing.sg.features.tokra.SGTradeHandler;
import gcewing.sg.features.tokra.TokraVillagerWorldRegistry;
import gcewing.sg.features.oc.OCIntegration;
import gcewing.sg.features.zpm.console.ZpmConsole;
import gcewing.sg.features.zpm.console.ZpmConsoleContainer;
import gcewing.sg.features.zpm.console.ZpmConsoleTE;
import gcewing.sg.generator.FeatureGeneration;
import gcewing.sg.generator.FeatureIgloo;
import gcewing.sg.generator.FeatureJungleTemple;
import gcewing.sg.generator.FeatureSwampHut;
import gcewing.sg.generator.FeatureUnderDesertPyramid;
import gcewing.sg.generator.FeatureVillageStargate;
import gcewing.sg.generator.FeatureNetherFortress;
import gcewing.sg.generator.FeatureOceanMonument;
import gcewing.sg.generator.GeneratorAddressRegistry;
import gcewing.sg.interfaces.IIntegration;
import gcewing.sg.interfaces.SoundSource;
import gcewing.sg.item.SGChevronUpgradeItem;
import gcewing.sg.item.SGIrisUpgradeItem;
import gcewing.sg.item.SGShieldUpgradeItem;
import gcewing.sg.item.SGInvulnerabilityItem;
import gcewing.sg.item.SGPegasusUpgradeItem;
import gcewing.sg.item.SGUniverseUpgradeItem;
import gcewing.sg.item.SGRingItem;
import gcewing.sg.network.GuiNetworkHandler;
import gcewing.sg.network.SupergatePowerNetworkHandler;
import gcewing.sg.network.SGChannel;
import gcewing.sg.generator.NaquadahOreWorldGen;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GateType;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.ComputerLookupLimiter;
import gcewing.sg.util.GeneralAddressRegistry;
import gcewing.sg.generator.GateAddressBookFunction;
import gcewing.sg.util.LootTableConfig;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import gcewing.sg.util.Info;
import gcewing.sg.util.PermissionsUtil;
import gcewing.sg.util.SGChunkData;
import gcewing.sg.util.WorldNames;
import gcewing.sg.util.Sound;
import net.minecraft.block.Block;
import net.minecraft.util.NonNullList;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import ninja.leaping.configurate.ConfigurationNode;

import static java.util.Objects.requireNonNull;
import static net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import static net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(modid = Info.modID, name = Info.modName, version = Info.versionNumber,
    acceptableRemoteVersions = Info.versionBounds, dependencies = "after:opencomputers;after:ic2;after:computercraft;after:malisiscore")

public class SGCraft extends BaseMod<SGCraftClient> {

    public static final Material machineMaterial = new Material(MapColor.IRON);

    public static SGCraft mod;

    public static SGChannel channel;
    public static BaseTEChunkManager chunkManager;

    public static SGBaseBlock sgBaseBlock;
    public static EventHorizonLightBlock eventHorizonLightBlock;
    // Bases that build a gate of their own type, for building one by hand in creative mode
    public static SGBaseBlock pegasusBaseBlock, universeBaseBlock;
    public static SGRingBlock sgRingBlock;
    public static SupergateBaseBlock supergateBaseBlock;
    public static SupergateRingBlock supergateRingBlock;
    // The order the gates' blocks are shown in the creative tab: each type's base, then the block
    // that builds that gate finished (see GateSpawnerBlock), side by side, whatever their item ids
    // (which a world keeps from when they were added)
    public static final List<Block> creativeGateOrder = new ArrayList<>();
    public static DHDBlock sgControllerBlock;
    public static Block naquadahBlock, naquadahOre;
    public static Block shieldConsole;
    public static Block asgardObelisk;
    public static Block asgardObeliskCeltic;
    public static Block ringPlatform, ringPlatformCeiling, ringPanel;
    public static Block cityShieldGenerator;
    public static Block naquadahGeneratorMk1, naquadahGeneratorMk2;
    public static SGPowerBlock sgPowerUnit;

    public static Item invulnerabilityUpgrade;
    public static Item ringRemote;
    public static Item naquadah, naquadahIngot, sgCoreCrystal, sgControllerCrystal, sgChevronUpgrade, sgIrisUpgrade, sgShieldUpgrade, sgIrisBlade, pegasus_upgrade, universe_upgrade;
    public static Item tollan_phase_shift_device;

    public static Block ic2PowerUnit;
    public static Item ic2Capacitor;

    public static boolean addOresToExistingWorlds;
    public static NaquadahOreWorldGen naquadahOreGenerator;

    public static BaseSubsystem ic2Integration; //[IC2]
    public static IIntegration ccIntegration; //[CC]
    public static OCIntegration ocIntegration; //[OC]

    public static Block zpm_interface_cart, zpm_console, zpm_hub;
    public static Item zpm, gdo, pdd, edd, configurator, zat;

    public static CreativeTabs creativeTabs;

    // Villager Profession for Generators
    public static VillagerProfession tokraProfession;

    // Block Harvests
    public static boolean canHarvestDHD = false;
    public static boolean canHarvestSGBaseBlock = false;
    public static boolean canHarvestSGRingBlock = false;

    // IC2 Options
    public static int Ic2SafeInput = 2048;
    public static int Ic2MaxEnergyBuffer = 1000000;
    public static double Ic2euPerSGEnergyUnit = 20.0;
    public static double Ic2FEPerEU = 4.0; // Forge Energy given per EU when an IC2 block feeds a Forge Energy (RF/FE) acceptor
    public static int Ic2PowerTETier = 5; //3

    // SG Power Block Options
    public static int FPMaxEnergyBuffer = 4000000;
    public static double FPPerSGEnergyUnit = 80.0;

    // ZPM Console Block Options
    public static double ZPMEnergyPerSGEnergyUnit = 10.0;

    // Client Options
    public static boolean useHDEventHorizionTexture = true;
    public static boolean saveAddressToClipboard = false;
    public static boolean displayGuiPowerDebug = true;

    // World data fixes
    public static boolean forceSGBaseTEUpdate = false;
    public static boolean forceDHDCfgUpdate = false;
    public static boolean forceIC2CfgUpdate = false;
    public static boolean forceFPCfgUpdate = false;
    public static boolean forceZPMCfgUpdate = false;
    public static boolean forceGateAccessSystemReset = false;
    public static boolean forcePlayerAccessSystemReset = false;

    // Unbreakable Block Option
    public static boolean wormholeCanDestroyUnbreakableBlocks = false;

    // Search Ranges for ZPM & Tools
    public static int zpmSearchRange = 6;
    public static int eventHorizonLightLevel = 12; // Light (0-15) a gate gives off while its event horizon is up; 0 for none
    public static int toolSearchRange = 6;

    public static PermissionsUtil permissionsUtil;

    public SGCraft() {
        mod = this;

    }

    @Mod.EventHandler
    @Override
    public void preInit(FMLPreInitializationEvent e) {
        this.creativeTab = new CreativeTabs("sgcraft:sgcraft") {
            @Override public ItemStack createIcon() {
                return new ItemStack(Item.getItemFromBlock(sgBaseBlock));
            }

            // The gates' blocks together, in creativeGateOrder, where the first of them would be
            @Override public void displayAllRelevantItems(NonNullList<ItemStack> items) {
                super.displayAllRelevantItems(items);
                List<ItemStack> gates = new ArrayList<>();
                for (Block block : creativeGateOrder) {
                    for (ItemStack stack : items) {
                        if (Block.getBlockFromItem(stack.getItem()) == block) {
                            gates.add(stack);
                        }
                    }
                }
                int at = -1;
                for (int i = 0; i < items.size(); i++) {
                    if (gates.contains(items.get(i))) {
                        at = at < 0 ? i : at;
                        items.remove(i--);
                    }
                }
                if (at >= 0) {
                    items.addAll(at, gates);
                }
            }
        };
        FMLCommonHandler.instance().bus().register(this);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            OBJLoader.INSTANCE.addDomain("sgcraft");
        }
        ic2Integration = integrateWithMod("ic2", "gcewing.sg.features.ic2.IC2Integration"); //[IC2]
        ccIntegration = (IIntegration) integrateWithMod("computercraft", "gcewing.sg.features.cc.CCIntegration"); //[CC]
        ocIntegration = (OCIntegration)integrateWithMod("opencomputers", "gcewing.sg.features.oc.OCIntegration"); //[OC]

        // With or without IC2: without it they power stargates and Forge Energy machines, as the ZPM console does
        GameRegistry.registerTileEntity(ZpmInterfaceCartTE.class, new ResourceLocation(this.modID + ":" + "tile_zpminterfacecart"));
        GameRegistry.registerTileEntity(ZpmHubTE.class, new ResourceLocation(this.modID + ":" + "tile_zpmmodulehub"));

        GameRegistry.registerTileEntity(ZpmConsoleTE.class, new ResourceLocation(this.modID + ":" + "tile_zpmconsole"));
        GameRegistry.registerTileEntity(AsgardObeliskTE.class, new ResourceLocation(this.modID + ":" + "tile_asgard_obelisk"));
        // One name either way, so platforms load with or without MalisisCore (which its fuel screen uses when installed)
        ResourceLocation ringPlatformId = new ResourceLocation(this.modID + ":" + "tile_ring_platform");
        if (isModLoaded("malisiscore")) {
            RingPlatformMalisis.registerTileEntity(ringPlatformId);
        } else {
            GameRegistry.registerTileEntity(RingPlatformTE.class, ringPlatformId);
        }
        // The City Shield Generator holds its ZPMs in a MalisisCore inventory, so needs MalisisCore
        if (isModLoaded("malisiscore")) {
            GameRegistry.registerTileEntity(CityShieldTE.class, new ResourceLocation(this.modID + ":" + "tile_cityshield"));
            MinecraftForge.EVENT_BUS.register(CityShieldTE.class); // keeps explosions outside the shield from reaching in
            GameRegistry.registerTileEntity(NaquadahGeneratorTE.class, new ResourceLocation(this.modID + ":" + "tile_naquadah_generator"));
        }

        LootFunctionManager.registerFunction(new GateAddressBookFunction.Serializer());
        MinecraftForge.EVENT_BUS.register(LootTableConfig.class);
        MinecraftForge.EVENT_BUS.register(AsgardObelisks.class); // beams players a moment after they touch the crystal
        MinecraftForge.EVENT_BUS.register(Zat.class); // fires zats once they've charged
        MinecraftForge.EVENT_BUS.register(ZpmLinks.class); // linking ZPM devices to stargates by sneak-clicking
        MinecraftForge.EVENT_BUS.register(RingHolds.class); // ring transport players held still and unharmed

        super.preInit(e);
    }

    @Mod.EventHandler
    @Override
    public void init(FMLInitializationEvent e) {
        super.init(e);
        System.out.printf("SGCraft.init\n");
        configure();
        channel = new SGChannel(Info.modID);
        new SupergatePowerNetworkHandler(Info.modID + "-power");
        new ObeliskNetworkHandler(Info.modID + "-obelisk");
        new ZpmLinkNetworkHandler(Info.modID + "-zpmlink");
        new ZatNetworkHandler(Info.modID + "-zat");
        chunkManager = new BaseTEChunkManager(this);
    }

    @Mod.EventHandler
    @Override
    public void postInit(FMLPostInitializationEvent e) {
        if (isModLoaded("spongeapi")) {
            permissionsUtil = new PermissionsUtil();
        } else {
            System.out.println("SGCraft - SpongeAPI not found, permissions system NOT initialized!");
        }
        super.postInit(e);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new SGSpawnCommand());
        e.registerServerCommand(new SGPowerCommand());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent e) {
        checkInjectLootTables(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0));
    }

    @Override
    protected SGCraftClient initClient() {
        return new SGCraftClient(this);
    }

    @Override
    void configure() {
        DHDTE.configure(config);
        NaquadahOreWorldGen.configure(config);
        SGBaseBlock.configure(config);
        SGBaseTE.configure(config);
        ComputerLookupLimiter.configure(config);
        Zat.configure(config);
        RingTransports.configure(config);
        if (isModLoaded("malisiscore")) {
            CityShieldTE.configure(config);
            NaquadahGenerators.configure(config);
        }
        FeatureGeneration.configure(config);

        // (Adding ore to existing chunks is read with the ore's settings: NaquadahOreWorldGen.configure)

        // Client-Side Options
        useHDEventHorizionTexture = config.getBoolean("client", "useHDEventHorizonTexture", useHDEventHorizionTexture);
        saveAddressToClipboard = config.getBoolean("client", "saveAddressToClipboard", saveAddressToClipboard);
        displayGuiPowerDebug = config.getBoolean("client", "showPowerValuesOnGui", displayGuiPowerDebug);


        // ZPM multiplier configuration loader
        final ConfigurationNode rootNode;
        try {
            rootNode = ZPMMultiplierRegistry.createRootNode(Paths.get(".", "config", "SGCraft", "zpm.yml"));
            ZPMMultiplierRegistry.populateMultipliers(rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Tokra Villager World configuration loader
        final ConfigurationNode tokraNode;
        try {
            tokraNode = TokraVillagerWorldRegistry.createRootNode(Paths.get(".", "config", "SGCraft", "tokra.yml"));
            TokraVillagerWorldRegistry.populateTokraVillagerWorlds(tokraNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void registerOther() {
        MinecraftForge.TERRAIN_GEN_BUS.register(this);
    }

    @Override
    protected void registerBlocks() {
        // The hidden light behind a closed iris (see EventHorizonLightBlock): no item, never in the creative tab
        eventHorizonLightBlock = new EventHorizonLightBlock();
        eventHorizonLightBlock.setTranslationKey(assetKey + ":event_horizon_light");
        eventHorizonLightBlock.setRegistryName(assetKey, "event_horizon_light");
        ForgeRegistries.BLOCKS.register(eventHorizonLightBlock);

        sgRingBlock = newBlock("stargateRing", SGRingBlock.class, SGRingItem.class);
        // Each type's base, then (if they are on) the creative-only block that builds that gate
        // finished, for testing: stargates, then supergates
        boolean spawners = config.getBoolean("options", "gateSpawnerBlocks", true);
        sgBaseBlock = newBlock("stargateBase", SGBaseBlock.class);
        creativeGateOrder.add(sgBaseBlock);
        if (spawners) {
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(sgBaseBlock, GateType.MILKY_WAY), "gatespawner_milkyway_stargate"));
        }
        pegasusBaseBlock = addBlock(new SGBaseBlock(GateType.PEGASUS, "stargatebase_front_pegasus"), "stargateBasePegasus");
        creativeGateOrder.add(pegasusBaseBlock);
        if (spawners) {
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(sgBaseBlock, GateType.PEGASUS), "gatespawner_pegasus_stargate"));
        }
        universeBaseBlock = addBlock(new SGBaseBlock(GateType.UNIVERSE, "stargatebase_front_universe"), "stargateBaseUniverse");
        creativeGateOrder.add(universeBaseBlock);
        if (spawners) {
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(sgBaseBlock, GateType.UNIVERSE), "gatespawner_universe_stargate"));
        }
        supergateRingBlock = newBlock("supergateRing", SupergateRingBlock.class, SGRingItem.class);
        supergateBaseBlock = newBlock("supergateBase", SupergateBaseBlock.class);
        creativeGateOrder.add(supergateBaseBlock);
        if (spawners) {
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(supergateBaseBlock, GateType.MILKY_WAY), "gatespawner_milkyway_supergate"));
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(supergateBaseBlock, GateType.PEGASUS), "gatespawner_pegasus_supergate"));
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(supergateBaseBlock, GateType.UNIVERSE), "gatespawner_universe_supergate"));
            creativeGateOrder.add(addBlock(new GateSpawnerBlock(supergateBaseBlock, GateType.ORI), "gatespawner_ori_supergate"));
        }
        sgControllerBlock = newBlock("stargateController", DHDBlock.class);
        naquadahBlock = newBlock("naquadahBlock", NaquadahBlock.class);
        naquadahOre = newBlock("naquadahOre", NaquadahOreBlock.class);
        sgPowerUnit = newBlock("sgPowerUnit", SGPowerBlock.class);

        zpm_interface_cart = newBlock("zpm_interface_cart", ZpmInterfaceCart.class);
        zpm_hub = newBlock("zpm_hub", ZpmHub.class);
        zpm_console = newBlock("zpm_console", ZpmConsole.class);
        asgardObelisk = newBlock("asgard_obelisk", AsgardObeliskBlock.class);
        asgardObeliskCeltic = addBlock(new AsgardObeliskBlock(AsgardObeliskBlock.Type.CELTIC), "asgard_obelisk_celtic");
        ringPlatform = addBlock(new RingPlatformBlock(false), "ring_platform");
        ringPlatformCeiling = addBlock(new RingPlatformBlock(true), "ring_platform_ceiling");
        ringPanel = newBlock("ring_panel", RingPanelBlock.class);

        this.setOptions();
    }

    @Override
    protected void registerItems() {
        naquadah = newItem("naquadah"); //, "Naquadah");
        naquadahIngot = newItem("naquadahIngot"); //, "Naquadah Alloy Ingot");
        sgCoreCrystal = newItem("sgCoreCrystal"); //, "Stargate Core Crystal");
        sgControllerCrystal = newItem("sgControllerCrystal"); //, "Stargate Controller Crystal");
        sgChevronUpgrade = addItem(new SGChevronUpgradeItem(), "sgChevronUpgrade");
        sgIrisUpgrade = addItem(new SGIrisUpgradeItem(), "sgIrisUpgrade");
        sgShieldUpgrade = addItem(new SGShieldUpgradeItem(), "sgShieldUpgrade");
        invulnerabilityUpgrade = addItem(new SGInvulnerabilityItem(), "invulnerability_upgrade"); // creative only: no recipe
        sgIrisBlade = newItem("sgIrisBlade");
        if (isModLoaded("ic2")) {
            ic2Capacitor = newItem("ic2Capacitor");
        }

        zpm = addItem(new ZPMItem(), "zpm");
        zat = addItem(new ZatItem(), "zat");
        ringRemote = addItem(new RingRemoteItem(), "ring_remote");
        tollan_phase_shift_device = newItem("tollan_phase_shift_device");

        pegasus_upgrade = addItem(new SGPegasusUpgradeItem(), "pegasus_upgrade");
        universe_upgrade = addItem(new SGUniverseUpgradeItem(), "universe_upgrade");

        if (isModLoaded("malisiscore")) {
            new GuiNetworkHandler(Info.modID+"-GUI");

            gdo = addItem(new GdoItem(), "gdo");
            new GdoNetworkHandler(Info.modID+"-gdo");

            pdd = addItem(new PddItem(), "pdd");
            new PddNetworkHandler(Info.modID+"-pdd");
            edd = addItem(new EddItem(), "edd");

            shieldConsole = newBlock("shield_console", ShieldConsoleBlock.class);
            new ShieldConsoleNetworkHandler(Info.modID+"-shield");

            cityShieldGenerator = newBlock("city_shield_generator", CityShieldBlock.class);
            new CityShieldNetworkHandler(Info.modID+"-cityshield");

            naquadahGeneratorMk1 = addBlock(new NaquadahGeneratorBlock(NaquadahGenerators.Mark.I), "naquadah_generator_mk1");
            naquadahGeneratorMk2 = addBlock(new NaquadahGeneratorBlock(NaquadahGenerators.Mark.II), "naquadah_generator_mk2");
            new NaquadahGeneratorNetworkHandler(Info.modID + "-naqgen");

            // PDD default address loader
            final ConfigurationNode rootNode;
            try {
                rootNode = AddressNameRegistry.createRootNode(Paths.get(".", "config", "SGCraft", "pdd.yml"));
                AddressNameRegistry.populateNames(rootNode);
            } catch (IOException e) {
                e.printStackTrace();
            }

            configurator = addItem(new ConfiguratorItem(), "configurator");
            new ConfiguratorNetworkHandler(Info.modID+"-configurator");
        }

        // Structure Generated Address Registry
        final ConfigurationNode rootNode;
        try {
            rootNode = GeneratorAddressRegistry.createRootNode(Paths.get(".", "config", "SGCraft", "generator.yml"));
            GeneratorAddressRegistry.populateAddresses(rootNode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // General Gate Address Registry
        final ConfigurationNode rootNode2;
        try {
            rootNode2 = GeneralAddressRegistry.createRootNode(Paths.get(".", "config", "SGCraft", "general.yml"));
            GeneralAddressRegistry.populateAddresses(rootNode2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void playSound(SoundSource source, SoundEvent sound) {
        playSound(source, sound, SoundCategory.AMBIENT);
    }

    @SideOnly(Side.CLIENT)
    public static void playSound(SoundSource source, SoundEvent sound, SoundCategory category) {
        SoundHandler soundHandler = getSoundHandler();
        soundHandler.playSound(new Sound(source, sound, category));
    }

    @SideOnly(Side.CLIENT)
    private static SoundHandler getSoundHandler() {
        return Minecraft.getMinecraft().getSoundHandler();
    }

    public static boolean isValidStargateUpgrade(Item item) {
        return item == sgChevronUpgrade || item == sgIrisUpgrade || item == sgShieldUpgrade;
    }

    @Override
    protected void registerOres() {
        addOre("oreNaquadah", naquadahOre);
        addOre("naquadah", naquadah);
        addOre("ingotNaquadahAlloy", naquadahIngot);
    }

    @Override
    protected void registerRecipes() {
        ItemStack chiselledSandstone = new ItemStack(Blocks.SANDSTONE, 1, 1);
        ItemStack smoothSandstone = new ItemStack(Blocks.SANDSTONE, 1, 2);
        ItemStack sgChevronBlock = new ItemStack(sgRingBlock, 1, 1);
        ItemStack blueDye = new ItemStack(Items.DYE, 1, 4);
        ItemStack orangeDye = new ItemStack(Items.DYE, 1, 14);

        if (config.getBoolean("recipes", "naquadah", false)) {
            newRecipe("naquadah", naquadah, 1, "aaa", "aaa", "bcd", 'a', Blocks.AIR, 'b', Items.COAL, 'c', Items.SLIME_BALL, 'd', Items.BLAZE_POWDER);
        }

        if (config.getBoolean("recipes", "naquadahIngot", true)) {
            newShapelessRecipe("naquadahingot", naquadahIngot, 1, Ingredient.fromItem(Items.IRON_INGOT), Ingredient.fromItem(naquadah));
        }

        if (config.getBoolean("recipes", "naquadahIngotFromBlock", true)) {
            newRecipe("naquadahingot_from_block", naquadahIngot, 9, "B", 'B', naquadahBlock);
        }

        if (config.getBoolean("recipes", "naquadahBlock", true)) {
            newRecipe("naquadahblock", naquadahBlock, 1, "NNN", "NNN", "NNN", 'N', "ingotNaquadahAlloy");
        }

        // Thor's hammer in sandstone on a stone pillar, an ender pearl for its crystal
        if (config.getBoolean("recipes", "asgardObelisk", true)) {
            newRecipe("asgard_obelisk", asgardObelisk, 1, " C ", "SES", "SNS", 'C', chiselledSandstone, 'S', Blocks.STONEBRICK,
                'E', Items.ENDER_PEARL, 'N', "ingotNaquadahAlloy");
        }
        // The zat: naquadah alloy round a core crystal, with a purple grip
        if (config.getBoolean("recipes", "zat", true)) {
            newRecipe("zat", zat, 1, " NC", "NRN", "PN ", 'N', "ingotNaquadahAlloy", 'C', sgCoreCrystal, 'R', Blocks.REDSTONE_BLOCK,
                'P', new ItemStack(Items.DYE, 1, 5));
        }
        // Ring platforms: naquadah alloy round an ender pearl in a ring of iron; the panel and the remote in gold
        if (config.getBoolean("recipes", "ringPlatform", true)) {
            newRecipe("ring_platform", ringPlatform, 1, "INI", "NEN", "INI", 'I', Blocks.IRON_BLOCK, 'N', "ingotNaquadahAlloy", 'E', Items.ENDER_PEARL);
            newRecipe("ring_platform_ceiling", ringPlatformCeiling, 1, "X", 'X', ringPlatform);
            newRecipe("ring_platform_floor", ringPlatform, 1, "X", 'X', ringPlatformCeiling);
        }
        if (config.getBoolean("recipes", "ringPanel", true)) {
            newRecipe("ring_panel", ringPanel, 1, "GRG", "GNG", 'G', Items.GOLD_INGOT, 'R', Items.REDSTONE, 'N', "ingotNaquadahAlloy");
        }
        if (config.getBoolean("recipes", "ringRemote", true)) {
            newRecipe("ring_remote", ringRemote, 1, " R ", "GNG", " G ", 'G', Items.GOLD_INGOT, 'R', Items.REDSTONE, 'N', "ingotNaquadahAlloy");
        }
        // The knotwork obelisk, all in sandstone
        if (config.getBoolean("recipes", "asgardObeliskCeltic", true)) {
            newRecipe("asgard_obelisk_celtic", asgardObeliskCeltic, 1, " C ", "SES", "SNS", 'C', chiselledSandstone, 'S', smoothSandstone,
                'E', Items.ENDER_PEARL, 'N', "ingotNaquadahAlloy");
        }

        if (config.getBoolean("recipes", "sgRingBlock", true)) {
            newRecipe("sgringblock", sgRingBlock, 1, "CCC", "NNN", "SSS", 'S', smoothSandstone, 'N', "ingotNaquadahAlloy", 'C', chiselledSandstone);
        }

        if (config.getBoolean("recipes", "sgChevronBlock", true)) {
            newRecipe("sgcheveronblock", sgChevronBlock, "CgC", "NpN", "SrS", 'S', smoothSandstone, 'N', "ingotNaquadahAlloy", 'C', chiselledSandstone, 'g', Items.GLOWSTONE_DUST, 'r', Items.REDSTONE, 'p', Items.ENDER_PEARL);
        }

        if (config.getBoolean("recipes", "sgBaseBlock", true)) {
            newRecipe("sgbaseblock", sgBaseBlock, 1, "CrC", "NeN", "ScS", 'S', smoothSandstone, 'N', "ingotNaquadahAlloy", 'C', chiselledSandstone, 'r', Items.REDSTONE, 'e', Items.ENDER_EYE, 'c', sgCoreCrystal);
        }

        ItemStack sgRingBlockStack = new ItemStack(sgRingBlock, 1, 0);
        ItemStack supergateRingStack = new ItemStack(supergateRingBlock, 1, 0);

        if (config.getBoolean("recipes", "supergateRingBlock", true)) {
            newRecipe("supergateringblock", supergateRingBlock, 4, "RNR", "NBN", "RNR", 'R', sgRingBlockStack, 'N', "ingotNaquadahAlloy", 'B', naquadahBlock);
        }

        if (config.getBoolean("recipes", "supergateChevronBlock", true)) {
            newRecipe("supergatechevronblock", new ItemStack(supergateRingBlock, 1, 1), "S", "C", 'S', supergateRingStack, 'C', sgChevronBlock);
        }

        if (config.getBoolean("recipes", "supergateBaseBlock", true)) {
            newRecipe("supergatebaseblock", supergateBaseBlock, 1, "SeS", "NbN", "SnS", 'S', supergateRingStack, 'N', "ingotNaquadahAlloy", 'e', Items.NETHER_STAR, 'b', sgBaseBlock, 'n', naquadahBlock);
        }

        if (config.getBoolean("recipes", "sgControllerBlock", true)) {
            newRecipe("sgcontrollerblock", sgControllerBlock, 1, "bbb", "OpO", "OcO", 'b', Blocks.STONE_BUTTON, 'O', Blocks.OBSIDIAN, 'p', Items.ENDER_PEARL, 'c', sgControllerCrystal);
        }

        if (config.getBoolean("recipes", "sgChevronUpgradeItem", true)) {
            newRecipe("sgchevronupgrade", sgChevronUpgrade, 1, "g g", "pNp", "r r", 'N', "ingotNaquadahAlloy", 'g', Items.GLOWSTONE_DUST, 'r', Items.REDSTONE, 'p', Items.ENDER_PEARL);
        }

        if (config.getBoolean("recipes", "sgIrisBladeItem", true)) {
            newRecipe("sgirisblade", sgIrisBlade, 1, " ii", "ic ", "i  ", 'i', Items.IRON_INGOT, 'c', new ItemStack(Items.COAL, 1, 1));
        }

        if (config.getBoolean("recipes", "sgIrisUpgradeItem", true)) {
            newRecipe("sgirisupgrade", sgIrisUpgrade, 1, "bbb", "brb", "bbb", 'b', sgIrisBlade, 'r', Items.REDSTONE);
        }

        if (config.getBoolean("recipes", "sgShieldUpgradeItem", true)) {
            newRecipe("sgshieldupgrade", sgShieldUpgrade, 1, "ili", "rcr", "ili", 'i', naquadahIngot, 'l', blueDye, 'r', Items.REDSTONE, 'c', sgCoreCrystal);
        }

        if (config.getBoolean("recipes", "sgCoreCrystalItem", false)) {
            newRecipe("sgcorecrystal", sgCoreCrystal, 1, "bbr", "rdb", "brb", 'b', blueDye, 'r', Items.REDSTONE, 'd', Items.DIAMOND);
        }

        if (config.getBoolean("recipes", "sgControllerCrystalItem", false)) {
            newRecipe("sgcontrollercrystal", sgControllerCrystal, 1, "roo", "odr", "oor", 'o', orangeDye, 'r', Items.REDSTONE, 'd', Items.DIAMOND);
        }
        if (config.getBoolean("recipes", "sgPowerUnit", true)) {
            newRecipe("sgPowerUnit", sgPowerUnit, 1, "rgr", "zIz", "InI", 'n', Blocks.EMERALD_BLOCK, 'z', sgCoreCrystal, 'r', Items.EMERALD, 'g', Blocks.GLASS_PANE, 'I', Blocks.IRON_BLOCK);
        }

        if(config.getBoolean("recipes","pegasus_upgrade",true)) {
            if(config.getBoolean("recipes","pegasus_upgrade_require_crystal", true)) {
                //Core Crystal crafting
                newRecipe("pegasus_upgrade_0", pegasus_upgrade, 1, "glg", "rnr", "glg", 'g', Items.GLOWSTONE_DUST, 'l', Blocks.LAPIS_BLOCK, 'r', Items.REDSTONE, 'n', sgCoreCrystal);
                newRecipe("pegasus_upgrade_1", pegasus_upgrade, 1, "grg", "lnl", "grg", 'g', Items.GLOWSTONE_DUST, 'l', Blocks.LAPIS_BLOCK, 'r', Items.REDSTONE, 'n', sgCoreCrystal);
                //Recraft upgrade
                newRecipe("pegasus_upgrade_2", pegasus_upgrade, 2, "glg", "rnr", "glg", 'g', Items.GLOWSTONE_DUST, 'l', Blocks.LAPIS_BLOCK, 'r', Items.REDSTONE, 'n', pegasus_upgrade);
                newRecipe("pegasus_upgrade_3", pegasus_upgrade, 2, "grg", "lnl", "grg", 'g', Items.GLOWSTONE_DUST, 'l', Blocks.LAPIS_BLOCK, 'r', Items.REDSTONE, 'n', pegasus_upgrade);
            } else {
                //Crafting via naquadah block
                newRecipe("pegasus_upgrade_0", pegasus_upgrade, 1, "glg", "rnr", "glg", 'g', Items.GLOWSTONE_DUST, 'l', Blocks.LAPIS_BLOCK, 'r', Items.REDSTONE, 'n', naquadahBlock);
                newRecipe("pegasus_upgrade_1", pegasus_upgrade, 1, "grg", "lnl", "grg", 'g', Items.GLOWSTONE_DUST, 'l', Blocks.LAPIS_BLOCK, 'r', Items.REDSTONE, 'n', naquadahBlock);
            }
        }

        // The Universe gate has all nine chevrons, so its upgrade takes two chevron upgrades
        if (config.getBoolean("recipes", "universe_upgrade", true)) {
            newRecipe("universe_upgrade", universe_upgrade, 1, "ede", "cnc", "ede", 'e', Items.ENDER_PEARL, 'd', Items.DIAMOND, 'c', sgChevronUpgrade, 'n', sgCoreCrystal);
        }

        if (isModLoaded("malisiscore")) {
            if (config.getBoolean("recipes", "pdd", true)) {
                newRecipe("pdd", pdd, 1, "rcr", "nCn", "xbz", 'n', naquadah, 'z', sgCoreCrystal, 'x', sgControllerCrystal, 'r', Items.REDSTONE, 'b', Items.WRITABLE_BOOK, 'C', Items.CLOCK, 'c', Items.COMPASS);
            }
            if (config.getBoolean("recipes", "edd", true)) {
                newRecipe("edd", edd, 1, "rcr", "nMn", "xbz", 'n', naquadah, 'z', sgCoreCrystal, 'x', sgControllerCrystal, 'r', Items.REDSTONE, 'b', Items.WRITABLE_BOOK, 'M', Items.MAP, 'c', Items.COMPASS);
            }
            if (config.getBoolean("recipes", "shieldConsole", true)) {
                newRecipe("shield_console", shieldConsole, 1, "IBI", "RCR", "III", 'I', Items.IRON_INGOT, 'B', Blocks.STONE_BUTTON, 'R', Items.REDSTONE, 'C', sgControllerCrystal);
            }
            if (config.getBoolean("recipes", "cityShieldGenerator", true)) {
                newRecipe("city_shield_generator", cityShieldGenerator, 1, "NSN", "CXC", "NSN", 'N', naquadahBlock, 'S', sgShieldUpgrade, 'C', sgCoreCrystal, 'X', Items.NETHER_STAR);
            }
            // The Mark I: naquadah alloy round a core crystal, in an iron case; the Mark II: a Mark I with a diamond and a
            // naquadah block
            if (config.getBoolean("recipes", "naquadahGeneratorMk1", true)) {
                newRecipe("naquadah_generator_mk1", naquadahGeneratorMk1, 1, "INI", "NCN", "INI", 'I', Items.IRON_INGOT, 'N', "ingotNaquadahAlloy", 'C', sgCoreCrystal);
            }
            if (config.getBoolean("recipes", "naquadahGeneratorMk2", true)) {
                newRecipe("naquadah_generator_mk2", naquadahGeneratorMk2, 1, "NDN", "NMN", "NBN", 'N', "ingotNaquadahAlloy", 'D', Items.DIAMOND, 'M', naquadahGeneratorMk1, 'B', naquadahBlock);
            }
            if (config.getBoolean("recipes", "gdo", true)) {
                newRecipe("gdo", gdo, 1, "rCr", "xLz", "nbn", 'n', naquadah, 'z', sgCoreCrystal, 'x', sgControllerCrystal, 'r', Items.REDSTONE, 'b', Items.WRITABLE_BOOK, 'C', Items.CLOCK, 'L', Blocks.LEVER);
            }
        }

        if (config.getBoolean("recipes", "zpmConsole", true)) {
            newRecipe("zpmConsole", zpm_console, 1, "rgr", "xIz", "InI", 'n', Blocks.GOLD_BLOCK, 'z', sgCoreCrystal, 'x', sgControllerCrystal, 'r', Items.EMERALD, 'g', Blocks.GLASS_PANE, 'I', Blocks.IRON_BLOCK);
        }

        if (!isModLoaded("ic2")) {
            addGenericCapacitorRecipe();
            addZpmBlockRecipes();
        }
    }

    // Without IC2 the ZPM interface cart and hub keep IC2's recipe shapes (IC2Integration), the SG Power Unit taking the
    // IC2 power unit's place: an iron block for the advanced casing, redstone for the cable, a naquadah block for the
    // machine block and a controller crystal for the circuit
    protected void addZpmBlockRecipes() {
        if (config.getBoolean("recipes", "zpmInterfaceCartBlock", true)) {
            newRecipe("zpm_Interface_Cart", zpm_interface_cart, 1, "eAw", "wBw", "wMe", 'B', sgPowerUnit, 'A', Blocks.IRON_BLOCK, 'w', Items.REDSTONE, 'M', naquadahBlock, 'e', sgControllerCrystal);
        }
        if (config.getBoolean("recipes", "zpmHubBlock", true)) {
            newRecipe("zpm_Hub", zpm_hub, 1, "eAw", "wBw", "BMB", 'B', zpm_console, 'A', Blocks.IRON_BLOCK, 'w', Items.REDSTONE, 'M', naquadahBlock, 'e', sgControllerCrystal);
        }
    }

    protected void addGenericCapacitorRecipe() {
        if (config.getBoolean("recipes", "genericCapacitorItem", true)) {
            newRecipe("ic2capacitor", ic2Capacitor, 1, "iii", "ppp", "iii", 'i', "ingotIron", 'p', "paper");
        }
    }

    @Override
    protected void registerContainers() {
        addContainer(SGGui.SGBase, SGBaseContainer.class);
        addContainer(SGGui.DHDFuel, DHDFuelContainer.class);
        addContainer(SGGui.RingFuel, RingFuelContainer.class);
        addContainer(SGGui.PowerUnit, PowerContainer.class);
        addContainer(SGGui.ZPMInterfaceCart, ZpmInterfaceCartContainer.class);
        addContainer(SGGui.ZPMConsole, ZpmConsoleContainer.class);
        addContainer(SGGui.ZPMHub, ZpmHubContainer.class);
    }

    @Override
    protected void registerWorldGenerators() {
        if (config.getBoolean("options", "enableNaquadahOre", true)) {
            System.out.printf("SGCraft: Registering NaquadahOreWorldGen\n");
            naquadahOreGenerator = new NaquadahOreWorldGen();
            GameRegistry.registerWorldGenerator(naquadahOreGenerator, 0);
        }
        MapGenStructureIO.registerStructureComponent(FeatureUnderDesertPyramid.class, "SGCraft:FeatureUnderDesertPyramid");
        MapGenStructureIO.registerStructureComponent(FeatureSwampHut.class, "SGCraft:FeatureSwampHut");
        MapGenStructureIO.registerStructureComponent(FeatureIgloo.class, "SGCraft:FeatureIgloo");
        MapGenStructureIO.registerStructureComponent(FeatureJungleTemple.class, "SGCraft:FeatureJungleTemple");

		VillagerRegistry VILLAGER_REGISTRY = VillagerRegistry.instance();
        VILLAGER_REGISTRY.registerVillageCreationHandler(new FeatureVillageStargate.VillageManager());
        MapGenStructureIO.registerStructureComponent(FeatureVillageStargate.class, "SGCraft:FeatureVillageStargate");

	    MapGenStructureIO.registerStructureComponent(FeatureNetherFortress.class, "SGCraft:FeatureNetherFortress");
        MapGenStructureIO.registerStructureComponent(FeatureOceanMonument.class, "SGCraft:FeatureOceanMonument");
    }

    @Override //[VILL]
    protected void registerVillagers() {
        tokraProfession = new VillagerProfession("sgcraft:tokra", "sgcraft:textures/skins/tokra.png","sgcraft:textures/skins/tokra.png");
        // Update: Needs new skin for Zombie mode?
        VillagerCareer tokraCareer = new VillagerCareer(tokraProfession, "sgcraft:tokra");
        tokraCareer.addTrade(1, new SGTradeHandler());
        ForgeRegistries.VILLAGER_PROFESSIONS.register(tokraProfession);
    }

    @Override
    protected void registerEntities() {
        addEntity(EntityStargateIris.class, "stargate_iris", SGEntity.Iris, 1000000, false);
    }

    @Override
    protected void registerSounds() {
        SGBaseTE.registerSounds(this);
        if (isModLoaded("malisiscore")) {
            CityShieldTE.registerSounds(this); // (it needs MalisisCore, so only with it)
        }
        NaquadahGenerators.registerSounds(this);
        AsgardObelisks.beamSound = newSound("asgard_beam");
        RingTransports.registerSounds(this);
        Zat.fireSound = newSound("zat_fire");
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load e) {
        Chunk chunk = e.getChunk();
        SGChunkData.onChunkLoad(e);
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save e) {
        Chunk chunk = e.getChunk();
        SGChunkData.onChunkSave(e);
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent e) {
        FeatureGeneration.onInitMapGen(e);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        switch (e.phase) {
            case START: {
                for (BaseSubsystem om : subsystems)
                    if (om instanceof IIntegration)
                        ((IIntegration)om).onServerTick();
                break;
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.world instanceof WorldServer) {
            SGChunkData.onWorldTick((WorldServer) e.world);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload e) {
        Chunk chunk = e.getChunk();
        if (!chunk.getWorld().isRemote) {
            for (Object obj : chunk.getTileEntityMap().values()) {
                if (obj instanceof SGBaseTE) {
                    SGBaseTE te = (SGBaseTE)obj;
                    te.disconnect();
                }
            }
            SGChunkData.onChunkUnload(chunk);
        }
    }

    // Each world's name, kept to name it by while it isn't loaded (see WorldNames)
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        WorldNames.record(e.getWorld());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (!e.getWorld().isRemote) {
            SGChunkData.onWorldUnload(e.getWorld());
        }
    }

    // When a player starts seeing a chunk, tell them their own access to any gates in it: clients aren't sent the
    // gates' access lists, but need to know which screens and slots to show
    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch e) {
        EntityPlayerMP player = e.getPlayer();
        Chunk chunk = e.getChunkInstance() != null ? e.getChunkInstance() : player.world.getChunk(e.getChunk().x, e.getChunk().z);
        for (TileEntity te : chunk.getTileEntityMap().values()) {
            if (te instanceof SGBaseTE) {
                ((SGBaseTE) te).sendPlayerAccessTo(player);
            }
        }
    }

    // A supergate's ring is drawn far wider than its blocks, so most clicks on it would miss them,
    // landing on whatever is behind or on nothing. An empty handed click on the drawn ring of any
    // gate in front of what it would otherwise hit acts on the gate.
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        EntityPlayer player = e.getEntityPlayer();
        if (e.getHand() != EnumHand.MAIN_HAND || !player.getHeldItemMainhand().isEmpty()) {
            return;
        }
        double[] ringDistance = new double[1];
        SGBaseTE gate = GateUtil.gateLookedAt(e.getWorld(), player, GateUtil.reach(player), ringDistance);
        if (gate == null || e.getHitVec() == null || ringDistance[0] >= player.getPositionEyes(1).distanceTo(e.getHitVec())) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(EnumActionResult.SUCCESS);
        if (!e.getWorld().isRemote) {
            GateUtil.activateGate(gate, player);
        }
    }

    // Client only: an empty handed click on nothing, which the server doesn't hear of
    @SubscribeEvent
    public void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty e) {
        EntityPlayer player = e.getEntityPlayer();
        if (e.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        SGBaseTE gate = GateUtil.gateLookedAt(e.getWorld(), player, GateUtil.reach(player));
        if (gate != null) {
            SGChannel.sendGateActivationToServer(gate);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelRegistry(ModelRegistryEvent event) {
        // The hidden iris light has no model; mapping it to none keeps Minecraft from looking for one
        ModelLoader.setCustomStateMapper(eventHorizonLightBlock, block -> Collections.emptyMap());

        // Register Complex Block Models
        // Note: Complex Item Models register within their creation class because their registration order isn't important.
        if (SGCraft.zpm_interface_cart != null) {
            registerModel(Item.getItemFromBlock(SGCraft.zpm_interface_cart));
        }
        if (SGCraft.zpm_hub != null) {
            registerModel(Item.getItemFromBlock(SGCraft.zpm_hub));
        }
        if (SGCraft.zpm_console != null) {
            registerModel(Item.getItemFromBlock(SGCraft.zpm_console));
        }
    }

    @SideOnly(Side.CLIENT)
    private void registerModel(Item item) {
        this.registerInventoryModel(item, requireNonNull(item.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    private void registerInventoryModel(Item item, ResourceLocation blockName) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(blockName, "inventory"));
    }

    private void setOptions() {
        // Block Harvests
        canHarvestDHD = config.getBoolean("block-harvest", "dhdBlock", canHarvestDHD);
        canHarvestSGBaseBlock  = config.getBoolean("block-harvest", "sgBaseBlock", canHarvestSGBaseBlock);
        canHarvestSGRingBlock  = config.getBoolean("block-harvest", "sgRingBlock", canHarvestSGRingBlock);

        // IC2
        Ic2SafeInput  = config.getInteger("ic2", "safeInputRate", Ic2SafeInput);
        Ic2MaxEnergyBuffer = config.getInteger("ic2", "energyBufferSize", Ic2MaxEnergyBuffer);
        Ic2euPerSGEnergyUnit = config.getDouble("ic2", "euPerSGEnergyUnit", Ic2euPerSGEnergyUnit);
        Ic2PowerTETier = config.getInteger("ic2", "PowerTETier", Ic2PowerTETier);
        Ic2FEPerEU = config.getDouble("ic2", "fePerEU", Ic2FEPerEU);
        forceIC2CfgUpdate = config.getBoolean("ic2", "force-update", forceIC2CfgUpdate);

        // FP Power Block (Forge Power) i.e.: RF/FE
        FPMaxEnergyBuffer = config.getInteger("fp-power", "energyBufferSize", FPMaxEnergyBuffer);
        FPPerSGEnergyUnit = config.getDouble("fp-power", "fpPerSGEnergyUnit", FPPerSGEnergyUnit);
        forceFPCfgUpdate = config.getBoolean("fp-power", "force-update", forceFPCfgUpdate);

        // ZPM Console
        ZPMEnergyPerSGEnergyUnit = config.getDouble("zpm-power", "zpmEnergyPerSGEnergyUnit", FPPerSGEnergyUnit);
        forceZPMCfgUpdate = config.getBoolean("zpm-power", "force-update", forceZPMCfgUpdate);

        // Range lookups for various things...
        zpmSearchRange = config.getInteger("stargate", "zpmSearchRange", zpmSearchRange);
        eventHorizonLightLevel = Math.max(0, Math.min(15, config.getInteger("stargate", "eventHorizonLightLevel", eventHorizonLightLevel)));

        // Loot
        LootTableConfig.load(config, assetKey);
        debugLoot = config.getBoolean("loot", "debugLoot", debugLoot); // log 100 sample rolls of each loot table at server start
        toolSearchRange = config.getInteger("stargate", "toolSearchRange", toolSearchRange);

        // World Update / Fixes
        forceSGBaseTEUpdate = config.getBoolean("stargate", "force-default-configs", forceSGBaseTEUpdate);
        forceDHDCfgUpdate = config.getBoolean("dhd", "force-update", forceDHDCfgUpdate);

        // Access System Resets
        forceGateAccessSystemReset = config.getBoolean("gate-access", "force-reset-on-load", forceGateAccessSystemReset);
        forcePlayerAccessSystemReset = config.getBoolean("player-access", "force-reset-on-load", forcePlayerAccessSystemReset);

        // Transient Damage - Unbreakable Blocks
        wormholeCanDestroyUnbreakableBlocks = config.getBoolean("stargate", "wormholeCanDestroyUnbreakableBlocks", wormholeCanDestroyUnbreakableBlocks);
    }

    public static boolean hasPermission(EntityPlayer player, String permission) {
        if (isModLoaded("spongeapi")) {
            return PermissionsUtil.spongeHasPermission(player, permission);
        }

        return true; // Fallback
    }

    public static boolean hasPermissionSystem() {
        if (isModLoaded("spongeapi")) {
            return true;
        }

        return false; // Fallback
    }
}
