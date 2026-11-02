package com.science.gtnl.common.block.blocks;

import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.science.gtnl.common.block.blocks.tile.TileEntitySuperDualInterface;
import com.science.gtnl.common.gui.GTNLGuiHandler;
import com.science.gtnl.utils.enums.GuiType;

import appeng.api.util.IOrientable;
import appeng.core.features.AEFeature;

public class BlockSuperDualInterface extends BlockSuperInterfaceBase {

    public BlockSuperDualInterface() {
        super();
        setHardness(50.0F);
        setResistance(2000.0F);
        setHarvestLevel("pickaxe", 3);
        setTileEntity(TileEntitySuperDualInterface.class);
        setFeature(EnumSet.of(AEFeature.Core));
    }

    @Override
    public String getUnlocalizedName() {
        return "gtnl.block.super_dual_interface";
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY,
        float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        var tile = getTileEntity(world, x, y, z);
        if (tile == null) {
            return false;
        }

        GTNLGuiHandler.openGui(player, GuiType.SuperDualInterfaceGUI, null, world, x, y, z);
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        TileEntitySuperDualInterface tile = getTileEntity(world, x, y, z);
        if (tile != null) {
            tile.getInterfaceDuality()
                .updateRedstoneState();
        }
    }

    @Override
    public boolean hasCustomRotation() {
        return true;
    }

    @Override
    public void customRotateBlock(IOrientable rotatable, ForgeDirection axis) {
        if (rotatable instanceof TileEntitySuperDualInterface superDualInterface) {
            superDualInterface.setSide(axis);
        }
    }
}
