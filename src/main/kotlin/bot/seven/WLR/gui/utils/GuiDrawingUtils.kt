package bot.seven.WLR.gui.elements

import net.minecraft.client.gui.Gui
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GuiDrawingUtils {

    private enum class CornerType { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Int) {
        val r = min(min(width, height) / 2f, max(0f, radius))

        if (r == 0f) {
            Gui.drawRect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), color)
            return
        }
        Gui.drawRect((x + r).roundToInt(), y.roundToInt(), (x + width - r).roundToInt(), (y + height).roundToInt(), color)
        Gui.drawRect(x.roundToInt(), (y + r).roundToInt(), (x + r).roundToInt(), (y + height - r).roundToInt(), color)
        Gui.drawRect((x + width - r).roundToInt(), (y + r).roundToInt(), (x + width).roundToInt(), (y + height - r).roundToInt(), color)

        drawFilledQuarterCircle(x, y, r, color, CornerType.TOP_LEFT)
        drawFilledQuarterCircle(x + width - r, y, r, color, CornerType.TOP_RIGHT)
        drawFilledQuarterCircle(x, y + height - r, r, color, CornerType.BOTTOM_LEFT)
        drawFilledQuarterCircle(x + width - r, y + height - r, r, color, CornerType.BOTTOM_RIGHT)
    }


    private fun drawFilledQuarterCircle(cornerX: Float, cornerY: Float, radius: Float, color: Int, type: CornerType) {
        if (radius <= 0f) return
        val rSq = radius * radius
        val rInt = radius.roundToInt()

        for (dxRel in 0 until rInt) {
            for (dyRel in 0 until rInt) {
                val currentPixelX = (cornerX + dxRel).roundToInt()
                val currentPixelY = (cornerY + dyRel).roundToInt()
                val effX: Float; val effY: Float
                when (type) {
                    CornerType.TOP_LEFT -> { effX = (dxRel + 0.5f) - radius; effY = (dyRel + 0.5f) - radius }
                    CornerType.TOP_RIGHT -> { effX = (dxRel + 0.5f); effY = (dyRel + 0.5f) - radius }
                    CornerType.BOTTOM_LEFT -> { effX = (dxRel + 0.5f) - radius; effY = (dyRel + 0.5f) }
                    CornerType.BOTTOM_RIGHT -> { effX = (dxRel + 0.5f); effY = (dyRel + 0.5f) }
                }
                if (effX * effX + effY * effY <= rSq) {
                    Gui.drawRect(currentPixelX, currentPixelY, currentPixelX + 1, currentPixelY + 1, color)
                }
            }
        }
    }

    fun drawRoundedRectWithBorder(
        x: Float, y: Float, width: Float, height: Float,
        radius: Float, bgColor: Int, borderColor: Int, borderThickness: Float
    ) {
        if (borderThickness <= 0f) {
            drawRoundedRect(x, y, width, height, radius, bgColor)
            return
        }
        drawRoundedRect(x, y, width, height, radius, borderColor)
        drawRoundedRect(
            x + borderThickness, y + borderThickness,
            width - 2 * borderThickness, height - 2 * borderThickness,
            max(0f, radius - borderThickness),
            bgColor
        )
    }

    fun drawModernRoundedRect(
        x: Float, y: Float, width: Float, height: Float, radius: Float,
        bgColor: Int,
        outerBorderColor: Int,
        innerShadowColor: Int,
        innerHighlightColor: Int,
        borderThickness: Float = 1f
    ) {
        drawRoundedRect(x, y, width, height, radius, outerBorderColor)
        val bgX = x + borderThickness; val bgY = y + borderThickness
        val bgWidth = width - 2 * borderThickness; val bgHeight = height - 2 * borderThickness
        val bgRadius = max(0f, radius - borderThickness)
        drawRoundedRect(bgX, bgY, bgWidth, bgHeight, bgRadius, bgColor)

        if (innerShadowColor != 0 && bgWidth > 0 && bgHeight > 0) {
            val shadowOffset = 0.5f
            drawRoundedRect(bgX, bgY, bgWidth, shadowOffset, bgRadius, innerShadowColor)
            drawRoundedRect(bgX, bgY + shadowOffset, shadowOffset, bgHeight - shadowOffset, bgRadius, innerShadowColor)
        }
        if (innerHighlightColor != 0 && bgWidth > 0 && bgHeight > 0) {
            val highlightOffset = 0.5f
            drawRoundedRect(bgX, bgY + bgHeight - highlightOffset, bgWidth, highlightOffset, bgRadius, innerHighlightColor)
            drawRoundedRect(bgX + bgWidth - highlightOffset, bgY, highlightOffset, bgHeight - highlightOffset, bgRadius, innerHighlightColor)
        }
    }

    fun drawRoundedRectDropShadow(
        x: Float, y: Float, width: Float, height: Float, radius: Float,
        shadowColor: Int, shadowOffsetX: Float, shadowOffsetY: Float, shadowRadiusPadding: Float = 0f
    ) {
        if (shadowColor != 0) {
            drawRoundedRect(
                x + shadowOffsetX,
                y + shadowOffsetY,
                width,
                height,
                radius + shadowRadiusPadding,
                shadowColor
            )
        }
    }

    fun drawCircle(centerX: Float, centerY: Float, radius: Float, color: Int) {
        if (radius <= 0f) return
        val rSq = radius * radius
        val rInt = radius.roundToInt()
        val startX = (centerX - radius).roundToInt(); val startY = (centerY - radius).roundToInt()
        val endX = (centerX + radius).roundToInt(); val endY = (centerY + radius).roundToInt()

        for (px in startX until endX) {
            for (py in startY until endY) {
                val dx = (px + 0.5f) - centerX; val dy = (py + 0.5f) - centerY
                if (dx * dx + dy * dy <= rSq) {
                    Gui.drawRect(px, py, px + 1, py + 1, color)
                }
            }
        }
    }
}