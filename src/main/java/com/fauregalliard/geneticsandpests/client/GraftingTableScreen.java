package com.fauregalliard.geneticsandpests.client;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.content.GraftingTableMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The bench's window: three slots and the player's inventory, no controls to learn. */
public class GraftingTableScreen extends AbstractContainerScreen<GraftingTableMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(GeneticsAndPests.MODID, "textures/gui/grafting_table.png");

    public GraftingTableScreen(GraftingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    /**
     * {@link AbstractContainerScreen#render} does not draw item tooltips in 1.21.11 — every vanilla
     * container screen asks for them itself, and a screen that forgets simply shows nothing when you
     * hover a slot.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BACKGROUND,
                this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }
}
