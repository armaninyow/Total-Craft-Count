package com.armaninyow.totalcraftcount;

import com.armaninyow.totalcraftcount.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.List;

public class CraftCountOverlay {

	// Container texture: 40x50 PNG
	private static final Identifier CONTAINER_TEXTURE = Identifier.of(
		TotalCraftCount.MOD_ID, "textures/gui/container.png"
	);

	// Container PNG dimensions
	private static final int CONTAINER_W = 40;
	private static final int CONTAINER_H = 50;

	// Slot area within the PNG (24x24): top-left at (8, 18)
	private static final int SLOT_PNG_X = 8;
	private static final int SLOT_PNG_Y = 18;
	private static final int SLOT_SIZE = 24;

	// Item is 16x16, centered inside 24x24 -> 4px padding on each side
	private static final int ITEM_PADDING = 4;

	// Gap between the vanilla GUI right edge and our container's left edge
	private static final int GAP = 2;

	// Vertical offset from the GUI background top edge to our container top
	// Player Inventory (2x2): top edge of background + 6px
	// Crafting Table (3x3): top edge of background + 13px
	private static final int INV_VERT_OFFSET = 6;
	private static final int CRAFT_VERT_OFFSET = 13;

	// White with drop shadow - matches vanilla item stack counter color (ARGB)
	private static final int COUNT_COLOR = 0xFFFFFFFF;

	/**
	 * Renders the total craft count overlay on top of the given handled screen.
	 *
	 * @param context    the draw context
	 * @param screen     the inventory or crafting screen
	 * @param isCrafting true if this is a CraftingScreen (3x3), false for InventoryScreen (2x2)
	 */
	public static void render(DrawContext context, HandledScreen<?> screen, boolean isCrafting) {
		if (!(screen.getScreenHandler() instanceof AbstractCraftingScreenHandler handler)) {
			return;
		}

		// Retrieve the vanilla output (computed server-side, synced to client)
		Slot outputSlot = handler.getOutputSlot();
		ItemStack outputStack = outputSlot.getStack();

		if (outputStack.isEmpty()) {
			return;
		}

		// Determine total possible yield from all input slots.
		// In all standard vanilla recipes, each craft consumes exactly 1 item from each
		// occupied input slot. So the number of possible crafts is min(slot.getCount())
		// across all non-empty input slots.
		List<Slot> inputSlots = handler.getInputSlots();
		int minCrafts = Integer.MAX_VALUE;
		boolean hasInput = false;

		for (Slot slot : inputSlots) {
			ItemStack stack = slot.getStack();
			if (!stack.isEmpty()) {
				hasInput = true;
				minCrafts = Math.min(minCrafts, stack.getCount());
			}
		}

		if (!hasInput || minCrafts == Integer.MAX_VALUE) {
			return;
		}

		int perCraftYield = outputStack.getCount();
		int totalYield = minCrafts * perCraftYield;

		// Visibility rule: only show when total > per-craft output
		// (i.e., more than one batch can be crafted)
		if (totalYield <= perCraftYield) {
			return;
		}

		// ---- Screen geometry ----
		HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
		int screenX = accessor.getX();
		int screenY = accessor.getY();
		int bgWidth = accessor.getBackgroundWidth();

		// Vertical: align container top with the GUI background top edge, then shift down.
		// screenX/screenY is the top-left of the vanilla GUI background texture.
		int vertOffset = isCrafting ? CRAFT_VERT_OFFSET : INV_VERT_OFFSET;
		int containerTop = screenY + vertOffset;

		// Horizontal: right of the vanilla GUI + 2px gap.
		// screenX already reflects any recipe-book shift (HandledScreen re-centers itself).
		int containerLeft = screenX + bgWidth + GAP;

		// ---- Draw container background ----
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			CONTAINER_TEXTURE,
			containerLeft, containerTop,
			0.0f, 0.0f,
			CONTAINER_W, CONTAINER_H,
			CONTAINER_W, CONTAINER_H
		);

		MinecraftClient client = MinecraftClient.getInstance();

		// ---- Draw output item (centered in the 24x24 slot) ----
		int slotAbsX = containerLeft + SLOT_PNG_X;
		int slotAbsY = containerTop + SLOT_PNG_Y;
		int itemX = slotAbsX + ITEM_PADDING;
		int itemY = slotAbsY + ITEM_PADDING;
		context.drawItem(outputStack, itemX, itemY);

		// ---- Draw total count text at bottom-right of the full 24x24 slot area ----
		// We render manually (not via drawStackOverlay) so the text anchors to the
		// 24x24 slot boundary, not the inner 16x16 item boundary.
		String countStr = String.valueOf(totalYield);
		int countW = client.textRenderer.getWidth(countStr);
		int countX = slotAbsX + SLOT_SIZE - countW + 1;
		int countY = slotAbsY + SLOT_SIZE - client.textRenderer.fontHeight + 2;
		context.drawText(client.textRenderer, countStr, countX, countY, COUNT_COLOR, true);
	}
}