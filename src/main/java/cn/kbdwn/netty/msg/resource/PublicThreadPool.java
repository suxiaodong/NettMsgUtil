package cn.kbdwn.netty.msg.resource;

import java.util.List;
import java.util.concurrent.*;

public class PublicThreadPool {
	
	private static ExecutorService cachedThreadPool=new ThreadPoolExecutor(0, 20, 60L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
	
	public static void execute(Runnable runnable){
		cachedThreadPool.execute(runnable);
	}
	
	public static <T> Future<T> submit(Callable<T> task){
		return cachedThreadPool.submit(task);
	}
	
	public static <T> List<Future<T>> invokeAll(List<Callable<T>> tasks) throws InterruptedException{
		return cachedThreadPool.invokeAll(tasks);
	}
}
