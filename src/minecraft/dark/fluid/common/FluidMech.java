package dark.fluid.common;

import hydraulic.api.ColorCode;
import hydraulic.api.FluidRestrictionHandler;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import org.modstats.ModstatInfo;
import org.modstats.Modstats;

import universalelectricity.prefab.TranslationHelper;
import universalelectricity.prefab.network.PacketManager;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Metadata;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import dark.fluid.common.fluids.BlockWasteLiquid;
import dark.fluid.common.item.ItemParts;
import dark.fluid.common.item.ItemTools;
import dark.fluid.common.item.ItemParts.Parts;
import dark.fluid.common.machines.BlockReleaseValve;
import dark.fluid.common.machines.BlockSink;
import dark.fluid.common.machines.BlockTank;
import dark.fluid.common.machines.ItemBlockLiquidMachine;
import dark.fluid.common.machines.ItemBlockReleaseValve;
import dark.fluid.common.machines.ItemBlockTank;
import dark.fluid.common.machines.TileEntityReleaseValve;
import dark.fluid.common.machines.TileEntitySink;
import dark.fluid.common.machines.TileEntityTank;
import dark.fluid.common.pipes.BlockPipe;
import dark.fluid.common.pipes.ItemBlockPipe;
import dark.fluid.common.pipes.TileEntityGenericPipe;
import dark.fluid.common.pipes.TileEntityPipe;
import dark.fluid.common.pipes.addon.TileEntityPipeWindow;
import dark.fluid.common.pump.BlockConstructionPump;
import dark.fluid.common.pump.BlockDrain;
import dark.fluid.common.pump.BlockPumpMachine;
import dark.fluid.common.pump.TileEntityConstructionPump;
import dark.fluid.common.pump.TileEntityDrain;
import dark.fluid.common.pump.TileEntityStarterPump;
import dark.library.DarkMain;
import dark.mech.common.machines.BlockGenerator;
import dark.mech.common.machines.BlockRod;
import dark.mech.common.machines.TileEntityGenerator;
import dark.mech.common.machines.TileEntityRod;


@ModstatInfo(prefix = "fluidmech")
@Mod(modid = FluidMech.MOD_ID, name = FluidMech.MOD_NAME, version = FluidMech.VERSION, dependencies = "after:BasicComponents", useMetadata = true)
@NetworkMod(channels = { FluidMech.CHANNEL }, clientSideRequired = true, serverSideRequired = false, packetHandler = PacketManager.class)
public class FluidMech extends DummyModContainer
{

	// @Mod Prerequisites
	public static final String MAJOR_VERSION = "@MAJOR@";
	public static final String MINOR_VERSION = "@MINOR@";
	public static final String REVIS_VERSION = "@REVIS@";
	public static final String BUILD_VERSION = "@BUILD@";

	// @Mod
	public static final String MOD_ID = "Fluid_Mechanics";
	public static final String MOD_NAME = "Fluid Mechanics";
	public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION + "." + REVIS_VERSION + "." + BUILD_VERSION;

	// @NetworkMod
	public static final String CHANNEL = "FluidMech";

	@Metadata(FluidMech.MOD_ID)
	public static ModMetadata meta;

	/* RESOURCE FILE PATHS */
	public static final String RESOURCE_PATH = "/mods/fluidmech/";
	public static final String TEXTURE_DIRECTORY = RESOURCE_PATH + "textures/";
	public static final String GUI_DIRECTORY = TEXTURE_DIRECTORY + "gui/";
	public static final String BLOCK_TEXTURE_DIRECTORY = TEXTURE_DIRECTORY + "blocks/";
	public static final String ITEM_TEXTURE_DIRECTORY = TEXTURE_DIRECTORY + "items/";
	public static final String MODEL_TEXTURE_DIRECTORY = TEXTURE_DIRECTORY + "models/";
	public static final String TEXTURE_NAME_PREFIX = "fluidmech:";
	public static final String LANGUAGE_PATH = RESOURCE_PATH + "languages/";

	/* SUPPORTED LANGS */
	private static final String[] LANGUAGES_SUPPORTED = new String[] { "en_US" };

	/* CONFIG FILE */
	public static final Configuration CONFIGURATION = new Configuration(new File(Loader.instance().getConfigDir() + "/UniversalElectricity/", FluidMech.MOD_NAME + ".cfg"));

	/* START IDS */
	public final static int BLOCK_ID_PREFIX = 3100;
	public final static int ITEM_ID_PREFIX = 13200;

	/* BLOCKS */
	public static Block blockPipe;
	public static Block blockGenPipe;
	public static Block blockTank;
	public static Block blockMachine;
	public static Block blockRod;
	public static Block blockGenerator;
	public static Block blockReleaseValve;
	public static Block blockSink;
	public static Block blockDrain;
	public static Block blockConPump;
	public static Block blockWasteLiquid;

	/* ITEMS */
	public static Item itemParts;
	public static Item itemGauge;

	@SidedProxy(clientSide = "dark.fluid.client.ClientProxy", serverSide = "dark.fluid.common.CommonProxy")
	public static CommonProxy proxy;

	@Instance(FluidMech.MOD_NAME)
	public static FluidMech instance;

	/* LOGGER - EXTENDS FORGE'S LOG SYSTEM */
	public static Logger FMLog = Logger.getLogger(FluidMech.MOD_NAME);

	static
	{
		/* EVENT BUS (done here to ensure all fluid events are caught) */
		MinecraftForge.EVENT_BUS.register(new FluidRestrictionHandler());
	}

	@PreInit
	public void preInit(FMLPreInitializationEvent event)
	{
		/* LOGGER SETUP */
		FMLog.setParent(FMLLog.getLogger());
		FMLog.info("Initializing...");

		instance = this;

		/* UPDATE NOTIFIER */
		Modstats.instance().getReporter().registerMod(this);

		/* CONFIGS */
		CONFIGURATION.load();

		/* BLOCK DECLARATION -- CONFIG LOADER */
		blockGenPipe = new BlockPipe(this.CONFIGURATION.getBlock("Pipes", BLOCK_ID_PREFIX).getInt());
		blockMachine = new BlockPumpMachine(this.CONFIGURATION.getBlock("Machines", BLOCK_ID_PREFIX + 1).getInt());
		blockRod = new BlockRod(this.CONFIGURATION.getBlock("Mechanical Rod", BLOCK_ID_PREFIX + 3).getInt());
		blockGenerator = new BlockGenerator((this.CONFIGURATION.getBlock("Generator", BLOCK_ID_PREFIX + 4).getInt()));
		blockReleaseValve = new BlockReleaseValve((this.CONFIGURATION.getBlock("Release Valve", BLOCK_ID_PREFIX + 5).getInt()));
		blockTank = new BlockTank(this.CONFIGURATION.getBlock("Tank", BLOCK_ID_PREFIX + 6).getInt());
		blockWasteLiquid = new BlockWasteLiquid(this.CONFIGURATION.getBlock("WasteLiquid", BLOCK_ID_PREFIX + 7).getInt());
		blockSink = new BlockSink(this.CONFIGURATION.getBlock("Sink", BLOCK_ID_PREFIX + 8).getInt());
		blockDrain = new BlockDrain(this.CONFIGURATION.getBlock("Drain", BLOCK_ID_PREFIX + 9).getInt());
		blockConPump = new BlockConstructionPump(this.CONFIGURATION.getBlock("ConstructionPump", BLOCK_ID_PREFIX + 10).getInt());
		blockPipe = new BlockPipe(this.CONFIGURATION.getBlock("RestrictedPipes", BLOCK_ID_PREFIX + 11).getInt());

		/* ITEM DECLARATION -- COFNGI LOADER */
		itemParts = new ItemParts(this.CONFIGURATION.getItem("Parts", ITEM_ID_PREFIX).getInt());
		itemGauge = new ItemTools(this.CONFIGURATION.getItem("PipeGuage", ITEM_ID_PREFIX + 3).getInt());
		if (CONFIGURATION.hasChanged())
		{
			CONFIGURATION.save();
		}
		/* CONFIG END */

		proxy.preInit();

		/* BLOCK REGISTER CALLS */
		GameRegistry.registerBlock(blockPipe, ItemBlockPipe.class, "lmPipe");
		GameRegistry.registerBlock(blockGenPipe, ItemBlockPipe.class, "lmGenPipe");
		GameRegistry.registerBlock(blockReleaseValve, ItemBlockReleaseValve.class, "eValve");
		GameRegistry.registerBlock(blockRod, "mechRod");
		GameRegistry.registerBlock(blockGenerator, "lmGen");
		GameRegistry.registerBlock(blockMachine, ItemBlockLiquidMachine.class, "lmMachines");
		GameRegistry.registerBlock(blockTank, ItemBlockTank.class, "lmTank");
		GameRegistry.registerBlock(blockSink, "lmSink");
		GameRegistry.registerBlock(blockDrain, "lmDrain");
		GameRegistry.registerBlock(blockConPump, "lmConPump");

	}

	@Init
	public void Init(FMLInitializationEvent event)
	{
		/* MCMOD.INFO FILE BUILDER? */
		meta.modId = FluidMech.MOD_ID;
		meta.name = FluidMech.MOD_NAME;
		meta.description = "Fluid Mechanics is a combination between supporting fluid handling and mechanical energy handling system. " + "Its designed to help other mods move there liquids to using a universal liquid system managed by forge. As a bonus it also " + "comes with suppot to help mods move energy by means of mechanics motion along rods. This mod by itself doesn't offer much more " + "than basic liquid storage, placement, and removel in the world. Its suggest to download other mods that supports the Forge's " + "LiquidDictionary. " + "\n" + "Suported Power systems: Universal Electric ";

		meta.url = "http://www.universalelectricity.com/fluidmechanics";

		meta.logoFile = FluidMech.TEXTURE_DIRECTORY + "FM_Banner.png";
		meta.version = FluidMech.VERSION;
		meta.authorList = Arrays.asList(new String[] { "DarkGuardsman AKA DarkCow" });
		meta.credits = "Please see the website.";
		meta.autogenerated = false;

		/* LOGGER */
		FMLog.info("Loading...");
		proxy.Init();

		/* TILE ENTITY REGISTER CALLS */
		GameRegistry.registerTileEntity(TileEntityPipe.class, "lmPipeTile");
		GameRegistry.registerTileEntity(TileEntityGenericPipe.class, "lmGenPipeTile");
		GameRegistry.registerTileEntity(TileEntityStarterPump.class, "lmPumpTile");
		GameRegistry.registerTileEntity(TileEntityRod.class, "lmRodTile");
		GameRegistry.registerTileEntity(TileEntityReleaseValve.class, "lmeValve");
		GameRegistry.registerTileEntity(TileEntityTank.class, "lmTank");
		GameRegistry.registerTileEntity(TileEntityGenerator.class, "lmGen");
		GameRegistry.registerTileEntity(TileEntitySink.class, "lmSink");
		GameRegistry.registerTileEntity(TileEntityDrain.class, "lmDrain");
		GameRegistry.registerTileEntity(TileEntityConstructionPump.class, "lmConPump");
		GameRegistry.registerTileEntity(TileEntityPipeWindow.class, "lmPipeWindow");

		/* LANG LOADING */
		FMLog.info(" Loaded: " + TranslationHelper.loadLanguages(LANGUAGE_PATH, LANGUAGES_SUPPORTED) + " Languages.");
		

		/* ORE DIRECTORY REGISTER */
		OreDictionary.registerOre("bronzeTube", new ItemStack(itemParts, 1, Parts.Bronze.ordinal()));
		OreDictionary.registerOre("ironTube", new ItemStack(itemParts, 1, Parts.Iron.ordinal()));
		OreDictionary.registerOre("netherTube", new ItemStack(itemParts, 1, Parts.Nether.ordinal()));
		OreDictionary.registerOre("obbyTube", new ItemStack(itemParts, 1, Parts.Obby.ordinal()));
		OreDictionary.registerOre("leatherSeal", new ItemStack(itemParts, 1, Parts.Seal.ordinal()));
		OreDictionary.registerOre("leatherSlimeSeal", new ItemStack(itemParts, 1, Parts.SlimeSeal.ordinal()));
		OreDictionary.registerOre("valvePart", new ItemStack(itemParts, 1, Parts.Valve.ordinal()));
		OreDictionary.registerOre("bronzeTube", new ItemStack(itemParts, 1, Parts.Bronze.ordinal()));
		OreDictionary.registerOre("unfinishedTank", new ItemStack(itemParts, 1, Parts.Tank.ordinal()));

		/* LIQUID DIRECTORY CALL */
		LiquidStack waste = LiquidDictionary.getOrCreateLiquid("Waste", new LiquidStack(FluidMech.blockWasteLiquid, 1));

	}

	@PostInit
	public void PostInit(FMLPostInitializationEvent event)
	{
		/* LOGGER */
		FMLog.info("Finalizing...");
		proxy.postInit();
		DarkMain.forceLoadBCItems(this, FluidMech.CHANNEL);
		/* TAB ITEM SET */
		TabFluidMech.setItemStack(new ItemStack(blockPipe, 1, 4));

		/* /******** RECIPES ************* */

		// generator
		CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(new ItemStack(this.blockGenerator, 1), new Object[] { "@T@", "OVO", "@T@", 'T', new ItemStack(FluidMech.blockRod, 1), '@', "plateSteel", 'O', "basicCircuit", 'V', "motor" }));
		// pipe gauge
		GameRegistry.addRecipe(new ItemStack(this.itemGauge, 1, 0), new Object[] { "TVT", " T ", 'V', new ItemStack(itemParts, 1, Parts.Valve.ordinal()), 'T', new ItemStack(itemParts, 1, Parts.Iron.ordinal()) });
		// iron tube
		GameRegistry.addRecipe(new ItemStack(itemParts, 4, Parts.Iron.ordinal()), new Object[] { "@@@", '@', Item.ingotIron });
		// bronze tube
		CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(new ItemStack(itemParts, 4, Parts.Bronze.ordinal()), new Object[] { "@@@", '@', "ingotBronze" }));
		// obby tube
		GameRegistry.addRecipe(new ItemStack(itemParts, 4, Parts.Obby.ordinal()), new Object[] { "@@@", '@', Block.obsidian });
		// nether tube
		GameRegistry.addRecipe(new ItemStack(itemParts, 4, Parts.Nether.ordinal()), new Object[] { "NNN", 'N', Block.netherrack });
		// seal
		GameRegistry.addRecipe(new ItemStack(itemParts, 4, Parts.Seal.ordinal()), new Object[] { "@@", "@@", '@', Item.leather });
		// slime steal
		GameRegistry.addShapelessRecipe(new ItemStack(itemParts, 1, Parts.SlimeSeal.ordinal()), new Object[] { new ItemStack(itemParts, 1, Parts.Seal.ordinal()), new ItemStack(Item.slimeBall, 1) });
		// part valve
		GameRegistry.addRecipe(new ItemStack(itemParts, 1, Parts.Valve.ordinal()), new Object[] { "T@T", 'T', new ItemStack(itemParts, 1, Parts.Iron.ordinal()), '@', Block.lever });

		// unfinished tank
		GameRegistry.addRecipe(new ItemStack(itemParts, 1, Parts.Tank.ordinal()), new Object[] { " @ ", "@ @", " @ ", '@', Item.ingotIron });
		// mechanical rod
		GameRegistry.addRecipe(new ItemStack(blockRod, 1), new Object[] { "I@I", 'I', Item.ingotIron, '@', new ItemStack(itemParts, 1, Parts.Iron.ordinal()) });

		// Iron Pipe
		GameRegistry.addShapelessRecipe(new ItemStack(blockPipe, 1, 15), new Object[] { new ItemStack(itemParts, 1, Parts.Iron.ordinal()), new ItemStack(itemParts, 1, Parts.Seal.ordinal()) });
		// Lava Tube
		GameRegistry.addRecipe(new ItemStack(blockPipe, 1, ColorCode.RED.ordinal()), new Object[] { "N@N", 'N', new ItemStack(itemParts, 1, Parts.Nether.ordinal()), '@', new ItemStack(itemParts, 1, Parts.Obby.ordinal()) });
		// fuel pipe
		GameRegistry.addShapelessRecipe(new ItemStack(blockPipe, 4, ColorCode.YELLOW.ordinal()), new Object[] { new ItemStack(blockGenPipe, 1, ColorCode.YELLOW.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.YELLOW.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.YELLOW.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.YELLOW.ordinal()), new ItemStack(itemParts, 1, Parts.SlimeSeal.ordinal()) });

		// oil pipe
		GameRegistry.addShapelessRecipe(new ItemStack(blockPipe, 4, ColorCode.BLACK.ordinal()), new Object[] { new ItemStack(blockGenPipe, 1, ColorCode.BLACK.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.BLACK.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.BLACK.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.BLACK.ordinal()), new ItemStack(itemParts, 1, Parts.SlimeSeal.ordinal()) });

		// water pipe
		GameRegistry.addShapelessRecipe(new ItemStack(blockPipe, 4, ColorCode.BLUE.ordinal()), new Object[] { new ItemStack(blockGenPipe, 1, ColorCode.BLUE.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.BLUE.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.BLUE.ordinal()), new ItemStack(blockGenPipe, 1, ColorCode.BLUE.ordinal()), new ItemStack(itemParts, 1, Parts.SlimeSeal.ordinal()) });

		// steam pipes
		GameRegistry.addShapelessRecipe(new ItemStack(blockPipe, 4, ColorCode.ORANGE.ordinal()), new Object[] { new ItemStack(itemParts, 1, Parts.Bronze.ordinal()), new ItemStack(itemParts, 1, Parts.Seal.ordinal()) });
		// generic pipe crafting
		for (int pipeMeta = 0; pipeMeta < 15; pipeMeta++)
		{
			if (pipeMeta != ColorCode.WHITE.ordinal() && pipeMeta != ColorCode.NONE.ordinal())
			{
				GameRegistry.addRecipe(new ItemStack(blockGenPipe, 4, pipeMeta), new Object[] { " P ", "PCP", " P ", 'P', blockGenPipe, 'C', new ItemStack(Item.dyePowder, 1, pipeMeta) });
			}
		}
		GameRegistry.addRecipe(new ItemStack(blockGenPipe, 1, 15), new Object[] { "P", 'P', blockGenPipe });
		GameRegistry.addRecipe(new ItemStack(blockGenPipe, 1, 15), new Object[] { "P", 'P', blockPipe });

		// white pipe crafting -- has to be separate since iron pipe is #15 instead of white
		GameRegistry.addRecipe(new ItemStack(blockGenPipe, 4, ColorCode.WHITE.ordinal()), new Object[] { " P ", "PCP", " P ", 'P', blockGenPipe, 'C', new ItemStack(Item.dyePowder, 1, 15) });

		// lava tank
		GameRegistry.addRecipe(new ItemStack(blockTank, 1, ColorCode.RED.ordinal()), new Object[] { "N@N", "@ @", "N@N", 'T', new ItemStack(itemParts, 1, Parts.Tank.ordinal()), '@', Block.obsidian, 'N', Block.netherrack });
		// water tank
		GameRegistry.addRecipe(new ItemStack(blockTank, 1, ColorCode.BLUE.ordinal()), new Object[] { "@G@", "STS", "@G@", 'T', new ItemStack(itemParts, 1, Parts.Tank.ordinal()), '@', Block.planks, 'G', Block.glass, 'S', new ItemStack(itemParts, 1, Parts.Seal.ordinal()) });
		// milk tank
		GameRegistry.addRecipe(new ItemStack(blockTank, 1, ColorCode.WHITE.ordinal()), new Object[] { "W@W", "WTW", "W@W", 'T', new ItemStack(itemParts, 1, Parts.Tank.ordinal()), '@', Block.stone, 'W', Block.planks });
		// generic Tank
		GameRegistry.addRecipe(new ItemStack(blockTank, 1, ColorCode.NONE.ordinal()), new Object[] { "@@@", "@T@", "@@@", 'T', new ItemStack(itemParts, 1, Parts.Tank.ordinal()), '@', Block.stone });

		// pump
		CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(new ItemStack(blockMachine, 1, 0), new Object[] { "C@C", "BMB", "@X@", '@', "plateSteel", 'X', new ItemStack(blockPipe, 1), 'B', new ItemStack(itemParts, 1, Parts.Valve.ordinal()), 'C', "basicCircuit", 'M', "motor" }));
		// construction pump
		CraftingManager.getInstance().getRecipeList().add(new ShapedOreRecipe(new ItemStack(blockConPump, 1, 0), new Object[] { "@C@", "BMB", "@@@", '@', "plateSteel", 'B', new ItemStack(itemParts, 1, Parts.Valve.ordinal()), 'C', "advancedCircuit", 'M', "motor" }));
		// Drain
		GameRegistry.addRecipe(new ItemStack(blockDrain, 1, 0), new Object[] { "IGI", "SVS", " P ", 'I', Item.ingotIron, 'G', Block.dispenser, 'S', Block.stone, 'P', new ItemStack(blockPipe, 1), 'V', new ItemStack(itemParts, 1, Parts.Valve.ordinal()) });

		// release valve
		GameRegistry.addRecipe(new ItemStack(blockReleaseValve, 1), new Object[] { "RPR", "PVP", "RPR", 'P', new ItemStack(blockPipe, 1), 'V', new ItemStack(itemParts, 1, Parts.Valve.ordinal()), 'R', Item.redstone });
		// sink
		GameRegistry.addRecipe(new ItemStack(blockSink, 1), new Object[] { "I I", "SIS", "SPS", 'P', new ItemStack(blockPipe, 1), 'I', Item.ingotIron, 'S', Block.stone });
		GameRegistry.addRecipe(new ItemStack(blockSink, 1), new Object[] { "I I", "SIS", "SPS", 'P', new ItemStack(blockGenPipe, 1), 'I', Item.ingotIron, 'S', Block.stone });

		FMLog.info("Done Loading");
	}
}