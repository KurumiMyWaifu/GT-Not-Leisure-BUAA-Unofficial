package com.science.gtnl.client.structure;

import static com.science.gtnl.utils.structure.StructureExportTask.FILE_NAME_PATTERN;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import com.science.gtnl.ScienceNotLeisure;
import com.science.gtnl.common.machine.basicMachine.StructureCompiler;
import com.science.gtnl.utils.structure.StructureExportTask;
import com.science.gtnl.utils.structure.StructureExportTask.ExportException;
import com.science.gtnl.utils.structure.StructureExportTask.ExportResult;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class StructureCompilerClientExport {

    private static final Path EXPORT_DIRECTORY = Path.of("gtnl", "structures");
    private static final Map<StructureCompiler, ExportState> EXPORTS = new WeakHashMap<>();

    public static void start(StructureCompiler machine, EntityPlayer player) {
        if (player == null) return;
        ExportState existing = EXPORTS.get(machine);
        if (existing != null && existing.isActive()) {
            player.addChatMessage(new ChatComponentTranslation("gtnl.machine.structure_compiler.error.busy"));
            return;
        }
        String name = machine.getExportName();
        if (name.isEmpty() || !FILE_NAME_PATTERN.matcher(name)
            .matches()) {
            machine.setLocalExportState("failed", 0);
            player.addChatMessage(new ChatComponentTranslation("gtnl.machine.structure_compiler.error.name"));
            return;
        }
        try {
            StructureExportTask task = new StructureExportTask(machine.getBaseMetaTileEntity(), machine.getSelection());
            EXPORTS.put(machine, new ExportState(task, name));
            machine.setLocalExportState("scanning", task.getProgress());
        } catch (ExportException exception) {
            machine.setLocalExportState("failed", 0);
            player.addChatMessage(new ChatComponentTranslation(exception.translationKey, exception.arguments));
        } catch (RuntimeException exception) {
            ScienceNotLeisure.LOG.error("Failed to start client structure export", exception);
            machine.setLocalExportState("failed", 0);
            player.addChatMessage(new ChatComponentTranslation("gtnl.machine.structure_compiler.error.write"));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        Iterator<Map.Entry<StructureCompiler, ExportState>> iterator = EXPORTS.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<StructureCompiler, ExportState> entry = iterator.next();
            StructureCompiler machine = entry.getKey();
            ExportState state = entry.getValue();
            if (machine == null) {
                iterator.remove();
                continue;
            }
            var base = machine.getBaseMetaTileEntity();
            if (minecraft.theWorld == null || base == null || base.getWorld() != minecraft.theWorld) {
                machine.setLocalExportState("idle", 0);
                iterator.remove();
                continue;
            }
            try {
                if (state.task != null) {
                    if (!state.task.advance()) {
                        machine.setLocalExportState("scanning", state.task.getProgress());
                        continue;
                    }
                    StructureExportTask task = state.task;
                    String name = state.name;
                    Path directory = minecraft.mcDataDir.toPath()
                        .resolve(EXPORT_DIRECTORY);
                    state.pendingWrite = CompletableFuture.supplyAsync(() -> writeExport(task, name, directory));
                    state.task = null;
                    machine.setLocalExportState("writing", 0);
                    continue;
                }
                if (state.pendingWrite == null || !state.pendingWrite.isDone()) continue;
                ExportResult result = state.pendingWrite.join();
                ScienceNotLeisure.LOG.info("\n{}", result.report());
                machine.setGeneratedCode(result.code());
                machine.setLocalExportState("done", 100);
                EntityPlayer player = minecraft.thePlayer;
                if (player != null) {
                    player.addChatMessage(
                        new ChatComponentTranslation(
                            "gtnl.machine.structure_compiler.exported",
                            directoryFor(minecraft).resolve(result.fileName())
                                .toAbsolutePath()
                                .toString(),
                            result.elementCount(),
                            result.warningCount()));
                }
                iterator.remove();
            } catch (ExportException exception) {
                fail(machine, minecraft.thePlayer, exception.translationKey, exception.arguments);
                iterator.remove();
            } catch (CompletionException exception) {
                ScienceNotLeisure.LOG.error("Failed to save client structure export", exception.getCause());
                fail(machine, minecraft.thePlayer, "gtnl.machine.structure_compiler.error.write");
                iterator.remove();
            } catch (RuntimeException exception) {
                ScienceNotLeisure.LOG.error("Failed to export client structure", exception);
                fail(machine, minecraft.thePlayer, "gtnl.machine.structure_compiler.error.write");
                iterator.remove();
            }
        }
    }

    private static ExportResult writeExport(StructureExportTask task, String name, Path directory) {
        try {
            ExportResult result = task.finish(name);
            Files.createDirectories(directory);
            Path target = directory.resolve(result.fileName());
            Path temporary = Files.createTempFile(directory, name + "-export-", ".tmp");
            try {
                Files.write(temporary, result.data());
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return result;
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }

    private static Path directoryFor(Minecraft minecraft) {
        return minecraft.mcDataDir.toPath()
            .resolve(EXPORT_DIRECTORY);
    }

    private static void fail(StructureCompiler machine, EntityPlayer player, String translationKey,
        Object... arguments) {
        machine.setLocalExportState("failed", 0);
        if (player != null) player.addChatMessage(new ChatComponentTranslation(translationKey, arguments));
    }

    private static class ExportState {

        private StructureExportTask task;
        private final String name;
        private CompletableFuture<ExportResult> pendingWrite;

        private ExportState(StructureExportTask task, String name) {
            this.task = task;
            this.name = name;
        }

        private boolean isActive() {
            return task != null || pendingWrite != null;
        }
    }
}
