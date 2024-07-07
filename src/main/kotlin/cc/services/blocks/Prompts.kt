package cc.services.blocks

import cc.services.rag.BaselineTextRAG
import cc.services.searches.NeuroSummarySearch
import org.springframework.stereotype.Service

@Service
class Prompts {

    fun summarize(content: String): String {
        val prompt = """
Ты выступаешь в роли технического писателя.
Сейчас мы анализируем существующую документацию и проводим её ревью.
Напиши короткое саммари, о чём эта статья и какие ключевые мысли в ней излагаются. Описания должно быть достаточно для того, чтобы прочитав его, мы могли найти для этой статьи подходящее место в оглавлении.
Описание не должно превышать 100 слов.

Вот текст статьи в md формате.

${content}
        """.trimIndent()
        return prompt
    }

    fun askToFindRelevantArticles(query: String, files: List<NeuroSummarySearch.ShortInfo>): String {
        val prompt = """
Ты выступаешь в роли ассистента и помогаешь пользователю ответить на вопрос.

Сейчас я перечислю список статей документации, которые у нас имеются и задам вопрос. Прочитай внимательно описание всех имеющихся статей и скажи, в каких статьях мне надо искать ответ на вопрос пользователя.

Список файлов:
${files.map { "${it.shortName} -> ${it.summary}" }.joinToString("\n") }

Вопрос пользователя:
${query}

Напиши 5 статей, в которых может содержаться ответ. Отсортируй список по релевантности. Выведи только названия файлов, без описания.
        """.trimIndent()
        return prompt
    }

    fun askToGenerateAnswer(query: String, data: List<BaselineTextRAG.Article>): String {
        val prompt = """
Ты выполняешь роль ассистента и отвечаешь на вопросы пользователя, помогаешь ему.

Список статей из документации поможет тебе ответить на вопрос. Ты так же можешь использовать имеющиеся у тебя знания.

Список статей:
${data.map { """
$DELIMITER
${it.name}
     
${it.content}
""".trimIndent() }.joinToString("\n\n") }

$DELIMITER
Вопрос пользователя:
${query}
        """.trimIndent()
        return prompt
    }

    fun generateQuestionsByArticle(content: String, count: Int, mode: Int = 1): String {
        return when (mode) {
            1 -> """
Ты выступаешь в роли технического писателя. Мы работаем над написанием и улучшением документации по продукту Caila.
Я буду показывать тебе фрагменты документации и задавать вопросы. Отвечай на них предельно чётко и без дополнительных комментариев и объяснений.
Ответы пиши на русском языке.

Задание: Основываясь на статье, которую я приведу ниже, придумай пожалуйста вопросы, ответы на которые можно найти в этой статье.

${content}

Задание: Основываясь на статье, которую я тебе показал, придумай пожалуйста $count вопросов, ответы на которые можно найти в этой статье.
Выпиши только вопросы с их номерами.
            """
            2 -> """
    Ты — чат-бот с задачей создавать вопросы на основании статьи документации.

    Вопрос должен быть понятен без знания названия статьи по которой он создан.

    Твой ответ должен содержать $count вопросов. Отвечай на русском языке!
"""
            4 -> """
    Ты — чат-бот ассистент преподавателя. Твоя задача помогать преподавателю готовить обучающий курс.

    Сейчас нам надо сгенерировать вопросы для проверки знаний студентов. Я буду присылать тебе статьи, а ты генерируй по ним вопросы. Вопросов должно быть $count.

    Твой ответ должен содержать только созданные вопросы, пронумерованные цифрами. Отвечай на русском языке!
"""
            else -> throw RuntimeException()
        }.trimIndent()
    }

    fun expandQuery(query: String, count: Int): String {
        val prompt = """
Представь, что ты человек, который ищет в интернете ответ на свой вопрос. Напиши, какие запросы ты будешь вводить в гугл?

Вопрос пользователя: ${query}

Напиши ${count} вариантов.
        """.trimIndent()
        return prompt
    }

    fun expandQuery2(query: String): String {
        val prompt = """
Ты человек, который ищет в интернете ответ на свой вопрос. Напиши, какой запрос ты будешь отправлять вводить в гугл?

Вопрос пользователя: $query

Напиши 2 варианта, без комментариев.
        """.trimIndent()
        return prompt
    }

    fun rerankBySummary(query: String, articles: List<NeuroSummarySearch.ShortInfo>): String {
        val prompt = """
Ты ассистент и помогаешь пользователю найти информацию.
Пользователь обратился к поисковой системе и нашёл несколько статей. Упорядочи статьи по релевантности, в порядке, в котором пользователю стоит их просматривать.

Список статей:
${articles.map { """
$DELIMITER
Идентификатор: ${it.shortName}
Описание статьи: ${it.summary}

""".trimIndent() }.joinToString("\n\n") }

Исходный запрос пользователя: $query

Напиши пронумерованный список идентификаторов статей.
        """.trimIndent()
        return prompt
    }

    companion object {
        val DELIMITER = "============================================="
    }
}