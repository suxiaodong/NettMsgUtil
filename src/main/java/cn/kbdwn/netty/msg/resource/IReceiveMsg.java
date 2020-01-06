package cn.kbdwn.netty.msg.resource;

@FunctionalInterface
public interface IReceiveMsg {

    Object receiveMsg(Object msg);

}
