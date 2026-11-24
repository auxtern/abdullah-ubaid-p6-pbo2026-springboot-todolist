package org.delcom.ifs18005springboottodolist.adapter.ai;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiTodoServiceTest {

    private HttpServer server;
    private String serverUrl;
    private String mockResponseBody;
    private int mockResponseCode;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] bytes = mockResponseBody != null ? mockResponseBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(mockResponseCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        serverUrl = "http://localhost:" + server.getAddress().getPort();
        mockResponseCode = 200;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generatesTodosSuccessfully() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "[{\\"title\\": \\"Belajar Testing\\", \\"description\\": \\"Membuat unit test Jacoco\\"}]"
                      }
                    }
                  ]
                }
                """;

        OpenAiTodoService service = new OpenAiTodoService(serverUrl, "valid-key", "test-model");
        List<AiGeneratedTodo> todos = service.generateTodos("Rencana belajar");
        assertEquals(1, todos.size());
        assertEquals("Belajar Testing", todos.get(0).title());
        assertEquals("Membuat unit test Jacoco", todos.get(0).description());
    }

    @Test
    void handlesMarkdownCodeFencesAndNullDescriptionAndFallbackModel() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n[{\\"title\\": \\"Tugas 1\\"}, {}, {\\"title\\": \\"   \\"}, {\\"title\\": \\"Tugas 2\\", \\"description\\": \\"Ada deskripsi\\"}]\\n```"
                      }
                    }
                  ]
                }
                """;

        OpenAiTodoService service = new OpenAiTodoService(serverUrl + "/", "valid-key", "   ");
        List<AiGeneratedTodo> todos = service.generateTodos("Rencana belajar");
        assertEquals(2, todos.size());
        assertEquals("Tugas 1", todos.get(0).title());
        assertEquals("", todos.get(0).description());
        assertEquals("Tugas 2", todos.get(1).title());
    }

    @Test
    void handlesCodeFenceWithoutNewlineAndWithoutClosingFence() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```[{\\"title\\": \\"Tugas inline\\"}]"
                      }
                    }
                  ]
                }
                """;

        OpenAiTodoService service = new OpenAiTodoService(serverUrl, "valid-key", "model");
        List<AiGeneratedTodo> todos = service.generateTodos("Rencana");
        assertEquals(1, todos.size());
        assertEquals("Tugas inline", todos.get(0).title());
    }

    @Test
    void handlesCodeFenceWithoutNewlineWithClosingFence() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```[{\\"title\\": \\"Tugas inline 2\\"}]```"
                      }
                    }
                  ]
                }
                """;

        OpenAiTodoService service = new OpenAiTodoService(serverUrl, "valid-key", "model");
        List<AiGeneratedTodo> todos = service.generateTodos("Rencana");
        assertEquals(1, todos.size());
        assertEquals("Tugas inline 2", todos.get(0).title());
    }

    @Test
    void handlesEndpointAlreadyEndingWithChatCompletions() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "[{\\"title\\": \\"Todo 1\\", \\"description\\": \\"Desc 1\\"}]"
                      }
                    }
                  ]
                }
                """;

        OpenAiTodoService service = new OpenAiTodoService(serverUrl + "/chat/completions", "valid-key", null);
        List<AiGeneratedTodo> todos = service.generateTodos("Rencana belajar");
        assertEquals(1, todos.size());
    }

    @Test
    void rejectsInvalidInputs() {
        OpenAiTodoService service = new OpenAiTodoService("http://localhost", "valid-key", "model");
        assertThrows(ValidationException.class, () -> service.generateTodos(null));
        assertThrows(ValidationException.class, () -> service.generateTodos("   "));

        OpenAiTodoService nullEndpoint = new OpenAiTodoService(null, "valid-key", "model");
        assertThrows(ValidationException.class, () -> nullEndpoint.generateTodos("Prompt"));

        OpenAiTodoService blankEndpoint = new OpenAiTodoService("   ", "valid-key", "model");
        assertThrows(ValidationException.class, () -> blankEndpoint.generateTodos("Prompt"));

        OpenAiTodoService nullKey = new OpenAiTodoService("http://localhost", null, "model");
        assertThrows(ValidationException.class, () -> nullKey.generateTodos("Prompt"));

        OpenAiTodoService blankKey = new OpenAiTodoService("http://localhost", "   ", "model");
        assertThrows(ValidationException.class, () -> blankKey.generateTodos("Prompt"));
    }

    @Test
    void throwsOnHttpError() {
        mockResponseCode = 500;
        mockResponseBody = "Internal Server Error";

        OpenAiTodoService service = new OpenAiTodoService(serverUrl, "valid-key", "test-model");
        DomainException ex = assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void throwsOnMissingChoicesOrEmptyContent() {
        OpenAiTodoService service = new OpenAiTodoService(serverUrl, "valid-key", "test-model");

        mockResponseBody = "{}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));

        mockResponseBody = "{\"choices\": []}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));

        mockResponseBody = "{\"choices\": [{}]}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));

        mockResponseBody = "{\"choices\": [{\"message\": {}}]}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));
    }

    @Test
    void throwsWhenContentIsNotJsonArrayOrEmptyResults() {
        OpenAiTodoService service = new OpenAiTodoService(serverUrl, "valid-key", "test-model");

        mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"{\\\"single\\\": 1}\"}}]}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));

        mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"[{\\\"title\\\": \\\"\\\"}]\"}}]}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));

        mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"invalid json syntax\"}}]}";
        assertThrows(DomainException.class, () -> service.generateTodos("Rencana"));
    }

    @Test
    void handlesHttpClientExceptions() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        OpenAiTodoService service = new OpenAiTodoService("http://localhost:1234", "key", "model", mockClient, new ObjectMapper());
        assertThrows(DomainException.class, () -> service.generateTodos("Prompt"));

        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("Interrupted"));
        assertThrows(DomainException.class, () -> service.generateTodos("Prompt"));
    }

    @Test
    void handlesSerializationError() {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failure"));

        OpenAiTodoService service = new OpenAiTodoService("http://localhost:1234", "key", "model", HttpClient.newHttpClient(), mockMapper);
        assertThrows(DomainException.class, () -> service.generateTodos("Prompt"));
    }
}
