# NettMsgUtil

基于Netty实现的轻量级消息工具，包含服务端和客户端，支持同步/异步/rpc调用，使用FastJSON序列化，支持断线重连，支持DES安全传输，支持安全认证

# 说明

用到了lombok来简化开发，Lombok项目是一个Java库，它会自动插入编辑器和构建工具中，Lombok提供了一组有用的注释，用来消除Java类中的大量样板代码。仅五个字符(@Data)就可以替换数百行代码从而产生干净，简洁且易于维护的Java类

# 快速开始

## 服务端

### 创建一个服务
```
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
```   
### 发送异步消息

```
simpleServer.sendMsg("服务端异步消息","10001"); 
```

### 发送同步消息

```
 System.out.println("同步消息调用结果:"+simpleServer.sendSynchronizeMsg("服务端同步消息","10001"));
```

### rpc调用

```
ClientLocalService clientLocalService=simpleServer.getRemoteService(ClientLocalService.class,"10001");
System.out.println("rpc调用结果:"+clientLocalService.test());
```

## 客户端

```
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
                .localService(new ClientLocalService()) ////绑定rpc提供的服务
                .build());
```
 
### 发送异步消息

```
reconnectionClient.sendMsg("客户端异步消息");
```

### 发送同步消息

```
System.out.println("同步消息调用结果："+reconnectionClient.sendSynchronizeMsg("客户端同步消息"));
```

### rpc调用

```
ClientLocalService clientLocalService=reconnectionClient.getRemoteService(ClientLocalService.class);
System.out.println("rpc调用结果："+clientLocalService.test());
```
