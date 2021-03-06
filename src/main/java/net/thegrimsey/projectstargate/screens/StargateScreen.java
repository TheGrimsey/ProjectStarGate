package net.thegrimsey.projectstargate.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.thegrimsey.projectstargate.ProjectStarGate;

public class StargateScreen extends HandledScreen<StargateScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/stargatebase.png");
    float textX, textY;

    public StargateScreen(StargateScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundHeight = 70;
        backgroundWidth = 180;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        textRenderer.draw(matrices, handler.getAddress(), textX, textY, 255);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        super.init();
        // Center the address
        textX = (backgroundWidth - textRenderer.getWidth(handler.getAddress())) / 2f;
        textY = backgroundHeight - textRenderer.fontHeight - 4;
    }
}
