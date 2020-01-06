package cn.kbdwn.netty.msg.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class DESUtil {

    /**
     * UTF-8编码
     */
    public static final String ENCODED_UTF8 = "UTF-8";
    /**
     * ASCII编码
     */
    public static final String ENCODED_ASCII = "ASCII";
    /**
     * CBC加密模式
     */
    public static final String CIPHER_INSTANCE_CBC = "DES/CBC/PKCS5Padding";
    /**
     * ECB加密模式
     */
    public static final String CIPHER_INSTANCE_ECB = "DES/ECB/PKCS5Padding";

    /**
     * DES加密
     * @param HexString 字符串（16位16进制字符串）
     * @param keyStr
     * @param HexStringENCODED 要加密值的转换byte编码
     * @param CipherInstanceType 需要加密类型
     * @return
     * @throws Exception
     */
    public static String encryot(String HexString, String keyStr,byte[] ivValue,String HexStringENCODED,String CipherInstanceType)
            throws Exception {
        Cipher cipher = Cipher.getInstance(CipherInstanceType);
        DESKeySpec desKeySpec = new DESKeySpec(keyStr.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

        IvParameterSpec iv = new IvParameterSpec(ivValue);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] theCph = cipher.doFinal(HexString.getBytes(HexStringENCODED));
        return toHexString(theCph);
    }

    public static String toHexString(byte b[]) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String plainText = Integer.toHexString(0xff & b[i]);
            if (plainText.length() < 2)
                plainText = "0" + plainText;
            hexString.append(plainText);
        }

        return hexString.toString();
    }

    /**
     * DES解密方法
     * @param message 需要解密字符串
     * @param key 解密需要的KEY
     * @param HexStringENCODED  解密字符串转换编码
     * @param CipherInstanceType 解密类型
     * @return
     * @throws Exception
     */
    public static String decrypt(String message, String key,byte[] ivValue,String HexStringENCODED,String CipherInstanceType) throws Exception {

        byte[] bytesrc = convertHexString(message);
        byte[] theKey = null;
        theKey = key.getBytes();
        Cipher cipher = Cipher.getInstance(CipherInstanceType);
        DESKeySpec desKeySpec = new DESKeySpec(theKey);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(ivValue);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

        byte[] retByte = cipher.doFinal(bytesrc);
        return new String(retByte,HexStringENCODED);
    }

    /**
     * 16进制字符串转换为byte数组
     * @param ss
     * @return
     */
    public static byte[] convertHexString(String ss) {
        byte digest[] = new byte[ss.length() / 2];
        for (int i = 0; i < digest.length; i++) {
            String byteString = ss.substring(2 * i, 2 * i + 2);
            int byteValue = Integer.parseInt(byteString, 16);
            digest[i] = (byte) byteValue;
        }

        return digest;
    }

}
