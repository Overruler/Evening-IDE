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

import org.apache.log.util.LoggerListener;

/**
 * This class encapsulates a basic independent log hierarchy.
 * The hierarchy is essentially a safe wrapper around root logger.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @author Peter Donald
 */
public class Hierarchy
{
    ///Format of default formatter
    public static final String DEFAULT_FORMAT =
        "%7.7{priority} %23.23{time:yyyy-MM-dd' 'HH:mm:ss.SSS} " +
        "[%8.8{category}] (%{context}): %{message}\n%{throwable}";

    /**
     * Retrieve the default hierarchy.
     *
     * <p>In most cases the default LogHierarchy is the only
     * one used in an application. However when security is
     * a concern or multiple independent applications will
     * be running in same JVM it is advantageous to create
     * new Hierarchies rather than reuse default.</p>
     *
     * @return the default Hierarchy
     */
    public static Hierarchy getDefaultHierarchy()
    {
        return null;
    }

    /**
     * Create a hierarchy object.
     * The default LogTarget writes to stdout.
     */
    public Hierarchy()
    {
    }

    /**
     * Set the default log target for hierarchy.
     * This is the target inherited by loggers if no other target is specified.
     *
     * @param target the default target
     */
    public void setDefaultLogTarget( final LogTarget target )
    {
    }

    /**
     * Set the default log targets for this hierarchy.
     * These are the targets inherited by loggers if no other targets are specified
     *
     * @param targets the default targets
     */
    public void setDefaultLogTargets( final LogTarget[] targets )
    {
    }

    /**
     * Set the default priority for hierarchy.
     * This is the priority inherited by loggers if no other priority is specified.
     *
     * @param priority the default priority
     */
    public void setDefaultPriority( final Priority priority )
    {
    }

    /**
     * Set the ErrorHandler associated with hierarchy.
     *
     * @param errorHandler the ErrorHandler
     */
    public void setErrorHandler( final ErrorHandler errorHandler )
    {
    }

    /**
     * Set the LoggerListener associated with hierarchy.  This is a
     * unicast listener, so only one LoggerListener is allowed.
     *
     * @param loggerListener the LoggerListener
     *
     * @throws UnsupportedOperationException if no more LoggerListeners are
     *         permitted.
     */
    public synchronized void addLoggerListener( final LoggerListener loggerListener )
    {
    }

    /**
     * Remove the LoggerListener associated with hierarchy.  Perform this
     * step before adding a new one if you want to change it.
     *
     * @param loggerListener the LoggerListener
     */
    public synchronized void removeLoggerListener( final LoggerListener loggerListener )
    {
    }

    /**
     * Retrieve a logger for named category.
     *
     * @param category the context
     * @return the Logger
     */
    public Logger getLoggerFor( final String category )
    {
        return null;
    }

    /**
     * Notify logger listener (if any) that a new logger was created.
     *
     * @param category the category of new logger
     * @param logger the logger
     */
    private synchronized void notifyLoggerCreated( final String category,
                                                   final Logger logger )
    {
    }

    /**
     * Utility method to retrieve logger for hierarchy.
     * This method is intended for use by sub-classes
     * which can take responsibility for manipulating
     * Logger directly.
     *
     * @return the Logger
     */
    public final Logger getRootLogger()
    {
        return null;
    }
}

