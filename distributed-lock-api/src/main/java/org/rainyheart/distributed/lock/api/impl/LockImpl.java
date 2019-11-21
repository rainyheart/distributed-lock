package org.rainyheart.distributed.lock.api.impl;

import java.util.Arrays;

import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.LockLevel;

public class LockImpl implements Lock {

    private final byte[] value;
    private final String id;
    private final LockLevel level;
    private final Integer mode; // 1 is persistence lock, otherwise temporary lock

    public LockImpl(String id, byte[] value, LockLevel level) {
        super();
        this.value = value;
        this.id = id;
        this.level = level;
        this.mode = Integer.valueOf(0);
    }
    
    public LockImpl(String id, byte[] value, LockLevel level, Integer mode) {
        super();
        this.value = value;
        this.id = id;
        this.level = level;
        this.mode = mode;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public LockLevel level() {
        return this.level;
    }

    @Override
    public byte[] value() {
        return this.value;
    }

    @Override
    public String toString() {
        return "LockImpl [value=" + Arrays.toString(value) + ", id=" + id + ", level=" + level + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LockImpl other = (LockImpl) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (level != other.level)
            return false;
        if (mode == null) {
            if (other.mode != null)
                return false;
        } else if (!mode.equals(other.mode))
            return false;
        if (!Arrays.equals(value, other.value))
            return false;
        return true;
    }

    @Override
    public Integer mode() {
        return this.mode;
    }

}
