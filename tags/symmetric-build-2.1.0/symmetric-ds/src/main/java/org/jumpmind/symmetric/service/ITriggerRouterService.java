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


package org.jumpmind.symmetric.service;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.config.ITriggerCreationListener;
import org.jumpmind.symmetric.model.NodeGroupLink;
import org.jumpmind.symmetric.model.Router;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;
import org.jumpmind.symmetric.model.TriggerRouter;

/**
 * Provides an API to configure {@link TriggerRouter}s
 *
 * ,
 */
public interface ITriggerRouterService {

    /**
     * Return a list of triggers used when extraction configuration data during 
     * the registration process.
     * @param sourceGroupId group id of the node being registered with
     * @param targetGroupId group id of the node that is registering
     */
    public List<TriggerRouter> getTriggerRoutersForRegistration(String version,String sourceGroupId, String targetGroupId);
    
    public Map<String, List<TriggerRouter>> getTriggerRoutersByChannel(String configurationTypeId);

    public Map<String, List<TriggerRouter>> getTriggerRoutersForCurrentNode(boolean refreshCache);

    /**
     * Get router that is currently in use by a trigger router at the node that is hosting this call.
     * @param routerId The router_id to retrieve
     * @param refreshCache Whether to force the router to be re-retrieved from the database
     */
    public Router getActiveRouterByIdForCurrentNode(String routerId, boolean refreshCache);
    
    public Router getRouterById(String routerId);
    
    public List<TriggerRouter> getAllTriggerRoutersForCurrentNode(String sourceNodeGroupId);
    
    public List<TriggerRouter> getAllTriggerRoutersForReloadForCurrentNode(String sourceNodeGroupId, String targetNodeGroupId);

    public TriggerRouter getTriggerRouterForTableForCurrentNode(NodeGroupLink link, String catalogName, String schemaName, String tableName, boolean refreshCache);
    
    public TriggerRouter getTriggerRouterForTableForCurrentNode(String catalog, String schema, String tableName, boolean refreshCache); 

    public TriggerRouter findTriggerRouterById(String triggerId, String routerId);

    public void inactivateTriggerHistory(TriggerHistory history);

    public TriggerHistory getNewestTriggerHistoryForTrigger(String triggerId);

    public TriggerHistory getTriggerHistory(int historyId);
    
    public TriggerHistory findTriggerHistory(String sourceTableName);
    
    public Trigger getTriggerById(String triggerId);

    public void insert(TriggerHistory newAuditRecord);

    public Map<Long, TriggerHistory> getHistoryRecords();

    public void saveTriggerRouter(TriggerRouter trigger);
    
    public void saveRouter(Router router);
    
    public void saveTrigger(Trigger trigger);
    
    public void syncTriggers();

    public void syncTriggers(StringBuilder sqlBuffer, boolean gen_always);
    
    public void addTriggerCreationListeners(ITriggerCreationListener l);

    public Map<Trigger, Exception> getFailedTriggers();

}