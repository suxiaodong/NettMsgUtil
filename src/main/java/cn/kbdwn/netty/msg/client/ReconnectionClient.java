package cn.kbdwn.netty.msg.client;

import cn.kbdwn.netty.msg.exception.NettyMsgUtilException;
import cn.kbdwn.netty.msg.message.*;
import cn.kbdwn.netty.msg.resource.ErrorCode;
import cn.kbdwn.netty.msg.resource.PublicThreadPool;
import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReconnectionClient extends Client {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReconnectionClient.class);

    private class Resource {
        private int connectTimeout = 3000; //连接超时时间
        private Bootstrap bootstrap;
        private final EventLoopGroup group = new NioEventLoopGroup();
        private volatile ChannelFuture future;
        private volatile java.util.concurrent.ScheduledFuture<?> reconnectExecutorFuture = null;
        private long lastConnectedTime = System.currentTimeMillis();
        private final AtomicInteger reconnect_count = new AtomicInteger(0);
        private final AtomicBoolean reconnect_error_log_flag = new AtomicBoolean(false);
        // 重连warning的间隔.(waring多少次之后，warning一次)
        private final int reconnect_warning_period = 1800;
        private final long shutdown_timeout = 1000 * 60 * 15;
        private final ScheduledThreadPoolExecutor reconnectExecutorService = new ScheduledThreadPoolExecutor(
                2, new NamedThreadFactory());
    }

    private Resource resource;

    public ReconnectionClient() {
        this.resource = new Resource();
        buildBootstrap();
    }

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
                    LOGGER.debug("收到消息：{}",decode(encodeStr));
                    Message message=JSON.parseObject(decode(encodeStr), Message.class);
                    //
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
                        LOGGER.info("客户端超过20秒没有收到服务端发送的消息，将断开当前与服务端的链接");
                        ctx.fireChannelInactive();
                    } else if (e.state() == IdleState.WRITER_IDLE) {
                        LOGGER.debug("客户端10秒内未发送数据到服务端，即将发送心跳包");
                        PublicThreadPool.execute(() -> {
                            sendPing();
                        });
                    }
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Message message=(Message)msg;
                if(message.getMask().equals(MsgTypeEnum.MGS_TYPE_PONG)){
                    LOGGER.debug("收到pong消息");
                }else{
                    ctx.fireChannelRead(msg);
                }
            }
        };
    }

    /**
     * 认证响应
     * @return
     */
    private ChannelInboundHandlerAdapter certificationHandler(){
        return new ChannelInboundHandlerAdapter(){
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Message message=(Message)msg;
                if(message.getMask().equals(MsgTypeEnum.MGS_TYPE_LOGIN_RESPONSE)){
                    PublicThreadPool.execute(() -> {
                        LoginResponseContent content=JSON.parseObject(JSON.toJSONString(message.getContent()),LoginResponseContent.class);
                        getMsgLockPool().trigger(message.getMsgId(),content.getIsSuccess(),content.getData(),content.getCode());
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
                LOGGER.info("客户端：{} 与服务端 {} 建立链接成功",clientConfigTemplate.getClientId(),ctx.channel().remoteAddress());
                super.channelActive(ctx);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                LOGGER.info("客户端:{} 与服务端 {} 断开连接",clientConfigTemplate.getClientId(),ctx.channel().remoteAddress());
                super.channelInactive(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                LOGGER.error("客户端:{} 发生异常:{}",clientConfigTemplate.getClientId(),cause.getMessage());
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
                                responseContent.setIsSuccess(false);
                                responseContent.setData(e.getMessage());
                                responseContent.setCode(ErrorCode.BUSINESS_ERROR);
                            }
                            responseRequest(responseContent,message.getMsgId());
                        }else if(message.getMask().equals(MsgTypeEnum.MSG_TYPE_REQUEST_RPC)){  //rpc请求
                            ResponseContent responseContent=new ResponseContent();
                            RpcRequestContent content=JSON.parseObject(JSON.toJSONString(message.getContent()), RpcRequestContent.class);
                            try{
                                responseContent.setData(invokeLocalServiceMethod(content));
                            }catch (NettyMsgUtilException coe){
                                responseContent.setIsSuccess(false);
                                responseContent.setData(coe.getMessage());
                                responseContent.setCode(coe.getErrorCode());
                            }
                            catch (Exception e){
                                responseContent.setIsSuccess(false);
                                responseContent.setData(e.getMessage());
                                responseContent.setCode(ErrorCode.BUSINESS_ERROR);
                            }
                            responseRequest(responseContent,message.getMsgId());
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
     * 构建客户端的Bootstrap
     */
    private void buildBootstrap() {
        resource.bootstrap = new Bootstrap();
        resource.bootstrap.option(ChannelOption.TCP_NODELAY, true);
        resource.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        resource.bootstrap.group(resource.group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new IdleStateHandler(20, 10, 0));
                        pipeline.addLast(new JsonObjectDecoder()); //解码
                        pipeline.addLast(desDecodeHandle());
                        pipeline.addLast(heartbeatHandler());
                        pipeline.addLast(certificationHandler());
                        pipeline.addLast(mainHandler());
                    }
                });
    }

    /**
     * 连接
     */
    protected void connection() {
        try {
            if (super.isConnected()) {
                return;
            }
            // 初始化连接线程池
            initReconnectExecutor();
            doConnect();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /**
     * 初始化客户端连接的任务执行线程池
     */
    private synchronized void initReconnectExecutor() {
        if (resource.reconnectExecutorFuture == null || resource.reconnectExecutorFuture.isCancelled()) {
            // 新建一个reconnect的任务执行线程池
            resource.reconnectExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    if (!isConnected()) {
                        doConnect();
                    } else {
                        resource.lastConnectedTime = System.currentTimeMillis();
                    }
                } catch (Throwable t) {
                    String errorMsg = "连接服务端 {} 发生错误 .";
                    if (System.currentTimeMillis() - resource.lastConnectedTime > resource.shutdown_timeout) {
                        if (!resource.reconnect_error_log_flag.get()) {
                            resource.reconnect_error_log_flag.set(true);
                            LOGGER.error(errorMsg, getRemoteAddress(), t);
                            return;
                        }
                    }
                    if (resource.reconnect_count.getAndIncrement() % resource.reconnect_warning_period == 0) {
                        LOGGER.warn(errorMsg, getRemoteAddress(), t);
                    }
                }
            }, 20000, 5000, TimeUnit.MILLISECONDS);
        }
    }

    private void connectSuccess(){
        Channel newChannel = resource.future.channel(); // 得到当前连接的channel
        // 将已断开连接的channel关闭
        try {
            Channel oldChannel = super.channel;
            if (oldChannel != null) {
                LOGGER.debug("客户端建立新的连接{},即将关闭旧的连接{}", newChannel, oldChannel);
                oldChannel.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        // 清空掉线重连标记
        resource.reconnect_count.set(0);
        resource.reconnect_error_log_flag.set(false);
        super.channel = newChannel; // 将当前的channel重置为新的channel
        LOGGER.info("连接服务端{}成功,Channel: {}", getRemoteAddress(), this.channel);
    }

    private void connectFailure(long start){
        if (resource.future.cause() != null) { // 连接发生异常
            resource.future.cause().printStackTrace();
            LOGGER.error("连接服务端:" + getRemoteAddress() + "发生异常:" + resource.future.cause().getMessage());
        } else {
            // 连接不成功
            LOGGER.error("连接服务端:" + getRemoteAddress() + "失败,用时：" + (System.currentTimeMillis() - start) + "毫秒.");
        }
    }

    /**
     * 执行连接
     */
    private void doConnect() throws Throwable {
        try{
            long start = System.currentTimeMillis();
            // 连接到服务端
            resource.future = resource.bootstrap.connect(getRemoteAddress());
            try {
                //在等待指定时间后检查连接是否成功
                boolean connectRet = resource.future.awaitUninterruptibly(resource.connectTimeout, TimeUnit.MILLISECONDS);
                if (connectRet && resource.future.isSuccess()) {
                    connectSuccess();
                    login(); //登陆
                }else{
                    connectFailure(start);
                }
            } finally {
                // 关闭回调接口
                if (!isConnected()) {
                    resource.future.cancel(true);
                }
            }
        }catch (Throwable throwable){
            LOGGER.error("连接服务端{}失败,原因:{}", getRemoteAddress(), throwable.getMessage());
        }
    }

    /**
     * 销毁任务执行线程池
     */
    private synchronized void destroyReconnectExecutor() {
        try {
            if (resource.reconnectExecutorFuture != null && !resource.reconnectExecutorFuture.isDone()) {
                resource.reconnectExecutorFuture.cancel(true);
                //从工作队列中移除所有是cannel状态的Future任务
                resource.reconnectExecutorService.purge();
            }
        } catch (Throwable e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    /**
     * 销毁客户端
     */
    @Override
    public void destroy() {
        super.destroy();
        destroyReconnectExecutor();
        try {
            resource.group.shutdownGracefully();
        } catch (Throwable t) {
            LOGGER.warn(t.getMessage());
        }
    }


    public static void main(String[] args) {
        /*ReconnectionClient reconnectionClient = new ReconnectionClient();
        ClientConfigTemplate target = ClientConfigTemplate
                .builder()
                .clientId("123")
                .iReceiveMsg((msg) -> {
                    return "";
                })
                .build();
        reconnectionClient.init(target);

        TestService testService = reconnectionClient.getRemoteService(TestService.class);
        System.out.println(testService.getObj("aa", 12, "test"));*/

    }


}
