package bot.seven.WLR.gui.elements

import bot.seven.WLR.gui.GuiColors
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiTextField

class Textfield(
    id: Int, x: Int, y: Int, width: Int, height: Int,
    label: String,
    initialText: String,
    private val validator: (String) -> Boolean = { true },
    val onTextChanged: (String) -> Unit,
    val onFocusChanged: (Boolean) -> Unit = {}
) : GuiComponentBase(id, x, y, width, height, label) {

    private val horizontalTextPadding = 5
    private val verticalTextPadding = (height - fontRenderer.FONT_HEIGHT) / 2

    val textField: GuiTextField

    private var lastText: String = initialText
    private var hasInitialFocusCallbackFired = false

    init {
        textField = GuiTextField(
            id,
            fontRenderer,
            this.x + horizontalTextPadding,
            this.y + verticalTextPadding,
            this.width - (2 * horizontalTextPadding),
            this.height - (2 * verticalTextPadding)
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
        if (validator(newText)) {
            val oldText = textField.text
            textField.text = newText
            if (notify && newText != oldText) {
                onTextChanged(newText)
                lastText = newText
            } else if (newText == oldText && newText != lastText) {
                lastText = newText
            }
        }
    }

    override fun drawComponent(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (!visible) return

        textField.setEnabled(this.enabled)

        textField.xPosition = this.x + horizontalTextPadding
        textField.yPosition = this.y + verticalTextPadding

        val backgroundColor = if (enabled) GuiColors.TEXTFIELD_BACKGROUND else GuiColors.COMPONENT_BACKGROUND_DISABLED
        Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor)

        val borderColor = when {
            !enabled -> GuiColors.COMPONENT_BORDER
            textField.isFocused -> GuiColors.TEXTFIELD_BORDER_FOCUSED
            else -> GuiColors.TEXTFIELD_BORDER
        }
        Gui.drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y, borderColor)
        Gui.drawRect(this.x - 1, this.y + this.height, this.x + this.width + 1, this.y + this.height + 1, borderColor)
        Gui.drawRect(this.x - 1, this.y, this.x, this.y + this.height, borderColor)
        Gui.drawRect(this.x + this.width, this.y, this.x + this.width + 1, this.y + this.height, borderColor)

        textField.drawTextBox()

        drawTopLabel()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!enabled || !visible) {
            if (textField.isFocused) {
                setFocused(false)
            }
            return false
        }

        val wasFocused = textField.isFocused

        val clickedOnComponent = mouseX >= this.x && mouseX < this.x + this.width &&
                mouseY >= this.y && mouseY < this.y + this.height

        if (clickedOnComponent) {
            textField.mouseClicked(mouseX, mouseY, mouseButton)
        } else {
            if (textField.isFocused) {
                setFocused(false)
            }
        }

        if (textField.isFocused != wasFocused) {
            onFocusChanged(textField.isFocused)
        }

        return clickedOnComponent && textField.isFocused
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!enabled || !visible || !textField.isFocused) return false

        val previousText = textField.text
        val handledByTextField = textField.textboxKeyTyped(typedChar, keyCode)

        if (handledByTextField) {
            if (textField.text != previousText) {
                if (validator(textField.text)) {
                    onTextChanged(textField.text)
                    lastText = textField.text
                } else {
                    textField.text = previousText
                }
            }
        }
        return handledByTextField
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