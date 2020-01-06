package cn.kbdwn.netty.msg.resource;

public class ErrorCode {
    /**
     * 禁止登陆
     */
    public static int RPC_DISABLE_LOGIN=1001;
    
    /**
     * 认证失败
     */
    public static int RPC_NO_PERMISSION=1002;

    public static int RPC_SERVER_INTERNAL_ERROR=101; //服务器错误

    public static int RPC_RESPONSE_TIMEOUT=102; //读取超时

    public static int RPC_TARGET_NOT_FOUND=103; //目标未找到

    public static int RPC_METHOD_NOT_FOUND=104; //方法未找到


    public static int BUSINESS_ERROR=200; //业务操作错误

}
