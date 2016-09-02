/* 
* OpenSpeedMonitor (OSM)
* Copyright 2014 iteratec GmbH
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* 	http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License.
*/

package de.iteratec.osm.report.chart

import de.iteratec.osm.csi.CsiAggregationUpdateEventCleanupService;

/**
 * This job calculates and closes aggregated csi values and deletes associated update events each night.
 * @author nkuhn
 */
class CsiAggregationUpdateEventCleanupJob {

    CsiAggregationUpdateEventCleanupService csiAggregationUpdateEventCleanupService
    boolean createBatchActivity = true
    static boolean isCurrentlyRunning = false

    static triggers = {
        /** Each Day at 5:30 am. */
        cron(name: 'dailyUpdateEventCleanup', cronExpression: '0 30 5 ? * *')
    }

    def execute() {
        if (isCurrentlyRunning) {
            log.info("Quartz controlled cleanup of CsiAggregationUpdateEvents: Job is already running.")
            return
        }

        isCurrentlyRunning = true
        try {
            csiAggregationUpdateEventCleanupService.closeCsiAggregationsExpiredForAtLeast(300, createBatchActivity)
        } catch (Exception e) {
            log.error("Quartz controlled cleanup of CsiAggregationUpdateEvents throws an exception: " + e.getMessage())
        } finally {
            isCurrentlyRunning = false
        }
    }
}
