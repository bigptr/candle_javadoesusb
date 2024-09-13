package com.example.candle;

import net.codecrete.usb.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Candle {
    private static final int CANDLE_BREQ_HOST_FORMAT = 0;
    private static final int CANDLE_BREQ_BITTIMING = 1;
    private static final int CANDLE_BREQ_MODE = 2;
    private static final int CANDLE_BREQ_BERR = 3;
    private static final int CANDLE_BREQ_BT_CONST = 4;
    private static final int CANDLE_BREQ_DEVICE_CONFIG = 5;
    private static final int CANDLE_TIMESTAMP_GET = 6;

    private static final int CANDLE_DEVMODE_START = 1;
    private static final int CANDLE_DEVMODE_RESET = 0;
    public static final int CANDLE_MODE_HW_TIMESTAMP = 0x01;
    public static final int CANDLE_MODE_LISTEN_ONLY = 0x02;
    public static final int CANDLE_MODE_ONE_SHOT = 0x04;
    public static final int CANDLE_MODE_TRIPLE_SAMPLE = 0x08;

    private UsbDevice device;
    private UsbInterface usbInterface;
    private UsbEndpoint bulkInEndpoint;
    private UsbEndpoint bulkOutEndpoint;

    private boolean listenOnlyMode = false;
    private boolean oneShotMode = false;
    private boolean tripleSampling = false;
    private int numRx = 0;
    private int numTx = 0;
    private int numTxErr = 0;
    private long deviceTicksStart = 0;
    private long hostOffsetStart = 0;

    public Candle(UsbDevice device) {
        this.device = device;
    }

    public boolean open() {
        try {
            device.open();
            usbInterface = device.getInterfaces().get(0); // Assuming the first interface is the correct one
            device.claimInterface(usbInterface.getNumber());

            for (UsbEndpoint endpoint : usbInterface.getCurrentAlternate().getEndpoints()) {
                if (endpoint.getDirection() == UsbDirection.IN && endpoint.getTransferType() == UsbTransferType.BULK) {
                    bulkInEndpoint = endpoint;
                } else if (endpoint.getDirection() == UsbDirection.OUT && endpoint.getTransferType() == UsbTransferType.BULK) {
                    bulkOutEndpoint = endpoint;
                }
            }

            if (bulkInEndpoint == null || bulkOutEndpoint == null) {
                throw new UsbException("Failed to find bulk endpoints");
            }

            // Initialize statistics
            numRx = 0;
            numTx = 0;
            numTxErr = 0;

            // Get initial timestamp
            deviceTicksStart = getDeviceTimestamp();
            hostOffsetStart = System.nanoTime() / 1000; // Convert to microseconds

            return true;
        } catch (UsbException e) {
            System.err.println("Unable to open USB device: " + e.getMessage());
            return false;
        }
    }

    public boolean setHostFormat(int channel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(0x0000beef);
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_BREQ_HOST_FORMAT, channel, usbInterface.getNumber());
            device.controlTransferOut(transfer, buffer.array());
            return true;
        } catch (UsbException e) {
            System.err.println("Failed to set host format: " + e.getMessage());
            return false;
        }
    }

    public boolean setDeviceMode(int channel, int mode, int flags) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(mode);
            buffer.putInt(flags);
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_BREQ_MODE, channel, usbInterface.getNumber());
            device.controlTransferOut(transfer, buffer.array());
            return true;
        } catch (UsbException e) {
            System.err.println("Failed to set device mode: " + e.getMessage());
            return false;
        }
    }

    public CandleDeviceConfig getConfig(int channel) {
        try {
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_BREQ_DEVICE_CONFIG, channel, usbInterface.getNumber());
            byte[] buffer = device.controlTransferIn(transfer, 12); // 3 * 1 byte + 1 * 1 byte + 2 * 4 bytes
            return new CandleDeviceConfig(buffer);
        } catch (UsbException e) {
            System.err.println("Failed to retrieve device config: " + e.getMessage());
            return null;
        }
    }

    public boolean getTimestamp(int channel) {
        try {
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_TIMESTAMP_GET, channel, usbInterface.getNumber());
            byte[] buffer = device.controlTransferIn(transfer, 4); // Assuming 4-byte timestamp
            // Parse the timestamp from buffer
            return true;
        } catch (UsbException e) {
            System.err.println("Failed to retrieve timestamp: " + e.getMessage());
            return false;
        }
    }

    public CandleCapability getCapability(int channel) {
        try {
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_BREQ_BT_CONST, channel, 0);
            byte[] buffer = device.controlTransferIn(transfer, 40); // 10 * 4 bytes
            return new CandleCapability(buffer);
        } catch (UsbException e) {
            System.err.println("Failed to retrieve capability: " + e.getMessage());
            return null;
        }
    }

    public boolean setBitrate(int channel, int bitrate) {
        try {
            if (getCapability(channel).fclkCan != 48000000) {
                System.err.println("This function only works for the candleLight base clock of 48MHz");
                return false;
            }

            CandleBitTiming bitTiming;

            /* 
            switch (bitrate) {
                case 10000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 300);
                    break;
                case 20000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 150);
                    break;
                case 50000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 60);
                    break;
                case 83333:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 36);
                    break;
                case 100000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 30);
                    break;
                case 125000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 24);
                    break;
                case 250000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 12);
                    break;
                case 500000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 6);
                    break;
                case 800000:
                    bitTiming = new CandleBitTiming(1, 12, 2, 1, 4);
                    break;
                case 1000000:
                    bitTiming = new CandleBitTiming(1, 13, 2, 1, 3);
                    break;
                default:
                    System.err.println("Unsupported bitrate");
                    return false;
            }
                    */
                    switch (bitrate) {
                        case 10000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 300);
                            break;
                        case 20000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 150);
                            break;
                        case 50000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 60);
                            break;
                        case 83333:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 36);
                            break;
                        case 100000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 30);
                            break;
                        case 125000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 24);
                            break;
                        case 250000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 12);
                            break;
                        case 500000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 6);
                            break;
                        case 800000:
                            bitTiming = new CandleBitTiming(1, 11, 2, 1, 4);
                            break;
                        case 1000000:
                            bitTiming = new CandleBitTiming(1, 12, 2, 1, 3);
                            break;
                        default:
                        System.err.println("Unsupported bitrate");
                            return false;
                            
                    }

            byte[] data = bitTiming.toByteArray();
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_BREQ_BITTIMING, channel, 0);
            device.controlTransferOut(transfer, data);
            return true;
        } catch (UsbException e) {
            System.err.println("Failed to set bitrate: " + e.getMessage());
            return false;
        }
    }

    public boolean close() {
        try {
            device.releaseInterface(usbInterface.getNumber());
            device.close();
            return true;
        } catch (UsbException e) {
            System.err.println("Unable to close USB device: " + e.getMessage());
            return false;
        }
    }

    public void setListenOnlyMode(boolean enabled) {
        this.listenOnlyMode = enabled;
    }

    public void setOneShotMode(boolean enabled) {
        this.oneShotMode = enabled;
    }

    public void setTripleSampling(boolean enabled) {
        this.tripleSampling = enabled;
    }

    public boolean startChannel(int channel, int flags) {
        /*
        if (listenOnlyMode) {
            flags |= CANDLE_MODE_LISTEN_ONLY;
        }
        if (oneShotMode) {
            flags |= CANDLE_MODE_ONE_SHOT;
        }
        if (tripleSampling) {
            flags |= CANDLE_MODE_TRIPLE_SAMPLE;
        } */
    //    flags |= CANDLE_MODE_HW_TIMESTAMP;
        return setDeviceMode(channel, CANDLE_DEVMODE_START, flags);
    }

    private long getDeviceTimestamp() {
        try {
            var transfer = new UsbControlTransfer(UsbRequestType.VENDOR, UsbRecipient.INTERFACE, CANDLE_TIMESTAMP_GET, 0, usbInterface.getNumber());
            byte[] buffer = device.controlTransferIn(transfer, 4);
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        } catch (UsbException e) {
            System.err.println("Failed to get device timestamp: " + e.getMessage());
            return 0;
        }
    }

    public boolean sendFrame(CandleFrame frame) {
        try {
            frame.echoId = 0; // Ensure echoId is set to 0
            frame.channel = 0; // Set the channel (adjust if needed)
            frame.reserved = 0; // Ensure reserved is set to 0
            byte[] frameData = frame.toByteArray();
            System.out.println("Sending frame: " + bytesToHex(frameData));
            System.out.println("Endpoint number: " + bulkOutEndpoint.getNumber());
            System.out.println("Frame size: " + frameData.length);
            device.transferOut(bulkOutEndpoint.getNumber(), frameData);
            // Since transferOut doesn't return the number of bytes transferred,
            // we assume success if no exception is thrown
            numTx++;
            return true;
        } catch (UsbException e) {
            System.err.println("Failed to send frame: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            numTxErr++;
            return false;
        }
    }
    /* 
    public boolean sendFrame(CandleFrame frame) {
        try {
            frame.echoId = 0; // Ensure echoId is set to 0
            byte[] frameData = frame.toByteArray();
            System.out.println("Sending frame: " + bytesToHex(frameData));
            System.out.println("Endpoint number: " + bulkOutEndpoint.getNumber());
            System.out.println("Frame size: " + frameData.length);
            device.transferOut(bulkOutEndpoint.getNumber(), frameData);
            numTx++;
            return true;
        } catch (UsbException e) {
            System.err.println("Failed to send frame: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            numTxErr++;
            return false;
        }
    }
*/
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    
    public List<CandleFrame> receiveFrames(int timeoutMs) {
        List<CandleFrame> frames = new ArrayList<>();
        try {
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                byte[] buffer = device.transferIn(bulkInEndpoint.getNumber(), 28); // Changed from 24 to 28 bytes
                
                if (buffer.length == 28) { // Changed from 24 to 28
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
                    CandleFrame frame = new CandleFrame();
                    frame.echoId = byteBuffer.getInt();
                    frame.canId = byteBuffer.getInt();
                    frame.canDlc = byteBuffer.get();
                    frame.channel = byteBuffer.get();
                    frame.flags = byteBuffer.get();
                    frame.reserved = byteBuffer.get();
                    frame.data = new byte[8];
                    byteBuffer.get(frame.data);
             //       frame.timestampUs = byteBuffer.getLong(); // Changed from getInt to getLong

                    // Adjust timestamp
           //         long dev_ts = frame.timestampUs - deviceTicksStart;
           //         frame.timestampUs = hostOffsetStart + dev_ts;

                    frames.add(frame);
                    numRx++;
                } else {
                    break; // No more complete frames available
                }
            }
        } catch (UsbException e) {
            System.err.println("Failed to receive frames: " + e.getMessage());
        }
        return frames;
    }

    public boolean stopChannel(int channel) {
        return setDeviceMode(channel, CANDLE_DEVMODE_RESET, 0);
    }

    public int getNumRxFrames() {
        return numRx;
    }

    public int getNumTxFrames() {
        return numTx;
    }

    public int getNumTxErrors() {
        return numTxErr;
    }
}