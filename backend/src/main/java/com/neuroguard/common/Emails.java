package com.neuroguard.common;

import java.util.Locale;

public final class Emails {

    private Emails() {
    }

    /** Emails are stored and looked up trimmed and lower-cased. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
