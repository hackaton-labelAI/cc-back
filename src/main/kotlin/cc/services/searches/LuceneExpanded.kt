package cc.services.searches

import cc.services.blocks.CailaConnector
import cc.services.blocks.Parsers
import cc.services.blocks.Prompts
import org.apache.lucene.search.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnBean(value = [LuceneIndexService::class, CailaConnector::class])
class LuceneExpandedService(
    private val searcher: LuceneIndexService,
    private val caila: CailaConnector,
    private val prompts: Prompts,
    private val parser: Parsers
): Searcher {

    override fun buildIndex(sourcePath: String) {
        searcher.buildIndex(sourcePath)
    }

    override fun search(query: String): List<SearchResult> {

        val p = prompts.expandQuery2(query)
        val cr = caila.chatCompletion(p)
        val variants = parser.parseNumberedList(cr.output) + listOf(query)

        val res = variants.flatMap { searcher.search(it) }
            .map { it.copy(article = noAnchor(it.article)) }
            .sortedBy { -it.score }.distinctBy { it.article }

        return res
    }
}
