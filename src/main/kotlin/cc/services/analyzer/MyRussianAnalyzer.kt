package cc.services.analyzer

import org.apache.lucene.analysis.*


class MyRussianAnalyzer(private val myStemService: MyStemService): Analyzer() {

    val RUSSIAN_STOP_WORDS = listOf(
        "и", "в", "во", "не", "что", "он", "на", "я", "с", "со",
        "как", "а", "то", "все", "она", "так", "его", "но", "да",
        "ты", "к", "у", "же", "вы", "за", "бы", "по", "только", "ее",
        "мне", "было", "вот", "от", "меня", "еще", "нет", "о", "из", "ему",
        "теперь", "когда", "даже", "ну", "вдруг", "ли", "если", "уже", "или",
        "ни", "быть", "был", "него", "до", "вас", "нибудь", "опять",
        "уж", "вам", "ведь", "там", "потом", "себя", "ничего",
        "ей", "может", "они", "тут", "где", "есть", "надо", "ней", "для",
        "мы", "тебя", "их", "чем", "была", "сам", "чтоб", "без", "будто",
        "чего", "раз", "тоже", "себе", "под", "будет", "ж", "тогда", "кто",
        "этот", "того", "потому", "этого", "какой", "совсем", "ним", "здесь",
        "этом", "один", "почти", "мой", "тем", "чтобы", "нее", "сейчас",
        "были", "куда", "зачем", "всех", "мог", "сама", "сами",
        "три", "эти", "моя", "всем", "самого", "мои", "своей", "этой",
        "перед", "чуть", "том", "такой", "им", "более", "всю", "между")

    override fun createComponents(fieldName: String): TokenStreamComponents {
        // create a filter
        val source: Tokenizer = MyStemTokenizer(myStemService)

        val stream = StopFilter(source, CharArraySet(CharArraySet(RUSSIAN_STOP_WORDS, true), true))

        return TokenStreamComponents(source, stream)
    }
}
