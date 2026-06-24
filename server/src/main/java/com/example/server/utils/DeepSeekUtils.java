package com.example.server.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class DeepSeekUtils {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public String analyzeContent(String content) {
        String systemPrompt = """
                You are a senior short-video analyst.
                Clean noisy ASR transcripts, identify the core idea, and output concise markdown.

                Output format:
                ## Core Summary
                2-4 sentences.

                ## Key Insights
                ### 1. Insight title
                Explain why it matters.
                ### 2. Insight title
                Explain why it matters.
                ### 3. Insight title
                Explain why it matters.

                ## Reusable Moments
                - Quote or paraphrase 2-3 strong beats from the transcript.

                ## Tags
                #tag1 #tag2 #tag3

                Write the output in Simplified Chinese.
                """;
        return chat(systemPrompt, content == null ? "" : content);
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI request failed: missing api key";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return "AI request failed: missing base url";
        }

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "deepseek-ai/DeepSeek-R1-Distill-Qwen-32B");
        jsonBody.put("stream", false);

        JSONArray messages = new JSONArray();
        messages.add(JSONObject.of("role", "system", "content", normalize(systemPrompt)));
        messages.add(JSONObject.of("role", "user", "content", normalize(userPrompt)));
        jsonBody.put("messages", messages);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() == null ? "" : response.body().string();
                return "AI request failed: " + response.code() + " - " + errorBody;
            }

            String resultJson = response.body() == null ? "" : response.body().string();
            JSONObject jsonObject = JSON.parseObject(resultJson);
            if (jsonObject == null || jsonObject.getJSONArray("choices") == null || jsonObject.getJSONArray("choices").isEmpty()) {
                return "AI request failed: empty response";
            }

            String result = jsonObject.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            if (result == null || result.isBlank()) {
                return "AI request failed: empty content";
            }
            return result.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "Network error: " + e.getMessage();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
