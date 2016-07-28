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

package de.iteratec.osm.csi

import de.iteratec.osm.measurement.schedule.JobGroup
import de.iteratec.osm.report.chart.CsiAggregation

/**
 * For caching elements during expensive calculations in {@link EventCsiAggregationService}, {@link PageCsiAggregationService} or {@link ShopCsiAggregationService}.
 * @author nkuhn
 *
 */
class CsiAggregationCachingContainer {
	JobGroup csiGroupToCalcCsiAggregationFor
	Page pageToCalcCsiAggregationFor
	Map<String,List<CsiAggregation>> hCsiAggregationsByCsiGroupPageCombination
}
