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

package de.iteratec.osm

import grails.buildtestdata.BuildDomainTest
import grails.buildtestdata.mixin.Build
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import static de.iteratec.osm.OsmConfiguration.DEFAULT_MAX_DOWNLOAD_TIME_IN_MINUTES
import static de.iteratec.osm.OsmConfiguration.DEFAULT_MIN_VALID_LOADTIME
import static de.iteratec.osm.OsmConfiguration.DEFAULT_MAX_VALID_LOADTIME
import static de.iteratec.osm.OsmConfiguration.DEFAULT_INITIAL_CHART_HEIGHT_IN_PIXELS

@Build([OsmConfiguration])
class ConfigServiceSpec extends Specification implements BuildDomainTest<OsmConfiguration>, ServiceUnitTest<ConfigService> {

    void "one config is fine"() {
        when: "only one config has been saved with a value"
        OsmConfiguration.build(detailDataStorageTimeInWeeks: 4)

        then: "config service hat its values including the defaults"
        service.getDetailDataStorageTimeInWeeks() == 4
        service.getDefaultMaxDownloadTimeInMinutes() == DEFAULT_MAX_DOWNLOAD_TIME_IN_MINUTES
        service.getMinValidLoadtime() == DEFAULT_MIN_VALID_LOADTIME
        service.getMaxValidLoadtime() == DEFAULT_MAX_VALID_LOADTIME
        service.getInitialChartHeightInPixels() == DEFAULT_INITIAL_CHART_HEIGHT_IN_PIXELS
    }

    void "only one config can be persisted"() {
        given: "two configs have been saved"
        2.times { OsmConfiguration.build() }

        expect: "only one configuration"
        OsmConfiguration.count() == 1
    }

    void "values in config can be changed"(int from, int to) {
        given: "value in config has been set"
        OsmConfiguration conf = OsmConfiguration.build(detailDataStorageTimeInWeeks: from)

        when: "value is changed in config"
        conf.detailDataStorageTimeInWeeks = to

        then: "config service has the new value"
        service.getDetailDataStorageTimeInWeeks() == to

        where:
        from | to
        1    | 2
        3    | 4
        5    | 1
    }

    void "Failure with no config"() {
        given: "no config"

        when: "getting DetailDataStorageTimeInWeeks"
        service.getDetailDataStorageTimeInWeeks()

        then: "should fail with IllegalStateException"
        thrown(IllegalStateException)
    }

}
