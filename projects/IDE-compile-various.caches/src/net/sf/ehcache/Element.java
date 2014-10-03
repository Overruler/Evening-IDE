/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package net.sf.ehcache;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import net.sf.ehcache.config.CacheConfiguration;

/**
 * A Cache Element, consisting of a key, value and attributes.
 * <p/>
 * From ehcache-1.2, Elements can have keys and values that are Serializable or Objects. To preserve backward
 * compatibility, special accessor methods for Object keys and values are provided: {@link #getObjectKey()} and
 * {@link #getObjectValue()}. If placing Objects in ehcache, developers must use the new getObject... methods to
 * avoid CacheExceptions. The get... methods are reserved for Serializable keys and values.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class Element implements Serializable, Cloneable {

    /**
     * A full constructor.
     * <p/>
     * Creation time is set to the current time. Last Access Time is not set.
     *
     * @since .4
     */
    public Element(final Serializable key, final Serializable value, final long version) {}

    /**
     * A full constructor.
     * <p/>
     * Creation time is set to the current time. Last Access Time and Previous To Last Access Time
     * are not set.
     *
     * @since 1.2
     */
    public Element(final Object key, final Object value, final long version) {}

    /**
     * Constructor.
     *
     * @deprecated The {@code nextToLastAccessTime} field is unused since
     *             version 1.7, setting it will have no effect. Use
     *             #Element(Object, Object, long, long, long, long, long)
     *             instead
     * @since 1.3
     * @see #Element(Object, Object, long, long, long, long, long)
     */
    @Deprecated
    public Element(final Object key, final Object value, final long version,
                   final long creationTime, final long lastAccessTime, final long nextToLastAccessTime,
                   final long lastUpdateTime, final long hitCount) {}

    /**
     * Constructor.
     *
     * @since 1.7
     */
    public Element(final Object key, final Object value, final long version,
                   final long creationTime, final long lastAccessTime,
                   final long lastUpdateTime, final long hitCount) {}

    /**
     * Constructor used by ElementData. Needs to be public since ElementData might be in another classloader
     *
     * @since 1.7
     */
    public Element(final Object key, final Object value, final long version, final long creationTime,
            final long lastAccessTime, final long hitCount, final boolean cacheDefaultLifespan,
            final int timeToLive, final int timeToIdle, final long lastUpdateTime) {}

    /**
     * Constructor
     *
     * @param key               any non null value
     * @param value             any value, including nulls
     * @param timeToIdleSeconds seconds to idle
     * @param timeToLiveSeconds seconds to live
     * @since 2.7.1
     */
    public Element(final Object key, final Object value,
                   final int timeToIdleSeconds, final int timeToLiveSeconds) {}

    /**
     * Constructor
     *
     * @param key               any non null value
     * @param value             any value, including nulls
     * @param eternal           whether this element is eternal
     * @since 2.7.1
     */
    public Element(final Object key, final Object value, final boolean eternal) {}

    /**
     * Constructor used by ehcache-server
     *
     * timeToIdleSeconds and timeToLiveSeconds will have precedence over eternal. Which means that what ever eternal says, non-null
     * timeToIdleSeconds or timeToLiveSeconds will result in the element not being eternal
     *
     * @param key               any non null value
     * @param value             any value, including nulls
     * @param eternal           specify as non-null to override cache configuration
     * @param timeToIdleSeconds specify as non-null to override cache configuration
     * @param timeToLiveSeconds specify as non-null to override cache configuration
     *
     * @deprecated
     */
    @Deprecated
    public Element(final Object key, final Object value,
                   final Boolean eternal, final Integer timeToIdleSeconds, final Integer timeToLiveSeconds) {}

    /**
     * Constructor.
     *
     * @param key
     * @param value
     */
    public Element(final Serializable key, final Serializable value) {}

    /**
     * Constructor.
     *
     * @param key
     * @param value
     * @since 1.2
     */
    public Element(final Object key, final Object value) {}

    /**
     * Gets the key attribute of the Element object.
     *
     * @return The key value.
     * @throws CacheException if the key is not {@code Serializable}.
     * @deprecated Please use {@link #getObjectKey()} instead.
     */
    @Deprecated
    public final Serializable getKey() throws CacheException {
		return null;
	}

    /**
     * Gets the key attribute of the Element object.
     * <p/>
     * This method is provided for those wishing to use ehcache as a memory only cache
     * and enables retrieval of non-Serializable values from elements.
     *
     * @return The key as an Object. i.e no restriction is placed on it
     * @see #getKey()
     */
    public final Object getObjectKey() {
		return null;
	}

    /**
     * Gets the value attribute of the Element object.
     *
     * @return The value which must be {@code Serializable}. If not use {@link #getObjectValue}.
     * @throws CacheException if the value is not {@code Serializable}.
     * @deprecated Please use {@link #getObjectValue()} instead.
     */
    @Deprecated
    public final Serializable getValue() throws CacheException {
		return null;
	}

    /**
     * Gets the value attribute of the Element object as an Object.
     * <p/>
     * This method is provided for those wishing to use ehcache as a memory only cache
     * and enables retrieval of non-Serializable values from elements.
     *
     * @return The value as an Object.  i.e no restriction is placed on it
     * @see #getValue()
     * @since 1.2
     */
    public final Object getObjectValue() {
		return null;
	}

    /**
     * Equals comparison with another element, based on the key.
     */
    @Override
    public final boolean equals(final Object object) {
		return false;
	}

    /**
     * Sets time to Live
     * <P/>
     * Value must be a positive integer, 0 means infinite time to live.
     * <P/>
     * If calling this method with 0 as the parameter, consider using {@link #setEternal(boolean)}
     * or make sure you also explicitly call {@link #setTimeToIdle(int)}.
     *
     * @param timeToLiveSeconds the number of seconds to live
     */
    public void setTimeToLive(final int timeToLiveSeconds) {}

    /**
     * Sets time to idle
     * <P/>
     * Value must be a positive integer, 0 means infinite time to idle.
     * <P/>
     * If calling this method with 0 as the parameter, consider using {@link #setEternal(boolean)}
     * or make sure you also explicitly call {@link #setTimeToLive(int)}.
     *
     * @param timeToIdleSeconds the number of seconds to idle
     */
    public void setTimeToIdle(final int timeToIdleSeconds) {}

    /**
     * Gets the hashcode, based on the key.
     */
    @Override
    public final int hashCode() {
		return 0;
	}

    /**
     * Sets the version attribute of the ElementAttributes object.
     *
     * @param version The new version value
     */
    public final void setVersion(final long version) {}

    /**
     * Sets the element identifier (this field is used internally by ehcache). Setting this field in application code will not be preserved
     *
     * @param id The new id value
     */
    void setId(final long id) {}

    /**
     * Gets the element identifier (this field is used internally by ehcache)
     *
     * @return id the id
     */
    long getId() {
		return 0;
	}

    /**
     * Determines if an Id has been set on this element
     *
     * @return true if this element has an Id
     */
    boolean hasId() {
		return false;
	}

    /**
     * Sets the creationTime attribute of the ElementAttributes object.
     * <p>
     * Note that in a Terracotta clustered environment, resetting the creation
     * time will not have any effect.
     *
     * @deprecated Resetting the creation time is not recommended as of version
     *             1.7
     */
    @Deprecated
    public final void setCreateTime() {}

    /**
     * Gets the creationTime of the Element
     *
     * @return The creationTime value
     */
    public final long getCreationTime() {
		return 0;
	}

    /**
     * Calculates the latest of creation and update time
     * @return if never updated, creation time is returned, otherwise updated time
     */
    public final long getLatestOfCreationAndUpdateTime() {
		return 0;
	}

    /**
     * Gets the version attribute of the ElementAttributes object.
     *
     * @return The version value
     */
    public final long getVersion() {
		return 0;
	}

    /**
     * Gets the last access time of this element.
     * <p/>
     * Access means the element was written into a cache or read from it.
     * When first instantiated an {@link Element} has a lastAccessTime of 0, unless passed into the constructor.
     *
     * @see #Element(Object, Object, long, long, long, long, boolean, int, int, long)
     * @see #Element(Object, Object, long, long, long, long, long)
     * @see #resetAccessStatistics()
     * @see #updateAccessStatistics()
     */
    public final long getLastAccessTime() {
		return 0;
	}

    /**
     * Gets the next to last access time.
     *
     * @deprecated The {@code nextToLastAccessTime} field is unused since
     *             version 1.7, retrieving it will return the {@code
     *             lastAccessTime}. Use #getLastAccessTime() instead.
     * @see #getLastAccessTime()
     */
    @Deprecated
    public final long getNextToLastAccessTime() {
		return 0;
	}

    /**
     * Gets the hit count on this element.
     */
    public final long getHitCount() {
		return 0;
	}

    /**
     * Resets the hit count to 0 and the last access time to now. Used when an Element is put into a cache.
     */
    public final void resetAccessStatistics() {}

    /**
     * Sets the last access time to now and increase the hit count.
     */
    public final void updateAccessStatistics() {}

    /**
     * Sets the last access time to now without updating the hit count.
     */
    public final void updateUpdateStatistics() {}


    /**
     * Returns a {@link String} representation of the {@link Element}.
     */
    @Override
    public final String toString() {
		return null;
	}

    /**
     * Clones an Element. A completely new object is created, with no common references with the
     * existing one.
     * <p/>
     * This method will not work unless the Object is Serializable
     * <p/>
     * Warning: This can be very slow on large object graphs. If you use this method
     * you should write a performance test to verify suitability.
     *
     * @return a new {@link Element}, with exactly the same field values as the one it was cloned from.
     * @throws CloneNotSupportedException
     */
    @Override
    public final Object clone() throws CloneNotSupportedException {
		return null;
	}

    private static Object deepCopy(final Object oldValue) throws IOException, ClassNotFoundException {
		return null;
	}

    /**
     * The size of this object in serialized form. This is not the same
     * thing as the memory size, which is JVM dependent. Relative values should be meaningful,
     * however.
     * <p/>
     * Warning: This method can be <b>very slow</b> for values which contain large object graphs.
     * <p/>
     * If the key or value of the Element is not Serializable, an error will be logged and 0 will be returned.
     *
     * @return The serialized size in bytes
     */
    public final long getSerializedSize() {
		return 0;
	}

    /**
     * Whether the element may be Serialized.
     * <p/>
     * While Element implements Serializable, it is possible to create non Serializable elements
     * for use in MemoryStores. This method checks that an instance of Element really is Serializable
     * and will not throw a NonSerializableException if Serialized.
     * <p/>
     * This method was tweaked in 1.6 as it has been shown that Serializable classes can be serializaed as can
     * null, regardless of what class it is a null of. ObjectOutputStream.write(null) works and ObjectInputStream.read()
     * will read null back.
     *
     * @return true if the element is Serializable
     * @since 1.2
     */
    public final boolean isSerializable() {
		return false;
	}

    /**
     * Whether the element's key may be Serialized.
     * <p/>
     * While Element implements Serializable, it is possible to create non Serializable elements and/or
     * non Serializable keys for use in MemoryStores.
     * <p/>
     * This method checks that an instance of an Element's key really is Serializable
     * and will not throw a NonSerializableException if Serialized.
     *
     * @return true if the element's key is Serializable
     * @since 1.2
     */
    public final boolean isKeySerializable() {
		return false;
	}

    /**
     * If there is an Element in the Cache and it is replaced with a new Element for the same key,
     * then both the version number and lastUpdateTime should be updated to reflect that. The creation time
     * will be the creation time of the new Element, not the original one, so that TTL concepts still work.
     *
     * @return the time when the last update occured. If this is the original Element, the time will be null
     */
    public long getLastUpdateTime() {
		return 0;
	}

    /**
     * An element is expired if the expiration time as given by {@link #getExpirationTime()} is in the past.
     *
     * @return true if the Element is expired, otherwise false. If no lifespan has been set for the Element it is
     *         considered not able to expire.
     * @see #getExpirationTime()
     */
    public boolean isExpired() {
		return false;
	}

    /**
     * An element is expired if the expiration time as given by {@link #getExpirationTime()} is in the past.
     * <p>
     * This method in addition propogates the default TTI/TTL values of the supplied cache into this element.
     *
     * @param config config to take default parameters from
     * @return true if the Element is expired, otherwise false. If no lifespan has been set for the Element it is
     *         considered not able to expire.
     * @see #getExpirationTime()
     */
    public boolean isExpired(CacheConfiguration config) {
		return false;
	}

    /**
     * Returns the expiration time based on time to live. If this element also has a time to idle setting, the expiry
     * time will vary depending on whether the element is accessed.
     *
     * @return the time to expiration
     */
    public long getExpirationTime() {
		return 0;
	}

    /**
     * @return true if the element is eternal
     */
    public boolean isEternal() {
		return false;
	}

    /**
     * Sets whether the element is eternal.
     *
     * @param eternal
     */
    public void setEternal(final boolean eternal) {}

    /**
     * Whether any combination of eternal, TTL or TTI has been set.
     *
     * @return true if set.
     */
    public boolean isLifespanSet() {
		return false;
	}

    /**
     * @return the time to live, in seconds
     */
    public int getTimeToLive() {
		return 0;
	}

    /**
     * @return the time to idle, in seconds
     */
    public int getTimeToIdle() {
		return 0;
	}

    /**
     * @return <code>false</code> if this Element has a custom lifespan
     */
    public boolean usesCacheDefaultLifespan() {
		return false;
	}

    /**
     * Set the default parameters of this element - those from its enclosing cache.
     * @param tti TTI in seconds
     * @param ttl TTL in seconds
     * @param eternal <code>true</code> if the element is eternal.
     */
    protected void setLifespanDefaults(int tti, int ttl, boolean eternal) {}

    /**
     * Seam for testing purposes
     * @return System.currentTimeMillis() by default
     */
    long getCurrentTime() {
		return 0;
	}

    /**
     * Custom serialization write logic
     */
    private void writeObject(ObjectOutputStream out) throws IOException {}

    /**
     * Custom serialization read logic
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {}
}

