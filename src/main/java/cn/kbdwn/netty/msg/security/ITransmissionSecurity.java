package cn.kbdwn.netty.msg.security;

public interface ITransmissionSecurity {

    /**
     * 加密
     * @param msg
     * @return
     */
    String encryption(String msg);

    /**
     * 解密
     * @param msg
     * @return
     */
    String decrypt(String msg);
}
