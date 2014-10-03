/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2013 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.AuthThreadMonitor;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.ConcatenationType;
import net.spy.memcached.ops.Mutator;
import net.spy.memcached.ops.StoreType;
import net.spy.memcached.transcoders.TranscodeService;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Client to a memcached server.
 *
 * <h2>Basic usage</h2>
 *
 * <pre>
 * MemcachedClient c = new MemcachedClient(
 *    new InetSocketAddress(&quot;hostname&quot;, portNum));
 *
 * // Store a value (async) for one hour
 * c.set(&quot;someKey&quot;, 3600, someObject);
 * // Retrieve a value.
 * Object myObject = c.get(&quot;someKey&quot;);
 * </pre>
 *
 * <h2>Advanced Usage</h2>
 *
 * <p>
 * MemcachedClient may be processing a great deal of asynchronous messages or
 * possibly dealing with an unreachable memcached, which may delay processing.
 * If a memcached is disabled, for example, MemcachedConnection will continue to
 * attempt to reconnect and replay pending operations until it comes back up. To
 * prevent this from causing your application to hang, you can use one of the
 * asynchronous mechanisms to time out a request and cancel the operation to the
 * server.
 * </p>
 *
 * <pre>
 *      // Get a memcached client connected to several servers
 *      // over the binary protocol
 *      MemcachedClient c = new MemcachedClient(new BinaryConnectionFactory(),
 *              AddrUtil.getAddresses("server1:11211 server2:11211"));
 *
 *      // Try to get a value, for up to 5 seconds, and cancel if it
 *      // doesn't return
 *      Object myObj = null;
 *      Future&lt;Object&gt; f = c.asyncGet("someKey");
 *      try {
 *          myObj = f.get(5, TimeUnit.SECONDS);
 *      // throws expecting InterruptedException, ExecutionException
 *      // or TimeoutException
 *      } catch (Exception e) {  /*  /
 *          // Since we don't need this, go ahead and cancel the operation.
 *          // This is not strictly necessary, but it'll save some work on
 *          // the server.  It is okay to cancel it if running.
 *          f.cancel(true);
 *          // Do other timeout related stuff
 *      }
 * </pre>
 *
 * <p>Optionally, it is possible to activate a check that makes sure that
 * the node is alive and responding before running actual operations (even
 * before authentication. Only enable this if you are sure that you do not
 * run into issues during connection (some memcached services have problems
 * with it). You can enable it by setting the net.spy.verifyAliveOnConnect
 * System Property to "true".</p>
 */
public class MemcachedClient extends SpyObject implements MemcachedClientIF,
    ConnectionObserver {

  protected volatile boolean shuttingDown = false;

  protected final long operationTimeout = 0;

  protected final MemcachedConnection mconn = null;

  protected final OperationFactory opFact = null;

  protected final Transcoder<Object> transcoder = null;

  protected final TranscodeService tcService = null;

  protected final AuthDescriptor authDescriptor = null;

  protected final ConnectionFactory connFactory = null;

  protected final AuthThreadMonitor authMonitor = null;

  protected final ExecutorService executorService = null;

  /**
   * Get a memcache client operating on the specified memcached locations.
   *
   * @param ia the memcached locations
   * @throws IOException if connections cannot be established
   */
  public MemcachedClient(InetSocketAddress... ia) throws IOException {}

  /**
   * Get a memcache client over the specified memcached locations.
   *
   * @param addrs the socket addrs
   * @throws IOException if connections cannot be established
   */
  public MemcachedClient(List<InetSocketAddress> addrs) throws IOException {}

  /**
   * Get a memcache client over the specified memcached locations.
   *
   * @param cf the connection factory to configure connections for this client
   * @param addrs the socket addresses
   * @throws IOException if connections cannot be established
   */
  public MemcachedClient(ConnectionFactory cf, List<InetSocketAddress> addrs)
    throws IOException {}

  /**
   * Get the addresses of available servers.
   *
   * <p>
   * This is based on a snapshot in time so shouldn't be considered completely
   * accurate, but is a useful for getting a feel for what's working and what's
   * not working.
   * </p>
   *
   * @return point-in-time view of currently available servers
   */
  @Override
  public Collection<SocketAddress> getAvailableServers() { return null; }

  /**
   * Get the addresses of unavailable servers.
   *
   * <p>
   * This is based on a snapshot in time so shouldn't be considered completely
   * accurate, but is a useful for getting a feel for what's working and what's
   * not working.
   * </p>
   *
   * @return point-in-time view of currently available servers
   */
  @Override
  public Collection<SocketAddress> getUnavailableServers() { return null; }

  /**
   * Get a read-only wrapper around the node locator wrapping this instance.
   *
   * @return this instance's NodeLocator
   */
  @Override
  public NodeLocator getNodeLocator() { return null; }

  /**
   * Get the default transcoder that's in use.
   *
   * @return this instance's Transcoder
   */
  @Override
  public Transcoder<Object> getTranscoder() { return null; }

  @Override
  public CountDownLatch broadcastOp(final BroadcastOpFactory of) { return null; }

  @Override
  public CountDownLatch broadcastOp(final BroadcastOpFactory of,
      Collection<MemcachedNode> nodes) { return null; }

  private CountDownLatch broadcastOp(BroadcastOpFactory of,
      Collection<MemcachedNode> nodes, boolean checkShuttingDown) { return null; }

  private <T> OperationFuture<Boolean> asyncStore(StoreType storeType,
      String key, int exp, T value, Transcoder<T> tc) { return null; }

  private OperationFuture<Boolean> asyncStore(StoreType storeType, String key,
      int exp, Object value) { return null; }

  private <T> OperationFuture<Boolean> asyncCat(ConcatenationType catType,
      long cas, String key, T value, Transcoder<T> tc) { return null; }

  /**
   * Touch the given key to reset its expiration time with the default
   * transcoder.
   *
   * @param key the key to fetch
   * @param exp the new expiration to set for the given key
   * @return a future that will hold the return value of whether or not the
   *         fetch succeeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> touch(final String key, final int exp) { return null; }

  /**
   * Touch the given key to reset its expiration time.
   *
   * @param key the key to fetch
   * @param exp the new expiration to set for the given key
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of whether or not the
   *         fetch succeeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> touch(final String key, final int exp,
      final Transcoder<T> tc) { return null; }

  /**
   * Append to an existing value in the cache.
   *
   * If 0 is passed in as the CAS identifier, it will override the value
   * on the server without performing the CAS check.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param cas cas identifier (ignored in the ascii protocol)
   * @param key the key to whose value will be appended
   * @param val the value to append
   * @return a future indicating success, false if there was no change to the
   *         value
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> append(long cas, String key, Object val) { return null; }

  /**
   * Append to an existing value in the cache.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param key the key to whose value will be appended
   * @param val the value to append
   * @return a future indicating success, false if there was no change to the
   *         value
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> append(String key, Object val) { return null; }

  /**
   * Append to an existing value in the cache.
   *
   * If 0 is passed in as the CAS identifier, it will override the value
   * on the server without performing the CAS check.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param <T>
   * @param cas cas identifier (ignored in the ascii protocol)
   * @param key the key to whose value will be appended
   * @param val the value to append
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future indicating success
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> append(long cas, String key, T val,
      Transcoder<T> tc) { return null; }

  /**
   * Append to an existing value in the cache.
   *
   * If 0 is passed in as the CAS identifier, it will override the value
   * on the server without performing the CAS check.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param <T>
   * @param key the key to whose value will be appended
   * @param val the value to append
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future indicating success
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> append(String key, T val,
      Transcoder<T> tc) { return null; }

  /**
   * Prepend to an existing value in the cache.
   *
   * If 0 is passed in as the CAS identifier, it will override the value
   * on the server without performing the CAS check.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param cas cas identifier (ignored in the ascii protocol)
   * @param key the key to whose value will be prepended
   * @param val the value to append
   * @return a future indicating success
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> prepend(long cas, String key, Object val) { return null; }

  /**
   * Prepend to an existing value in the cache.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param key the key to whose value will be prepended
   * @param val the value to append
   * @return a future indicating success
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> prepend(String key, Object val) { return null; }

  /**
   * Prepend to an existing value in the cache.
   *
   * If 0 is passed in as the CAS identifier, it will override the value
   * on the server without performing the CAS check.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param <T>
   * @param cas cas identifier (ignored in the ascii protocol)
   * @param key the key to whose value will be prepended
   * @param val the value to append
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future indicating success
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> prepend(long cas, String key, T val,
      Transcoder<T> tc) { return null; }

  /**
   * Prepend to an existing value in the cache.
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * @param <T>
   * @param key the key to whose value will be prepended
   * @param val the value to append
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future indicating success
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> prepend(String key, T val,
      Transcoder<T> tc) { return null; }

  /**
   * Asynchronous CAS operation.
   *
   * @param <T>
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param value the new value
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future that will indicate the status of the CAS
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<CASResponse>
  asyncCAS(String key, long casId, T value, Transcoder<T> tc) { return null; }

  /**
   * Asynchronous CAS operation.
   *
   * @param <T>
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param exp the expiration of this object
   * @param value the new value
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future that will indicate the status of the CAS
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<CASResponse>
  asyncCAS(String key, long casId, int exp, T value, Transcoder<T> tc) { return null; }

  /**
   * Asynchronous CAS operation using the default transcoder.
   *
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param value the new value
   * @return a future that will indicate the status of the CAS
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<CASResponse>
  asyncCAS(String key, long casId, Object value) { return null; }

  /**
   * Asynchronous CAS operation using the default transcoder with expiration.
   *
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param exp the expiration of this object
   * @param value the new value
   * @return a future that will indicate the status of the CAS
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<CASResponse>
  asyncCAS(String key, long casId, int exp, Object value) { return null; }

  /**
   * Perform a synchronous CAS operation.
   *
   * @param <T>
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param value the new value
   * @param tc the transcoder to serialize and unserialize the value
   * @return a CASResponse
   * @throws OperationTimeoutException if global operation timeout is exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> CASResponse cas(String key, long casId, T value,
      Transcoder<T> tc) { return null; }

  /**
   * Perform a synchronous CAS operation.
   *
   * @param <T>
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param exp the expiration of this object
   * @param value the new value
   * @param tc the transcoder to serialize and unserialize the value
   * @return a CASResponse
   * @throws OperationTimeoutException if global operation timeout is exceeded
   * @throws CancellationException if operation was canceled
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> CASResponse cas(String key, long casId, int exp, T value,
      Transcoder<T> tc) { return null; }

  /**
   * Perform a synchronous CAS operation with the default transcoder.
   *
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param value the new value
   * @return a CASResponse
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public CASResponse cas(String key, long casId, Object value) { return null; }

  /**
   * Perform a synchronous CAS operation with the default transcoder.
   *
   * @param key the key
   * @param casId the CAS identifier (from a gets operation)
   * @param exp the expiration of this object
   * @param value the new value
   * @return a CASResponse
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public CASResponse cas(String key, long casId, int exp, Object value) { return null; }

  /**
   * Add an object to the cache iff it does not exist already.
   *
   * <p>
   * The {@code exp} value is passed along to memcached exactly as given,
   * and will be processed per the memcached protocol specification:
   * </p>
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param <T>
   * @param key the key under which this object should be added.
   * @param exp the expiration of this object
   * @param o the object to store
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future representing the processing of this operation
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> add(String key, int exp, T o,
      Transcoder<T> tc) { return null; }

  /**
   * Add an object to the cache (using the default transcoder) iff it does not
   * exist already.
   *
   * <p>
   * The {@code exp} value is passed along to memcached exactly as given,
   * and will be processed per the memcached protocol specification:
   * </p>
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param key the key under which this object should be added.
   * @param exp the expiration of this object
   * @param o the object to store
   * @return a future representing the processing of this operation
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> add(String key, int exp, Object o) { return null; }

  /**
   * Set an object in the cache regardless of any existing value.
   *
   * <p>
   * The {@code exp} value is passed along to memcached exactly as given,
   * and will be processed per the memcached protocol specification:
   * </p>
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param <T>
   * @param key the key under which this object should be added.
   * @param exp the expiration of this object
   * @param o the object to store
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future representing the processing of this operation
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> set(String key, int exp, T o,
      Transcoder<T> tc) { return null; }

  /**
   * Set an object in the cache (using the default transcoder) regardless of any
   * existing value.
   *
   * <p>
   * The {@code exp} value is passed along to memcached exactly as given,
   * and will be processed per the memcached protocol specification:
   * </p>
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param key the key under which this object should be added.
   * @param exp the expiration of this object
   * @param o the object to store
   * @return a future representing the processing of this operation
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> set(String key, int exp, Object o) { return null; }

  /**
   * Replace an object with the given value iff there is already a value for the
   * given key.
   *
   * <p>
   * The {@code exp} value is passed along to memcached exactly as given,
   * and will be processed per the memcached protocol specification:
   * </p>
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param <T>
   * @param key the key under which this object should be added.
   * @param exp the expiration of this object
   * @param o the object to store
   * @param tc the transcoder to serialize and unserialize the value
   * @return a future representing the processing of this operation
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<Boolean> replace(String key, int exp, T o,
      Transcoder<T> tc) { return null; }

  /**
   * Replace an object with the given value (transcoded with the default
   * transcoder) iff there is already a value for the given key.
   *
   * <p>
   * The {@code exp} value is passed along to memcached exactly as given,
   * and will be processed per the memcached protocol specification:
   * </p>
   *
   * <p>
   * Note that the return will be false any time a mutation has not occurred.
   * </p>
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param key the key under which this object should be added.
   * @param exp the expiration of this object
   * @param o the object to store
   * @return a future representing the processing of this operation
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> replace(String key, int exp, Object o) { return null; }

  /**
   * Get the given key asynchronously.
   *
   * @param <T>
   * @param key the key to fetch
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> GetFuture<T> asyncGet(final String key, final Transcoder<T> tc) { return null; }

  /**
   * Get the given key asynchronously and decode with the default transcoder.
   *
   * @param key the key to fetch
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public GetFuture<Object> asyncGet(final String key) { return null; }

  /**
   * Gets (with CAS support) the given key asynchronously.
   *
   * @param <T>
   * @param key the key to fetch
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<CASValue<T>> asyncGets(final String key,
      final Transcoder<T> tc) { return null; }

  /**
   * Gets (with CAS support) the given key asynchronously and decode using the
   * default transcoder.
   *
   * @param key the key to fetch
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<CASValue<Object>> asyncGets(final String key) { return null; }

  /**
   * Gets (with CAS support) with a single key.
   *
   * @param <T>
   * @param key the key to get
   * @param tc the transcoder to serialize and unserialize value
   * @return the result from the cache and CAS id (null if there is none)
   * @throws OperationTimeoutException if global operation timeout is exceeded
   * @throws CancellationException if operation was canceled
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> CASValue<T> gets(String key, Transcoder<T> tc) { return null; }

  /**
   * Get with a single key and reset its expiration.
   *
   * @param <T>
   * @param key the key to get
   * @param exp the new expiration for the key
   * @param tc the transcoder to serialize and unserialize value
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws CancellationException if operation was canceled
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) { return null; }

  /**
   * Get a single key and reset its expiration using the default transcoder.
   *
   * @param key the key to get
   * @param exp the new expiration for the key
   * @return the result from the cache and CAS id (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public CASValue<Object> getAndTouch(String key, int exp) { return null; }

  /**
   * Gets (with CAS support) with a single key using the default transcoder.
   *
   * @param key the key to get
   * @return the result from the cache and CAS id (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public CASValue<Object> gets(String key) { return null; }

  /**
   * Get with a single key.
   *
   * @param <T>
   * @param key the key to get
   * @param tc the transcoder to serialize and unserialize value
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws CancellationException if operation was canceled
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> T get(String key, Transcoder<T> tc) { return null; }

  /**
   * Get with a single key and decode using the default transcoder.
   *
   * @param key the key to get
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Object get(String key) { return null; }

  /**
   * Asynchronously get a bunch of objects from the cache.
   *
   * @param <T>
   * @param keyIter Iterator that produces keys.
   * @param tcIter an iterator of transcoders to serialize and unserialize
   *          values; the transcoders are matched with the keys in the same
   *          order. The minimum of the key collection length and number of
   *          transcoders is used and no exception is thrown if they do not
   *          match
   * @return a Future result of that fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter,
      Iterator<Transcoder<T>> tcIter) { return null; }

  /**
   * Asynchronously get a bunch of objects from the cache.
   *
   * @param <T>
   * @param keys the keys to request
   * @param tcIter an iterator of transcoders to serialize and unserialize
   *          values; the transcoders are matched with the keys in the same
   *          order. The minimum of the key collection length and number of
   *          transcoders is used and no exception is thrown if they do not
   *          match
   * @return a Future result of that fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
          Iterator<Transcoder<T>> tcIter) { return null; }

  /**
   * Asynchronously get a bunch of objects from the cache.
   *
   * @param <T>
   * @param keyIter Iterator for the keys to request
   * @param tc the transcoder to serialize and unserialize values
   * @return a Future result of that fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keyIter,
      Transcoder<T> tc) { return null; }

  /**
   * Asynchronously get a bunch of objects from the cache.
   *
   * @param <T>
   * @param keys the keys to request
   * @param tc the transcoder to serialize and unserialize values
   * @return a Future result of that fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys,
      Transcoder<T> tc) { return null; }

  /**
   * Asynchronously get a bunch of objects from the cache and decode them with
   * the given transcoder.
   *
   * @param keyIter Iterator that produces the keys to request
   * @return a Future result of that fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public BulkFuture<Map<String, Object>> asyncGetBulk(
         Iterator<String> keyIter) { return null; }

  /**
   * Asynchronously get a bunch of objects from the cache and decode them with
   * the given transcoder.
   *
   * @param keys the keys to request
   * @return a Future result of that fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) { return null; }

  /**
   * Varargs wrapper for asynchronous bulk gets.
   *
   * @param <T>
   * @param tc the transcoder to serialize and unserialize value
   * @param keys one more more keys to get
   * @return the future values of those keys
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc,
      String... keys) { return null; }

  /**
   * Varargs wrapper for asynchronous bulk gets with the default transcoder.
   *
   * @param keys one more more keys to get
   * @return the future values of those keys
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) { return null; }

  /**
   * Get the given key to reset its expiration time.
   *
   * @param key the key to fetch
   * @param exp the new expiration to set for the given key
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<CASValue<Object>> asyncGetAndTouch(final String key,
      final int exp) { return null; }

  /**
   * Get the given key to reset its expiration time.
   *
   * @param key the key to fetch
   * @param exp the new expiration to set for the given key
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> OperationFuture<CASValue<T>> asyncGetAndTouch(final String key,
      final int exp, final Transcoder<T> tc) { return null; }

  /**
   * Get the values for multiple keys from the cache.
   *
   * @param <T>
   * @param keyIter Iterator that produces the keys
   * @param tc the transcoder to serialize and unserialize value
   * @return a map of the values (for each value that exists)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws CancellationException if operation was canceled
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> Map<String, T> getBulk(Iterator<String> keyIter,
      Transcoder<T> tc) { return null; }

  /**
   * Get the values for multiple keys from the cache.
   *
   * @param keyIter Iterator that produces the keys
   * @return a map of the values (for each value that exists)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Map<String, Object> getBulk(Iterator<String> keyIter) { return null; }

  /**
   * Get the values for multiple keys from the cache.
   *
   * @param <T>
   * @param keys the keys
   * @param tc the transcoder to serialize and unserialize value
   * @return a map of the values (for each value that exists)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> Map<String, T> getBulk(Collection<String> keys,
      Transcoder<T> tc) { return null; }

  /**
   * Get the values for multiple keys from the cache.
   *
   * @param keys the keys
   * @return a map of the values (for each value that exists)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Map<String, Object> getBulk(Collection<String> keys) { return null; }

  /**
   * Get the values for multiple keys from the cache.
   *
   * @param <T>
   * @param tc the transcoder to serialize and unserialize value
   * @param keys the keys
   * @return a map of the values (for each value that exists)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) { return null; }

  /**
   * Get the values for multiple keys from the cache.
   *
   * @param keys the keys
   * @return a map of the values (for each value that exists)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Map<String, Object> getBulk(String... keys) { return null; }

  /**
   * Get the versions of all of the connected memcacheds.
   *
   * @return a Map of SocketAddress to String for connected servers
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Map<SocketAddress, String> getVersions() { return null; }

  /**
   * Get all of the stats from all of the connections.
   *
   * @return a Map of a Map of stats replies by SocketAddress
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Map<SocketAddress, Map<String, String>> getStats() { return null; }

  /**
   * Get a set of stats from all connections.
   *
   * @param arg which stats to get
   * @return a Map of the server SocketAddress to a map of String stat keys to
   *         String stat values.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public Map<SocketAddress, Map<String, String>> getStats(final String arg) { return null; }

  private long mutate(Mutator m, String key, long by, long def, int exp) { return 0; }

  /**
   * Increment the given key by the given amount.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the amount to increment
   * @return the new value (-1 if the key doesn't exist)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long incr(String key, long by) { return 0; }

  /**
   * Increment the given key by the given amount.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the amount to increment
   * @return the new value (-1 if the key doesn't exist)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long incr(String key, int by) { return 0; }

  /**
   * Decrement the given key by the given value.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the value
   * @return the new value (-1 if the key doesn't exist)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long decr(String key, long by) { return 0; }

  /**
   * Decrement the given key by the given value.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the value
   * @return the new value (-1 if the key doesn't exist)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long decr(String key, int by) { return 0; }

  /**
   * Increment the given counter, returning the new value.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the amount to increment
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return the new value, or -1 if we were unable to increment or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long incr(String key, long by, long def, int exp) { return 0; }

  /**
   * Increment the given counter, returning the new value.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the amount to increment
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return the new value, or -1 if we were unable to increment or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long incr(String key, int by, long def, int exp) { return 0; }

  /**
   * Decrement the given counter, returning the new value.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the amount to decrement
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return the new value, or -1 if we were unable to decrement or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long decr(String key, long by, long def, int exp) { return 0; }

  /**
   * Decrement the given counter, returning the new value.
   *
   * Due to the way the memcached server operates on items, incremented and
   * decremented items will be returned as Strings with any operations that
   * return a value.
   *
   * @param key the key
   * @param by the amount to decrement
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return the new value, or -1 if we were unable to decrement or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long decr(String key, int by, long def, int exp) { return 0; }

  private long mutateWithDefault(Mutator t, String key, long by, long def,
      int exp) { return 0; }

  private OperationFuture<Long> asyncMutate(Mutator m, String key, long by,
      long def, int exp) { return null; }

  /**
   * Asychronous increment.
   *
   * @param key key to increment
   * @param by the amount to increment the value by
   * @return a future with the incremented value, or -1 if the increment failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncIncr(String key, long by) { return null; }

  /**
   * Asychronous increment.
   *
   * @param key key to increment
   * @param by the amount to increment the value by
   * @return a future with the incremented value, or -1 if the increment failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncIncr(String key, int by) { return null; }

  /**
   * Asynchronous decrement.
   *
   * @param key key to decrement
   * @param by the amount to decrement the value by
   * @return a future with the decremented value, or -1 if the decrement failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncDecr(String key, long by) { return null; }

  /**
   * Asynchronous decrement.
   *
   * @param key key to decrement
   * @param by the amount to decrement the value by
   * @return a future with the decremented value, or -1 if the decrement failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncDecr(String key, int by) { return null; }

  /**
   * Asychronous increment.
   *
   * @param key key to increment
   * @param by the amount to increment the value by
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return a future with the incremented value, or -1 if the increment failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncIncr(String key, long by, long def,
    int exp) { return null; }

  /**
   * Asychronous increment.
   *
   * @param key key to increment
   * @param by the amount to increment the value by
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return a future with the incremented value, or -1 if the increment failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncIncr(String key, int by, long def,
    int exp) { return null; }

  /**
   * Asynchronous decrement.
   *
   * @param key key to decrement
   * @param by the amount to decrement the value by
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return a future with the decremented value, or -1 if the decrement failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncDecr(String key, long by, long def,
    int exp) { return null; }

  /**
   * Asynchronous decrement.
   *
   * @param key key to decrement
   * @param by the amount to decrement the value by
   * @param def the default value (if the counter does not exist)
   * @param exp the expiration of this object
   * @return a future with the decremented value, or -1 if the decrement failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncDecr(String key, int by, long def,
    int exp) { return null; }

  /**
   * Asychronous increment.
   *
   * @param key key to increment
   * @param by the amount to increment the value by
   * @param def the default value (if the counter does not exist)
   * @return a future with the incremented value, or -1 if the increment failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncIncr(String key, long by, long def) { return null; }

  /**
   * Asychronous increment.
   *
   * @param key key to increment
   * @param by the amount to increment the value by
   * @param def the default value (if the counter does not exist)
   * @return a future with the incremented value, or -1 if the increment failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncIncr(String key, int by, long def) { return null; }

  /**
   * Asynchronous decrement.
   *
   * @param key key to decrement
   * @param by the amount to decrement the value by
   * @param def the default value (if the counter does not exist)
   * @return a future with the decremented value, or -1 if the decrement failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncDecr(String key, long by, long def) { return null; }

  /**
   * Asynchronous decrement.
   *
   * @param key key to decrement
   * @param by the amount to decrement the value by
   * @param def the default value (if the counter does not exist)
   * @return a future with the decremented value, or -1 if the decrement failed.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Long> asyncDecr(String key, int by, long def) { return null; }

  /**
   * Increment the given counter, returning the new value.
   *
   * @param key the key
   * @param by the amount to increment
   * @param def the default value (if the counter does not exist)
   * @return the new value, or -1 if we were unable to increment or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long incr(String key, long by, long def) { return 0; }

  /**
   * Increment the given counter, returning the new value.
   *
   * @param key the key
   * @param by the amount to increment
   * @param def the default value (if the counter does not exist)
   * @return the new value, or -1 if we were unable to increment or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long incr(String key, int by, long def) { return 0; }

  /**
   * Decrement the given counter, returning the new value.
   *
   * @param key the key
   * @param by the amount to decrement
   * @param def the default value (if the counter does not exist)
   * @return the new value, or -1 if we were unable to decrement or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long decr(String key, long by, long def) { return 0; }

  /**
   * Decrement the given counter, returning the new value.
   *
   * @param key the key
   * @param by the amount to decrement
   * @param def the default value (if the counter does not exist)
   * @return the new value, or -1 if we were unable to decrement or add
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public long decr(String key, int by, long def) { return 0; }

  /**
   * Delete the given key from the cache.
   *
   * <p>
   * The hold argument specifies the amount of time in seconds (or Unix time
   * until which) the client wishes the server to refuse "add" and "replace"
   * commands with this key. For this amount of item, the item is put into a
   * delete queue, which means that it won't possible to retrieve it by the
   * "get" command, but "add" and "replace" command with this key will also fail
   * (the "set" command will succeed, however). After the time passes, the item
   * is finally deleted from server memory.
   * </p>
   *
   * @param key the key to delete
   * @param hold how long the key should be unavailable to add commands
   *
   * @return whether or not the operation was performed
   * @deprecated Hold values are no longer honored.
   */
  @Deprecated
  public OperationFuture<Boolean> delete(String key, int hold) { return null; }

  /**
   * Delete the given key from the cache.
   *
   * @param key the key to delete
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> delete(String key) { return null; }

  /**
   * Delete the given key from the cache of the given CAS value applies.
   *
   * @param key the key to delete
   * @param cas the CAS value to apply.
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> delete(String key, long cas) { return null; }

  /**
   * Flush all caches from all servers with a delay of application.
   *
   * @param delay the period of time to delay, in seconds
   * @return whether or not the operation was accepted
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> flush(final int delay) { return null; }

  /**
   * Flush all caches from all servers immediately.
   *
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public OperationFuture<Boolean> flush() { return null; }

  @Override
  public Set<String> listSaslMechanisms() { return null; }

  /**
   * Shut down immediately.
   */
  @Override
  public void shutdown() {}

  /**
   * Shut down this client gracefully.
   *
   * @param timeout the amount of time time for shutdown
   * @param unit the TimeUnit for the timeout
   * @return result of the shutdown request
   */
  @Override
  public boolean shutdown(long timeout, TimeUnit unit) { return false; }

  /**
   * Wait for the queues to die down.
   *
   * @param timeout the amount of time time for shutdown
   * @param unit the TimeUnit for the timeout
   * @return result of the request for the wait
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  @Override
  public boolean waitForQueues(long timeout, TimeUnit unit) { return false; }

  /**
   * Add a connection observer.
   *
   * If connections are already established, your observer will be called with
   * the address and -1.
   *
   * @param obs the ConnectionObserver you wish to add
   * @return true if the observer was added.
   */
  @Override
  public boolean addObserver(ConnectionObserver obs) { return false; }

  /**
   * Remove a connection observer.
   *
   * @param obs the ConnectionObserver you wish to add
   * @return true if the observer existed, but no longer does
   */
  @Override
  public boolean removeObserver(ConnectionObserver obs) { return false; }

  @Override
  public void connectionEstablished(SocketAddress sa, int reconnectCount) {}

  private MemcachedNode findNode(SocketAddress sa) { return null; }

  private String buildTimeoutMessage(long timeWaited, TimeUnit unit) { return null; }

  @Override
  public void connectionLost(SocketAddress sa) {}

  @Override
  public String toString() { return null; }
}

