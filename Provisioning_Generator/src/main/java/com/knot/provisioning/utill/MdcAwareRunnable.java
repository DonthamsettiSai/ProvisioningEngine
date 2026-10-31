package com.knot.provisioning.utill;

import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

public class MdcAwareRunnable implements Runnable {
	private final Runnable delegate;
	private final Map<String, String> contextMap;

	public MdcAwareRunnable(Runnable delegate) {
		this.delegate = delegate;
		this.contextMap = ThreadContext.getImmutableContext(); // capture MDC
	}

	@Override
	public void run() {
		try {
			ThreadContext.putAll(contextMap); // restore MDC
			delegate.run();
		} finally {
			ThreadContext.clearAll(); // cleanup
		}
	}
}
