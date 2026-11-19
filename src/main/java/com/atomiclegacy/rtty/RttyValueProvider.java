package com.atomiclegacy.rtty;

/** A machine whose state can be published by an HBM RTTY reader. */
public interface RttyValueProvider extends RttyInfo {
    String provideRttyValue(String name);

    /** [parity T2: ROR] false for blocks that share a Java class with an IRORValueProvider but were not one in 1.7.10. */
    default boolean rttyProvidesValues() {
        return true;
    }
}
