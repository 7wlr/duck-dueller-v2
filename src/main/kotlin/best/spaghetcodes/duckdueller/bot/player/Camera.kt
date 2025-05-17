package best.spaghetcodes.duckdueller.bot.player

import best.spaghetcodes.duckdueller.DuckDueller
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.jetbrains.annotations.NotNull

object Camera {

    private var isManuallyEnabled: Boolean = false

    fun enable() {
        isManuallyEnabled = true
    }

    fun disable() {
        isManuallyEnabled = false
    }

    @SubscribeEvent
    fun onCameraSetup(@NotNull event: CameraSetup) {
        val config = DuckDueller.config ?: return
        if (!config.enableCustomCamera || !isManuallyEnabled) {
            return
        }

        val mc = Minecraft.getMinecraft()
        val player = mc.thePlayer ?: return

        val translateX = -config.cameraOffsetX
        val translateY = -config.cameraOffsetY
        val translateZ = -config.cameraOffsetZ

        GlStateManager.translate(translateX, translateY, translateZ)

        event.pitch = config.cameraPitch
        event.yaw = config.cameraYaw
    }
}
