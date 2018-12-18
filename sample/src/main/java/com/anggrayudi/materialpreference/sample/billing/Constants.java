package com.anggrayudi.materialpreference.sample.billing;

public final class Constants {

    public static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgSJPLtro04R+Eo05yDJaRZa70QeACHarNi2GBIFWWQBTMrvS2L568EL5uq6g+tyy29W3TpU0800ahIIijnkWgN8t7SmkSMsd9XDTPaphnqcVQxAKGkM7QxnUi6RoJSFlSkkjnklcJqIhkdaBj3hWlTmPF3Q3vgCDM4ZUs0O8k71mPoSWDcZlbsZ3bLdOIpFoQt4PsMjqJBP9ZxVrdAeuYYKyrWRsN5RfRvSqE1go0seoirn11n4c46md0Zp+VrQwOYCtIhc12n4i6H9NjPrbddLUJG+WSSExkPOa1gS6NDqCfEiOP0Fz64BaQlcwA1UgtU78M3YCX3nBVZ2GaiQhoQIDAQAB";

    public static final DonationItem[] DONATION_ITEMS = {
            new DonationItem("good", "Good hob!", "$0.99"),
            new DonationItem("love", "I love it!", "$1.99"),
            new DonationItem("perfect", "Perfect!", "$2.99"),
            new DonationItem("saved_my_life", "You saved my life!", "$4.99"),
            new DonationItem("stfu", "Shut up and take my money!", "$6.99")
    };
}
