package cc.services.searches

import cc.services.blocks.CailaConnector
import cc.utils.consoleRunner
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.KnnFloatVectorField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.KnnFloatVectorQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.NIOFSDirectory
import org.apache.lucene.store.SingleInstanceLockFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

@Service
@ConditionalOnProperty("lucene.vector-search")
class VectorSearchLuceneDemo(
    val cailaConnector: CailaConnector
) {

    fun createIndex(texts: List<String>) {
        val analyzer = StandardAnalyzer()
        File("/tmp/index2").deleteRecursively()
        val index: Directory = NIOFSDirectory(Path.of("/tmp/index2"), SingleInstanceLockFactory())
        val config = IndexWriterConfig(analyzer)
        val writer = IndexWriter(index, config)

        texts.forEach {
            addDocument(writer, it, getEmbedding(it))
        }

        writer.close()
    }

    private fun getEmbedding(text: String): List<Float> {
        return cailaConnector.embeddings(listOf(text))[0]
    }

    private fun addDocument(writer: IndexWriter, text: String, embedding: List<Float>) {
        val doc = Document()
        doc.add(TextField("text", text, Field.Store.YES))
        doc.add(KnnFloatVectorField("embedding", embedding.toFloatArray()))
        writer.addDocument(doc)
    }

    fun search(text: String) {
        val index: Directory = NIOFSDirectory(Path.of("/tmp/index2"), SingleInstanceLockFactory())
        val reader: IndexReader = DirectoryReader.open(index)
        val searcher = IndexSearcher(reader)

        val embedding = getEmbedding(text)
        val query = KnnFloatVectorQuery("embedding", embedding.toFloatArray(), 10)
        val results = searcher.search(query, 10)

        println(text)
        for (scoreDoc in results.scoreDocs) {
            val t = searcher.doc(scoreDoc.doc).get("text")
            val s = scoreDoc.score
            println("  $t - $s")
        }
        reader.close()
    }

}


fun main() {
    // It works only with vectorizer-caila-roberta embeddings. Not with ada2
    val context = consoleRunner("jvector-search.yml")

    val idx = context.getBean(VectorSearchLuceneDemo::class.java)
    idx.createIndex(listOf("привет", "пока-пока"))

    idx.search("здрасте")
    idx.search("прощайте")
    idx.search("hi")
    idx.search("bye-bye")
}