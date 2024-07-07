package cc.services.analyzer

import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute
import java.io.IOException

class MyStemTokenizer(private val myStemService: MyStemService) : Tokenizer() {
    private var done = false
    private val termAtt: CharTermAttribute = addAttribute(CharTermAttribute::class.java)
    private val offsetAtt: OffsetAttribute = addAttribute(OffsetAttribute::class.java)

    private var text: String = ""
    private var words: List<MyStemInfo> = emptyList()

    private var wordPointer = 0
    private var textPointer = 0

    init {
        termAtt.resizeBuffer(DEFAULT_BUFFER_SIZE)
    }

    @Throws(IOException::class)
    override fun incrementToken(): Boolean {
        clearAttributes()
        if (!done) {
            // read all input
            text = input.readText()
            words = myStemService.getFullInfo(text)
            words = words.filter { it.getLemm().isNotEmpty() }

            wordPointer = 0
            textPointer = 0

            done = true
        } else {
            textPointer += words[wordPointer].text.length
            wordPointer++
        }

        if (wordPointer == words.size) {
            return false
        }

        val wi: MyStemInfo = words[wordPointer]
        textPointer = text.indexOf(wi.text, textPointer)

        val termChars = wi.getLemm().toCharArray()
        termAtt.copyBuffer(termChars, 0, termChars.size)
        termAtt.setLength(termChars.size)

        return true
    }

    override fun end() {
        super.end()
        // set final offset
        offsetAtt.setOffset(0, 0)
    }

    override fun reset() {
        super.reset()
        this.done = false
    }

    companion object {
        /** Default read buffer size  */
        const val DEFAULT_BUFFER_SIZE: Int = 1024
    }
}
