package com.soulinchains.arcaneartifacts.client.notebook;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** One chapter of the Researcher's Secret Notebook: shown in the index, opened as a series of pages. */
public record NotebookEntry(Component title, ItemStack icon, List<NotebookPage> pages) {
}
