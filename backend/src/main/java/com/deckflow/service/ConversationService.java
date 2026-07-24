package com.deckflow.service;

import com.deckflow.api.ApiDtos;
import com.deckflow.domain.ChatMessage;
import com.deckflow.domain.ConversationSession;
import com.deckflow.domain.DeckSpec;
import com.deckflow.domain.PptVersion;
import com.deckflow.repository.ConversationRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {
    private final ConversationRepository repository;
    private final AiService aiService;
    private final PptService pptService;

    public ConversationService(ConversationRepository repository, AiService aiService, PptService pptService) {
        this.repository = repository;
        this.aiService = aiService;
        this.pptService = pptService;
    }

    public ConversationSession create(ApiDtos.CreateSessionRequest request) {
        ConversationSession session = new ConversationSession();
        session.setId(UUID.randomUUID().toString());
        session.setTitle(shortTitle(request.requirement()));
        session.setPageCount(request.pageCount());
        session.setInitialRequirement(request.requirement());
        session.setStatus("CLARIFYING");
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        session.getMessages().add(ChatMessage.user(request.requirement()));

        String reply = aiService.converse(session, request.requirement());
        session.getMessages().add(ChatMessage.assistant(reply));
        return repository.save(session);
    }

    public ApiDtos.ChatResponse chat(String id, String message) {
        ConversationSession session = get(id);
        String reply = aiService.converse(session, message);
        session.getMessages().add(ChatMessage.user(message));
        session.getMessages().add(ChatMessage.assistant(reply));
        session.setStatus(session.getDeckSpec() == null ? "CLARIFYING" : "REVISING");
        session.setUpdatedAt(Instant.now());
        repository.save(session);
        return new ApiDtos.ChatResponse(reply, true);
    }

    public ApiDtos.GenerateResponse generate(String id) {
        ConversationSession session = get(id);
        session.setStatus("GENERATING");
        session.setUpdatedAt(Instant.now());
        repository.save(session);
        try {
            DeckSpec spec = aiService.createOrReviseDeck(session);
            int versionNumber = session.getVersions().size() + 1;
            Path file = pptService.generate(session.getId(), versionNumber, spec);
            PptVersion version = new PptVersion(versionNumber, file.getFileName().toString(), file.toString(), Instant.now());
            session.setDeckSpec(spec);
            session.getVersions().add(version);
            session.setTitle(spec.getTitle().isBlank() ? session.getTitle() : spec.getTitle());
            session.setStatus("GENERATED");
            session.setUpdatedAt(Instant.now());
            session.getMessages().add(ChatMessage.assistant(
                    "已生成第 " + versionNumber + " 版 PPT。你可以下载查看，也可以继续告诉我需要修改的地方。"));
            repository.save(session);
            return new ApiDtos.GenerateResponse(
                    id, versionNumber, version.fileName(),
                    "/api/sessions/" + id + "/versions/" + versionNumber + "/download");
        } catch (RuntimeException e) {
            session.setStatus(session.getDeckSpec() == null ? "CLARIFYING" : "REVISING");
            session.setUpdatedAt(Instant.now());
            repository.save(session);
            throw e;
        }
    }

    public ConversationSession get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException("会话不存在：" + id));
    }

    public List<ApiDtos.SessionSummary> list() {
        return repository.findAll().stream()
                .map(s -> new ApiDtos.SessionSummary(
                        s.getId(), s.getTitle(), s.getPageCount(), s.getStatus(),
                        s.getUpdatedAt(), s.getVersions().size()))
                .toList();
    }

    public Resource download(String id, int versionNumber) {
        ConversationSession session = get(id);
        PptVersion version = session.getVersions().stream()
                .filter(item -> item.version() == versionNumber)
                .findFirst()
                .orElseThrow(() -> new SessionNotFoundException("PPT 版本不存在"));
        try {
            Resource resource = new UrlResource(Path.of(version.filePath()).toUri());
            if (!resource.exists()) throw new SessionNotFoundException("PPT 文件不存在");
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("PPT 文件路径无效", e);
        }
    }

    public String fileName(String id, int versionNumber) {
        return get(id).getVersions().stream()
                .filter(item -> item.version() == versionNumber)
                .map(PptVersion::fileName)
                .findFirst()
                .orElseThrow(() -> new SessionNotFoundException("PPT 版本不存在"));
    }

    public void delete(String id) {
        get(id);
        repository.delete(id);
    }

    private String shortTitle(String requirement) {
        String clean = requirement.replaceAll("\\s+", " ").strip();
        return clean.substring(0, Math.min(clean.length(), 26));
    }

    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String message) { super(message); }
    }
}

