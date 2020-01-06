package cn.kbdwn.netty.msg.server;

import cn.kbdwn.netty.msg.resource.IReceiveMsg;
import cn.kbdwn.netty.msg.security.ICertificationService;
import cn.kbdwn.netty.msg.security.ITransmissionSecurity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfigTemplate {

    private ITransmissionSecurity transmissionSecurity;
    private int port; //端口
    private IReceiveMsg iReceiveMsg; //消息接收
    //private Object localService; //提供服务的接口
    private Object localService;
    private ICertificationService iCertificationService; //设备登陆认证
    @Builder.Default
    private long readTimeout=5000;  //读取超时时间

}
