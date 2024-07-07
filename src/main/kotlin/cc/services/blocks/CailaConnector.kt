package cc.services.blocks

import cc.endpoints.RAGConfig
import cc.services.rag.send
import com.mlp.api.TypeInfo
import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.gate.ClientResponseProto
import com.mlp.sdk.MlpServiceBase
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.taskzoo.EmbeddedTextsCollection
import com.mlp.sdk.datatypes.taskzoo.TextsCollection
import com.mlp.sdk.utils.JSON
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*

data class ResponseOrError<T>(
    val response: T?,
    val error: String?
)
data class CompletionResponseData(
    val promptTokens: Int,
    val output: String,
    val completionTokens: Int,
    val firstTokenTime: Int,
    val lastTokenTime: Int,
)


@Service
@ConditionalOnProperty("caila.grpcUrl")
class CailaConnector(val caila: CailaClient) {

    fun embeddings(texts: List<String>): List<List<Float>> {
        val model = caila.config.embedderModel!!.parseModelId()
        val r = caila.grpcClient.predictBlocking(model.account, model.service, JSON.stringify(TextsCollection(texts)))
        return JSON.parse(r, EmbeddedTextsCollection::class.java).embedded_texts.map { it.vector }
    }

    fun chatCompletion(prompt: String): CompletionResponseData {
        val requestId = UUID.randomUUID().toString()

        val req = buildRequest(prompt, null)

        val start = System.currentTimeMillis()

        val res = runBlocking {
            val stream = caila.grpcClient.predictStream(
                caila.defaultModel.model().account,
                caila.defaultModel.model().service,
                buildPayload(req),
                caila.defaultModel.predictConfig?.let { Payload(it.toString()) },
                authToken = caila.grpcClient.config.clientToken!!
            )

            collectResponse(stream, requestId, start)
        }

        if (res.response == null) {
            throw RuntimeException(res.error)
        }

        return res.response
    }

    suspend fun chatCompletion(prompt: String, debug: Boolean, gen: MlpServiceBase.ResultGenerator<ChatCompletionResult>, config: RAGConfig?) {
        val modelId = config?.headModel?.parseModelId()
        val req = buildRequest(prompt, modelId)

        val flow = caila.grpcClient.predictStream(
            modelId?.account ?: caila.defaultModel.model().account,
            modelId?.service ?: caila.defaultModel.model().service,
            buildPayload(req),
            caila.defaultModel.predictConfig?.let { Payload(it.toString()) },
            authToken = caila.grpcClient.config.clientToken!!
        )
        flow.collect {
            if (!it.hasPartialPredict()) {
                if (debug) {
                    gen.send("Unknown response: ${it}")
                }
            } else {
                gen.next(
                    JSON.parse(it.partialPredict.data.json, ChatCompletionResult::class.java),
                    it.partialPredict.finish
                )
            }
        }
    }

    private fun buildRequest(prompt: String, modelId: ModelId?): ChatCompletionRequest {
        return ChatCompletionRequest(
                model = if (modelId != null) modelId.model else caila.defaultModel.model().model,
                messages = listOf(ChatMessage(ChatRole.user, prompt)),
                stream = true
            )
    }

    private fun buildPayload(req: ChatCompletionRequest): Payload {
        val json = JSON.stringify(req)
        return Payload(TypeInfo.canonicalName(ChatCompletionRequest::class.java), json)
    }
    private suspend fun collectResponse(stream: Flow<ClientResponseProto>, requestId: String, startTime: Long): ResponseOrError<CompletionResponseData> {
        var first: Long? = null
        var last: Long? = null
        val output = StringBuilder()
        var promptTokens: Int = 0
        var completionTokens: Int = 0
        var error: String? = null
        stream.collect {
            last = System.currentTimeMillis()
            if (first == null) {
                first = last
            }

            if (it.hasError()) {
                error = it.error.toString()
            } else {
                val data =
                    if (it.hasPartialPredict()) it.partialPredict.data.json
                    else if (it.hasPredict()) it.predict.data.json
                    else null
                if (!data.isNullOrEmpty()) {
                    val result = JSON.parse(data, ChatCompletionResult::class.java)
                    if (result.choices.isNotEmpty()) {
                        val text = result.choices[0].delta?.content ?: result.choices[0].message?.content
                        if (!text.isNullOrEmpty()) {
                            output.append(text)
                        }
                    }
                    promptTokens += result.usage?.promptTokens ?: 0
                    completionTokens += result.usage?.completionTokens ?: 0
                }
            }
        }
        if (last == null) last = System.currentTimeMillis()
        return if (error != null) {
            ResponseOrError(null, error)
        } else {
            ResponseOrError(CompletionResponseData(
                promptTokens, output.toString(), completionTokens, ((first ?: last!!) - startTime).toInt(), (last!! - startTime).toInt()
            ), null)
        }
    }

}