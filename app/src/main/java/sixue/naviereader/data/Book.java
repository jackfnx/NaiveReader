package sixue.naviereader.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    public Book() {

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
}
