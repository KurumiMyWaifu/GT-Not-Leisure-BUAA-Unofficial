package com.science.gtnl.utils.structure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnllib.utils.StructureFileCodec;
import com.science.gtnl.ScienceNotLeisure;
import com.science.gtnl.utils.StructureUtils;
import com.science.gtnl.utils.structure.StructureElementRegistry.CasingExpression;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTStructureUtility;
import gregtech.common.blocks.BlockFrameBox;
import it.unimi.dsi.fastutil.ints.Int2CharMap;
import it.unimi.dsi.fastutil.ints.Int2CharOpenHashMap;

public class StructureExportTask {

    public static final int MAX_BLOCKS = 1_048_5760;
    public static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-z0-9_-]{0,64}");
    private static final int BLOCKS_PER_TICK = 65536;
    private static final long SCAN_BUDGET_NS = 2_000_000L;
    private static final String SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz=|!@#$%&()[]{};:<>/?_,.*^'`";

    private final World world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final Selection selection;
    private final ExtendedFacing facing;
    private final String[][] shape;
    private final char[] row;
    private final int[] abc = new int[3];
    private final int[] xyz = new int[3];
    private final Map<Block, Int2CharMap> blockSymbols = new IdentityHashMap<>();
    private final Map<TileKey, Character> tileSymbols = new HashMap<>();
    private final List<String> elements = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Set<String> imports = new TreeSet<>();
    private int scanned;

    public StructureExportTask(IGregTechTileEntity origin, Selection selection) {
        this.world = origin.getWorld();
        this.originX = origin.getXCoord();
        this.originY = origin.getYCoord();
        this.originZ = origin.getZCoord();
        this.selection = selection;
        this.facing = ExtendedFacing.of(selection.direction());
        if (selection.sizeA() < 1 || selection.sizeB() < 1
            || selection.sizeC() < 1
            || selection.sizeA() > Short.MAX_VALUE
            || selection.sizeB() > Short.MAX_VALUE
            || selection.sizeC() > Short.MAX_VALUE
            || selection.volume() > MAX_BLOCKS) {
            throw new ExportException("gtnl.machine.structure_compiler.error.volume", MAX_BLOCKS);
        }
        validateBounds();
        shape = new String[selection.sizeC()][selection.sizeB()];
        row = new char[selection.sizeA()];
        addImport(StructureUtility.class);
        addImport(StructureUtils.class);
    }

    public boolean advance() {
        long deadline = System.nanoTime() + SCAN_BUDGET_NS;
        int end = Math.min((int) selection.volume(), scanned + BLOCKS_PER_TICK);
        while (scanned < end) {
            int a = scanned % selection.sizeA();
            int b = scanned / selection.sizeA() % selection.sizeB();
            int c = scanned / (selection.sizeA() * selection.sizeB());
            abc[0] = a - selection.offsetA();
            abc[1] = b - selection.offsetB();
            abc[2] = c - selection.offsetC();
            facing.getWorldOffset(abc, xyz);
            int x = originX + xyz[0];
            int y = originY + xyz[1];
            int z = originZ + xyz[2];
            if (!world.blockExists(x, y, z)) {
                throw new ExportException("gtnl.machine.structure_compiler.error.unloaded");
            }
            row[a] = x == originX && y == originY && z == originZ ? '~' : getSymbol(x, y, z);
            if (a + 1 == selection.sizeA()) shape[c][b] = new String(row);
            scanned++;
            if ((scanned & 127) == 0 && System.nanoTime() >= deadline) break;
        }
        return scanned == selection.volume();
    }

    public int getProgress() {
        return (int) (scanned * 100L / selection.volume());
    }

    public ExportResult finish(String name) throws IOException {
        if (scanned != selection.volume()) throw new IllegalStateException("Structure scan is incomplete");
        if (name == null || name.isEmpty()
            || !FILE_NAME_PATTERN.matcher(name)
                .matches()) {
            throw new ExportException("gtnl.machine.structure_compiler.error.name");
        }
        String[][] exportedShape = selection.transpose() ? StructureUtility.transpose(shape) : shape;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StructureFileCodec.writeBinary(output, exportedShape);
        byte[] data = output.toByteArray();
        String relativeFile = "gtnl/structures/" + name + ".mbs";
        String code = createCode(name);
        String report = "Structure: " + name
            + "\nClient file: "
            + relativeFile
            + "\nDimensions (A, B, C): "
            + selection.sizeA()
            + ", "
            + selection.sizeB()
            + ", "
            + selection.sizeC()
            + "\nOffsets (A, B, C): "
            + selection.offsetA()
            + ", "
            + selection.offsetB()
            + ", "
            + selection.offsetC()
            + "\nResource: assets/"
            + ScienceNotLeisure.RESOURCE_ROOT_ID
            + "/multiblock/"
            + name
            + ".mbs"
            + "\nTranspose on load: "
            + selection.transpose()
            + "\n\n"
            + code;
        if (!warnings.isEmpty()) report += "\nManual review:\n" + String.join("\n", warnings) + "\n";
        return new ExportResult(name + ".mbs", data, code, report, elements.size(), warnings.size());
    }

    private void validateBounds() {
        Bounds bounds = selection.bounds(originX, originY, originZ);
        if (bounds.minY() < 0 || bounds.maxY() >= world.getHeight()) {
            throw new ExportException("gtnl.machine.structure_compiler.error.height");
        }
        if (!world.checkChunksExist(
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.maxX(),
            bounds.maxY(),
            bounds.maxZ())) {
            throw new ExportException("gtnl.machine.structure_compiler.error.unloaded");
        }
    }

    private char getSymbol(int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == Blocks.air) return ' ';
        int blockMetadata = world.getBlockMetadata(x, y, z);
        int metadata = block instanceof BlockFrameBox ? block.getDamageValue(world, x, y, z) : blockMetadata;
        TileEntity tile = !(block instanceof BlockFrameBox) && block.hasTileEntity(blockMetadata)
            ? world.getTileEntity(x, y, z)
            : null;
        if (tile != null) {
            int machineId = tile instanceof IGregTechTileEntity gtTile ? gtTile.getMetaTileID() : -1;
            TileKey key = new TileKey(block, metadata, tile.getClass(), machineId);
            Character existing = tileSymbols.get(key);
            if (existing != null) return existing;
            CasingExpression casing = StructureElementRegistry.findCasing(block, metadata);
            if (casing != null) {
                addImport(casing.owner());
                char symbol = addElement(casing.expression());
                tileSymbols.put(key, symbol);
                return symbol;
            }
            char symbol = addTileElement(block, metadata, tile, machineId);
            tileSymbols.put(key, symbol);
            return symbol;
        }
        Int2CharMap metadataSymbols = blockSymbols.computeIfAbsent(block, ignored -> new Int2CharOpenHashMap());
        char existing = metadataSymbols.get(metadata);
        if (existing != 0) return existing;
        char symbol = addElement(getBlockExpression(block, metadata));
        metadataSymbols.put(metadata, symbol);
        return symbol;
    }

    private String getBlockExpression(Block block, int metadata) {
        CasingExpression casing = StructureElementRegistry.findCasing(block, metadata);
        if (casing != null) {
            addImport(casing.owner());
            return casing.expression();
        }
        if (block instanceof BlockFrameBox) {
            Materials material = BlockFrameBox.getMaterial(metadata);
            if (material != null) {
                addImport(GTStructureUtility.class);
                addImport(Materials.class);
                return "GTStructureUtility.ofFrame(Materials." + material.name() + ")";
            }
        }
        return "StructureUtility.ofBlock(" + getBlockReference(block) + ", " + metadata + ")";
    }

    private char addTileElement(Block block, int metadata, TileEntity tile, int machineId) {
        String blockReference = getBlockReference(block);
        String expression;
        if (machineId >= 0) {
            addImport(IGregTechTileEntity.class);
            expression = "StructureUtility.ofTileAdder((machine, tile) -> tile instanceof IGregTechTileEntity gtTile"
                + " && gtTile.getMetaTileID() == "
                + machineId
                + ", "
                + blockReference
                + ", "
                + metadata
                + ")";
        } else {
            expression = "StructureUtility.ofBlock(" + blockReference + ", " + metadata + ")";
        }
        char symbol = addElement(expression);
        warnings.add(
            "'" + symbol
                + "': "
                + tile.getClass()
                    .getName()
                + (machineId >= 0 ? " (machine ID " + machineId + ")" : "")
                + "; configure hatch registration, placement, and NBT checks manually.");
        return symbol;
    }

    private String getBlockReference(Block block) {
        UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(block);
        if (identifier == null) throw new ExportException("gtnl.machine.structure_compiler.error.unregistered");
        addImport(GameRegistry.class);
        return "GameRegistry.findBlock(\"" + escape(identifier.modId) + "\", \"" + escape(identifier.name) + "\")";
    }

    private char addElement(String expression) {
        if (elements.size() >= SYMBOLS.length()) {
            throw new ExportException("gtnl.machine.structure_compiler.error.symbols", SYMBOLS.length());
        }
        char symbol = SYMBOLS.charAt(elements.size());
        elements.add(".addElement('" + escapeCharacter(symbol) + "', " + expression + ")");
        return symbol;
    }

    private void addImport(Class<?> type) {
        imports.add(type.getCanonicalName());
    }

    private String createCode(String name) {
        StringBuilder code = new StringBuilder();
        for (String type : imports) code.append("import ")
            .append(type)
            .append(";\n");
        code.append("\nprivate static final int HORIZONTAL_OFFSET = ")
            .append(selection.offsetA())
            .append(";\n")
            .append("private static final int VERTICAL_OFFSET = ")
            .append(selection.offsetB())
            .append(";\n")
            .append("private static final int DEPTH_OFFSET = ")
            .append(selection.offsetC())
            .append(";\n")
            .append("private static final String[][] shape = StructureUtils.readStructureFromFile(\"")
            .append(ScienceNotLeisure.RESOURCE_ROOT_ID)
            .append(":multiblock/")
            .append(name)
            .append("\");\n\n")
            .append(".addShape(STRUCTURE_PIECE_MAIN, ")
            .append(selection.transpose() ? "StructureUtility.transpose(shape)" : "shape")
            .append(")\n");
        for (String element : elements) code.append(element)
            .append('\n');
        return code.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static String escapeCharacter(char value) {
        return value == '\'' ? "\\'" : Character.toString(value);
    }

    public record Selection(ForgeDirection direction, int offsetA, int offsetB, int offsetC, int sizeA, int sizeB,
        int sizeC, boolean transpose) {

        public long volume() {
            return (long) sizeA * sizeB * sizeC;
        }

        public Bounds bounds(int x, int y, int z) {
            ExtendedFacing facing = ExtendedFacing.of(direction);
            int[] first = new int[3];
            int[] last = new int[3];
            facing.getWorldOffset(new int[] { -offsetA, -offsetB, -offsetC }, first);
            facing.getWorldOffset(new int[] { sizeA - 1 - offsetA, sizeB - 1 - offsetB, sizeC - 1 - offsetC }, last);
            return new Bounds(
                x + Math.min(first[0], last[0]),
                y + Math.min(first[1], last[1]),
                z + Math.min(first[2], last[2]),
                x + Math.max(first[0], last[0]),
                y + Math.max(first[1], last[1]),
                z + Math.max(first[2], last[2]));
        }
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    public record ExportResult(String fileName, byte[] data, String code, String report, int elementCount,
        int warningCount) {}

    private record TileKey(Block block, int metadata, Class<?> tileType, int machineId) {}

    public static class ExportException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        public final String translationKey;
        public final Object[] arguments;

        public ExportException(String translationKey, Object... arguments) {
            super(translationKey);
            this.translationKey = translationKey;
            this.arguments = arguments;
        }
    }
}
