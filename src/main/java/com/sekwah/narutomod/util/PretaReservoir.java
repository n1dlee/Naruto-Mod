package com.sekwah.narutomod.util;

public final class PretaReservoir {
    public static final int WINDUP = 6;
    public static final int REST = 26;
    static final float CAPACITY = 25f;
    private static final float LIMIT = 8f;
    private float room = CAPACITY;
    private float yielded;

    public boolean exhausted() { return this.room <= 0.001f; }

    public Receipt absorb(float amount, boolean full) {
        if (!Float.isFinite(amount) || amount <= 0 || this.exhausted() || full && amount > this.room) {
            return new Receipt(0, 0);
        }
        float accepted = Math.min(this.room, full ? amount : amount * 0.78f);
        this.room -= accepted;
        float recover = Math.min(LIMIT - this.yielded, accepted * 0.34f);
        this.yielded += recover;
        return new Receipt(accepted, recover);
    }

    public record Receipt(float absorbed, float chakra) {
    }

    public enum Stage { OPEN, READY, SIP, FADE }
}
