package best.spaghetcodes.duckdueller.bot.replay

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.utils.ChatUtils
import best.spaghetcodes.duckdueller.utils.RandomUtils
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ReplayClearingBot : BotBase(queueCommand = "") {

    private var commandsSentThisSession = 0
    private var commandsSkippedThisSession = 0
    private var sessionStartTimeMillis = 0L

    private var maxCommandsToExecute = 500
    private var minDelayMs = 2000
    private var maxDelayMs = 5000

    private var nextCommandTime = 0L
    private var sessionInProgress = false

    private var lastCommandSentTime = 0L
    private var waitingForCooldown = false
    private val cooldownMessageTrigger = "This command is on cooldown!"
    private val housingCommand = "/housing random"

    override fun getName(): String = "Replay Clearing"

    private fun startSession() {
        commandsSentThisSession = 0
        commandsSkippedThisSession = 0
        sessionStartTimeMillis = System.currentTimeMillis()

        maxCommandsToExecute = DuckDueller.config?.replayClearingCommandCount ?: 500
        minDelayMs = DuckDueller.config?.replayClearingMinDelay ?: 2000
        maxDelayMs = DuckDueller.config?.replayClearingMaxDelay ?: 5000

        if (minDelayMs <= 0) minDelayMs = 1000
        if (maxDelayMs < minDelayMs) maxDelayMs = minDelayMs

        nextCommandTime = System.currentTimeMillis() + RandomUtils.randomIntInRange(minDelayMs / 2, minDelayMs)
        sessionInProgress = true
        waitingForCooldown = false
    }

    private fun stopSession(completed: Boolean = false) {
        sessionInProgress = false
        waitingForCooldown = false
        if (completed) {
        } else {
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: ClientChatReceivedEvent) {
        if (DuckDueller.bot == null || !toggled() || DuckDueller.bot !== this || !sessionInProgress) {
            return
        }

        val message = event.message.unformattedText
        if (message.contains(cooldownMessageTrigger)) {
            if (System.currentTimeMillis() - lastCommandSentTime < 2000) {
                if (commandsSentThisSession > 0) {
                    commandsSentThisSession--
                }
                commandsSkippedThisSession++

                val cooldownWaitDelay = 1000L + RandomUtils.randomIntInRange(0, 500)
                nextCommandTime = System.currentTimeMillis() + cooldownWaitDelay
                waitingForCooldown = true
            }
        }
    }

    override fun onTick() {
        if (mc.thePlayer == null) return

        if (DuckDueller.bot == null || !toggled() || DuckDueller.bot !== this) {
            if (sessionInProgress) {
                stopSession(false)
            }
            return
        }

        val currentCommandCountTarget = DuckDueller.config?.replayClearingCommandCount ?: 500

        if (!sessionInProgress && commandsSentThisSession < currentCommandCountTarget) {
            startSession()
        }

        if (!sessionInProgress) {
            return
        }

        if (System.currentTimeMillis() >= nextCommandTime) {
            if (commandsSentThisSession < currentCommandCountTarget) {
                ChatUtils.sendAsPlayer(housingCommand)
                lastCommandSentTime = System.currentTimeMillis()
                commandsSentThisSession++
                waitingForCooldown = false

                if (commandsSentThisSession >= currentCommandCountTarget) {
                    val self = this
                    best.spaghetcodes.duckdueller.utils.TimeUtils.setTimeout(fun() {
                        if (self.commandsSentThisSession >= (DuckDueller.config?.replayClearingCommandCount ?: 500) && self.sessionInProgress) {
                            self.stopSession(completed = true)
                        }
                    }, 100)
                }

                if (sessionInProgress) {
                    val currentMinDelay = DuckDueller.config?.replayClearingMinDelay ?: 2000
                    val currentMaxDelay = DuckDueller.config?.replayClearingMaxDelay ?: 5000
                    val delay = RandomUtils.randomIntInRange(
                        if (currentMinDelay <= 0) 1000 else currentMinDelay,
                        if (currentMaxDelay < currentMinDelay) currentMinDelay else currentMaxDelay
                    )
                    nextCommandTime = System.currentTimeMillis() + delay
                }
            } else {
                if (sessionInProgress) {
                    stopSession(completed = true)
                }
            }
        }
    }

    private fun getSessionUptimeString(): String {
        if (sessionStartTimeMillis == 0L) return if (sessionInProgress) "00:00:00 (Starting)" else "00:00:00 (Not Started)"
        if (!sessionInProgress) return "00:00:00 (Paused)"

        val uptimeMillis = System.currentTimeMillis() - sessionStartTimeMillis
        val seconds = (uptimeMillis / 1000) % 60
        val minutes = (uptimeMillis / (1000 * 60)) % 60
        val hours = (uptimeMillis / (1000 * 60 * 60))

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    @SubscribeEvent
    fun onRenderReplayClearingHUD(event: RenderGameOverlayEvent.Text) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return
        }

        if (DuckDueller.bot == null || !this.toggled() || DuckDueller.bot !== this) {
            return
        }

        val fr: FontRenderer = mc.fontRendererObj
        val M = EnumChatFormatting.GRAY
        val V = EnumChatFormatting.WHITE
        val H = EnumChatFormatting.LIGHT_PURPLE

        val xPos = 5f
        var yPos = 5f
        val yStep = fr.FONT_HEIGHT + 2

        fr.drawStringWithShadow(
            "${H}${EnumChatFormatting.BOLD}WLR${EnumChatFormatting.RESET} ${M}> ${EnumChatFormatting.YELLOW}${getName()}",
            xPos,
            yPos,
            0xFFFFFF
        )
        yPos += yStep + 2

        val targetCommands = DuckDueller.config?.replayClearingCommandCount ?: "N/A"
        fr.drawStringWithShadow("${M}Sent: ${V}$commandsSentThisSession / $targetCommands", xPos, yPos, 0xFFFFFF)
        yPos += yStep
        fr.drawStringWithShadow("${M}Skipped (Cooldown): ${EnumChatFormatting.RED}$commandsSkippedThisSession", xPos, yPos, 0xFFFFFF)
        yPos += yStep
        fr.drawStringWithShadow("${M}Uptime: ${EnumChatFormatting.AQUA}${getSessionUptimeString()}", xPos, yPos, 0xFFFFFF)
        yPos += yStep

        if (!sessionInProgress) {
            val configTarget = DuckDueller.config?.replayClearingCommandCount ?: 0
            if (commandsSentThisSession >= configTarget && configTarget > 0) {
                fr.drawStringWithShadow("${EnumChatFormatting.GREEN}Session Complete!", xPos, yPos, 0xFFFFFF)
            } else {
                fr.drawStringWithShadow("${EnumChatFormatting.YELLOW}Session Paused/Stopped.", xPos, yPos, 0xFFFFFF)
            }
            yPos += yStep
        }
    }

    override fun onGameStart() {}
    override fun onGameEnd() {}
    override fun onAttack() {}
    override fun onAttacked() {}
    override fun onJoinGame() {}
    override fun beforeStart() {}
    override fun beforeLeave() {}
    override fun onFoundOpponent() {}
}