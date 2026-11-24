package org.delcom.ifs18005springboottodolist.adapter.ai;

import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.delcom.ifs18005springboottodolist.domain.repository.ITodoAiService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementasi ITodoAiService yang memanggil endpoint OpenAI-compatible HTTP API.
 */
public class OpenAiTodoService implements ITodoAiService {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiTodoService(String endpoint, String apiKey, String model) {
        this(endpoint, apiKey, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), new ObjectMapper());
    }

    public OpenAiTodoService(String endpoint, String apiKey, String model, HttpClient httpClient, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiGeneratedTodo> generateTodos(String prompt) throws DomainException {
        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException("Prompt instruksi tidak boleh kosong");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new ValidationException("Konfigurasi OPENAI_ENDPOINT belum diatur");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ValidationException("Konfigurasi OPENAI_KEY belum diatur");
        }

        String targetUrl = normalizeEndpoint(endpoint.trim());
        String requestJson = buildRequestBody(prompt.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Pemanggilan API AI terinterupsi: " + e.getMessage());
        } catch (IOException e) {
            throw new DomainException("Gagal menghubungi server AI: " + e.getMessage());
        }

        if (response.statusCode() != 200) {
            throw new DomainException("Layanan AI mengembalikan error (" + response.statusCode() + "): " + response.body());
        }

        return parseAiResponse(response.body());
    }

    private String normalizeEndpoint(String ep) {
        String base = ep.endsWith("/") ? ep.substring(0, ep.length() - 1) : ep;
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private String buildRequestBody(String prompt) throws DomainException {
        try {
            String systemInstruction = "Anda adalah asisten produktivitas cerdas. "
                    + "Tugas Anda adalah menghasilkan daftar todo berdasarkan instruksi pengguna. "
                    + "Kembalikan HANYA JSON array valid tanpa markdown formatting atau teks tambahan. "
                    + "Setiap elemen array wajib memiliki objek dengan key 'title' (string singkat) dan 'description' (string penjelasan tugas). "
                    + "Contoh format: [{\"title\":\"Membaca materi\",\"description\":\"Pelajari bab 1 dan 2\"}]";

            String selectedModel = (model == null || model.isBlank()) ? "gpt-4o-mini" : model.trim();
            Map<String, Object> payload = Map.of(
                    "model", selectedModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new DomainException("Gagal menyusun request AI: " + e.getMessage());
        }
    }

    private List<AiGeneratedTodo> parseAiResponse(String responseBody) throws DomainException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new DomainException("Respon AI tidak memiliki pilihan (choices)");
            }

            JsonNode message = choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new DomainException("Konten respon AI kosong");
            }

            String content = message.get("content").asText().trim();
            content = stripMarkdownCodeFence(content);

            JsonNode arrayNode = objectMapper.readTree(content);
            if (!arrayNode.isArray()) {
                throw new DomainException("Format konten respon AI bukan merupakan JSON array");
            }

            List<AiGeneratedTodo> result = new ArrayList<>();
            for (JsonNode item : arrayNode) {
                JsonNode titleNode = item.get("title");
                if (titleNode == null || titleNode.asText().isBlank()) {
                    continue;
                }
                String title = titleNode.asText().trim();
                JsonNode descNode = item.get("description");
                String desc = (descNode == null) ? "" : descNode.asText().trim();
                result.add(new AiGeneratedTodo(title, desc));
            }

            if (result.isEmpty()) {
                throw new DomainException("AI tidak menghasilkan daftar todo yang valid");
            }

            return result;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Gagal memproses respon dari AI: " + e.getMessage());
        }
    }

    private String stripMarkdownCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = (firstNewline != -1) ? trimmed.substring(firstNewline + 1) : trimmed.substring(3);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
