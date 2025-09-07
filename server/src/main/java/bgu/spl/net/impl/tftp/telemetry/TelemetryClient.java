package bgu.spl.net.impl.tftp.telemetry;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

public class TelemetryClient {
    private final InetAddress host;
    private final int port;
    private final DatagramSocket socket;

    public TelemetryClient(String host, int port) {
        try {
            this.host = InetAddress.getByName(host);
            this.port = port;
            this.socket = new DatagramSocket();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void sendJson(String json) {
        try {
            byte[] b = json.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(b, b.length, host, port);
            socket.send(p);
        } catch (Exception ignore) {}
    }

    private static String esc(String s){
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }

    private String makeEvent(String type, String user, String file, String msg, Map<String,Object> extra) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"ts\":").append(Instant.now().toEpochMilli())
          .append(",\"event\":\"").append(esc(type)).append("\"")
          .append(",\"user\":\"").append(esc(user)).append("\"")
          .append(",\"file\":\"").append(esc(file)).append("\"");
        if (msg != null) sb.append(",\"msg\":\"").append(esc(msg)).append("\"");
        if (extra != null) {
            for (var e : extra.entrySet()) {
                sb.append(",\"").append(esc(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v == null) sb.append("null");
                else if (v instanceof Number || v instanceof Boolean) sb.append(v);
                else sb.append("\"").append(esc(v.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void log(String type, String user, String file, String msg, Map<String,Object> extra) {
        sendJson(makeEvent(type, user, file, msg, extra));
    }

    // convenience
    public void wrqComplete(String user, String file, long size){ log("FILE_ADD", user, file, "WRQ complete", Map.of("size",size)); }
    public void delComplete(String user, String file){ log("FILE_DELETE", user, file, "DELRQ complete", null); }
    public void rrqComplete(String user, String file){ log("FILE_ACCESS", user, file, "RRQ complete", null); }
    public void ack(String user, String file, int block){ log("ACK", user, file, null, Map.of("block",block)); }
    public void error(String user, String file, int code, String emsg){ log("ERROR", user, file, emsg, Map.of("code",code)); }
    public void bcastAdd(String file){ log("BCAST_ADD", "", file, null, null); }
    public void bcastDel(String file){ log("BCAST_DEL", "", file, null, null); }
}
