package pl.geesio.redcircuit.proxy;

import cpw.mods.fml.client.registry.RenderingRegistry;
import pl.geesio.redcircuit.RenderIds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import pl.geesio.redcircuit.client.GuiGate;
import pl.geesio.redcircuit.client.RenderGate;
import pl.geesio.redcircuit.client.RenderWire;
import pl.geesio.redcircuit.gate.GateType;

/** Kod tylko po stronie klienta: renderery bloków, okna bez kontenera. */
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        RenderIds.faceWire = RenderingRegistry.getNextAvailableRenderId();
        RenderIds.encasedWire = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new RenderWire(RenderIds.faceWire, false));
        RenderingRegistry.registerBlockHandler(new RenderWire(RenderIds.encasedWire, true));
        RenderIds.gate = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new RenderGate(RenderIds.gate));
        RenderIds.wireless = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new pl.geesio.redcircuit.client.RenderWireless(RenderIds.wireless));
        RenderIds.tube = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new pl.geesio.redcircuit.client.RenderTube(RenderIds.tube));
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(pl.geesio.redcircuit.tube.TileTube.class,
                new pl.geesio.redcircuit.client.RenderTubeItems());
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(pl.geesio.redcircuit.energy.TileGenerator.class,
                new pl.geesio.redcircuit.client.RenderRotor());
        RenderIds.boxes = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new pl.geesio.redcircuit.client.RenderBoxes(RenderIds.boxes));
        cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(pl.geesio.redcircuit.fluid.TileFluidTank.class,
                new pl.geesio.redcircuit.client.RenderFluidTank());
    }

    @Override
    public void openGateGui(World world, int x, int y, int z, GateType type) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiGate(x, y, z, type));
    }

    @Override
    public void openFrequencyGui(int target, int x, int y, int z, int frequency, String titleKey) {
        Minecraft.getMinecraft().displayGuiScreen(new pl.geesio.redcircuit.client.GuiFrequency(target, x, y, z, frequency, titleKey));
    }
}
