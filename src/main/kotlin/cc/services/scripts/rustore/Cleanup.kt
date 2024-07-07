package cc.services.scripts.rustore

import cc.services.scripts.QuestionGenerator
import com.mlp.sdk.utils.JSON
import java.io.File

data class RuStoreData(val label: String, val url: String, val text: String, val links: List<String>)

fun main() {

    val sourceData = File("../cc-data/rustore/rustore_help_full.json").readText()

    val data = JSON.parseList<RuStoreData>(sourceData).sortedBy { it.text.length }
        .filter { it.text.length in 1201..29999 }
        .filter { !it.url.contains("/en/") }

    val root = File("../cc-data/rustore/big_chunks")
    root.listFiles()?.forEach { it.delete() }

    data.forEach { d ->
        println(" > " + d.url)
        val fn = urlToFile(d.url)
        println(" - " + fn)
        runCatching { File(root, fn).writeText(d.text)  }
            .onFailure {
                println(fn)
            }
    }

}

fun urlToFile(url: String): String {
    return url.removePrefix("https://www.rustore.ru/help/").removePrefix("/")
        .replace(Regex("(/)"), "_") + ".txt"

}