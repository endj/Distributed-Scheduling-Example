package org.example.locking;

public enum Leases {
    USER_UPDATE("user_updating"),
    USER_INSERTING("user_profiles_fetching");
    final String field;

    Leases(String field) {
        this.field = field;
    }
}