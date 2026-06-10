package com.example.myapplication;

import java.io.Serializable;

public class Category implements Serializable {
    private String id;
    private String name;
    private String imageUrl;
    private String parentId;
    private boolean showOnHome;
    private int sortOrder;

    public Category() {
        this.showOnHome = true;
    }

    public Category(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.parentId = null; // Default to top-level category
    }

    public Category(String id, String name, String imageUrl, String parentId) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.parentId = parentId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public boolean isShowOnHome() { return showOnHome; }
    public void setShowOnHome(boolean showOnHome) { this.showOnHome = showOnHome; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
