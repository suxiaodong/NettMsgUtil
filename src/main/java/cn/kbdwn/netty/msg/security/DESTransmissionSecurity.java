package cn.kbdwn.netty.msg.security;

import cn.kbdwn.netty.msg.exception.NettyMsgUtilException;
import cn.kbdwn.netty.msg.utils.DESUtil;
import cn.kbdwn.netty.msg.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DES加解密实现
 */
@Data
@AllArgsConstructor
public class DESTransmissionSecurity implements ITransmissionSecurity{

    //TODO 检查空，检查desIV是否为数字，检查长度是否为8位

    private String desKey;

    private String desIv;

    @Override
    public String encryption(String msg){
        try{
            return DESUtil.encryot(msg, desKey, Utils.getUtf8Bytes(desIv), DESUtil.ENCODED_UTF8, DESUtil.CIPHER_INSTANCE_CBC);
        }catch (Exception e){
            throw new NettyMsgUtilException(e);
        }

    }

    @Override
    public String decrypt(String msg){
        try{
            return DESUtil.decrypt(msg, desKey, Utils.getUtf8Bytes(desIv), DESUtil.ENCODED_UTF8, DESUtil.CIPHER_INSTANCE_CBC);
        }catch (Exception e){
            throw new NettyMsgUtilException(e);
        }
    }
}
