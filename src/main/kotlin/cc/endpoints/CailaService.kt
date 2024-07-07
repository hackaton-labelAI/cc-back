package cc.endpoints

import cc.services.blocks.CailaConfig
import cc.services.rag.BaselineTextRAG
import cc.services.rag.BaselineVectorRAG
import cc.services.rag.CombinedRAG
import com.mlp.api.datatypes.chatgpt.*
import com.mlp.sdk.MlpPredictWithConfigServiceBase
import com.mlp.sdk.MlpServiceConfig
import com.mlp.sdk.MlpServiceSDK
import com.mlp.sdk.createGenerator
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

enum class RAGMode {
    text, vector, combined
}
data class RAGConfig(
    val stream: Boolean? = null,
    val debug: Boolean? = null,

    val mode: RAGMode? = null,
    val headModel: String? = null,
    val temperature: Double? = null
)

@Service
@ConditionalOnProperty("caila.serviceToken")
class CailaService(
    val cailaConfig: CailaConfig,
    val baselineText: BaselineTextRAG,
    val baselineVector: BaselineVectorRAG,
    val combinedRAG: CombinedRAG
    ):
    MlpPredictWithConfigServiceBase<ChatCompletionRequest, RAGConfig, ChatCompletionResult>(
        request, config, response
    ) {

    override suspend fun predict(request: ChatCompletionRequest, config: RAGConfig?): ChatCompletionResult? {
        val query = request.messages.last().content

        if (request.stream == true) {
            val generator = createGenerator<ChatCompletionResult>(sdk)
            when (config?.mode ?: RAGMode.text) {
                RAGMode.text -> baselineText.process(query, config, generator)
                RAGMode.vector -> baselineVector.process(query, config, generator)
                RAGMode.combined -> combinedRAG.process(query, config, generator)
            }

            return null
        } else {
            return ChatCompletionResult(choices = listOf(ChatCompletionChoice(0,
                message = ChatMessage(ChatRole.assistant, "Only streaming mode is supported for now")
            )))
        }
    }

    companion object {
        val request = ChatCompletionRequest(
            messages = listOf(ChatMessage(ChatRole.user, "что умеет Caila?"))
        )
        val config = RAGConfig(debug = true)
        val response = ChatCompletionResult(choices = listOf(ChatCompletionChoice(0,
            message = ChatMessage(ChatRole.assistant, "Всё")
        )))

        val debugStream = ThreadLocal<ResultGenerator<ChatCompletionResult>>()
    }

    lateinit var mlp: MlpServiceSDK
    @PostConstruct
    fun init() {
        mlp = MlpServiceSDK(this,
            initConfig = MlpServiceConfig(
                initialGateUrls = listOf(cailaConfig.grpcUrl),
                connectionToken = cailaConfig.serviceToken!!
            ))
        mlp.start()
    }

    @PreDestroy
    fun fini() {
        mlp.gracefulShutdown()
    }

}