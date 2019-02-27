package com.anggrayudi.materialpreference.sample.billing;

public final class DonationItem {

    public String sku;
    public String title;
    public String price;

    private DonationItem(String sku, String title, String price) {
        this.sku = sku;
        this.title = title;
        this.price = price;
    }

    public static DonationItem[] items = {
            new DonationItem("good", "Good job!", "$0.99"),
            new DonationItem("love", "I love it!", "$1.99"),
            new DonationItem("perfect", "Perfect!", "$2.99"),
            new DonationItem("saved_my_life", "You saved my life", "$4.99"),
            new DonationItem("stfu", "Shut up and take my money!", "$6.99")
    };
}
