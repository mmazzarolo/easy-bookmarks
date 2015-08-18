package com.mmazzarolo.dev.easy_bookmarks.models;

import com.mmazzarolo.dev.easy_bookmarks.Utilities;
import com.firebase.client.DataSnapshot;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Matteo on 08/07/2015.
 */
@Parcel
public class PrivateBookmark {

    protected String key;
    protected String url;
    protected String title;
    protected String titleCustom;
    protected long dateSaved;
    protected List<String> tags = new ArrayList<>();

    public PrivateBookmark() {}

    public PrivateBookmark(String key, String url, String title, String titleCustom) {
        this.key = key;
        this.url = url;
        this.title = title;
        this.titleCustom = titleCustom;
    }

    public PrivateBookmark(PrivateBookmark privateBookmark) {
        this.key = privateBookmark.getKey();
        this.url = privateBookmark.getUrl();
        this.title = privateBookmark.getTitle();
        this.titleCustom = privateBookmark.getTitleCustom();
        this.dateSaved = privateBookmark.dateSaved;
        this.tags = privateBookmark.getTags();
    }

    public PrivateBookmark(DataSnapshot dataSnapshot) {
        Map<String, Object> bookmarkMap = (Map<String, Object>) dataSnapshot.getValue();
        this.key = dataSnapshot.getKey();
        this.url = (String) bookmarkMap.get("url");
        this.title = (String) bookmarkMap.get("title");
        this.titleCustom = (String) bookmarkMap.get("titleCustom");
        this.dateSaved = (Long) bookmarkMap.get("dateSaved");
        if (bookmarkMap.get("tags") != null) {
            this.tags = (List<String>) bookmarkMap.get("tags");
        }
    }

    public void setTagsFromPrivateTagList(List<PrivateTag> privateTagList) {
        List<String> tagNameList = new ArrayList<>();
        for (PrivateTag tag : privateTagList) {
            tagNameList.add(tag.getName());
        }
        setTags(tagNameList);
    }

    public boolean isTaggedWith(String tag) {
        for (String t :tags) {
            if (t.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleCustom() {
        return titleCustom;
    }

    public void setTitleCustom(String titleCustom) {
        this.titleCustom = titleCustom;
    }

    public long getDateSaved() {
        return dateSaved;
    }

    public List<String> getTags() {
        Utilities.stringListToUppercase(tags);
        return tags;
    }

    public void setTags(List<String> tags) {
        java.util.Collections.sort(tags);
        Utilities.stringListToUppercase(tags);
        this.tags = tags;
    }

    public void setDateSaved(long dateSaved) {
        this.dateSaved = dateSaved;
    }
}
