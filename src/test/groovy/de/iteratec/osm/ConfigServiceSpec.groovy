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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.runtime.FreshRuntime
import org.junit.Before

import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@FreshRuntime
@TestFor(ConfigService)
@Mock([OsmConfiguration])
class ConfigServiceSpec {
	
	ConfigService serviceUnderTest
	InMemoryConfigService inMemoryConfigService

	def doWithSpring = {
		inMemoryConfigService(InMemoryConfigService)
	}
	@Before
	void setUp(){
		serviceUnderTest = service
		inMemoryConfigService = grailsApplication.mainContext.getBean('inMemoryConfigService')
	}

    void testFailingGettingDetailDataStorageTime() {
		//test-specific data
		int firstStorageTimeInWeeks = 4
		int secondStorageTimeInWeeks = 8 
		int thirdStorageTimeInWeeks = 12
		//test executions and assertions
		
		//no conf
		shouldFail (IllegalStateException) {
			serviceUnderTest.getDetailDataStorageTimeInWeeks()
		}
		new OsmConfiguration(
			detailDataStorageTimeInWeeks: firstStorageTimeInWeeks, 
			defaultMaxDownloadTimeInMinutes: 60,
			minDocCompleteTimeInMillisecs: 250,
			maxDocCompleteTimeInMillisecs: 180000).save(failOnError: true)
		new OsmConfiguration(
			detailDataStorageTimeInWeeks: secondStorageTimeInWeeks, 
			defaultMaxDownloadTimeInMinutes: 60,
			minDocCompleteTimeInMillisecs: 250,
			maxDocCompleteTimeInMillisecs: 180000).save(failOnError: true)
		// two confs
		shouldFail (IllegalStateException) {
			serviceUnderTest.getDetailDataStorageTimeInWeeks()
		}
    }
	void testSuccessfulGettingDetailDataStorageTime() {
		//test-specific data
		int firstStorageTimeInWeeks = 4
		int secondStorageTimeInWeeks = 8
		int thirdStorageTimeInWeeks = 12
		//test executions and assertions
		OsmConfiguration conf = new OsmConfiguration(
			detailDataStorageTimeInWeeks: firstStorageTimeInWeeks,
			defaultMaxDownloadTimeInMinutes: 60,
			minDocCompleteTimeInMillisecs: 250,
			maxDocCompleteTimeInMillisecs: 180000).save(failOnError: true)
		assertThat(serviceUnderTest.getDetailDataStorageTimeInWeeks(), is(firstStorageTimeInWeeks))
		
		conf.detailDataStorageTimeInWeeks = secondStorageTimeInWeeks
		conf.save(failOnError: true)
		assertThat(serviceUnderTest.getDetailDataStorageTimeInWeeks(), is(secondStorageTimeInWeeks))
		
		conf.detailDataStorageTimeInWeeks = thirdStorageTimeInWeeks
		conf.save(failOnError: true)
		assertThat(serviceUnderTest.getDetailDataStorageTimeInWeeks(), is(thirdStorageTimeInWeeks))
	}
	
	void testSuccessfulGettingMeasurementsGenerallyEnabled() {
		//test-specific data
		OsmConfiguration conf = new OsmConfiguration().save(failOnError: true) //config with default values
		Boolean defaultMeasurementActivation = false
		//test executions and assertions
		assertThat(inMemoryConfigService.areMeasurementsGenerallyEnabled(), is(defaultMeasurementActivation))
		inMemoryConfigService.activateMeasurementsGenerally()
		assertThat(inMemoryConfigService.areMeasurementsGenerallyEnabled(), is(true))
	}
	
	void testSuccessfulGettingInitialChartHeight() {
		//test-specific data
		Integer expectedInitialChartHeight = 720
		OsmConfiguration conf = new OsmConfiguration(initialChartHeightInPixels: expectedInitialChartHeight).save(failOnError: true)
		//test executions and assertions
		assertThat(serviceUnderTest.getInitialChartHeightInPixels(), is(expectedInitialChartHeight))
	}

    void testActivatingMeasurements(){
        //test-specific data
        OsmConfiguration conf = new OsmConfiguration().save(failOnError: true)
        assertThat(inMemoryConfigService.areMeasurementsGenerallyEnabled(), is(false))
        //test execution
		inMemoryConfigService.activateMeasurementsGenerally()
        // assertions
        assertThat(inMemoryConfigService.areMeasurementsGenerallyEnabled(), is(true))
        //test execution
		inMemoryConfigService.activateMeasurementsGenerally()
        // assertions
        assertThat(inMemoryConfigService.areMeasurementsGenerallyEnabled(), is(true))
    }
}
