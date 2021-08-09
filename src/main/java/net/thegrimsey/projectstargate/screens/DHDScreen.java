package net.thegrimsey.projectstargate.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.thegrimsey.projectstargate.ProjectStarGate;
import net.thegrimsey.projectstargate.utils.AddressingUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class DHDScreen extends HandledScreen<DHDScreenHandler> {
    private static final Identifier DHD_TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/dhd_gui.png");
    private static final Identifier CENTER_BUTTON_TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/dhd_centre.png");

    public DHDScreen(DHDScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 320;
        backgroundHeight = 120;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {

        // Draw background / main console.
        RenderSystem.setShaderTexture(0, DHD_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);

        // Draw center button
        RenderSystem.setShaderTexture(0, CENTER_BUTTON_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.2f, 0.2f, 0.2f, 1.0f);

        double rx = this.backgroundWidth * 48 / 512.0;
        double ry = this.backgroundHeight * 48 / 256.0;

        drawTexture(matrices, (int)(width/2 - rx), (int)(y + backgroundHeight/2 - ry - 7), (int)(2 * rx), 48, 64, 0, 64, 64, 128, 64);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        this.drawBackground(matrices, delta, mouseX, mouseY);
    }

    @Override
    protected void init() {
        x = (width - backgroundWidth) / 2;
        y = height - backgroundHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                handler.eraseGlyph();
                return true;

            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                handler.dialGate();
                return true;

            case GLFW.GLFW_KEY_ESCAPE:
                onClose();
                return true;

            default:
                handler.dialGlyph((byte) AddressingUtil.GLYPHS.indexOf(keyCode));
                return true;
        }
    }
}
