// filepath: client/tftp-client/src/main/java/bgu/spl/net/impl/tftp/TftpClient.java
package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TftpClient {
    private static FileOutputStream fileOut = null;
    private static boolean isRRQ = false;
    private static boolean isDIRQ = false;
    private static boolean isWRQ = false;
    private static boolean waitingDiscAck = false;
    private static String rrqFilename = null;
    private static String wrqFilename = null;
    private static int wrqBlock = 0;             // next block to send after ACK
    private static FileInputStream wrqFileIn = null;

    private static short bytesToShort(byte hi, byte lo) {
        return (short)(((hi & 0xff) << 8) | (lo & 0xff));
    }
    private static byte[] shortToBytes(int v) {
        return new byte[]{ (byte)((v>>8)&0xff), (byte)(v&0xff) };
    }

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 7777;
        if (args.length > 0) host = args[0];
        if (args.length > 1) port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(host, port)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            TftpEncoderDecoder encdec = new TftpEncoderDecoder();

            Thread listener = new Thread(() -> {
                try {
                    int r;
                    while ((r = in.read()) >= 0) {
                        byte[] p = encdec.decodeNextByte((byte) r);
                        if (p == null) continue;

                        short op = bytesToShort(p[0], p[1]);
                        switch (op) {
                            case 4: { // ACK
                                int block = bytesToShort(p[2], p[3]);
                                System.out.println("ACK " + block);

                                // WRQ flow: after ACK N, send block N+1; for ACK 0 start transmission
                                if (isWRQ && wrqFileIn != null) {
                                    sendNextWrqBlock(out);
                                }

                                if (waitingDiscAck && block == 0) {
                                    System.out.println("Disconnected.");
                                    System.exit(0);
                                }
                                break;
                            }
                            case 5: { // ERROR
                                int code = bytesToShort(p[2], p[3]);
                                String msg = new String(p, 4, p.length - 5, StandardCharsets.UTF_8);
                                System.out.println("Error " + code + " " + msg);
                                // abort current mode on error
                                isRRQ = isWRQ = isDIRQ = false;
                                closeQuietly(fileOut);
                                closeQuietly(wrqFileIn);
                                wrqFileIn = null; wrqBlock = 0;
                                break;
                            }
                            case 9: { // BCAST
                                String action = (p[2] == 0) ? "del" : "add";
                                String fileName = new String(p, 3, p.length - 4, StandardCharsets.UTF_8);
                                System.out.println("BCAST " + action + " " + fileName);
                                break;
                            }
                            case 3: { // DATA
                                int size  = bytesToShort(p[2], p[3]);
                                int block = bytesToShort(p[4], p[5]);
                                byte[] data = new byte[size];
                                System.arraycopy(p, 6, data, 0, size);

                                if (isRRQ) {
                                    try {
                                        if (fileOut == null) {
                                            fileOut = new FileOutputStream(rrqFilename == null ? "downloaded_file" : rrqFilename);
                                        }
                                        fileOut.write(data);
                                    } catch (IOException e) {
                                        System.out.println("Failed to save file.");
                                        isRRQ = false;
                                    }
                                } else if (isDIRQ) {
                                    String dirList = new String(data, StandardCharsets.UTF_8);
                                    for (String f : dirList.split("\0")) {
                                        if (!f.isEmpty()) System.out.println(f);
                                    }
                                } else {
                                    // unknown context; still ACK per spec to allow server progress
                                }

                                // ACK the DATA block
                                out.write(new byte[]{0,4, shortToBytes(block)[0], shortToBytes(block)[1]});
                                out.flush();

                                // if last block => finalize
                                if (size < 512) {
                                    if (isRRQ) {
                                        closeQuietly(fileOut);
                                        fileOut = null; rrqFilename = null; isRRQ = false;
                                        System.out.println("File download complete.");
                                    }
                                    if (isDIRQ) {
                                        isDIRQ = false;
                                        System.out.println("End of directory listing.");
                                    }
                                }
                                break;
                            }
                            default:
                                // other packets are handled elsewhere
                                break;
                        }
                    }
                } catch (IOException e) {
                    // socket closed or error
                }
            });
            listener.setDaemon(true);
            listener.start();

            // Keyboard loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // handle client-side validations and send
                byte[] packet = buildOutgoing(line);
                if (packet != null) {
                    out.write(packet);
                    out.flush();
                }

                if (line.equalsIgnoreCase("DISC")) {
                    waitingDiscAck = true; // wait for ACK 0 from server
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] buildOutgoing(String line) {
        String[] parts = line.trim().split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String arg = (parts.length > 1) ? parts[1] : null;

        switch (cmd) {
            case "LOGRQ":
                if (arg == null) return null;
                return stringCmd((short)7, arg);
            case "DELRQ":
                if (arg == null) return null;
                return stringCmd((short)8, arg);
            case "RRQ":
                if (arg == null) return null;
                // precheck: local file must NOT exist
                if (new File(arg).exists()) {
                    System.out.println("file already exists");
                    return null;
                }
                isRRQ = true; isDIRQ = false; isWRQ = false;
                rrqFilename = arg;
                return stringCmd((short)1, arg);
            case "WRQ":
                if (arg == null) return null;
                // precheck: local file MUST exist
                if (!new File(arg).exists()) {
                    System.out.println("file does not exists");
                    return null;
                }
                isWRQ = true; isRRQ = false; isDIRQ = false;
                wrqFilename = arg;
                wrqBlock = 0; // expect ACK 0 then send block 1
                try {
                    wrqFileIn = new FileInputStream(wrqFilename);
                } catch (FileNotFoundException e) {
                    System.out.println("File not found for upload: " + wrqFilename);
                    isWRQ = false;
                    return null;
                }
                return stringCmd((short)2, arg);
            case "DIRQ":
                isDIRQ = true; isRRQ = false; isWRQ = false;
                return new byte[]{0,6};
            case "DISC":
                return new byte[]{0,10};
            default:
                System.out.println("Unknown command");
                return null;
        }
    }

    private static void sendNextWrqBlock(OutputStream out) throws IOException {
        byte[] buf = new byte[512];
        int n = wrqFileIn.read(buf);
        if (n < 0) n = 0;
        wrqBlock++;
        ByteArrayOutputStream packet = new ByteArrayOutputStream(6 + n);
        packet.write(0); packet.write(3);
        packet.write(shortToBytes(n));
        packet.write(shortToBytes(wrqBlock));
        if (n > 0) packet.write(buf, 0, n);
        out.write(packet.toByteArray());
        out.flush();

        if (n < 512) {
            // finished sending
            closeQuietly(wrqFileIn);
            wrqFileIn = null; isWRQ = false; wrqBlock = 0;
            System.out.println("File upload complete.");
        }
    }

    private static byte[] stringCmd(short opcode, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[2 + b.length + 1];
        out[0] = (byte)(opcode >> 8); out[1] = (byte)(opcode & 0xff);
        System.arraycopy(b, 0, out, 2, b.length);
        out[out.length - 1] = 0;
        return out;
    }

    private static void closeQuietly(Closeable c) {
        try { if (c != null) c.close(); } catch (IOException ignore) {}
    }
}
