package cn.kbdwn.netty.msg.message;

import lombok.Data;

/**
 * 消息内容
 */
@Data
public class LoginResponseContent extends ResponseContent{

    private String clientMask; //客户端标识,登陆的时候必须

}
