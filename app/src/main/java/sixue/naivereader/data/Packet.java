package sixue.naivereader.data;

import java.util.List;

public class Packet {
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Boolean getSimple() {
        return simple;
    }

    public void setSimple(Boolean simple) {
        this.simple = simple;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<PackChapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<PackChapter> chapters) {
        this.chapters = chapters;
    }

    private String title;
    private String author;
    private Boolean simple;
    private String key;
    private String summary;
    private long timestamp;
    private List<PackChapter> chapters;
}
