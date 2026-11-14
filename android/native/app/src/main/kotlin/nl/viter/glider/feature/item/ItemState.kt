package nl.viter.glider.feature.item

import nl.viter.glider.shared.data.HnItem

data class CommentRow(val item: HnItem, val depth: Int, val isNew: Boolean = false)
data class ItemState(
    val item: HnItem? = null,
    val comments: List<CommentRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)
