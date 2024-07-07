package cc.services.scripts

import cc.services.blocks.CailaConnector
import cc.services.blocks.Parsers
import cc.services.blocks.Prompts
import cc.services.searches.NeuroSummarySearch
import cc.utils.Loader.listFilesRecursive
import cc.utils.consoleRunner
import com.mlp.sdk.utils.JSON
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

@Service
@ConditionalOnProperty("summarize")
class Summarization(
    val caila: CailaConnector,
    val prompts: Prompts,
    val parsers: Parsers
) {

    fun go(source: String, target: String) {
        val folder = File(source).absoluteFile
        val files = listFilesRecursive(folder.path)

        val collector = ArrayList<NeuroSummarySearch.ShortInfo>()
        files.forEach { fn ->
            val content = File(folder, fn).readText()

            val prompt = prompts.summarize(content)
            try {
                val res = caila.chatCompletion(prompt)

                println("$fn -> ${res.output}")
                collector.add(NeuroSummarySearch.ShortInfo(fn, res.output))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        File(target).writeText(JSON.stringify(collector))
    }


}

fun main() {
    val context = consoleRunner("summarize.yml")

    context.getBean(Summarization::class.java)
        .go("../cc-data/rustore/big_chunks", "../cc-data/rustore/summary_info.json")
}
