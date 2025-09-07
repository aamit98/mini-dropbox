package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TftpClient {

    private static FileOutputStream fileOut = null;
    private static boolean isRRQ = false;
    private static boolean isDIRQ = false;
    private static boolean isWRQ = false;
    private static String rrqFilename = null;
    private static String wrqFilename = null;
    private static int wrqBlock = 1;
    private static FileInputStream wrqFileIn = null;

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 7777;
        if (args.length > 0) host = args[0];
        if (args.length > 1) port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(host, port)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            TftpEncoderDecoder encdec = new TftpEncoderDecoder();

            // Listening thread
            Thread listener = new Thread(() -> {
                try {
                    int read;
                    while ((read = in.read()) >= 0) {
                        byte[] packet = encdec.decodeNextByte((byte) read);
                        if (packet != null) {
                            switch (packet[1]) {
                                case 4: // ACK
                                    System.out.println("ACK " + packet[3]);
                                    // WRQ: send next DATA block after ACK
                                    if (isWRQ && wrqFileIn != null) {
                                        try {
                                            byte[] buffer = new byte[512];
                                            int bytesRead = wrqFileIn.read(buffer);
                                            if (bytesRead > 0) {
                                                byte[] dataPacket = new byte[6 + bytesRead];
                                                dataPacket[0] = 0;
                                                dataPacket[1] = 3;
                                                dataPacket[2] = (byte)(bytesRead >> 8);
                                                dataPacket[3] = (byte)(bytesRead & 0xff);
                                                dataPacket[4] = (byte)(wrqBlock >> 8);
                                                dataPacket[5] = (byte)(wrqBlock & 0xff);
                                                System.arraycopy(buffer, 0, dataPacket, 6, bytesRead);
                                                out.write(dataPacket);
                                                out.flush();
                                                wrqBlock++;
                                            }
                                            if (bytesRead < 512) {
                                                wrqFileIn.close();
                                                wrqFileIn = null;
                                                isWRQ = false;
                                                wrqBlock = 1;
                                                System.out.println("File upload complete.");
                                            }
                                        } catch (IOException e) {
                                            System.out.println("Failed to upload file: " + wrqFilename);
                                            isWRQ = false;
                                        }
                                    }
                                    break;
                                case 5: // ERROR
                                    int errorCode = packet[3];
                                    String errorMsg = new String(packet, 4, packet.length - 5);
                                    System.out.println("Error " + errorCode + " " + errorMsg);
                                    break;
                                case 9: // BCAST
                                    String action = (packet[2] == 0) ? "del" : "add";
                                    String fileName = new String(packet, 3, packet.length - 4);
                                    System.out.println("BCAST " + action + " " + fileName);
                                    break;
                                case 3: // DATA
                                    short dataSize = (short)(((packet[2] & 0xff) << 8) | (packet[3] & 0xff));
                                    short blockNum = (short)(((packet[4] & 0xff) << 8) | (packet[5] & 0xff));
                                    byte[] data = new byte[dataSize];
                                    System.arraycopy(packet, 6, data, 0, dataSize);

                                    if (isRRQ) {
                                        try {
                                            if (fileOut == null) {
                                                fileOut = new FileOutputStream(rrqFilename == null ? "downloaded_file" : rrqFilename);
                                            }
                                            fileOut.write(data);
                                            if (dataSize < 512) {
                                                fileOut.close();
                                                fileOut = null;
                                                isRRQ = false;
                                                rrqFilename = null;
                                                System.out.println("File download complete.");
                                            }
                                        } catch (IOException e) {
                                            System.out.println("Failed to save file.");
                                            isRRQ = false;
                                        }
                                    } else if (isDIRQ) {
                                        String dirList = new String(data);
                                        String[] files = dirList.split("\0");
                                        System.out.println("Directory listing:");
                                        for (String f : files) {
                                            if (!f.isEmpty())
                                                System.out.println("  " + f);
                                        }
                                        if (dataSize < 512) {
                                            isDIRQ = false;
                                            System.out.println("End of directory listing.");
                                        }
                                    } else {
                                        System.out.println("Received DATA packet (unknown context)");
                                    }
                                    break;
                                default:
                                    System.out.println("Received packet with opcode " + packet[1]);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listener.start();

            // Keyboard thread
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                byte[] packet = parseCommand(line, encdec);
                if (packet != null) {
                    out.write(packet);
                    out.flush();
                }
                if (line.startsWith("DISC")) break;
            }
            listener.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] parseCommand(String line, TftpEncoderDecoder encdec) {
        String[] parts = line.trim().split(" ", 2);
        String cmd = parts[0].toUpperCase();

        isRRQ = false;
        isDIRQ = false;
        isWRQ = false;
        rrqFilename = null;
        wrqFilename = null;

        switch (cmd) {
            case "LOGRQ":
                if (parts.length < 2) return null;
                return encodeStringCommand((short)7, parts[1]);
            case "DELRQ":
                if (parts.length < 2) return null;
                return encodeStringCommand((short)8, parts[1]);
            case "RRQ":
                if (parts.length < 2) return null;
                isRRQ = true;
                rrqFilename = parts[1];
                return encodeStringCommand((short)1, parts[1]);
            case "WRQ":
                if (parts.length < 2) return null;
                isWRQ = true;
                wrqFilename = parts[1];
                try {
                    wrqFileIn = new FileInputStream(wrqFilename);
                } catch (FileNotFoundException e) {
                    System.out.println("File not found for upload: " + wrqFilename);
                    isWRQ = false;
                    return null;
                }
                wrqBlock = 1;
                return encodeStringCommand((short)2, parts[1]);
            case "DIRQ":
                isDIRQ = true;
                return new byte[]{0, 6};
            case "DISC":
                return new byte[]{0, 10};
            default:
                System.out.println("Unknown command");
                return null;
        }
    }

    private static byte[] encodeStringCommand(short opcode, String arg) {
        byte[] op = new byte[]{(byte)(opcode >> 8), (byte)(opcode & 0xff)};
        byte[] str = arg.getBytes();
        byte[] msg = new byte[op.length + str.length + 1];
        System.arraycopy(op, 0, msg, 0, op.length);
        System.arraycopy(str, 0, msg, op.length, str.length);
        msg[msg.length - 1] = 0; // null terminator
        return msg;
    }
}