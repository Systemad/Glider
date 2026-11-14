package nl.viter.glider.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.HnRepository

class ItemViewModel(private val id: Long, private val repository: HnRepository) : ViewModel() {
    private val mutable = MutableStateFlow(ItemState())
    val state = mutable.asStateFlow()
    private var request: kotlinx.coroutines.Job? = null
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
                mutable.value = ItemState(item = root, loading = true)
                val rows = mutableListOf<CommentRow>()
                suspend fun children(ids: List<Long>, depth: Int) {
                    for (child in repository.items(ids)) {
                        rows += CommentRow(
                            child,
                            depth,
                            isNew = hasPrevious && child.id !in previous
                        )
                        mutable.value = mutable.value.copy(comments = rows.toList())
                        children(child.kids, depth + 1)
                    }
                }
                children(root.parts + root.kids, 1)
                mutable.value = mutable.value.copy(loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.value =
                    mutable.value.copy(loading = false, error = "Could not load discussion")
            }
        }
    }
}
