package pl.geesio.redcircuit.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;
import pl.geesio.redcircuit.energy.BlockEnergyMachine;
import pl.geesio.redcircuit.energy.TileGenerator;

/**
 * Trójwymiarowy wiatrak (ok. 5x5) i koło wodne (3x3) przed generatorem (port RotorRenderer z 1.21).
 * Model z prostopadłościanów w pikselach, oteksturowany teksturami bloków z gry (drewno, wełna, żelazo).
 */
public class RenderRotor extends TileEntitySpecialRenderer {
    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTick) {
        if (!(te instanceof TileGenerator gen) || gen.rotor == TileGenerator.ROTOR_NONE || te.getWorldObj() == null) return;
        ForgeDirection facing = BlockEnergyMachine.facing(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord);
        boolean windmill = gen.rotor == TileGenerator.ROTOR_WINDMILL;
        float speed = windmill ? 2.0F : 3.5F;   // stopnie na tick
        float angle = gen.running ? ((te.getWorldObj().getTotalWorldTime() % 3600) + partialTick) * speed : 0F;
        float yRot = switch (facing) {
            case WEST -> 90F;
            case NORTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };

        int light = te.getWorldObj().getLightBrightnessForSkyBlocks(te.xCoord + facing.offsetX, te.yCoord, te.zCoord + facing.offsetZ, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glRotatef(-yRot, 0F, 1F, 0F);           // lokalne +Z = przód generatora
        GL11.glTranslatef(0F, 0F, 0.5F);             // płaszczyzna przedniej ściany
        GL11.glScalef(1 / 16F, 1 / 16F, 1 / 16F);    // dalej wszystko w pikselach
        GL11.glRotatef(windmill ? -angle : angle, 0F, 0F, 1F);
        if (windmill) windmill();
        else waterWheel();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    // ------------------------------------------------------------------ modele

    private static IIcon beam() {
        return Blocks.log.getIcon(2, 1);        // pień świerku
    }

    private static void windmill() {
        IIcon beam = beam(), metal = Blocks.iron_block.getIcon(0, 0), cloth = Blocks.wool.getIcon(0, 0), planks = Blocks.planks.getIcon(0, 0);
        box(beam, -2, -2, 0, 2, 2, 9);                  // wał
        box(metal, -5, -5, 6, 5, 5, 12);                // piasta
        box(metal, -3, -3, 12, 3, 3, 14);               // kołpak
        for (int k = 0; k < 4; k++) {
            GL11.glPushMatrix();
            GL11.glRotatef(k * 90, 0F, 0F, 1F);
            box(beam, -1.5F, 4, 7.5F, 1.5F, 38, 10.5F);   // ramię
            // Skrzydło: płótno na ramie, lekko skręcone
            GL11.glPushMatrix();
            GL11.glTranslatef(1.5F, 0F, 9F);
            GL11.glRotatef(-14F, 0F, 1F, 0F);
            box(cloth, 0, 9, -0.5F, 12.5F, 37, 0.5F);
            for (float yy : new float[]{9, 16, 23, 30, 36}) box(planks, 0, yy, 0.5F, 12.5F, yy + 1, 1.5F);
            box(planks, 11.5F, 9, 0.5F, 12.5F, 37, 1.5F);
            GL11.glPopMatrix();
            GL11.glPopMatrix();
        }
    }

    private static void waterWheel() {
        IIcon beam = beam(), metal = Blocks.iron_block.getIcon(0, 0), planks = Blocks.planks.getIcon(0, 0), dark = Blocks.planks.getIcon(0, 1);
        box(beam, -2, -2, 0, 2, 2, 16);                 // oś
        box(metal, -4, -4, 2, 4, 4, 14);                // piasta
        float[][] rims = {{2, 4}, {12, 14}};
        for (float[] rim : rims) {
            for (int i = 0; i < 16; i++) {              // obręcz z 16 segmentów
                GL11.glPushMatrix();
                GL11.glRotatef(i * 22.5F, 0F, 0F, 1F);
                box(dark, -4.4F, 20, rim[0], 4.4F, 23, rim[1]);
                GL11.glPopMatrix();
            }
            for (int i = 0; i < 8; i++) {               // szprychy
                GL11.glPushMatrix();
                GL11.glRotatef(i * 45 + 22.5F, 0F, 0F, 1F);
                box(beam, -1, 4, rim[0] + 0.5F, 1, 20, rim[1] - 0.5F);
                GL11.glPopMatrix();
            }
        }
        for (int i = 0; i < 12; i++) {                  // łopatki
            GL11.glPushMatrix();
            GL11.glRotatef(i * 30, 0F, 0F, 1F);
            box(planks, -0.75F, 13, 2, 0.75F, 24, 14);
            GL11.glPopMatrix();
        }
    }

    // ------------------------------------------------------------------ prostopadłościany

    /** Prostopadłościan w pikselach; tekstura powtarza się co 16 pikseli (bez rozciągania). */
    private static void box(IIcon s, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        face(t, s, x0, y0, z1, 1, 0, 0, dx, 0, 1, 0, dy, 0, 0, 1);
        face(t, s, x1, y0, z0, -1, 0, 0, dx, 0, 1, 0, dy, 0, 0, -1);
        face(t, s, x1, y0, z1, 0, 0, -1, dz, 0, 1, 0, dy, 1, 0, 0);
        face(t, s, x0, y0, z0, 0, 0, 1, dz, 0, 1, 0, dy, -1, 0, 0);
        face(t, s, x0, y1, z1, 1, 0, 0, dx, 0, 0, -1, dz, 0, 1, 0);
        face(t, s, x0, y0, z0, 1, 0, 0, dx, 0, 0, 1, dz, 0, -1, 0);
        t.draw();
    }

    /** Ściana od punktu o, wzdłuż osi u (długość lu) i v (długość lv), z normalną n; dzielona na kafle 16x16. */
    private static void face(Tessellator t, IIcon s, float ox, float oy, float oz,
                             float ux, float uy, float uz, float lu,
                             float vx, float vy, float vz, float lv,
                             float nx, float ny, float nz) {
        t.setNormal(nx, ny, nz);
        for (float a = 0; a < lu - 1e-4F; a += 16) {
            float a1 = Math.min(lu, a + 16);
            for (float b = 0; b < lv - 1e-4F; b += 16) {
                float b1 = Math.min(lv, b + 16);
                double u0 = s.getInterpolatedU(a % 16), u1 = s.getInterpolatedU(a % 16 + (a1 - a));
                double v0 = s.getInterpolatedV(16 - (b % 16 + (b1 - b))), v1 = s.getInterpolatedV(16 - b % 16);
                t.addVertexWithUV(ox + ux * a + vx * b, oy + uy * a + vy * b, oz + uz * a + vz * b, u0, v1);
                t.addVertexWithUV(ox + ux * a1 + vx * b, oy + uy * a1 + vy * b, oz + uz * a1 + vz * b, u1, v1);
                t.addVertexWithUV(ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1, u1, v0);
                t.addVertexWithUV(ox + ux * a + vx * b1, oy + uy * a + vy * b1, oz + uz * a + vz * b1, u0, v0);
            }
        }
    }
}
