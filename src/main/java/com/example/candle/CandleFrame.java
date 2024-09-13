package com.example.candle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CandleFrame {
    public int echoId;
    public int canId;
    public byte canDlc;
    public byte channel;
    public byte flags;
    public byte reserved;
    public byte[] data;

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(echoId);
        buffer.putInt(canId);
        buffer.put(canDlc);
        buffer.put(channel);
        buffer.put(flags);
        buffer.put(reserved);
        buffer.put(data);
        return buffer.array();
    }
}

/* 
public class CandleFrame {
    public int echoId;
    public int canId;
    public byte canDlc;
    public byte channel;
    public byte flags;
    public byte reserved;
    public byte[] data;
    public long timestampUs; // Changed from int to long

    public CandleFrame() {
        // Default constructor
        data = new byte[8]; // Initialize data array with size 8
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN); // Increased size to 28 bytes
        buffer.putInt(echoId);
        buffer.putInt(canId);
        buffer.put(canDlc);
        buffer.put(channel);
        buffer.put(flags);
        buffer.put(reserved);
        buffer.put(data);
        buffer.putLong(timestampUs); // Changed from putInt to putLong
        return buffer.array();
    }
}
    */