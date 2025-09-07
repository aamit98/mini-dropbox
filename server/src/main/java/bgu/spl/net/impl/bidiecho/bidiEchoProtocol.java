package bgu.spl.net.impl.bidiecho;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;


 class holder{
    static ConcurrentHashMap<Integer,Boolean> ids_login=new ConcurrentHashMap<>();

}

public class bidiEchoProtocol implements BidiMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections;


    @Override
    public void start(int connectionId, Connections<String> connections) {
          this.shouldTerminate=false;
          this.connectionId=connectionId;
          this.connections=connections;   // initialization of everything ofcourse yes !
          holder.ids_login.put(connectionId,true);


    }

    @Override
    public void process(String message) {


        shouldTerminate="bye".equals(message);
        System.out.println("[" + LocalDateTime.now() + "]: " + message);
        String echo=createEcho(message);
        for(Integer id : holder.ids_login.keySet()){
            connections.send(id,echo);

        }

    }

    private String createEcho(String message) {
        String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
        return message + " .. " + echoPart + " .. " + echoPart + " ..";
    }

    @Override
    public boolean shouldTerminate() {
        this.connections.disconnect(this.connectionId);
        holder.ids_login.remove(this.connectionId);
        return shouldTerminate;
    }

}