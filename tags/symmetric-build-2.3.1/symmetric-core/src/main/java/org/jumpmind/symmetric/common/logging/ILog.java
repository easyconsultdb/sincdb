/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.  */

package org.jumpmind.symmetric.common.logging;


/**
 * 
 */
public interface ILog {

    public abstract boolean isDebugEnabled();

    public abstract void debug(String messageKey);

    public abstract void debug(String messageKey, Throwable t);

    public abstract void debug(String messageKey, Object... args);

    public abstract void debug(String messageKey, Throwable t, Object... args);

    public abstract boolean isInfoEnabled();

    public abstract void info(String messageKey);

    public abstract void info(String messageKey, Throwable t);

    public abstract void info(String messageKey, Object... args);

    public abstract void info(String messageKey, Throwable t, Object... args);

    public abstract boolean isWarnEnabled();

    public abstract void warn(String messageKey);

    public abstract void warn(String messageKey, Throwable t);

    public abstract void warn(String messageKey, Object... args);

    public abstract void warn(String messageKey, Throwable t, Object... args);

    public abstract void warn(Throwable t);

    public abstract boolean isErrorEnabled();

    public abstract void error(String messageKey);

    public abstract void error(String messageKey, Throwable t);

    public abstract void error(String messageKey, Object... args);

    public abstract void error(String messageKey, Throwable t, Object... args);

    public abstract void error(Throwable t);

    public abstract void fatal(String messageKey);

    public abstract void fatal(String messageKey, Throwable t);

    public abstract void fatal(String messageKey, Object... args);

    public abstract void fatal(String messageKey, Throwable t, Object... args);

    public abstract void fatal(Throwable t);

}