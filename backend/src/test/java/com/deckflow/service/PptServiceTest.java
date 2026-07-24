package com.deckflow.service;

import com.deckflow.domain.DeckSpec;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PptServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsReadablePowerPointWithExpectedPageCount() throws Exception {
        DeckSpec spec = new DeckSpec();
        spec.setTitle("测试演示文稿");
        spec.setSubtitle("由 DeckFlow 生成");
        spec.setTheme("现代简约");

        DeckSpec.SlideSpec cover = new DeckSpec.SlideSpec();
        cover.setTitle("测试演示文稿");
        cover.setLayout("cover");

        DeckSpec.SlideSpec content = new DeckSpec.SlideSpec();
        content.setTitle("核心观点");
        content.setLayout("content");
        content.setBullets(List.of("第一条关键观点", "第二条关键观点"));
        spec.setSlides(List.of(cover, content));

        Path result = new PptService(tempDir.toString()).generate("test-session", 1, spec);

        assertThat(result).exists().hasExtension("pptx");
        try (InputStream input = Files.newInputStream(result); XMLSlideShow slideShow = new XMLSlideShow(input)) {
            assertThat(slideShow.getSlides()).hasSize(2);
            assertThat(slideShow.getSlides().get(1).getShapes()).isNotEmpty();
        }
    }
}
