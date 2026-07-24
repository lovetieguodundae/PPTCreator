package com.deckflow.repository;

import com.deckflow.domain.ConversationSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class ConversationRepository {
    private static final String KEY_PREFIX = "deckflow:session:";
    private static final String INDEX_KEY = "deckflow:sessions";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ConversationRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public ConversationSession save(ConversationSession session) {
        try {
            redis.opsForValue().set(KEY_PREFIX + session.getId(), objectMapper.writeValueAsString(session));
            redis.opsForZSet().add(INDEX_KEY, session.getId(), session.getUpdatedAt().toEpochMilli());
            return session;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法序列化会话", e);
        }
    }

    public Optional<ConversationSession> findById(String id) {
        String json = redis.opsForValue().get(KEY_PREFIX + id);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ConversationSession.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法读取会话", e);
        }
    }

    public List<ConversationSession> findAll() {
        Set<String> ids = redis.opsForZSet().reverseRange(INDEX_KEY, 0, -1);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    public void delete(String id) {
        redis.delete(KEY_PREFIX + id);
        redis.opsForZSet().remove(INDEX_KEY, id);
    }
}

