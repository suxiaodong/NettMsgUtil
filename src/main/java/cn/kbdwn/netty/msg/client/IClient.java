package cn.kbdwn.netty.msg.client;

public interface IClient {

    /**
     * 初始化
     * @param configTemplate
     */
    void init(ClientConfigTemplate configTemplate);


    /**
     * 销毁客户端
     */
    void destroy();

}
