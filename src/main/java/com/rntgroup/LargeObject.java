package com.rntgroup;

public class LargeObject {

    private LargeObject prev; // Чтобы не зачищался через GC
    private String bigLine = "ABCDEFGRTFHJKLYTUNEWQ".repeat(1_000_000);

    public LargeObject(LargeObject prev) {
        this.prev = prev;
    }
}
