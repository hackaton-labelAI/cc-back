package cc.services.searches

import cc.services.blocks.CailaConnector
import cc.services.blocks.Parsers
import cc.services.blocks.Prompts
import cc.utils.Loader
import cc.utils.consoleRunner
import com.mlp.sdk.utils.JSON
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty("neuro-search")
class NeuroSummarySearch(
    val prompts: Prompts,
    val parsers: Parsers,
    val connector: CailaConnector): Searcher {

    data class ShortInfo(val shortName: String, val summary: String)

    override fun buildIndex(folder: String) {
        val files =
            Loader.listFilesRecursive(folder)
                .filter { it.endsWith(".md") || it.endsWith(".mdx") }

        // summarize and save
        val data = ArrayList<ShortInfo>()
        val dataFile = File("/tmp/summary.json")
        files.forEachIndexed { index, it ->
            val name = Loader.removeNotionSalt(it)
            val content = File(folder + it).readText()

            val prompt = prompts.summarize(content)

            val res = connector.chatCompletion(prompt)

            data.add(ShortInfo(
                shortName = name,
                summary = res.output
            ))
            dataFile.writeText(JSON.stringify(data))
            println("$index -> ${res.output}")
        }
    }

    override fun search(query: String): List<SearchResult> {
        val dataFile = File("/tmp/summary.json")
        val data = JSON.parseList<ShortInfo>(dataFile.readText())

        val prompt = prompts.askToFindRelevantArticles(query, data)

        println(prompt)

        val res = connector.chatCompletion(prompt)
        println(res.output)

        val links = parsers.extractLinks(res.output)

        return links.map { SearchResult(query, it) }
    }
}

fun main(args: Array<String>) {
    val context = consoleRunner("neuro-search.yml")

    // Получаем бины из контекста
    val runner = context.getBean(NeuroSummarySearch::class.java)

    // Вызываем метод run() у Runner
//    runner.buildIndex("../cc-data/caila/source")
    val res = runner.search("Что такое Caila?")

    println(res)
}
