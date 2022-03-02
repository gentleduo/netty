package org.duo.netty.protocol.http.json;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 请求类
 */
public class HttpJsonRequest {

    private FullHttpRequest request;
    private Object body;

    public HttpJsonRequest(FullHttpRequest request, Object body) {
        this.request = request;
        this.body = body;
    }

    public final FullHttpRequest getRequest() {
        return request;
    }

    public final void setRequest(FullHttpRequest request) {
        this.request = request;
    }

    public final Object getBody() {
        return body;
    }

    public final void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "HttpJsonRequest [request=" + request + ", body =" + body + "]";
    }
}
