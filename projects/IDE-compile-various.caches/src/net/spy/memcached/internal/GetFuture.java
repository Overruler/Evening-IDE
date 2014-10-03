package net.spy.memcached.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GetFuture<T> extends AbstractListenableFuture<T, GetCompletionListener> implements Future<T> {
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}
	@Override
	public boolean isCancelled() {
		return false;
	}
	@Override
	public boolean isDone() {
		return false;
	}
	@Override
	public T get() throws InterruptedException, ExecutionException {
		return null;
	}
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}
}
