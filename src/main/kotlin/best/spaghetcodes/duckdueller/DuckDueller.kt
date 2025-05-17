package best.spaghetcodes.duckdueller

import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.bot.StateManager
import best.spaghetcodes.duckdueller.bot.player.LobbyMovement
import best.spaghetcodes.duckdueller.bot.player.Mouse
import best.spaghetcodes.duckdueller.commands.ConfigCommand
import best.spaghetcodes.duckdueller.core.Config
import best.spaghetcodes.duckdueller.core.KeyBindings
import best.spaghetcodes.duckdueller.events.packet.PacketListener
import best.spaghetcodes.duckdueller.utils.ChatUtils
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent

@Mod(
    modid = DuckDueller.MOD_ID,
    name = DuckDueller.MOD_NAME,
    version = DuckDueller.VERSION
)
class DuckDueller {

    companion object {
        const val MOD_ID = "duckdueller"
        const val MOD_NAME = "DuckDueller"
        const val VERSION = "0.1.0"
        const val configLocation = "./config/duckdueller.toml"

        val mc: Minecraft by lazy { Minecraft.getMinecraft() }
        val gson = com.google.gson.Gson()
        var config: Config? = null
        var bot: BotBase? = null

        fun updateActiveBot(
            newReplayClearingModeState: Boolean? = null,
            newBoostingModeState: Boolean? = null,
            newRegularBotIndex: Int? = null,
            newBoostingBotIndex: Int? = null
        ) {
            val localConfig = config
            if (localConfig == null) {
                return
            }

            val effectiveReplayClearingEnabled = newReplayClearingModeState ?: localConfig.enableReplayClearingMode
            val effectiveBoostingEnabled = newBoostingModeState ?: localConfig.enableBoostingMode

            val effectiveRegularBotIdx = newRegularBotIndex ?: localConfig.currentBot
            val effectiveBoostingBotIdx = newBoostingBotIndex ?: localConfig.selectedBoostingBotIndex

            var newBotToSelect: BotBase? = null

            if (effectiveReplayClearingEnabled) {
                newBotToSelect = localConfig.replayClearingBotInstance
            } else if (effectiveBoostingEnabled) {
                if (localConfig.boostingBotInstances.isNotEmpty()) {
                    val minIdx = 0
                    val maxIdx = localConfig.boostingBotInstances.size - 1
                    val actualIndexToUse = effectiveBoostingBotIdx.coerceIn(minIdx, maxIdx)
                    newBotToSelect = localConfig.boostingBotInstances.getOrNull(actualIndexToUse)
                } else {
                    newBotToSelect = null
                }

            } else {
                newBotToSelect = localConfig.bots[effectiveRegularBotIdx]
                if (newBotToSelect == null && localConfig.bots.isNotEmpty()) {
                    val fallbackIndex = localConfig.bots.keys.minOrNull() ?: 0
                    newBotToSelect = localConfig.bots[fallbackIndex]
                }
            }

            if (bot === newBotToSelect && bot != null) {
                return
            }

            val oldSelectedBot = bot
            if (oldSelectedBot != null) {
                try {
                    MinecraftForge.EVENT_BUS.unregister(oldSelectedBot)
                } catch (e: Exception) {
                }
            }

            bot = newBotToSelect

            if (bot != null) {
                try {
                    MinecraftForge.EVENT_BUS.register(bot)
                } catch (e: Exception) {
                    bot = null
                }
            }
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        try {
            config = Config()

            ConfigCommand().register()
            KeyBindings.register()
            MinecraftForge.EVENT_BUS.register(this)
            MinecraftForge.EVENT_BUS.register(PacketListener())
            MinecraftForge.EVENT_BUS.register(StateManager)
            MinecraftForge.EVENT_BUS.register(Mouse)
            MinecraftForge.EVENT_BUS.register(LobbyMovement)
            MinecraftForge.EVENT_BUS.register(KeyBindings)
            updateActiveBot()

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}