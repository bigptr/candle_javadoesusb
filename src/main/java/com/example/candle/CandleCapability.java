package com.example.candle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CandleCapability {
    public int feature;
    public int fclkCan;
    public int tseg1Min;
    public int tseg1Max;
    public int tseg2Min;
    public int tseg2Max;
    public int sjwMax;
    public int brpMin;
    public int brpMax;
    public int brpInc;

    public CandleCapability(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.feature = buffer.getInt();
        this.fclkCan = buffer.getInt();
        this.tseg1Min = buffer.getInt();
        this.tseg1Max = buffer.getInt();
        this.tseg2Min = buffer.getInt();
        this.tseg2Max = buffer.getInt();
        this.sjwMax = buffer.getInt();
        this.brpMin = buffer.getInt();
        this.brpMax = buffer.getInt();
        this.brpInc = buffer.getInt();
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(40); // 10 * 4 bytes
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(feature);
        buffer.putInt(fclkCan);
        buffer.putInt(tseg1Min);
        buffer.putInt(tseg1Max);
        buffer.putInt(tseg2Min);
        buffer.putInt(tseg2Max);
        buffer.putInt(sjwMax);
        buffer.putInt(brpMin);
        buffer.putInt(brpMax);
        buffer.putInt(brpInc);
        return buffer.array();
    }
}