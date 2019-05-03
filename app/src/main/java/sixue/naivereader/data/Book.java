package sixue.naivereader.data;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

import sixue.naivereader.helper.BookHelper;
import sixue.naivereader.helper.LocalTextHelper;
import sixue.naivereader.helper.OnlineHelper;
import sixue.naivereader.helper.PacketHelper;

public class Book {
    private String id;
    private String title;
    private String author;
    private BookKind kind;
    private String localPath;
    private String siteId;
    private String sitePara;
    @JsonIgnore
    private List<Chapter> chapterList;
    private List<Source> sources;
    private int currentChapterIndex;
    private int currentPosition;
    private int wordCount;
    private String coverSavePath;
    private boolean end;
    @JsonIgnore
    private BookHelper bookHelper;

    public Book() {
        id = "";
        title = "";
        author = "";
        siteId = "";
        sitePara = "";
        chapterList = new ArrayList<>();
        sources = new ArrayList<>();
        coverSavePath = "";
        end = false;
        bookHelper = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public BookKind getKind() {
        return kind;
    }

    public void setKind(BookKind kind) {
        this.kind = kind;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getSitePara() {
        return sitePara;
    }

    public void setSitePara(String sitePara) {
        this.sitePara = sitePara;
    }

    public List<Chapter> getChapterList() {
        return chapterList;
    }

    public void setChapterList(List<Chapter> chapterList) {
        this.chapterList = chapterList;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public int getCurrentChapterIndex() {
        return currentChapterIndex;
    }

    public void setCurrentChapterIndex(int currentChapterIndex) {
        this.currentChapterIndex = currentChapterIndex;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getCoverSavePath() {
        return coverSavePath;
    }

    public void setCoverSavePath(String coverSavePath) {
        this.coverSavePath = coverSavePath;
    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("id=");
        sb.append(id);
        sb.append(",");
        sb.append("title=");
        sb.append(title);
        sb.append(",");
        sb.append("author=");
        sb.append(author);
        sb.append(",");
        sb.append("kind=");
        sb.append(kind);
        if (localPath != null) {
            sb.append(",");
            sb.append("localPath=");
            sb.append(localPath);
        }
        if (siteId != null) {
            sb.append(",");
            sb.append("siteId=");
            sb.append(siteId);
        }
        if (sitePara != null) {
            sb.append(",");
            sb.append("sitePara=");
            sb.append(sitePara);
        }
        sb.append("}");
        return sb.toString();
    }

    public BookHelper buildHelper() {
        if (bookHelper == null) {
            if (kind == BookKind.LocalText) {
                bookHelper = new LocalTextHelper(this);
            } else if (kind == BookKind.Online) {
                bookHelper = new OnlineHelper(this);
            } else if (kind == BookKind.Packet) {
                bookHelper = new PacketHelper(this);
            } else {
                throw new IllegalArgumentException("Unknown KIND: " + kind.toString());
            }
        }
        return bookHelper;
    }
}
