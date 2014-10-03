/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This class encapsulates each individual log event.
 * LogEvents usually originate at a Logger and are routed
 * to LogTargets.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @author Peter Donald
 */
public final class LogEvent
    implements Serializable
{

    /**
     * Get Priority for LogEvent.
     *
     * @return the LogEvent Priority
     */
    public final Priority getPriority()
    {
        return null;
    }

    /**
     * Set the priority of LogEvent.
     *
     * @param priority the new LogEvent priority
     */
    public final void setPriority( final Priority priority )
    {
    }

    /**
     * Get ContextMap associated with LogEvent
     *
     * @return the ContextMap
     */
    public final ContextMap getContextMap()
    {
        return null;
    }

    /**
     * Set the ContextMap for this LogEvent.
     *
     * @param contextMap the context map
     */
    public final void setContextMap( final ContextMap contextMap )
    {
    }

    /**
     * Get the category that LogEvent relates to.
     *
     * @return the name of category
     */
    public final String getCategory()
    {
		return null;
    }

    /**
     * Get the message associated with event.
     *
     * @return the message
     */
    public final String getMessage()
    {
		return null;
    }

    /**
     * Get throwabe instance associated with event.
     *
     * @return the Throwable
     */
    public final Throwable getThrowable()
    {
		return null;
    }

    /**
     * Get the absolute time of the log event.
     *
     * @return the absolute time
     */
    public final long getTime()
    {
		return 0;
    }

    /**
     * Get the time of the log event relative to start of application.
     *
     * @return the time
     */
    public final long getRelativeTime()
    {
		return 0;
    }

    /**
     * Set the LogEvent category.
     *
     * @param category the category
     */
    public final void setCategory( final String category )
    {
    }

    /**
     * Set the message for LogEvent.
     *
     * @param message the message
     */
    public final void setMessage( final String message )
    {
    }

    /**
     * Set the throwable for LogEvent.
     *
     * @param throwable the instance of Throwable
     */
    public final void setThrowable( final Throwable throwable )
    {
    }

    /**
     * Set the absolute time of LogEvent.
     *
     * @param time the time
     */
    public final void setTime( final long time )
    {
    }

    /**
     * Helper method that replaces deserialized priority with correct singleton.
     *
     * @return the singleton version of object
     * @exception ObjectStreamException if an error occurs
     */
    private Object readResolve()
        throws ObjectStreamException
    {
        return null;
    }
}

