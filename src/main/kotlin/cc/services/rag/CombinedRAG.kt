package cc.services.rag

import cc.endpoints.RAGConfig
import cc.services.blocks.CailaConnector
import cc.services.blocks.Prompts
import cc.services.searches.*
import cc.utils.consoleRunner
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.sdk.MlpServiceBase.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty("first-rag")
class CombinedRAG(
    val lucene: LuceneExpandedService,
    val vector: VectorSearchJVector,
    val connector: CailaConnector,
    val prompts: Prompts
) {

    val bigChunks = File("../cc-data/rustore/big_chunks").absolutePath
    val shortChunks = File("../cc-data/rustore/short_chunks").absolutePath

    suspend fun process(query: String, config: RAGConfig?, gen: ResultGenerator<ChatCompletionResult>) {
        val debug = config?.debug ?: false
        log.info(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>> ")
        log.info(query)

        if (debug) {
            gen.send("Search the documents...\n\n")
        }

        val sr = lucene.search(query)
        val data1 = sr.distinctBy { it.article }
            .filter { File("$bigChunks/${it.article}.txt").exists() }
            .take(3).map {
            BaselineTextRAG.Article(
                it.article,
                File("$bigChunks/${it.article}.txt").readText()
            )
        }

        if (debug) {
            gen.send("Docs from full-text search with query expansion:\n ${
                data1.map { "* ${it.name}" }.joinToString("\n")
            }\n\n")
        }

        val sr2 = vector.search(query)
        val data2 = sr2.distinctBy { noAnchor(it.article) }
            .filter { File("$bigChunks/${noAnchor(it.article)}.txt").exists() }
            .take(2)
            .map { BaselineTextRAG.Article(it.article, File("$bigChunks/${noAnchor(it.article)}.txt").readText()) }

        if (debug) {
            gen.send("Docs from vector search:\n ${
                data2.map { "* ${it.name}" }.joinToString("\n")
            }\n\n")
        }
        val data = data1 + data2

        // TODO: filter and rerank documents with NN..

        log.info(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>> ")
        val prompt = prompts.askToGenerateAnswer(query, data)
        log.info(prompt)

        log.info(" <<<<<<<<<<<<<<<<<<<<<<<<<<<<< ")
        connector.chatCompletion(prompt, debug, gen, config)
    }

    companion object {
        val log = LoggerFactory.getLogger(BaselineTextRAG::class.java)
    }

}

fun main() {
    val context = consoleRunner("first-rag.yml")

    val rag = context.getBean(BaselineTextRAG::class.java)

    rag.buildIndex()
}