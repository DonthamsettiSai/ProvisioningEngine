package com.knot.provisioning;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

public class DecodeHttpInputMessage implements HttpInputMessage {

    private HttpHeaders headers;

    private InputStream body;

    public DecodeHttpInputMessage(HttpHeaders headers, InputStream body) {
        super();
        this.headers = headers;
        this.body = body;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.body;
    }

}
