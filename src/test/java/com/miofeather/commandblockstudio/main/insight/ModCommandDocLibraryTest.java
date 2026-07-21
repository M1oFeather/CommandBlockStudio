package com.miofeather.commandblockstudio.main.insight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCommandDocLibraryTest {
    @Test
    void recognizesCuratedModRootsWithoutTreatingVanillaAsModDocs() {
        assertTrue(ModCommandDocLibrary.contains("create"));
        assertTrue(ModCommandDocLibrary.contains("/set"));
        assertTrue(ModCommandDocLibrary.contains("securitycraft"));
        assertFalse(ModCommandDocLibrary.contains("tp"));
        assertFalse(ModCommandDocLibrary.contains("give"));
        assertTrue(CommandDocLibrary.isModDocumented("/set"));
        assertFalse(CommandDocLibrary.isModDocumented("minecraft:tp"));
    }
}
