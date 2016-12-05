package sixue.naviereader.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String id;
    private String title;
    private boolean isLocal;
    private String localPath;
    private String siteId;
    private String sitePara;
    @JsonIgnore
    private List<Chapter> chapterList;
    private String currentChapterId;
    private int currentChapterIndex;
    private String author;

    public Book() {
        chapterList = new ArrayList<>();
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

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
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

    public String getCurrentChapterId() {
        return currentChapterId;
    }

    public void setCurrentChapterId(String currentChapterId) {
        this.currentChapterId = currentChapterId;
    }

    public int getCurrentChapterIndex() {
        return currentChapterIndex;
    }

    public void setCurrentChapterIndex(int currentChapterIndex) {
        this.currentChapterIndex = currentChapterIndex;
    }

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
        sb.append("isLocal=");
        sb.append(isLocal);
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
}
