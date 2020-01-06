package cn.kbdwn.netty.msg.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Utils {

    private final static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static String getMessageId(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static byte[] getUtf8Bytes(String msg){
        byte[] bytes=null;
        try{
            bytes=msg.getBytes("utf-8");
        }catch (UnsupportedEncodingException e){
            LOGGER.error(e.getMessage(),e);
            throw new RuntimeException(e.getMessage());
        }
        return bytes;
    }

    private static void getSuperClassName(Class<?> clazz, List<String> nameList){
        if(clazz.getName().equals("java.lang.Object")){
            return;
        }else {
            nameList.add(clazz.getName());
            getSuperClassName(clazz.getSuperclass(),nameList);
        }
    }

    public static boolean checkClassName(String nameA, Class<?> classB) {
        if (nameA == null || classB == null) {
            LOGGER.error("类名不能为空");
            throw new RuntimeException("类名不能为空");
        }
        List<String> nameBList=new ArrayList<>();
        getSuperClassName(classB,nameBList);
        String[] nameAArray = nameA.split("\\.");

        for (String nameB : nameBList) {
            String[] nameBArray = nameB.split("\\.");
            boolean result=nameAArray[nameAArray.length - 1].equals(nameBArray[nameBArray.length - 1]);
            if(result){
                return result;
            }
        }
        return false;
    }
}
