package net.spy.memcached.internal;

import java.util.concurrent.Future;

public interface ListenableFuture<T, L extends GenericCompletionListener> extends Future<T> {}
