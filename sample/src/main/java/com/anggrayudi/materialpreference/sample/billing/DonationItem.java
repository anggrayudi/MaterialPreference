package com.anggrayudi.materialpreference.sample.billing;

public class DonationItem {

    public final String sku;
    public final String title;
    public final String price;

    public DonationItem(String sku, String title, String price) {
        this.sku = sku;
        this.title = title;
        this.price = price;
    }
}
