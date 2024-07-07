package cc.services.rag

import cc.endpoints.RAGConfig
import cc.services.blocks.CailaConnector
import cc.services.blocks.Prompts
import cc.services.searches.VectorSearchJVector
import cc.utils.consoleRunner
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.sdk.MlpServiceBase.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty("first-rag")
class BaselineVectorRAG(
    val searcher: VectorSearchJVector,
    val connector: CailaConnector,
    val prompts: Prompts
) {

    val sourceData = File("../cc-data/rustore/short_chunks").absolutePath

//    @PostConstruct
    fun buildIndex() {
        searcher.buildIndex(sourceData)
    }

    suspend fun process(query: String, config: RAGConfig?, gen: ResultGenerator<ChatCompletionResult>) {
        val debug = config?.debug ?: false
        log.info(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>> ")
        log.info(query)

        if (debug) {
            gen.send("Performing vector search with ada2 embeddings...\n\n")
        }

        val sr = searcher.search(query)
        val data = sr.filter { File("$sourceData/${it.article}").exists() }.distinctBy { it.article }
            .take(5)
            .map { BaselineTextRAG.Article(it.article, File("$sourceData/${it.article}").readText()) }

        if (debug) {
            gen.send("Found documents:\n ${
                data.map { "* ${it.name}" }.joinToString("\n")
            }\n\n")
        }

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