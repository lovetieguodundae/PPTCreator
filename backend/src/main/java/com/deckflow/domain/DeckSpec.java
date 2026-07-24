package com.deckflow.domain;

import java.util.ArrayList;
import java.util.List;

public class DeckSpec {
    private String title = "";
    private String subtitle = "";
    private String theme = "现代简约";
    private List<SlideSpec> slides = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public List<SlideSpec> getSlides() { return slides; }
    public void setSlides(List<SlideSpec> slides) { this.slides = slides == null ? new ArrayList<>() : slides; }

    public static class SlideSpec {
        private String title = "";
        private String layout = "content";
        private List<String> bullets = new ArrayList<>();
        private String speakerNotes = "";

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getLayout() { return layout; }
        public void setLayout(String layout) { this.layout = layout; }
        public List<String> getBullets() { return bullets; }
        public void setBullets(List<String> bullets) { this.bullets = bullets == null ? new ArrayList<>() : bullets; }
        public String getSpeakerNotes() { return speakerNotes; }
        public void setSpeakerNotes(String speakerNotes) { this.speakerNotes = speakerNotes; }
    }
}

