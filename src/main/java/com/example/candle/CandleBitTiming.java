package com.example.candle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CandleBitTiming {
    public int propSeg;
    public int phaseSeg1;
    public int phaseSeg2;
    public int sjw;
    public int brp;

    public CandleBitTiming(int propSeg, int phaseSeg1, int phaseSeg2, int sjw, int brp) {
        this.propSeg = 1;//propSeg;
        this.phaseSeg1 = phaseSeg1;
        this.phaseSeg2 = phaseSeg2;
        this.sjw = 1;//sjw;
        this.brp = brp;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(20); // 5 * 4 bytes
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(propSeg);
        buffer.putInt(phaseSeg1);
        buffer.putInt(phaseSeg2);
        buffer.putInt(sjw);
        buffer.putInt(brp);
        return buffer.array();
    }
}