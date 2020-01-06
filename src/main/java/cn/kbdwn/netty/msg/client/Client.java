package cn.kbdwn.netty.msg.client;

import cn.kbdwn.netty.msg.exception.NettyMsgUtilException;
import cn.kbdwn.netty.msg.message.MsgTypeEnum;
import cn.kbdwn.netty.msg.message.RpcRequestContent;
import cn.kbdwn.netty.msg.resource.ErrorCode;
import cn.kbdwn.netty.msg.security.ITransmissionSecurity;
import cn.kbdwn.netty.msg.utils.DESUtil;
import cn.kbdwn.netty.msg.utils.Utils;
import com.alibaba.fastjson.JSON;
import cn.kbdwn.netty.msg.message.LoginRequestContent;
import cn.kbdwn.netty.msg.message.TransferObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

public abstract class Client extends ClientMsgService implements IClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(Client.class);

    /**
     * 客户端配置
     */
    protected ClientConfigTemplate clientConfigTemplate;

    /**
     * 初始化
     */
    protected boolean isInit;

    /**
     * 连接通道
     */
    protected Channel channel;

    /**
     * 是否登陆
     */
    protected boolean isLogin;


    /*提供子类调用的方法*/

    /**
     * 连接是否建立成功
     *
     * @return
     */
    protected boolean isConnected() {
        if (channel == null) {
            return false;
        }
        return channel.isActive();
    }


    /**
     * 登陆
     */
    protected boolean login() {
        try {
            LOGGER.debug("发送登陆消息");
            LoginRequestContent messageContent = new LoginRequestContent();
            messageContent.setClientMask(clientConfigTemplate.getClientId());
            messageContent.setClientAuthInfo(clientConfigTemplate.getCertificationInfo()); // TODO认证信息
            sendRequestResponseMsg(messageContent, MsgTypeEnum.MGS_TYPE_LOGIN_REQUEST);
            isLogin = true;
        } catch (Exception e) {
            isLogin = false;
            e.printStackTrace();
        }
        return isLogin;
    }

    /**
     * 超时发送心跳消息
     */
    protected void sendPing() {
        LOGGER.debug("发送ping消息");
        sendAsynchronousMsg(null, MsgTypeEnum.MGS_TYPE_PING);
    }


    /**
     * 加密
     *
     * @param msg
     * @return
     */
    protected String encode(String msg) {
        try {
            ITransmissionSecurity transmissionSecurity =clientConfigTemplate.getTransmissionSecurity();
            if(transmissionSecurity!=null){
                return transmissionSecurity.encryption(msg);
            }
            return msg;
        } catch (Exception e) {
            LOGGER.error("加密失败，{}", e.getMessage(), e);
            throw new RuntimeException("加密失败");
        }
    }

    /**
     * 解密
     *
     * @param msg
     * @return
     */
    protected String decode(String msg) {
        try {
            ITransmissionSecurity transmissionSecurity =clientConfigTemplate.getTransmissionSecurity();
            if(transmissionSecurity!=null){
                return transmissionSecurity.decrypt(msg);
            }
            return msg;
        } catch (Exception e) {
            LOGGER.error("解密失败，{}", e.getMessage(), e);
            throw new RuntimeException("解密失败");
        }
    }


    /**
     * 调用本地服务方法
     *
     * @param rpcRequestContent
     * @return
     */
    protected Object invokeLocalServiceMethod(RpcRequestContent rpcRequestContent) {
        Object service = clientConfigTemplate.getLocalService();
        try {
            //String className = rpcRequestContent.getClassName();
            String methodName = rpcRequestContent.getMethodName();
            Object[] args = rpcRequestContent.getArgs();

            //判断类名
           /* if (!Utils.checkClassName(className, service.getClass())) {
                throw new NettyMsgUtilException("指定目标对象不存在", ErrorCode.RPC_TARGET_NOT_FOUND);
            }*/
            Method[] methods = service.getClass().getDeclaredMethods();
            Method invokeMethod = null;
            for (Method me : methods) {
                if (me.getName().equals(methodName) && me.getParameterTypes().length == args.length) {  //json序列化丢失类型，不能通过类型判断
                    invokeMethod = me;
                    break;
                }
            }
            if (invokeMethod == null) {
                LOGGER.error("指定方法不存在，方法：{},参数个数{}", methodName, args.length);
                throw new NettyMsgUtilException("指定方法不存在,方法：" + methodName + ",参数个数" + args.length, ErrorCode.RPC_METHOD_NOT_FOUND);
            } else {
                return invokeMethod.invoke(service, args);
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行发送消息
     *
     * @param data
     */
    protected void actionSendMsg(Object data) {
        String encodeStr = encode(JSON.toJSONString(data));
        String msg = JSON.toJSONString(new TransferObject(encodeStr)); //传输对象json字符串
        channel.writeAndFlush(Unpooled.copiedBuffer(Utils.getUtf8Bytes(msg)));
    }

    @Override
    protected long getReadTimeOut() {
        return clientConfigTemplate.getReadTimeout();
    }

    /**
     * 收到消息
     *
     * @param msg
     */
    @Override
    protected Object receiveMsg(Object msg) {
        return clientConfigTemplate.getIReceiveMsg().receiveMsg(msg);
    }

    /*提供公共方法的方法*/

    /**
     * 获取远程服务端的地址
     *
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress(clientConfigTemplate.getServerAddress(), clientConfigTemplate.getPort());
    }

    /**
     * 初始化
     *
     * @param configTemplate
     */
    public void init(ClientConfigTemplate configTemplate) {
        if (this.isInit) {
            throw new RuntimeException("已经初始化");
        }
        this.clientConfigTemplate = configTemplate;
        this.connection();
        msgInit(); //初始化消息服务
        this.isInit = true;
    }

    /**
     * 获取远程提供的服务
     *
     * @param t 提供的服务类
     * @param <T>
     * @return
     */
    public <T> T getRemoteService(Class<T> t) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(t);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                //检查方法是否属于classt
                Method[] methods=t.getDeclaredMethods();
                for (Method me : methods) {
                    if(me.equals(method)){
                        RpcRequestContent rpcRequestContent = new RpcRequestContent(null, method.getName(), objects);
                        return sendRequestResponseMsg(rpcRequestContent, MsgTypeEnum.MSG_TYPE_REQUEST_RPC);
                    }
                }
                return method.invoke(t.newInstance(),objects);
            }
        });
        return (T) enhancer.create();
    }

    /**
     * 获取远程提供的服务
     * @param t 提供服务的类
     * @param remoteService 服务名称
     * @param <T>
     * @return
     */
/*
    public <T> T getRemoteService(Class<T> t,String remoteService) {
        if(remoteService==null || remoteService.trim().length()==0){
            remoteService=t.getName();
        }
        String beanName=remoteService;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(t);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                //检查方法是否属于classt
                Method[] methods=t.getDeclaredMethods();
                for (Method me : methods) {
                    if(me.equals(method)){
                        RpcRequestContent rpcRequestContent = new RpcRequestContent(beanName, method.getName(), objects);
                        return sendRequestResponseMsg(rpcRequestContent, MsgTypeEnum.MSG_TYPE_REQUEST_RPC);
                    }
                }
               return method.invoke(t.newInstance(),objects);
            }
        });
        return (T) enhancer.create();
    }
*/



    /**
     * 销毁
     */
    @Override
    public void destroy() {
        try {
            if (channel != null) {
                channel.close();
            }
            msgDestroy();
        } catch (Throwable e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    /*子类需要实现的方法*/

    /**
     * 连接
     */
    protected abstract void connection();

}
