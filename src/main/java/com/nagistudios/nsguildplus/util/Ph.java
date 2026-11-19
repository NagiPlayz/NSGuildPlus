package com.nagistudios.nsguildplus.util;

import java.util.HashMap;
import java.util.Map;

/** Small helper to build placeholder maps. */
public final class Ph {
    private Ph() {
    }

    public static Map<String, String> of(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return m;
    }

    public static Map<String, String> with(Map<String, String> base, String... kv) {
        Map<String, String> m = new HashMap<>();
        if (base != null) {
            m.putAll(base);
        }
        m.putAll(of(kv));
        return m;
    }
}
