// filepath: client/tftp-client/src/main/java/bgu/spl/net/impl/tftp/TftpEncoderDecoder.java
package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.util.Arrays;

/**
 * Minimal decoder for the client:
 *  - DATA  (3):  2(op) + 2(size) + 2(block) + <size> bytes
 *  - ACK   (4):  2(op) + 2(block)
 *  - ERROR (5):  2(op) + 2(code) + <msg> + 0
 *  - BCAST (9):  2(op) + 1(flag) + <filename> + 0
 * For encode(), the client builds packets manually, so we passthrough.
 */
public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private byte[] buf = new byte[1024];
    private int len = 0;
    private int expected = -1; // -1 = unknown/variable length

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        push(nextByte);

        // We need at least two bytes to know opcode
        if (len >= 2) {
            int op = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);

            switch (op) {
                case 3: // DATA: fixed once we read size
                    if (len >= 4 && expected < 0) {
                        int size = ((buf[2] & 0xff) << 8) | (buf[3] & 0xff);
                        expected = 6 + size;
                    }
                    if (expected >= 0 && len >= expected) {
                        return pop();
                    }
                    break;

                case 4: // ACK: always 4 bytes
                    expected = 4;
                    if (len >= expected) {
                        return pop();
                    }
                    break;

                case 5: // ERROR: ends with trailing 0 after index >= 4
                    if (len >= 5 && buf[len - 1] == 0) {
                        return pop();
                    }
                    break;

                case 9: // BCAST: ends with trailing 0 after index >= 3
                    if (len >= 4 && buf[len - 1] == 0) {
                        return pop();
                    }
                    break;

                default:
                    // Not expected from server, but if it has a trailing 0 (string-cmd style) accept it.
                    if (len >= 3 && buf[len - 1] == 0) {
                        return pop();
                    }
                    break;
            }
        }

        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        // Client builds its own packets; just pass through.
        return message;
    }

    private void push(byte b) {
        if (len >= buf.length) buf = Arrays.copyOf(buf, buf.length * 2);
        buf[len++] = b;
    }

    private byte[] pop() {
        byte[] out = Arrays.copyOf(buf, len);
        len = 0;
        expected = -1;
        return out;
    }
}
