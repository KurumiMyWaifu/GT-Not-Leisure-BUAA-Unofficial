package com.science.gtnl.utils.structure;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.block.Block;

import com.science.gtnl.ScienceNotLeisure;
import com.science.gtnl.api.casing.GTNLCasings;

import gregtech.api.casing.Casings;
import gregtech.api.casing.ICasing;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class StructureElementRegistry {

    private static final Set<Class<?>> CASING_ENUMS = new LinkedHashSet<>();
    private static final Map<Block, Int2ObjectMap<CasingExpression>> EXPRESSIONS = new IdentityHashMap<>();
    private static boolean initialized;

    static {
        CASING_ENUMS.add(Casings.class);
        CASING_ENUMS.add(GTNLCasings.class);
    }

    private StructureElementRegistry() {}

    public static synchronized <E extends Enum<E> & ICasing> void registerCasingEnum(Class<E> type) {
        if (CASING_ENUMS.add(Objects.requireNonNull(type, "type"))) initialized = false;
    }

    public static synchronized CasingExpression findCasing(Block block, int metadata) {
        if (!initialized) {
            EXPRESSIONS.clear();
            for (Class<?> type : CASING_ENUMS) {
                for (Object constant : type.getEnumConstants()) {
                    ICasing casing = (ICasing) constant;
                    try {
                        if (casing.isTiered()) continue;
                        Block casingBlock = casing.getBlock();
                        int casingMeta = casing.getBlockMeta();
                        if (casingBlock == null || casingMeta < 0) continue;
                        EXPRESSIONS.computeIfAbsent(casingBlock, ignored -> new Int2ObjectOpenHashMap<>())
                            .putIfAbsent(
                                casingMeta,
                                new CasingExpression(
                                    type,
                                    type.getSimpleName() + "." + ((Enum<?>) constant).name() + ".asElement()"));
                    } catch (RuntimeException | LinkageError exception) {
                        ScienceNotLeisure.LOG.debug("Skipping unavailable structure casing: {}", constant, exception);
                    }
                }
            }
            initialized = true;
        }
        Int2ObjectMap<CasingExpression> metadataExpressions = EXPRESSIONS.get(block);
        return metadataExpressions == null ? null : metadataExpressions.get(metadata);
    }

    public record CasingExpression(Class<?> owner, String expression) {}
}
