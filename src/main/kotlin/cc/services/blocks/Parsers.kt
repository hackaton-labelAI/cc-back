package cc.services.blocks

import org.springframework.stereotype.Service

@Service
class Parsers {

    val fileLinkRe = Regex("/(?:gatsby|notion)/(?:[a-zA-Z]|[а-яА-Я]| |-|/|\\.)+")
    fun extractLinks(text: String): List<String> {
        val res = fileLinkRe.findAll(text).toList()
        val links = res.map {
            it.groupValues[0]
        }

        val l2 = links.distinct()

        return l2
    }

    fun parseNumberedList(text: String): List<String> {
        return text.lines().mapNotNull {
            if (it.getOrNull(1) != '.') {
                null
            } else {
                it.removeRange(0, 2).trim().removePrefix("\"").removeSuffix("\"")
            }
        }
    }

}
