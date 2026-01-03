package com.myk.vitrin;

public class Ad {
    private int id;
    private String title;
    private String price;
    private String description;
    private String category;
    private String subcategory;
    private String imageUri;

    public Ad(int id, String title, String price, String description, String category, String subcategory, String imageUri) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.description = description;
        this.category = category;
        this.subcategory = subcategory;
        this.imageUri = imageUri;
    }

    public Ad(String title, String price, String description, String category, String subcategory, String imageUri) {
        this.title = title;
        this.price = price;
        this.description = description;
        this.category = category;
        this.subcategory = subcategory;
        this.imageUri = imageUri;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getPrice() { return price; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public String getImageUri() { return imageUri; }
}