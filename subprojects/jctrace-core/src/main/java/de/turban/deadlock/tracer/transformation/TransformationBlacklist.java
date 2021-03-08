package de.turban.deadlock.tracer.transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class TransformationBlacklist {

    private static final List<String> blackList;

    static {
        String list = System.getProperty("de.turban.DeadLockTracer.blacklist");
        if (list == null) {
            blackList = Collections.emptyList();
        } else {
            List<String> blackListLoc = new ArrayList<>();
            for (String pkg : list.trim().split(";")) {
                String pkgTrimmed = pkg.trim();
                if (!pkgTrimmed.isEmpty()) {
                    blackListLoc.add(pkgTrimmed);
                }
            }
            blackList = Collections.unmodifiableList(blackListLoc);
        }
    }

    private static List<String> getPackageBlacklist() {
        return blackList;
    }

    public static boolean isClassBlacklisted(String className) {

        if (className.contains("/")) {
            className = className.replace("/", ".");
        }
        for (String pkg : getPackageBlacklist()) {
            if (className.startsWith(pkg)) {

                return true;
            }
        }

        return false;
    }
}
