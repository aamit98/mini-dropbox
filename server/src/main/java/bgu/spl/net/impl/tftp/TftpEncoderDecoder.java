// filepath: server/src/main/java/bgu/spl/net/impl/tftp/TftpEncoderDecoder.java
package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.ByteArrayOutputStream;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private final ByteArrayOutputStream frame = new ByteArrayOutputStream();
    private short opcode = -1;
    private int state = 0;     // 0=opcode(2), 1=STR+\0, 2=ERR code(2)->msg+\0, 3=DATA hdr(4)->payload, 4=ACK block(2), 5=BCAST flag(1)->name+\0
    private int need = 2;      // bytes still needed in current step

    @Override
    public byte[] decodeNextByte(byte next) {
        frame.write(next);

        if (state == 0) {
            if (--need == 0) { // read 2 bytes opcode
                byte[] b = frame.toByteArray();
                opcode = (short) (((b[0] & 0xff) << 8) | (b[1] & 0xff));
                switch (opcode) {
                    case 1: case 2: case 7: case 8:   // <str>\0
                        state = 1; need = Integer.MAX_VALUE; break;
                    case 6: case 10:                  // just opcode
                        return take();
                    case 4:                           // ACK: + block(2)
                        state = 4; need = 2; break;
                    case 5:                           // ERROR: + code(2) then msg+\0
                        state = 2; need = 2; break;
                    case 3:                           // DATA: + size(2)+block(2) then payload[size]
                        state = 3; need = 4; break;
                    case 9:                           // BCAST: + flag(1) then name+\0
                        state = 5; need = 1; break;
                    default:
                        return take(); // let protocol decide
                }
            }
            return null;
        }

        switch (state) {
            case 1: // STR+\0
                if (next == 0) return take();
                return null;
            case 2: // ERROR: code(2) -> msg+\0
                if (--need == 0) { state = 1; need = Integer.MAX_VALUE; }
                return null;
            case 3: // DATA header then payload
                if (--need == 0) {
                    byte[] a = frame.toByteArray();
                    int size = ((a[2] & 0xff) << 8) | (a[3] & 0xff);
                    need = size;
                    state = 33; // payload
                }
                return null;
            case 33: // DATA payload[size]
                if (--need == 0) return take();
                return null;
            case 4: // ACK block(2)
                if (--need == 0) return take();
                return null;
            case 5: // BCAST flag(1) then name+\0
                if (--need == 0) { state = 1; need = Integer.MAX_VALUE; }
                return null;
            default:
                return null;
        }
    }

    @Override
    public byte[] encode(byte[] message) {
        return message; // frames are already complete
    }

    private byte[] take() {
        byte[] out = frame.toByteArray();
        frame.reset();
        opcode = -1; state = 0; need = 2;
        return out;
    }
}
