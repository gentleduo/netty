package org.duo.netty.protocol.http.json;

import io.netty.handler.codec.http.FullHttpResponse;

/**
 * 响应类
 */
public class HttpJsonResponse {

    private FullHttpResponse httpResponse;
    private Object result;

    public HttpJsonResponse(FullHttpResponse httpResponse, Object result) {
        this.httpResponse = httpResponse;
        this.result = result;
    }

    public final FullHttpResponse getHttpResponse() {
        return httpResponse;
    }

    public final void setHttpResponse(FullHttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public final Object getResult() {
        return result;
    }

    public final void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "HttpJsonResponse [httpResponse=" + httpResponse + ", result="
                + result + "]";
    }

}