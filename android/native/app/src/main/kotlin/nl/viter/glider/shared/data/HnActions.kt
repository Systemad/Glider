package nl.viter.glider.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class HnActions(private val store: AppStore) {
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        followRedirects = false
    }
    private suspend fun page(path: String): Document {
        val cookie = store.session() ?: error("Please log in first")
        val response = client.get("https://news.ycombinator.com/$path") {
            header(HttpHeaders.Cookie, "user=$cookie")
        }
        val document = Jsoup.parse(response.bodyAsText(), "https://news.ycombinator.com/")
        check(document.selectFirst("input[name=pw]") == null) { "Please log in again" }
        check(document.selectFirst(".g-recaptcha") == null) {
            "HN requires a challenge. Open Account to complete it."
        }
        return document
    }
    private suspend fun post(path: String, fields: Map<String, String>) {
        val response = client.post("https://news.ycombinator.com/$path") {
            header(HttpHeaders.Cookie, "user=${store.session() ?: error("Please log in first")}")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(fields.toList().formUrlEncode())
        }
        val body = response.bodyAsText()
        check(response.headers[HttpHeaders.Location]?.contains("login") != true) {
            "Please log in again"
        }
        check(response.status.value in 200..399) { "HN rejected this action" }
        check(!body.contains("recaptcha", true)) {
            "HN requires a challenge. Complete it in Account."
        }
        check(!body.contains("input name=\"pw\"")) { "Please log in again" }
        if (response.status.value == 200 && body.isNotBlank()) {
            val document = Jsoup.parse(body)
            check(document.selectFirst("table#hnmain") != null || body.trim() == "OK") {
                document.text().take(180)
            }
        }
    }
    suspend fun action(item: HnItem, kind: String) {
        val current = store.state.value
        if (kind == "favorite" && store.session() == null) {
            store.update {
                it.copy(
                    favorites = if (item.id in
                        it.favorites
                    ) {
                        it.favorites - item.id
                    } else {
                        it.favorites + item.id
                    }
                )
            }
            return
        }
        val doc = page("item?id=${item.id}")
        val prefix = when (kind) {
            "favorite" -> "fave?"
            "flag" -> "flag?"
            else -> "vote?"
        }
        val authLink = doc.select("a[href]").firstOrNull {
            it.attr("href").startsWith(prefix) &&
                Url("https://news.ycombinator.com/${it.attr("href")}").parameters["id"] ==
                item.id.toString()
        }
        val auth =
            authLink?.attr("href")?.let {
                Url("https://news.ycombinator.com/$it").parameters["auth"]
            }
                ?: error("This action is unavailable")
        val fields = mutableMapOf("id" to item.id.toString(), "auth" to auth)
        val path = when (kind) {
            "upvote" -> {
                fields["how"] = if (item.id in current.upvoted) "un" else "up"
                "vote"
            }

            "downvote" -> {
                fields["how"] = if (item.id in
                    current.downvoted
                ) {
                    "un"
                } else {
                    "down"
                }
                "vote"
            }

            "favorite" -> {
                if (item.id in current.favorites) fields["un"] = "t"
                "fave"
            }

            "flag" -> {
                if (item.id in current.flagged) fields["un"] = "t"
                "flag"
            }

            else -> error("Unknown action")
        }
        post(path, fields)
        fun Set<Long>.toggle() = if (item.id in this) this - item.id else this + item.id
        store.update {
            when (kind) {
                "upvote" -> it.copy(
                    upvoted = it.upvoted.toggle(),
                    downvoted = it.downvoted - item.id
                )

                "downvote" -> it.copy(
                    downvoted = it.downvoted.toggle(),
                    upvoted = it.upvoted - item.id
                )

                "favorite" -> it.copy(favorites = it.favorites.toggle())

                else -> it.copy(flagged = it.flagged.toggle())
            }
        }
    }
    suspend fun write(mode: String, id: Long?, title: String, url: String, text: String) {
        val doc =
            page(
                when (mode) {
                    "Submit" -> "submit"
                    "Edit" -> "edit?id=$id"
                    else -> "item?id=$id"
                }
            )
        val fields = doc.select("input[type=hidden][name]").associate {
            it.attr("name") to
                it.attr("value")
        }.toMutableMap()
        fields["text"] = text
        val path = when (mode) {
            "Submit" -> {
                fields["title"] = title
                fields["url"] = url
                "r"
            }

            "Edit" -> {
                fields["id"] = "$id"
                fields["title"] = title
                "xedit"
            }

            else -> {
                fields["parent"] = "$id"
                fields["goto"] = "item?id=$id"
                "comment"
            }
        }
        post(path, fields)
    }
    suspend fun delete(id: Long) {
        val doc = page("delete-confirm?id=$id")
        val hmac = doc.selectFirst("input[name=hmac]")?.attr("value") ?: error("Delete unavailable")
        post("xdelete", mapOf("id" to "$id", "d" to "Yes", "hmac" to hmac))
    }
    suspend fun editDraft(id: Long): Pair<String, String> {
        val doc = page("edit?id=$id")
        return doc.selectFirst("input[name=title]")?.attr("value").orEmpty() to
            doc.selectFirst("textarea[name=text]")?.wholeText().orEmpty()
    }
    suspend fun synchronize() {
        val username = store.state.value.username
        require(username.isNotBlank())
        suspend fun collect(endpoint: String): Set<Long> {
            val ids = mutableSetOf<Long>()
            for (comments in listOf(false, true)) {
                val suffix = if (comments) {
                    if (endpoint ==
                        "flagged"
                    ) {
                        "&kind=comment"
                    } else {
                        "&comments=t"
                    }
                } else {
                    ""
                }
                var path: String? = "$endpoint?id=${username.encodeURLParameter()}$suffix"
                val seen = mutableSetOf<String>()
                while (path != null && seen.add(path)) {
                    val doc = page(path)
                    check(doc.selectFirst("table#hnmain") != null) {
                        "Could not synchronize $endpoint"
                    }
                    doc.select("tr.athing[id]").mapNotNullTo(ids) { it.id().toLongOrNull() }
                    path = doc.selectFirst("a.morelink")?.attr("href")
                    check(path == null || (!path.contains(":") && !path.startsWith("//")))
                }
            }
            return ids
        }
        val favorites = collect("favorites")
        val upvoted = collect("upvoted")
        val flagged = collect("flagged")
        store.update {
            it.copy(
                favorites = it.favorites + favorites,
                upvoted = it.upvoted + upvoted,
                flagged =
                    it.flagged + flagged
            )
        }
    }
}
