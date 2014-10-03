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
 * The object interacted with by client objects to perform logging.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @author Peter Donald
 */
public class Logger
{
    private static final Logger[] EMPTY_SET = new Logger[ 0 ];

    /**
     * Separator character use to separate different categories
     */
    public static final char CATEGORY_SEPARATOR = '.';

    /**
     * Protected constructor for use inside the logging toolkit.
     * You should not be using this constructor directly.
     *
     * @param errorHandler the ErrorHandler logger uses to log errors
     * @param category the fully qualified name of category
     * @param logTargets the LogTargets associated with logger
     * @param parent the parent logger (used for inheriting from)
     */
    Logger( final ErrorHandler errorHandler,
            final LoggerListener loggerListener,
            final String category,
            final LogTarget[] logTargets,
            final Logger parent )
    {
    }

    /**
     * Determine if messages of priority DEBUG will be logged.
     *
     * @return true if DEBUG messages will be logged
     */
    public final boolean isDebugEnabled()
    {
		return false;
    }

    /**
     * Log a debug priority event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public final void debug( final String message, final Throwable throwable )
    {
    }

    /**
     * Log a debug priority event.
     *
     * @param message the message
     */
    public final void debug( final String message )
    {
    }

    /**
     * Determine if messages of priority INFO will be logged.
     *
     * @return true if INFO messages will be logged
     */
    public final boolean isInfoEnabled()
    {
		return false;
    }

    /**
     * Log a info priority event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public final void info( final String message, final Throwable throwable )
    {
    }

    /**
     * Log a info priority event.
     *
     * @param message the message
     */
    public final void info( final String message )
    {
    }

    /**
     * Determine if messages of priority WARN will be logged.
     *
     * @return true if WARN messages will be logged
     */
    public final boolean isWarnEnabled()
    {
		return false;
    }

    /**
     * Log a warn priority event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public final void warn( final String message, final Throwable throwable )
    {
    }

    /**
     * Log a warn priority event.
     *
     * @param message the message
     */
    public final void warn( final String message )
    {
    }

    /**
     * Determine if messages of priority ERROR will be logged.
     *
     * @return true if ERROR messages will be logged
     */
    public final boolean isErrorEnabled()
    {
		return false;
    }

    /**
     * Log a error priority event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public final void error( final String message, final Throwable throwable )
    {
    }

    /**
     * Log a error priority event.
     *
     * @param message the message
     */
    public final void error( final String message )
    {
    }

    /**
     * Determine if messages of priority FATAL_ERROR will be logged.
     *
     * @return true if FATAL_ERROR messages will be logged
     */
    public final boolean isFatalErrorEnabled()
    {
		return false;
    }

    /**
     * Log a fatalError priority event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public final void fatalError( final String message, final Throwable throwable )
    {
    }

    /**
     * Log a fatalError priority event.
     *
     * @param message the message
     */
    public final void fatalError( final String message )
    {
    }

    /**
     * Make this logger additive. I.e. Send all log events to parent
     * loggers LogTargets regardless of whether or not the
     * LogTargets have been overidden.
     *
     * This is derived from Log4js notion of Additivity.
     *
     * @param additivity true to make logger additive, false otherwise
     */
    public final void setAdditivity( final boolean additivity )
    {
    }

    /**
     * Determine if messages of priority ï¿½will be logged.
     * @param priority the priority
     * @return true if messages will be logged
     */
    public final boolean isPriorityEnabled( final Priority priority )
    {
		return false;
    }

    /**
     * Log a event at specific priority with a certain message and throwable.
     *
     * @param priority the priority
     * @param message the message
     * @param throwable the throwable
     */
    public final void log( final Priority priority,
                           final String message,
                           final Throwable throwable )
    {
    }

    /**
     * Log a event at specific priority with a certain message.
     *
     * @param priority the priority
     * @param message the message
     */
    public final void log( final Priority priority, final String message )
    {
    }

    /**
     * Set the priority for this logger.
     *
     * @param priority the priority
     */
    public synchronized void setPriority( final Priority priority )
    {
    }

    /**
     * Unset the priority of Logger.
     * (Thus it will use it's parent's priority or DEBUG if no parent.
     */
    public synchronized void unsetPriority()
    {
    }

    /**
     * Unset the priority of Logger.
     * (Thus it will use it's parent's priority or DEBUG if no parent.
     * If recursive is true unset priorities of all child loggers.
     *
     * @param recursive true to unset priority of all child loggers
     */
    public synchronized void unsetPriority( final boolean recursive )
    {
    }

    /**
     * Set the log targets for this logger.
     *
     * @param logTargets the Log Targets
     */
    public synchronized void setLogTargets( final LogTarget[] logTargets )
    {
    }

    /**
     * Unset the logtargets for this logger.
     * This logger (and thus all child loggers who don't specify logtargets) will
     * inherit from the parents LogTargets.
     */
    public synchronized void unsetLogTargets()
    {
    }

    /**
     * Unset the logtargets for this logger and all child loggers if recursive is set.
     * The loggers unset (and all child loggers who don't specify logtargets) will
     * inherit from the parents LogTargets.
     * @param recursive the recursion policy
     */
    public synchronized void unsetLogTargets( final boolean recursive )
    {
    }

    /**
     * Get all the child Loggers of current logger.
     *
     * @return the child loggers
     */
    public synchronized Logger[] getChildren()
    {
		return null;
    }

    /**
     * Create a new child logger.
     * The category of child logger is [current-category].subcategory
     *
     * @param subCategory the subcategory of this logger
     * @return the new logger
     * @exception IllegalArgumentException if subCategory has an empty element name
     */
    public synchronized Logger getChildLogger( final String subCategory )
        throws IllegalArgumentException
    {
		return null;
    }

    /**
     * Internal method to do actual outputting.
     *
     * @param priority the priority
     * @param message the message
     * @param throwable the throwable
     */
    private final void output( final Priority priority,
                               final String message,
                               final Throwable throwable )
    {
    }

    private final void output( final LogEvent event )
    {
    }

    private final void fireEvent( final LogEvent event, final LogTarget[] targets )
    {
    }

    /**
     * Update priority of children if any.
     */
    private synchronized void resetChildPriorities( final boolean recursive )
    {
    }

    /**
     * Update priority of this Logger.
     * If this loggers priority was manually set then ignore
     * otherwise get parents priority and update all children's priority.
     *
     */
    private synchronized void resetPriority( final boolean recursive )
    {
    }

    /**
     * Retrieve logtarget array contained in logger.
     * This method is provided so that child Loggers can access a
     * copy of  parents LogTargets.
     *
     * @return the array of LogTargets
     */
    private synchronized LogTarget[] safeGetLogTargets()
    {
		return null;
    }

    /**
     * Update logTargets of children if any.
     */
    private synchronized void resetChildLogTargets( final boolean recursive )
    {
    }

    /**
     * Set ErrorHandlers of LogTargets if necessary.
     */
    private synchronized void setupErrorHandlers()
    {
    }

    /**
     * Update logTarget of this Logger.
     * If this loggers logTarget was manually set then ignore
     * otherwise get parents logTarget and update all children's logTarget.
     *
     */
    private synchronized void resetLogTargets( final boolean recursive )
    {
    }
}

