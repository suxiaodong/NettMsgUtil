package cn.kbdwn.netty.msg.message;

import lombok.Data;

/**
 * 主要区别消息类型和消息id
 */
@Data
public class Message {

    private MsgTypeEnum mask; //消息类型

    private String msgId; //消息id

    private Object content; //内容
}
