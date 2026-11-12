package com.soulinchains.arcaneartifacts.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Only ever loaded on the physical client. */
public final class ClientAccess {
    private ClientAccess() {
    }

    public static @Nullable Level level() {
        return Minecraft.getInstance().level;
    }

    /** Opens the False Mage's Guide (the Pseudomage's book). */
    public static void openFalseMageGuide() {
        Minecraft.getInstance().setScreen(new com.soulinchains.arcaneartifacts.client.notebook.ArcaneNotebookScreen(true));
    }

    /** Opens the Researcher's Secret Notebook, the addon's own book. */
    public static void openNotebook() {
        Minecraft.getInstance().setScreen(new com.soulinchains.arcaneartifacts.client.notebook.ArcaneNotebookScreen());
    }
}
