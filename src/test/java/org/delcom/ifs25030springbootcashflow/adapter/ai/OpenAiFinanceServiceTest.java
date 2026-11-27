package org.delcom.ifs25030springbootcashflow.adapter.ai;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs25030springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25030springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiFinanceServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /** Membungkus konten model AI ke format respon chat completions. */
    private static String completion(String content) {
        return MAPPER.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", content)))
        ));
    }

    @Test
    void generatesTransactionsSuccessfully() throws Exception {
        mockResponseBody = completion("[{\"description\":\"Gaji bulanan\",\"amount\":5000000,\"type\":\"INCOME\"}]");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");
        List<AiGeneratedTransaction> result = service.generateTransactions("Gajian 5 juta");

        assertEquals(1, result.size());
        assertEquals("Gaji bulanan", result.get(0).description());
        assertEquals(5_000_000L, result.get(0).amount());
        assertEquals(TransactionType.INCOME, result.get(0).type());
    }

    @Test
    void skipsInvalidItemsAndAcceptsIndonesianTypesWithCodeFenceAndFallbackModel() throws Exception {
        String items = "["
                + "{},"
                + "{\"description\":\"  \",\"amount\":1,\"type\":\"INCOME\"},"
                + "{\"description\":\"Tanpa jumlah\",\"type\":\"INCOME\"},"
                + "{\"description\":\"Tanpa tipe\",\"amount\":5},"
                + "{\"description\":\"Nol\",\"amount\":0,\"type\":\"INCOME\"},"
                + "{\"description\":\"Tipe aneh\",\"amount\":10,\"type\":\"lainnya\"},"
                + "{\"description\":\" Gaji \",\"amount\":\"1500\",\"type\":\"pemasukan\"},"
                + "{\"description\":\"Makan\",\"amount\":25000,\"type\":\"Pengeluaran\"},"
                + "{\"description\":\"Parkir\",\"amount\":7,\"type\":\"expense\"}"
                + "]";
        mockResponseBody = completion("```json\n" + items + "\n```");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/", "valid-key", "   ");
        List<AiGeneratedTransaction> result = service.generateTransactions("Catatan");

        assertEquals(3, result.size());
        assertEquals("Gaji", result.get(0).description());
        assertEquals(1500L, result.get(0).amount());
        assertEquals(TransactionType.INCOME, result.get(0).type());
        assertEquals(TransactionType.EXPENSE, result.get(1).type());
        assertEquals("Parkir", result.get(2).description());
        assertEquals(TransactionType.EXPENSE, result.get(2).type());
    }

    @Test
    void handlesCodeFenceWithoutNewlineAndWithoutClosingFence() throws Exception {
        mockResponseBody = completion("```[{\"description\":\"Inline\",\"amount\":10,\"type\":\"INCOME\"}]");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "model");
        List<AiGeneratedTransaction> result = service.generateTransactions("Catatan");
        assertEquals(1, result.size());
        assertEquals("Inline", result.get(0).description());
    }

    @Test
    void handlesCodeFenceWithoutNewlineWithClosingFence() throws Exception {
        mockResponseBody = completion("```[{\"description\":\"Inline 2\",\"amount\":10,\"type\":\"EXPENSE\"}]```");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "model");
        List<AiGeneratedTransaction> result = service.generateTransactions("Catatan");
        assertEquals(1, result.size());
        assertEquals("Inline 2", result.get(0).description());
    }

    @Test
    void handlesEndpointAlreadyEndingWithChatCompletions() throws Exception {
        mockResponseBody = completion("[{\"description\":\"A\",\"amount\":1,\"type\":\"INCOME\"}]");

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/chat/completions", "valid-key", null);
        assertEquals(1, service.generateTransactions("Catatan").size());
    }

    @Test
    void rejectsInvalidInputs() {
        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost", "valid-key", "model");
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("   "));

        OpenAiFinanceService nullEndpoint = new OpenAiFinanceService(null, "valid-key", "model");
        assertThrows(ValidationException.class, () -> nullEndpoint.generateTransactions("Prompt"));

        OpenAiFinanceService blankEndpoint = new OpenAiFinanceService("   ", "valid-key", "model");
        assertThrows(ValidationException.class, () -> blankEndpoint.generateTransactions("Prompt"));

        OpenAiFinanceService nullKey = new OpenAiFinanceService("http://localhost", null, "model");
        assertThrows(ValidationException.class, () -> nullKey.generateTransactions("Prompt"));

        OpenAiFinanceService blankKey = new OpenAiFinanceService("http://localhost", "   ", "model");
        assertThrows(ValidationException.class, () -> blankKey.generateTransactions("Prompt"));
    }

    @Test
    void throwsOnHttpError() {
        mockResponseCode = 500;
        mockResponseBody = "Internal Server Error";

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");
        DomainException ex = assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void throwsOnMissingChoicesOrEmptyContent() {
        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");

        mockResponseBody = "{}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": []}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{\"message\": {}}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
    }

    @Test
    void throwsWhenContentIsNotJsonArrayOrEmptyResults() {
        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");

        mockResponseBody = completion("{\"single\": 1}");
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = completion("[{\"description\":\"\"}]");
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = completion("invalid json syntax");
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
    }

    @Test
    void handlesHttpClientExceptions() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1234", "key", "model", mockClient, new ObjectMapper());
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));

        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("Interrupted"));
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));
        Thread.interrupted();
    }

    @Test
    void handlesSerializationError() {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failure"));

        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1234", "key", "model", HttpClient.newHttpClient(), mockMapper);
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));
    }
}
