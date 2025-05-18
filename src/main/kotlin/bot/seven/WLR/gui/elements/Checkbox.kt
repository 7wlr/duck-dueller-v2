package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.gui.Gui

class Checkbox(
    id: Int, x: Int, y: Int,
    private val boxSize: Int = 10,
    label: String,
    initialValue: Boolean,
    val onValueChanged: (Boolean) -> Unit
) : GuiComponentBase(id, x, y, boxSize, boxSize, label) {

    var isChecked: Boolean = initialValue
        private set

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val actualHovered = enabled && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height
        this.hovered = actualHovered
        if (!visible) return
        val boxColor = if (enabled) (if (actualHovered) GuiColors.CHECKBOX_BOX_HOVER else GuiColors.CHECKBOX_BOX) else GuiColors.COMPONENT_BACKGROUND_DISABLED
        val checkColor = if (enabled) GuiColors.CHECKBOX_CHECK else GuiColors.DARK_RED

        Gui.drawRect(x, y, x + width, y + height, boxColor)

        Gui.drawRect(x, y, x + width, y + 1, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x, y + height - 1, x + width, y + height, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x, y, x + 1, y + height, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x + width - 1, y, x + width, y + height, GuiColors.COMPONENT_BORDER)
        if (isChecked) {

            val inset = 2
            Gui.drawRect(x + inset, y + inset, x + width - inset, y + height - inset, checkColor)
        }
        drawSideLabel()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (enabled && visible && mouseButton == 0 &&
            mouseX >= this.x && mouseY >= this.y &&
            mouseX < this.x + this.width && mouseY < this.y + this.height) {
            isChecked = !isChecked
            onValueChanged(isChecked)
            mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 0.7F))
            return true
        }
        val labelStartX = x + width + 5
        val labelWidth = fontRenderer.getStringWidth(label)
        val labelHeight = fontRenderer.FONT_HEIGHT
        val labelActualY = y + (height - fontRenderer.FONT_HEIGHT) / 2

        if (enabled && visible && mouseButton == 0 &&
            mouseX >= labelStartX && mouseX < labelStartX + labelWidth &&
            mouseY >= labelActualY && mouseY < labelActualY + labelHeight) {
            isChecked = !isChecked
            onValueChanged(isChecked)
            mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 0.7F))
            return true
        }
        return false
    }
}