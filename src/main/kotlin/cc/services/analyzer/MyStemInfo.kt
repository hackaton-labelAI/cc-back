package cc.services.analyzer

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashSet

data class MyStemInfo(
    val text: String,
    val analysis: List<Analysis>? = null
) {

    companion object {
        private val gramSplitter: Pattern = Pattern.compile(",(?=([^(]*\\([^)]*\\))*[^)]*$)")
    }

    data class Analysis(
        val lex: String?,
        val qual: String?,
        val gr: String?
    ) {

        @JsonIgnore
        fun getPos(): Set<String> {
            val parts = gramSplitter.split(gr)
            return HashSet(parts.asList())
        }
    }

    @JsonIgnore
    fun getLemm(): String {
        return analysis?.getOrNull(0)?.lex ?: text.lowercase(Locale.getDefault())
    }

    @JsonIgnore
    fun getPOS(): String {
        return analysis?.getOrNull(0)?.gr?.split("[=,()\\|]".toRegex())?.get(0) ?: ""
    }

    @JsonIgnore
    fun getGram(): Set<String> {
        return analysis?.getOrNull(0)?.getPos() ?: emptySet()
    }
}