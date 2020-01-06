package cn.kbdwn.netty.msg.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RpcRequestContent {

    private String className; //类名

    private String methodName; //方法名

    private Object[] args; //方法参数
}
