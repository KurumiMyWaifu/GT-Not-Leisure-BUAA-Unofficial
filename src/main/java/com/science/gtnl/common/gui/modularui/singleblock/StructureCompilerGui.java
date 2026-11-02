package com.science.gtnl.common.gui.modularui.singleblock;

import static com.science.gtnl.utils.structure.StructureExportTask.FILE_NAME_PATTERN;
import static gregtech.api.util.GTUtility.clamp;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.ShortSyncValue;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.science.gtnl.client.structure.StructureCompilerClientExport;
import com.science.gtnl.common.machine.basicMachine.StructureCompiler;
import com.science.gtnl.common.render.StructureCompilerRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

public class StructureCompilerGui extends MTETieredMachineBlockBaseGui<StructureCompiler> {

    private static final int COLUMN_WIDTH = 84;
    private static final int CONTENT_WIDTH = 194;

    public StructureCompilerGui(StructureCompiler machine) {
        super(machine);
    }

    @Override
    protected int getBasePanelWidth() {
        return 208;
    }

    @Override
    protected int getBasePanelHeight() {
        return 204;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager
            .syncValue("exportName", new StringSyncValue(machine::getExportName, machine::setExportName).allowC2S());
        if (syncManager.isClient()) trackSelection();
    }

    @SideOnly(Side.CLIENT)
    private void trackSelection() {
        StructureCompilerRenderer.track(machine);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ShortSyncValue[] coordinates = new ShortSyncValue[6];
        for (int index = 0; index < coordinates.length; index++) {
            int coordinate = index;
            coordinates[index] = new ShortSyncValue(
                () -> machine.getNumber(coordinate),
                number -> machine.setNumber(coordinate, number)).allowC2S();
        }
        StringSyncValue name = syncManager.findSyncHandler("exportName", StringSyncValue.class);
        return Flow.column()
            .fullWidth()
            .coverChildrenHeight()
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                Flow.row()
                    .coverChildren()
                    .childPadding(8)
                    .child(createInputColumn(true, coordinates, syncManager))
                    .child(createInputColumn(false, coordinates, syncManager)))
            .child(
                Flow.row()
                    .coverChildren()
                    .childPadding(4)
                    .child(
                        IKey.lang("gtnl.machine.structure_compiler.gui.file")
                            .asWidget()
                            .size(42, 16))
                    .child(
                        new TextFieldWidget().value(name)
                            .size(140, 16)
                            .setMaxLength(64)
                            .setPattern(FILE_NAME_PATTERN)
                            .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                            .tooltip(t -> t.addLine(IKey.lang("gtnl.machine.structure_compiler.gui.file.tooltip")))))
            .child(
                IKey.dynamic(
                    () -> IKey.lang("gtnl.machine.structure_compiler.status." + machine.getStatus())
                        .get()
                        + ("scanning".equals(machine.getStatus()) ? " " + machine.getExportProgress() + "%" : ""))
                    .asWidget()
                    .size(CONTENT_WIDTH, 12)
                    .textAlign(Alignment.CenterLeft));
    }

    private Flow createInputColumn(boolean origin, ShortSyncValue[] coordinates, PanelSyncManager syncManager) {
        Flow column = Flow.column()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.lang(
                    origin ? "GT5U.machines.debugstructurewriter.gui.origin"
                        : "GT5U.machines.debugstructurewriter.gui.size")
                    .asWidget()
                    .size(COLUMN_WIDTH, 12)
                    .textAlign(Alignment.CenterLeft));
        for (int axis = 0; axis < 3; axis++) {
            ShortSyncValue coordinate = coordinates[(origin ? 0 : 3) + axis];
            int minimum = origin ? Short.MIN_VALUE : 1;
            column.child(
                Flow.row()
                    .coverChildren()
                    .childPadding(2)
                    .child(
                        new TextFieldWidget().value(coordinate)
                            .size(42, 14)
                            .setTextAlignment(Alignment.CenterRight)
                            .setMaxLength(origin ? 6 : 5)
                            .formatAsInteger(true)
                            .numbersInt(minimum, Short.MAX_VALUE)
                            .background(GTGuiTextures.BACKGROUND_TEXT_FIELD))
                    .child(
                        IKey.str(Character.toString((char) ('A' + axis)))
                            .asWidget()
                            .size(8, 14))
                    .child(createArithmeticButton(coordinate, origin, false, syncManager))
                    .child(createArithmeticButton(coordinate, origin, true, syncManager)));
        }
        return column;
    }

    private ButtonWidget<?> createArithmeticButton(ShortSyncValue coordinate, boolean origin, boolean multiply,
        PanelSyncManager syncManager) {
        return new ButtonWidget<>().size(14)
            .overlay(multiply ? GuiTextures.CLOSE : GuiTextures.ADD)
            .syncHandler(
                new InteractionSyncHandler().setOnMousePressed(
                    mouse -> { if (!syncManager.isClient()) changeCoordinate(coordinate, origin, multiply, mouse); }))
            .tooltip(
                t -> t.addLine(IKey.lang("gtnl.machine.structure_compiler.gui." + (multiply ? "multiply" : "add"))));
    }

    private void changeCoordinate(ShortSyncValue coordinate, boolean origin, boolean multiply, MouseData mouse) {
        if (mouse.mouseButton != 0 && mouse.mouseButton != 1) return;
        int value = coordinate.getShortValue();
        int minimum = origin ? Short.MIN_VALUE : 1;
        if (multiply) {
            int factor = mouse.shift ? 16 : 2;
            if (value != 0) {
                int lower = value < 0 ? Short.MIN_VALUE : 1;
                int upper = value < 0 ? -1 : Short.MAX_VALUE;
                value = clamp(mouse.mouseButton == 0 ? value * factor : value / factor, lower, upper);
            }
        } else {
            int delta = mouse.shift ? 16 : 1;
            value += mouse.mouseButton == 0 ? delta : -delta;
        }
        coordinate.setShortValue((short) clamp(value, minimum, Short.MAX_VALUE));
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        BooleanSyncValue transpose = new BooleanSyncValue(machine::getTranspose, machine::setTranspose).allowC2S();
        BooleanSyncValue highlight = new BooleanSyncValue(machine::getShowHighlightBox, machine::setShowHighlightBox)
            .allowC2S();
        return super.createBottomLeftCornerFlow(panel, syncManager).child(
            new ButtonWidget<>().size(18)
                .overlay(GTGuiTextures.OVERLAY_BUTTON_PRINT)
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    if (syncManager.isClient()) startClientExport(syncManager.getPlayer());
                    return true;
                })
                .tooltip(t -> t.addLine(IKey.lang("gtnl.machine.structure_compiler.gui.export"))))
            .child(
                new ButtonWidget<>().size(18)
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_COPY)
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        if (syncManager.isClient()) copyCode(machine.getGeneratedCode(), syncManager.getPlayer());
                        return true;
                    })
                    .tooltip(t -> t.addLine(IKey.lang("gtnl.machine.structure_compiler.gui.copy"))))
            .child(
                new ToggleButton().size(18)
                    .value(transpose)
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_TRANSPOSE)
                    .tooltip(t -> t.addLine(IKey.lang("gtnl.machine.structure_compiler.gui.transpose"))))
            .child(
                new ToggleButton().size(18)
                    .value(highlight)
                    .overlay(GTGuiTextures.OVERLAY_BUTTON_BOUNDING_BOX)
                    .tooltip(t -> t.addLine(IKey.lang("GT5U.machines.debugstructurewriter.gui.highlight.tooltip"))));
    }

    @SideOnly(Side.CLIENT)
    private void startClientExport(EntityPlayer player) {
        StructureCompilerClientExport.start(machine, player);
    }

    @SideOnly(Side.CLIENT)
    private static void copyCode(String code, EntityPlayer player) {
        if (code.isEmpty()) {
            player.addChatMessage(new ChatComponentTranslation("gtnl.machine.structure_compiler.error.no_code"));
        } else {
            GuiScreen.setClipboardString(code);
        }
    }

    @Override
    protected boolean supportsTopRightCornerFlow() {
        return false;
    }
}
