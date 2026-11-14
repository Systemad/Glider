package nl.viter.glider.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.viter.glider.shared.data.HnItem
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
                val loaded = mutableMapOf<Long, HnItem>()
                var failures = 0
                fun rows(): List<CommentRow> = buildList {
                    fun append(ids: List<Long>, depth: Int) {
                        for (childId in ids) {
                            val child = loaded[childId] ?: continue
                            add(CommentRow(child, depth, hasPrevious && child.id !in previous))
                            append(child.parts + child.kids, depth + 1)
                        }
                    }
                    append(root.parts + root.kids, 1)
                }
                var pending = root.parts + root.kids
                while (pending.isNotEmpty()) {
                    val level = coroutineScope {
                        pending.map { childId ->
                            async {
                                try {
                                    repository.items(listOf(childId)).firstOrNull()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }.awaitAll()
                    }
                    for (child in level) {
                        if (child == null) failures++ else loaded[child.id] = child
                    }
                    mutable.value = mutable.value.copy(comments = rows())
                    pending = level.filterNotNull().flatMap { it.parts + it.kids }
                }
                mutable.value =
                    mutable.value.copy(
                        loading = false,
                        error = if (failures ==
                            0
                        ) {
                            null
                        } else {
                            "Some replies could not be loaded. Retry to reload."
                        }
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.value =
                    mutable.value.copy(loading = false, error = "Could not load discussion")
            }
        }
    }
}
