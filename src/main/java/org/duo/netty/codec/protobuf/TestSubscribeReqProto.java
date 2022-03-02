package org.duo.netty.codec.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

/**
 * Protobuf的编解码过程：
 * 1.通过SubscribeReqProto.SubscribeReq的静态方法newBuilder创建SubscribeReqProto.SubscribeReq的Builder实例；
 * 2.通过Builder构建器对SubscribeReq的属性进行设置，对于集合类型，通过addAllXXX()方法可以将集合对象设置到对应的属性中；
 * 3.编码时通过调用SubscribeReqProto.SubscribeReq实例的toByteArray即可将SubscribeReq编码为byte数组；
 * 4.解码时通过SubscribeReqProto.SubscribeReq的静态方法parseFrom将二进制byte数组解码为原始对象。
 */
public class TestSubscribeReqProto {

    private static byte[] encode(SubscribeReqProto.SubscribeReq req) {
        return req.toByteArray();
    }

    private static SubscribeReqProto.SubscribeReq decode(byte[] body)
            throws InvalidProtocolBufferException {
        return SubscribeReqProto.SubscribeReq.parseFrom(body);
    }

    private static SubscribeReqProto.SubscribeReq createSubscribeReq() {
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq
                .newBuilder();
        builder.setSubReqID(1);
        builder.setUserName("Lilinfeng");
        builder.setProductName("Netty Book");

        List<String> address = new ArrayList<>();
        address.add("NanJing YuHuaTai");
        address.add("BeiJing LiuLiChang");
        address.add("ShenZhen HongShuLin");
        builder.addAllAddress(address);
        return builder.build();
    }

    /**
     * 运行结果表明：经过Protobuf编码后，生成的SubscribeReqProto.SubscribeReq与编码前原始的SubscribeReqProto.SubscribeReq等价。
     *
     * @param args
     * @throws InvalidProtocolBufferException
     */
    public static void main(String[] args)
            throws InvalidProtocolBufferException {
        SubscribeReqProto.SubscribeReq req = createSubscribeReq();
        System.out.println("Before encode : " + req.toString());
        SubscribeReqProto.SubscribeReq req2 = decode(encode(req));
        System.out.println("After decode : " + req.toString());
        System.out.println("Assert equal : --> " + req2.equals(req));
    }

}
