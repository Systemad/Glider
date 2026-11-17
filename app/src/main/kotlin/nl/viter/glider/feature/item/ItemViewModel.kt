package nl.viter.glider.feature.item

import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.HnRepository

class ItemViewModel(private val id: Long, private val repository: HnRepository) : ViewModel() {
    private val mutable = MutableStateFlow(recentDiscussions.get(id) ?: ItemState())
    val state = mutable.asStateFlow()
    private var request: Job? = null
    init {
        refresh()
    }
    fun refresh() {
        request?.cancel()
        request = viewModelScope.launch {
            val previous = mutable.value.comments.map { it.item.id }.toSet()
            val hasPrevious = mutable.value.item != null
            mutable.value = mutable.value.copy(loading = true, error = null)
            try {
                val root = repository.item(id) ?: error("Item unavailable")
                mutable.value = mutable.value.copy(item = root)
                val comments = repository.discussion(root).map { (item, depth) ->
                    CommentRow(item, depth, hasPrevious && item.id !in previous)
                }
                val loaded = mutable.value.copy(comments = comments, loading = false)
                recentDiscussions.put(id, loaded)
                mutable.value = loaded
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.value =
                    mutable.value.copy(loading = false, error = "Could not load discussion")
            }
        }
    }

    private companion object {
        val recentDiscussions = LruCache<Long, ItemState>(6)
    }
}
