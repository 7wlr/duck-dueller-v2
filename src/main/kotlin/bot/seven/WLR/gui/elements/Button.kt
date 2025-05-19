package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer

class Button(
    id: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int = MODERN_BUTTON_HEIGHT,
    buttonText: String,
    val onClick: () -> Unit
) : GuiComponentBase(id, x, y, width, height, buttonText) {

    private val cornerRadius = MODERN_CORNER_RADIUS

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawComponent(mouseX, mouseY, partialTicks)
        if (!visible) return

        val actualDrawableHeight = this.height

        val currentBgColor: Int
        val currentTextColor: Int
        val currentBorderColor: Int
        var glowColor = 0

        when {
            !enabled -> {
                currentBgColor = GuiColors.COMPONENT_BACKGROUND_DISABLED
                currentTextColor = GuiColors.TEXT_DISABLED
                currentBorderColor = GuiColors.MODERN_UI_ELEMENT_BORDER
            }
            hovered -> {
                currentBgColor = GuiColors.BUTTON_MODERN_BACKGROUND_HOVER
                currentTextColor = GuiColors.BUTTON_MODERN_TEXT
                currentBorderColor = GuiColors.PRIMARY_RED_DARK
                glowColor = GuiColors.PRIMARY_RED_BRIGHT_GLOW_EFFECT
            }
            else -> {
                currentBgColor = GuiColors.BUTTON_MODERN_BACKGROUND
                currentTextColor = GuiColors.BUTTON_MODERN_TEXT
                currentBorderColor = GuiColors.PRIMARY_RED_DARK
            }
        }

        if (glowColor != 0) {
            GuiDrawingUtils.drawRoundedRect(
                x.toFloat() - 1f, y.toFloat() - 1f,
                width.toFloat() + 2f, actualDrawableHeight.toFloat() + 2f,
                cornerRadius + 1f,
                glowColor
            )
        }

        GuiDrawingUtils.drawModernRoundedRect(
            x.toFloat(), y.toFloat(),
            width.toFloat(), actualDrawableHeight.toFloat(),
            cornerRadius,
            currentBgColor,
            currentBorderColor,
            GuiColors.TRANSPARENT_RED_VERY_LIGHT_HIGHLIGHT,
            GuiColors.TRANSPARENT_BLACK_VERY_LIGHT,
            MODERN_BORDER_THICKNESS
        )

        val textY = y + (actualDrawableHeight - fontRenderer.FONT_HEIGHT) / 2 + 1
        drawCenteredString(
            Minecraft.getMinecraft().fontRendererObj,
            label,
            x + width / 2,
            textY,
            currentTextColor
        )
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (enabled && visible && mouseButton == 0 &&
            mouseX >= this.x && mouseY >= this.y &&
            mouseX < this.x + this.width && mouseY < this.y + this.height) {
            mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 1.0F))
            onClick()
            return true
        }
        return false
    }

    private fun drawCenteredString(fontRenderer: FontRenderer, text: String, x: Int, y: Int, color: Int) {
        fontRenderer.drawString(text, x - fontRenderer.getStringWidth(text) / 2, y, color)
    }
}