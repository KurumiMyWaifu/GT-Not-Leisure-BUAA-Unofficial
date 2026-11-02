package com.science.gtnl.common.machine.basicMachine;

import static com.science.gtnl.utils.structure.StructureExportTask.FILE_NAME_PATTERN;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.science.gtnl.common.gui.modularui.singleblock.StructureCompilerGui;
import com.science.gtnl.utils.structure.StructureExportTask;
import com.science.gtnl.utils.structure.StructureExportTask.Bounds;
import com.science.gtnl.utils.structure.StructureExportTask.Selection;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.api.render.TextureFactory;

public class StructureCompiler extends MTETieredMachineBlock {

    private final short[] numbers = { 0, 0, 0, 1, 1, 1 };
    private boolean transpose = true;
    private boolean showHighlightBox = true;
    private String exportName = "structure";
    private String generatedCode = "";
    private String status = "idle";
    private int exportProgress;
    private ForgeDirection cachedDirection;
    private int cachedX;
    private int cachedY;
    private int cachedZ;
    private Bounds cachedBounds;

    public StructureCompiler(int id, String name, String regionalName, int tier) {
        super(id, name, regionalName, tier, 0, "");
    }

    public StructureCompiler(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, 0, description, textures);
    }

    public short getNumber(int index) {
        return numbers[index];
    }

    public void setNumber(int index, short number) {
        short clamped = index < 3 ? number : (short) Math.max(1, number);
        if (numbers[index] == clamped) return;
        numbers[index] = clamped;
        invalidateSelection();
    }

    public boolean getTranspose() {
        return transpose;
    }

    public void setTranspose(boolean transpose) {
        if (this.transpose == transpose) return;
        this.transpose = transpose;
        invalidateSelection();
    }

    public boolean getShowHighlightBox() {
        return showHighlightBox;
    }

    public void setShowHighlightBox(boolean showHighlightBox) {
        if (this.showHighlightBox == showHighlightBox) return;
        this.showHighlightBox = showHighlightBox;
        getBaseMetaTileEntity().markDirty();
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String name) {
        if (name == null || !FILE_NAME_PATTERN.matcher(name)
            .matches() || exportName.equals(name)) return;
        exportName = name;
        getBaseMetaTileEntity().markDirty();
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public String getStatus() {
        return status;
    }

    public void setLocalExportState(String status, int progress) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isClientSide()) return;
        this.status = status;
        this.exportProgress = Math.max(0, Math.min(100, progress));
    }

    public void setGeneratedCode(String generatedCode) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isClientSide()) return;
        this.generatedCode = generatedCode == null ? "" : generatedCode;
    }

    public int getExportProgress() {
        return exportProgress;
    }

    public Bounds getSelectionBounds() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (cachedBounds == null || cachedDirection != base.getFrontFacing()
            || cachedX != base.getXCoord()
            || cachedY != base.getYCoord()
            || cachedZ != base.getZCoord()) {
            cachedDirection = base.getFrontFacing();
            cachedX = base.getXCoord();
            cachedY = base.getYCoord();
            cachedZ = base.getZCoord();
            cachedBounds = getSelection().bounds(cachedX, cachedY, cachedZ);
        }
        return cachedBounds;
    }

    public Selection getSelection() {
        return new Selection(
            getBaseMetaTileEntity().getFrontFacing(),
            numbers[0],
            numbers[1],
            numbers[2],
            numbers[3],
            numbers[4],
            numbers[5],
            transpose);
    }

    private void invalidateSelection() {
        cachedBounds = null;
        getBaseMetaTileEntity().markDirty();
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        for (int index = 0; index < numbers.length; index++) nbt.setShort("eData" + index, numbers[index]);
        nbt.setBoolean("Transpose", transpose);
        nbt.setBoolean("HighlightBox", showHighlightBox);
        nbt.setString("ExportName", exportName);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        for (int index = 0; index < numbers.length; index++) {
            numbers[index] = index < 3 ? nbt.getShort("eData" + index)
                : (short) Math.max(1, nbt.getShort("eData" + index));
        }
        transpose = !nbt.hasKey("Transpose") || nbt.getBoolean("Transpose");
        showHighlightBox = !nbt.hasKey("HighlightBox") || nbt.getBoolean("HighlightBox");
        String savedName = nbt.getString("ExportName");
        exportName = !savedName.isEmpty() && FILE_NAME_PATTERN.matcher(savedName)
            .matches() ? savedName : "structure";
        cachedBounds = null;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new StructureCompiler(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity base, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1], TextureFactory.builder()
            .addIcon(
                side == facing ? Textures.BlockIcons.STRUCTURE_MARK : Textures.BlockIcons.OVERLAY_TELEPORTER_ACTIVE)
            .glow()
            .build() };
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] textures) {
        return null;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int slot, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int slot, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return facing != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean isElectric() {
        return false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity base, EntityPlayer player) {
        openGui(player);
        return true;
    }

    @Override
    public String[] getDescription() {
        return new String[] { StatCollector.translateToLocal("gtnl.machine.structure_compiler.tooltip.0"),
            StatCollector
                .translateToLocalFormatted("gtnl.machine.structure_compiler.tooltip.1", StructureExportTask.MAX_BLOCKS),
            StatCollector.translateToLocal("gtnl.machine.structure_compiler.tooltip.2") };
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public String[] getInfoData() {
        return new String[] { StatCollector.translateToLocal("gtnl.machine.structure_compiler.status." + status) };
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new StructureCompilerGui(this).build(data, syncManager, uiSettings);
    }
}
