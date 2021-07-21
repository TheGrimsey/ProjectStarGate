package net.thegrimsey.projectstargate.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.thegrimsey.projectstargate.ProjectStarGate;
import org.lwjgl.glfw.GLFW;

public class DHDScreen extends HandledScreen<DHDScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(ProjectStarGate.MODID, "textures/gui/dhd_gui.png");

    public DHDScreen(DHDScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 320;
        backgroundHeight = 120;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {


        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                break;

            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                break;

            default:
                break;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    void dial() {
        
    }
}
