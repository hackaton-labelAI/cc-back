package cc.services.analyzer

import cc.utils.JSON
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Service
@ConditionalOnProperty("myStem.path")
class MyStemService(
    @Value("\${myStem.path}") private val myStemPath: String? = null,
    @Value("\${myStem.poolSize}") private val poolSize: Int = 1,
) {
    private val processes: Array<MyStemProcess?>
    private val counter = AtomicInteger()

    // TODO: Надо реализовать кеш. Значения храним в памяти, сохраняем на диск раз в 1сек, если есть изменения. Или в mongo.
    private val simpleCache: SimpleCache<String, List<MyStemInfo>>? = null

    init {
        processes = arrayOfNulls(poolSize)

        start()
    }

    fun start() {
        for (i in processes.indices) {
            processes[i] = MyStemProcess(myStemPath ?: TEST_PATH)
            try {
                processes[i]!!.start()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    fun getFullInfo(phrase: String): List<MyStemInfo> {
        return if (simpleCache != null) {
            simpleCache.get(phrase) { getFullInfo0(phrase) }
        } else {
            getFullInfo0(phrase)
        }
    }

    private fun getFullInfo0(phrase: String): List<MyStemInfo> {
        val i = counter.incrementAndGet() % processes.size
        try {
            val ret = processes[i]!!.process(phrase.trim { it <= ' ' })
//            println("$phrase -> ${JSON.stringify(ret)}")
            return ret
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun lemmatize(text: String): List<String> {
        return getFullInfo(text).map { it.getLemm() ?: it.text!! }
    }

    fun lemmatizeWithGram(text: String): List<String> {
        return getFullInfo(text).map {
            it.getLemm() + "_" + it.getGram()
        }
    }

    private class MyStemProcess(myStemPath: String?) {
        private val myStemPath: String = File(myStemPath).absolutePath
        private var process: Process? = null
        private var writer: BufferedWriter? = null
        private var reader: BufferedReader? = null

        @Throws(IOException::class)
        fun start() {
            // TODO: don't use disambiguation?
            val builder = ProcessBuilder(myStemPath, "-c", "-ig", "--eng-gr", "--format", "json")
            val process = builder.start()
            writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            reader = BufferedReader(InputStreamReader(process.inputStream))
            this.process = process
        }

        fun stop() {
            if (process != null) {
                try {
                    writer!!.close()
                    reader!!.close()
                } catch (_: IOException) {
                }
                process!!.destroy()
                process = null
            }
        }

        @Throws(IOException::class)
        fun process(input: String): List<MyStemInfo> {
            val result = processMyStem(input)
            log.trace("myStem result {} for input {}", result, input)

            val myStemInfos = JSON.parseList<MyStemInfo>(result)
            if (log.isTraceEnabled) {
                val mystemJoin: String = myStemInfos.map { it.text }.joinToString()
                if (!mystemJoin.trim { it <= ' ' }.equals(input.trim { it <= ' ' }, ignoreCase = true)) {
                    log.trace("mystembroken result '{}' for input '{}' ({})", mystemJoin, input, myStemInfos)
                }
            }
            return myStemInfos
        }

        @Synchronized
        @Throws(IOException::class)
        fun processMyStem(input: String): String {
            check()
            writer!!.write(input.replace("\n".toRegex(), " "))
            writer!!.newLine()
            writer!!.flush()
            val builder = StringBuilder()
            var line: String?
            while ((reader!!.readLine().also { line = it }) != null && reader!!.ready()) {
                builder.append(line)
                builder.append(System.lineSeparator())
            }
            builder.append(line)
            return builder.toString()
        }

        @Throws(IOException::class)
        private fun check() {
            if (!process!!.isAlive) {
                stop()
                start()
            }
        }

        companion object {
            private val log = LoggerFactory.getLogger(MyStemService::class.java)
            private val mapper = ObjectMapper()

            init {
                mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                mapper.enable(JsonParser.Feature.ALLOW_COMMENTS)
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
    }

    companion object {
        val TEST_PATH: String? = findMyStem()

        private fun findMyStem(): String {
            val path = listOf("/opt/mystem", "/usr/local/bin/mystem", "./mystem")
            return path.find {
                File(it).exists()
            } ?: "not found"
        }
    }
}

fun main(args: Array<String>) {
    val service =
        MyStemService(
            "/usr/local/bin/mystem",
            1,
        )

    var result = service.lemmatize("кузявые бутявки")
    System.out.println(result.joinToString())
    result = service.lemmatizeWithGram("кузявые бутявки")
    System.out.println(result.joinToString())
}
