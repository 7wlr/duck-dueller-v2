package best.spaghetcodes.duckdueller.utils

import best.spaghetcodes.duckdueller.DuckDueller
import com.google.gson.JsonObject
import java.io.IOException
import java.net.URL


object HttpUtils {

    fun usernameToUUID(username: String): String? {
        val url = URL("https://api.mojang.com/users/profiles/minecraft/$username")
        val json = url.readText()
        if (json == "") { // invalid username
            return null
        }
        val obj = DuckDueller.gson.fromJson(json, JsonObject::class.java)
        return obj.get("id").asString
    }

}