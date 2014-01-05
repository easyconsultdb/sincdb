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
package org.jumpmind.symmetric.transport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.statistic.IStatisticManager;

public class ConcurrentConnectionManager implements IConcurrentConnectionManager {

    protected IParameterService parameterService;

    protected IStatisticManager statisticManager;

    protected Map<String, Map<String, Reservation>> activeReservationsByNodeByPool = new HashMap<String, Map<String, Reservation>>();

    protected Map<String, Map<String, NodeConnectionStatistics>> nodeConnectionStatistics = new HashMap<String, Map<String, NodeConnectionStatistics>>();

    protected Set<String> whiteList = new HashSet<String>();

    protected void logTooBusyRejection(String nodeId, String poolId) {
        getNodeConnectionStatistics(nodeId, poolId).numOfRejections++;
    }

    protected void logConnectedTimePeriod(String nodeId, long startMs, long endMs, String poolId) {
        NodeConnectionStatistics stats = getNodeConnectionStatistics(nodeId, poolId);
        stats.totalConnectionCount++;
        stats.totalConnectionTimeMs += endMs - startMs;
        stats.lastConnectionTimeMs = startMs;
    }

    private synchronized NodeConnectionStatistics getNodeConnectionStatistics(String nodeId, String poolId) {
        Map<String, NodeConnectionStatistics> statsMap = nodeConnectionStatistics.get(poolId);
        if (statsMap == null) {
            statsMap = new HashMap<String, NodeConnectionStatistics>();
            nodeConnectionStatistics.put(poolId, statsMap);
        }
        NodeConnectionStatistics stats = statsMap.get(nodeId);
        if (stats == null) {
            stats = new NodeConnectionStatistics();
            statsMap.put(nodeId, stats);
        }
        return stats;
    }

    synchronized public boolean releaseConnection(String nodeId, String poolId) {
        Map<String, Reservation> reservations = getReservationMap(poolId);
        Reservation reservation = reservations.remove(nodeId);
        if (reservation != null) {
            logConnectedTimePeriod(nodeId, reservation.createTime, System.currentTimeMillis(), poolId);
            return true;
        } else {
            return false;
        }
    }

    synchronized public void addToWhitelist(String nodeId) {
        whiteList.add(nodeId);
    }

    synchronized public void removeFromWhiteList(String nodeId) {
        whiteList.remove(nodeId);
    }

    synchronized public String[] getWhiteList() {
        return whiteList.toArray(new String[whiteList.size()]);
    }

    synchronized public int getReservationCount(String poolId) {
        return getReservationMap(poolId).size();
    }

    synchronized public boolean reserveConnection(String nodeId, String poolId, ReservationType reservationRequest) {
        Map<String, Reservation> reservations = getReservationMap(poolId);
        int maxPoolSize = parameterService.getInt(ParameterConstants.CONCURRENT_WORKERS);
        long timeout = parameterService.getLong(ParameterConstants.CONCURRENT_RESERVATION_TIMEOUT);
        removeTimedOutReservations(reservations);
        if (reservations.size() < maxPoolSize || reservations.containsKey(nodeId) || whiteList.contains(nodeId)) {
            reservations.put(nodeId, new Reservation(nodeId, reservationRequest == ReservationType.SOFT ? System
                    .currentTimeMillis()
                    + timeout : Long.MAX_VALUE));
            return true;
        } else {
            return false;
        }
    }

    protected void removeTimedOutReservations(Map<String, Reservation> reservations) {
        long currentTime = System.currentTimeMillis();
        String[] keys = reservations.keySet().toArray(new String[reservations.size()]);
        if (keys != null) {
            for (String nodeId : keys) {
                Reservation reservation = reservations.get(nodeId);
                if (reservation.timeToLiveInMs < currentTime) {
                    reservations.remove(nodeId);
                }                
            }
        }   
    }

    private Map<String, Reservation> getReservationMap(String poolId) {
        Map<String, Reservation> reservations = activeReservationsByNodeByPool.get(poolId);
        if (reservations == null) {
            reservations = new HashMap<String, Reservation>();
            activeReservationsByNodeByPool.put(poolId, reservations);
        }
        return reservations;
    }

    public void setParameterService(IParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public void setStatisticManager(IStatisticManager statisticManager) {
        this.statisticManager = statisticManager;
    }

    protected static class Reservation {
        String nodeId;
        long timeToLiveInMs;
        long createTime = System.currentTimeMillis();

        public Reservation(String nodeId, long timeToLiveInMs) {
            this.nodeId = nodeId;
            this.timeToLiveInMs = timeToLiveInMs;
        }

        @Override
        public int hashCode() {
            return nodeId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Reservation) {
                return nodeId.equals(((Reservation) obj).nodeId);
            } else {
                return false;
            }

        }

        public String getNodeId() {
            return nodeId;
        }

        public long getTimeToLiveInMs() {
            return timeToLiveInMs;
        }

        public long getCreateTime() {
            return createTime;
        }
    }

    public Map<String, Map<String, NodeConnectionStatistics>> getNodeConnectionStatisticsByPoolByNodeId() {
        return this.nodeConnectionStatistics;
    }

    public class NodeConnectionStatistics {

        int numOfRejections;
        long totalConnectionCount;
        long totalConnectionTimeMs;
        long lastConnectionTimeMs;

        public int getNumOfRejections() {
            return numOfRejections;
        }

        public long getTotalConnectionCount() {
            return totalConnectionCount;
        }

        public long getTotalConnectionTimeMs() {
            return totalConnectionTimeMs;
        }

        public long getLastConnectionTimeMs() {
            return lastConnectionTimeMs;
        }
    }

    public Map<String, Map<String, Reservation>> getActiveReservationsByNodeByPool() {
        return activeReservationsByNodeByPool;
    }

}