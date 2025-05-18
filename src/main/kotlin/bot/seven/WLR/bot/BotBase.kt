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
    fun toggle() {
        toggled = !toggled
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

    private var reconnectTimer: Timer? = null

    private var ticksSinceGameStart = 0

    private var lastOpponentName = ""

    private var calledGameEnd = false

    fun opponent() = opponent

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

                                    if (!iWon) {
                                        TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(1000, 2000))
                                    }

                                    if (Config.disconnectAfterGames > 0) {
                                        if (Session.wins + Session.losses >= Config.disconnectAfterGames) {
                                            ChatUtils.info("Played ${Config.disconnectAfterGames} games, disconnecting...")
                                            TimeUtils.setTimeout(fun () {
                                                ChatUtils.sendAsPlayer("/l duels")
                                                TimeUtils.setTimeout(fun () {
                                                    toggle()
                                                    disconnect()
                                                }, RandomUtils.randomIntInRange(2300, 5000))
                                            }, RandomUtils.randomIntInRange(900, 1700))
                                        }
                                    }

                                    if (Config.disconnectAfterMinutes > 0) {
                                        if (Session.getUptimeMillis() >= Config.disconnectAfterMinutes * 60 * 1000L) {
                                            ChatUtils.info("Played for ${Config.disconnectAfterMinutes} minutes, disconnecting...")
                                            TimeUtils.setTimeout(fun () {
                                                ChatUtils.sendAsPlayer("/l duels")
                                                TimeUtils.setTimeout(fun () {
                                                    toggle()
                                                    disconnect()
                                                }, RandomUtils.randomIntInRange(2300, 5000))
                                            }, RandomUtils.randomIntInRange(900, 1700))
                                        }
                                    }

                                    if (Config.sendWebhookMessages) {
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
                                                    if (iWon) 0x000000 else 0xED4245
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
        if (toggled) {
            onTick()

            if (StateManager.state != StateManager.States.PLAYING) {
                ticksSinceGameStart++
                if (ticksSinceGameStart / 20 > Config.rqNoGame) {
                    ticksSinceGameStart = 0
                    joinGame()
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

        if (KeyBindings.toggleBotKeyBinding.isPressed) {
            toggle()
            ChatUtils.info("WLR has been toggled ${if (toggled()) "${EnumChatFormatting.GREEN}on" else "${EnumChatFormatting.RED}off"}")
            if (toggled()) {
                ChatUtils.info("Current selected bot: ${EnumChatFormatting.GREEN}${getName()}")
                joinGame()
                Session.reset()
            }
        }
    }

    @SubscribeEvent
    fun onChat(ev: ClientChatReceivedEvent) {
        val unformatted = ev.message.unformattedText
        if (toggled() && mc.thePlayer != null) {

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
            if (toggled()) {
                resetVars()
                LobbyMovement.stop()
                Movement.clearAll()
                Combat.stopRandomStrafe()
                Mouse.stopLeftAC()
                calledGameEnd = false
            }
        }
    }

    @SubscribeEvent
    fun onConnect(ev: ClientConnectedToServerEvent) {
        if (toggled()) {
            println("Reconnect successful!")

            if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank()) {
                val author = WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")
                val thumbnail = WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")

                WebHook.sendEmbed(
                    Config.webhookURL,
                    WebHook.buildEmbed(
                        "✅ Bot Reconnected",
                        "The bot successfully reconnected to the server!",
                        JsonArray(),
                        JsonObject(),
                        author,
                        thumbnail,
                        0x57F287
                    )
                )
            }


            reconnectTimer?.cancel()
            TimeUtils.setTimeout(this::joinGame, RandomUtils.randomIntInRange(6000, 8000))
        }
    }

    @SubscribeEvent
    fun onDisconnect(ev: ClientDisconnectionFromServerEvent) {
        if (toggled()) {
            println("Disconnected from server, reconnecting...")

            if (Config.sendWebhookMessages && Config.webhookURL.isNotBlank()) {
                val author = WebHook.buildAuthor("WLR", "https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")
                val thumbnail = WebHook.buildThumbnail("https://raw.githubusercontent.com/7wlr/logos/refs/heads/main/wlr.png")

                WebHook.sendEmbed(
                    Config.webhookURL,
                    WebHook.buildEmbed(
                        "⚠️ Bot Disconnected",
                        "The bot was disconnected! Attempting to reconnect...",
                        JsonArray(),
                        JsonObject(),
                        author,
                        thumbnail,
                        0xFAA61A
                    )
                )
            }

            TimeUtils.setTimeout(fun () {
                reconnectTimer = TimeUtils.setInterval(this::reconnect, 0, 30000)
            }, RandomUtils.randomIntInRange(5000, 7000))
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

        val xPos = 5f
        var yPos = 5f
        val yStep = fr.FONT_HEIGHT + 2

        fr.drawStringWithShadow(
            "${EnumChatFormatting.LIGHT_PURPLE}${EnumChatFormatting.BOLD}WLR${EnumChatFormatting.RESET} ${EnumChatFormatting.GRAY}> ${EnumChatFormatting.YELLOW}${getName()}",
            xPos,
            yPos,
            0xFFFFFF
        )
        yPos += yStep + 2

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
        if (toggled()) {
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
        if (toggled()) {
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
        if (StateManager.state == StateManager.States.PLAYING) {
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
        if (StateManager.state == StateManager.States.GAME) {
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
        if (toggled()) {
            TimeUtils.setTimeout(fun () {
                ChatUtils.sendAsPlayer("/l")
            }, RandomUtils.randomIntInRange(100, 300))
        }
    }

    fun joinGame(second: Boolean = false) {
        if (toggled() && StateManager.state != StateManager.States.PLAYING && !StateManager.gameFull) {
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
            mc.addScheduledTask(fun () {
                mc.theWorld.sendQuittingDisconnectingPacket()
                mc.loadWorld(null)
                mc.displayGuiScreen(GuiMultiplayer(GuiMainMenu()))
            })
        }
    }

    private fun reconnect() {
        if (mc.theWorld == null) {
            if (mc.currentScreen is GuiMultiplayer) {
                mc.addScheduledTask(fun () {
                    println("Reconnecting...")
                    FMLClientHandler.instance().setupServerList()
                    FMLClientHandler.instance().connectToServer(mc.currentScreen, ServerData("Hypixel", "mc.hypixel.net", false))
                })
            } else {
                if (mc.theWorld == null && mc.currentScreen !is GuiConnecting) {
                    mc.addScheduledTask(fun () {
                        println("Attempting to show new multiplayer screen...")
                        mc.displayGuiScreen(GuiMultiplayer(GuiMainMenu()))
                        TimeUtils.setTimeout(this::reconnect, 50)
                    })
                }
            }
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
                println("Registered ${getName()}_packet_handler")
            } catch (e: IllegalArgumentException) {
                if (!e.message.orEmpty().contains("Duplicate handler name")) {
                    e.printStackTrace()
                }
            }
        }
    }

}