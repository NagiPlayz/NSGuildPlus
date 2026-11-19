package com.nagistudios.nsguildplus.guild;

import java.util.Locale;

public enum Role {
    CAPTAIN("captain"),
    VICE_CAPTAIN("vice-captain"),
    OFFICER("officer"),
    MEMBER("member");

    private final String key;

    Role(String key) {
        this.key = key;
    }

    /** Key below "roles:" in lang.yml. */
    public String key() {
        return key;
    }

    /** True if this role is the same as or higher than the given role. */
    public boolean atLeast(Role min) {
        return this.ordinal() <= min.ordinal();
    }

    public static Role parse(String s, Role def) {
        if (s == null) {
            return def;
        }
        try {
            return Role.valueOf(s.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
