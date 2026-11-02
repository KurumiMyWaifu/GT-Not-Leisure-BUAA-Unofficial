package com.science.gtnl.common.render;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.science.gtnl.common.machine.basicMachine.StructureCompiler;
import com.science.gtnl.utils.ColorUtils;
import com.science.gtnl.utils.structure.StructureExportTask.Bounds;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@SideOnly(Side.CLIENT)
public class StructureCompilerRenderer {

    private static final Set<StructureCompiler> MACHINES = Collections.newSetFromMap(new WeakHashMap<>());

    public static void track(StructureCompiler machine) {
        MACHINES.add(machine);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MACHINES.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        Entity camera = minecraft.renderViewEntity;
        if (minecraft.theWorld == null || camera == null) return;
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glTranslated(-x, -y, -z);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            Iterator<StructureCompiler> machines = MACHINES.iterator();
            while (machines.hasNext()) {
                StructureCompiler machine = machines.next();
                IGregTechTileEntity base = machine.getBaseMetaTileEntity();
                if (base == null || base.isDead() || base.getWorld() != minecraft.theWorld) {
                    machines.remove();
                    continue;
                }
                if (!machine.getShowHighlightBox()) continue;
                Bounds bounds = machine.getSelectionBounds();
                RenderGlobal.drawOutlinedBoundingBox(
                    AxisAlignedBB.getBoundingBox(
                        bounds.minX(),
                        bounds.minY(),
                        bounds.minZ(),
                        bounds.maxX() + 1.0D,
                        bounds.maxY() + 1.0D,
                        bounds.maxZ() + 1.0D),
                    ColorUtils.rgb(ColorUtils.guiTextColorRed));
            }
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
