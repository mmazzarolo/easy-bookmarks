package com.mmazzarolo.dev.easy_bookmarks.models;

import com.firebase.client.DataSnapshot;

import org.parceler.Parcel;

import java.util.Map;

/**
 * Created by Matteo on 08/07/2015.
 */
@Parcel
public class PublicBookmark {

    protected String key;
    protected String url;
    protected String title;
    protected String lastUser;
    protected long numSaved;
    protected long lastSaved;

    public PublicBookmark() {
    }

    public PublicBookmark(PrivateBookmark privateBookmark, String lastUser) {
        this.key = privateBookmark.getKey();
        this.url = privateBookmark.getUrl();
        this.title = privateBookmark.getTitle();
        this.lastUser = lastUser;
        this.numSaved = 1;
    }

    public PublicBookmark(DataSnapshot dataSnapshot) {
        Map<String, Object> bookmarkMap = (Map<String, Object>) dataSnapshot.getValue();
        this.key = dataSnapshot.getKey();
        this.url = (String) bookmarkMap.get("url");
        this.title = (String) bookmarkMap.get("title");
        this.lastUser = (String) bookmarkMap.get("lastUser");
        this.numSaved = (long) bookmarkMap.get("numSaved");
        this.lastSaved = (long) bookmarkMap.get("lastSaved");
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

    public String getLastUser() {
        return lastUser;
    }

    public void setLastUser(String lastUser) {
        this.lastUser = lastUser;
    }

    public long getNumSaved() {
        return numSaved;
    }

    public void setNumSaved(int numSaved) {
        this.numSaved = numSaved;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setNumSaved(long numSaved) {
        this.numSaved = numSaved;
    }

    public long getLastSaved() {
        return lastSaved;
    }

    public void setLastSaved(long lastSaved) {
        this.lastSaved = lastSaved;
    }
}
