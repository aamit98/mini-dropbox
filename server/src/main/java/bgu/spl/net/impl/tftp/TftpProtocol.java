package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.tftp.telemetry.TelemetryClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class ConnectionState {
    static final ConcurrentHashMap<Integer, String> userByConn = new ConcurrentHashMap<>();
    static final Set<String> uploadingNow = Collections.synchronizedSet(new HashSet<>()); // names only (per user handled by folder)
    static final ConcurrentHashMap<Integer, String> currentUpload = new ConcurrentHashMap<>(); // connId -> file name
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    // -------- configuration (shared with REST) --------
    private static final Path BASE = Paths.get(System.getProperty("storage.base-dir", "server/Files"))
            .toAbsolutePath().normalize();

    private static final TelemetryClient TELE = new TelemetryClient(
            System.getProperty("telemetry.host", "127.0.0.1"),
            Integer.getInteger("telemetry.port", 9099)
    );

    // -------- instance state --------
    private Connections<byte[]> conns;
    private int id;
    private boolean terminate = false;

    private FileInputStream rrqIn = null;
    private int sendBlock = 0;
    private List<String> dirNames = null;
    private int dirCursor = 0;

    // -------- helpers --------
    private static short bytesToShort(byte hi, byte lo) {
        return (short) (((hi & 0xff) << 8) | (lo & 0xff));
    }
    private static byte[] shortToBytes(int v) {
        return new byte[]{(byte) ((v >> 8) & 0xff), (byte) (v & 0xff)};
    }
    private static byte[] op(short op) { return new byte[]{(byte) (op >> 8), (byte) (op & 0xff)}; }
    private static void safeClose(Closeable c){ try { if (c != null) c.close(); } catch (IOException ignore) {} }

    private boolean isLoggedIn() { return ConnectionState.userByConn.containsKey(id); }

    /** Per-user folder inside BASE, created if missing */
    private Path userDir() {
        String u = ConnectionState.userByConn.get(id);
        if (u == null || u.isBlank()) return null;
        try {
            Path p = BASE.resolve(u).normalize();
            Files.createDirectories(p);
            if (!p.startsWith(BASE)) throw new SecurityException("bad user dir");
            return p;
        } catch (Exception e){ return null; }
    }

    private String readZString(byte[] msg, int from) {
        int i = from;
        while (i < msg.length && msg[i] != 0) i++;
        if (i >= msg.length) return null;
        return new String(msg, from, i - from, StandardCharsets.UTF_8);
    }

    // -------- protocol lifecycle --------
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.id = connectionId;
        this.conns = connections;
        this.terminate = false;
    }

    @Override
    public void process(byte[] msg) {
        if (msg.length < 2) return;
        short opcode = bytesToShort(msg[0], msg[1]);
        switch (opcode) {
            case 7:  handleLOGRQ(msg); break;
            case 6:  handleDIRQ(); break;
            case 1:  handleRRQ(msg); break;
            case 2:  handleWRQ(msg); break;
            case 3:  handleDATA(msg); break;
            case 4:  handleACK(msg); break;
            case 8:  handleDELRQ(msg); break;
            case 10: handleDISC(); break;
            default: sendERROR(4, "Illegal TFTP operation – Unknown Opcode.");
        }
    }

    @Override
    public boolean shouldTerminate() { return terminate; }

    // -------- handlers --------
    private void handleLOGRQ(byte[] msg) {
        String user = readZString(msg, 2);
        if (user == null || user.isEmpty()) { sendERROR(0, "Bad username"); return; }
        if (isLoggedIn() || ConnectionState.userByConn.containsValue(user)) {
            sendERROR(7, "User already logged in – Login username already connected.");
            return;
        }
        ConnectionState.userByConn.put(id, user);
        try { Files.createDirectories(BASE.resolve(user)); } catch (Exception ignore) {}
        sendACK(0);
        TELE.log("LOGIN", user, null, null, null);
    }

    private void handleDIRQ() {
        if (!isLoggedIn()) { sendERROR(6, "Not logged in – operation requires login."); return; }
        Path dir = userDir();
        if (dir == null) { sendERROR(6, "Not logged in"); return; }

        File[] files = dir.toFile().listFiles();
        if (files == null) files = new File[0];

        List<String> names = new ArrayList<>();
        synchronized (ConnectionState.uploadingNow) {
            for (File f : files) {
                if (f.isFile() && !ConnectionState.uploadingNow.contains(f.getName())) {
                    names.add(f.getName());
                }
            }
        }
        dirNames = names;
        dirCursor = 0;
        sendBlock = 0;
        sendNextDirqChunk();
    }

    private void handleRRQ(byte[] msg) {
        if (!isLoggedIn()) { sendERROR(6, "Not logged in – operation requires login."); return; }
        String name = readZString(msg, 2);
        Path dir = userDir();
        if (dir == null) { sendERROR(6, "Not logged in"); return; }
        File f = dir.resolve(name).toFile();
        if (!f.exists() || !f.isFile()) { sendERROR(1, "File not found"); return; }
        try {
            rrqIn = new FileInputStream(f);
            sendBlock = 0;
            sendNextRrqChunk(); // will start DATA streaming
        } catch (IOException e) {
            sendERROR(2, "Access violation / open failed");
        }
    }

    private void handleWRQ(byte[] msg) {
        if (!isLoggedIn()) { sendERROR(6, "Not logged in – operation requires login."); return; }
        String name = readZString(msg, 2);
        Path dir = userDir();
        if (dir == null) { sendERROR(6, "Not logged in"); return; }
        File f = dir.resolve(name).toFile();
        synchronized (ConnectionState.uploadingNow) {
            if (f.exists() || ConnectionState.uploadingNow.contains(name)) {
                sendERROR(5, "File already exists – File name exists on WRQ.");
                return;
            }
            ConnectionState.uploadingNow.add(name);
            ConnectionState.currentUpload.put(id, name);
        }
        sendACK(0); // client should start sending DATA 1..N
    }

    private void handleDATA(byte[] msg) {
        int size  = bytesToShort(msg[2], msg[3]) & 0xffff;
        int block = bytesToShort(msg[4], msg[5]) & 0xffff;
        byte[] data = new byte[size];
        if (size > 0) System.arraycopy(msg, 6, data, 0, size);

        if (!isLoggedIn()) { sendERROR(6, "Not logged in"); return; }

        String fname = ConnectionState.currentUpload.get(id);
        if (fname == null) { sendERROR(2, "Unexpected DATA – no WRQ in progress"); return; }

        Path dir = userDir();
        if (dir == null) { sendERROR(6, "Not logged in"); return; }
        File target = dir.resolve(fname).toFile();

        try (FileOutputStream out = new FileOutputStream(target, true)) {
            if (size > 0) out.write(data);
        } catch (IOException e) {
            sendERROR(2, "Write failed");
            return;
        }

        sendACK(block);

        if (size < 512) {
            ConnectionState.uploadingNow.remove(fname);
            ConnectionState.currentUpload.remove(id);
            String user = ConnectionState.userByConn.get(id);
            TELE.wrqComplete(user, fname, target.length());
            bcast((byte)1, fname); // add
        }
    }

    private void handleACK(byte[] msg) {
        if (rrqIn != null) {
            sendNextRrqChunk();
        } else if (dirNames != null) {
            sendNextDirqChunk();
        }
    }

    private void handleDELRQ(byte[] msg) {
        if (!isLoggedIn()) { sendERROR(6, "Not logged in – operation requires login."); return; }
        String name = readZString(msg, 2);
        synchronized (ConnectionState.uploadingNow) {
            if (ConnectionState.uploadingNow.contains(name)) {
                sendERROR(2, "Access violation – file is uploading");
                return;
            }
        }
        Path dir = userDir();
        if (dir == null) { sendERROR(6, "Not logged in"); return; }
        File f = dir.resolve(name).toFile();
        if (!f.exists() || !f.isFile()) { sendERROR(1, "File not found – DELRQ of non-existing file."); return; }
        if (!f.delete()) { sendERROR(2, "Delete failed"); return; }
        sendACK(0);
        String user = ConnectionState.userByConn.get(id);
        TELE.delComplete(user, name);
        bcast((byte)0, name); // del
    }

    private void handleDISC() {
        sendACK(0);
        terminate = true;

        String fname = ConnectionState.currentUpload.remove(id);
        if (fname != null) {
            ConnectionState.uploadingNow.remove(fname);
        }

        conns.disconnect(id);
        String u = ConnectionState.userByConn.remove(id);
        TELE.log("DISC", u, null, null, null);
    }

    // -------- streaming helpers --------
    private void sendNextRrqChunk() {
        try {
            byte[] buf = new byte[512];
            int n = (rrqIn != null) ? rrqIn.read(buf) : -1;
            if (n < 0) n = 0;
            sendBlock++;

            ByteArrayOutputStream out = new ByteArrayOutputStream(6 + n);
            byte[] op = op((short)3);
            out.write(op, 0, op.length);
            byte[] sz = shortToBytes(n);
            out.write(sz, 0, sz.length);
            byte[] blk = shortToBytes(sendBlock);
            out.write(blk, 0, blk.length);
            if (n > 0) out.write(buf, 0, n);

            conns.send(id, out.toByteArray());

            if (n < 512) {
                safeClose(rrqIn); rrqIn = null; sendBlock = 0;
                String user = ConnectionState.userByConn.get(id);
                TELE.rrqComplete(user, null); // file name not tracked here; optional to add if you track it
            }
        } catch (IOException e) {
            sendERROR(2, "Read failed");
            safeClose(rrqIn);
            rrqIn = null; sendBlock = 0;
        }
    }

    private void sendNextDirqChunk() {
        if (dirNames == null) return;

        ByteArrayOutputStream payload = new ByteArrayOutputStream(512);

        while (dirCursor < dirNames.size()) {
            byte[] name = dirNames.get(dirCursor).getBytes(StandardCharsets.UTF_8);
            if (payload.size() + name.length + 1 > 512) break; // +1 for '\0'
            payload.write(name, 0, name.length);
            payload.write(0);
            dirCursor++;
        }

        sendBlock++;
        byte[] data = payload.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream(6 + data.length);
        byte[] op = op((short)3);
        out.write(op, 0, op.length);
        byte[] sz = shortToBytes(data.length);
        out.write(sz, 0, sz.length);
        byte[] blk = shortToBytes(sendBlock);
        out.write(blk, 0, blk.length);
        if (data.length > 0) out.write(data, 0, data.length);

        conns.send(id, out.toByteArray());

        if (data.length < 512) {
            dirNames = null;
            dirCursor = 0;
            sendBlock = 0;
        }
    }

    // -------- wire helpers --------
    private void sendACK(int block) {
        byte[] b = shortToBytes(block);
        conns.send(id, new byte[]{0,4, b[0], b[1]});
        String u = ConnectionState.userByConn.get(id);
        if (u != null) TELE.ack(u, null, block);
    }

    private void sendERROR(int code, String msg) {
        byte[] m = msg.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[2 + 2 + m.length + 1];
        out[0]=0; out[1]=5;
        out[2]=0; out[3]=(byte)code;
        System.arraycopy(m, 0, out, 4, m.length);
        out[out.length-1]=0;
        conns.send(id, out);
        String u = ConnectionState.userByConn.get(id);
        TELE.error(u, null, code, msg);
    }

    private void bcast(byte flag, String name) {
        byte[] b = name.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[2 + 1 + b.length + 1];
        out[0]=0; out[1]=9;
        out[2]=flag;
        System.arraycopy(b, 0, out, 3, b.length);
        out[out.length-1]=0;

        for (int connId : new ArrayList<>(ConnectionState.userByConn.keySet())) {
            conns.send(connId, out);
        }
        TELE.log(flag==1 ? "BCAST_ADD" : "BCAST_DEL", "", name, null, null);
    }
}
