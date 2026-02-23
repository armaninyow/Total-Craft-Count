package com.armaninyow.totalcraftcount;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotalCraftCount implements ClientModInitializer {
	public static final String MOD_ID = "totalcraftcount";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof InventoryScreen inventoryScreen) {
				ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) -> {
					CraftCountOverlay.render(context, inventoryScreen, false);
				});
			} else if (screen instanceof CraftingScreen craftingScreen) {
				ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) -> {
					CraftCountOverlay.render(context, craftingScreen, true);
				});
			}
		});
	}
}