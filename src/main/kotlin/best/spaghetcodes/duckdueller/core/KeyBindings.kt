package best.spaghetcodes.duckdueller.core

import best.spaghetcodes.duckdueller.DuckDueller
import gg.essential.api.EssentialAPI
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object KeyBindings {

    val toggleBotKeyBinding = KeyBinding("Toggle bot", Keyboard.KEY_SEMICOLON, "Elixir Client")
    val configGuiKeyBinding = KeyBinding("Config Gui", Keyboard.KEY_RSHIFT, "Elixir Client")

    fun register() {
        ClientRegistry.registerKeyBinding(toggleBotKeyBinding)
        ClientRegistry.registerKeyBinding(configGuiKeyBinding)
    }

    @SubscribeEvent
    fun onTick(ev: ClientTickEvent) {
        if (configGuiKeyBinding.isPressed) {
            EssentialAPI.getGuiUtil().openScreen(DuckDueller.config?.gui())
        }
    }

}