package best.spaghetcodes.duckdueller.bot.boosting

import best.spaghetcodes.duckdueller.utils.ChatUtils
import best.spaghetcodes.duckdueller.utils.RandomUtils
import best.spaghetcodes.duckdueller.utils.TimeUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

class SwBoost : BoostingBotBase(
    queueCommand = "/play duels_sw_duel",
    gameStartTriggerMessage = "Opponent:",
    quickRefresh = 500
) {

    private var gameHasStartedForBoosting = false

    override fun getName(): String {
        return "Skywars Boosting (Solos)"
    }

    init {
        setStatKeys(
            mapOf(
                "wins" to "player.stats.Duels.sw_duel_wins",
                "losses" to "player.stats.Duels.sw_duel_losses",
                "ws" to "player.stats.Duels.current_sw_winstreak",
            )
        )
    }

    override fun onJoinGame() {
        super.onJoinGame()
    }

    override fun onGameStart() {
        super.onGameStart()
    }

    override fun onGameEnd() {
        super.onGameEnd()
    }

    @SubscribeEvent
    fun onBoostClientTick(ev: ClientTickEvent) {
        if (mc.thePlayer != null && toggled()) {
            super.onTick()
        }
    }
}