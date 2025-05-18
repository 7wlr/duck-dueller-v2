package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.gui.Gui
import kotlin.math.abs
import kotlin.math.roundToInt

class Slider(
    id: Int, x: Int, y: Int, width: Int,
    label: String,
    initialValue: Float,
    private val minValue: Float,
    private val maxValue: Float,
    private val step: Float = 0.1f,
    private val displayFormat: (Float) -> String = { "%.2f".format(it) },
    val onValueChanged: (Float) -> Unit
) : GuiComponentBase(id, x, y, width, 14, label) {

    private var currentValue: Float = initialValue
    private var valueOnDragStart: Float = initialValue

    private var isDragging: Boolean = false
    private val sliderKnobWidth = 6
    private val sliderKnobHeight = height
    private val sliderTrackHeight = 2

    private var visualKnobX: Float
    private var targetKnobX: Float
    private val KNOB_SMOOTH_FACTOR = 0.35f

    init {
        internalSetValue(initialValue, false)
        visualKnobX = calculateRawKnobX(this.currentValue)
        targetKnobX = visualKnobX
    }

    private fun calculateRawKnobX(value: Float): Float {
        val progress = if (maxValue - minValue == 0f) 0f else (value - minValue) / (maxValue - minValue)
        return this.x + ((this.width - sliderKnobWidth) * progress)
    }

    private fun internalSetValue(newValue: Float, notify: Boolean) {
        val oldValue = this.currentValue
        var tempValue = newValue.coerceIn(minValue, maxValue)

        if (step > 0) {
            tempValue = (tempValue / step).roundToInt() * step
            tempValue = String.format("%.${getDecimalPlaces(step)}f", tempValue).toFloat()
        }
        this.currentValue = tempValue.coerceIn(minValue, maxValue)
        this.targetKnobX = calculateRawKnobX(this.currentValue)

        if (notify && (oldValue != this.currentValue || String.format("%.5f", oldValue) != String.format("%.5f", this.currentValue))) {
            onValueChanged(this.currentValue)
        }
    }

    fun setValue(newValue: Float) {
        internalSetValue(newValue, true)
        valueOnDragStart = currentValue
        visualKnobX = targetKnobX
    }

    private fun getDecimalPlaces(value: Float): Int {
        val s = value.toString()
        val dotIndex = s.indexOf('.')
        return if (dotIndex < 0) 0 else s.length - dotIndex - 1
    }

    fun getCurrentValue(): Float = currentValue

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawComponent(mouseX, mouseY, partialTicks)
        if (!visible) return

        val diff = targetKnobX - visualKnobX
        if (abs(diff) > 0.01f) {
            visualKnobX += diff * KNOB_SMOOTH_FACTOR
            if (abs(targetKnobX - visualKnobX) < 0.01f) {
                visualKnobX = targetKnobX
            }
        } else {
            visualKnobX = targetKnobX
        }
        val currentKnobRenderX = visualKnobX.roundToInt()

        val trackColor = if (enabled) GuiColors.SLIDER_TRACK else GuiColors.COMPONENT_BACKGROUND_DISABLED
        val knobColor = when {
            !enabled -> GuiColors.TEXT_DISABLED
            isDragging -> GuiColors.LIGHT_RED
            this.hovered -> GuiColors.SLIDER_KNOB_HOVER
            else -> GuiColors.SLIDER_KNOB
        }
        val filledTrackColor = if (enabled) GuiColors.SLIDER_TRACK_FILLED else GuiColors.PRIMARY_RED_DARK

        val valueText = displayFormat(currentValue)
        val labelText = label
        val labelColor = if (enabled) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED
        val valueColor = if (enabled) GuiColors.TEXT_ACCENT else GuiColors.TEXT_DISABLED

        fontRenderer.drawStringWithShadow(labelText, x.toFloat(), (y - fontRenderer.FONT_HEIGHT - 3).toFloat(), labelColor)
        val valueTextWidth = fontRenderer.getStringWidth(valueText)
        fontRenderer.drawStringWithShadow(valueText, (x + width - valueTextWidth).toFloat(), (y - fontRenderer.FONT_HEIGHT - 3).toFloat(), valueColor)

        val trackY = this.y + (this.height - sliderTrackHeight) / 2
        Gui.drawRect(this.x, trackY, this.x + this.width, trackY + sliderTrackHeight, trackColor)
        Gui.drawRect(this.x, trackY, currentKnobRenderX + sliderKnobWidth / 2, trackY + sliderTrackHeight, filledTrackColor)
        Gui.drawRect(currentKnobRenderX, this.y, currentKnobRenderX + sliderKnobWidth, this.y + this.sliderKnobHeight, knobColor)

        val knobBorderColor = GuiColors.COMPONENT_BORDER
        if (isDragging || this.hovered) {
            Gui.drawRect(currentKnobRenderX, this.y, currentKnobRenderX + sliderKnobWidth, this.y + 1, knobBorderColor)
            Gui.drawRect(currentKnobRenderX, this.y + this.sliderKnobHeight - 1, currentKnobRenderX + sliderKnobWidth, this.y + this.sliderKnobHeight, knobBorderColor)
            Gui.drawRect(currentKnobRenderX, this.y, currentKnobRenderX + 1, this.y + this.sliderKnobHeight, knobBorderColor)
            Gui.drawRect(currentKnobRenderX + sliderKnobWidth - 1, this.y, currentKnobRenderX + sliderKnobWidth, this.y + this.sliderKnobHeight, knobBorderColor)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (enabled && visible && mouseButton == 0 && super.mouseClicked(mouseX, mouseY, mouseButton)) {
            isDragging = true
            valueOnDragStart = currentValue
            updateValueFromMouse(mouseX, false)
            visualKnobX = targetKnobX

            mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 0.6F))
            return true
        }
        return false
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (isDragging && clickedMouseButton == 0 && enabled) {
            updateValueFromMouse(mouseX, false)
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state == 0 && isDragging) {
            isDragging = false
            visualKnobX = targetKnobX

            if (currentValue != valueOnDragStart || String.format("%.5f", currentValue) != String.format("%.5f", valueOnDragStart) ) {
                onValueChanged(currentValue)
            }
        }
    }

    private fun updateValueFromMouse(mouseX: Int, notify: Boolean) {
        if (!enabled) return
        val posRatio = (mouseX - (this.x + sliderKnobWidth / 2f)) / (this.width - sliderKnobWidth).toFloat()
        val newValue = minValue + (maxValue - minValue) * posRatio.coerceIn(0f, 1f)
        internalSetValue(newValue, notify)
    }
}