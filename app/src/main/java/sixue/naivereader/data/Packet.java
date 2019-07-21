package sixue.naivereader.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getRegexps() {
        return regexps;
    }

    public void setRegexps(List<String> regexps) {
        this.regexps = regexps;
    }

    private String title;
    private String author;
    private Boolean simple;
    private String key;
    private String summary;
    private long timestamp;
    private String source;
    @JsonIgnore
    private List<Chapter> chapters;
    private List<String> regexps;
}
