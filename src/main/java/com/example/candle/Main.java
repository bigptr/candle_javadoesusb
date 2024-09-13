package com.example.candle;

import java.util.List;

import net.codecrete.usb.Usb;

public class Main {
    public static void main(String[] args) {
        var testDevice = Usb.findDevice(0x1D50, 0x606F); // Replace with your VID and PID
        if (testDevice.isPresent()) {
            System.out.println("Device found: " + testDevice.get());
            Candle candle = new Candle(testDevice.get());

            if (candle.open()) {
                // ... existing code for setting host format, bitrate, etc. ...
                if (candle.setHostFormat(0)) {
                    System.out.println("Host format set successfully");
                } else {
                    System.err.println("Failed to set host format");
                }

           // Set bitrate to 500 kbps before getting config
           if (candle.setBitrate(0, 500000)) {
            System.out.println("Bitrate set to 500 kbps successfully");
        } else {
            System.err.println("Failed to set bitrate");
        }
                // Set modes before starting the channel
                candle.setListenOnlyMode(false);
                candle.setOneShotMode(false);
                candle.setTripleSampling(false);

                CandleCapability capability = candle.getCapability(0);
                if (capability != null) {
                    System.out.println("Device capability retrieved successfully");
                    System.out.println("Feature: " + capability.feature);
                    System.out.println("Fclk CAN: " + capability.fclkCan);
                    System.out.println("Tseg1 Min: " + capability.tseg1Min);
                    System.out.println("Tseg1 Max: " + capability.tseg1Max);
                    System.out.println("Tseg2 Min: " + capability.tseg2Min);
                    System.out.println("Tseg2 Max: " + capability.tseg2Max);
                    System.out.println("SJW Max: " + capability.sjwMax);
                    System.out.println("BRP Min: " + capability.brpMin);
                    System.out.println("BRP Max: " + capability.brpMax);
                    System.out.println("BRP Inc: " + capability.brpInc);
                } else {
                    System.err.println("Failed to retrieve device capability");
                }
                // Start the channel
                if (candle.startChannel(0, 0)) {
                    System.out.println("Channel started successfully");
                } else {
                    System.err.println("Failed to start channel");
                }

                // Send multiple frames
for (int i = 0; i < 5; i++) {
    CandleFrame frameToSend = new CandleFrame();
    frameToSend.echoId = 0;
    frameToSend.flags = 0;
    frameToSend.reserved = 0;
    frameToSend.canId = 0x123 + i;
    frameToSend.canDlc = 8;
    frameToSend.channel = 0;
    frameToSend.data = new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) (0x04 + i)};
    if (candle.sendFrame(frameToSend)) {
        System.out.println("Frame " + i + " sent successfully");
    } else {
        System.err.println("Failed to send frame " + i);
    }
    try {
        Thread.sleep(100); // Wait 100ms between sends
    } catch (Exception e) {
        // TODO: handle exception
        e.printStackTrace();
    }

}

// Try to receive frames multiple times
for (int i = 0; i < 5; i++) {
    List<CandleFrame> receivedFrames = candle.receiveFrames(1000); // 1000 ms timeout
    for (CandleFrame receivedFrame : receivedFrames) {
        System.out.println("Received frame: CAN ID: 0x" + Integer.toHexString(receivedFrame.canId) + ", DLC: " + receivedFrame.canDlc + ", Data: " + bytesToHex(receivedFrame.data) );
    }
    if (receivedFrames.isEmpty()) {
        System.out.println("No frames received in attempt " + i);
    }
    try {
        Thread.sleep(100); // Wait 100ms between receive attempts
    } catch (Exception e) {
        // TODO: handle exception
        e.printStackTrace();
    }
}
                // ... existing code for sending frames ...

                // Example: Receive frames
                List<CandleFrame> receivedFrames = candle.receiveFrames(1000); // 1000 ms timeout
                for (CandleFrame receivedFrame : receivedFrames) {
                    System.out.println("Received frame: CAN ID: 0x" + Integer.toHexString(receivedFrame.canId) + ", DLC: " + receivedFrame.canDlc + ", Data: " + bytesToHex(receivedFrame.data) );
                }

                // Print statistics
                System.out.println("Frames received: " + candle.getNumRxFrames());
                System.out.println("Frames sent: " + candle.getNumTxFrames());
                System.out.println("Transmission errors: " + candle.getNumTxErrors());

                // ... existing code for stopping the channel and closing the device ...
            } else {
                System.err.println("Failed to open device");
            }
        } else {
            System.out.println("Device not found");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}