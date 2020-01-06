package cn.kbdwn.netty.msg.server;

public interface IServer {

    /**
     * 初始化
     * @param configTemplate
     */
    void init(ServerConfigTemplate configTemplate);

    /**
     * 销毁
     */
    void destroy();

}
