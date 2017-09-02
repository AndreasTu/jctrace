package de.turban.deadlock.tracer.runtime.datacollection;

import java.lang.ref.WeakReference;
import java.util.Objects;

class LockWeakRef extends WeakReference<Object> {

    private final int hash;

    public LockWeakRef(Object referent) {
        super(referent);
        Objects.requireNonNull(referent);
        hash = System.identityHashCode(referent);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LockWeakRef other = (LockWeakRef) obj;
        Object otherObj = other.get();
        Object thisObj = this.get();
        return otherObj == thisObj;
    }

}