package de.turban.deadlock.tracer.runtime.serdata;

import java.io.Serializable;
import java.util.Comparator;

public interface IHasRevision extends Serializable {

    int getRevision();

    public static final class RevisionComparator implements Comparator<IHasRevision> {

        public static final RevisionComparator INSTANCE = new RevisionComparator();

        @Override
        public int compare(IHasRevision o1, IHasRevision o2) {
            int r1 = o1.getRevision();
            int r2 = o2.getRevision();
            if (r1 < r2) {
                return -1;
            }
            if (r1 > r2) {
                return 1;
            }
            return 0;
        }

    }
}
