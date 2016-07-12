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

package de.iteratec.osm.batch

import spock.lang.Specification

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(BatchActivityService)
@Mock(BatchActivity)
class BatchActivityServiceSpec extends Specification {

    BatchActivityService serviceUnderTest = service

    void "testGetBatchActivity"() {
        given:
        String name = "BatchActivityCreateTest"
        Class aClass = this.class
        Activity activity = Activity.CREATE

        when: "We order a BatchActivityUpdater from this service"
        BatchActivityUpdater updater = serviceUnderTest.getActiveBatchActivity(aClass, activity, name, 1, true)

        then: "The underlying BatchActivity should be persisted"
        BatchActivity.count() == 1
        serviceUnderTest.runningBatch(aClass,name, activity)

        cleanup:
        updater.cancel()
    }

    void "testGetBatchActivityDummy"() {
        given:
        String name = "BatchActivityCreateTest"
        Class aClass = this.class
        Activity activity = Activity.CREATE

        when: "We order a BatchActivityUpdaterDummy from this service"
        BatchActivityUpdater updater = serviceUnderTest.getActiveBatchActivity(aClass, activity, name, 1, false)

        then: "The should'nt be a real BatchActivity"
        !serviceUnderTest.runningBatch(aClass,name, activity)

        cleanup:
        updater.cancel()
    }

}