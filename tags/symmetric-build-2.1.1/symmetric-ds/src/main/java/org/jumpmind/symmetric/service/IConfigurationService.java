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


package org.jumpmind.symmetric.service;

import java.util.List;

import org.jumpmind.symmetric.model.Channel;
import org.jumpmind.symmetric.model.ChannelMap;
import org.jumpmind.symmetric.model.NodeChannel;
import org.jumpmind.symmetric.model.NodeGroupChannelWindow;
import org.jumpmind.symmetric.model.NodeGroupLink;
import org.jumpmind.symmetric.model.NodeGroupLinkAction;

/**
 * Provides an API to configure data synchronizations.
 *
 * ,
 */
public interface IConfigurationService {

    public List<NodeGroupLink> getGroupLinks();

    public List<NodeGroupLink> getGroupLinksFor(String sourceGroupId);
    
    public List<NodeGroupLink> getGroupLinksFor(String sourceNodeGroupId, String targetNodeGroupId);

    public void saveChannel(Channel channel, boolean reloadChannels);

    public void saveChannel(NodeChannel channel, boolean reloadChannels);

    public void saveNodeChannel(NodeChannel channel, boolean reloadChannels);

    public void saveNodeChannelControl(NodeChannel channel, boolean reloadChannels);

    public void deleteChannel(Channel channel);

    public List<NodeGroupChannelWindow> getNodeGroupChannelWindows(String nodeGroupId, String channelId);

    public NodeGroupLinkAction getDataEventActionsByGroupId(String sourceGroupId, String targetGroupId);

    public List<NodeChannel> getNodeChannels(boolean refreshExtractMillis);

    public List<NodeChannel> getNodeChannels(String nodeId, boolean refreshExtractMillis);

    public NodeChannel getNodeChannel(String channelId, boolean refreshExtractMillis);

    public NodeChannel getNodeChannel(String channelId, String nodeId, boolean refreshExtractMillis);

    public void reloadChannels();
    
    public void autoConfigDatabase(boolean force);

    /**
     * Returns two sets of channel names, one for suspended channels and one for
     * ignored.
     * 
     * @param nodeId
     * @return A Map with two entries, the sets of which will always be defined
     *         but may be empty.
     */
    public ChannelMap getSuspendIgnoreChannelLists(String nodeId);

    public ChannelMap getSuspendIgnoreChannelLists();

}