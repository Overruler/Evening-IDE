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
package org.apache.log.util;

import org.apache.log.Logger;

/**

 * The LoggerListener class is used to notify listeners

 * when a new Logger object is created. Loggers are created

 * when a client requests a new Logger via {@link Logger#getChildLogger}.

 *

 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @author Peter Donald

 */

public abstract class LoggerListener

{

    /**

     * Notify listener that Logger was created.

     *

     * @param category the error message

     * @param logger the logger that was created

     */

    public abstract void loggerCreated( String category, Logger logger );

}


