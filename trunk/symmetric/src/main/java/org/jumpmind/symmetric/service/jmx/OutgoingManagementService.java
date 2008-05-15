/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.jumpmind.symmetric.service.jmx;

import java.math.BigDecimal;

import org.jumpmind.symmetric.statistic.IStatisticManager;
import org.jumpmind.symmetric.statistic.StatisticName;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(description = "The management interface for outgoing synchronization")
public class OutgoingManagementService {

    IStatisticManager statisticManager;

    public void setStatisticManager(IStatisticManager statisticManager) {
        this.statisticManager = statisticManager;
    }

    @ManagedAttribute(description = "Get the average number of events in each batch since the last statistic flush")
    public BigDecimal getPeriodicAverageEventsPerBatch() {
        return this.statisticManager.getStatistic(StatisticName.OUTGOING_EVENTS_PER_BATCH).getAverageValue();
    }

    @ManagedAttribute(description = "Get the average number of events in each batch for the lifetime of the server")
    public BigDecimal getServerLifetimeAverageEventsPerBatch() {
        return this.statisticManager.getStatistic(StatisticName.OUTGOING_EVENTS_PER_BATCH)
                .getLifetimeAverageValue();
    }
}
