package cn.kbdwn.netty.msg.server;

public interface IClientEventListener {
    void event(String clientId,String eventName,Object eventValue);
}
