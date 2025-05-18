package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer

abstract class GuiComponentBase(
    var id: Int,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var label: String = ""
) {
    protected val mc: Minecraft = Minecraft.getMinecraft()
    protected val fontRenderer: FontRenderer = mc.fontRendererObj
    var enabled: Boolean = true
    var visible: Boolean = true
    var hovered: Boolean = false

    open fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!visible) return
        this.hovered = enabled &&
                mouseX >= this.x && mouseY >= this.y &&
                mouseX < this.x + this.width && mouseY < this.y + this.height
    }

    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!enabled || !visible) return false
        return mouseX >= this.x && mouseY >= this.y &&
                mouseX < this.x + this.width && mouseY < this.y + this.height
    }

    open fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {}
    open fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {}
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean { return false }

    protected fun drawSideLabel(labelYOffset: Int = (height - fontRenderer.FONT_HEIGHT) / 2) {
        if (label.isNotEmpty()) {
            val labelColor = if (enabled) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED
            fontRenderer.drawStringWithShadow(label, (x + width + 5).toFloat(), (y + labelYOffset).toFloat(), labelColor)
        }
    }

    protected fun drawTopLabel() {
        if (label.isNotEmpty()) {
            val labelColor = if (enabled) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED
            fontRenderer.drawStringWithShadow(label, x.toFloat(), (y - fontRenderer.FONT_HEIGHT - 2).toFloat(), labelColor)
        }
    }
}