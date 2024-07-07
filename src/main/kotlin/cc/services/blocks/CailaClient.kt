package cc.services.blocks

import com.fasterxml.jackson.databind.node.ObjectNode
import com.mlp.sdk.MlpClientConfig
import com.mlp.sdk.MlpClientSDK
import com.mlp.sdk.MlpRestClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

@ConfigurationProperties("caila")
@ConditionalOnProperty("caila.grpcUrl")
data class CailaConfig(
    val grpcUrl: String,
    val restUrl: String,
    val clientToken: String,
    val defaultModel: ModelConfig,
    val embedderModel: String?,
    val serviceToken: String?,
)

data class ModelId(
    val account: String, val service: String, val model: String?
)

data class ModelConfig(
    val model: String,
    val systemPrompt: String? = null,
    val predictConfig: ObjectNode? = null
) {
    fun model(): ModelId {
        val p = model.split("/")
        return ModelId(p[0], p[1], p.getOrNull(2))
    }
}

fun String.parseModelId(): ModelId {
    val p = this.split("/")
    return ModelId(p[0], p[1], p.getOrNull(2))
}


@Service
@ConditionalOnProperty("caila.grpcUrl")
class CailaClient(
    val config: CailaConfig
) {
    val grpcClient = MlpClientSDK(
        MlpClientConfig(
            initialGateUrls = listOf(config.grpcUrl),
            restUrl = config.restUrl,
            clientToken = config.clientToken
        )
    )
    val restClient = MlpRestClient(config.restUrl, config.clientToken)

    val defaultModel = config.defaultModel
}