package cn.kbdwn.netty.msg.server;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * 客户端资源对象
 */
@Data
public class ClientResource {
	private String id;  //可持久化到数据库
	private Channel channel;
	private Long loginTime;
	private String clientId;
}
