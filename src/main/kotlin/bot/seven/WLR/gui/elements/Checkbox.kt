package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors

class Checkbox(
    id: Int, x: Int, y: Int,
    private val boxSize: Int = MODERN_CHECKBOX_SIZE,
    label: String,
    initialValue: Boolean,
    val onValueChanged: (Boolean) -> Unit
) : GuiComponentBase(id, x, y, boxSize, boxSize, label) {

    var isChecked: Boolean = initialValue
        private set
    private val cornerRadius = 2f
    private val checkmarkInsetRatio = 0.25f

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawComponent(mouseX, mouseY, partialTicks)
        if (!visible) return

        val boxActuallyHovered = this.hovered || (isLabelHovered(mouseX, mouseY) && enabled)

        val currentBgColor: Int
        val currentBorderColor: Int
        val currentCheckColor: Int

        when {
            !enabled -> {
                currentBgColor = GuiColors.COMPONENT_BACKGROUND_DISABLED
                currentBorderColor = GuiColors.MODERN_UI_ELEMENT_BORDER
                currentCheckColor = GuiColors.TEXT_DISABLED
            }
            else -> {
                currentBgColor = if (boxActuallyHovered) GuiColors.CHECKBOX_BOX_HOVER else GuiColors.CHECKBOX_BOX
                currentBorderColor = if (boxActuallyHovered || isChecked) GuiColors.PRIMARY_RED_BRIGHT else GuiColors.MODERN_UI_ELEMENT_BORDER
                currentCheckColor = GuiColors.CHECKBOX_CHECK
            }
        }

        GuiDrawingUtils.drawRoundedRectDropShadow(
            this.x.toFloat(), this.y.toFloat(),
            width.toFloat(), height.toFloat(),
            cornerRadius,
            GuiColors.SUBTLE_SHADOW_COLOR,
            SHADOW_OFFSET_X / 2f, SHADOW_OFFSET_Y / 2f
        )

        GuiDrawingUtils.drawModernRoundedRect(
            this.x.toFloat(), this.y.toFloat(),
            width.toFloat(), height.toFloat(),
            cornerRadius,
            currentBgColor,
            currentBorderColor,
            GuiColors.TRANSPARENT_TEXT_PRIMARY_VERY_LIGHT,
            GuiColors.TRANSPARENT_BLACK_VERY_LIGHT,
            MODERN_BORDER_THICKNESS
        )

        if (isChecked) {
            val inset = (width * checkmarkInsetRatio).toInt().coerceAtLeast(1)
            GuiDrawingUtils.drawRoundedRect(
                (this.x + inset).toFloat(), (this.y + inset).toFloat(),
                (width - 2 * inset).toFloat(), (height - 2 * inset).toFloat(),
                1f,
                currentCheckColor
            )
        }

        drawSideLabel(labelYOffset = (height - fontRenderer.FONT_HEIGHT) / 2 + 1, xOffset = 6)
    }

    private fun isLabelHovered(mouseX: Int, mouseY: Int): Boolean {
        if (label.isEmpty()) return false
        val labelStartX = x + width + 6
        val labelRenderY = y + (height - fontRenderer.FONT_HEIGHT) / 2 + 1
        val labelTextWidth = fontRenderer.getStringWidth(label)
        return mouseX >= labelStartX && mouseX < labelStartX + labelTextWidth &&
                mouseY >= labelRenderY && mouseY < labelRenderY + fontRenderer.FONT_HEIGHT
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (enabled && visible && mouseButton == 0) {
            val boxClicked = mouseX >= this.x && mouseX < this.x + this.width &&
                    mouseY >= this.y && mouseY < this.y + this.height
            val labelClicked = isLabelHovered(mouseX, mouseY)

            if (boxClicked || labelClicked) {
                isChecked = !isChecked
                onValueChanged(isChecked)
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 0.7F))
                return true
            }
        }
        return false
    }
}