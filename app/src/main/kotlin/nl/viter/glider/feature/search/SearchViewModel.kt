package nl.viter.glider.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import nl.viter.glider.shared.data.HnItem
import nl.viter.glider.shared.data.HnRepository

internal data class SearchRequest(
    val query: String = "",
    val tags: String = "story",
    val since: Long? = null,
    val until: Long? = null,
    val byDate: Boolean = false,
    val page: Int = 0,
    val revision: Int = 0
)
internal data class SearchUiState(
    val request: SearchRequest,
    val results: List<HnItem> = emptyList(),
    val loading: Boolean = true,
    val hasMore: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class SearchViewModel(
    private val repository: HnRepository,
    tags: String,
    since: Long?,
    until: Long?,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val requests = MutableStateFlow(
        SearchRequest(
            query = savedState["query"] ?: "",
            tags = savedState["tags"] ?: tags,
            since = since,
            until = until,
            byDate = savedState["byDate"] ?: false
        )
    )
    val uiState: StateFlow<SearchUiState> = requests.flatMapLatest { request ->
        flow {
            val existing = if (request.page == 0) emptyList() else uiState.value.results
            emit(SearchUiState(request, existing))
            delay(300)
            val result = repository.search(
                request.query,
                request.page,
                request.tags,
                request.since,
                request.until,
                request.byDate
            )
            emit(
                SearchUiState(
                    request,
                    (existing + result.items).distinctBy { it.id },
                    false,
                    request.page + 1 < result.pages
                )
            )
        }.catch {
            emit(
                SearchUiState(
                    request,
                    if (request.page ==
                        0
                    ) {
                        emptyList()
                    } else {
                        uiState.value.results
                    },
                    loading = false,
                    error = "Could not load results"
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState(requests.value))

    fun query(value: String) {
        savedState["query"] = value
        requests.update { it.copy(query = value, page = 0) }
    }
    fun sort(byDate: Boolean) {
        savedState["byDate"] = byDate
        requests.update { it.copy(byDate = byDate, page = 0) }
    }
    fun toggleComments() {
        requests.update {
            it.copy(
                tags = if (it.tags ==
                    "comment"
                ) {
                    "story"
                } else {
                    "comment"
                },
                page = 0
            )
        }
        savedState["tags"] = requests.value.tags
    }
    fun range(since: Long?, until: Long?) {
        if (requests.value.since != since ||
            requests.value.until != until
        ) {
            requests.update { it.copy(since = since, until = until, page = 0) }
        }
    }
    fun loadMore() {
        if (!uiState.value.loading &&
            uiState.value.hasMore
        ) {
            requests.update { it.copy(page = it.page + 1) }
        }
    }
    fun retry() {
        requests.update { it.copy(revision = it.revision + 1) }
    }
}
