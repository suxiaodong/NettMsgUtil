package cn.kbdwn.netty.msg.resource;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class MsgLock {

    private static final Logger LOG = LoggerFactory.getLogger(MsgLock.class);

    private long initTime;
    private String msgId;
    private boolean success=true;
    private Object result;
    private Integer code;

    public MsgLock(String msgId){
        this.msgId=msgId;
        this.initTime=System.currentTimeMillis();
    }


    public void success(Object result) {
        this.result=result;
        this.success=true;
    }

    public void failure(Object result,Integer code) {
        this.result=result;
        this.success=false;
        this.code=code;
    }

}
