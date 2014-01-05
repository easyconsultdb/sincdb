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
package org.jumpmind.symmetric.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.statistic.Statistic;
import org.jumpmind.symmetric.statistic.StatisticAlertThresholds;
import org.springframework.transaction.annotation.Transactional;

public interface IStatisticService {

    @Transactional
    public void save(Collection<Statistic> stats, Date captureEndTime);

    public List<StatisticAlertThresholds> getAlertThresholds();

    @Transactional
    public void saveStatisticAlertThresholds(StatisticAlertThresholds threshold);

    public boolean removeStatisticAlertThresholds(String statisticName);
}