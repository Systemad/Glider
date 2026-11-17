package nl.viter.glider.feature.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.HnItem
import nl.viter.glider.shared.data.HnRepository

data class FeedState(
    val kind: String = "top",
    val rows: List<HnItem> = emptyList(),
    val loading: Boolean = false,
    val more: Boolean = true,
    val error: String? = null
)

class FeedViewModel(private val repository: HnRepository) : ViewModel() {
    private val mutable = MutableStateFlow(FeedState())
    val state = mutable.asStateFlow()
    private var offset = 0
    private var ids = emptyList<Long>()
    private var request: kotlinx.coroutines.Job? = null
    private var prefetched: Deferred<List<HnItem>?>? = null

    fun prefetch() {
        val current = mutable.value
        if (current.loading || current.error != null || offset >= ids.size || prefetched != null) {
            return
        }
        val batch = ids.drop(offset).take(20)
        prefetched = viewModelScope.async {
            try {
                repository.items(batch)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }
    init {
        load(refresh = true)
    }
    fun select(kind: String) {
        if (kind !=
            mutable.value.kind
        ) {
            mutable.value = FeedState(kind = kind)
            load(refresh = true)
        }
    }
    fun load(refresh: Boolean = false) {
        if (mutable.value.loading && !refresh) return
        if (refresh) {
            prefetched?.cancel()
            prefetched = null
        }
        request?.cancel()
        request = viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, error = null)
            try {
                if (refresh) {
                    ids = repository.feed(mutable.value.kind)
                    offset = 0
                }
                val existing = if (refresh) emptyList() else mutable.value.rows
                val next = prefetched?.await() ?: repository.items(ids.drop(offset).take(20))
                prefetched = null
                offset += 20
                mutable.value =
                    mutable.value.copy(
                        rows = existing + next,
                        loading = false,
                        more =
                            offset < ids.size
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.value =
                    mutable.value.copy(loading = false, error = "Could not load stories")
            }
        }
    }
}
