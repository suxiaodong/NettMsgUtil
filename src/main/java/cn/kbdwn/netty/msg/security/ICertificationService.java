package cn.kbdwn.netty.msg.security;

/**
 * 认证
 */
public interface ICertificationService {

    boolean login(String clientId,String clientAuthInfo);

}
