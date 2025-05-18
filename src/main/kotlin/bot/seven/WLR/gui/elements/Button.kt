package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui

class Button(
    id: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    buttonText: String,
    val onClick: () -> Unit
) : GuiComponentBase(id, x, y, width, height, buttonText) {

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawComponent(mouseX, mouseY, partialTicks)
        if (!visible) return

        val bgColor = when {
            !enabled -> GuiColors.COMPONENT_BACKGROUND_DISABLED
            hovered -> GuiColors.PRIMARY_RED
            else -> GuiColors.COMPONENT_BACKGROUND
        }
        val textColor = if (enabled) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED

        Gui.drawRect(x, y, x + width, y + height, bgColor)

        Gui.drawRect(x, y, x + width, y + 1, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x, y + height - 1, x + width, y + height, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x, y, x + 1, y + height, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x + width - 1, y, x + width, y + height, GuiColors.COMPONENT_BORDER)

        drawCenteredString(Minecraft.getMinecraft().fontRendererObj, label, x + width / 2, y + (height - 8) / 2, textColor)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {

        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            if (mouseButton == 0) {
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 1.0F))
                onClick()
                return true
            }
        }
        return false
    }

    private fun drawCenteredString(fontRenderer: FontRenderer, text: String, x: Int, y: Int, color: Int) {
        fontRenderer.drawStringWithShadow(text, (x - fontRenderer.getStringWidth(text) / 2).toFloat(), y.toFloat(), color)
    }
}