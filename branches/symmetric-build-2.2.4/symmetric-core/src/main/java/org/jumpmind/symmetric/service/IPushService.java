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
 * under the License. 
 */

package org.jumpmind.symmetric.service;

import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.NodeGroupLinkAction;
import org.jumpmind.symmetric.model.RemoteNodeStatuses;

/**
 * Service API that is responsible for pushing data to the list of configured
 * {@link Node}s that are configured to {@link NodeGroupLinkAction#P}
 */
public interface IPushService {

    /**
     * Attempt to push data, if any has been captured, to nodes that the
     * captured data is targeted for.
     * 
     * @return RemoteNodeStatuses the status of the push attempt(s)
     */
    public RemoteNodeStatuses pushData();

}