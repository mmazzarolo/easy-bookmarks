package com.mmazzarolo.dev.easy_bookmarks.models;

import java.io.Serializable;

/**
 * Created by Matteo on 01/08/2015.
 */
public class PrivateTag implements Serializable {

    private String name;

    public PrivateTag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override public String toString() {
        return name;
    }
}
