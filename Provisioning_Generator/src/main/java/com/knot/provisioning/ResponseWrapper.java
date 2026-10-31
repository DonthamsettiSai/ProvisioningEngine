package com.knot.provisioning;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResponseWrapper extends HttpServletResponseWrapper {
    private final CharArrayWriter writer;
    private static final Logger logger = LogManager.getLogger(ResponseWrapper.class);
    public ResponseWrapper(HttpServletResponse response) {
        super(response);
        writer = new CharArrayWriter();
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(writer);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            @Override
            public void write(int b) {
                writer.write(b);
            }

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener listener) {
				
				logger.info("Need to define");
			}
        };
    }

    public String getResponseBody() {
        return writer.toString();
    }

}
