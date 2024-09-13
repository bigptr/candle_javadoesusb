package com.example.candle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CandleDeviceConfig {
    public byte reserved1;
    public byte reserved2;
    public byte reserved3;
    public byte icount;
    public int swVersion;
    public int hwVersion;

    public CandleDeviceConfig(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.reserved1 = buffer.get();
        this.reserved2 = buffer.get();
        this.reserved3 = buffer.get();
        this.icount = buffer.get();
        this.swVersion = buffer.getInt();
        this.hwVersion = buffer.getInt();
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(12); // 3 * 1 byte + 1 * 1 byte + 2 * 4 bytes
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(reserved1);
        buffer.put(reserved2);
        buffer.put(reserved3);
        buffer.put(icount);
        buffer.putInt(swVersion);
        buffer.putInt(hwVersion);
        return buffer.array();
    }
}