//------------------------------------------------------------------------------------------------
//
//   SG Craft - Main Class
//
//------------------------------------------------------------------------------------------------

package gcewing.sg;

import gcewing.sg.block.DHDBlock;
import gcewing.sg.block.NaquadahBlock;
import gcewing.sg.block.NaquadahOreBlock;
import gcewing.sg.block.SGBaseBlock;
import gcewing.sg.block.SGPowerBlock;
import gcewing.sg.block.SGRingBlock;
import gcewing.sg.client.gui.SGGui;
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
import gcewing.sg.generator.GeneratorAddressRegistry;
import gcewing.sg.interfaces.IIntegration;
import gcewing.sg.interfaces.SoundSource;
import gcewing.sg.item.*;
import gcewing.sg.network.GuiNetworkHandler;
import gcewing.sg.network.SGChannel;
import gcewing.sg.generator.NaquadahOreWorldGen;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GeneralAddressRegistry;
import gcewing.sg.util.Info;
import gcewing.sg.util.PermissionsUtil;
import gcewing.sg.util.SGChunkData;
import gcewing.sg.util.Sound;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ninja.leaping.configurate.ConfigurationNode;

import static java.util.Objects.requireNonNull;
import static net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import static net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;

import java.io.IOException;
import java.nio.file.Paths;

@Mod(modid = Info.modID, name = Info.modName, version = Info.versionNumber,
    acceptableRemoteVersions = Info.versionBounds, dependencies = "after:opencomputers;after:ic2;after:computercraft;after:malisiscore")

public class SGCraft extends BaseMod<SGCraftClient> {

    public static final Material machineMaterial = new Material(MapColor.IRON);

    public static SGCraft mod;

    public static SGChannel channel;
    public static BaseTEChunkManager chunkManager;

    public static SGBaseBlock sgBaseBlock;
    public static SGRingBlock sgRingBlock;
    public static DHDBlock sgControllerBlock;
    public static Block naquadahBlock, naquadahOre;
    public static SGPowerBlock sgPowerUnit;

    public static Item naquadah, naquadahIngot, sgCoreCrystal, sgControllerCrystal, sgChevronUpgrade, sgIrisUpgrade, sgIrisBlade, pegasus_upgrade;
    public static Item tollan_phase_shift_device;
    public static Item admin_upgrade;

    public static Block ic2PowerUnit;
    public static Item ic2Capacitor;

    public static boolean addOresToExistingWorlds;
    public static NaquadahOreWorldGen naquadahOreGenerator;

    public static BaseSubsystem ic2Integration; //[IC2]
    public static IIntegration ccIntegration; //[CC]
    public static OCIntegration ocIntegration; //[OC]

    public static Block zpm_interface_cart, zpm_console, zpm_hub;
    public static Item zpm, gdo, pdd, configurator;

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
        };
        FMLCommonHandler.instance().bus().register(this);
        ic2Integration = integrateWithMod("ic2", "gcewing.sg.features.ic2.IC2Integration"); //[IC2]
        ccIntegration = (IIntegration) integrateWithMod("computercraft", "gcewing.sg.features.cc.CCIntegration"); //[CC]
        ocIntegration = (OCIntegration)integrateWithMod("opencomputers", "gcewing.sg.features.oc.OCIntegration"); //[OC]

        if (isModLoaded("ic2")) {
            GameRegistry.registerTileEntity(ZpmInterfaceCartTE.class, new ResourceLocation(this.modID + ":" + "tile_zpminterfacecart"));
            GameRegistry.registerTileEntity(ZpmHubTE.class, new ResourceLocation(this.modID + ":" + "tile_zpmmodulehub"));
        }

        GameRegistry.registerTileEntity(ZpmConsoleTE.class, new ResourceLocation(this.modID + ":" + "tile_zpmconsole"));

        super.preInit(e);
    }

    @Mod.EventHandler
    @Override
    public void init(FMLInitializationEvent e) {
        super.init(e);
        System.out.printf("SGCraft.init\n");
        configure();
        channel = new SGChannel(Info.modID);
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
        FeatureGeneration.configure(config);

        // Server-Side Options
        addOresToExistingWorlds = config.getBoolean("options", "addOresToExistingWorlds", false);

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
        sgRingBlock = newBlock("stargateRing", SGRingBlock.class, SGRingItem.class);
        sgBaseBlock = newBlock("stargateBase", SGBaseBlock.class);
        sgControllerBlock = newBlock("stargateController", DHDBlock.class);
        naquadahBlock = newBlock("naquadahBlock", NaquadahBlock.class);
        naquadahOre = newBlock("naquadahOre", NaquadahOreBlock.class);
        sgPowerUnit = newBlock("sgPowerUnit", SGPowerBlock.class);

        if (isModLoaded("ic2")) {
            zpm_interface_cart = newBlock("zpm_interface_cart", ZpmInterfaceCart.class);
            zpm_hub = newBlock("zpm_hub", ZpmHub.class);
        }
        zpm_console = newBlock("zpm_console", ZpmConsole.class);

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
        sgIrisBlade = newItem("sgIrisBlade");
        if (isModLoaded("ic2")) {
            ic2Capacitor = newItem("ic2Capacitor");
        }

        zpm = addItem(new ZPMItem(), "zpm");
        tollan_phase_shift_device = newItem("tollan_phase_shift_device");

        pegasus_upgrade = addItem(new SGPegasusUpgradeItem(), "pegasus_upgrade");

        admin_upgrade = addItem(new SGAdminUpgradeItem(), "admin_upgrade");

        if (isModLoaded("malisiscore")) {
            new GuiNetworkHandler(Info.modID+"-GUI");

            gdo = addItem(new GdoItem(), "gdo");
            new GdoNetworkHandler(Info.modID+"-gdo");

            pdd = addItem(new PddItem(), "pdd");
            new PddNetworkHandler(Info.modID+"-pdd");

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
        return item == sgChevronUpgrade || item == sgIrisUpgrade;
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

        if (config.getBoolean("recipes", "sgRingBlock", true)) {
            newRecipe("sgringblock", sgRingBlock, 1, "CCC", "NNN", "SSS", 'S', smoothSandstone, 'N', "ingotNaquadahAlloy", 'C', chiselledSandstone);
        }

        if (config.getBoolean("recipes", "sgChevronBlock", true)) {
            newRecipe("sgcheveronblock", sgChevronBlock, "CgC", "NpN", "SrS", 'S', smoothSandstone, 'N', "ingotNaquadahAlloy", 'C', chiselledSandstone, 'g', Items.GLOWSTONE_DUST, 'r', Items.REDSTONE, 'p', Items.ENDER_PEARL);
        }

        if (config.getBoolean("recipes", "sgBaseBlock", true)) {
            newRecipe("sgbaseblock", sgBaseBlock, 1, "CrC", "NeN", "ScS", 'S', smoothSandstone, 'N', "ingotNaquadahAlloy", 'C', chiselledSandstone, 'r', Items.REDSTONE, 'e', Items.ENDER_EYE, 'c', sgCoreCrystal);
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

        if (isModLoaded("malisiscore")) {
            if (config.getBoolean("recipes", "pdd", true)) {
                newRecipe("pdd", pdd, 1, "rcr", "nCn", "xbz", 'n', naquadah, 'z', sgCoreCrystal, 'x', sgControllerCrystal, 'r', Items.REDSTONE, 'b', Items.WRITABLE_BOOK, 'C', Items.CLOCK, 'c', Items.COMPASS);
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
    public void onChunkUnload(ChunkEvent.Unload e) {
        Chunk chunk = e.getChunk();
        if (!chunk.getWorld().isRemote) {
            for (Object obj : chunk.getTileEntityMap().values()) {
                if (obj instanceof SGBaseTE) {
                    SGBaseTE te = (SGBaseTE)obj;
                    te.disconnect();
                }
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelRegistry(ModelRegistryEvent event) {
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
        forceIC2CfgUpdate = config.getBoolean("ic2", "force-update", forceIC2CfgUpdate);

        // FP Power Block (Forge Power) i.e.: RF/FE
        FPMaxEnergyBuffer = config.getInteger("fp-power", "energyBufferSize", FPMaxEnergyBuffer);
        FPPerSGEnergyUnit = config.getDouble("fp-power", "fpPerSGEnergyUnit", FPPerSGEnergyUnit);
        forceFPCfgUpdate = config.getBoolean("fp-power", "force-update", forceFPCfgUpdate);

        // ZPM Console
        ZPMEnergyPerSGEnergyUnit = config.getDouble("zpm-power", "zpmEnergyPerSGEnergyUnit", FPPerSGEnergyUnit);
        forceZPMCfgUpdate = config.getBoolean("zpm-power", "force-update", forceZPMCfgUpdate);

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
