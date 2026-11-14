package nl.viter.glider.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class HnItem(
    val id: Long,
    val type: String = "story",
    val title: String = "",
    val by: String = "",
    val url: String = "",
    val text: String = "",
    val score: Int = 0,
    val descendants: Int = 0,
    val time: Long = 0,
    val kids: List<Long> = emptyList(),
    val deleted: Boolean = false,
    val dead: Boolean = false,
    val parent: Long? = null,
    val parts: List<Long> = emptyList(),
    val poll: Long? = null
)

@Serializable
data class HnUser(
    val id: String,
    val created: Long = 0,
    val karma: Int = 0,
    val about: String = "",
    val submitted: List<Long> = emptyList()
)
data class SearchPage(val items: List<HnItem>, val page: Int, val pages: Int)

@Singleton
class HnRepository @Inject constructor(private val cache: ItemCache) {
    val client = HttpClient(OkHttp) { expectSuccess = true }
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val requests = Semaphore(8)
    suspend fun feed(kind: String): List<Long> {
        try {
            val body = client.get("$BASE/${kind}stories.json").bodyAsText()
            val ids = json.decodeFromString<List<Long>>(body)
            cache.dao.put(CachedFeed(kind, body))
            return ids
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return cache.dao.feed(kind)?.let {
                json.decodeFromString<List<Long>>(it.ids)
            } ?: throw e
        }
    }
    suspend fun item(id: Long): HnItem? {
        try {
            val item = json.decodeFromString<HnItem?>(
                client.get("$BASE/item/$id.json").bodyAsText()
            )
            if (item !=
                null
            ) {
                cache.dao.put(
                    CachedItem(id, json.encodeToString(item), System.currentTimeMillis())
                )
            }
            return item
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return cache.dao.get(id)?.let { json.decodeFromString<HnItem>(it.json) } ?: throw e
        }
    }
    suspend fun user(name: String): HnUser = json.decodeFromString(
        client.get("$BASE/user/${name.encodeURLPathPart()}.json").bodyAsText()
    )
    suspend fun items(ids: List<Long>): List<HnItem> = coroutineScope {
        ids.map { id -> async { requests.withPermit { item(id) } } }.awaitAll().filterNotNull()
    }
    suspend fun search(
        query: String = "",
        page: Int = 0,
        tags: String = "story",
        since: Long? = null,
        until: Long? = null,
        byDate: Boolean = false,
        numericFilter: String? = null,
        urlOnly: Boolean = false
    ): SearchPage {
        val body = client.get(
            "https://hn.algolia.com/api/v1/${if (byDate) "search_by_date" else "search"}"
        ) {
            if (urlOnly) parameter("restrictSearchableAttributes", "url")
            parameter("query", query)
            parameter("tags", tags)
            parameter("page", page)
            parameter("hitsPerPage", 30)
            val filters =
                listOfNotNull(
                    numericFilter,
                    since?.let {
                        "created_at_i>=$it"
                    },
                    until?.let { "created_at_i<=$it" }
                )
            if (filters.isNotEmpty()) parameter("numericFilters", filters.joinToString(","))
        }.bodyAsText()
        val data = json.parseToJsonElement(body).jsonObject
        val hits = data["hits"]!!.jsonArray.map { hit ->
            val obj = hit.jsonObject
            fun value(key: String) = obj[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            HnItem(
                id = value(
                    "objectID"
                ).toLong(),
                type = if (value("comment_text").isNotBlank()) "comment" else "story",
                title = value("title"), by = value("author"), url = value("url"),
                text = value("comment_text").ifBlank {
                    value("story_text")
                },
                score = value("points").toIntOrNull() ?: 0,
                descendants =
                    value("num_comments").toIntOrNull() ?: 0,
                time = value(
                    "created_at_i"
                ).toLongOrNull() ?: 0,
                parent = value("parent_id").toLongOrNull()
            )
        }
        return SearchPage(hits, page, data["nbPages"]?.jsonPrimitive?.int ?: 0)
    }
    suspend fun replies(username: String): List<Pair<HnItem, HnItem>> {
        val ids = user(username).submitted.take(30)
        if (ids.isEmpty()) return emptyList()
        val replies = search(
            tags = "comment",
            byDate = true,
            numericFilter = ids.joinToString(",", "(", ")") {
                "parent_id=$it"
            }
        ).items
        val parents = items(replies.mapNotNull { it.parent }.distinct()).associateBy { it.id }
        return replies.mapNotNull { reply -> parents[reply.parent]?.let { it to reply } }
    }
    suspend fun similar(item: HnItem): List<HnItem> = search(
        query = item.url,
        tags = "(story,poll)",
        urlOnly = true,
        numericFilter = "num_comments>0"
    ).items.filter {
        it.id !=
            item.id
    }.take(3)
    suspend fun pageTitle(url: String): String {
        require(Url(url).protocol in listOf(URLProtocol.HTTP, URLProtocol.HTTPS))
        return org.jsoup.Jsoup.parse(client.get(url).bodyAsText()).title()
    }
    fun close() = client.close()
    companion object {
        private const val BASE = "https://hacker-news.firebaseio.com/v0"
    }
}
