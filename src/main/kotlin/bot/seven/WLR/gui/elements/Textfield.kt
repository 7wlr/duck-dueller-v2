package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.gui.GuiTextField

class Textfield(
    id: Int, x: Int, y: Int, width: Int,
    height: Int = MODERN_TEXT_INPUT_HEIGHT,
    label: String,
    initialText: String,
    private val validator: (String) -> Boolean = { true },
    val onTextChanged: (String) -> Unit,
    val onFocusChanged: (Boolean) -> Unit = {}
) : GuiComponentBase(id, x, y, width, height, label) {

    private val horizontalTextPadding = MODERN_ELEMENT_PADDING_X
    private val verticalTextPadding = (this.height - fontRenderer.FONT_HEIGHT) / 2 + 1

    val textField: GuiTextField
    private var lastText: String = initialText
    private var hasInitialFocusCallbackFired = false
    private val cornerRadius = MODERN_CORNER_RADIUS

    init {
        textField = GuiTextField(
            id,
            fontRenderer,
            this.x + horizontalTextPadding,
            this.y + verticalTextPadding,
            this.width - (2 * horizontalTextPadding),
            fontRenderer.FONT_HEIGHT
        )
        textField.text = initialText
        textField.maxStringLength = 256
        textField.enableBackgroundDrawing = false
        textField.setTextColor(GuiColors.TEXTFIELD_TEXT)
        textField.setDisabledTextColour(GuiColors.TEXT_DISABLED)
        textField.isFocused = false
    }

    fun getText(): String = textField.text

    fun setText(newText: String, notify: Boolean = true) {
        val oldText = textField.text
        if (validator(newText)) {
            textField.text = newText
            if (notify && newText != oldText) {
                onTextChanged(newText)
            }
            lastText = newText
        }
    }

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawComponent(mouseX, mouseY, partialTicks)
        if (!visible) return

        textField.setEnabled(this.enabled)

        textField.xPosition = this.x + horizontalTextPadding
        textField.yPosition = this.y + verticalTextPadding
        textField.width = this.width - (2 * horizontalTextPadding)

        val currentBgColor: Int
        val currentOuterBorderColor: Int
        val innerShadowEffect = GuiColors.TRANSPARENT_BLACK_LIGHT
        val innerHighlightEffect = GuiColors.TRANSPARENT_TEXT_PRIMARY_VERY_LIGHT

        when {
            !enabled -> {
                currentBgColor = GuiColors.COMPONENT_BACKGROUND_DISABLED
                currentOuterBorderColor = GuiColors.MODERN_UI_ELEMENT_BORDER
            }
            textField.isFocused -> {
                currentBgColor = GuiColors.TEXTFIELD_BACKGROUND
                currentOuterBorderColor = GuiColors.TEXTFIELD_BORDER_FOCUSED
            }
            else -> {
                currentBgColor = GuiColors.TEXTFIELD_BACKGROUND
                currentOuterBorderColor = GuiColors.TEXTFIELD_BORDER
            }
        }

        if (textField.isFocused && enabled) {
            GuiDrawingUtils.drawRoundedRectDropShadow(
                x.toFloat(), y.toFloat(),
                width.toFloat(), height.toFloat(),
                cornerRadius,
                GuiColors.PRIMARY_RED_BRIGHT_GLOW_EFFECT,
                0f, SHADOW_OFFSET_Y, 1f
            )
        }

        GuiDrawingUtils.drawModernRoundedRect(
            x.toFloat(), y.toFloat(),
            width.toFloat(), height.toFloat(),
            cornerRadius,
            currentBgColor,
            currentOuterBorderColor,
            if (textField.isFocused || !enabled) 0 else innerHighlightEffect,
            if (textField.isFocused || !enabled) 0 else innerShadowEffect,
            MODERN_BORDER_THICKNESS
        )

        textField.setTextColor(if (enabled) GuiColors.TEXTFIELD_TEXT else GuiColors.TEXT_DISABLED)
        textField.drawTextBox()

        drawTopLabel(yOffset = -3)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!visible) {
            if (textField.isFocused) setFocused(false)
            return false
        }

        val wasFocused = textField.isFocused
        var clickedOnThisComponent = false

        if (mouseX >= this.x && mouseX < this.x + this.width &&
            mouseY >= this.y && mouseY < this.y + this.height) {
            clickedOnThisComponent = true
            if (enabled) {
                textField.mouseClicked(mouseX, mouseY, mouseButton)
            } else {
                if (textField.isFocused) setFocused(false)
            }
        } else {
            if (textField.isFocused) {
                setFocused(false)
            }
        }

        if (enabled && textField.isFocused != wasFocused) {
            onFocusChanged(textField.isFocused)
        }

        return enabled && clickedOnThisComponent && textField.isFocused
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!enabled || !visible || !textField.isFocused) return false

        val previousText = textField.text
        val prevCursorPos = textField.cursorPosition
        val prevSelectionEnd = textField.selectionEnd

        val handledByVanillaField = textField.textboxKeyTyped(typedChar, keyCode)

        if (handledByVanillaField) {
            if (textField.text != previousText) {
                if (validator(textField.text)) {
                    onTextChanged(textField.text)
                    lastText = textField.text
                } else {
                    textField.text = previousText
                    textField.setCursorPosition(prevCursorPos)
                    textField.setSelectionPos(prevSelectionEnd)
                }
            }
        }
        return handledByVanillaField
    }

    fun setFocused(isFocused: Boolean) {
        if (!enabled && isFocused) return

        val oldFocusState = textField.isFocused
        textField.isFocused = isFocused

        if (oldFocusState != isFocused || !hasInitialFocusCallbackFired) {
            onFocusChanged(isFocused)
            hasInitialFocusCallbackFired = true
        }
    }

    fun unfocusIfNeeded() {
        if (textField.isFocused) {
            setFocused(false)
        }
    }
}