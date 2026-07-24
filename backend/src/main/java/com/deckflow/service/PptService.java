package com.deckflow.service;

import com.deckflow.domain.DeckSpec;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PptService {
    private static final Dimension WIDE = new Dimension(13_333_333, 7_500_000);
    private static final Color NAVY = new Color(20, 28, 47);
    private static final Color BLUE = new Color(68, 104, 255);
    private static final Color ICE = new Color(239, 243, 255);
    private static final Color MUTED = new Color(99, 110, 135);

    private final Path storageRoot;

    public PptService(@Value("${deckflow.storage-path}") String storagePath) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public Path generate(String sessionId, int version, DeckSpec spec) {
        try {
            Path sessionDir = storageRoot.resolve(sessionId).normalize();
            if (!sessionDir.startsWith(storageRoot)) throw new IllegalArgumentException("非法会话 ID");
            Files.createDirectories(sessionDir);
            String safeTitle = sanitizeFileName(spec.getTitle());
            Path output = sessionDir.resolve("v" + version + "-" + safeTitle + ".pptx");

            try (XMLSlideShow ppt = new XMLSlideShow(); OutputStream stream = Files.newOutputStream(output)) {
                ppt.setPageSize(WIDE);
                for (int i = 0; i < spec.getSlides().size(); i++) {
                    DeckSpec.SlideSpec slide = spec.getSlides().get(i);
                    if (i == 0 || "cover".equalsIgnoreCase(slide.getLayout())) {
                        addCover(ppt, spec, slide);
                    } else {
                        addContentSlide(ppt, slide, i + 1, spec.getSlides().size());
                    }
                }
                ppt.write(stream);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("PPT 文件生成失败", e);
        }
    }

    private void addCover(XMLSlideShow ppt, DeckSpec spec, DeckSpec.SlideSpec slideSpec) {
        XSLFSlide slide = ppt.createSlide();
        addBackground(slide, NAVY);

        XSLFAutoShape accent = slide.createAutoShape();
        accent.setShapeType(ShapeType.RECT);
        accent.setAnchor(new Rectangle2D.Double(66, 95, 9, 280));
        accent.setFillColor(BLUE);
        accent.setLineColor(BLUE);

        XSLFTextBox eyebrow = slide.createTextBox();
        eyebrow.setAnchor(new Rectangle2D.Double(100, 95, 700, 36));
        addRun(eyebrow, spec.getTheme().toUpperCase(), 15, new Color(153, 171, 255), true);

        XSLFTextBox title = slide.createTextBox();
        title.setAnchor(new Rectangle2D.Double(100, 155, 720, 190));
        title.setVerticalAlignment(VerticalAlignment.MIDDLE);
        addRun(title, spec.getTitle().isBlank() ? slideSpec.getTitle() : spec.getTitle(), 38, Color.WHITE, true);

        XSLFTextBox subtitle = slide.createTextBox();
        subtitle.setAnchor(new Rectangle2D.Double(103, 365, 670, 78));
        addRun(subtitle, spec.getSubtitle(), 18, new Color(198, 205, 220), false);

        XSLFAutoShape orb = slide.createAutoShape();
        orb.setShapeType(ShapeType.ELLIPSE);
        orb.setAnchor(new Rectangle2D.Double(800, 70, 150, 150));
        orb.setFillColor(BLUE);
        orb.setLineColor(BLUE);
    }

    private void addContentSlide(XMLSlideShow ppt, DeckSpec.SlideSpec slideSpec, int page, int total) {
        XSLFSlide slide = ppt.createSlide();
        addBackground(slide, Color.WHITE);

        XSLFAutoShape topBar = slide.createAutoShape();
        topBar.setShapeType(ShapeType.RECT);
        topBar.setAnchor(new Rectangle2D.Double(0, 0, 960, 12));
        topBar.setFillColor(BLUE);
        topBar.setLineColor(BLUE);

        XSLFTextBox kicker = slide.createTextBox();
        kicker.setAnchor(new Rectangle2D.Double(68, 43, 200, 28));
        addRun(kicker, String.format("%02d / %02d", page, total), 12, BLUE, true);

        XSLFTextBox title = slide.createTextBox();
        title.setAnchor(new Rectangle2D.Double(65, 80, 830, 70));
        addRun(title, slideSpec.getTitle(), 28, NAVY, true);

        XSLFAutoShape panel = slide.createAutoShape();
        panel.setShapeType(ShapeType.ROUND_RECT);
        panel.setAnchor(new Rectangle2D.Double(65, 170, 830, 310));
        panel.setFillColor(ICE);
        panel.setLineColor(new Color(220, 227, 246));

        XSLFTextBox body = slide.createTextBox();
        body.setAnchor(new Rectangle2D.Double(95, 192, 770, 260));
        body.setWordWrap(true);
        body.clearText();

        if (slideSpec.getBullets().isEmpty()) {
            addBullet(body, "本页内容待补充");
        } else {
            for (String bullet : slideSpec.getBullets()) {
                addBullet(body, bullet);
            }
        }

        XSLFTextBox footer = slide.createTextBox();
        footer.setAnchor(new Rectangle2D.Double(68, 505, 820, 24));
        addRun(footer, "DECKFLOW AI  ·  " + page, 10, MUTED, false);
    }

    private void addBackground(XSLFSlide slide, Color color) {
        XSLFAutoShape background = slide.createAutoShape();
        background.setShapeType(ShapeType.RECT);
        background.setAnchor(new Rectangle2D.Double(0, 0, 960, 540));
        background.setFillColor(color);
        background.setLineColor(color);
    }

    private void addRun(XSLFTextBox box, String text, double size, Color color, boolean bold) {
        box.clearText();
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        paragraph.setSpaceAfter(0d);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text == null ? "" : text);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(size);
        run.setFontColor(color);
        run.setBold(bold);
    }

    private void addBullet(XSLFTextBox box, String text) {
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        paragraph.setBullet(true);
        paragraph.setIndent(22d);
        paragraph.setLeftMargin(8d);
        paragraph.setSpaceAfter(13d);
        paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(20d);
        run.setFontColor(NAVY);
    }

    private String sanitizeFileName(String input) {
        String result = input == null ? "presentation" : input.replaceAll("[\\\\/:*?\"<>|]", "_").strip();
        return result.isBlank() ? "presentation" : result.substring(0, Math.min(60, result.length()));
    }
}
