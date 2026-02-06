package com.guideon.core.domain.zone.entity;

public enum ZoneType {
    INNER,
    SUB;

    public int toLevel() {
        return this == INNER ? 1 : 2;
    }
}
