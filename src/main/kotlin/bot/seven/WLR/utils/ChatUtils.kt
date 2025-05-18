package bot.seven.WLR.utils

import bot.seven.WLR.wlr
import bot.seven.WLR.core.Config
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

object ChatUtils {

    fun removeFormatting(text: String): String {
        var t = ""
        var skip = false
        for (i in text.indices) {
            if (!skip) {
                if (text[i] == '§') {
                    skip = true
                } else {
                    t += text[i]
                }
            } else {
                skip = false
            }
        }
        return t
    }

    fun sendAsPlayer(message: String) {
        if (wlr.mc.thePlayer != null) {
            wlr.mc.thePlayer.sendChatMessage(message)
        }
    }

    fun info(message: String) {
        sendChatMessage("${EnumChatFormatting.DARK_PURPLE}[${EnumChatFormatting.LIGHT_PURPLE}${EnumChatFormatting.BOLD}WLR${EnumChatFormatting.RESET}${EnumChatFormatting.DARK_PURPLE}] ${EnumChatFormatting.WHITE}$message")
    }

    fun error(message: String) {
        sendChatMessage("${EnumChatFormatting.DARK_PURPLE}[${EnumChatFormatting.LIGHT_PURPLE}${EnumChatFormatting.BOLD}WLR${EnumChatFormatting.RESET}${EnumChatFormatting.DARK_PURPLE}] ${EnumChatFormatting.RED}$message")
    }

    private fun sendChatMessage(message: String) {
        // CORRECTED ACCESS:
        if (wlr.mc.thePlayer != null && !Config.disableChatMessages) {
            wlr.mc.thePlayer.addChatMessage(ChatComponentText(message))
        }
    }

}