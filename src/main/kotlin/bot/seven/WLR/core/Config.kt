package bot.seven.WLR.core

import bot.seven.WLR.bot.BotBase
import bot.seven.WLR.bot.boosting.*
import bot.seven.WLR.bot.bots.*
import bot.seven.WLR.bot.replay.ReplayClearingBot
import bot.seven.WLR.wlr
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.math.max

object Config {
    val REGULAR_BOT_OPTIONS = arrayOf("Sumo", "Boxing", "Classic", "OP", "Combo")
    val BOOSTING_BOT_OPTIONS = arrayOf(
        "Sumo", "Blitz", "Boxing", "Classic", "OP", "TNT", "UHC",
        "Bow", "Combo", "NoDebuff", "MW", "Skywars"
    )

    enum class SumoStrafeIntensity {
        LIGHT, MEDIUM, HARD;

        companion object {
            private val valuesArray = values()

            fun fromOrdinal(ordinal: Int): SumoStrafeIntensity {
                return if (ordinal >= 0 && ordinal < valuesArray.size) {
                    valuesArray[ordinal]
                } else {
                    MEDIUM
                }
            }
            val options: List<String> = valuesArray.map { intensity ->
                intensity.name.lowercase().replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
        }
    }

    val boostingBotInstances: List<BoostingBotBase> = listOf(
        SumoBoost(), BlitzBoost(), BoxingBoost(), ClassicBoost(), OPBoost(),
        TntBoost(), UhcBoost(), BowBoost(), ComboBoost(), PotionBoost(),
        MwBoost(), SwBoost()
    )
    val replayClearingBotInstance: ReplayClearingBot = ReplayClearingBot()
    val bots: Map<Int, BotBase> = mapOf(
        0 to Sumo(), 1 to Boxing(), 2 to Classic(), 3 to OP(), 4 to Combo()
    )

    val sumoBotIndex: Int = REGULAR_BOT_OPTIONS.indexOf("Sumo")

    private val minRegularBotIndex = 0
    private val maxRegularBotIndex = if (REGULAR_BOT_OPTIONS.isNotEmpty()) REGULAR_BOT_OPTIONS.size - 1 else 0
    private val minBoostingBotIndex = 0
    private val maxBoostingBotIndex = if (boostingBotInstances.isNotEmpty()) boostingBotInstances.size - 1 else 0

    var currentBot = 0
        private set
    var lobbyMovement = true
    var disableChatMessages = false
    var throwAfterGames = 0
    var disconnectAfterGames = 0
    var disconnectAfterMinutes = 0

    var enableBoostingMode = false
        private set
    var selectedBoostingBotIndex = 0
        private set
    var boostingRequeueDelay = 250

    var enableCustomCamera = false
    var cameraOffsetX = 0.5f
    var cameraOffsetY = -5.0f
    var cameraOffsetZ = 5.0f
    var cameraPitch = 40.0f
    var cameraYaw = -180.0f
    @JvmField
    var enableCameraZoom: Boolean = false
    @JvmField
    var cameraZoomFovValue: Float = 70f


    var enableReplayClearingMode = false
        private set
    var replayClearingMinDelay = 2000
    var replayClearingMaxDelay = 5000
    var replayClearingCommandCount = 500

    var minCPS = 10
    var maxCPS = 14
    var lookSpeedHorizontal = 10
    var lookSpeedVertical = 5
    var lookRand = 0.3f
    var maxDistanceLook = 8
    var maxDistanceAttack = 5

    var enableComboResetByDistance = true
    var comboResetDistance = 5

    var enableSumoDistanceJump = true
        set(value) {
            if (field != value) {
                field = value
                save()
            }
        }

    var enableSumoStrafing = true
        set(value) {
            if (field != value) {
                field = value
                save()
            }
        }
    var sumoStrafeIntensity: SumoStrafeIntensity = SumoStrafeIntensity.MEDIUM
        set(value) {
            if (field != value) {
                field = value
                save()
            }
        }


    var sendAutoGG = true
    var ggMessage = "gg"
    var ggDelay = 100
    var sendStartMessage = false
    var startMessage = "GL HF!"
    var startMessageDelay = 100

    var autoRqDelay = 2500
    var rqNoGame = 30
    var paperRequeue = true
    var fastRequeue = true

    var sendWebhookMessages = false
    var webhookURL = ""
    var sendWebhookStats = false
    var sendWebhookDodge = false

    var boxingFish = false
    var sessionStatsHUD = true

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(wlr.configLocation)

    private data class ConfigData(
        var currentBot: Int, var lobbyMovement: Boolean, var disableChatMessages: Boolean,
        var throwAfterGames: Int, var disconnectAfterGames: Int, var disconnectAfterMinutes: Int,
        var enableBoostingMode: Boolean, var selectedBoostingBotIndex: Int, var boostingRequeueDelay: Int,
        var enableCustomCamera: Boolean, var cameraOffsetX: Float, var cameraOffsetY: Float,
        var cameraOffsetZ: Float, var cameraPitch: Float, var cameraYaw: Float,
        var enableCameraZoom: Boolean?,
        var cameraZoomFovValue: Float?,
        var enableReplayClearingMode: Boolean, var replayClearingMinDelay: Int,
        var replayClearingMaxDelay: Int, var replayClearingCommandCount: Int,
        var minCPS: Int, var maxCPS: Int, var lookSpeedHorizontal: Int, var lookSpeedVertical: Int,
        var lookRand: Float, var maxDistanceLook: Int, var maxDistanceAttack: Int,
        var enableComboResetByDistance: Boolean, var comboResetDistance: Int,
        var enableSumoDistanceJump: Boolean?, var enableSumoStrafing: Boolean?,
        var sumoStrafeIntensity: Int?,
        var sendAutoGG: Boolean, var ggMessage: String, var ggDelay: Int,
        var sendStartMessage: Boolean, var startMessage: String, var startMessageDelay: Int,
        var autoRqDelay: Int, var rqNoGame: Int, var paperRequeue: Boolean, var fastRequeue: Boolean,
        var sendWebhookMessages: Boolean, var webhookURL: String, var sendWebhookStats: Boolean?, var sendWebhookDodge: Boolean?,
        var boxingFish: Boolean, var sessionStatsHUD: Boolean
    )

    init {
        load()
    }

    fun load() {
        try {
            if (configFile.exists()) {
                val data = gson.fromJson(configFile.reader(), ConfigData::class.java)

                currentBot = data.currentBot.coerceIn(minRegularBotIndex, maxRegularBotIndex)
                lobbyMovement = data.lobbyMovement
                disableChatMessages = data.disableChatMessages
                throwAfterGames = data.throwAfterGames.coerceIn(0, 1000)
                disconnectAfterGames = data.disconnectAfterGames.coerceIn(0, 10000)
                disconnectAfterMinutes = data.disconnectAfterMinutes.coerceIn(0, 500)

                enableBoostingMode = data.enableBoostingMode
                selectedBoostingBotIndex = data.selectedBoostingBotIndex.coerceIn(minBoostingBotIndex, maxBoostingBotIndex)
                boostingRequeueDelay = data.boostingRequeueDelay.coerceIn(0, 5000).let { max(50, it) }

                enableCustomCamera = data.enableCustomCamera
                cameraOffsetX = data.cameraOffsetX.coerceIn(-10.0f, 10.0f)
                cameraOffsetY = data.cameraOffsetY.coerceIn(-10.0f, 10.0f)
                cameraOffsetZ = data.cameraOffsetZ.coerceIn(-15.0f, 15.0f)
                cameraPitch = data.cameraPitch.coerceIn(-90.0f, 90.0f)
                cameraYaw = data.cameraYaw.coerceIn(-180.0f, 180.0f)

                enableCameraZoom = data.enableCameraZoom ?: false
                cameraZoomFovValue = (data.cameraZoomFovValue ?: 70f).coerceIn(10f, 90f)

                enableReplayClearingMode = data.enableReplayClearingMode
                replayClearingMinDelay = data.replayClearingMinDelay.coerceIn(500, 20000)
                replayClearingMaxDelay = data.replayClearingMaxDelay.coerceIn(500, 20000)
                replayClearingCommandCount = data.replayClearingCommandCount.coerceIn(1, 600)

                minCPS = data.minCPS.coerceIn(1, 20)
                maxCPS = data.maxCPS.coerceIn(5, 25)
                if (minCPS > maxCPS) minCPS = maxCPS

                lookSpeedHorizontal = data.lookSpeedHorizontal.coerceIn(1, 30)
                lookSpeedVertical = data.lookSpeedVertical.coerceIn(1, 30)
                lookRand = data.lookRand.coerceIn(0f, 5f)
                maxDistanceLook = data.maxDistanceLook.coerceIn(3, 150)
                maxDistanceAttack = data.maxDistanceAttack.coerceIn(3, 8)

                enableComboResetByDistance = data.enableComboResetByDistance
                comboResetDistance = data.comboResetDistance.coerceIn(1, 10)

                enableSumoDistanceJump = data.enableSumoDistanceJump ?: true
                enableSumoStrafing = data.enableSumoStrafing ?: true
                sumoStrafeIntensity = SumoStrafeIntensity.fromOrdinal(data.sumoStrafeIntensity ?: SumoStrafeIntensity.MEDIUM.ordinal)


                sendAutoGG = data.sendAutoGG
                ggMessage = data.ggMessage ?: "gg"
                ggDelay = data.ggDelay.coerceIn(0, 2000)
                sendStartMessage = data.sendStartMessage
                startMessage = data.startMessage ?: "GL HF!"
                startMessageDelay = data.startMessageDelay.coerceIn(0, 2000)

                autoRqDelay = data.autoRqDelay.coerceIn(0, 5000)
                rqNoGame = data.rqNoGame.coerceIn(5, 120)
                paperRequeue = data.paperRequeue
                fastRequeue = data.fastRequeue

                sendWebhookMessages = data.sendWebhookMessages
                webhookURL = data.webhookURL ?: ""
                sendWebhookStats = data.sendWebhookStats ?: false
                sendWebhookDodge = data.sendWebhookDodge ?: false
                boxingFish = data.boxingFish
                sessionStatsHUD = data.sessionStatsHUD

            } else {
                save()
            }
        } catch (e: Exception) {
            System.err.println("Error loading WLR config: ${e.message}")
            resetToDefaultsAndSave()
        }
    }

    private fun resetToDefaultsAndSave() {
        currentBot = 0
        lobbyMovement = true
        disableChatMessages = false
        throwAfterGames = 0
        disconnectAfterGames = 0
        disconnectAfterMinutes = 0
        enableBoostingMode = false
        selectedBoostingBotIndex = 0
        boostingRequeueDelay = 250
        enableCustomCamera = false
        cameraOffsetX = 0.0f
        cameraOffsetY = -2.2f
        cameraOffsetZ = 3.5f
        cameraPitch = 40.0f
        cameraYaw = -180.0f
        enableCameraZoom = false
        cameraZoomFovValue = 70f
        enableReplayClearingMode = false
        replayClearingMinDelay = 2000
        replayClearingMaxDelay = 5000
        replayClearingCommandCount = 490
        minCPS = 10
        maxCPS = 14
        lookSpeedHorizontal = 10
        lookSpeedVertical = 5
        lookRand = 0.3f
        maxDistanceLook = 8
        maxDistanceAttack = 5
        enableComboResetByDistance = true
        comboResetDistance = 5
        enableSumoDistanceJump = true
        enableSumoStrafing = true
        sumoStrafeIntensity = SumoStrafeIntensity.MEDIUM
        sendAutoGG = true
        ggMessage = "gg"
        ggDelay = 100
        sendStartMessage = false
        startMessage = "GL HF!"
        startMessageDelay = 100
        autoRqDelay = 2500
        rqNoGame = 30
        paperRequeue = true
        fastRequeue = true
        sendWebhookMessages = false
        webhookURL = ""
        sendWebhookStats = false
        sendWebhookDodge = false
        boxingFish = false
        sessionStatsHUD = true
        save()
    }

    fun save() {
        try {
            configFile.parentFile?.mkdirs()
            val data = ConfigData(
                currentBot, lobbyMovement, disableChatMessages, throwAfterGames, disconnectAfterGames,
                disconnectAfterMinutes, enableBoostingMode, selectedBoostingBotIndex, boostingRequeueDelay,
                enableCustomCamera, cameraOffsetX, cameraOffsetY, cameraOffsetZ, cameraPitch, cameraYaw,
                enableCameraZoom, cameraZoomFovValue,
                enableReplayClearingMode, replayClearingMinDelay, replayClearingMaxDelay, replayClearingCommandCount,
                minCPS, maxCPS, lookSpeedHorizontal, lookSpeedVertical, lookRand, maxDistanceLook,
                maxDistanceAttack, enableComboResetByDistance, comboResetDistance,
                enableSumoDistanceJump, enableSumoStrafing,
                sumoStrafeIntensity.ordinal,
                sendAutoGG, ggMessage, ggDelay, sendStartMessage, startMessage, startMessageDelay, autoRqDelay, rqNoGame,
                paperRequeue, fastRequeue, sendWebhookMessages, webhookURL, sendWebhookStats, sendWebhookDodge,
                boxingFish, sessionStatsHUD
            )
            configFile.writeText(gson.toJson(data))
        } catch (e: Exception) {
            System.err.println("Error saving WLR config: ${e.message}")
        }
    }

    fun setCurrentBot(newBotIndex: Int) {
        val clampedIndex = newBotIndex.coerceIn(minRegularBotIndex, maxRegularBotIndex)
        if (enableBoostingMode || enableReplayClearingMode || currentBot != clampedIndex) {
            currentBot = clampedIndex
            enableBoostingMode = false
            enableReplayClearingMode = false

            save()
            wlr.updateActiveBot(
                newReplayClearingModeState = false,
                newBoostingModeState = false,
                newRegularBotIndex = currentBot,
                newBoostingBotIndex = null
            )
        }
    }

    fun setEnableBoostingMode(enabled: Boolean) {
        if (enableBoostingMode != enabled) {
            enableBoostingMode = enabled
            if (enabled) {
                enableReplayClearingMode = false
            }

            save()
            wlr.updateActiveBot(
                newReplayClearingModeState = if (enabled) false else null,
                newBoostingModeState = enabled,
                newRegularBotIndex = null,
                newBoostingBotIndex = if (enabled) selectedBoostingBotIndex else null
            )
        }
    }

    fun setSelectedBoostingBot(newBoostingIndex: Int) {
        val clampedIndex = newBoostingIndex.coerceIn(minBoostingBotIndex, maxBoostingBotIndex)
        if (selectedBoostingBotIndex != clampedIndex || !enableBoostingMode || enableReplayClearingMode) {
            selectedBoostingBotIndex = clampedIndex
            enableBoostingMode = true
            enableReplayClearingMode = false

            save()
            wlr.updateActiveBot(
                newReplayClearingModeState = false,
                newBoostingModeState = true,
                newRegularBotIndex = null,
                newBoostingBotIndex = selectedBoostingBotIndex
            )
        }
    }

    fun setEnableReplayClearingMode(enabled: Boolean) {
        if (enableReplayClearingMode != enabled) {
            enableReplayClearingMode = enabled
            if (enabled) {
                enableBoostingMode = false
            }

            save()
            wlr.updateActiveBot(
                newReplayClearingModeState = enabled,
                newBoostingModeState = if (enabled) false else null,
                newRegularBotIndex = null,
                newBoostingBotIndex = null
            )
        }
    }

    fun getActiveBoostingBotInstance(): BoostingBotBase? {
        if (boostingBotInstances.isEmpty() || selectedBoostingBotIndex < 0 || selectedBoostingBotIndex >= boostingBotInstances.size) return null
        return boostingBotInstances[selectedBoostingBotIndex]
    }
}