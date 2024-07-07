package cc.services.searches

import cc.services.analyzer.MyRussianAnalyzer
import cc.services.analyzer.MyStemService
import cc.utils.Loader
import cc.services.scripts.QA
import cc.services.scripts.rustore.urlToFile
import cc.utils.consoleRunner
import com.mlp.sdk.utils.JSON
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.store.NativeFSLockFactory
import org.apache.lucene.store.SingleInstanceLockFactory
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

@Service
@ConditionalOnProperty("lucene.indexPath")
class LuceneIndexService(
    @Value("\${lucene.indexPath}") private val indexPath: String,
    private val myStemService: MyStemService
): Searcher {

    val analyzer = RussianAnalyzer()
//    val analyzer = MyRussianAnalyzer(myStemService)

    override fun buildIndex(sourcePath0: String) {
        RussianAnalyzer()
        File(indexPath).deleteRecursively()
        File(indexPath).mkdirs()
        val sourcePath = File(sourcePath0).absolutePath
        val index: Directory = NIOFSDirectory(Path.of(indexPath), NativeFSLockFactory.INSTANCE)
        val config = IndexWriterConfig(analyzer)
        val writer = IndexWriter(index, config)

        val options = MutableDataSet()
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()

        val files =
            Loader.listFilesRecursive(sourcePath)
                .filter { it.endsWith(".md") || it.endsWith(".mdx") || it.endsWith(".txt") }

        files.forEach {
            try {
                val file = File(sourcePath + it)
                val markdown = file.readText()
                val document = parser.parse(markdown)
                val html = renderer.render(document)

                val jsoupDocument = Jsoup.parse(html)
                val titleElement = jsoupDocument.selectFirst("h1")
                val title = titleElement?.text() ?: ""
                titleElement?.remove()
                val text = jsoupDocument.body().text()

                val doc = Document()
                doc.add(TextField("title", title, Field.Store.YES))
                doc.add(TextField("text", text, Field.Store.YES))
                doc.add(StringField("filename", it, Field.Store.YES))
                writer.addDocument(doc)
            } catch (e: Exception) {
                log.error("Failed to extract text: ${e.message}", e)
            }
        }
        writer.close()
    }

    var searcher: IndexSearcher? = null

    override fun search(query: String): List<SearchResult> {
        if (searcher == null) {
            searcher = buildSearcher()
        }
        val queryParser = QueryParser("text", analyzer)
        val textQuery: Query = queryParser.parse(QueryParser.escape(query))

        val titleQuery: Query = TermQuery(Term("title", query))

        val boostedTitleQuery = BoostQuery(titleQuery, 50.0f)

        val booleanQuery =
            BooleanQuery.Builder()
                .add(textQuery, BooleanClause.Occur.SHOULD)
                .add(boostedTitleQuery, BooleanClause.Occur.SHOULD)
                .build()

        val docs = searcher!!.search(booleanQuery, 10)
        val hits: Array<ScoreDoc> = docs.scoreDocs

        val result = mutableListOf<SearchResult>()
        for (i in hits.indices) {
            val score = hits[i].score
            val docId = hits[i].doc
            val d = searcher!!.doc(docId)

            result.add(SearchResult(query, d.get("filename"), score, null))
//            log.info(
//                "Поисковый запрос $querystr.Приоретет ${i + 1}. Имя файла ${d.get("filename")}. Текст ${d.get("text")}. Увереность $score",
//            )
        }
        return result
    }

    private fun buildSearcher(): IndexSearcher? {
        val indexPath = Path.of(indexPath)
        if (indexPath.exists()) {
            val reader = DirectoryReader.open(NIOFSDirectory(indexPath, SingleInstanceLockFactory()))
            val searcher = IndexSearcher(reader)
            return searcher
        } else {
            return null
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(LuceneIndexService::class.java)
    }
}

fun noAnchor(url0: String): String {
    val url = url0.removePrefix("https://www.rustore.ru/help")
    val i = url.indexOf("#")
    if (i != -1)
        return url.substring(0, i)
    val i2 = url.indexOf(".")
    if (i2 != -1)
        return url.substring(0, i2)
    return url
}

fun searchEval(searcher: Searcher, data: List<QA>) {
    val counter = HashMap<Int, Int>()
    counter[0] = 0
    counter[1] = 0
    counter[2] = 0
    counter[3] = 0
    counter[4] = 0
    data.forEachIndexed { n, q ->
        if (n % 10 == 0) {
            println(" $n - ${counter.filterKeys { it in 0..4 }}")
        }
        val res = searcher.search(q.question)

        val i = res.indexOfFirst { r ->
            noAnchor(r.article) == noAnchor(q.article)
        }

        counter[i] = counter.getOrDefault(i, 0) + 1
    }

    val total = data.size
    var hits = 0
    (0 until counter.keys.max()).forEach {
        hits += counter.getOrDefault(it, 0)
        println(" $it: %.2f".format(hits.toDouble() / total * 100))
    }
}

fun main() {
    val context = consoleRunner("lucene-search.yml")

    val idx = context.getBean(LuceneIndexService::class.java)

    idx.buildIndex("../cc-data/rustore/short_chunks")
    val qa = JSON.parseList<QA>(File("../cc-data/rustore/generated_questions.json").readText())

//    idx.buildIndex("../cc-data/caila/source")
//    val qa = JSON.parseList<QA>(File("../cc-data/caila/qa.json").readText())

//    val searcher = context.getBean(LuceneNNRank::class.java)
    searchEval(idx, qa)
//    searchEval(searcher, qa)
}
