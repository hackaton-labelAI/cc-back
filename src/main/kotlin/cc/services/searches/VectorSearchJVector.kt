package cc.services.searches

import cc.services.blocks.CailaConnector
import cc.utils.Loader
import cc.services.scripts.QA
import cc.utils.consoleRunner
import com.mlp.sdk.utils.JSON
import io.github.jbellis.jvector.disk.CachingGraphIndex
import io.github.jbellis.jvector.disk.OnDiskGraphIndex
import io.github.jbellis.jvector.disk.SimpleMappedReaderSupplier
import io.github.jbellis.jvector.graph.*
import io.github.jbellis.jvector.util.Bits
import io.github.jbellis.jvector.vector.VectorEncoding
import io.github.jbellis.jvector.vector.VectorSimilarityFunction
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path

data class IndexPiece(
    val article: String,
    val content: String,
    val embedding: List<Float>,
    val range: IntRange? = null
)

@Service
@ConditionalOnProperty("jvector")
class VectorSearchJVector(
    val cailaConnector: CailaConnector
): Searcher {
    val pref = "/tmp/index3"
//    val pref = "/tmp/index4"

    override fun buildIndex(folder: String) {
        val files =
            Loader.listFilesRecursive(folder)
                .filter { it.endsWith(".md") || it.endsWith(".mdx") || it.endsWith(".txt") }

        val data = files.map {
            val file = File(folder + it)
            val markdown = file.readText()
            val e = cailaConnector.embeddings(listOf(markdown))[0]

            IndexPiece(it, markdown, e, null)
        }

        createIndex(data)
    }

    var index: GraphIndex<FloatArray>? = null
    private fun createIndex(data: List<IndexPiece>) {
        val randomAccessVectorValues = ListRandomAccessVectorValues(data.map { it.embedding.toFloatArray() }, data[0].embedding.size)

        val builder = GraphIndexBuilder(
            randomAccessVectorValues,
            VectorEncoding.FLOAT32,
            VectorSimilarityFunction.COSINE,
            16,
            100,
            1.5f,
            1.4f
        )

        // build the index (in memory)
        index = builder.build()

        saveIndex(index!!, randomAccessVectorValues, data)
        builder.cleanup()
    }

    data class TextAndVector(val text: String, val vector: List<Float>)
    private fun saveIndex(index: GraphIndex<FloatArray>, vectors: RandomAccessVectorValues<FloatArray>, data: List<IndexPiece>) {
        val file = File("$pref-1")
        DataOutputStream(FileOutputStream(file)).use { outputStream ->
            OnDiskGraphIndex.write(index, vectors, outputStream)
            outputStream.close()
        }

        val file2 = File("$pref-2")
        file2.writeText(JSON.stringify(data))
    }


    override fun search(query: String): List<SearchResult> {
        if (index == null) {
            index = CachingGraphIndex(OnDiskGraphIndex(SimpleMappedReaderSupplier(Path.of("$pref-1")), 0))
        }

        val file2 = File("$pref-2")
        val indexData = JSON.parseList<IndexPiece>(file2.readText())
        val rav = ListRandomAccessVectorValues(indexData.map { it.embedding.toFloatArray() }, indexData[0].embedding.size)

        val qv =  getEmbedding(query)
        val sr = GraphSearcher.search(
            qv,
            10,  // number of results
            rav,  // vectors we're searching, used for scoring
            VectorEncoding.FLOAT32,
            VectorSimilarityFunction.EUCLIDEAN,  // how to score
            index,
            Bits.ALL
        )

//        println(query)
//        for (ns in sr.getNodes()) {
//            println("   ${indexData[ns.node].article} - ${ns.score}")
//        }

        return sr.nodes.map { SearchResult(query, Loader.removeNotionSalt(indexData[it.node].article), it.score ) }
    }

    private fun getEmbedding(text: String): FloatArray {
        return cailaConnector.embeddings(listOf(text))[0].toFloatArray()
    }

}


fun main() {
    val context = consoleRunner("jvector-search.yml")

    val idx = context.getBean(VectorSearchJVector::class.java)
    idx.buildIndex("../cc-data/rustore/short_chunks")

    val qa = JSON.parseList<QA>(File("../cc-data/rustore/generated_questions.json").readText())
    searchEval(idx, qa)

}