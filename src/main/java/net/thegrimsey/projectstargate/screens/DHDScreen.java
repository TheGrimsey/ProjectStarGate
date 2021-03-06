package net.thegrimsey.projectstargate.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.blocks.entity.DHDBlockEntity;
import net.thegrimsey.projectstargate.utils.AddressingUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class DHDScreen extends HandledScreen<DHDScreenHandler> {
    // Textures
    private static final Identifier DHD_TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/dhd_gui.png");
    private static final Identifier CENTER_BUTTON_TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/dhd_centre.png");
    private static final Identifier SYMBOL_TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/symbols.png");

    // Size of a symbol in the texture.
    final static int SYMBOL_TEXTURE_SIZE = 48;

    int buttonX, buttonY, buttonWidth, buttonHeight;

    public DHDScreen(DHDScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 320;
        backgroundHeight = 120;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        DHDBlockEntity dhdBlockEntity = handler.getDHD();
        boolean isActive = dhdBlockEntity.hasGate() && dhdBlockEntity.getGate().isActive();

        // Draw background / main console.
        RenderSystem.setShaderTexture(0, DHD_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);

        // Draw center button
        RenderSystem.setShaderTexture(0, CENTER_BUTTON_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        if(dhdBlockEntity.hasGate() && !dhdBlockEntity.getGate().notMerged())
        {
            if(isActive)
                RenderSystem.setShaderColor(1.0f, 0.5f, 0.0f, 1.0f); // Gate is active.
            else
                RenderSystem.setShaderColor(0.5f, 0.25f, 0.0f, 1.0f); // Gate is idle.
        }
        else
            RenderSystem.setShaderColor(0.2f, 0.2f, 0.2f, 1.0f); // No gate color

        drawTexture(matrices, buttonX, buttonY, buttonWidth, buttonHeight, 64, 0, 64, 64, 128, 64);

        // Draw light up outline if connected.
        if(isActive)
        {
            RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
            int d = 5;
            drawTexture(matrices, buttonX - d, buttonY - d, buttonWidth + 2*d, buttonHeight + d, 0, 0, 64, 64, 128, 64);
            drawTexture(matrices, buttonX - d, buttonY + 11, buttonWidth + 2*d, (int) (0.5 * (buttonHeight + d)), 0, 0, 64, 64, 128, 64);
        }

    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        float symbolScale = 0.5f;

        // Draw written address.
        textRenderer.draw(matrices, handler.text, (width - textRenderer.getWidth(handler.text))/2f, (height + SYMBOL_TEXTURE_SIZE*symbolScale)/2 - textRenderer.fontHeight, Color.WHITE.getRGB());

        // Draw written glyphs.
        RenderSystem.setShaderTexture(0, SYMBOL_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        int drawCount = Math.min(handler.getDHD().getGate().getChevronCount(), handler.writeHead);
        int x = (int) (this.width/2 - drawCount * SYMBOL_TEXTURE_SIZE * symbolScale / 2);
        int y = (this.height - SYMBOL_TEXTURE_SIZE)/2;

        for(int i = 0; i < drawCount; i++)
        {
            int texX = (handler.writtenAddress[i] % 10) * SYMBOL_TEXTURE_SIZE;
            int texY = (handler.writtenAddress[i] / 10) * SYMBOL_TEXTURE_SIZE;
            drawTexture(matrices, (int)(x + SYMBOL_TEXTURE_SIZE*symbolScale*i), y, (int)(SYMBOL_TEXTURE_SIZE*symbolScale), (int)(SYMBOL_TEXTURE_SIZE*symbolScale), texX, texY, SYMBOL_TEXTURE_SIZE, SYMBOL_TEXTURE_SIZE, 512, 256);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        this.drawBackground(matrices, delta, mouseX, mouseY);
        this.drawForeground(matrices, mouseX, mouseY);
    }

    @Override
    protected void init() {
        x = (width - backgroundWidth) / 2;
        y = height - backgroundHeight;

        double rx = this.backgroundWidth * 48 / 512.0;
        double ry = this.backgroundHeight * 48 / 256.0;
        buttonX = (int)(width/2 - rx);
        buttonY = (int)(y + backgroundHeight/2 - ry - 7);
        buttonWidth = (int)(2 * rx);
        buttonHeight = 48;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> handler.eraseGlyph();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> handler.dialGate();
            case GLFW.GLFW_KEY_ESCAPE -> onClose();
            default -> handler.writeGlyph((byte) AddressingUtil.GLYPHS.indexOf(keyCode));
        }

        return true;
    }
}
