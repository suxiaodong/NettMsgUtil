package cn.kbdwn.netty.msg.server;

import cn.kbdwn.netty.msg.client.Client;
import cn.kbdwn.netty.msg.exception.NettyMsgUtilException;
import cn.kbdwn.netty.msg.message.MsgTypeEnum;
import cn.kbdwn.netty.msg.message.RpcRequestContent;
import cn.kbdwn.netty.msg.message.TransferObject;
import cn.kbdwn.netty.msg.resource.ErrorCode;
import cn.kbdwn.netty.msg.security.ITransmissionSecurity;
import cn.kbdwn.netty.msg.utils.Utils;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class Server extends ServerMsgService implements IServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(Client.class);

    protected IClientEventListener clientEventListener;

    protected ServerConfigTemplate configTemplate;
    protected Channel serverChannel;
    protected boolean isInit;


    /**
     * 加密
     *
     * @param msg
     * @return
     */
    protected String encode(String msg) {
        try {
            ITransmissionSecurity transmissionSecurity =configTemplate.getTransmissionSecurity();
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
            ITransmissionSecurity transmissionSecurity =configTemplate.getTransmissionSecurity();
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
     * 收到消息
     *
     * @param msg
     */
    @Override
    protected Object receiveMsg(Object msg) {
        return configTemplate.getIReceiveMsg().receiveMsg(msg);
    }

    /**
     * 执行发送消息
     *
     * @param data
     */
    protected void actionSendMsg(Object data, Channel channel) {
        String encodeStr = encode(JSON.toJSONString(data));
        String msg = JSON.toJSONString(new TransferObject(encodeStr)); //传输对象json字符串
        channel.writeAndFlush(Unpooled.copiedBuffer(Utils.getUtf8Bytes(msg)));
    }


    @Override
    protected long getReadTimeOut() {
        return configTemplate.getReadTimeout();
    }


    /**
     * 超时发送心跳消息
     */
    protected void sendPong(Channel channel) {
        LOGGER.debug("发送pong消息");
        sendAsynchronousMsg(null, MsgTypeEnum.MGS_TYPE_PONG, channel);
    }

    /**
     * 调用本地服务方法
     *
     * @param rpcRequestContent
     * @return
     */
    protected Object invokeLocalServiceMethod(RpcRequestContent rpcRequestContent) {
        Object service = configTemplate.getLocalService();
        try {
            //String serviceName = rpcRequestContent.getClassName();
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


    @Override
    public void init(ServerConfigTemplate configTemplate) {
        if (this.isInit) {
            throw new RuntimeException("已经初始化");
        }
        this.configTemplate = configTemplate;
        if(startServer()){
            msgInit(); //初始化消息服务
            this.isInit = true;
        }

    }

    @Override
    public void destroy() {
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
            msgDestroy();
        } catch (Throwable e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    /**
     * 获取远程提供的服务
     *
     * @param t
     * @param <T>
     * @return
     */
    public <T> T getRemoteService(Class<T> t,String clientId) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(t);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                //检查方式是否属于classt
                Method[] methods=t.getDeclaredMethods();
                for (Method me : methods) {
                    if(me.equals(method)){
                        RpcRequestContent rpcRequestContent = new RpcRequestContent(null, method.getName(), objects);
                        return sendRequestResponseMsg(rpcRequestContent, MsgTypeEnum.MSG_TYPE_REQUEST_RPC,ClientPool.getClient(clientId).getChannel());
                    }
                }
                return method.invoke(t.newInstance(),objects);
            }
        });
        return (T) enhancer.create();
    }

    /**
     * 获取远程提供的服务
     * @param t 代理对象
     * @param name 服务提供者名称
     * @param <T>
     * @return
     */
  /*  public <T> T getRemoteService(Class<T> t,String clientId,String name) {
        if(name==null || name.trim().length()==0){
            name=t.getName();
        }
        String beanName=name;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(t);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                //检查方式是否属于classt
                Method[] methods=t.getDeclaredMethods();
                for (Method me : methods) {
                    if(me.equals(method)){
                        RpcRequestContent rpcRequestContent = new RpcRequestContent(beanName, method.getName(), objects);
                        return sendRequestResponseMsg(rpcRequestContent, MsgTypeEnum.MSG_TYPE_REQUEST_RPC,ClientPool.getClient(clientId).getChannel());
                    }
                }
                return method.invoke(t.newInstance(),objects);
            }
        });
        return (T) enhancer.create();
    }*/

    /**
     * 启动服务
     */
    protected abstract boolean startServer();

    public void registClientEventListener(IClientEventListener clientEventListener){
        this.clientEventListener=clientEventListener;
    }

}
