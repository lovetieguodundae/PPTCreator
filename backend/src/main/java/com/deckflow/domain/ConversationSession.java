package com.deckflow.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ConversationSession {
    private String id;
    private String title;
    private int pageCount;
    private String initialRequirement;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ChatMessage> messages = new ArrayList<>();
    private DeckSpec deckSpec;
    private List<PptVersion> versions = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public String getInitialRequirement() { return initialRequirement; }
    public void setInitialRequirement(String initialRequirement) { this.initialRequirement = initialRequirement; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages == null ? new ArrayList<>() : messages; }
    public DeckSpec getDeckSpec() { return deckSpec; }
    public void setDeckSpec(DeckSpec deckSpec) { this.deckSpec = deckSpec; }
    public List<PptVersion> getVersions() { return versions; }
    public void setVersions(List<PptVersion> versions) { this.versions = versions == null ? new ArrayList<>() : versions; }
}

