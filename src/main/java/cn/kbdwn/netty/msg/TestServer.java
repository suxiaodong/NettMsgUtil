package cn.kbdwn.netty.msg;

import cn.kbdwn.netty.msg.security.DESTransmissionSecurity;
import cn.kbdwn.netty.msg.server.ServerConfigTemplate;
import cn.kbdwn.netty.msg.server.SimpleServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServer {

    private static final Logger LOGGER= LoggerFactory.getLogger(TestServer.class);

    public static void main(String[] args) {
        SimpleServer simpleServer = new SimpleServer();
        simpleServer.init(ServerConfigTemplate
                    .builder()
                    .port(6000) //绑定端口
                    .readTimeout(5000) //读取超时事件
                    .transmissionSecurity(new DESTransmissionSecurity("45625478","10000000")) //传输加密
                    .iReceiveMsg(msg -> { //处理收到的消息
                        System.out.println(msg);
                        return "服务端返回消息";
                    })
                    .localService(new ServerLocalService()) //绑定rpc提供的服务
                    .iCertificationService((clientId, clientAuthInfo) -> { //认证
                        System.out.println("客户端登陆，"+clientId);
                        return true;
                     })
                    .build());

        //监听客户端事件
        simpleServer.registClientEventListener((clientId, eventName, eventValue) -> {
            LOGGER.info("收到客户端事件，客户端：{}，事件名：{}，事件值：{}",clientId,eventName,eventValue);
        });
       /* try{
            Thread.sleep(5000);
        }catch (Exception e){
            e.printStackTrace();
        }
        simpleServer.sendMsg("服务端异步消息","10001");
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("同步消息调用结果:"+simpleServer.sendSynchronizeMsg("服务端同步消息","10001"));
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        //rpc远程调用
        ClientLocalService clientLocalService=simpleServer.getRemoteService(ClientLocalService.class,"10001");
        System.out.println("rpc调用结果:"+clientLocalService.test());*/

    }
}


