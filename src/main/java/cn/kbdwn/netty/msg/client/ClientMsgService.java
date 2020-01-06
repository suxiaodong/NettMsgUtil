package cn.kbdwn.netty.msg.client;

import cn.kbdwn.netty.msg.message.Message;
import cn.kbdwn.netty.msg.message.MsgTypeEnum;
import cn.kbdwn.netty.msg.resource.MsgLock;
import cn.kbdwn.netty.msg.resource.MsgLockPool;
import cn.kbdwn.netty.msg.utils.Utils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public abstract class ClientMsgService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClientMsgService.class);

    private Timer timer = new Timer();

    @Getter
    private MsgLockPool msgLockPool=new MsgLockPool();

    /**
     * 收到消息
     * @return
     */
    protected abstract Object receiveMsg(Object msg);


    /**
     * 执行发送方法
     * @param data
     */
    protected abstract void actionSendMsg(Object data);

    /**
     * 获取读取超时时间
     */
    protected abstract long getReadTimeOut();


    /**
     * 发送异步消息
     *
     * @param msg
     * @return
     */
    public void sendMsg(Object msg) {
        sendAsynchronousMsg(msg, MsgTypeEnum.MSG_TYPE_REQUEST);
    }

    /**
     * 发送同步消息
     *
     * @param msg
     * @return
     */
    public Object sendSynchronizeMsg(Object msg) {
        return sendRequestResponseMsg(msg, MsgTypeEnum.MSG_TYPE_REQUEST_SYNC);
    }


    /**
     * 响应请求
     */
    protected void responseRequest(Object msg, String msgId) {
        Message message = new Message();
        message.setMask(MsgTypeEnum.MSG_TYPE_RESPONSE);
        message.setMsgId(msgId);
        message.setContent(msg);
        actionSendMsg(message); //发送消息
    }

    /**
     * 发送异步消息
     *
     * @param msg
     * @param msgTypeEnum
     * @return
     */
    protected void sendAsynchronousMsg(Object msg, MsgTypeEnum msgTypeEnum) {
        String msgId = Utils.getMessageId();
        Message message = new Message();
        message.setMask(msgTypeEnum);
        message.setMsgId(msgId);
        message.setContent(msg);
        actionSendMsg(message); //发送消息
    }

    /**
     * 请求响应消息（同步）
     *
     * @param msg
     * @param msgTypeEnum
     * @return
     */
    protected Object sendRequestResponseMsg(Object msg, MsgTypeEnum msgTypeEnum) {
        String msgId = Utils.getMessageId();
        Message message = new Message();
        message.setMask(msgTypeEnum);
        message.setMsgId(msgId);
        message.setContent(msg);
        MsgLock msgLock = getMsgLockPool().create(msgId);
        actionSendMsg(message); //发送消息
        //等待通知
        synchronized (msgLock) {
            try {
                msgLock.wait();
                if (!msgLock.isSuccess()) {
                    LOGGER.error("远程调用失败，错误码：{},内容：{}", msgLock.getCode(), msgLock.getResult().toString());
                    throw new RuntimeException("远程调用失败");
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("远程调用失败");
            }
        }
        return msgLock.getResult();
    }

    /**
     * 初始化
     */
    protected void msgInit(){
        long readTimeOut=getReadTimeOut();
        timer.schedule(new TimerTask() {
            public void run() {
                msgLockPool.handlerTimeout(readTimeOut); //xx时间超时，lock释放
            }
        },readTimeOut,readTimeOut); //xx秒后开始监测，每次监测间隔xx秒
    };

    /**
     * 销毁
     */
    protected void msgDestroy(){
        timer.cancel();
    }
}
