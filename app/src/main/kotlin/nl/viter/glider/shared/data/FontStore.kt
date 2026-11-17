package nl.viter.glider.shared.data

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Google Fonts' OFL families; cached locally after the first selection. */
@Singleton
class FontStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val client = HttpClient(OkHttp) { expectSuccess = true }
    suspend fun load(name: String): FontFamily = withContext(Dispatchers.IO) {
        val family = name.lowercase().replace(" ", "")
        val file = File(context.filesDir, "fonts/$family.ttf")
        if (!file.exists()) {
            val entries = Json.parseToJsonElement(
                client.get(
                    "https://api.github.com/repos/google/fonts/contents/ofl/$family"
                ).bodyAsText()
            ).jsonArray
            val entry = entries.first {
                val filename = it.jsonObject["name"]!!.jsonPrimitive.content
                filename.endsWith(".ttf") &&
                    !filename.contains("Italic")
            }
            val url = entry.jsonObject["download_url"]!!.jsonPrimitive.content
            val bytes = client.get(url).readRawBytes()
            file.parentFile!!.mkdirs()
            val temporary = File(file.parentFile, "$family.tmp")
            temporary.writeBytes(bytes)
            check(temporary.renameTo(file))
        }
        FontFamily(Font(file))
    }
}
