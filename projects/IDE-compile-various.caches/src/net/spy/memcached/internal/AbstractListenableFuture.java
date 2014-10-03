package net.spy.memcached.internal;

import net.spy.memcached.compat.SpyObject;

public abstract class AbstractListenableFuture<T, L extends GenericCompletionListener> extends SpyObject implements
	ListenableFuture<T, L> {}
