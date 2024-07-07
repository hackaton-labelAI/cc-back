//package cc.services
//
//import cc.utils.JSON
//import com.mlp.api.client.model.PredictRequestData
//import com.mlp.api.datatypes.chatgpt.*
//import com.mlp.sdk.datatypes.taskzoo.EmbeddedTextsCollection
//import com.mlp.sdk.datatypes.taskzoo.Texts
//import com.mlp.sdk.datatypes.taskzoo.TextsCollection
//import devcsrj.okhttp3.logging.HttpLoggingInterceptor
//import jdk.incubator.vector.Vector
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.slf4j.LoggerFactory
//import org.springframework.stereotype.Service
//import java.io.IOException
//
//@Service
//class CailaConnector {
//    private val client = OkHttpClient.Builder()
////        .addInterceptor(HttpLoggingInterceptor(log))
//        .build()
//    private val CAILA_TOKEN = "1000005888.90713.irLIhMwk4ImPQobOZcbipfXyZuyPPz8c559j319X"
//
//    fun embeddings(texts: List<String>): List<List<Float>> {
//        val model = "vectorizer-openai-ada2-proxy"
////        val model = "vectorizer-caila-roberta"
////        val model = "semantic-vector-search-vectorizer"
//
//        val request = Request.Builder()
//            .url("https://caila.io/api/mlpgate/account/just-ai/model/$model/predict")
//            .header("MLP-API-KEY", CAILA_TOKEN)
//            .post(JSON.stringify(TextsCollection(texts)).toRequestBody("application/json".toMediaType()))
//            .build()
//
//        val response = client.newCall(request).execute()
//        if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//        val res = JSON.parse(response.body!!.string(), EmbeddedTextsCollection::class.java)
//
//        return res.embedded_texts.map { it.vector }
//    }
//
//    fun simplePrompt(prompt: String): String {
//        return chatCompletion(listOf(ChatMessage(ChatRole.user, prompt)))!!.content
//    }
//
//    fun chatCompletion(messages: List<ChatMessage>): ChatMessage? {
////        val model = "openai-proxy"
//        val model = "vllm-llama3-70B-awq"
//
//        val cr = ChatCompletionRequest(
////            model = "gpt-4o",
//            messages = messages
//        )
//        val pc = ChatCompletionConfig()
//        val pr = PredictRequestData().data(cr).config(pc)
//
//        val request = Request.Builder()
//            .url("https://caila.io/api/mlpgate/account/just-ai/model/$model/predict-with-config")
//            .header("MLP-API-KEY", CAILA_TOKEN)
//            .post(JSON.stringify(pr).toRequestBody("application/json".toMediaType()))
//            .build()
//
//        val response = client.newCall(request).execute()
//        try {
//            if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//            val res = JSON.parse(response.body!!.string(), ChatCompletionResult::class.java)
//
//            return res.choices[0].message
//        } finally {
//            response.close()
//        }
//    }
//
//    companion object {
//        val log = LoggerFactory.getLogger(CailaConnector::class.java)
//    }
//}
