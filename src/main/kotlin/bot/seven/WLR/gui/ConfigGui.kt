package bot.seven.WLR.gui

import bot.seven.WLR.core.Config
import bot.seven.WLR.core.ConfigSorter
import bot.seven.WLR.gui.elements.*
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ConfigGui : GuiScreen() {

    private data class Tab(
        val name: String,
        val id: String,
        val icon: ResourceLocation? = null,
        val components: MutableList<GuiComponentBase> = mutableListOf(),
        var scrollY: Int = 0,
        var targetScrollY: Int = 0,
        var contentHeight: Int = 0,
        var maxScrollY: Int = 0
    )

    private val tabs = mutableListOf<Tab>()
    private var currentTabIndex = 0
    private fun currentTab(): Tab {
        return if (tabs.isNotEmpty() && currentTabIndex >= 0 && currentTabIndex < tabs.size) {
            tabs[currentTabIndex]
        } else if (tabs.isNotEmpty()) {
            tabs[0]
        } else {
            Tab("Error", "error_no_tabs", null)
        }
    }

    private val guiTitle = "WLR Settings"
    private val titleBarHeight = 25
    private val tabBarButtonHeight = 28
    private val tabButtonWidth = 85

    private var tabScrollX: Int = 0
    private var targetTabScrollX: Int = 0
    private var totalTabsWidthUnscrolled: Int = 0
    private var visibleTabBarAreaWidth: Int = 0
    private var maxTabScrollX: Int = 0
    private val tabButtonSpacing = 4
    private val tabBarScrollButtonWidth = 20
    private val tabBarScrollButtonHeight = tabBarButtonHeight

    private val tabBarYOffset = titleBarHeight + 5
    private val tabBarInternalHeight = tabBarButtonHeight + 4
    private val contentAreaMarginTop = tabBarYOffset + tabBarInternalHeight + 5

    private val componentStartXOffset = 20
    private val scrollbarWidth = 8
    private val scrollbarMargin = 5

    private val SCROLL_SMOOTHING_FACTOR = 0.28f

    private lateinit var currentBotDropdown: Dropdown
    private lateinit var lobbyMovementCheckbox: Checkbox
    private lateinit var disableChatMessagesCheckbox: Checkbox
    private lateinit var throwAfterGamesSlider: Slider
    private lateinit var disconnectAfterGamesSlider: Slider
    private lateinit var disconnectAfterMinutesSlider: Slider
    private lateinit var enableBoostingModeCheckbox: Checkbox
    private lateinit var selectedBoostingBotDropdown: Dropdown
    private lateinit var boostingRequeueDelaySlider: Slider
    private lateinit var enableCustomCameraCheckbox: Checkbox
    private lateinit var cameraOffsetXSlider: Slider
    private lateinit var cameraOffsetYSlider: Slider
    private lateinit var cameraOffsetZSlider: Slider
    private lateinit var cameraPitchSlider: Slider
    private lateinit var cameraYawSlider: Slider
    private lateinit var enableCameraZoomCheckbox: Checkbox
    private lateinit var cameraZoomFovSlider: Slider
    private lateinit var enableReplayClearingModeCheckbox: Checkbox
    private lateinit var replayClearingMinDelaySlider: Slider
    private lateinit var replayClearingMaxDelaySlider: Slider
    private lateinit var replayClearingCommandCountSlider: Slider
    private lateinit var minCPSSlider: Slider
    private lateinit var maxCPSSlider: Slider
    private lateinit var lookSpeedHorizontalSlider: Slider
    private lateinit var lookSpeedVerticalSlider: Slider
    private lateinit var lookRandSlider: Slider
    private lateinit var maxDistanceLookSlider: Slider
    private lateinit var maxDistanceAttackSlider: Slider
    private lateinit var enableComboResetByDistanceCheckbox: Checkbox
    private lateinit var comboResetDistanceSlider: Slider
    private lateinit var sendAutoGGCheckbox: Checkbox
    private lateinit var ggMessageTextField: Textfield
    private lateinit var ggDelaySlider: Slider
    private lateinit var sendStartMessageCheckbox: Checkbox
    private lateinit var startMessageTextField: Textfield
    private lateinit var startMessageDelaySlider: Slider
    private lateinit var autoRqDelaySlider: Slider
    private lateinit var rqNoGameSlider: Slider
    private lateinit var paperRequeueCheckbox: Checkbox
    private lateinit var fastRequeueCheckbox: Checkbox
    private lateinit var sendWebhookMessagesCheckbox: Checkbox
    private lateinit var webhookURLTextField: Textfield
    private lateinit var boxingFishCheckbox: Checkbox
    private lateinit var sessionStatsHUDCheckbox: Checkbox


    private var logicalCurrentY = 0
    private val interComponentSpacing = 12
    private val componentWidth = 240
    private val checkboxSize = 12
    private val textFieldHeight = 20
    private val dropdownHeight = 20
    private val sliderHeight = 14
    private var labelHeightAboveComponent: Int = 0
    private val contentPaddingTopForComponents = 15
    private val contentPaddingBottomForComponents = 15

    private var openDropdown: Dropdown? = null
    private val allPossibleTabsMap = mutableMapOf<String, Tab>()

    private var nextComponentId = 1
    private fun getNextId(): Int = nextComponentId++


    override fun initGui() {
        super.initGui()
        this.labelHeightAboveComponent = mc.fontRendererObj.FONT_HEIGHT + 2
        nextComponentId = 1

        Keyboard.enableRepeatEvents(true)
        openDropdown = null
        tabScrollX = 0
        targetTabScrollX = 0

        try {
            defineAllPossibleTabs()
            orderAndPopulateTabs()
            if (this.tabs.isEmpty()) {
                return
            }
            if (currentTabIndex >= tabs.size) currentTabIndex = 0

            calculateTabScrolling()
            calculateContentScrolling()
            updateGuiElementStates()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun defineAllPossibleTabs() {
        allPossibleTabsMap.clear()
        allPossibleTabsMap["General"] = Tab("General", "General", null)
        allPossibleTabsMap["Combat"] = Tab("Combat", "Combat", null)
        allPossibleTabsMap["Requeue"] = Tab("Requeue", "Requeue", null)
        allPossibleTabsMap["Messages"] = Tab("Messages", "Messages", null)
        allPossibleTabsMap["Boosting"] = Tab("Boosting", "Boosting", null)
        allPossibleTabsMap["Webhook"] = Tab("Webhook", "Webhook", null)
        allPossibleTabsMap["Replays"] = Tab("Replays", "Replays", null)
        allPossibleTabsMap["Camera"] = Tab("Camera", "Camera", null)
        allPossibleTabsMap["HUD"] = Tab("HUD", "HUD", null)
        allPossibleTabsMap["Misc"] = Tab("Misc", "Misc", null)
    }

    private fun orderAndPopulateTabs() {
        this.tabs.clear()
        ConfigSorter.WLR_TAB_ORDER.forEach { tabId ->
            allPossibleTabsMap[tabId]?.let { tabDefinition ->
                populateComponentsForTab(tabDefinition)
                this.tabs.add(tabDefinition)
            } ?: run {

            }
        }
        allPossibleTabsMap.values.forEach { tabDefinition ->
            if (!this.tabs.any { it.id == tabDefinition.id }) {
                populateComponentsForTab(tabDefinition)
                this.tabs.add(tabDefinition)
            }
        }
    }


    private fun populateComponentsForTab(tab: Tab) {
        val contentPaneFullWidth = this.width - (componentStartXOffset * 2)
        val availableWidthForComponents = contentPaneFullWidth - 20
        val actualComponentWidth = min(componentWidth, availableWidthForComponents)
        val startX = componentStartXOffset + (contentPaneFullWidth - actualComponentWidth) / 2

        logicalCurrentY = 0
        logicalCurrentY += contentPaddingTopForComponents

        when (tab.id) {
            "General" -> {
                currentBotDropdown = Dropdown(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Current Bot", Config.REGULAR_BOT_OPTIONS.toList(), Config.currentBot) { index, _ -> Config.setCurrentBot(index); updateGuiElementStates() }
                tab.components.add(currentBotDropdown)
                logicalCurrentY += labelHeightAboveComponent + dropdownHeight + interComponentSpacing

                lobbyMovementCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Lobby Movement", Config.lobbyMovement) { Config.lobbyMovement = it; Config.save() }
                tab.components.add(lobbyMovementCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                disableChatMessagesCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Disable Chat Messages", Config.disableChatMessages) { Config.disableChatMessages = it; Config.save() }
                tab.components.add(disableChatMessagesCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                throwAfterGamesSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Throw After X Games", Config.throwAfterGames.toFloat(), 0f, 1000f, 1f, { "%.0f".format(it) }) { Config.throwAfterGames = it.toInt(); Config.save() }
                tab.components.add(throwAfterGamesSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                disconnectAfterGamesSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Disconnect After X Games", Config.disconnectAfterGames.toFloat(), 0f, 10000f, 10f, { "%.0f".format(it) }) { Config.disconnectAfterGames = it.toInt(); Config.save() }
                tab.components.add(disconnectAfterGamesSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                disconnectAfterMinutesSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Disconnect After X Mins", Config.disconnectAfterMinutes.toFloat(), 0f, 500f, 5f, { "%.0f".format(it) }) { Config.disconnectAfterMinutes = it.toInt(); Config.save() }
                tab.components.add(disconnectAfterMinutesSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight
            }
            "Boosting" -> {
                enableBoostingModeCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Enable Boosting Mode", Config.enableBoostingMode) { Config.setEnableBoostingMode(it); updateGuiElementStates() }
                tab.components.add(enableBoostingModeCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                selectedBoostingBotDropdown = Dropdown(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Selected Boosting Bot", Config.BOOSTING_BOT_OPTIONS.toList(), Config.selectedBoostingBotIndex) { index, _ -> Config.setSelectedBoostingBot(index); updateGuiElementStates() }
                tab.components.add(selectedBoostingBotDropdown)
                logicalCurrentY += labelHeightAboveComponent + dropdownHeight + interComponentSpacing

                boostingRequeueDelaySlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Boosting Requeue Delay", Config.boostingRequeueDelay.toFloat(), 0f, 5000f, 50f, { "%.0f ms".format(it) }) { Config.boostingRequeueDelay = max(50, it.toInt()); Config.save() }
                tab.components.add(boostingRequeueDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight
            }
            "Camera" -> {
                enableCustomCameraCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Enable Custom Camera", Config.enableCustomCamera) { Config.enableCustomCamera = it; Config.save(); updateGuiElementStates() }
                tab.components.add(enableCustomCameraCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                cameraOffsetXSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Offset X", Config.cameraOffsetX, -10f, 10f, 0.1f) { Config.cameraOffsetX = it; Config.save() }
                tab.components.add(cameraOffsetXSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                cameraOffsetYSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Offset Y", Config.cameraOffsetY, -10f, 10f, 0.1f) { Config.cameraOffsetY = it; Config.save() }
                tab.components.add(cameraOffsetYSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                cameraOffsetZSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Offset Z", Config.cameraOffsetZ, -15f, 15f, 0.1f) { Config.cameraOffsetZ = it; Config.save() }
                tab.components.add(cameraOffsetZSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                cameraPitchSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Pitch", Config.cameraPitch, -90f, 90f, 0.5f, { "%.1f°".format(it)}) { Config.cameraPitch = it; Config.save() }
                tab.components.add(cameraPitchSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                cameraYawSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Yaw", Config.cameraYaw, -180f, 180f, 0.5f, { "%.1f°".format(it)}) { Config.cameraYaw = it; Config.save() }
                tab.components.add(cameraYawSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                enableCameraZoomCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Enable Camera Zoom", Config.enableCameraZoom) {
                    Config.enableCameraZoom = it; Config.save(); updateGuiElementStates()
                }
                tab.components.add(enableCameraZoomCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                cameraZoomFovSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Zoom FOV", Config.cameraZoomFovValue, 10f, 90f, 1f, { "%.0f".format(it) }) {
                    Config.cameraZoomFovValue = it; Config.save()
                }
                tab.components.add(cameraZoomFovSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight
            }
            "Replays" -> {
                enableReplayClearingModeCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Enable Replay Clearing", Config.enableReplayClearingMode) { Config.setEnableReplayClearingMode(it); updateGuiElementStates() }
                tab.components.add(enableReplayClearingModeCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                replayClearingMinDelaySlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Min Delay", Config.replayClearingMinDelay.toFloat(), 500f, 20000f, 100f, { "%.0f ms".format(it) }) { Config.replayClearingMinDelay = it.toInt(); Config.save() }
                tab.components.add(replayClearingMinDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                replayClearingMaxDelaySlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Max Delay", Config.replayClearingMaxDelay.toFloat(), 500f, 20000f, 100f, { "%.0f ms".format(it) }) { Config.replayClearingMaxDelay = it.toInt(); Config.save() }
                tab.components.add(replayClearingMaxDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                replayClearingCommandCountSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Command Count", Config.replayClearingCommandCount.toFloat(), 1f, 10000f, 10f, { "%.0f".format(it) }) { Config.replayClearingCommandCount = it.toInt(); Config.save() }
                tab.components.add(replayClearingCommandCountSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight
            }
            "Combat" -> {
                minCPSSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Min CPS", Config.minCPS.toFloat(), 1f, 20f, 0.5f, { "%.1f".format(it) }) { Config.minCPS = it.toInt(); Config.save() }
                tab.components.add(minCPSSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                maxCPSSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Max CPS", Config.maxCPS.toFloat(), 1f, 20f, 0.5f, { "%.1f".format(it) }) { Config.maxCPS = it.toInt(); Config.save() }
                tab.components.add(maxCPSSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                lookSpeedHorizontalSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Horizontal Look Speed", Config.lookSpeedHorizontal.toFloat(), 1f, 30f, 1f, { "%.0f".format(it) }) { Config.lookSpeedHorizontal = it.toInt(); Config.save() }
                tab.components.add(lookSpeedHorizontalSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                lookSpeedVerticalSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Vertical Look Speed", Config.lookSpeedVertical.toFloat(), 1f, 30f, 1f, { "%.0f".format(it) }) { Config.lookSpeedVertical = it.toInt(); Config.save() }
                tab.components.add(lookSpeedVerticalSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                lookRandSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Look Randomization", Config.lookRand, 0f, 5f, 0.1f) { Config.lookRand = it; Config.save() }
                tab.components.add(lookRandSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                maxDistanceLookSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Max Look Distance", Config.maxDistanceLook.toFloat(), 3f, 8f, 0.1f, { "%.1f".format(it) }) { Config.maxDistanceLook = it.toInt(); Config.save() }
                tab.components.add(maxDistanceLookSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                maxDistanceAttackSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Max Attack Distance", Config.maxDistanceAttack.toFloat(), 3f, 8f, 0.1f, { "%.1f".format(it) }) { Config.maxDistanceAttack = it.toInt(); Config.save() }
                tab.components.add(maxDistanceAttackSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                enableComboResetByDistanceCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Combo Reset by Distance", Config.enableComboResetByDistance) { Config.enableComboResetByDistance = it; Config.save(); updateGuiElementStates() }
                tab.components.add(enableComboResetByDistanceCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                comboResetDistanceSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Combo Reset Distance", Config.comboResetDistance.toFloat(), 1f, 10f, 0.5f, { "%.1f".format(it) }) { Config.comboResetDistance = it.toInt(); Config.save() }
                tab.components.add(comboResetDistanceSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight
            }
            "Messages" -> {
                sendAutoGGCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Enable AutoGG", Config.sendAutoGG) { Config.sendAutoGG = it; Config.save(); updateGuiElementStates() }
                tab.components.add(sendAutoGGCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                ggMessageTextField = Textfield(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, textFieldHeight, "AutoGG Message", Config.ggMessage, onTextChanged = { Config.ggMessage = it }, onFocusChanged = { if (!it) Config.save() })
                tab.components.add(ggMessageTextField)
                logicalCurrentY += labelHeightAboveComponent + textFieldHeight + interComponentSpacing

                ggDelaySlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "AutoGG Delay", Config.ggDelay.toFloat(), 0f, 2000f, 50f, { "%.0f ms".format(it) }) { Config.ggDelay = it.toInt(); Config.save() }
                tab.components.add(ggDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                sendStartMessageCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Game Start Message", Config.sendStartMessage) { Config.sendStartMessage = it; Config.save(); updateGuiElementStates() }
                tab.components.add(sendStartMessageCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                startMessageTextField = Textfield(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, textFieldHeight, "Start Message", Config.startMessage, onTextChanged = { Config.startMessage = it }, onFocusChanged = { if (!it) Config.save() })
                tab.components.add(startMessageTextField)
                logicalCurrentY += labelHeightAboveComponent + textFieldHeight + interComponentSpacing

                startMessageDelaySlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Start Message Delay", Config.startMessageDelay.toFloat(), 0f, 2000f, 50f, { "%.0f ms".format(it) }) { Config.startMessageDelay = it.toInt(); Config.save() }
                tab.components.add(startMessageDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight
            }
            "Requeue" -> {
                autoRqDelaySlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Requeue Delay", Config.autoRqDelay.toFloat(), 0f, 5000f, 50f, { "%.0f ms".format(it) }) { Config.autoRqDelay = it.toInt(); Config.save() }
                tab.components.add(autoRqDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                rqNoGameSlider = Slider(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, "Requeue No Game Timer", Config.rqNoGame.toFloat(), 5f, 120f, 1f, { "%.0f s".format(it) }) { Config.rqNoGame = it.toInt(); Config.save() }
                tab.components.add(rqNoGameSlider)
                logicalCurrentY += labelHeightAboveComponent + sliderHeight + interComponentSpacing

                paperRequeueCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Paper Requeue", Config.paperRequeue) { Config.paperRequeue = it; Config.save() }
                tab.components.add(paperRequeueCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                fastRequeueCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Fast Requeue", Config.fastRequeue) { Config.fastRequeue = it; Config.save() }
                tab.components.add(fastRequeueCheckbox)
                logicalCurrentY += checkboxSize
            }
            "Webhook" -> {
                sendWebhookMessagesCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Enable Webhook", Config.sendWebhookMessages) { Config.sendWebhookMessages = it; Config.save(); updateGuiElementStates() }
                tab.components.add(sendWebhookMessagesCheckbox)
                logicalCurrentY += checkboxSize + interComponentSpacing

                webhookURLTextField = Textfield(getNextId(), startX, logicalCurrentY + labelHeightAboveComponent, actualComponentWidth, textFieldHeight, "Webhook URL", Config.webhookURL, onTextChanged = { Config.webhookURL = it }, onFocusChanged = { if (!it) Config.save() })
                tab.components.add(webhookURLTextField)
                logicalCurrentY += labelHeightAboveComponent + textFieldHeight + interComponentSpacing
            }
            "Misc" -> {
                boxingFishCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Boxing Fish (Visual)", Config.boxingFish) { Config.boxingFish = it; Config.save() }
                tab.components.add(boxingFishCheckbox)
                logicalCurrentY += checkboxSize
            }
            "HUD" -> {
                sessionStatsHUDCheckbox = Checkbox(getNextId(), startX, logicalCurrentY, checkboxSize, "Session Stats HUD", Config.sessionStatsHUD) { Config.sessionStatsHUD = it; Config.save() }
                tab.components.add(sessionStatsHUDCheckbox)
                logicalCurrentY += checkboxSize
            }
        }
        tab.contentHeight = (logicalCurrentY - contentPaddingTopForComponents) + contentPaddingBottomForComponents
    }

    private fun calculateTabScrolling() {
        if (tabs.isEmpty()) {
            totalTabsWidthUnscrolled = 0; visibleTabBarAreaWidth = 0; maxTabScrollX = 0; return
        }
        totalTabsWidthUnscrolled = tabs.sumOf { tabButtonWidth + tabButtonSpacing }
        if (tabs.isNotEmpty() && tabButtonSpacing > 0) {
            totalTabsWidthUnscrolled -= tabButtonSpacing
        }

        visibleTabBarAreaWidth = this.width - (componentStartXOffset) * 2
        if (totalTabsWidthUnscrolled > visibleTabBarAreaWidth && tabs.size > 1) {
            visibleTabBarAreaWidth -= (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
        }
        maxTabScrollX = max(0, totalTabsWidthUnscrolled - visibleTabBarAreaWidth)
    }

    private fun calculateContentScrolling() {
        if (tabs.isEmpty()) return
        val contentAreaDrawableHeight = this.height - contentAreaMarginTop - 20
        tabs.forEach { tab ->
            tab.maxScrollY = max(0, tab.contentHeight - contentAreaDrawableHeight)
            tab.scrollY = tab.scrollY.coerceIn(0, tab.maxScrollY)
            tab.targetScrollY = tab.targetScrollY.coerceIn(0, tab.maxScrollY)
        }
    }


    private fun updateGuiElementStates() {
        if (tabs.isEmpty() || !::currentBotDropdown.isInitialized) return

        if (::selectedBoostingBotDropdown.isInitialized) selectedBoostingBotDropdown.enabled = Config.enableBoostingMode
        if (::boostingRequeueDelaySlider.isInitialized) boostingRequeueDelaySlider.enabled = Config.enableBoostingMode

        if (::cameraOffsetXSlider.isInitialized) cameraOffsetXSlider.enabled = Config.enableCustomCamera
        if (::cameraOffsetYSlider.isInitialized) cameraOffsetYSlider.enabled = Config.enableCustomCamera
        if (::cameraOffsetZSlider.isInitialized) cameraOffsetZSlider.enabled = Config.enableCustomCamera
        if (::cameraPitchSlider.isInitialized) cameraPitchSlider.enabled = Config.enableCustomCamera
        if (::cameraYawSlider.isInitialized) cameraYawSlider.enabled = Config.enableCustomCamera
        if (::enableCameraZoomCheckbox.isInitialized) enableCameraZoomCheckbox.enabled = Config.enableCustomCamera
        if (::cameraZoomFovSlider.isInitialized) cameraZoomFovSlider.enabled = Config.enableCustomCamera && Config.enableCameraZoom


        if (::replayClearingMinDelaySlider.isInitialized) replayClearingMinDelaySlider.enabled = Config.enableReplayClearingMode
        if (::replayClearingMaxDelaySlider.isInitialized) replayClearingMaxDelaySlider.enabled = Config.enableReplayClearingMode
        if (::replayClearingCommandCountSlider.isInitialized) replayClearingCommandCountSlider.enabled = Config.enableReplayClearingMode

        if (::comboResetDistanceSlider.isInitialized) comboResetDistanceSlider.enabled = Config.enableComboResetByDistance

        if (::ggMessageTextField.isInitialized) ggMessageTextField.enabled = Config.sendAutoGG
        if (::ggDelaySlider.isInitialized) ggDelaySlider.enabled = Config.sendAutoGG
        if (::startMessageTextField.isInitialized) startMessageTextField.enabled = Config.sendStartMessage
        if (::startMessageDelaySlider.isInitialized) startMessageDelaySlider.enabled = Config.sendStartMessage

        if (::webhookURLTextField.isInitialized) webhookURLTextField.enabled = Config.sendWebhookMessages
    }


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val activeTab = currentTab()
        if (activeTab.id != "error_no_tabs") {
            val scrollYDiff = activeTab.targetScrollY - activeTab.scrollY
            if (abs(scrollYDiff) > 0) {
                var scrollYStep = (scrollYDiff * SCROLL_SMOOTHING_FACTOR); if (abs(scrollYStep) < 1f && scrollYDiff != 0) scrollYStep = if (scrollYDiff > 0) 1f else -1f
                activeTab.scrollY += scrollYStep.toInt()
                if ((scrollYDiff > 0 && activeTab.scrollY >= activeTab.targetScrollY) || (scrollYDiff < 0 && activeTab.scrollY <= activeTab.targetScrollY) || abs(activeTab.scrollY - activeTab.targetScrollY) < 1) activeTab.scrollY = activeTab.targetScrollY
            }
            activeTab.scrollY = activeTab.scrollY.coerceIn(0, activeTab.maxScrollY)

            val tabScrollXDiff = targetTabScrollX - tabScrollX
            if (abs(tabScrollXDiff) > 0) {
                var tabScrollXStep = (tabScrollXDiff * SCROLL_SMOOTHING_FACTOR); if (abs(tabScrollXStep) < 1f && tabScrollXDiff != 0) tabScrollXStep = if (tabScrollXDiff > 0) 1f else -1f
                tabScrollX += tabScrollXStep.toInt()
                if ((tabScrollXDiff > 0 && tabScrollX >= targetTabScrollX) || (tabScrollXDiff < 0 && tabScrollX <= targetTabScrollX) || abs(tabScrollX - targetTabScrollX) < 1) tabScrollX = targetTabScrollX
            }
            tabScrollX = tabScrollX.coerceIn(0, maxTabScrollX)
        }

        Gui.drawRect(0, 0, this.width, this.height, GuiColors.SCREEN_BACKGROUND)
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO); GlStateManager.disableLighting()

        Gui.drawRect(0, 0, this.width, titleBarHeight, GuiColors.COMPONENT_BACKGROUND)
        Gui.drawRect(0, titleBarHeight - 1, this.width, titleBarHeight, GuiColors.PRIMARY_RED)
        drawCenteredString(fontRendererObj, guiTitle, this.width / 2, (titleBarHeight - fontRendererObj.FONT_HEIGHT) / 2, GuiColors.TEXT_PRIMARY)

        Gui.drawRect(0, tabBarYOffset, this.width, tabBarYOffset + tabBarInternalHeight, GuiColors.COMPONENT_BACKGROUND)

        val tabsInitialRenderX = componentStartXOffset / 2
        var tabsViewportStartX = tabsInitialRenderX
        var localVisibleTabBarAreaWidth = this.width - (tabsInitialRenderX * 2)
        val needsTabBarScrollButtons = totalTabsWidthUnscrolled > localVisibleTabBarAreaWidth && tabs.size > 1


        if (needsTabBarScrollButtons) {
            tabsViewportStartX += tabBarScrollButtonWidth + tabButtonSpacing
            localVisibleTabBarAreaWidth -= (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)

            val scrollButtonActualY = tabBarYOffset + 2
            val scrollLeftX = tabsInitialRenderX
            val scrollLeftHover = mouseX >= scrollLeftX && mouseX < scrollLeftX + tabBarScrollButtonWidth && mouseY >= scrollButtonActualY && mouseY < scrollButtonActualY + tabBarScrollButtonHeight
            Gui.drawRect(scrollLeftX, scrollButtonActualY, scrollLeftX + tabBarScrollButtonWidth, scrollButtonActualY + tabBarScrollButtonHeight, if(scrollLeftHover) GuiColors.COMPONENT_BACKGROUND_HOVER else GuiColors.COMPONENT_BACKGROUND)
            Gui.drawRect(scrollLeftX, scrollButtonActualY, scrollLeftX + tabBarScrollButtonWidth, scrollButtonActualY + 1, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(scrollLeftX, scrollButtonActualY + tabBarScrollButtonHeight -1 , scrollLeftX + tabBarScrollButtonWidth, scrollButtonActualY + tabBarScrollButtonHeight, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(scrollLeftX, scrollButtonActualY, scrollLeftX + 1, scrollButtonActualY + tabBarScrollButtonHeight, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(scrollLeftX + tabBarScrollButtonWidth -1, scrollButtonActualY, scrollLeftX + tabBarScrollButtonWidth, scrollButtonActualY + tabBarScrollButtonHeight, GuiColors.COMPONENT_BORDER)
            drawCenteredString(fontRendererObj, "<", scrollLeftX + tabBarScrollButtonWidth / 2, scrollButtonActualY + (tabBarScrollButtonHeight - fontRendererObj.FONT_HEIGHT) / 2, if (tabScrollX > 0 || targetTabScrollX > 0) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED)

            val scrollRightX = tabsInitialRenderX + tabBarScrollButtonWidth + tabButtonSpacing + localVisibleTabBarAreaWidth + tabButtonSpacing
            val scrollRightHover = mouseX >= scrollRightX && mouseX < scrollRightX + tabBarScrollButtonWidth && mouseY >= scrollButtonActualY && mouseY < scrollButtonActualY + tabBarScrollButtonHeight
            Gui.drawRect(scrollRightX, scrollButtonActualY, scrollRightX + tabBarScrollButtonWidth, scrollButtonActualY + tabBarScrollButtonHeight, if(scrollRightHover) GuiColors.COMPONENT_BACKGROUND_HOVER else GuiColors.COMPONENT_BACKGROUND)
            Gui.drawRect(scrollRightX, scrollButtonActualY, scrollRightX + tabBarScrollButtonWidth, scrollButtonActualY + 1, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(scrollRightX, scrollButtonActualY + tabBarScrollButtonHeight -1 , scrollRightX + tabBarScrollButtonWidth, scrollButtonActualY + tabBarScrollButtonHeight, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(scrollRightX, scrollButtonActualY, scrollRightX + 1, scrollButtonActualY + tabBarScrollButtonHeight, GuiColors.COMPONENT_BORDER)
            Gui.drawRect(scrollRightX + tabBarScrollButtonWidth -1, scrollButtonActualY, scrollRightX + tabBarScrollButtonWidth, scrollButtonActualY + tabBarScrollButtonHeight, GuiColors.COMPONENT_BORDER)
            drawCenteredString(fontRendererObj, ">", scrollRightX + tabBarScrollButtonWidth / 2, scrollButtonActualY + (tabBarScrollButtonHeight - fontRendererObj.FONT_HEIGHT) / 2, if (tabScrollX < maxTabScrollX || targetTabScrollX < maxTabScrollX) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_DISABLED)
        }


        startScissor(tabsViewportStartX, tabBarYOffset + 2, localVisibleTabBarAreaWidth, tabBarButtonHeight)
        var currentTabButtonVisualX = tabsViewportStartX - tabScrollX
        tabs.forEachIndexed { index, tab ->
            if (currentTabButtonVisualX + tabButtonWidth > tabsViewportStartX && currentTabButtonVisualX < tabsViewportStartX + localVisibleTabBarAreaWidth) {
                val isSelected = index == currentTabIndex
                val tabHovered = mouseX >= currentTabButtonVisualX && mouseX < currentTabButtonVisualX + tabButtonWidth &&
                        mouseY >= tabBarYOffset + 2 && mouseY < tabBarYOffset + 2 + tabBarButtonHeight &&
                        mouseX >= tabsViewportStartX && mouseX < tabsViewportStartX + localVisibleTabBarAreaWidth

                val tabBgColor = when {
                    isSelected -> GuiColors.PRIMARY_RED
                    tabHovered -> GuiColors.COMPONENT_BACKGROUND_HOVER
                    else -> GuiColors.COMPONENT_BACKGROUND
                }
                val textColor = if (isSelected) GuiColors.TEXT_PRIMARY else GuiColors.TEXT_SECONDARY

                Gui.drawRect(currentTabButtonVisualX, tabBarYOffset + 2, currentTabButtonVisualX + tabButtonWidth, tabBarYOffset + 2 + tabBarButtonHeight, tabBgColor)
                if (isSelected) {
                    Gui.drawRect(currentTabButtonVisualX, tabBarYOffset + 2 + tabBarButtonHeight - 2, currentTabButtonVisualX + tabButtonWidth, tabBarYOffset + 2 + tabBarButtonHeight, GuiColors.PRIMARY_RED)
                } else {
                    Gui.drawRect(currentTabButtonVisualX, tabBarYOffset + 2 + tabBarButtonHeight - 1, currentTabButtonVisualX + tabButtonWidth, tabBarYOffset + 2 + tabBarButtonHeight, GuiColors.COMPONENT_BORDER)
                }
                Gui.drawRect(currentTabButtonVisualX, tabBarYOffset + 2, currentTabButtonVisualX + 1, tabBarYOffset + 2 + tabBarButtonHeight, GuiColors.COMPONENT_BORDER)
                Gui.drawRect(currentTabButtonVisualX + tabButtonWidth - 1, tabBarYOffset + 2, currentTabButtonVisualX + tabButtonWidth, tabBarYOffset + 2 + tabBarButtonHeight, GuiColors.COMPONENT_BORDER)
                Gui.drawRect(currentTabButtonVisualX, tabBarYOffset + 2, currentTabButtonVisualX + tabButtonWidth, tabBarYOffset + 2 + 1, GuiColors.COMPONENT_BORDER)


                val textY = tabBarYOffset + 2 + (tabBarButtonHeight - fontRendererObj.FONT_HEIGHT) / 2
                tab.icon?.let {
                    mc.textureManager.bindTexture(it)
                    GlStateManager.color(1f, 1f, 1f, 1f)
                    val iconSize = 16
                    val iconY = tabBarYOffset + 2 + (tabBarButtonHeight - iconSize) / 2
                    val iconX = currentTabButtonVisualX + 4
                    Gui.drawModalRectWithCustomSizedTexture(iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize.toFloat(), iconSize.toFloat())
                    drawCenteredString(fontRendererObj, tab.name, currentTabButtonVisualX + tabButtonWidth / 2 + (iconSize/2) , textY, textColor)
                } ?: run {
                    drawCenteredString(fontRendererObj, tab.name, currentTabButtonVisualX + tabButtonWidth / 2, textY, textColor)
                }
            }
            currentTabButtonVisualX += tabButtonWidth + tabButtonSpacing
        }
        stopScissor()

        val contentAreaVisualTop = contentAreaMarginTop
        val contentAreaVisualBottom = this.height - 10
        val contentAreaDrawableHeight = contentAreaVisualBottom - contentAreaVisualTop

        Gui.drawRect(componentStartXOffset / 2, contentAreaVisualTop, this.width - componentStartXOffset / 2, contentAreaVisualBottom, Color(10,10,10, 200).rgb)
        Gui.drawRect(componentStartXOffset / 2, contentAreaVisualTop, this.width - componentStartXOffset / 2, contentAreaVisualTop + 1, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(componentStartXOffset / 2, contentAreaVisualBottom - 1, this.width - componentStartXOffset / 2, contentAreaVisualBottom, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(componentStartXOffset / 2, contentAreaVisualTop, componentStartXOffset / 2 + 1, contentAreaVisualBottom, GuiColors.COMPONENT_BORDER)
        Gui.drawRect(this.width - componentStartXOffset / 2 - 1, contentAreaVisualTop, this.width - componentStartXOffset / 2, contentAreaVisualBottom, GuiColors.COMPONENT_BORDER)


        var mainContentScissorWidth = this.width - (componentStartXOffset) - (componentStartXOffset / 2)
        if (activeTab.id != "error_no_tabs" && activeTab.maxScrollY > 0) {
            mainContentScissorWidth -= (scrollbarWidth + scrollbarMargin)
        }
        startScissor(componentStartXOffset / 2 + 1, contentAreaVisualTop + 1, mainContentScissorWidth, contentAreaDrawableHeight - 2)

        if (activeTab.id != "error_no_tabs") {
            activeTab.components.forEach { component ->
                val originalLogicalY = component.y
                val componentScreenY = contentAreaVisualTop + originalLogicalY - activeTab.scrollY

                if (componentScreenY + component.height > contentAreaVisualTop && componentScreenY < contentAreaVisualBottom) {
                    component.y = componentScreenY

                    if (component is Dropdown && component.isOpen && component != openDropdown) {
                        component.drawComponent(mouseX, mouseY, partialTicks)
                    } else if (!(component is Dropdown && component.isOpen)) {
                        component.drawComponent(mouseX, mouseY, partialTicks)
                    }
                    component.y = originalLogicalY
                }
            }
        }
        stopScissor()

        if (activeTab.id != "error_no_tabs" && activeTab.maxScrollY > 0) {
            val scrollBarActualX = this.width - componentStartXOffset / 2 - scrollbarMargin - scrollbarWidth
            val scrollBarTrackY = contentAreaVisualTop + 2
            val scrollBarTrackHeight = contentAreaDrawableHeight - 4

            Gui.drawRect(scrollBarActualX, scrollBarTrackY, scrollBarActualX + scrollbarWidth, scrollBarTrackY + scrollBarTrackHeight, GuiColors.SCROLLBAR_BG)

            if (activeTab.contentHeight > contentAreaDrawableHeight) {
                val thumbHeightRatio = (contentAreaDrawableHeight.toFloat() / activeTab.contentHeight.toFloat()).coerceIn(0.05f, 1f)
                val thumbHeight = max(15, (scrollBarTrackHeight * thumbHeightRatio).toInt())

                val thumbYRatio = if (activeTab.maxScrollY > 0) activeTab.scrollY.toFloat() / activeTab.maxScrollY.toFloat() else 0f
                val thumbY = scrollBarTrackY + ((scrollBarTrackHeight - thumbHeight) * thumbYRatio).toInt()

                Gui.drawRect(
                    scrollBarActualX + 1,
                    thumbY.coerceIn(scrollBarTrackY, scrollBarTrackY + scrollBarTrackHeight - thumbHeight),
                    scrollBarActualX + scrollbarWidth - 1,
                    (thumbY + thumbHeight).coerceIn(scrollBarTrackY + thumbHeight, scrollBarTrackY + scrollBarTrackHeight),
                    GuiColors.SCROLLBAR_THUMB
                )
            }
        }

        openDropdown?.let { dd ->
            val originalLogicalY_dd = dd.y
            val dropdownScreenY = contentAreaVisualTop + originalLogicalY_dd - activeTab.scrollY
            dd.y = dropdownScreenY
            dd.drawComponent(mouseX, mouseY, partialTicks)
            dd.y = originalLogicalY_dd
        }
        GlStateManager.disableBlend()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) { return }

        val tabsInitialRenderX = componentStartXOffset / 2
        var localTabsViewportStartX = tabsInitialRenderX
        val localVisibleTabBarAreaWidthFull = this.width - (tabsInitialRenderX * 2)
        val needsTabBarScrollButtons = totalTabsWidthUnscrolled > localVisibleTabBarAreaWidthFull && tabs.size > 1
        val scrollButtonActualY = tabBarYOffset + 2

        if (needsTabBarScrollButtons) {
            val scrollLeftX = tabsInitialRenderX
            if (mouseX >= scrollLeftX && mouseX < scrollLeftX + tabBarScrollButtonWidth &&
                mouseY >= scrollButtonActualY && mouseY < scrollButtonActualY + tabBarScrollButtonHeight) {
                targetTabScrollX = max(0, targetTabScrollX - (tabButtonWidth + tabButtonSpacing))
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 0.7F))
                return
            }

            val actualTabBarViewportWidth = localVisibleTabBarAreaWidthFull - (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
            val scrollRightX = tabsInitialRenderX + tabBarScrollButtonWidth + tabButtonSpacing + actualTabBarViewportWidth + tabButtonSpacing
            if (mouseX >= scrollRightX && mouseX < scrollRightX + tabBarScrollButtonWidth &&
                mouseY >= scrollButtonActualY && mouseY < scrollButtonActualY + tabBarScrollButtonHeight) {
                targetTabScrollX = min(maxTabScrollX, targetTabScrollX + (tabButtonWidth + tabButtonSpacing))
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 0.7F))
                return
            }
            localTabsViewportStartX += tabBarScrollButtonWidth + tabButtonSpacing
        }

        val actualClickableTabBarWidth = if (needsTabBarScrollButtons) {
            localVisibleTabBarAreaWidthFull - (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
        } else {
            localVisibleTabBarAreaWidthFull
        }

        var currentTabButtonVisualX = localTabsViewportStartX - tabScrollX
        tabs.forEachIndexed { index, _ ->
            if (mouseX >= currentTabButtonVisualX && mouseX < currentTabButtonVisualX + tabButtonWidth &&
                mouseY >= tabBarYOffset + 2 && mouseY < tabBarYOffset + 2 + tabBarButtonHeight &&
                mouseX >= localTabsViewportStartX && mouseX < localTabsViewportStartX + actualClickableTabBarWidth) {
                if (currentTabIndex != index) {
                    currentTab().components.forEach {
                        if (it is Dropdown) it.close()
                        if (it is Textfield) it.setFocused(false)
                    }
                    openDropdown = null

                    currentTabIndex = index
                    currentTab().scrollY = currentTab().targetScrollY
                    mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 0.9F))
                }
                return
            }
            currentTabButtonVisualX += tabButtonWidth + tabButtonSpacing
        }

        val activeTab = currentTab()
        if (activeTab.id == "error_no_tabs") return

        val contentAreaVisualTop = contentAreaMarginTop

        if (this.openDropdown != null) {
            val dd = this.openDropdown!!
            val originalLogicalY_dd = dd.y
            val originalLogicalX_dd = dd.x

            val dropdownScreenY = contentAreaVisualTop + originalLogicalY_dd - activeTab.scrollY
            dd.y = dropdownScreenY

            if (dd.mouseClicked(mouseX, mouseY, mouseButton)) {
                dd.y = originalLogicalY_dd
                if (!dd.isOpen) {
                    this.openDropdown = null
                }
                return
            }

            val clickInsideExpandedDropdown = mouseX >= originalLogicalX_dd && mouseX < originalLogicalX_dd + dd.width &&
                    mouseY >= dropdownScreenY &&
                    mouseY < dropdownScreenY + dd.height + (if (dd.isOpen) dd.options.take(dd.maxDisplayableOptions).size * dd.optionHeight else 0)

            dd.y = originalLogicalY_dd

            if (!clickInsideExpandedDropdown) {
                dd.close()
                this.openDropdown = null
            } else {
                return
            }
        }

        var clickedFocusableComponent = false
        for (component in activeTab.components.asReversed()) {
            if (component is Dropdown && component == openDropdown && component.isOpen) {
                continue
            }

            val originalLogicalY_comp = component.y
            val originalLogicalX_comp = component.x

            val componentScreenY = contentAreaVisualTop + originalLogicalY_comp - activeTab.scrollY

            if (mouseX >= originalLogicalX_comp && mouseX < originalLogicalX_comp + component.width &&
                mouseY >= componentScreenY && mouseY < componentScreenY + component.height) {

                component.y = componentScreenY

                if (component.mouseClicked(mouseX, mouseY, mouseButton)) {
                    component.y = originalLogicalY_comp
                    clickedFocusableComponent = true

                    if (component is Textfield && component.textField.isFocused) {
                        activeTab.components.forEach { other ->
                            if (other is Textfield && other != component) other.setFocused(false)
                        }
                        if (this.openDropdown != null) {
                            this.openDropdown?.close()
                            this.openDropdown = null
                        }
                    } else if (component is Dropdown) {
                        if (component.isOpen) {
                            if (this.openDropdown != null && this.openDropdown != component) {
                                this.openDropdown?.close()
                            }
                            this.openDropdown = component
                            activeTab.components.forEach { other ->
                                if (other is Textfield) other.setFocused(false)
                            }
                        } else {
                            if (this.openDropdown == component) {
                                this.openDropdown = null
                            }
                        }
                    } else {
                        if (this.openDropdown != null) {
                            this.openDropdown?.close()
                            this.openDropdown = null
                        }
                        activeTab.components.forEach { other ->
                            if (other is Textfield) other.setFocused(false)
                        }
                    }
                    return
                }
                component.y = originalLogicalY_comp
            }
        }

        val contentAreaVisualBottom = this.height - 10
        if (!clickedFocusableComponent &&
            mouseX >= componentStartXOffset / 2 && mouseX < this.width - componentStartXOffset / 2 &&
            mouseY >= contentAreaVisualTop && mouseY < contentAreaVisualBottom) {

            activeTab.components.forEach { if (it is Textfield) it.setFocused(false) }
            if (this.openDropdown != null) {
                this.openDropdown?.close()
                this.openDropdown = null
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return

        val contentAreaVisualTop = contentAreaMarginTop

        openDropdown?.let { dd ->
            val oY = dd.y
            dd.y = contentAreaVisualTop + oY - activeTab.scrollY
            dd.mouseReleased(mouseX,mouseY,state)
            dd.y = oY
        }

        activeTab.components.filter{it != openDropdown}.forEach {
            val oY = it.y
            it.y = contentAreaVisualTop + oY - activeTab.scrollY
            it.mouseReleased(mouseX,mouseY,state)
            it.y=oY
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return

        val contentAreaVisualTop = contentAreaMarginTop

        openDropdown?.let { dd ->
            val oY = dd.y
            dd.y = contentAreaVisualTop + oY - activeTab.scrollY
            dd.mouseClickMove(mouseX,mouseY,clickedMouseButton,timeSinceLastClick)
            dd.y = oY
        }

        activeTab.components.filter{it != openDropdown}.forEach {
            if (it is Slider) {
                val oY = it.y
                it.y = contentAreaVisualTop + oY - activeTab.scrollY
                it.mouseClickMove(mouseX,mouseY,clickedMouseButton,timeSinceLastClick)
                it.y=oY
            }
        }
    }


    override fun handleMouseInput() {
        super.handleMouseInput()
        val rawMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth
        val rawMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1
        val dWheel = Mouse.getDWheel()

        if (dWheel != 0) {
            val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return

            val tabsAreaStartX = componentStartXOffset / 2
            val tabsAreaEndX = this.width - tabsAreaStartX
            if (rawMouseY >= tabBarYOffset && rawMouseY < tabBarYOffset + tabBarInternalHeight &&
                rawMouseX >= tabsAreaStartX && rawMouseX < tabsAreaEndX) {
                if (maxTabScrollX > 0) {
                    val scrollAmount = tabButtonWidth + tabButtonSpacing
                    if (dWheel > 0) targetTabScrollX = max(0, targetTabScrollX - scrollAmount)
                    else targetTabScrollX = min(maxTabScrollX, targetTabScrollX + scrollAmount)
                    return
                }
            }

            val contentAreaVisualTop = contentAreaMarginTop

            openDropdown?.let { dd ->
                val oY = dd.y
                dd.y = contentAreaVisualTop + oY - activeTab.scrollY
                val handledByDropdown = dd.handleMouseScroll(rawMouseX,rawMouseY,dWheel)
                dd.y=oY
                if(handledByDropdown) return
            }

            val contentAreaVisualBottom = this.height - 10
            var mainContentScrollAreaXEnd = this.width - componentStartXOffset / 2
            if (activeTab.maxScrollY > 0) {
                mainContentScrollAreaXEnd -= (scrollbarWidth + scrollbarMargin)
            }

            if (rawMouseX >= componentStartXOffset / 2 && rawMouseX < mainContentScrollAreaXEnd &&
                rawMouseY >= contentAreaVisualTop && rawMouseY < contentAreaVisualBottom) {
                if (activeTab.maxScrollY > 0) {
                    val scrollAmount = if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) 70 else 25
                    if (dWheel > 0) activeTab.targetScrollY = max(0, activeTab.targetScrollY - scrollAmount)
                    else activeTab.targetScrollY = min(activeTab.maxScrollY, activeTab.targetScrollY + scrollAmount)
                } else {
                    activeTab.targetScrollY = 0
                }
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null)
            return
        }
        val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return

        val contentAreaVisualTop = contentAreaMarginTop

        for (component in activeTab.components) {
            if (component is Textfield && component.textField.isFocused) {
                val oY = component.y
                component.y = contentAreaVisualTop + oY - activeTab.scrollY
                val handled = component.keyTyped(typedChar,keyCode)
                component.y=oY
                if (handled) return
            }
        }

    }


    override fun onGuiClosed() {
        super.onGuiClosed()
        Keyboard.enableRepeatEvents(false)
        Config.save()
        tabs.forEach { tab ->
            tab.components.forEach { comp ->
                if (comp is Textfield) comp.setFocused(false)
                if (comp is Dropdown) comp.close()
            }
        }
        openDropdown = null
    }

    override fun doesGuiPauseGame(): Boolean = false

    private fun startScissor(x: Int, y: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val sr = net.minecraft.client.gui.ScaledResolution(mc)
        val scale = sr.scaleFactor
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(
            (x * scale).toInt(),
            ((sr.scaledHeight - (y + height)) * scale).toInt(),
            (width * scale).toInt(),
            (height * scale).toInt()
        )
    }

    private fun stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }
}
