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
}
