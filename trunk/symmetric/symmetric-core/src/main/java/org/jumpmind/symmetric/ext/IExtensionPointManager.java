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

package org.jumpmind.symmetric.ext;

import java.util.List;

import org.jumpmind.extension.IExtensionPoint;
import org.springframework.beans.BeansException;

/**
 * An API that is responsible for finding and registering available
 * extension points.
 */
public interface IExtensionPointManager {

    public void register() throws BeansException;
    
    public List<ExtensionPointMetaData> getExtensionPoints();
    
    public <T extends IExtensionPoint> T getExtensionPoint(String name);
    
}