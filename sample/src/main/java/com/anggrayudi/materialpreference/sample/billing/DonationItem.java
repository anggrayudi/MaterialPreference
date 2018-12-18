package com.anggrayudi.materialpreference.sample.billing;

public class DonationItem {

    public final String sku;
    public final String title;
    public final String price;
    public final String description;

    public DonationItem(String sku, String title, String price, String description) {
        this.sku = sku;
        this.title = title;
        this.price = price;
        this.description = description;
    }
}
