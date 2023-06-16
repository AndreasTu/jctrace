package de.turban.deadlock.tracer.runtime;

import javax.annotation.Nullable;

public final class JctraceUtil {

    private JctraceUtil() {

    }


    @SuppressWarnings({"unchecked"})
    public static <T> T uncheckedCast(@Nullable Object obj) {
        return (T) obj;
    }

    public static <T> void ensureIsNull(@Nullable T obj) {
        if (obj != null) {
            throw new IllegalStateException();
        }
    }

    public static void ensureArgument(boolean expr) {
        if (!expr) {
            throw new IllegalStateException();
        }
    }
}
