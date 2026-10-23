package pl.geesio.redcircuit;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.geesio.redcircuit.block.ModBlocks;
import pl.geesio.redcircuit.config.RCConfig;
import pl.geesio.redcircuit.item.ModItems;
import pl.geesio.redcircuit.proxy.CommonProxy;
import pl.geesio.redcircuit.worldgen.OreGenerator;

/**
 * RedCircuit dla Minecrafta 1.7.10 (Forge 10.13.4).
 * Etap 1-2: materiały (rudy, sztabki, pyły, zębatki), generowanie rud i piec do stopów.
 */
@Mod(modid = RedCircuit.MODID, name = "RedCircuit", version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]",
        dependencies = "after:ForgeMultipart;after:ComputerCraft;after:OpenComputers;after:Waila;after:NotEnoughItems")
public class RedCircuit {
    public static final String MODID = "redcircuit";
    public static final Logger LOG = LogManager.getLogger("RedCircuit");

    @Mod.Instance(MODID)
    public static RedCircuit instance;

    @SidedProxy(clientSide = "pl.geesio.redcircuit.proxy.ClientProxy", serverSide = "pl.geesio.redcircuit.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        RCConfig.load(event.getSuggestedConfigurationFile());
        ModItems.register();
        ModBlocks.register();
        ModRecipes.registerOreDictionary();
        GameRegistry.registerWorldGenerator(new OreGenerator(), 10);
        pl.geesio.redcircuit.network.RCNetwork.register();
        pl.geesio.redcircuit.wire.BundledIO.register(new pl.geesio.redcircuit.wire.BundledIO.Source() {
            @Override
            public int output(net.minecraft.world.World world, pl.geesio.redcircuit.util.Pos pos, net.minecraftforge.common.util.ForgeDirection side) {
                return -1;
            }

            @Override
            public boolean isDevice(net.minecraft.world.IBlockAccess world, pl.geesio.redcircuit.util.Pos pos, net.minecraft.block.Block block) {
                return block instanceof pl.geesio.redcircuit.machine.BlockAssembler;   // montażownica czyta kabel wiązkowy
            }
        });
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        ModRecipes.register();
        initCompat();
        proxy.init();
    }

    /** Etap 10: integracje z innymi modami (każda tylko wtedy, gdy mod jest zainstalowany). NEI znajduje swoją klasę sam. */
    private static void initCompat() {
        if (Loader.isModLoaded("ForgeMultipart")) pl.geesio.redcircuit.compat.fmp.FmpCompat.init();
        if (Loader.isModLoaded("ComputerCraft")) pl.geesio.redcircuit.compat.computer.CcCompat.init();
        if (Loader.isModLoaded("OpenComputers")) pl.geesio.redcircuit.compat.computer.OcCompat.init();
        if (Loader.isModLoaded("Waila")) {
            FMLInterModComms.sendMessage("Waila", "register", "pl.geesio.redcircuit.compat.waila.WailaCompat.callbackRegister");
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }
}
