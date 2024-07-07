package cc.services.searches

data class SearchResult(
    val query: String,
    val article: String,
    val score: Float = 1f,
    val span: IntRange? = null
)

interface Searcher {

    fun buildIndex(folder: String)

    fun search(query: String): List<SearchResult>

}