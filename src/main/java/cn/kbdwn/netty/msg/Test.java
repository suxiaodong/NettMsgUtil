package cn.kbdwn.netty.msg;

import cn.kbdwn.netty.msg.client.ClientConfigTemplate;
import cn.kbdwn.netty.msg.client.ReconnectionClient;
import cn.kbdwn.netty.msg.security.DESTransmissionSecurity;
import com.alibaba.fastjson.JSON;

import java.util.HashMap;

public class Test {

    public static void main(String[] args) {
        ReconnectionClient reconnectionClient = new ReconnectionClient();
        reconnectionClient.init(ClientConfigTemplate
                .builder()
                .serverAddress("127.0.0.1") //远程地址
                .port(6000) //端口
                .clientId("10001") //本地客户端标识
                .certificationInfo(JSON.toJSONString(new HashMap<String,Object>(){{
                    put("name","su");
                    put("pwd","123");
                }}))
                .transmissionSecurity(new DESTransmissionSecurity("45625478","10000000")) //传输加密
                .iReceiveMsg(msg -> {
                    System.out.println(msg);
                    return "客户端返回消息";
                }) //处理收到的消息
                .localService(new ClientLocalService())
                .build());

        try{
            Thread.sleep(5000);
        }catch (Exception e){
            e.printStackTrace();
        }
        reconnectionClient.sendMsg("客户端异步消息");
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("同步消息调用结果："+reconnectionClient.sendSynchronizeMsg("客户端同步消息"));
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        ClientLocalService clientLocalService=reconnectionClient.getRemoteService(ClientLocalService.class);
        System.out.println("rpc调用结果："+clientLocalService.test());
    }
}


