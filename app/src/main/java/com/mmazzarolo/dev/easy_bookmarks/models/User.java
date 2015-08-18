package com.mmazzarolo.dev.easy_bookmarks.models;

/**
 * Created by Matteo on 22/07/2015.
 */
public class User {

    public String id;

    private String displayName;
    private String provider;
    private String providerId;

    public User(String id, String displayName, String provider, String providerId) {
        this.id = id;
        this.displayName = displayName;
        this.provider = provider;
        this.providerId = providerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
