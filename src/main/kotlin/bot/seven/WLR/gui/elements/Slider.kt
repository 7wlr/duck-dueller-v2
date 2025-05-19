package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.roundToInt

class Slider(
    id: Int,
    x: Int,
    y: Int,
    width: Int,
    height: Int = MODERN_SLIDER_HEIGHT,
    label: String,
    initialValue: Float,
    private val minValue: Float,
    private val maxValue: Float,
    private val step: Float = 0.1f,
    private val displayFormat: (Float) -> String = { "%.2f".format(it) },
    val onValueChanged: (Float) -> Unit
) : GuiComponentBase(id, x, y, width, height, label) {

    private var currentValue: Float = initialValue
    private var valueOnDragStart: Float = initialValue
    private var isDragging: Boolean = false

    private val knobVisualRadius: Float = MODERN_SLIDER_KNOB_RADIUS
    private val trackHeightToUse: Float = MODERN_SLIDER_TRACK_HEIGHT
    private val trackCornerRadius = trackHeightToUse / 2f

    private var visualKnobCenterX: Float
    private var targetKnobCenterX: Float
    private val KNOB_SMOOTH_FACTOR = 0.25f

    companion object {
        private val KNOB_TEXTURE = ResourceLocation("wlr", "textures/gui/white_knob.png")
    }

    init {
        internalSetValue(initialValue, false)
        targetKnobCenterX = calculateKnobCenterX(this.currentValue)
        visualKnobCenterX = targetKnobCenterX
    }

    private fun calculateKnobCenterX(value: Float): Float {
        val progress = if (maxValue - minValue == 0f) 0f else (value - minValue) / (maxValue - minValue)
        val actualKnobRadius = if (knobVisualRadius > 0f) knobVisualRadius else 0.1f
        val travelWidth = this.width - (2 * actualKnobRadius)
        return this.x + actualKnobRadius + (if (travelWidth > 0) travelWidth * progress else 0f)
    }

    private fun getKnobClickableRadius(): Float = (if (knobVisualRadius > 0f) knobVisualRadius else 2f) + 3f

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawComponent(mouseX, mouseY, partialTicks)
        if (!visible) return

        val diff = targetKnobCenterX - visualKnobCenterX
        visualKnobCenterX = if (abs(diff) > 0.001f) visualKnobCenterX + diff * KNOB_SMOOTH_FACTOR else targetKnobCenterX

        val actualKnobRadius = if (knobVisualRadius > 0f) knobVisualRadius else 0.1f
        val currentKnobRenderCenterX = visualKnobCenterX.coerceIn(x + actualKnobRadius, x + width - actualKnobRadius)
        val knobRenderY = y + height / 2f

        drawTopLabel(yOffset = -3)
        val valueText = displayFormat(currentValue)
        val valueColor = if (enabled) GuiColors.TEXT_ACCENT else GuiColors.TEXT_DISABLED
        val valueTextWidth = fontRenderer.getStringWidth(valueText)
        fontRenderer.drawString(
            valueText,
            x + width - valueTextWidth,
            y - fontRenderer.FONT_HEIGHT - 7,
            valueColor
        )

        val trackActualY = y + (height - trackHeightToUse) / 2f

        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        GlStateManager.disableAlpha()
        GlStateManager.disableDepth()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        val trackColorToUse = if (enabled) GuiColors.SLIDER_TRACK else GuiColors.COMPONENT_BACKGROUND_DISABLED
        GuiDrawingUtils.drawRoundedRect(
            x.toFloat(), trackActualY, width.toFloat(), trackHeightToUse,
            trackCornerRadius, trackColorToUse
        )

        val filledWidth = currentKnobRenderCenterX - x
        if (filledWidth > 0f) {
            val filledTrackColorToUse = if (enabled) GuiColors.SLIDER_TRACK_FILLED else GuiColors.PRIMARY_RED_DARK
            GuiDrawingUtils.drawRoundedRect(
                x.toFloat(), trackActualY, filledWidth.coerceAtMost(width.toFloat()), trackHeightToUse,
                trackCornerRadius, filledTrackColorToUse
            )
        }
        GlStateManager.popMatrix()

        if (knobVisualRadius <= 0f) return
        val knobDiameter = (knobVisualRadius * 2).toInt()
        if (knobDiameter <= 0) return

        val knobCenterX = currentKnobRenderCenterX
        val knobCenterY = knobRenderY

        val isHoveringKnob = mouseX >= knobCenterX - knobVisualRadius &&
                mouseX <= knobCenterX + knobVisualRadius &&
                mouseY >= knobCenterY - knobVisualRadius &&
                mouseY <= knobCenterY + knobVisualRadius

        val scale = 0.85f

        GlStateManager.pushMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

        mc.textureManager.bindTexture(KNOB_TEXTURE)

        if (isHoveringKnob) {
            GlStateManager.color(1.0f, 0.2f, 0.2f, 1.0f)
        } else {
            GlStateManager.color(1.0f, 0.0f, 0.0f, 1.0f)
        }

        GlStateManager.translate(knobCenterX, knobCenterY, 0f)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.translate(-knobDiameter / 2f, -knobDiameter / 2f, 0f)

        try {
            Gui.drawModalRectWithCustomSizedTexture(
                0, 0,
                0f, 0f,
                knobDiameter, knobDiameter,
                14f, 14f
            )
        } catch (_: Throwable) { }

        GlStateManager.popMatrix()
    }

    private fun internalSetValue(newValue: Float, notify: Boolean) {
        val oldValue = this.currentValue
        var tempValue = newValue.coerceIn(minValue, maxValue)
        if (step > 0f) {
            val decimalPlaces = getDecimalPlaces(step)
            tempValue = (tempValue / step).roundToInt() * step
            tempValue = String.format("%.${decimalPlaces}f", tempValue).replace(',', '.').toFloat()
        }
        this.currentValue = tempValue.coerceIn(minValue, maxValue)
        this.targetKnobCenterX = calculateKnobCenterX(this.currentValue)
        if (notify && abs(oldValue - this.currentValue) > (step / 2.0f).coerceAtMost(0.00001f)) {
            onValueChanged(this.currentValue)
        }
    }

    fun setValue(newValue: Float) {
        internalSetValue(newValue, true)
        valueOnDragStart = currentValue
        visualKnobCenterX = calculateKnobCenterX(this.currentValue)
        targetKnobCenterX = visualKnobCenterX
    }

    private fun getDecimalPlaces(value: Float): Int {
        val s = value.toString().replace(',', '.')
        val dot = s.indexOf('.')
        return if (dot < 0) 0 else s.length - dot - 1
    }

    fun getCurrentValue(): Float = currentValue

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        val wasClickedInBounds = super.mouseClicked(mouseX, mouseY, mouseButton)
        if (!wasClickedInBounds) return false
        if (mouseButton == 0) {
            val knobRenderY = this.y + this.height / 2f
            val dx = mouseX - visualKnobCenterX
            val dy = mouseY - knobRenderY
            val clickableRadius = getKnobClickableRadius()
            val clickedKnob = dx * dx + dy * dy <= clickableRadius * clickableRadius
            val trackActualY = this.y + (this.height - trackHeightToUse) / 2f
            val clickedTrack = mouseX >= this.x && mouseX < this.x + this.width &&
                    mouseY >= trackActualY && mouseY < trackActualY + trackHeightToUse
            if (clickedKnob || clickedTrack) {
                isDragging = true; valueOnDragStart = currentValue
                updateValueFromMouse(mouseX, true)
                visualKnobCenterX = calculateKnobCenterX(this.currentValue)
                targetKnobCenterX = visualKnobCenterX
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 0.6F))
                return true
            }
        }
        return false
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (isDragging && clickedMouseButton == 0 && enabled) {
            updateValueFromMouse(mouseX, true)
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state == 0 && isDragging) {
            isDragging = false
        }
    }

    private fun updateValueFromMouse(mouseX: Int, notify: Boolean) {
        if (!enabled) return
        val actualKnobRadius = if (knobVisualRadius > 0f) knobVisualRadius else 0.1f
        val travelWidth = this.width - (2 * actualKnobRadius)
        if (travelWidth <= 0f) return
        val relativeMouseX = mouseX - (this.x + actualKnobRadius)
        val ratio = (relativeMouseX / travelWidth).coerceIn(0f, 1f)
        val newValue = minValue + (maxValue - minValue) * ratio
        internalSetValue(newValue, notify)
    }
}