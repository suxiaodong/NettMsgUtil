package cn.kbdwn.netty.msg.client;

import cn.kbdwn.netty.msg.resource.IReceiveMsg;
import cn.kbdwn.netty.msg.security.ITransmissionSecurity;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigTemplate {

    private ITransmissionSecurity transmissionSecurity;
    private String serverAddress; //服务端IP
    private int port; //端口
    private String clientId; //客户端Id
    private String certificationInfo; //认证信息
    private IReceiveMsg iReceiveMsg; //消息接收实现类
    private Object localService;
    @Builder.Default
    private long readTimeout=5000;

}
