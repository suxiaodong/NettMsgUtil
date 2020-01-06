package cn.kbdwn.netty.msg.server;

import cn.kbdwn.netty.msg.exception.NettyMsgUtilException;
import cn.kbdwn.netty.msg.message.*;
import cn.kbdwn.netty.msg.resource.ErrorCode;
import cn.kbdwn.netty.msg.resource.PublicThreadPool;
import cn.kbdwn.netty.msg.security.ICertificationService;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Date;

public class SimpleServer extends Server {

    private Logger LOGGER= LoggerFactory.getLogger(SimpleServer.class);

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    /**
     * 解码
     * @return
     */
    private ChannelInboundHandlerAdapter desDecodeHandle(){
        return new ChannelInboundHandlerAdapter(){
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                try{
                    String msgStr=((ByteBuf) msg).toString(Charset.forName("utf-8"));
                    TransferObject transferObject= JSON.parseObject(msgStr, TransferObject.class);
                    //解密
                    String encodeStr=transferObject.getData();
                    Message message=JSON.parseObject(decode(encodeStr), Message.class);
                    //LOGGER.debug("收到消息：{}",message);
                    ctx.fireChannelRead(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 心跳
     * @return
     */
    private ChannelInboundHandlerAdapter heartbeatHandler(){
        return new ChannelInboundHandlerAdapter(){
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
                if (event instanceof IdleStateEvent) {
                    IdleStateEvent e = (IdleStateEvent) event;
                    if (e.state() == IdleState.READER_IDLE) {
                        LOGGER.info("服务器超过20秒没有收到客户端{}发送的消息，将断开当前与客户端的链接",ctx.channel().remoteAddress());
                        ctx.close();
                    }
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Message message=(Message)msg;
                if(message.getMask().equals(MsgTypeEnum.MGS_TYPE_PING)){
                    LOGGER.debug("收到ping消息");
                    sendPong(ctx.channel());
                }else{
                    ctx.fireChannelRead(msg);
                }
            }
        };
    }

    /**
     * 认证
     * @return
     */
    private ChannelInboundHandlerAdapter certificationHandler(){
        return new ChannelInboundHandlerAdapter(){
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Message message=(Message)msg;
                if(message.getMask().equals(MsgTypeEnum.MGS_TYPE_LOGIN_REQUEST)){
                    PublicThreadPool.execute(() -> {
                        LoginRequestContent content=JSON.parseObject(JSON.toJSONString(message.getContent()),LoginRequestContent.class);
                        ICertificationService iCertificationService=configTemplate.getICertificationService();
                        boolean login=true;
                        if(iCertificationService!=null){
                            login=configTemplate.getICertificationService().login(content.getClientMask(),content.getClientAuthInfo()); //登陆认证
                        }
                        LoginResponseContent loginResponseContent=new LoginResponseContent();
                        loginResponseContent.setClientMask(content.getClientMask());
                        if(login){
                            ClientResource clientResource=new ClientResource();
                            clientResource.setChannel(ctx.channel());
                            clientResource.setClientId(content.getClientMask());
                            clientResource.setLoginTime(new Date().getTime());
                            ClientPool.put(ctx.channel().id().asLongText(),clientResource); //缓存客户端
                            responseRequest(loginResponseContent,message.getMsgId(),ctx.channel());

                            if(clientEventListener!=null){
                                PublicThreadPool.execute(() -> {
                                    clientEventListener.event(clientResource.getClientId(),"login",content.getClientAuthInfo());
                                });
                            }

                        }else{
                            loginResponseContent.setIsSuccess(false);
                            loginResponseContent.setCode(ErrorCode.RPC_NO_PERMISSION);
                            loginResponseContent.setData("登陆认证失败");
                            responseRequest(loginResponseContent,message.getMsgId(),ctx.channel());
                        }
                    });
                }else{
                    ctx.fireChannelRead(msg);
                }
            }
        };
    }

    /**
     * 主handler
     * @return
     */
    private ChannelInboundHandlerAdapter mainHandler(){
        return new ChannelInboundHandlerAdapter(){
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                LOGGER.info("客户端：{} 建立链接成功",ctx.channel().remoteAddress());
                super.channelActive(ctx);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                LOGGER.info("客户端:{} 断开连接", ctx.channel().remoteAddress());
                super.channelInactive(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                LOGGER.error("客户端:{} 发生异常:{}",ctx.channel().remoteAddress(),cause.getMessage());
                ctx.close();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Message message=(Message)msg;
                PublicThreadPool.execute(() -> {
                    try{
                        if(message.getMask().equals(MsgTypeEnum.MSG_TYPE_REQUEST)){  //请求
                            receiveMsg(message.getContent());
                        }else if(message.getMask().equals(MsgTypeEnum.MSG_TYPE_REQUEST_SYNC)){  //同步请求
                            ResponseContent responseContent=new ResponseContent();
                            try{
                                responseContent.setData(receiveMsg(message.getContent()));
                            }catch (Exception e){
                                e.printStackTrace();
                                responseContent.setIsSuccess(false);
                                responseContent.setData(e.getMessage());
                                responseContent.setCode(ErrorCode.BUSINESS_ERROR);
                            }
                            responseRequest(responseContent,message.getMsgId(),ctx.channel());
                        }else if(message.getMask().equals(MsgTypeEnum.MSG_TYPE_REQUEST_RPC)){  //rpc请求
                            ResponseContent responseContent=new ResponseContent();
                            RpcRequestContent content=JSON.parseObject(JSON.toJSONString(message.getContent()), RpcRequestContent.class);
                            try{
                                responseContent.setData(invokeLocalServiceMethod(content));
                            }catch (NettyMsgUtilException coe){
                                coe.printStackTrace();
                                responseContent.setIsSuccess(false);
                                responseContent.setData(coe.getMessage());
                                responseContent.setCode(coe.getErrorCode());
                            }
                            catch (Exception e){
                                e.printStackTrace();
                                responseContent.setIsSuccess(false);
                                responseContent.setData(e.getMessage());
                                responseContent.setCode(ErrorCode.BUSINESS_ERROR);
                            }
                            responseRequest(responseContent,message.getMsgId(),ctx.channel());
                        }else if(message.getMask().equals(MsgTypeEnum.MSG_TYPE_RESPONSE)){  //响应
                            ResponseContent content=JSON.parseObject(JSON.toJSONString(message.getContent()), ResponseContent.class);
                            getMsgLockPool().trigger(message.getMsgId(),content.getIsSuccess(),content.getData(),content.getCode());
                        }
                    }catch (Exception e){
                        LOGGER.error(e.getMessage(),e);
                    }
                });
            }
        };
    }

    /**
     * 启动服务
     */
    @Override
    protected boolean startServer(){
        ChannelFuture f = null;
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 选择通信实现方式（nio,oio(bio),epoll(适用于特定操作系统)）
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new IdleStateHandler(20, 0, 0));
                            pipeline.addLast(new JsonObjectDecoder()); //解码
                            pipeline.addLast(desDecodeHandle());
                            pipeline.addLast(heartbeatHandler());
                            pipeline.addLast(certificationHandler());
                            pipeline.addLast(mainHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            f =b.bind(configTemplate.getPort()).sync();
            serverChannel = f.channel();
            return true;
        }catch (Exception e) {
            LOGGER.error("Netty start error:", e);
            destroy();
            return false;
        } finally {
            if (f != null && f.isSuccess()) {
                LOGGER.info("Netty server listening port " + configTemplate.getPort() + " and ready for connections...");
            } else {
                LOGGER.error("Netty server start up Error!");
                destroy();
            }
        }
    }


    @Override
    public void destroy() {
        LOGGER.info("Shutdown Netty Server...");
        super.destroy();
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        LOGGER.info("Shutdown Netty Server Success!");
    }

}
