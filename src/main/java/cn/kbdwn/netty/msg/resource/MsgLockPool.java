package cn.kbdwn.netty.msg.resource;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个客户端或者服务端实例应对应一个MsgLockPool
 */
public class MsgLockPool {

    private List<MsgLock> msgLocks=new ArrayList<>();

    private void trigger(MsgLock lock){
        synchronized (lock) {
            lock.notifyAll();
        }
        msgLocks.remove(lock);
    }

    public MsgLock create(String msgId){
        MsgLock msgLock=new MsgLock(msgId);
        msgLocks.add(msgLock);
        return msgLock;
    }

    /**
     * 处理超时消息
     * @param overtime
     */
    public synchronized void handlerTimeout(long overtime){
        long now=System.currentTimeMillis();
        for (MsgLock msgLock : msgLocks) {
            if(msgLock.getInitTime()+overtime<now){
                msgLock.failure("Request timed out", ErrorCode.RPC_RESPONSE_TIMEOUT); //错误码
                trigger(msgLock);
            }
        }
    }

    /**
     * 触发通知
     * @param msgId
     */
    public synchronized void trigger(String msgId,Boolean success,Object data,Integer code){
        for (MsgLock msgLock : msgLocks) {
            if(msgLock.getMsgId().equals(msgId)){
                if(success){
                    msgLock.success(data);
                }else{
                    msgLock.failure(data,code);
                }
                trigger(msgLock);
                return;
            }
        }
    }
}
