package com.deckflow.service;

import com.deckflow.domain.ChatMessage;
import com.deckflow.domain.ConversationSession;
import com.deckflow.domain.DeckSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {
    private static final String CHAT_SYSTEM = """
            你是 DeckFlow，一位资深演示文稿策划师。你的任务是通过多轮中文对话明确用户的 PPT 需求。
            重点澄清：演示目标、受众、使用场景、核心观点、内容范围、视觉风格、语气、数据或案例要求。
            不要一次提出过多问题，每轮最多提出 2 个最关键的问题。已经明确的信息不要重复询问。
            如果需求足够清楚，就用简短条目总结你理解的方案。
            无论需求是否完整，每次回复的最后必须单独一行写：是否现在开始生成PPT？
            如果已有 PPT，用户的新消息应被理解为修改意见；先复述拟修改内容，再询问是否生成新版本。
            不要输出 JSON，不要声称已经生成文件。
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AiService(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public String converse(ConversationSession session, String userMessage) {
        String context = buildConversationContext(session.getMessages());
        String deckContext = session.getDeckSpec() == null ? "尚未生成 PPT。"
                : "已有 PPT 结构如下：\n" + toJson(session.getDeckSpec());
        String userPrompt = """
                会话参数：
                - 目标页数：%d 页（含封面）
                - 初始要求：%s
                - 当前状态：%s

                历史对话：
                %s

                %s

                用户最新消息：
                %s
                """.formatted(
                session.getPageCount(),
                session.getInitialRequirement(),
                session.getStatus(),
                context,
                deckContext,
                userMessage
        );

        String reply = chatClient.prompt()
                .system(CHAT_SYSTEM)
                .user(userPrompt)
                .call()
                .content();
        if (reply == null || reply.isBlank()) {
            throw new IllegalStateException("模型未返回有效内容");
        }
        if (!reply.contains("是否现在开始生成PPT？")) {
            reply = reply.strip() + "\n\n是否现在开始生成PPT？";
        }
        return reply;
    }

    public DeckSpec createOrReviseDeck(ConversationSession session) {
        String previousDeck = session.getDeckSpec() == null ? "无，这是首次生成。"
                : toJson(session.getDeckSpec());
        String prompt = """
                请根据下列完整会话，为用户生成或修订一份可直接制作成 PPT 的结构化内容。

                强制要求：
                1. 总页数必须正好为 %d 页，包含封面。
                2. slides 数组必须正好有 %d 项。
                3. 第 1 页 layout 必须为 "cover"，其余可用 "content"、"section" 或 "summary"。
                4. 每页标题简洁；普通内容页提供 2-5 条 bullets，每条不超过 45 个汉字。
                5. speakerNotes 提供演讲提示，可为空。
                6. 如果存在旧版本，必须在保留未被否定内容的基础上落实用户最新修改意见。
                7. 仅输出合法 JSON，不要 Markdown 代码块，不要解释。

                JSON 结构：
                {
                  "title": "演示文稿标题",
                  "subtitle": "副标题",
                  "theme": "视觉风格描述",
                  "slides": [
                    {
                      "title": "页面标题",
                      "layout": "cover|content|section|summary",
                      "bullets": ["要点"],
                      "speakerNotes": "演讲提示"
                    }
                  ]
                }

                初始要求：%s
                完整对话：
                %s
                旧版 PPT：
                %s
                """.formatted(
                session.getPageCount(),
                session.getPageCount(),
                session.getInitialRequirement(),
                buildConversationContext(session.getMessages()),
                previousDeck
        );

        String raw = chatClient.prompt()
                .system("你是专业 PPT 内容架构师，严格按用户要求输出合法 JSON。")
                .user(prompt)
                .call()
                .content();
        return parseDeck(raw, session.getPageCount());
    }

    private DeckSpec parseDeck(String raw, int pageCount) {
        if (raw == null || raw.isBlank()) throw new IllegalStateException("模型未返回 PPT 内容");
        String json = raw.strip();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalStateException("模型返回的 PPT 结构不是有效 JSON");
        json = json.substring(start, end + 1);
        try {
            DeckSpec spec = objectMapper.readValue(json, DeckSpec.class);
            normalizePageCount(spec, pageCount);
            return spec;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法解析模型生成的 PPT 结构，请重试", e);
        }
    }

    private void normalizePageCount(DeckSpec spec, int pageCount) {
        List<DeckSpec.SlideSpec> slides = spec.getSlides();
        while (slides.size() > pageCount) slides.remove(slides.size() - 1);
        while (slides.size() < pageCount) {
            DeckSpec.SlideSpec slide = new DeckSpec.SlideSpec();
            slide.setTitle(slides.size() == pageCount - 1 ? "总结与下一步" : "补充内容");
            slide.setLayout(slides.size() == pageCount - 1 ? "summary" : "content");
            slide.setBullets(List.of("请结合实际材料补充本页内容"));
            slides.add(slide);
        }
        if (!slides.isEmpty()) slides.get(0).setLayout("cover");
    }

    private String buildConversationContext(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return "暂无";
        StringBuilder result = new StringBuilder();
        for (ChatMessage message : messages) {
            result.append("user".equals(message.role()) ? "用户：" : "助手：")
                    .append(message.content()).append("\n");
        }
        return result.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法构造模型上下文", e);
        }
    }
}

