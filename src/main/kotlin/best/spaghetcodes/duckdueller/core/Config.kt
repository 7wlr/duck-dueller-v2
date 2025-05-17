package best.spaghetcodes.duckdueller.core

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.bot.boosting.BoostingBotBase
import best.spaghetcodes.duckdueller.bot.boosting.*
import best.spaghetcodes.duckdueller.bot.bots.*
import best.spaghetcodes.duckdueller.bot.features.Potion
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType

import java.io.File
import kotlin.math.max
import kotlin.math.min

class Config : Vigilant(File(DuckDueller.configLocation), sortingBehavior = ConfigSorter()) {

    /*
        GENERAL
     */

    @Property(
        type = PropertyType.SELECTOR,
        name = "Current Bot",
        description = "The bot you want to use",
        category = "General",
        options = ["Sumo", "Boxing", "Classic", "OP", "Combo"]
    )
    var currentBot = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Lobby Movement",
        description = "Whether or not the bot should move in pre-game lobbies.",
        category = "General",
    )
    val lobbyMovement = true

    @Property(
    type = PropertyType.SWITCH,
    name = "Disable Chat Messages",
    description = "When this is enabled, the bot will not send any chat messages.",
    category = "General",
    )
    val disableChatMessages = false

    @Property(
        type = PropertyType.NUMBER,
        name = "Throw After X Games",
        description = "After X games the bot will underperform and throw the game. 0 = disabled.",
        category = "General",
        min = 0,
        max = 1000,
        increment = 10
    )
    val throwAfterGames = 0

    @Property(
        type = PropertyType.SLIDER,
        name = "Disconnect After X Games",
        description = "After X games the bot will toggle off and disconnect. 0 = disabled.",
        category = "General",
        min = 0,
        max = 10000
    )
    val disconnectAfterGames = 0

    @Property(
        type = PropertyType.NUMBER,
        name = "Disconnect After X Minutes",
        description = "After X minutes the bot will toggle off and disconnect. 0 = disabled",
        category = "General",
        min = 0,
        max = 500,
        increment = 30
    )
    val disconnectAfterMinutes = 0


    @Property(
        type = PropertyType.SWITCH,
        name = "Enable Boosting Mode",
        description = "Enable to use boosting bots instead of gameplay bots.",
        category = "Boosting"
    )
    var enableBoostingMode = false

    val boostingBotInstances: List<BoostingBotBase> = listOf(
        SumoBoost() as BoostingBotBase,
        BlitzBoost() as BoostingBotBase,
        BoxingBoost() as BoostingBotBase,
        ClassicBoost() as BoostingBotBase,
        OPBoost() as BoostingBotBase,
        TntBoost() as BoostingBotBase,
        UhcBoost() as BoostingBotBase,
        BowBoost() as BoostingBotBase,
        ComboBoost() as BoostingBotBase,
        PotionBoost() as BoostingBotBase,
        MwBoost() as BoostingBotBase,
        SwBoost() as BoostingBotBase
    )

    @Property(
        type = PropertyType.SELECTOR,
        name = "Selected Boosting Bot",
        description = "Choose which boosting bot to use when Boosting Mode is ON.",
        category = "Boosting",
        options = ["Sumo", "Blitz", "Boxing", "Classic", "OP", "TNT", "UHC", "Bow", "Combo", "NoDebuff", "MW", "Skywars"]
    )
    var selectedBoostingBotIndex = 0

    @Property(
        type = PropertyType.NUMBER,
        name = "Boosting Requeue Delay (ms)",
        description = "Base delay (ms) after game start detection before sending requeue command. Randomness will be added.",
        category = "Boosting",
        min = 0,
        max = 5000,
        increment = 50
    )
    var boostingRequeueDelay = 250

    @Property(
        type = PropertyType.NUMBER,
        name = "Min CPS",
        description = "The minimum CPS that the bot will be clicking at.",
        category = "Combat",
        min = 6,
        max = 15,
        increment = 1
    )
    val minCPS = 10

    @Property(
        type = PropertyType.NUMBER,
        name = "Max CPS",
        description = "The maximum CPS that the bot will be clicking at.",
        category = "Combat",
        min = 1,
        max = 18,
        increment = 1
    )
    val maxCPS = 14

    @Property(
        type = PropertyType.NUMBER,
        name = "Horizontal Look Speed",
        description = "How fast the bot can look left/right (lower number = less snappy, slightly less accurate when teleporting)",
        category = "Combat",
        min = 5,
        max = 15,
        increment = 1
    )
    val lookSpeedHorizontal = 10

    @Property(
        type = PropertyType.NUMBER,
        name = "Vertical Look Speed",
        description = "How fast the bot can look up/down (lower number = less snappy, slightly less accurate when teleporting)",
        category = "Combat",
        min = 1,
        max = 15,
        increment = 1
    )
    val lookSpeedVertical = 5

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Look Randomization",
        description = "How much randomization should happen when looking (higher number = more jittery aim)",
        category = "Combat",
        minF = 0f,
        maxF = 2f,
    )
    val lookRand = 0.3f

    @Property(
        type = PropertyType.NUMBER,
        name = "Max Look Distance",
        description = "How close the opponent has to be before the bot starts tracking them",
        category = "Combat",
        min = 120,
        max = 180,
        increment = 5
    )
    val maxDistanceLook = 150

    @Property(
        type = PropertyType.NUMBER,
        name = "Max Attack Distance",
        description = "How close the opponent has to be before the bot starts attacking them",
        category = "Combat",
        min = 5,
        max = 15,
        increment = 1
    )
    val maxDistanceAttack = 5

    /*
        Auto GG
     */

    @Property(
        type = PropertyType.SWITCH,
        name = "Enable AutoGG",
        description = "Send a gg message after every game",
        category = "AutoGG",
    )
    val sendAutoGG = true

    @Property(
        type = PropertyType.TEXT,
        name = "AutoGG Message",
        description = "AutoGG message the bot sends after every game",
        category = "AutoGG",
    )
    val ggMessage = "gg"

    @Property(
        type = PropertyType.NUMBER,
        name = "AutoGG Delay",
        description = "How long to wait after the game before sending the message",
        category = "AutoGG",
        min = 50,
        max = 1000,
        increment = 50
    )
    val ggDelay = 100

    @Property(
        type = PropertyType.SWITCH,
        name = "Game Start Message",
        description = "Send a message as soon as the game starts",
        category = "AutoGG",
    )
    val sendStartMessage = false

    @Property(
        type = PropertyType.TEXT,
        name = "Start Message",
        description = "Message to send at the beginning of the game",
        category = "AutoGG",
    )
    val startMessage = "GL HF!"

    @Property(
        type = PropertyType.NUMBER,
        name = "Start Message Delay",
        description = "How long to wait before sending the start message",
        category = "AutoGG",
        min = 50,
        max = 1000,
        increment = 50
    )
    val startMessageDelay = 100

    /*
        Auto Requeue
     */

    @Property(
        type = PropertyType.NUMBER,
        name = "Auto Requeue Delay",
        description = "How long to wait after a game before re-queueing",
        category = "Auto Requeue",
        min = 0,
        max = 5000,
        increment = 50
    )
    val autoRqDelay = 2500

    @Property(
        type = PropertyType.NUMBER,
        name = "Requeue After No Game",
        description = "How long to wait before re-queueing if no game starts",
        category = "Auto Requeue",
        min = 15,
        max = 60,
        increment = 5
    )
    val rqNoGame = 30

    @Property(
        type = PropertyType.SWITCH,
        name = "Paper Requeue",
        description = "Use the paper to requeue",
        category = "Auto Requeue",
    )
    val paperRequeue = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Fast Requeue",
        description = "Faster Requeue (no rewards)",
        category = "Auto Requeue",
    )
    val fastRequeue = true

    /*
        Webhook
     */

    @Property(
        type = PropertyType.SWITCH,
        name = "Send Webhook Messages",
        description = "Whether or not the bot should send a discord webhook message after each game.",
        category = "Webhook",
    )
    val sendWebhookMessages = false

    @Property(
        type = PropertyType.TEXT,
        name = "Discord Webhook URL",
        description = "The webhook URL to send messages to.",
        category = "Webhook",
    )
    val webhookURL = ""

    @Property(
        type = PropertyType.SWITCH,
        name = "Send Queue Stats",
        description = "Should the bot send the stats of the player in the lobby to the webhook?",
        category = "Webhook",
    )
    val sendWebhookStats = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Send Dodge Alerts",
        description = "If enabled, the bot will send a webhook whenever it dodged a player/nick.",
        category = "Webhook",
    )
    val sendWebhookDodge = false

    /*
        Misc
     */

    @Property(
        type = PropertyType.SWITCH,
        name = "Boxing Fish",
        description = "Switch between the sword and the fish in boxing.",
        category = "Misc",
    )
    val boxingFish = false

    /*
        HUD
    */

    @Property(
        type = PropertyType.SWITCH,
        name = "Session Stats HUD",
        description = "Show a HUD with the current session stats.",
        category = "HUD"
    )
    var sessionStatsHUD = true


    private val regularBotOptionsArray = arrayOf("Sumo", "Boxing", "Classic", "OP", "Combo")
    private val minRegularBotIndex = 0
    private val maxRegularBotIndex = if (regularBotOptionsArray.isNotEmpty()) regularBotOptionsArray.size - 1 else 0

    private val minBoostingBotIndex = 0
    private val maxBoostingBotIndex = if (boostingBotInstances.isNotEmpty()) boostingBotInstances.size - 1 else 0


    val bots: Map<Int, BotBase> = mapOf(
        0 to Sumo(),
        1 to Boxing(),
        2 to Classic(),
        3 to OP(),
        4 to Combo()
    )


    init {
        initialize()

        if (this.currentBot < minRegularBotIndex || this.currentBot > maxRegularBotIndex) {
            this.currentBot = minRegularBotIndex
        }

        if (this.selectedBoostingBotIndex < minBoostingBotIndex || this.selectedBoostingBotIndex > maxBoostingBotIndex) {
            this.selectedBoostingBotIndex = minBoostingBotIndex
        }

        val minBoostingDelay = 50
        if (this.boostingRequeueDelay < minBoostingDelay) {
            this.boostingRequeueDelay = minBoostingDelay
        }


        addDependency("selectedBoostingBotIndex", "enableBoostingMode")
        addDependency("boostingRequeueDelay", "enableBoostingMode")

        addDependency("webhookURL", "sendWebhookMessages")

        addDependency("ggMessage", "sendAutoGG")
        addDependency("ggDelay", "sendAutoGG")

        addDependency("startMessage", "sendStartMessage")
        addDependency("startMessageDelay", "sendStartMessage")


        registerListener<Int>("currentBot") { uiAttemptedValue ->
            if (!this.enableBoostingMode) {
                DuckDueller.updateActiveBot(
                    newBoostingModeState = null,
                    newRegularBotIndex = uiAttemptedValue,
                    newBoostingBotIndex = null
                )
            }
        }

        registerListener<Boolean>("enableBoostingMode") { newValueFromUI ->
            DuckDueller.updateActiveBot(
                newBoostingModeState = newValueFromUI,
                newRegularBotIndex = null,
                newBoostingBotIndex = null
            )
        }

        registerListener<Int>("selectedBoostingBotIndex") { uiAttemptedValue ->
            if (this.enableBoostingMode) {
                DuckDueller.updateActiveBot(
                    newBoostingModeState = null,
                    newRegularBotIndex = null,
                    newBoostingBotIndex = uiAttemptedValue
                )
            }
        }
    }

    fun getActiveBoostingBotInstance(): BoostingBotBase? {
        if (boostingBotInstances.isEmpty()) return null
        val indexToUse = selectedBoostingBotIndex.coerceIn(minBoostingBotIndex, maxBoostingBotIndex)
        return boostingBotInstances.getOrNull(indexToUse)
    }
}