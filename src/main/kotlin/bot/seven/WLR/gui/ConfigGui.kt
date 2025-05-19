package bot.seven.WLR.gui

import bot.seven.WLR.core.Config
import bot.seven.WLR.core.ConfigSorter
import bot.seven.WLR.gui.elements.*
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ConfigGui : GuiScreen() {

    private data class Tab(
        val name: String,
        val id: String,
        val icon: ResourceLocation? = null,
        val components: MutableList<GuiComponentBase> = mutableListOf(),
        var scrollY: Float = 0f,
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

    private var tabScrollX: Float = 0f
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

    private var isDraggingContentScrollbar = false
    private var contentScrollbarMouseDragStartY = 0f
    private var contentScrollbarInitialScrollY = 0f

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
    private lateinit var enableSumoDistanceJumpCheckbox: Checkbox
    private lateinit var enableSumoStrafingCheckbox: Checkbox
    private lateinit var sumoStrafeIntensityDropdown: Dropdown

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
    private var labelHeightAboveComponent: Int = 0
    private val contentPaddingTopForComponents = 15
    private val contentPaddingBottomForComponents = 15

    private var openDropdown: Dropdown? = null
    private val allPossibleTabsMap = mutableMapOf<String, Tab>()

    private var nextComponentId = 1
    private fun getNextId(): Int = nextComponentId++


    override fun initGui() {
        super.initGui()
        this.labelHeightAboveComponent = mc.fontRendererObj.FONT_HEIGHT + 3
        nextComponentId = 1

        Keyboard.enableRepeatEvents(true)
        openDropdown = null
        tabScrollX = 0f
        targetTabScrollX = 0
        isDraggingContentScrollbar = false

        try {
            defineAllPossibleTabs()
            orderAndPopulateTabs()

            if (this.tabs.isEmpty()) {
                return
            }
            if (currentTabIndex >= tabs.size || currentTabIndex < 0 || tabs.getOrNull(currentTabIndex) == null) {
                currentTabIndex = 0
            }

            calculateTabScrolling()
            tabs.forEach { tab ->
                calculateContentScrollingForTab(tab)
                tab.scrollY = tab.targetScrollY.toFloat()
            }
            tabScrollX = targetTabScrollX.toFloat()

            updateGuiElementStates()
        } catch (e: Exception) {
            println("Error initializing ConfigGui:")
            e.printStackTrace()
        }
    }

    private fun defineAllPossibleTabs() {
        allPossibleTabsMap.clear()
        allPossibleTabsMap["General"] = Tab(name = "General", id = "General", icon = null)
        allPossibleTabsMap["Combat"] = Tab(name = "Combat", id = "Combat", icon = null)
        allPossibleTabsMap["Requeue"] = Tab(name = "Requeue", id = "Requeue", icon = null)
        allPossibleTabsMap["Messages"] = Tab(name = "Messages", id = "Messages", icon = null)
        allPossibleTabsMap["Boosting"] = Tab(name = "Boosting", id = "Boosting", icon = null)
        allPossibleTabsMap["Webhook"] = Tab(name = "Webhook", id = "Webhook", icon = null)
        allPossibleTabsMap["Replays"] = Tab(name = "Replays", id = "Replays", icon = null)
        allPossibleTabsMap["Camera"] = Tab(name = "Camera", id = "Camera", icon = null)
        allPossibleTabsMap["HUD"] = Tab(name = "HUD", id = "HUD", icon = null)
        allPossibleTabsMap["Misc"] = Tab(name = "Misc", id = "Misc", icon = null)

    }

    private fun orderAndPopulateTabs() {
        this.tabs.clear()

        ConfigSorter.WLR_TAB_ORDER.forEach { tabId ->
            allPossibleTabsMap[tabId]?.let { tabDefinition ->
                tabDefinition.components.clear()
                tabDefinition.scrollY = 0f
                tabDefinition.targetScrollY = 0
                populateComponentsForTab(tabDefinition)
                this.tabs.add(tabDefinition)
            } ?: run {
                println("Warning: Tab ID '$tabId' from ConfigSorter not found in allPossibleTabsMap.")
            }
        }

        allPossibleTabsMap.values.forEach { tabDefinition ->
            if (!this.tabs.any { it.id == tabDefinition.id }) {
                tabDefinition.components.clear()
                tabDefinition.scrollY = 0f
                tabDefinition.targetScrollY = 0
                populateComponentsForTab(tabDefinition)
                this.tabs.add(tabDefinition)
            }
        }
    }


    private fun populateComponentsForTab(tab: Tab) {
        val contentPaneFullWidth = this.width - (componentStartXOffset * 2)
        val availableWidthForComponents = max(0, contentPaneFullWidth - 20)
        val actualComponentWidth = min(this.componentWidth, availableWidthForComponents)
        val startX = componentStartXOffset + max(0, (contentPaneFullWidth - actualComponentWidth) / 2)

        logicalCurrentY = 0
        logicalCurrentY += contentPaddingTopForComponents

        when (tab.id) {
            "General" -> {
                currentBotDropdown = Dropdown(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth,
                    height = MODERN_DROPDOWN_HEIGHT,
                    label = "Current Bot",
                    options = Config.REGULAR_BOT_OPTIONS.toList(),
                    initialSelectedIndex = Config.currentBot,
                    onSelectionChanged = { index, _ ->
                        Config.setCurrentBot(index)
                    }
                )
                tab.components.add(currentBotDropdown)
                logicalCurrentY += labelHeightAboveComponent + currentBotDropdown.height + interComponentSpacing

                lobbyMovementCheckbox = Checkbox(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY,
                    label = "Lobby Movement",
                    initialValue = Config.lobbyMovement,
                    onValueChanged = { newValue -> Config.lobbyMovement = newValue; Config.save() }
                )
                tab.components.add(lobbyMovementCheckbox)
                logicalCurrentY += lobbyMovementCheckbox.height + interComponentSpacing

                disableChatMessagesCheckbox = Checkbox(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY,
                    label = "Disable Chat Messages",
                    initialValue = Config.disableChatMessages,
                    onValueChanged = { newValue -> Config.disableChatMessages = newValue; Config.save() }
                )
                tab.components.add(disableChatMessagesCheckbox)
                logicalCurrentY += disableChatMessagesCheckbox.height + interComponentSpacing

                throwAfterGamesSlider = Slider(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth,
                    height = MODERN_SLIDER_HEIGHT,
                    label = "Throw After X Games",
                    initialValue = Config.throwAfterGames.toFloat(),
                    minValue = 0f,
                    maxValue = 1000f,
                    step = 1f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.throwAfterGames = newValue.toInt(); Config.save() }
                )
                tab.components.add(throwAfterGamesSlider)
                logicalCurrentY += labelHeightAboveComponent + throwAfterGamesSlider.height + interComponentSpacing

                disconnectAfterGamesSlider = Slider(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth,
                    height = MODERN_SLIDER_HEIGHT,
                    label = "Disconnect After X Games",
                    initialValue = Config.disconnectAfterGames.toFloat(),
                    minValue = 0f,
                    maxValue = 10000f,
                    step = 10f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.disconnectAfterGames = newValue.toInt(); Config.save() }
                )
                tab.components.add(disconnectAfterGamesSlider)
                logicalCurrentY += labelHeightAboveComponent + disconnectAfterGamesSlider.height + interComponentSpacing

                disconnectAfterMinutesSlider = Slider(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth,
                    height = MODERN_SLIDER_HEIGHT,
                    label = "Disconnect After X Mins",
                    initialValue = Config.disconnectAfterMinutes.toFloat(),
                    minValue = 0f,
                    maxValue = 500f,
                    step = 5f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.disconnectAfterMinutes = newValue.toInt(); Config.save() }
                )
                tab.components.add(disconnectAfterMinutesSlider)
                logicalCurrentY += labelHeightAboveComponent + disconnectAfterMinutesSlider.height + interComponentSpacing * 2

                val sectionLabelSumo = "Sumo Bot Settings:"
                logicalCurrentY += mc.fontRendererObj.FONT_HEIGHT + interComponentSpacing / 2
            }
            "Boosting" -> {
                enableBoostingModeCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Boosting Mode", initialValue = Config.enableBoostingMode,
                    onValueChanged = { newValue -> Config.setEnableBoostingMode(newValue) }
                )
                tab.components.add(enableBoostingModeCheckbox)
                logicalCurrentY += enableBoostingModeCheckbox.height + interComponentSpacing

                selectedBoostingBotDropdown = Dropdown(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_DROPDOWN_HEIGHT,
                    label = "Selected Boosting Bot", options = Config.BOOSTING_BOT_OPTIONS.toList(),
                    initialSelectedIndex = Config.selectedBoostingBotIndex,
                    onSelectionChanged = { index, _ -> Config.setSelectedBoostingBot(index) }
                )
                tab.components.add(selectedBoostingBotDropdown)
                logicalCurrentY += labelHeightAboveComponent + selectedBoostingBotDropdown.height + interComponentSpacing

                boostingRequeueDelaySlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT,
                    label = "Boosting Requeue Delay", initialValue = Config.boostingRequeueDelay.toFloat(),
                    minValue = 0f, maxValue = 5000f, step = 50f,
                    displayFormat = { value -> "%.0f ms".format(value) },
                    onValueChanged = { newValue -> Config.boostingRequeueDelay = max(50, newValue.toInt()); Config.save() }
                )
                tab.components.add(boostingRequeueDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + boostingRequeueDelaySlider.height
            }
            "Camera" -> {
                enableCustomCameraCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Custom Camera", initialValue = Config.enableCustomCamera,
                    onValueChanged = { newValue -> Config.enableCustomCamera = newValue; Config.save(); updateGuiElementStates() }
                )
                tab.components.add(enableCustomCameraCheckbox)
                logicalCurrentY += enableCustomCameraCheckbox.height + interComponentSpacing

                cameraOffsetXSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Offset X",
                    initialValue = Config.cameraOffsetX, minValue = -10f, maxValue = 10f, step = 0.1f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.cameraOffsetX = newValue; Config.save() }
                )
                tab.components.add(cameraOffsetXSlider)
                logicalCurrentY += labelHeightAboveComponent + cameraOffsetXSlider.height + interComponentSpacing

                cameraOffsetYSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Offset Y",
                    initialValue = Config.cameraOffsetY, minValue = -10f, maxValue = 10f, step = 0.1f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.cameraOffsetY = newValue; Config.save() }
                )
                tab.components.add(cameraOffsetYSlider)
                logicalCurrentY += labelHeightAboveComponent + cameraOffsetYSlider.height + interComponentSpacing

                cameraOffsetZSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Offset Z",
                    initialValue = Config.cameraOffsetZ, minValue = -15f, maxValue = 15f, step = 0.1f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.cameraOffsetZ = newValue; Config.save() }
                )
                tab.components.add(cameraOffsetZSlider)
                logicalCurrentY += labelHeightAboveComponent + cameraOffsetZSlider.height + interComponentSpacing

                cameraPitchSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Pitch",
                    initialValue = Config.cameraPitch, minValue = -90f, maxValue = 90f, step = 0.5f,
                    displayFormat = { value -> "%.1f°".format(value) },
                    onValueChanged = { newValue -> Config.cameraPitch = newValue; Config.save() }
                )
                tab.components.add(cameraPitchSlider)
                logicalCurrentY += labelHeightAboveComponent + cameraPitchSlider.height + interComponentSpacing

                cameraYawSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Yaw",
                    initialValue = Config.cameraYaw, minValue = -180f, maxValue = 180f, step = 0.5f,
                    displayFormat = { value -> "%.1f°".format(value) },
                    onValueChanged = { newValue -> Config.cameraYaw = newValue; Config.save() }
                )
                tab.components.add(cameraYawSlider)
                logicalCurrentY += labelHeightAboveComponent + cameraYawSlider.height + interComponentSpacing

                enableCameraZoomCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Camera Zoom", initialValue = Config.enableCameraZoom,
                    onValueChanged = { newValue -> Config.enableCameraZoom = newValue; Config.save(); updateGuiElementStates() }
                )
                tab.components.add(enableCameraZoomCheckbox)
                logicalCurrentY += enableCameraZoomCheckbox.height + interComponentSpacing

                cameraZoomFovSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Zoom FOV",
                    initialValue = Config.cameraZoomFovValue, minValue = 10f, maxValue = 90f, step = 1f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.cameraZoomFovValue = newValue; Config.save() }
                )
                tab.components.add(cameraZoomFovSlider)
                logicalCurrentY += labelHeightAboveComponent + cameraZoomFovSlider.height
            }
            "Replays" -> {
                enableReplayClearingModeCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Replay Clearing", initialValue = Config.enableReplayClearingMode,
                    onValueChanged = { newValue -> Config.setEnableReplayClearingMode(newValue) }
                )
                tab.components.add(enableReplayClearingModeCheckbox)
                logicalCurrentY += enableReplayClearingModeCheckbox.height + interComponentSpacing

                replayClearingMinDelaySlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Min Delay",
                    initialValue = Config.replayClearingMinDelay.toFloat(), minValue = 500f, maxValue = 20000f, step = 100f,
                    displayFormat = { value -> "%.0f ms".format(value) },
                    onValueChanged = { newValue -> Config.replayClearingMinDelay = newValue.toInt(); Config.save() }
                )
                tab.components.add(replayClearingMinDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + replayClearingMinDelaySlider.height + interComponentSpacing

                replayClearingMaxDelaySlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Max Delay",
                    initialValue = Config.replayClearingMaxDelay.toFloat(), minValue = 500f, maxValue = 20000f, step = 100f,
                    displayFormat = { value -> "%.0f ms".format(value) },
                    onValueChanged = { newValue -> Config.replayClearingMaxDelay = newValue.toInt(); Config.save() }
                )
                tab.components.add(replayClearingMaxDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + replayClearingMaxDelaySlider.height + interComponentSpacing

                replayClearingCommandCountSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Command Count",
                    initialValue = Config.replayClearingCommandCount.toFloat(), minValue = 1f, maxValue = 10000f, step = 1f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.replayClearingCommandCount = newValue.toInt(); Config.save() }
                )
                tab.components.add(replayClearingCommandCountSlider)
                logicalCurrentY += labelHeightAboveComponent + replayClearingCommandCountSlider.height
            }
            "Combat" -> {
                minCPSSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Min CPS",
                    initialValue = Config.minCPS.toFloat(), minValue = 1f, maxValue = 20f, step = 0.5f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.minCPS = newValue.toInt(); Config.save() }
                )
                tab.components.add(minCPSSlider)
                logicalCurrentY += labelHeightAboveComponent + minCPSSlider.height + interComponentSpacing

                maxCPSSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Max CPS",
                    initialValue = Config.maxCPS.toFloat(), minValue = 5f, maxValue = 25f, step = 0.5f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.maxCPS = newValue.toInt(); Config.save() }
                )
                tab.components.add(maxCPSSlider)
                logicalCurrentY += labelHeightAboveComponent + maxCPSSlider.height + interComponentSpacing

                lookSpeedHorizontalSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Horizontal Look Speed",
                    initialValue = Config.lookSpeedHorizontal.toFloat(), minValue = 1f, maxValue = 30f, step = 1f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.lookSpeedHorizontal = newValue.toInt(); Config.save() }
                )
                tab.components.add(lookSpeedHorizontalSlider)
                logicalCurrentY += labelHeightAboveComponent + lookSpeedHorizontalSlider.height + interComponentSpacing

                lookSpeedVerticalSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Vertical Look Speed",
                    initialValue = Config.lookSpeedVertical.toFloat(), minValue = 1f, maxValue = 30f, step = 1f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.lookSpeedVertical = newValue.toInt(); Config.save() }
                )
                tab.components.add(lookSpeedVerticalSlider)
                logicalCurrentY += labelHeightAboveComponent + lookSpeedVerticalSlider.height + interComponentSpacing

                lookRandSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Look Randomization",
                    initialValue = Config.lookRand, minValue = 0f, maxValue = 5f, step = 0.1f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.lookRand = newValue; Config.save() }
                )
                tab.components.add(lookRandSlider)
                logicalCurrentY += labelHeightAboveComponent + lookRandSlider.height + interComponentSpacing

                maxDistanceLookSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Max Look Distance",
                    initialValue = Config.maxDistanceLook.toFloat(), minValue = 3f, maxValue = 150f, step = 1f,
                    displayFormat = { value -> "%.0f".format(value) },
                    onValueChanged = { newValue -> Config.maxDistanceLook = newValue.roundToInt(); Config.save() }
                )
                tab.components.add(maxDistanceLookSlider)
                logicalCurrentY += labelHeightAboveComponent + maxDistanceLookSlider.height + interComponentSpacing

                maxDistanceAttackSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Max Attack Distance",
                    initialValue = Config.maxDistanceAttack.toFloat(), minValue = 3f, maxValue = 8f, step = 0.1f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.maxDistanceAttack = newValue.roundToInt(); Config.save() }
                )
                tab.components.add(maxDistanceAttackSlider)
                logicalCurrentY += labelHeightAboveComponent + maxDistanceAttackSlider.height + interComponentSpacing

                enableComboResetByDistanceCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Combo Reset by Distance", initialValue = Config.enableComboResetByDistance,
                    onValueChanged = { newValue -> Config.enableComboResetByDistance = newValue; Config.save(); updateGuiElementStates() }
                )
                tab.components.add(enableComboResetByDistanceCheckbox)
                logicalCurrentY += enableComboResetByDistanceCheckbox.height + interComponentSpacing

                comboResetDistanceSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Combo Reset Distance",
                    initialValue = Config.comboResetDistance.toFloat(), minValue = 1f, maxValue = 10f, step = 0.1f,
                    displayFormat = { value -> "%.1f".format(value) },
                    onValueChanged = { newValue -> Config.comboResetDistance = newValue.roundToInt(); Config.save() }
                )
                tab.components.add(comboResetDistanceSlider)
                logicalCurrentY += labelHeightAboveComponent + comboResetDistanceSlider.height

                sumoStrafeIntensityDropdown = Dropdown(
                    id = getNextId(),
                    x = startX,
                    y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth,
                    height = MODERN_DROPDOWN_HEIGHT,
                    label = "Sumo Strafe Intensity",
                    options = Config.SumoStrafeIntensity.options,
                    initialSelectedIndex = Config.sumoStrafeIntensity.ordinal,
                    onSelectionChanged = { index, _ ->
                        Config.sumoStrafeIntensity = Config.SumoStrafeIntensity.fromOrdinal(index)
                    }
                )
                tab.components.add(sumoStrafeIntensityDropdown)
                logicalCurrentY += labelHeightAboveComponent + sumoStrafeIntensityDropdown.height + interComponentSpacing

                enableSumoDistanceJumpCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Sumo Distance Jump", initialValue = Config.enableSumoDistanceJump,
                    onValueChanged = { newValue -> Config.enableSumoDistanceJump = newValue }
                )
                tab.components.add(enableSumoDistanceJumpCheckbox)
                logicalCurrentY += enableSumoDistanceJumpCheckbox.height + interComponentSpacing

                enableSumoStrafingCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Sumo Strafing", initialValue = Config.enableSumoStrafing,
                    onValueChanged = { newValue -> Config.enableSumoStrafing = newValue; updateGuiElementStates() }
                )
                tab.components.add(enableSumoStrafingCheckbox)
                logicalCurrentY += enableSumoStrafingCheckbox.height + interComponentSpacing
            }
            "Messages" -> {
                sendAutoGGCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable AutoGG", initialValue = Config.sendAutoGG,
                    onValueChanged = { newValue -> Config.sendAutoGG = newValue; Config.save(); updateGuiElementStates() }
                )
                tab.components.add(sendAutoGGCheckbox)
                logicalCurrentY += sendAutoGGCheckbox.height + interComponentSpacing

                ggMessageTextField = Textfield(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_TEXT_INPUT_HEIGHT, label = "AutoGG Message",
                    initialText = Config.ggMessage,
                    onTextChanged = { newText -> Config.ggMessage = newText },
                    onFocusChanged = { isFocused -> if (!isFocused) Config.save() }
                )
                tab.components.add(ggMessageTextField)
                logicalCurrentY += labelHeightAboveComponent + ggMessageTextField.height + interComponentSpacing

                ggDelaySlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "AutoGG Delay",
                    initialValue = Config.ggDelay.toFloat(), minValue = 0f, maxValue = 2000f, step = 50f,
                    displayFormat = { value -> "%.0f ms".format(value) },
                    onValueChanged = { newValue -> Config.ggDelay = newValue.toInt(); Config.save() }
                )
                tab.components.add(ggDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + ggDelaySlider.height + interComponentSpacing

                sendStartMessageCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Game Start Message", initialValue = Config.sendStartMessage,
                    onValueChanged = { newValue -> Config.sendStartMessage = newValue; Config.save(); updateGuiElementStates() }
                )
                tab.components.add(sendStartMessageCheckbox)
                logicalCurrentY += sendStartMessageCheckbox.height + interComponentSpacing

                startMessageTextField = Textfield(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_TEXT_INPUT_HEIGHT, label = "Start Message",
                    initialText = Config.startMessage,
                    onTextChanged = { newText -> Config.startMessage = newText },
                    onFocusChanged = { isFocused -> if (!isFocused) Config.save() }
                )
                tab.components.add(startMessageTextField)
                logicalCurrentY += labelHeightAboveComponent + startMessageTextField.height + interComponentSpacing

                startMessageDelaySlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Start Message Delay",
                    initialValue = Config.startMessageDelay.toFloat(), minValue = 0f, maxValue = 2000f, step = 50f,
                    displayFormat = { value -> "%.0f ms".format(value) },
                    onValueChanged = { newValue -> Config.startMessageDelay = newValue.toInt(); Config.save() }
                )
                tab.components.add(startMessageDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + startMessageDelaySlider.height
            }
            "Requeue" -> {
                autoRqDelaySlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Requeue Delay",
                    initialValue = Config.autoRqDelay.toFloat(), minValue = 0f, maxValue = 5000f, step = 50f,
                    displayFormat = { value -> "%.0f ms".format(value) },
                    onValueChanged = { newValue -> Config.autoRqDelay = newValue.toInt(); Config.save() }
                )
                tab.components.add(autoRqDelaySlider)
                logicalCurrentY += labelHeightAboveComponent + autoRqDelaySlider.height + interComponentSpacing

                rqNoGameSlider = Slider(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_SLIDER_HEIGHT, label = "Requeue No Game Timer",
                    initialValue = Config.rqNoGame.toFloat(), minValue = 5f, maxValue = 120f, step = 1f,
                    displayFormat = { value -> "%.0f s".format(value) },
                    onValueChanged = { newValue -> Config.rqNoGame = newValue.toInt(); Config.save() }
                )
                tab.components.add(rqNoGameSlider)
                logicalCurrentY += labelHeightAboveComponent + rqNoGameSlider.height + interComponentSpacing

                paperRequeueCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Paper Requeue", initialValue = Config.paperRequeue,
                    onValueChanged = { newValue -> Config.paperRequeue = newValue; Config.save() }
                )
                tab.components.add(paperRequeueCheckbox)
                logicalCurrentY += paperRequeueCheckbox.height + interComponentSpacing

                fastRequeueCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Fast Requeue", initialValue = Config.fastRequeue,
                    onValueChanged = { newValue -> Config.fastRequeue = newValue; Config.save() }
                )
                tab.components.add(fastRequeueCheckbox)
                logicalCurrentY += fastRequeueCheckbox.height
            }
            "Webhook" -> {
                sendWebhookMessagesCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Enable Webhook", initialValue = Config.sendWebhookMessages,
                    onValueChanged = { newValue -> Config.sendWebhookMessages = newValue; Config.save(); updateGuiElementStates() }
                )
                tab.components.add(sendWebhookMessagesCheckbox)
                logicalCurrentY += sendWebhookMessagesCheckbox.height + interComponentSpacing

                webhookURLTextField = Textfield(
                    id = getNextId(), x = startX, y = logicalCurrentY + labelHeightAboveComponent,
                    width = actualComponentWidth, height = MODERN_TEXT_INPUT_HEIGHT, label = "Webhook URL",
                    initialText = Config.webhookURL,
                    onTextChanged = { newText -> Config.webhookURL = newText },
                    onFocusChanged = { isFocused -> if (!isFocused) Config.save() }
                )
                tab.components.add(webhookURLTextField)
                logicalCurrentY += labelHeightAboveComponent + webhookURLTextField.height
            }
            "Misc" -> {
                boxingFishCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Boxing Fish (Visual)", initialValue = Config.boxingFish,
                    onValueChanged = { newValue -> Config.boxingFish = newValue; Config.save() }
                )
                tab.components.add(boxingFishCheckbox)
                logicalCurrentY += boxingFishCheckbox.height
            }
            "HUD" -> {
                sessionStatsHUDCheckbox = Checkbox(
                    id = getNextId(), x = startX, y = logicalCurrentY,
                    label = "Session Stats HUD", initialValue = Config.sessionStatsHUD,
                    onValueChanged = { newValue -> Config.sessionStatsHUD = newValue; Config.save() }
                )
                tab.components.add(sessionStatsHUDCheckbox)
                logicalCurrentY += sessionStatsHUDCheckbox.height
            }
        }
        tab.contentHeight = (logicalCurrentY - contentPaddingTopForComponents) + contentPaddingBottomForComponents
        calculateContentScrollingForTab(tab)
    }

    private fun calculateTabScrolling() {
        if (tabs.isEmpty()) {
            totalTabsWidthUnscrolled = 0
            this.visibleTabBarAreaWidth = 0
            maxTabScrollX = 0
            return
        }
        totalTabsWidthUnscrolled = tabs.sumOf { tabButtonWidth + tabButtonSpacing }
        if (tabs.isNotEmpty()) {
            totalTabsWidthUnscrolled -= tabButtonSpacing
        }

        val tabBarContainerWidth = this.width - componentStartXOffset
        val needsScrolling = totalTabsWidthUnscrolled > tabBarContainerWidth && tabs.size > 1

        if (needsScrolling) {
            this.visibleTabBarAreaWidth = tabBarContainerWidth - (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
        } else {
            this.visibleTabBarAreaWidth = tabBarContainerWidth
        }
        maxTabScrollX = max(0, totalTabsWidthUnscrolled - this.visibleTabBarAreaWidth)

        targetTabScrollX = targetTabScrollX.coerceIn(0, maxTabScrollX)
        tabScrollX = tabScrollX.coerceIn(0f, maxTabScrollX.toFloat())
    }


    private fun calculateContentScrollingForTab(tab: Tab) {
        val contentAreaDrawableHeight = this.height - contentAreaMarginTop - 20
        tab.maxScrollY = max(0, tab.contentHeight - contentAreaDrawableHeight)
        tab.targetScrollY = tab.targetScrollY.coerceIn(0, tab.maxScrollY)
        tab.scrollY = tab.scrollY.coerceIn(0f, tab.maxScrollY.toFloat())
    }

    fun updateGuiElementStates() {
        if (tabs.isEmpty() || !::currentBotDropdown.isInitialized) return

        val isSumoBotActive = Config.currentBot == Config.sumoBotIndex && !Config.enableBoostingMode && !Config.enableReplayClearingMode
        val isBoostingEnabled = Config.enableBoostingMode
        val isReplayClearingEnabled = Config.enableReplayClearingMode
        val isCustomCameraEnabled = Config.enableCustomCamera
        val isCameraZoomEnabled = Config.enableCameraZoom && isCustomCameraEnabled
        val isAutoGGEnabled = Config.sendAutoGG
        val isStartMessageEnabled = Config.sendStartMessage
        val isWebhookEnabled = Config.sendWebhookMessages
        val isComboResetEnabled = Config.enableComboResetByDistance
        val isSumoStrafingEnabled = Config.enableSumoStrafing


        tabs.forEach { tab ->
            tab.components.forEach { component ->
                component.enabled = true

                if (::currentBotDropdown.isInitialized && component == currentBotDropdown) {
                    component.enabled = !isBoostingEnabled && !isReplayClearingEnabled
                } else if (::lobbyMovementCheckbox.isInitialized && component == lobbyMovementCheckbox) {
                } else if (::disableChatMessagesCheckbox.isInitialized && component == disableChatMessagesCheckbox) {
                } else if (::throwAfterGamesSlider.isInitialized && component == throwAfterGamesSlider) {
                } else if (::disconnectAfterGamesSlider.isInitialized && component == disconnectAfterGamesSlider) {
                } else if (::disconnectAfterMinutesSlider.isInitialized && component == disconnectAfterMinutesSlider) {
                }
                else if (::enableSumoDistanceJumpCheckbox.isInitialized && component == enableSumoDistanceJumpCheckbox) {
                    component.enabled = isSumoBotActive
                } else if (::enableSumoStrafingCheckbox.isInitialized && component == enableSumoStrafingCheckbox) {
                    component.enabled = isSumoBotActive
                } else if (::sumoStrafeIntensityDropdown.isInitialized && component == sumoStrafeIntensityDropdown) {
                    component.enabled = isSumoBotActive && isSumoStrafingEnabled
                }


                else if (::enableBoostingModeCheckbox.isInitialized && component == enableBoostingModeCheckbox) {
                }
                else if (::selectedBoostingBotDropdown.isInitialized && component == selectedBoostingBotDropdown) {
                    component.enabled = isBoostingEnabled
                } else if (::boostingRequeueDelaySlider.isInitialized && component == boostingRequeueDelaySlider) {
                    component.enabled = isBoostingEnabled
                }

                else if (::enableCustomCameraCheckbox.isInitialized && component == enableCustomCameraCheckbox) {
                }
                else if (::cameraOffsetXSlider.isInitialized && component == cameraOffsetXSlider) {
                    component.enabled = isCustomCameraEnabled
                } else if (::cameraOffsetYSlider.isInitialized && component == cameraOffsetYSlider) {
                    component.enabled = isCustomCameraEnabled
                } else if (::cameraOffsetZSlider.isInitialized && component == cameraOffsetZSlider) {
                    component.enabled = isCustomCameraEnabled
                } else if (::cameraPitchSlider.isInitialized && component == cameraPitchSlider) {
                    component.enabled = isCustomCameraEnabled
                } else if (::cameraYawSlider.isInitialized && component == cameraYawSlider) {
                    component.enabled = isCustomCameraEnabled
                } else if (::enableCameraZoomCheckbox.isInitialized && component == enableCameraZoomCheckbox) {
                    component.enabled = isCustomCameraEnabled
                } else if (::cameraZoomFovSlider.isInitialized && component == cameraZoomFovSlider) {
                    component.enabled = isCameraZoomEnabled
                }

                else if (::enableReplayClearingModeCheckbox.isInitialized && component == enableReplayClearingModeCheckbox) {
                }
                else if (::replayClearingMinDelaySlider.isInitialized && component == replayClearingMinDelaySlider) {
                    component.enabled = isReplayClearingEnabled
                } else if (::replayClearingMaxDelaySlider.isInitialized && component == replayClearingMaxDelaySlider) {
                    component.enabled = isReplayClearingEnabled
                } else if (::replayClearingCommandCountSlider.isInitialized && component == replayClearingCommandCountSlider) {
                    component.enabled = isReplayClearingEnabled
                }

                else if (::minCPSSlider.isInitialized && component == minCPSSlider) {
                } else if (::maxCPSSlider.isInitialized && component == maxCPSSlider) {
                } else if (::lookSpeedHorizontalSlider.isInitialized && component == lookSpeedHorizontalSlider) {
                } else if (::lookSpeedVerticalSlider.isInitialized && component == lookSpeedVerticalSlider) {
                } else if (::lookRandSlider.isInitialized && component == lookRandSlider) {
                } else if (::maxDistanceLookSlider.isInitialized && component == maxDistanceLookSlider) {
                } else if (::maxDistanceAttackSlider.isInitialized && component == maxDistanceAttackSlider) {
                } else if (::enableComboResetByDistanceCheckbox.isInitialized && component == enableComboResetByDistanceCheckbox) {
                } else if (::comboResetDistanceSlider.isInitialized && component == comboResetDistanceSlider) {
                    component.enabled = isComboResetEnabled
                }

                else if (::sendAutoGGCheckbox.isInitialized && component == sendAutoGGCheckbox) {
                } else if (::ggMessageTextField.isInitialized && component == ggMessageTextField) {
                    component.enabled = isAutoGGEnabled
                } else if (::ggDelaySlider.isInitialized && component == ggDelaySlider) {
                    component.enabled = isAutoGGEnabled
                } else if (::sendStartMessageCheckbox.isInitialized && component == sendStartMessageCheckbox) {
                } else if (::startMessageTextField.isInitialized && component == startMessageTextField) {
                    component.enabled = isStartMessageEnabled
                } else if (::startMessageDelaySlider.isInitialized && component == startMessageDelaySlider) {
                    component.enabled = isStartMessageEnabled
                }

                else if (::autoRqDelaySlider.isInitialized && component == autoRqDelaySlider) {
                } else if (::rqNoGameSlider.isInitialized && component == rqNoGameSlider) {
                } else if (::paperRequeueCheckbox.isInitialized && component == paperRequeueCheckbox) {
                } else if (::fastRequeueCheckbox.isInitialized && component == fastRequeueCheckbox) {
                }

                else if (::sendWebhookMessagesCheckbox.isInitialized && component == sendWebhookMessagesCheckbox) {
                } else if (::webhookURLTextField.isInitialized && component == webhookURLTextField) {
                    component.enabled = isWebhookEnabled
                }

                else if (::boxingFishCheckbox.isInitialized && component == boxingFishCheckbox) {
                }

                else if (::sessionStatsHUDCheckbox.isInitialized && component == sessionStatsHUDCheckbox) {
                }
            }
        }
        calculateContentScrollingForTab(currentTab())
    }


    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val activeTab = currentTab()
        if (activeTab.id != "error_no_tabs") {
            if (isDraggingContentScrollbar) {
                activeTab.scrollY = activeTab.targetScrollY.toFloat().coerceIn(0f, activeTab.maxScrollY.toFloat())
            } else {
                val scrollYDiff = activeTab.targetScrollY - activeTab.scrollY
                if (abs(scrollYDiff) > 0.1f) {
                    var scrollYStep = (scrollYDiff * SCROLL_SMOOTHING_FACTOR)
                    if (abs(scrollYStep) < 1f && scrollYDiff.toInt() != 0) scrollYStep = if (scrollYDiff > 0) 1f else -1f
                    activeTab.scrollY += scrollYStep
                    if ((scrollYDiff > 0 && activeTab.scrollY >= activeTab.targetScrollY) || (scrollYDiff < 0 && activeTab.scrollY <= activeTab.targetScrollY) || abs(activeTab.scrollY - activeTab.targetScrollY) < 2) {
                        activeTab.scrollY = activeTab.targetScrollY.toFloat()
                    }
                } else {
                    activeTab.scrollY = activeTab.targetScrollY.toFloat()
                }
                activeTab.scrollY = activeTab.scrollY.coerceIn(0f, activeTab.maxScrollY.toFloat())
            }

            val tabScrollXDiff = targetTabScrollX - tabScrollX
            if (abs(tabScrollXDiff) > 0.1f) {
                var tabScrollXStep = (tabScrollXDiff * SCROLL_SMOOTHING_FACTOR)
                if (abs(tabScrollXStep) < 1f && tabScrollXDiff.toInt() != 0) tabScrollXStep = if (tabScrollXDiff > 0) 1f else -1f
                tabScrollX += tabScrollXStep
                if ((tabScrollXDiff > 0 && tabScrollX >= targetTabScrollX) || (tabScrollXDiff < 0 && tabScrollX <= targetTabScrollX) || abs(tabScrollX - targetTabScrollX) < 2) {
                    tabScrollX = targetTabScrollX.toFloat()
                }
            } else {
                tabScrollX = targetTabScrollX.toFloat()
            }
            tabScrollX = tabScrollX.coerceIn(0f, maxTabScrollX.toFloat())
        }


        Gui.drawRect(0, 0, this.width, this.height, GuiColors.SCREEN_BACKGROUND)
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO); GlStateManager.disableLighting()

        Gui.drawRect(0, 0, this.width, titleBarHeight, GuiColors.TITLE_BAR_BACKGROUND)
        Gui.drawRect(0, titleBarHeight - 1, this.width, titleBarHeight, GuiColors.TITLE_BAR_SEPARATOR)
        drawCenteredString(fontRendererObj, guiTitle, this.width / 2, (titleBarHeight - fontRendererObj.FONT_HEIGHT) / 2, GuiColors.TITLE_BAR_TEXT)

        Gui.drawRect(0, tabBarYOffset, this.width, tabBarYOffset + tabBarInternalHeight, GuiColors.TAB_BAR_BACKGROUND)

        val tabsInitialRenderX = componentStartXOffset / 2
        var tabsViewportStartX = tabsInitialRenderX
        var localVisibleTabBarAreaWidth = this.width - (tabsInitialRenderX * 2)
        val needsTabBarScrollButtons = totalTabsWidthUnscrolled > localVisibleTabBarAreaWidth && tabs.size > 1

        if (needsTabBarScrollButtons) {
            val buttonY = tabBarYOffset + (tabBarInternalHeight - tabBarScrollButtonHeight) / 2
            tabsViewportStartX += tabBarScrollButtonWidth + tabButtonSpacing
            localVisibleTabBarAreaWidth -= (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)

            val scrollLeftX = tabsInitialRenderX
            val scrollLeftHover = mouseX >= scrollLeftX && mouseX < scrollLeftX + tabBarScrollButtonWidth && mouseY >= buttonY && mouseY < buttonY + tabBarScrollButtonHeight
            GuiDrawingUtils.drawRoundedRectWithBorder(scrollLeftX.toFloat(), buttonY.toFloat(), tabBarScrollButtonWidth.toFloat(), tabBarScrollButtonHeight.toFloat(), 2f, if(scrollLeftHover) GuiColors.TAB_SCROLL_BUTTON_HOVER_BG else GuiColors.TAB_SCROLL_BUTTON_BG, GuiColors.COMPONENT_BORDER, 1f)
            drawCenteredString(fontRendererObj, "<", scrollLeftX + tabBarScrollButtonWidth / 2, buttonY + (tabBarScrollButtonHeight - fontRendererObj.FONT_HEIGHT) / 2, if (tabScrollX > 0f || targetTabScrollX > 0) GuiColors.TAB_SCROLL_BUTTON_ARROW else GuiColors.TEXT_DISABLED)

            val scrollRightX = tabsInitialRenderX + tabBarScrollButtonWidth + tabButtonSpacing + localVisibleTabBarAreaWidth + tabButtonSpacing
            val scrollRightHover = mouseX >= scrollRightX && mouseX < scrollRightX + tabBarScrollButtonWidth && mouseY >= buttonY && mouseY < buttonY + tabBarScrollButtonHeight
            GuiDrawingUtils.drawRoundedRectWithBorder(scrollRightX.toFloat(), buttonY.toFloat(), tabBarScrollButtonWidth.toFloat(), tabBarScrollButtonHeight.toFloat(), 2f, if(scrollRightHover) GuiColors.TAB_SCROLL_BUTTON_HOVER_BG else GuiColors.TAB_SCROLL_BUTTON_BG, GuiColors.COMPONENT_BORDER, 1f)
            drawCenteredString(fontRendererObj, ">", scrollRightX + tabBarScrollButtonWidth / 2, buttonY + (tabBarScrollButtonHeight - fontRendererObj.FONT_HEIGHT) / 2, if (tabScrollX < maxTabScrollX || targetTabScrollX < maxTabScrollX) GuiColors.TAB_SCROLL_BUTTON_ARROW else GuiColors.TEXT_DISABLED)
        }

        val tabButtonVisualY = tabBarYOffset + (tabBarInternalHeight - tabBarButtonHeight) / 2
        startScissor(tabsViewportStartX, tabButtonVisualY, localVisibleTabBarAreaWidth, tabBarButtonHeight)
        var currentTabButtonVisualX = tabsViewportStartX - tabScrollX
        tabs.forEachIndexed { index, tab ->
            if (currentTabButtonVisualX + tabButtonWidth > tabsViewportStartX - (tabButtonWidth + tabButtonSpacing) && currentTabButtonVisualX < tabsViewportStartX + localVisibleTabBarAreaWidth + (tabButtonWidth + tabButtonSpacing) ) {
                val isSelected = index == currentTabIndex
                val tabHovered = mouseX >= currentTabButtonVisualX && mouseX < currentTabButtonVisualX + tabButtonWidth &&
                        mouseY >= tabButtonVisualY && mouseY < tabButtonVisualY + tabBarButtonHeight

                val tabBgColor = when {
                    isSelected -> GuiColors.TAB_BUTTON_BACKGROUND_ACTIVE
                    tabHovered -> GuiColors.TAB_BUTTON_BACKGROUND_HOVER
                    else -> GuiColors.TAB_BUTTON_BACKGROUND_INACTIVE
                }
                val textColor = when {
                    isSelected -> GuiColors.TAB_BUTTON_TEXT_ACTIVE
                    tabHovered -> GuiColors.TAB_BUTTON_TEXT_HOVER
                    else -> GuiColors.TAB_BUTTON_TEXT_INACTIVE
                }

                GuiDrawingUtils.drawRoundedRectWithBorder(
                    currentTabButtonVisualX.toFloat(), tabButtonVisualY.toFloat(),
                    tabButtonWidth.toFloat(), tabBarButtonHeight.toFloat(),
                    3f,
                    tabBgColor,
                    GuiColors.TAB_BAR_BORDER,
                    1f
                )
                if (isSelected) {
                    Gui.drawRect(currentTabButtonVisualX.toInt() + 3, tabButtonVisualY + tabBarButtonHeight - 2, currentTabButtonVisualX.toInt() + tabButtonWidth - 3, tabButtonVisualY + tabBarButtonHeight -1, GuiColors.PRIMARY_RED_BRIGHT)
                }

                val textY = tabButtonVisualY + (tabBarButtonHeight - fontRendererObj.FONT_HEIGHT) / 2
                tab.icon?.let {
                    drawCenteredString(fontRendererObj, tab.name, currentTabButtonVisualX.toInt() + tabButtonWidth / 2, textY, textColor)
                } ?: run {
                    drawCenteredString(fontRendererObj, tab.name, currentTabButtonVisualX.toInt() + tabButtonWidth / 2, textY, textColor)
                }
            }
            currentTabButtonVisualX += tabButtonWidth + tabButtonSpacing
        }
        stopScissor()
        Gui.drawRect(0, tabBarYOffset + tabBarInternalHeight, this.width, tabBarYOffset + tabBarInternalHeight + 1, GuiColors.TITLE_BAR_SEPARATOR)


        val contentAreaVisualTop = contentAreaMarginTop
        val contentAreaVisualBottom = this.height - 10
        val contentAreaDrawableHeight = contentAreaVisualBottom - contentAreaVisualTop

        GuiDrawingUtils.drawRoundedRectWithBorder(
            (componentStartXOffset / 2).toFloat(), contentAreaVisualTop.toFloat(),
            (this.width - componentStartXOffset).toFloat(), contentAreaDrawableHeight.toFloat(),
            3f, GuiColors.MODERN_SECONDARY_BACKGROUND, GuiColors.COMPONENT_BORDER, 1f
        )

        var mainContentScissorWidth = this.width - (componentStartXOffset)
        if (activeTab.id != "error_no_tabs" && activeTab.maxScrollY > 0) {
            mainContentScissorWidth -= (scrollbarWidth + scrollbarMargin + 2)
        }
        startScissor(componentStartXOffset / 2 + 1, contentAreaVisualTop + 1, mainContentScissorWidth -2 , contentAreaDrawableHeight - 2)

        if (activeTab.id != "error_no_tabs") {
            activeTab.components.forEach { component ->
                val originalLogicalY = component.y
                val componentScreenY = contentAreaVisualTop + originalLogicalY - activeTab.scrollY.toInt()

                if (componentScreenY + component.height >= contentAreaVisualTop && componentScreenY <= contentAreaVisualBottom) {
                    component.y = componentScreenY
                    if (!(component is Dropdown && component.isOpen)) {
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

            GuiDrawingUtils.drawRoundedRect(
                scrollBarActualX.toFloat(),
                scrollBarTrackY.toFloat(),
                scrollbarWidth.toFloat(),
                scrollBarTrackHeight.toFloat(),
                3f,
                GuiColors.SCROLLBAR_BG
            )

            if (activeTab.contentHeight > contentAreaDrawableHeight) {
                val thumbHeightRatio = (contentAreaDrawableHeight.toFloat() / activeTab.contentHeight.toFloat()).coerceIn(0.05f, 1f)
                val thumbHeight = max(20, (scrollBarTrackHeight * thumbHeightRatio).toInt())

                val thumbYRatio = if (activeTab.maxScrollY > 0) activeTab.scrollY / activeTab.maxScrollY.toFloat() else 0f
                val thumbYPos = scrollBarTrackY + ((scrollBarTrackHeight - thumbHeight) * thumbYRatio).toInt()

                val thumbHovered = (mouseX >= scrollBarActualX && mouseX < scrollBarActualX + scrollbarWidth &&
                        mouseY >= thumbYPos && mouseY < thumbYPos + thumbHeight) || isDraggingContentScrollbar

                GuiDrawingUtils.drawRoundedRect(
                    (scrollBarActualX + 1f),
                    thumbYPos.toFloat().coerceIn(scrollBarTrackY.toFloat(), (scrollBarTrackY + scrollBarTrackHeight - thumbHeight).toFloat()),
                    (scrollbarWidth - 2f),
                    thumbHeight.toFloat(),
                    3f,
                    if (thumbHovered) GuiColors.MODERN_SCROLLBAR_THUMB_HOVER else GuiColors.SCROLLBAR_THUMB
                )
            }
        }

        openDropdown?.let { dd ->
            val originalLogicalY_dd = dd.y
            val dropdownScreenY = contentAreaVisualTop + originalLogicalY_dd - activeTab.scrollY.toInt()
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
        val localVisibleTabBarAreaWidthFull = this.width - componentStartXOffset
        val needsTabBarScrollButtons = totalTabsWidthUnscrolled > localVisibleTabBarAreaWidthFull && tabs.size > 1
        val tabButtonActualY = tabBarYOffset + (tabBarInternalHeight - tabBarButtonHeight) / 2

        if (needsTabBarScrollButtons) {
            val scrollButtonY = tabBarYOffset + (tabBarInternalHeight - tabBarScrollButtonHeight) / 2
            val scrollLeftX = tabsInitialRenderX
            if (mouseX >= scrollLeftX && mouseX < scrollLeftX + tabBarScrollButtonWidth &&
                mouseY >= scrollButtonY && mouseY < scrollButtonY + tabBarScrollButtonHeight) {
                targetTabScrollX = max(0, targetTabScrollX - (tabButtonWidth + tabButtonSpacing))
                mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 0.7F))
                return
            }

            val actualTabBarViewportWidthForButtons = localVisibleTabBarAreaWidthFull - (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
            val scrollRightX = tabsInitialRenderX + tabBarScrollButtonWidth + tabButtonSpacing + actualTabBarViewportWidthForButtons + tabButtonSpacing
            if (mouseX >= scrollRightX && mouseX < scrollRightX + tabBarScrollButtonWidth &&
                mouseY >= scrollButtonY && mouseY < scrollButtonY + tabBarScrollButtonHeight) {
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

        var currentTabButtonVisualX = localTabsViewportStartX - tabScrollX.toInt()
        tabs.forEachIndexed { index, tab ->
            if (mouseX >= currentTabButtonVisualX && mouseX < currentTabButtonVisualX + tabButtonWidth &&
                mouseY >= tabButtonActualY && mouseY < tabButtonActualY + tabBarButtonHeight &&
                mouseX >= localTabsViewportStartX && mouseX < localTabsViewportStartX + actualClickableTabBarWidth) {
                if (currentTabIndex != index) {
                    currentTab().components.forEach { comp ->
                        if (comp is Dropdown) comp.close()
                        if (comp is Textfield) comp.setFocused(false)
                    }
                    openDropdown = null
                    isDraggingContentScrollbar = false

                    currentTabIndex = index
                    currentTab().targetScrollY = 0
                    currentTab().scrollY = 0f
                    mc.soundHandler.playSound(net.minecraft.client.audio.PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 0.9F))
                    updateGuiElementStates()
                }
                return
            }
            currentTabButtonVisualX += tabButtonWidth + tabButtonSpacing
        }

        var activeTab = currentTab()
        if (activeTab.id == "error_no_tabs") return

        val contentAreaVisualTop = contentAreaMarginTop

        if (this.openDropdown != null) {
            val dd = this.openDropdown!!
            val originalLogicalY_dd = dd.y
            val dropdownScreenY = contentAreaVisualTop + originalLogicalY_dd - activeTab.scrollY.toInt()
            dd.y = dropdownScreenY

            if (dd.mouseClicked(mouseX, mouseY, mouseButton)) {
                dd.y = originalLogicalY_dd
                if (!dd.isOpen) {
                    this.openDropdown = null
                }
                updateGuiElementStates()
                return
            }
            val dropdownExpandedListHeight = if (dd.isOpen) dd.options.take(dd.maxDisplayableOptions).size * dd.optionHeight else 0
            val dropdownClickableHeight = dd.height + dropdownExpandedListHeight
            val clickInsideExpandedDropdown = mouseX >= dd.x && mouseX < dd.x + dd.width &&
                    mouseY >= dropdownScreenY && mouseY < dropdownScreenY + dropdownClickableHeight
            dd.y = originalLogicalY_dd
            if (!clickInsideExpandedDropdown) {
                dd.close()
                this.openDropdown = null
            } else {
                return
            }
        }

        var clickedFocusableComponentThisTurn = false
        for (component in activeTab.components.asReversed()) {
            if (component == openDropdown) continue
            val originalLogicalY_comp = component.y
            val componentScreenY = contentAreaVisualTop + originalLogicalY_comp - activeTab.scrollY.toInt()
            val contentAreaVisualBottom = this.height - 10
            val clickInContentAreaBounds = mouseX >= componentStartXOffset / 2 && mouseX < this.width - componentStartXOffset / 2 &&
                    mouseY >= contentAreaVisualTop && mouseY < contentAreaVisualBottom

            if (component.enabled && clickInContentAreaBounds &&
                mouseX >= component.x && mouseX < component.x + component.width &&
                mouseY >= componentScreenY && mouseY < componentScreenY + component.height) {
                component.y = componentScreenY
                val handledByComponent = component.mouseClicked(mouseX, mouseY, mouseButton)
                component.y = originalLogicalY_comp
                if (handledByComponent) {
                    clickedFocusableComponentThisTurn = true
                    if (component is Textfield) {
                        activeTab.components.filterIsInstance<Textfield>().filter { it != component }.forEach { it.setFocused(false) }
                        this.openDropdown?.close(); this.openDropdown = null
                    } else if (component is Dropdown) {
                        if (component.isOpen) {
                            if (this.openDropdown != null && this.openDropdown != component) this.openDropdown?.close()
                            this.openDropdown = component
                            activeTab.components.filterIsInstance<Textfield>().forEach { it.setFocused(false) }
                        } else {
                            if (this.openDropdown == component) this.openDropdown = null
                        }
                    } else {
                        activeTab.components.filterIsInstance<Textfield>().forEach { it.setFocused(false) }
                        this.openDropdown?.close(); this.openDropdown = null
                    }
                    updateGuiElementStates()
                    return
                }
            }
        }

        activeTab = currentTab()
        if (activeTab.id != "error_no_tabs" && activeTab.maxScrollY > 0 && mouseButton == 0) {
            val contentAreaVisualBottom = this.height - 10
            val contentAreaDrawableHeight = contentAreaVisualBottom - contentAreaVisualTop

            val scrollBarActualX = this.width - componentStartXOffset / 2 - scrollbarMargin - scrollbarWidth
            val scrollBarTrackY = contentAreaVisualTop + 2
            val scrollBarTrackHeight = contentAreaDrawableHeight - 4

            if (activeTab.contentHeight > contentAreaDrawableHeight) {
                val thumbHeightRatio = (contentAreaDrawableHeight.toFloat() / activeTab.contentHeight.toFloat()).coerceIn(0.05f, 1f)
                val thumbHeight = max(20, (scrollBarTrackHeight * thumbHeightRatio).toInt())
                val thumbYRatio = if (activeTab.maxScrollY > 0) activeTab.scrollY / activeTab.maxScrollY.toFloat() else 0f
                val thumbYPos = scrollBarTrackY + ((scrollBarTrackHeight - thumbHeight) * thumbYRatio).toInt()

                if (mouseX >= scrollBarActualX && mouseX < scrollBarActualX + scrollbarWidth &&
                    mouseY >= thumbYPos && mouseY < thumbYPos + thumbHeight) {

                    isDraggingContentScrollbar = true
                    contentScrollbarMouseDragStartY = mouseY.toFloat()
                    contentScrollbarInitialScrollY = activeTab.scrollY
                    return
                }
            }
        }


        val contentAreaVisualBottom = this.height - 10
        val clickInContentArea = mouseX >= componentStartXOffset / 2 && mouseX < this.width - componentStartXOffset / 2 &&
                mouseY >= contentAreaVisualTop && mouseY < contentAreaVisualBottom
        if (!clickedFocusableComponentThisTurn && clickInContentArea) {
            activeTab.components.filterIsInstance<Textfield>().forEach { it.setFocused(false) }
            this.openDropdown?.close(); this.openDropdown = null
        }
    }


    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state == 0 && isDraggingContentScrollbar) {
            isDraggingContentScrollbar = false
        }

        super.mouseReleased(mouseX, mouseY, state)

        val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return
        val contentAreaVisualTop = contentAreaMarginTop

        openDropdown?.let { dd ->
            val oY = dd.y
            dd.y = contentAreaVisualTop + oY - activeTab.scrollY.toInt()
            dd.mouseReleased(mouseX, mouseY, state)
            dd.y = oY
        }

        activeTab.components.filter { it != openDropdown }.forEach { component ->
            if (component.enabled && (component is Slider || state == 0)) {
                val oY = component.y
                component.y = contentAreaVisualTop + oY - activeTab.scrollY.toInt()
                component.mouseReleased(mouseX, mouseY, state)
                component.y = oY
            }
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (isDraggingContentScrollbar && clickedMouseButton == 0) {
            val activeTab = currentTab()
            if (activeTab.id == "error_no_tabs" || activeTab.maxScrollY <= 0) {
                isDraggingContentScrollbar = false
            } else {
                val contentAreaVisualTop = contentAreaMarginTop
                val contentAreaVisualBottom = this.height - 10
                val contentAreaDrawableHeight = contentAreaVisualBottom - contentAreaVisualTop
                val scrollBarTrackHeight = contentAreaDrawableHeight - 4

                val thumbHeightRatio = (contentAreaDrawableHeight.toFloat() / activeTab.contentHeight.toFloat()).coerceIn(0.05f, 1f)
                val thumbHeight = max(20, (scrollBarTrackHeight * thumbHeightRatio).toInt())

                val scrollablePixelRangeForThumb = scrollBarTrackHeight - thumbHeight

                if (scrollablePixelRangeForThumb <= 0) {
                    isDraggingContentScrollbar = false
                } else {
                    val mouseYDelta = mouseY.toFloat() - contentScrollbarMouseDragStartY
                    val scrollUnitsPerPixel = activeTab.maxScrollY.toFloat() / scrollablePixelRangeForThumb.toFloat()
                    val scrollYChange = mouseYDelta * scrollUnitsPerPixel
                    val newScrollY = contentScrollbarInitialScrollY + scrollYChange

                    activeTab.scrollY = newScrollY.coerceIn(0f, activeTab.maxScrollY.toFloat())
                    activeTab.targetScrollY = activeTab.scrollY.roundToInt()

                    return
                }
            }
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return
        val contentAreaVisualTop = contentAreaMarginTop

        openDropdown?.let { dd ->
            val oY = dd.y
            dd.y = contentAreaVisualTop + oY - activeTab.scrollY.toInt()
            dd.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
            dd.y = oY
        }
        activeTab.components.filter { it != openDropdown && it is Slider && it.enabled }.forEach { component ->
            val oY = component.y
            component.y = contentAreaVisualTop + oY - activeTab.scrollY.toInt()
            component.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
            component.y = oY
        }
    }


    override fun handleMouseInput() {
        super.handleMouseInput()
        val rawMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth
        val rawMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1
        val dWheel = Mouse.getDWheel()

        if (dWheel != 0) {
            if (isDraggingContentScrollbar) return

            val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return

            val contentAreaVisualTop = contentAreaMarginTop
            val contentAreaVisualBottom = this.height - 10
            val mainContentScrollAreaXEnd = this.width - componentStartXOffset / 2 - (if (activeTab.maxScrollY > 0) (scrollbarWidth + scrollbarMargin + 2) else 0)
            val guiMouseY = rawMouseY

            val tabsAreaStartX = componentStartXOffset / 2
            val tabsAreaEndX = this.width - componentStartXOffset / 2
            val tabAreaVisualTop = tabBarYOffset
            val tabAreaVisualBottom = tabBarYOffset + tabBarInternalHeight
            val tabDetectionAreaTop = tabAreaVisualTop - 5
            val tabDetectionAreaBottom = tabAreaVisualBottom + 5

            if (guiMouseY >= tabDetectionAreaTop && guiMouseY < tabDetectionAreaBottom &&
                rawMouseX >= tabsAreaStartX && rawMouseX < tabsAreaEndX) {
                if (maxTabScrollX > 0) {
                    val scrollAmount = tabButtonWidth + tabButtonSpacing
                    if (dWheel > 0) targetTabScrollX = max(0, targetTabScrollX - scrollAmount)
                    else targetTabScrollX = min(maxTabScrollX, targetTabScrollX + scrollAmount)
                    return
                }
            }

            openDropdown?.let { dd ->
                val originalLogicalY_dd = dd.y
                val dropdownScreenY = contentAreaVisualTop + originalLogicalY_dd - activeTab.scrollY.toInt()
                dd.y = dropdownScreenY
                val handledByDropdown = dd.handleMouseScroll(rawMouseX, guiMouseY, dWheel)
                dd.y = originalLogicalY_dd
                if(handledByDropdown) {
                    return
                }
            }

            if (rawMouseX >= componentStartXOffset / 2 && rawMouseX < mainContentScrollAreaXEnd &&
                guiMouseY >= contentAreaVisualTop && guiMouseY < contentAreaVisualBottom) {
                if (activeTab.maxScrollY > 0) {
                    val scrollAmount = if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) 80 else 30
                    if (dWheel > 0) activeTab.targetScrollY = max(0, activeTab.targetScrollY - scrollAmount)
                    else activeTab.targetScrollY = min(activeTab.maxScrollY, activeTab.targetScrollY + scrollAmount)
                } else {
                    activeTab.targetScrollY = 0
                    activeTab.scrollY = 0f
                }
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown?.close()
                openDropdown = null
            }
            val activeTab = currentTab();
            if (activeTab.id != "error_no_tabs") {
                for (component in activeTab.components.asReversed()) {
                    if (component is Textfield && component.textField.isFocused) {
                        val handled = component.keyTyped(typedChar, keyCode)
                        if (handled) return
                    }
                }
            }
            this.mc.displayGuiScreen(null)
            return
        }
        val activeTab = currentTab(); if (activeTab.id == "error_no_tabs") return

        for (component in activeTab.components.asReversed()) {
            if (component is Textfield && component.textField.isFocused) {
                val handled = component.keyTyped(typedChar, keyCode)
                if (handled) return
            }
        }
        openDropdown?.let { dd ->
            val handledByDropdown = dd.keyTyped(typedChar, keyCode)
            if (handledByDropdown) return
        }
    }


    override fun onGuiClosed() {
        super.onGuiClosed()
        Keyboard.enableRepeatEvents(false)
        Config.save()

        isDraggingContentScrollbar = false

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
        if (width <= 0 || height <= 0) {
            return
        }
        val sr = ScaledResolution(mc)
        val scale = sr.scaleFactor
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(
            (x * scale),
            ((sr.scaledHeight - (y + height)) * scale),
            (width * scale),
            (height * scale)
        )
    }

    private fun stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }
}