package com.miofeather.commandblockstudio.main;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CommandBlockWorkspace {
    private static final int MAX_PINNED_BLOCKS = 16;

    private static Level activeLevel;
    private static final Set<BlockPos> PINNED = new LinkedHashSet<>();

    private CommandBlockWorkspace() {
    }

    public static synchronized boolean isPinned(Level level, BlockPos position) {
        ensureLevel(level);
        return PINNED.contains(position);
    }

    public static synchronized boolean togglePinned(Level level, BlockPos position) {
        ensureLevel(level);
        BlockPos immutable = position.immutable();
        if (PINNED.remove(immutable)) {
            return false;
        }
        if (PINNED.size() >= MAX_PINNED_BLOCKS) {
            BlockPos oldest = PINNED.iterator().next();
            PINNED.remove(oldest);
        }
        PINNED.add(immutable);
        return true;
    }

    public static synchronized List<BlockPos> pinned(Level level) {
        ensureLevel(level);
        return new ArrayList<>(PINNED);
    }

    private static void ensureLevel(Level level) {
        if (activeLevel != level) {
            activeLevel = level;
            PINNED.clear();
        }
    }
}
