package cn.kbdwn.netty.msg.message;

public enum MsgTypeEnum {

    MGS_TYPE_PING("ping"),
    MGS_TYPE_PONG("pong"),
    MGS_TYPE_LOGIN_REQUEST("发起登陆"),
    MGS_TYPE_LOGIN_RESPONSE("登陆响应"),
    MSG_TYPE_REQUEST("请求"),
    MSG_TYPE_REQUEST_SYNC("同步请求"),
    MSG_TYPE_REQUEST_RPC("rpc请求"),
    MSG_TYPE_RESPONSE("响应");

    private String desc;

    private MsgTypeEnum(String desc) {
        this.desc = desc;
    }
}