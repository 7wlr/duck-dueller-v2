package bot.seven.WLR.bot

import bot.seven.WLR.bot.player.*
import bot.seven.WLR.bot.replay.ReplayClearingBot
import bot.seven.WLR.core.Config
import bot.seven.WLR.core.KeyBindings
import bot.seven.WLR.utils.*
import bot.seven.WLR.wlr
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.concurrent.thread

/**
 * Base class for all bots
 * @param queueCommand Command to join a new game
 * @param quickRefresh MS for which to quickly refresh opponent entity
 */
open class BotBase(val queueCommand: String, val quickRefresh: Int = 10000) {

    protected val mc = Minecraft.getMinecraft()

    private var toggled = false
    fun toggled() = toggled
    fun toggle() { // Renamed from toggleBot to just toggle to match KeyBinding call
        val oldToggledState = toggled
        toggled = !toggled // Toggle the state first

        if (toggled) { // Bot is turning ON
            ChatUtils.info("WLR has been toggled ${EnumChatFormatting.GREEN}on")
            ChatUtils.info("Current selected bot: ${EnumChatFormatting.GREEN}${getName()}")
            Session.reset() // Resets session uptime, good.
            lastPlaySessionStartTime = System.currentTimeMillis() // Reset play session timer for dynamic breaks
            isTakingDynamicBreak = false // Ensure not in break state when toggled on
            explicitlyTakingBreak = false
            dynamicBreakEndTime = 0L
            dynamicBreakReconnectTimer?.cancel()
            dynamicBreakReconnectTimer = null
            reconnectTimer?.cancel() // Cancel any general reconnect attempts
            reconnectTimer = null

            if (mc.theWorld == null && mc.currentScreen is GuiMultiplayer) {
                ChatUtils.info("Attempting to connect to server on toggle...")
                reconnect() // Try to connect if on multiplayer screen
            } else if (mc.theWorld != null) {
                joinGame() // Initial join game if already connected
            } else {
                ChatUtils.info("Not connected to a server. Please connect manually or wait for auto-reconnect if applicable.")
            }

        } else { // Bot is turning OFF
            ChatUtils.info("WLR has been toggled ${EnumChatFormatting.RED}off")
            onToggleOff()
        }
    }


    private var attackedID = -1

    private var statKeys: Map<String, String> = mapOf("wins" to "", "losses" to "", "ws" to "")

    private var playerCache: HashMap<String, String> = hashMapOf()
    private var playersSent: ArrayList<String> = arrayListOf()
    private var playersQuit: ArrayList<String> = arrayListOf()

    private var opponent: EntityPlayer? = null
    private var opponentTimer: Timer? = null
    private var calledFoundOpponent = false

    protected var combo = 0
    protected var opponentCombo = 0
    protected var ticksSinceHit = 0

    private var reconnectTimer: Timer? = null // General purpose reconnect timer

    private var ticksSinceGameStart = 0

    private var lastOpponentName = ""

    private var calledGameEnd = false

    fun opponent() = opponent

    // Dynamic Break Properties
    private var isTakingDynamicBreak = false // True if the bot is currently in an active dynamic break period initiated by playtime
    private var explicitlyTakingBreak = false // True if the bot *initiated* the disconnect for a break
    private var dynamicBreakEndTime = 0L
    private var lastPlaySessionStartTime = 0L // Time when the current play session (between breaks or since toggle on) started
    private var dynamicBreakReconnectTimer: Timer? = null


    /********
     * Methods to override
     ********/

    open fun getName(): String {
        return "Base"
    }

    /**
     * Called when the bot attacks the opponent
     * Triggered by the damage sound, not the clientside attack event
     */
    protected open fun onAttack() {}

    /**
     * Called when the bot is attacked
     * Triggered by the damage sound, not the clientside attack event
     */
    protected open fun onAttacked() {}

    /**
     * Called when the game starts
     */
    protected open fun onGameStart() {}

    /**
     * Called when the game ends
     */
    protected open fun onGameEnd() {}

    /**
     * Called when the bot joins a game
     */
    protected open fun onJoinGame() {}

    /**
     * Called before the game starts (1s)
     */
    protected open fun beforeStart() {}

    /**
     * Called before the bot leaves the game
     */
    protected open fun beforeLeave() {}

    /**
     * Called when the opponent entity is found
     */
    protected open fun onFoundOpponent() {}

    /**
     * Called every tick
     */
    protected open fun onTick() {}

    /********
     * Protected Methods
     ********/

    protected fun setStatKeys(keys: Map<String, String>) {
        statKeys = keys
    }

    open fun onToggleOff() {
        Movement.clearAll()
        Mouse.stopLeftAC()
        Mouse.stopTracking()
        Combat.stopRandomStrafe()
        LobbyMovement.stop()
        Camera.disable()

        // Reset dynamic break state
        isTakingDynamicBreak = false
        explicitlyTakingBreak = false
        dynamicBreakEndTime = 0L
        dynamicBreakReconnectTimer?.cancel()
        dynamicBreakReconnectTimer = null
        reconnectTimer?.cancel() // also cancel general reconnect
        reconnectTimer = null
    }

    fun onPacket(packet: Packet<*>) {
        if (toggled) {
            when (packet) {
                is S19PacketEntityStatus -> {
                    if (packet.opCode.toInt() == 2) { // damage
                        val entity = packet.getEntity(mc.theWorld)
                        if (entity != null) {
                            if (entity.entityId == attackedID) {
                                attackedID = -1
                                onAttack()
                                combo++
                                opponentCombo = 0
                                ticksSinceHit = 0
                            } else if (mc.thePlayer != null && entity.entityId == mc.thePlayer.entityId) {
                                onAttacked()
                                combo = 0
                                opponentCombo++
                            }
                        }
                    }
                }
                is S3EPacketTeams -> {
                    if (packet.action == 3 && packet.name == "§7§k") {
                        val players = packet.players
                        for (player in players) {
                            if (playersQuit.contains(player)) {
                                playersQuit.remove(player)
                            }
                            TimeUtils.setTimeout(fun () {
                                handlePlayer(player)
                            }, 1500)
                        }
                    } else if (packet.action == 4 && packet.name == "§7§k") {
                        val players = packet.players
                        for (player in players) {
                            playersQuit.add(player)
                        }
                    }
                }
                is S45PacketTitle -> {
                    if (mc.theWorld != null) {
                        TimeUtils.setTimeout(fun () {
                            if (packet.message != null) {
                                val unformatted = packet.message.unformattedText.lowercase()
                                if (unformatted.contains("won the duel!") && mc.thePlayer != null) {
                                    var winnerName = ""
                                    var loserName = ""
                                    var iWon = false

                                    val p = ChatUtils.removeFormatting(packet.message.unformattedText).split("won")[0].trim()
                                    if (unformatted.contains(mc.thePlayer.displayNameString.lowercase())) {
                                        Session.addWin()
                                        winnerName = mc.thePlayer.displayNameString
                                        loserName = lastOpponentName
                                        iWon = true
                                    } else {
                                        Session.addLoss()
                                        winnerName = p
                                        loserName = mc.thePlayer.displayNameString
                                        iWon = false
                                    }
                                    ChatUtils.info("Wins: ${Session.wins}, Losses: ${Session.losses}")
                                    ChatUtils.info(Session.getSession())

                                    if (!iWon && !isTakingDynamicBreak) { // Don't auto-requeue if a break is pending/active
                                        TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(1000, 2000))
                                    }

                                    // Check for configured disconnects only if not in a dynamic break process
                                    if (!isTakingDynamicBreak) {
                                        if (Config.disconnectAfterGames > 0) {
                                            if (Session.wins + Session.losses >= Config.disconnectAfterGames) {
                                                ChatUtils.info("Played ${Config.disconnectAfterGames} games, disconnecting...")
                                                TimeUtils.setTimeout(fun() {
                                                    ChatUtils.sendAsPlayer("/l duels")
                                                    TimeUtils.setTimeout(fun() {
                                                        toggle() // This will call onToggleOff
                                                        disconnect()
                                                    }, RandomUtils.randomIntInRange(2300, 5000))
                                                }, RandomUtils.randomIntInRange(900, 1700))
                                                return@setTimeout // Don't process further if disconnecting
                                            }
                                        }

                                        if (Config.disconnectAfterMinutes > 0) {
                                            if (Session.getUptimeMillis() >= Config.disconnectAfterMinutes * 60 * 1000L) {
                                                ChatUtils.info("Played for ${Config.disconnectAfterMinutes} minutes, disconnecting...")
                                                TimeUtils.setTimeout(fun() {
                                                    ChatUtils.sendAsPlayer("/l duels")
                                                    TimeUtils.setTimeout(fun() {
                                                        toggle() // This will call onToggleOff
                                                        disconnect()
                                                    }, RandomUtils.randomIntInRange(2300, 5000))
                                                }, RandomUtils.randomIntInRange(900, 1700))
                                                return@setTimeout // Don't process further if disconnecting
                                            }
                                        }
                                    }


                                    if (Config.sendWebhookMessages) {
                                        // ... (webhook logic remains the same)
                                        if (Config.webhookURL.isNotBlank()) {
                                            val opponentNameDisplayInWebhook = if (iWon) loserName else winnerName
                                            val author = WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")
                                            val opponentFaceUrl = if (playerCache.containsKey(opponentNameDisplayInWebhook)) {
                                                "https://crafatar.com/avatars/${playerCache[opponentNameDisplayInWebhook]}?size=128&overlay"
                                            } else {
                                                "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"
                                            }
                                            val thumbnail = WebHook.buildThumbnail(opponentFaceUrl)
                                            val gameStatusTitle = if (iWon) "🏆 Game WON!" else "❌ Game LOST!"
                                            val gameDurationSeconds = StateManager.lastGameDuration / 1000
                                            val description = """
                                            **Against:** `${opponentNameDisplayInWebhook}`
                                            **Duration:** `${gameDurationSeconds}` seconds
                                            """.trimIndent()
                                            val currentWins = Session.wins
                                            val currentLosses = Session.losses
                                            val dfRatio = DecimalFormat("#.##").apply { roundingMode = RoundingMode.DOWN }
                                            val wlrString = if (currentLosses == 0) {
                                                if (currentWins > 0) "Infinity" else "N/A"
                                            } else {
                                                dfRatio.format(currentWins.toDouble() / currentLosses.toDouble())
                                            }

                                            val fields = WebHook.buildFields(arrayListOf(
                                                mapOf("name" to "👑 Winner", "value" to "`$winnerName`", "inline" to "true"),
                                                mapOf("name" to "💔 Loser", "value" to "`$loserName`", "inline" to "true"),
                                                mapOf("name" to "📊 Session Stats", "value" to "**Wins:** `${currentWins}`\n**Losses:** `${currentLosses}`\n**WLR:** `${wlrString}`", "inline" to "true"),
                                                mapOf("name" to "⏱️ Session Uptime", "value" to "`" + Session.getUptimeString() + "`", "inline" to "true"),
                                                mapOf("name" to "🤖 Bot Started", "value" to "<t:${(Session.startTime / 1000).toInt()}:R>", "inline" to "false")
                                            ))
                                            val footerText = "WLR Bot | Wins: ${Session.wins} | Losses: ${Session.losses}"
                                            val footer = WebHook.buildFooter(footerText, "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")

                                            WebHook.sendEmbed(
                                                Config.webhookURL,
                                                WebHook.buildEmbed(
                                                    gameStatusTitle,
                                                    description,
                                                    fields,
                                                    footer,
                                                    author,
                                                    thumbnail,
                                                    if (iWon) 0x000000 else 0xED4245 // Standard green/red
                                                )
                                            )
                                        } else {
                                            ChatUtils.error("Webhook URL hasn't been set!")
                                        }
                                    }
                                }
                            }
                        }, 1000)
                    }

                }
            }
        }
    }

    @SubscribeEvent
    fun onAttackEntityEvent(ev: AttackEntityEvent) {
        if (toggled() && ev.entity == mc.thePlayer) {
            attackedID = ev.target.entityId
        }
    }

    @SubscribeEvent
    fun onClientTick(ev: ClientTickEvent) {
        registerPacketListener()

        if (KeyBindings.toggleBotKeyBinding.isPressed) {
            toggle() // Handles all toggle logic including variable resets
            return // Return after handling toggle to avoid processing game logic in the same tick
        }

        if (toggled) {
            // --- Dynamic Break Logic ---
            if (isTakingDynamicBreak) {
                if (System.currentTimeMillis() >= dynamicBreakEndTime) {
                    ChatUtils.info("${EnumChatFormatting.GREEN}Dynamic break finished. Attempting to reconnect and resume...")
                    isTakingDynamicBreak = false
                    explicitlyTakingBreak = false // Break is over
                    lastPlaySessionStartTime = System.currentTimeMillis() // Reset play session timer for next session
                    dynamicBreakReconnectTimer?.cancel() // Cancel any existing timer
                    dynamicBreakReconnectTimer = null

                    if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank()) {
                        WebHook.sendEmbed(
                            Config.webhookURL,
                            WebHook.buildEmbed(
                                "🟢 Dynamic Break Over",
                                "Bot is resuming activity.",
                                JsonArray(), JsonObject(),
                                WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                                WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                                0x57F287 // Green
                            )
                        )
                    }

                    // Attempt to reconnect and join game
                    if (mc.theWorld == null) {
                        ChatUtils.info("Not connected to server. Starting reconnect timer for dynamic break.")
                        dynamicBreakReconnectTimer = TimeUtils.setInterval(fun () {
                            if (mc.theWorld == null) {
                                if (mc.currentScreen !is GuiConnecting) { // Avoid spamming connect if already trying
                                    ChatUtils.info("Dynamic break reconnect: Attempting to connect...")
                                    reconnect()
                                }
                            } else {
                                dynamicBreakReconnectTimer?.cancel()
                                dynamicBreakReconnectTimer = null
                                ChatUtils.info("Dynamic break reconnect: Successfully reconnected. Joining game.")
                                TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(4000, 7000))
                            }
                        }, RandomUtils.randomIntInRange(5000,8000), 20000) // Initial delay, then every 20s
                    } else {
                        ChatUtils.info("Already connected to server after dynamic break. Joining game.")
                        TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(1000, 3000))
                    }
                } else {
                    // Still on break. Ensure bot isn't trying to do things.
                    // For example, if it was in lobby and not disconnected, prevent it from joining games.
                    if (mc.thePlayer != null && StateManager.state != StateManager.States.GAME) {
                        // If in lobby, just wait. No specific action needed other than preventing game joins.
                    }
                    return // Important: Don't process further game logic if on break
                }
            } else { // Not on a dynamic break, check if one should start
                if (Config.enableDynamicBreaks && Config.playDurationHours > 0 && !explicitlyTakingBreak) {
                    val playDurationMillis = Config.playDurationHours * 60 * 60 * 1000L
                    val currentPlayTime = System.currentTimeMillis() - lastPlaySessionStartTime
                    if (currentPlayTime >= playDurationMillis) {
                        ChatUtils.info("${EnumChatFormatting.YELLOW}Playtime limit reached (${TimeUtils.formatMillis(currentPlayTime)}). Starting dynamic break.")
                        isTakingDynamicBreak = true
                        explicitlyTakingBreak = true // Mark that we are initiating this break
                        val breakDurationMinutes = RandomUtils.randomIntInRange(Config.breakDurationMinMinutes, Config.breakDurationMaxMinutes)
                        val breakDurationMillis = breakDurationMinutes * 60 * 1000L
                        dynamicBreakEndTime = System.currentTimeMillis() + breakDurationMillis
                        ChatUtils.info("Break duration: $breakDurationMinutes minutes. Resuming at approx: ${Date(dynamicBreakEndTime)}")

                        if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank()) {
                            WebHook.sendEmbed(
                                Config.webhookURL,
                                WebHook.buildEmbed(
                                    "⏸️ Dynamic Break Started",
                                    "Bot is taking a dynamic break for approximately $breakDurationMinutes minutes.",
                                    JsonArray(), JsonObject(),
                                    WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                                    WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                                    0xFAA61A // Orange
                                )
                            )
                        }

                        if (mc.theWorld != null) {
                            ChatUtils.info("Disconnecting for dynamic break...")
                            Movement.clearAll(); Mouse.stopLeftAC(); Mouse.stopTracking(); Combat.stopRandomStrafe(); LobbyMovement.stop(); Camera.disable()
                            disconnect() // This changes screen to GuiMultiplayer
                        } else {
                            ChatUtils.info("Not connected to a server, but dynamic break timer is active.")
                        }
                        return // Don't process game logic in this tick
                    }
                }
            }
            // --- End Dynamic Break Logic ---

            // Regular bot operations if not on break / not just started a break
            if (!isTakingDynamicBreak) { // Double check, to ensure no logic runs if break just started
                onTick()

                if (StateManager.state != StateManager.States.PLAYING) {
                    ticksSinceGameStart++
                    if (ticksSinceGameStart / 20 > Config.rqNoGame) {
                        ticksSinceGameStart = 0
                        joinGame() // joinGame now has the isTakingDynamicBreak check
                    }
                } else {
                    ticksSinceGameStart = 0
                }

                if (mc.thePlayer != null && opponent != null) {
                    ticksSinceHit++
                    val distance = EntityUtils.getDistanceNoY(mc.thePlayer, opponent)
                    val comboResetEnabled = Config.enableComboResetByDistance
                    val comboResetDistValue = Config.comboResetDistance
                    if (comboResetEnabled) {
                        if (distance > comboResetDistValue && (combo != 0 || opponentCombo != 0)) {
                            combo = 0
                            opponentCombo = 0
                            ChatUtils.info("combo reset")
                        }
                    }
                }
            }
        }
    }


    @SubscribeEvent
    fun onChat(ev: ClientChatReceivedEvent) {
        val unformatted = ev.message.unformattedText
        if (toggled() && mc.thePlayer != null && !isTakingDynamicBreak) { // Don't process chat if on break

            if (unformatted.contains("The game starts in 2 seconds!")) {
                println(playersSent.joinToString(", "))
            } else if (unformatted.contains("The game starts in 1 second!")) {
                beforeStart()
            }

            if (unformatted.contains("Are you sure? Type /lobby again")) {
                leaveGame()
            }

            if (unformatted.contains("Opponent:")) {
                gameStart()
            }

            if (unformatted.contains("Accuracy") && !calledGameEnd) {
                calledGameEnd = true
                gameEnd()
            }

            if (unformatted.lowercase().contains("something went wrong trying") || unformatted.lowercase().contains("please don't spam the command")) {
                TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(6000, 8000))
            } else if (unformatted.contains("A disconnect occurred in your connection, so you were put")) {
                Movement.clearAll()
                Mouse.stopLeftAC()
                TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(6000, 8000))
            }
        }
    }

    @SubscribeEvent
    fun onJoinWorld(ev: EntityJoinWorldEvent) {
        if (wlr.mc.thePlayer != null && ev.entity == wlr.mc.thePlayer) {
            if (toggled()) { // This runs when player joins any world (e.g. after connecting)
                resetVars()
                LobbyMovement.stop()
                Movement.clearAll()
                Combat.stopRandomStrafe()
                Mouse.stopLeftAC()
                calledGameEnd = false

                // If we joined a world and a dynamic break was supposed to be active,
                // but we weren't the ones initiating the disconnect (explicitlyTakingBreak = false),
                // it means the break might have been interrupted or we reconnected manually.
                // If explicitlyTakingBreak is true, it means we are likely in the process of our break.
                // The onClientTick will handle resuming.
                // If we connect and a dynamic break should be active (isTakingDynamicBreak is true),
                // the onClientTick logic will prevent joining games until breakEndTime.
                if (isTakingDynamicBreak) {
                    ChatUtils.info("Joined world while dynamic break is active. Waiting for break to end.")
                } else {
                    // If not in a dynamic break, and we just joined a world (e.g. initial connect, or reconnect after non-break DC)
                    // and not already trying to join via dynamicBreakReconnectTimer, then try to join.
                    if (dynamicBreakReconnectTimer == null) { // Avoid double joinGame calls
                        ChatUtils.info("Joined world, attempting to join game.")
                        TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(1000,3000)) // Short delay after world join
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onConnect(ev: ClientConnectedToServerEvent) { // Successfully established connection with a server
        if (toggled()) {
            ChatUtils.info("${EnumChatFormatting.GREEN}Successfully connected to server.")
            reconnectTimer?.cancel() // Cancel the general purpose reconnect timer
            reconnectTimer = null
            dynamicBreakReconnectTimer?.cancel() // Also cancel dynamic break's specific reconnect timer if it was running
            dynamicBreakReconnectTimer = null

            val wasExplicitlyTakingBreak = explicitlyTakingBreak
            explicitlyTakingBreak = false // Connection established, so we are no longer in the "initiating break disconnect" phase

            if (isTakingDynamicBreak) {
                // If connected during a dynamic break (e.g. server restarted while bot was on break, or our reconnect timer worked)
                // the `isTakingDynamicBreak` logic in `onClientTick` will handle waiting until breakEndTime.
                ChatUtils.info("Connected to server, but currently in a dynamic break period. Waiting for break to end before joining games.")
                if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank() && wasExplicitlyTakingBreak) {
                    WebHook.sendEmbed( // Inform that bot is back online but waiting
                        Config.webhookURL,
                        WebHook.buildEmbed(
                            "♻️ Bot Reconnected (During Break)",
                            "Bot reconnected during a dynamic break. Will resume after break ends.",
                            JsonArray(), JsonObject(),
                            WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                            WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                            0xFEE75C // Yellow
                        )
                    )
                }
            } else {
                // Standard connection, not related to a dynamic break recovery
                if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank()) {
                    WebHook.sendEmbed(
                        Config.webhookURL,
                        WebHook.buildEmbed(
                            "✅ Bot Reconnected",
                            "The bot successfully reconnected to the server!",
                            JsonArray(), JsonObject(),
                            WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                            WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png"),
                            0x57F287 // Green
                        )
                    )
                }
                // Now that we are connected, schedule a game join. onJoinWorld might also trigger this.
                // Let onJoinWorld handle the joinGame to avoid race conditions if onConnect and onJoinWorld fire closely.
                // TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(3000, 6000))
            }
        }
    }


    @SubscribeEvent
    fun onDisconnect(ev: ClientDisconnectionFromServerEvent) { // Fired when connection is lost
        if (toggled()) {
            if (isTakingDynamicBreak && explicitlyTakingBreak) {
                ChatUtils.info("Disconnected from server as part of dynamic break. Break timer continues.")
                // `explicitlyTakingBreak` will be true if we called `disconnect()`.
                // The `onClientTick` logic for `isTakingDynamicBreak` will handle reconnection after `dynamicBreakEndTime`.
            } else if (isTakingDynamicBreak && !explicitlyTakingBreak) {
                ChatUtils.info("Disconnected from server unexpectedly during a dynamic break. Break timer continues, will attempt reconnect after break.")
                // Still on break, but it wasn't us who DC'd. Reconnection will be handled by onClientTick after break.
            } else { // Not related to a dynamic break we initiated or were in
                ChatUtils.info("${EnumChatFormatting.RED}Disconnected from server. Attempting to reconnect...")
                if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank()) {
                    val author = WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")
                    val thumbnail = WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")
                    WebHook.sendEmbed(
                        Config.webhookURL,
                        WebHook.buildEmbed(
                            "⚠️ Bot Disconnected",
                            "The bot was disconnected! Attempting to reconnect...",
                            JsonArray(), JsonObject(), author, thumbnail, 0xFAA61A // Orange
                        )
                    )
                }
                // Standard reconnect logic, only if not already handling a dynamic break reconnect
                if (dynamicBreakReconnectTimer == null) {
                    reconnectTimer?.cancel()
                    reconnectTimer = TimeUtils.setInterval(fun() {
                        if (mc.theWorld == null && mc.currentScreen !is GuiConnecting && !isTakingDynamicBreak) { // Don't try if on break
                            ChatUtils.info("General reconnect: Attempting to connect...")
                            reconnect()
                        } else if (mc.theWorld != null || isTakingDynamicBreak) {
                            reconnectTimer?.cancel() // Stop if connected or if a break started
                            reconnectTimer = null
                        }
                    }, RandomUtils.randomIntInRange(8000, 12000), 30000) // Initial delay, then every 30s
                }
            }
        }
    }


    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Text) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return
        }

        if (wlr.bot == null ||
            wlr.bot !== this ||
            !this.toggled() ||
            wlr.bot is ReplayClearingBot
        ) {
            return
        }

        val fr: FontRenderer = mc.fontRendererObj
        val M = EnumChatFormatting.GRAY
        val V = EnumChatFormatting.WHITE

        var xPos = 5f
        var yPos = 5f
        val yStep = fr.FONT_HEIGHT + 2

        fr.drawStringWithShadow(
            "${EnumChatFormatting.LIGHT_PURPLE}${EnumChatFormatting.BOLD}WLR${EnumChatFormatting.RESET} ${EnumChatFormatting.GRAY}> ${EnumChatFormatting.YELLOW}${getName()}",
            xPos,
            yPos,
            0xFFFFFF
        )
        yPos += yStep + 2

        if (isTakingDynamicBreak) {
            val timeLeft = dynamicBreakEndTime - System.currentTimeMillis()
            fr.drawStringWithShadow("${EnumChatFormatting.AQUA}On Break: ${EnumChatFormatting.YELLOW}${TimeUtils.formatMillis(timeLeft, true)} left", xPos, yPos, 0xFFFFFF)
            yPos += yStep
        }


        if (Config.sessionStatsHUD) {
            val dfRatio = DecimalFormat("#.##").apply { roundingMode = RoundingMode.DOWN }
            val dfPerHour = DecimalFormat("#.#").apply { roundingMode = RoundingMode.DOWN }

            val currentWins = Session.wins
            val currentLosses = Session.losses
            val uptimeMillis = Session.getUptimeMillis()

            val wlr = if (currentLosses == 0) {
                if (currentWins > 0) "Inf" else "N/A"
            } else {
                dfRatio.format(currentWins.toDouble() / currentLosses.toDouble())
            }

            val hoursTotal = if (uptimeMillis > 0) uptimeMillis.toDouble() / (1000.0 * 60.0 * 60.0) else 0.0
            val winsPerHour = if (hoursTotal > 0.001) {
                dfPerHour.format(currentWins.toDouble() / hoursTotal)
            } else {
                "N/A"
            }

            fr.drawStringWithShadow("${M}Wins: ${EnumChatFormatting.GREEN}$currentWins ${V}($winsPerHour/h)", xPos, yPos, 0xFFFFFF)
            yPos += yStep
            fr.drawStringWithShadow("${M}Losses: ${EnumChatFormatting.RED}$currentLosses", xPos, yPos, 0xFFFFFF)
            yPos += yStep
            fr.drawStringWithShadow("${M}WLR: ${EnumChatFormatting.AQUA}$wlr", xPos, yPos, 0xFFFFFF)
            yPos += yStep
            fr.drawStringWithShadow("${M}Uptime: ${EnumChatFormatting.LIGHT_PURPLE}${Session.getUptimeString()}", xPos, yPos, 0xFFFFFF)
            // Display current play session time if dynamic breaks enabled
            if (Config.enableDynamicBreaks && Config.playDurationHours > 0 && !isTakingDynamicBreak) {
                yPos += yStep
                val currentPlayTime = System.currentTimeMillis() - lastPlaySessionStartTime
                val playTimeTotal = Config.playDurationHours * 60 * 60 * 1000L
                fr.drawStringWithShadow("${M}Playtime: ${EnumChatFormatting.GOLD}${TimeUtils.formatMillis(currentPlayTime)} / ${TimeUtils.formatMillis(playTimeTotal)}", xPos, yPos, 0xFFFFFF)
            }

        }
    }

    /********
     * Private Methods
     ********/

    private fun resetVars() {
        playersSent.clear()
        playersQuit.clear()
        calledFoundOpponent = false
        opponentTimer?.cancel()
        opponent = null
        combo = 0
        opponentCombo = 0
        ticksSinceHit = 0
        ticksSinceGameStart = 0
    }

    private fun gameStart() {
        if (toggled() && !isTakingDynamicBreak) { // Added check
            if (Config.sendStartMessage) {
                TimeUtils.setTimeout(fun () {
                    ChatUtils.sendAsPlayer("/ac " + (Config.startMessage))
                }, Config.startMessageDelay)
            }

            val quickRefreshTimer = TimeUtils.setInterval(this::bakery, 200, 50)
            TimeUtils.setTimeout(fun () {
                quickRefreshTimer?.cancel()
                opponentTimer = TimeUtils.setInterval(this::bakery, 0, 500)
            }, quickRefresh)

            onGameStart()
        }
    }

    private fun gameEnd() {
        if (toggled() && !isTakingDynamicBreak) { // Added check
            onGameEnd()
            resetVars()

            if (Config.sendAutoGG) {
                TimeUtils.setTimeout(fun () {
                    ChatUtils.sendAsPlayer("/ac " + (Config.ggMessage))
                }, Config.ggDelay)
            }

            val delay = Config.autoRqDelay
            if (Config.fastRequeue) {
                TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(300, 500))
            } else {
                TimeUtils.setTimeout(this::joinGame, delay)
            }
            calledGameEnd = false
        }
    }

    private fun bakery() {
        if (StateManager.state == StateManager.States.PLAYING && !isTakingDynamicBreak) { // Added check
            val entity = EntityUtils.getOpponentEntity()
            if (entity != null) {
                opponent = entity
                lastOpponentName = opponent!!.displayNameString
                if (!calledFoundOpponent) {
                    calledFoundOpponent = true
                    onFoundOpponent()
                }
            }
        }
    }

    private fun handlePlayer(player: String) {
        if (StateManager.state == StateManager.States.GAME && !isTakingDynamicBreak) { // Added check
            if (player.length > 2) {
                if (mc.thePlayer != null) {
                    if (player == mc.thePlayer.displayNameString) {
                        onJoinGame()
                    } else {
                        thread {
                            var uuid: String? = null
                            if (playerCache.containsKey(player)) {
                                uuid = playerCache[player]
                            } else {
                                // API fetch logic was here, assumed removed or handled elsewhere
                            }
                            println("Handling player: $player (UUID: ${uuid ?: "Not Found/Fetched"})")

                            if (!playersSent.contains(player)) {
                                if (playersQuit.contains(player)) {
                                    return@thread
                                }
                                playersSent.add(player)
                            }
                        }
                    }
                } else {
                    thread {
                        var uuid: String? = null
                        if (playerCache.containsKey(player)) {
                            uuid = playerCache[player]
                        } else {
                            // API fetch logic was here
                        }
                        println("Handling player (no mc.thePlayer): $player (UUID: ${uuid ?: "Not Found/Fetched"})")

                        if (!playersSent.contains(player)) {
                            if (playersQuit.contains(player)) {
                                return@thread
                            }
                            playersSent.add(player)
                        }
                    }
                }
            }
        }
    }

    private fun leaveGame() {
        if (toggled() && !isTakingDynamicBreak) { // Added check
            TimeUtils.setTimeout(fun () {
                ChatUtils.sendAsPlayer("/l")
            }, RandomUtils.randomIntInRange(100, 300))
        }
    }

    fun joinGame(second: Boolean = false) {
        if (toggled() && !isTakingDynamicBreak && StateManager.state != StateManager.States.PLAYING && !StateManager.gameFull) {
            if (StateManager.state == StateManager.States.GAME) {
                val paper = Config.paperRequeue && Inventory.setInvItem("paper")
                if (paper) {
                    TimeUtils.setTimeout(fun () {
                        Mouse.rClick(RandomUtils.randomIntInRange(30, 70))
                        TimeUtils.setTimeout(fun () {
                            Mouse.rClick(RandomUtils.randomIntInRange(30, 70))
                        }, RandomUtils.randomIntInRange(100, 300))
                    }, RandomUtils.randomIntInRange(100, 300))
                } else {
                    if (second) {
                        TimeUtils.setTimeout(fun () {
                            ChatUtils.sendAsPlayer(queueCommand)
                        }, RandomUtils.randomIntInRange(100, 300))
                    } else {
                        TimeUtils.setTimeout(fun () {
                            joinGame(true)
                        }, RandomUtils.randomIntInRange(1000, 1400))
                    }
                }
            } else {
                TimeUtils.setTimeout(fun () {
                    ChatUtils.sendAsPlayer(queueCommand)
                }, RandomUtils.randomIntInRange(100, 300))
            }
        }
    }

    private fun disconnect() {
        if (mc.theWorld != null) {
            mc.addScheduledTask {
                mc.theWorld.sendQuittingDisconnectingPacket()
                mc.loadWorld(null)
                // Avoid displaying GuiMultiplayer if another screen is more appropriate or if toggling off.
                // If toggled off, GuiMainMenu might be better.
                // If dynamic break, GuiMultiplayer is fine as the bot will try to reconnect.
                if (toggled && (isTakingDynamicBreak || explicitlyTakingBreak)) { // if toggled and it's for a break
                    mc.displayGuiScreen(GuiMultiplayer(GuiMainMenu()))
                } else if (!toggled && mc.currentScreen !is GuiMainMenu) { // if toggled off
                    mc.displayGuiScreen(GuiMainMenu())
                } else if (mc.currentScreen !is GuiMultiplayer && mc.currentScreen !is GuiMainMenu) {
                    mc.displayGuiScreen(GuiMultiplayer(GuiMainMenu()))
                }
            }
        }
    }

    private fun reconnect() { // This is the actual connection attempt
        if (mc.theWorld == null && !isTakingDynamicBreak) { // Don't try to reconnect if already on break and waiting
            if (mc.currentScreen is GuiMultiplayer || mc.currentScreen is GuiMainMenu || mc.currentScreen == null) {
                mc.addScheduledTask {
                    ChatUtils.info("Attempting to connect to Hypixel...")
                    FMLClientHandler.instance().setupServerList() // Ensure server list is loaded
                    val serverData = ServerData("Hypixel", "mc.hypixel.net", false) // Ensure correct IP
                    // GuiConnecting will be shown by this call
                    FMLClientHandler.instance().connectToServer(GuiMultiplayer(GuiMainMenu()), serverData)
                }
            } else if (mc.currentScreen !is GuiConnecting) {
                // If on some other screen, try to go to multiplayer screen first
                mc.addScheduledTask {
                    ChatUtils.info("Not on a suitable screen for reconnect, displaying multiplayer screen.")
                    mc.displayGuiScreen(GuiMultiplayer(GuiMainMenu()))
                    // The reconnect timer will try again
                }
            }
        } else if (isTakingDynamicBreak) {
            ChatUtils.info("Reconnect attempt skipped: currently in dynamic break period.")
        } else if (mc.theWorld != null) {
            ChatUtils.info("Reconnect attempt skipped: already connected to a world.")
        }
    }


    class PacketReader(private val container: BotBase) : SimpleChannelInboundHandler<Packet<*>>(false) {
        override fun channelRead0(ctx: ChannelHandlerContext?, msg: Packet<*>?) {
            if (msg != null) {
                container.onPacket(msg)
            }
            ctx?.fireChannelRead(msg)
        }
    }

    private fun registerPacketListener() {
        val pipeline = mc.thePlayer?.sendQueue?.networkManager?.channel()?.pipeline()
        if (pipeline != null && pipeline.get("${getName()}_packet_handler") == null && pipeline.get("packet_handler") != null) {
            try {
                pipeline.addBefore(
                    "packet_handler",
                    "${getName()}_packet_handler",
                    PacketReader(this)
                )
                //println("Registered ${getName()}_packet_handler") // Less verbose
            } catch (e: IllegalArgumentException) {
                if (!e.message.orEmpty().contains("Duplicate handler name")) {
                    // e.printStackTrace() // Can be spammy
                    println("Error registering packet listener (non-duplicate): ${e.message}")
                }
            }  catch (e: NoSuchElementException) {
                // Can happen during disconnects if "packet_handler" is gone
                // println("Error registering packet listener (NoSuchElementException): ${e.message}")
            }
        }
    }
}