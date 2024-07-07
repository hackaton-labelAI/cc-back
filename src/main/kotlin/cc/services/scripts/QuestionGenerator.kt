package cc.services.scripts

import cc.services.blocks.CailaConnector
import cc.services.blocks.Parsers
import cc.services.blocks.Prompts
import cc.utils.Loader.listFilesRecursive
import cc.utils.consoleRunner
import com.mlp.sdk.utils.JSON
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File

data class QA(val question: String, val article: String)

@Service
@ConditionalOnProperty("question-generator")
class QuestionGenerator(
    val caila: CailaConnector,
    val prompts: Prompts,
    val parsers: Parsers
) {

    fun go(source: String, target: String, fileCount: Int) {
        val folder = File(source).absoluteFile
        val files = listFilesRecursive(folder.path).shuffled().take(fileCount)

        val collector = ArrayList<QA>()
        files.forEach { fn ->
            val content = File(folder, fn).readText()

            val prompt = prompts.generateQuestionsByArticle(content, 5, 1)
            try {
                val res = caila.chatCompletion(prompt)
                val qq = parsers.parseNumberedList(res.output)

                qq.forEach {q ->
                    collector.add(QA(q, fn))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        File(target).writeText(JSON.stringify(collector))
    }


}

//fun main() {
////    QuestionGenerator(CailaConnector()).go()
//
//    val r = JSON.parseList<ObjectNode>(File("questions.json").readText())
//
//    fun parseQuestions(text: String): List<String> {
//        return text.lines().mapNotNull {
//            if (it.getOrNull(1) != '.') {
//                null
//            } else {
//                it.removeRange(0, 2).trim()
//            }
//        }
//    }
//
//    val qa = r.map { parseQuestions(it.get("questions").asText()).map { q -> QA(q, removeNotionSalt(it.get("file").asText())) } }.flatten()
//
//    println(qa.size)
//    File("qa.json").writeText(JSON.stringify(qa))
//}


fun main() {
    val context = consoleRunner("question-generator.yml")

    val qg = context.getBean(QuestionGenerator::class.java)

    qg.go("../cc-data/rustore/big_chunks", "../cc-data/rustore/generated_questions.json", 100)
}
