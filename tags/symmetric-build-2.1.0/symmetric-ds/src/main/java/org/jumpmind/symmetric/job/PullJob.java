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


package org.jumpmind.symmetric.job;

import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IPullService;

/**
 * 
 */
public class PullJob extends AbstractJob {

    private IPullService pullService;
    
    private INodeService nodeService;

    @Override
    public void doJob() throws Exception {
        boolean dataPulled = pullService.pullData();

        // Re-pull immediately if we are in the middle of an initial load
        // so that the initial load completes as quickly as possible.
        while (nodeService.isDataLoadStarted() && dataPulled) {
            dataPulled = pullService.pullData();
        }
    }

    public void setPullService(IPullService service) {
        this.pullService = service;
    }

    public void setNodeService(INodeService nodeService) {
        this.nodeService = nodeService;
    }
}