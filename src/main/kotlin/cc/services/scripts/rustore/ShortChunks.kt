package cc.services.scripts.rustore

import cc.services.scripts.QuestionGenerator
import cc.services.searches.noAnchor
import com.fasterxml.jackson.databind.node.ArrayNode
import com.mlp.sdk.utils.JSON
import java.io.File

data class RuStoreDataS(val title: String, val url: String, val text: String)

fun main() {

    val sourceData = File("../cc-data/rustore/short_fragments.json").readText()

    val a = JSON.parse(sourceData)
    val dd = (a as ArrayNode).flatMap { (it as ArrayNode).mapIndexed { i, it ->
        val v = JSON.parse<RuStoreDataS>(it)
        v.copy(url = noAnchor(v.url) + "#$i" )
    } }
        .filter { it.text.isNotBlank() }.filter { !it.url.contains("/en/") }

    val root = File("../cc-data/rustore/short_chunks")
    root.listFiles()?.forEach { it.delete() }

    dd.forEach { d ->
        File(root, urlToFile(d.url)).writeText(d.text)
    }

}

