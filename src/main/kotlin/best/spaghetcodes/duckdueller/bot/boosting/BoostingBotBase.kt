package best.spaghetcodes.duckdueller.bot.boosting

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.utils.ChatUtils
import best.spaghetcodes.duckdueller.utils.RandomUtils
import best.spaghetcodes.duckdueller.utils.TimeUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

abstract class BoostingBotBase(
    queueCommand: String,
    val gameStartTriggerMessage: String,
    quickRefresh: Int
) : BotBase(queueCommand, quickRefresh = quickRefresh) {

    protected var gameHasStartedCurrentCycle = false

    abstract override fun getName(): String

    @SubscribeEvent
    open fun onBoostingBotChatEvent(ev: ClientChatReceivedEvent) {
        if (toggled() && mc.thePlayer != null && DuckDueller.config?.enableBoostingMode == true && DuckDueller.bot === this) {
            val unformatted = ev.message.unformattedText
            if (unformatted.contains(gameStartTriggerMessage)) {
                if (!gameHasStartedCurrentCycle) {
                    gameHasStartedCurrentCycle = true
                    onGameStartDetected()

                    val configuredDelay = DuckDueller.config?.boostingRequeueDelay ?: 250
                    val randomizedDelay = RandomUtils.randomIntInRange(
                        configuredDelay,
                        configuredDelay + RandomUtils.randomIntInRange(50, 200)
                    )

                    TimeUtils.setTimeout(fun() {
                        if (toggled() && DuckDueller.config?.enableBoostingMode == true && DuckDueller.bot === this) {
                            ChatUtils.sendAsPlayer(this.queueCommand)
                            TimeUtils.setTimeout(fun () { gameHasStartedCurrentCycle = false }, 750)
                        } else {
                            gameHasStartedCurrentCycle = false
                        }
                    }, randomizedDelay)
                }
            }
        }
    }

    protected open fun onGameStartDetected() {
    }

    override fun onJoinGame() {
        super.onJoinGame()
        if (DuckDueller.config?.enableBoostingMode == true && DuckDueller.bot === this) {
            gameHasStartedCurrentCycle = false
        }
    }

    override fun onGameStart() {
        if (DuckDueller.config?.enableBoostingMode == true && DuckDueller.bot === this) {
        } else {
            super.onGameStart()
        }
    }

    override fun onGameEnd() {
        if (DuckDueller.config?.enableBoostingMode == true && DuckDueller.bot === this) {
            gameHasStartedCurrentCycle = false
        }
        super.onGameEnd()
    }

    override fun onTick() {
        if (DuckDueller.bot === this) {
            super.onTick()
        }
    }
}