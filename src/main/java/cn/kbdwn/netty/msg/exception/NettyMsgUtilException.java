package cn.kbdwn.netty.msg.exception;

public class NettyMsgUtilException extends RuntimeException {

    private int errorCode;

    public NettyMsgUtilException() {
    }

    public NettyMsgUtilException(String message) {
        super(message);
    }

    public NettyMsgUtilException(String message, int errorCode) {
        super(message);
        this.errorCode=errorCode;
    }

    public NettyMsgUtilException(String message, Throwable cause) {
        super(message, cause);
    }

    public NettyMsgUtilException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode=errorCode;
    }

    public NettyMsgUtilException(Throwable cause) {
        super(cause);
    }

    public int getErrorCode(){
        return this.errorCode;
    }
}
