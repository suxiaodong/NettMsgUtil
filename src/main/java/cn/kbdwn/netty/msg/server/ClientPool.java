package cn.kbdwn.netty.msg.server;

import java.util.*;

/**
 * 缓存tcp客户端
 */
public class ClientPool {
	
	private static Map<String, ClientResource> clients=new HashMap<>();

	public static synchronized void put(String channelId, ClientResource clientResource){
		clients.put(channelId, clientResource);
	}

	public static ClientResource get(String channelId){
		return clients.get(channelId);
	}

	public static synchronized void remove(String channelId){
		clients.remove(channelId);
	}

	public static ClientResource getClient(String clientId){
	    Collection<ClientResource> clientResources=clients.values();
	    for (ClientResource cr : clientResources) {
            if(cr.getClientId().equals(clientId)){
                return cr;
            }
        }
	    return null;
	}
	
	public static List<ClientResource> listClients(){
	    return new ArrayList<>(clients.values());
	}
}
