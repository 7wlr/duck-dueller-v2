package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min

class Dropdown(
    id: Int, x: Int, y: Int, width: Int,
    label: String,
    val options: List<String>,
    initialSelectedIndex: Int,
    val onSelectionChanged: (Int, String) -> Unit
) : GuiComponentBase(id, x, y, width, 20, label) {

    var selectedIndex: Int = initialSelectedIndex.coerceIn(0, options.size -1)
        private set
    var isOpen: Boolean = false
    val optionHeight = 16
    val maxDisplayableOptions = 5

    private var scrollYOptions: Float = 0f
    private var maxScrollYOptions: Float = 0f
    private var isDraggingOptionScrollbar: Boolean = false
    private val scrollbarWidth = 6
    private var needsScrollbar: Boolean = false

    private var lastMouseYForScrollDrag: Int = 0

    fun setSelected(index: Int, notify: Boolean = true) {
        val oldIndex = selectedIndex
        selectedIndex = index.coerceIn(0, options.indices.lastOrNull() ?: 0)
        if (notify && oldIndex != selectedIndex && options.isNotEmpty() && selectedIndex < options.size && selectedIndex >= 0) {
            onSelectionChanged(selectedIndex, options[selectedIndex])
        }
    }

    fun getSelectedOption(): String? = if (options.isNotEmpty()) options.getOrNull(selectedIndex) else "No options"

    private fun getDisplayableOptionCount(): Int = min(options.size, maxDisplayableOptions)
    private fun getDropdownListVisibleHeight(): Int = getDisplayableOptionCount() * optionHeight
    private fun getTotalOptionsContentHeight(): Int = options.size * optionHeight

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val mainBoxHovered = enabled && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height
        this.hovered = mainBoxHovered

        if (!visible) return

        val mainBoxColor = when {
            !enabled -> GuiColors.COMPONENT_BACKGROUND_DISABLED
            mainBoxHovered && !isOpen -> GuiColors.COMPONENT_BACKGROUND_HOVER
            else -> GuiColors.COMPONENT_BACKGROUND
        }
        val textColor = if (enabled) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED
        val arrowColor = if (enabled) GuiColors.DROPDOWN_ARROW else GuiColors.TEXT_DISABLED

        Gui.drawRect(x, y, x + width, y + height, mainBoxColor)
        Gui.drawRect(x, y, x + width, y + 1, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x, y + height - 1, x + width, y + height, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x, y, x + 1, y + height, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(x + width - 1, y, x + width, y + height, GuiColors.COMPONENT_BORDER)

        val selectedText = getSelectedOption() ?: "Select..."
        fontRenderer.drawString(selectedText, x + 5, y + (height - fontRenderer.FONT_HEIGHT) / 2 + 1, textColor)

        val arrow = if (isOpen) "▲" else "▼"
        fontRenderer.drawString(arrow, x + width - fontRenderer.getStringWidth(arrow) - 5, y + (height - fontRenderer.FONT_HEIGHT) / 2 + 1, arrowColor)

        drawTopLabel()

        if (isOpen && enabled) {
            val totalContentHeight = getTotalOptionsContentHeight()
            val visibleListHeight = getDropdownListVisibleHeight()
            needsScrollbar = totalContentHeight > visibleListHeight

            val dropdownRenderY = y + height
            val listRenderWidth = if (needsScrollbar) width - scrollbarWidth else width

            Gui.drawRect(x, dropdownRenderY, x + width, dropdownRenderY + visibleListHeight, GuiColors.DROPDOWN_BACKGROUND_OPEN)
            Gui.drawRect(x, dropdownRenderY, x + 1, dropdownRenderY + visibleListHeight, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(x + width - 1, dropdownRenderY, x + width, dropdownRenderY + visibleListHeight, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(x, dropdownRenderY + visibleListHeight - 1, x + width, dropdownRenderY + visibleListHeight, GuiColors.COMPONENT_BORDER)

            val scaledResolution = ScaledResolution(mc)
            val scaleFactor = scaledResolution.scaleFactor

            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(
                (this.x * scaleFactor),
                (scaledResolution.scaledHeight - (dropdownRenderY + visibleListHeight)) * scaleFactor,
                listRenderWidth * scaleFactor,
                visibleListHeight * scaleFactor
            )

            for (i in options.indices) {
                val optionTopYAbsolute = i * optionHeight
                val optionTopYOnScreen = dropdownRenderY + optionTopYAbsolute - scrollYOptions.toInt()

                if (optionTopYOnScreen + optionHeight < dropdownRenderY || optionTopYOnScreen > dropdownRenderY + visibleListHeight) {
                    continue
                }

                val optionTextY = optionTopYOnScreen + (optionHeight - fontRenderer.FONT_HEIGHT) / 2 + 1
                val isOptionHovered = mouseX >= x && mouseX < x + listRenderWidth &&
                        mouseY >= optionTopYOnScreen && mouseY < optionTopYOnScreen + optionHeight &&
                        mouseY >= dropdownRenderY && mouseY < dropdownRenderY + visibleListHeight

                val optionBgColor = when {
                    isOptionHovered -> GuiColors.DROPDOWN_ITEM_HOVER_BG
                    i == selectedIndex -> GuiColors.DROPDOWN_ITEM_SELECTED_BG
                    else -> 0
                }
                if (optionBgColor != 0) {
                    Gui.drawRect(x + 1, optionTopYOnScreen, x + listRenderWidth -1 , optionTopYOnScreen + optionHeight, optionBgColor)
                }
                fontRenderer.drawString(options[i], x + 5, optionTextY, GuiColors.DROPDOWN_ITEM_TEXT)
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST)

            if (needsScrollbar) {
                maxScrollYOptions = max(0f, (totalContentHeight - visibleListHeight).toFloat())
                scrollYOptions = scrollYOptions.coerceIn(0f, maxScrollYOptions)

                val scrollbarActualX = x + width - scrollbarWidth
                Gui.drawRect(scrollbarActualX, dropdownRenderY, scrollbarActualX + scrollbarWidth, dropdownRenderY + visibleListHeight, GuiColors.SCROLLBAR_BG)

                if (maxScrollYOptions > 0) {
                    val thumbHeightRatio = (visibleListHeight.toFloat() / totalContentHeight.toFloat()).coerceIn(0.1f, 1f)
                    val thumbHeight = max(10, (visibleListHeight * thumbHeightRatio).toInt())
                    val thumbY = dropdownRenderY + ((visibleListHeight - thumbHeight) * (scrollYOptions / maxScrollYOptions)).toInt()
                    Gui.drawRect(scrollbarActualX + 1, thumbY, scrollbarActualX + scrollbarWidth - 1, thumbY + thumbHeight, GuiColors.SCROLLBAR_THUMB)
                }
            } else {
                scrollYOptions = 0f
                maxScrollYOptions = 0f
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!enabled || !visible) return false

        if (mouseButton == 0) {
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                isOpen = !isOpen
                if (isOpen) {
                    scrollYOptions = 0f
                    if (options.isNotEmpty() && selectedIndex >= 0 && selectedIndex < options.size) {
                        val selectedOptionTopY = selectedIndex * optionHeight
                        val selectedOptionBottomY = selectedOptionTopY + optionHeight
                        val visibleListHeight = getDropdownListVisibleHeight()
                        if (selectedOptionTopY < scrollYOptions) {
                            scrollYOptions = selectedOptionTopY.toFloat()
                        } else if (selectedOptionBottomY > scrollYOptions + visibleListHeight) {
                            scrollYOptions = (selectedOptionBottomY - visibleListHeight).toFloat()
                        }
                        scrollYOptions = scrollYOptions.coerceIn(0f, maxScrollYOptions)
                    }
                }
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 1.0F))
                return true
            }

            if (isOpen) {
                val dropdownRenderY = y + height
                val visibleListHeight = getDropdownListVisibleHeight()

                if (needsScrollbar) {
                    val scrollbarActualX = x + width - scrollbarWidth
                    if (mouseX >= scrollbarActualX && mouseX < scrollbarActualX + scrollbarWidth &&
                        mouseY >= dropdownRenderY && mouseY < dropdownRenderY + visibleListHeight) {
                        isDraggingOptionScrollbar = true
                        lastMouseYForScrollDrag = mouseY
                        val clickRatio = (mouseY - dropdownRenderY).toFloat() / visibleListHeight.toFloat()
                        scrollYOptions = (maxScrollYOptions * clickRatio).coerceIn(0f, maxScrollYOptions)
                        return true
                    }
                }

                val listRenderWidth = if (needsScrollbar) width - scrollbarWidth else width
                if (mouseX >= x && mouseX < x + listRenderWidth && mouseY >= dropdownRenderY && mouseY < dropdownRenderY + visibleListHeight) {
                    val mouseYInList = mouseY - dropdownRenderY
                    val absoluteMouseYInOptions = mouseYInList + scrollYOptions
                    val clickedOptionIndex = (absoluteMouseYInOptions / optionHeight).toInt()

                    if (clickedOptionIndex >= 0 && clickedOptionIndex < options.size) {
                        setSelected(clickedOptionIndex)
                        isOpen = false
                        mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(net.minecraft.util.ResourceLocation("gui.button.press"), 0.8F))
                        return true
                    }
                }
                if (mouseX >= x && mouseX < x + width && mouseY >= dropdownRenderY && mouseY < dropdownRenderY + visibleListHeight) {
                    return true
                }
            }
        }
        return false
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state == 0) {
            isDraggingOptionScrollbar = false
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (isDraggingOptionScrollbar && clickedMouseButton == 0 && needsScrollbar && maxScrollYOptions > 0) {
            val dropdownRenderY = y + height
            val visibleListHeight = getDropdownListVisibleHeight()
            val totalContentH = getTotalOptionsContentHeight()

            val dy = mouseY - lastMouseYForScrollDrag
            lastMouseYForScrollDrag = mouseY

            val scrollRatio = maxScrollYOptions / (visibleListHeight.toFloat() - (visibleListHeight.toFloat() * visibleListHeight.toFloat() / totalContentH.toFloat()).coerceIn(10f, visibleListHeight.toFloat()))
            if (!scrollRatio.isNaN() && scrollRatio.isFinite() && scrollRatio != 0f) {
                scrollYOptions += dy * scrollRatio
            }
            scrollYOptions = scrollYOptions.coerceIn(0f, maxScrollYOptions)
        }
    }

    fun handleMouseScroll(rawMouseX: Int, rawMouseY: Int, dWheel: Int): Boolean {
        if (isOpen && enabled && visible && needsScrollbar) {
            val dropdownRenderY = y + height
            val visibleListHeight = getDropdownListVisibleHeight()
            if (rawMouseX >= x && rawMouseX < x + width && rawMouseY >= dropdownRenderY && rawMouseY < dropdownRenderY + visibleListHeight) {
                val scrollAmount = if (dWheel > 0) -optionHeight.toFloat() * 1.5f else optionHeight.toFloat() * 1.5f
                scrollYOptions = (scrollYOptions + scrollAmount).coerceIn(0f, maxScrollYOptions)
                return true
            }
        }
        return false
    }

    fun close() {
        if (isOpen) {
            isOpen = false
            isDraggingOptionScrollbar = false
        }
    }
}