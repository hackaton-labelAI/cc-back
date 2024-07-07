package cc.services.searches

import cc.services.blocks.CailaConnector
import cc.services.blocks.Parsers
import cc.services.blocks.Prompts
import cc.utils.JSON
import org.apache.lucene.search.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnBean(value = [LuceneIndexService::class, CailaConnector::class])
class LuceneNNRank(
    private val searcher: LuceneExpandedService,
    private val caila: CailaConnector,
    private val prompts: Prompts,
    private val parser: Parsers
): Searcher {

    override fun buildIndex(sourcePath: String) {
        searcher.buildIndex(sourcePath)
    }

    val summaries = JSON.parseList<NeuroSummarySearch.ShortInfo>(File("../cc-data/rustore/summary_info.json").readText())


    override fun search(query: String): List<SearchResult> {
        val res = searcher.search(query)

        val p = prompts.rerankBySummary(query, res.map { r ->
            NeuroSummarySearch.ShortInfo(r.article, summaries.find { s -> noAnchor(s.shortName) == r.article }!!.summary) })
//        println(">>>>>>>>>>>>>>")
//        println(p)
        val r = caila.chatCompletion(p)
//        println("<<<<<<<<<<<<<<")
//        println(r.output)

        val rr = parser.parseNumberedList(r.output)

        val rrr = rr.mapNotNull { n -> res.find { o -> o.article == n } }

        return rrr
    }

}
