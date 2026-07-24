package com.deckflow.api;

import com.deckflow.domain.ConversationSession;
import com.deckflow.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApiDtos.SessionSummary> list() {
        return service.list();
    }

    @PostMapping
    public ConversationSession create(@Valid @RequestBody ApiDtos.CreateSessionRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public ConversationSession get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/messages")
    public ApiDtos.ChatResponse chat(
            @PathVariable String id,
            @Valid @RequestBody ApiDtos.ChatRequest request) {
        return service.chat(id, request.message());
    }

    @PostMapping("/{id}/generate")
    public ApiDtos.GenerateResponse generate(@PathVariable String id) {
        return service.generate(id);
    }

    @GetMapping("/{id}/versions/{version}/download")
    public ResponseEntity<Resource> download(@PathVariable String id, @PathVariable int version) {
        Resource resource = service.download(id, version);
        String encodedName = java.net.URLEncoder.encode(service.fileName(id, version), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

