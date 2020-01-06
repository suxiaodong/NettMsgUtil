package cn.kbdwn.netty.msg.message;

import lombok.Data;

/**
 * 响应内容
 */
@Data
public class ResponseContent{

    private Object data; //响应数据

    private Boolean isSuccess=true; //成功

    private Integer code; //错误码

}
