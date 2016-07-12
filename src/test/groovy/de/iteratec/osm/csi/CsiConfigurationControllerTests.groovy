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
import de.iteratec.osm.util.I18nService
import de.iteratec.osm.util.PerformanceLoggingService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(CsiConfigurationController)
@Mock([CsiConfiguration, CsiDay, JobGroup])
class CsiConfigurationControllerTests extends Specification {

    CsiConfiguration config1
    CsiConfiguration config2

    def doWithSpring = {
        performanceLoggingService(PerformanceLoggingService)
    }

    void setup() {
        CsiDay testDay = new CsiDay(name: "testDay")
        (0..23).each {
            testDay.setHourWeight(it, it)
        }
        CsiDay testDay2 = new CsiDay(name: "testDay2")
        (0..23).each {
            testDay2.setHourWeight(it, (24 - it))
        }
        config1 = new CsiConfiguration(label: "config1", csiDay: testDay)
        config2 = new CsiConfiguration(label: "config2", csiDay: testDay2)
        config1.save(failOnError: true)
        config2.save(failOnError: true)

        I18nService i18nService = Mock(I18nService)
        controller.performanceLoggingService = grailsApplication.mainContext.getBean('performanceLoggingService')
        controller.i18nService = i18nService
    }

    void "test saveCopy"() {
        given:
        String labelOfCopy = "ConfigCopy"
        int configCountBeforeCopy = CsiConfiguration.count

        when:
        params.label = labelOfCopy
        params.sourceCsiConfigLabel = "config1"

        controller.saveCopy()
        CsiConfiguration copy = CsiConfiguration.findByLabel(labelOfCopy)

        then:
        CsiConfiguration.count == configCountBeforeCopy + 1
        CsiConfiguration.findAllByLabel(labelOfCopy).size() == 1
        copy.label == labelOfCopy
        copy.label != config1.label
    }

    void "test deleteCsiConfiguration"() {
        given:
        int csiConfigurationCountBeforeDeleting = CsiConfiguration.count

        when:
        params.label = config1.label
        controller.deleteCsiConfiguration()

        then:
        CsiConfiguration.count == csiConfigurationCountBeforeDeleting - 1
    }

    void "test deleteCsiConfiguration when jobGroup using this configuration"() {
        given:
        int csiConfigurationCountBeforeDeleting = CsiConfiguration.count
        JobGroup jobGroup = new JobGroup(name: "jobGroup", csiConfiguration: config1)
        jobGroup.save()

        when:
        params.label = config1.label
        controller.deleteCsiConfiguration()

        then:
        CsiConfiguration.count == csiConfigurationCountBeforeDeleting - 1
        jobGroup.csiConfiguration == null
    }

    void "test exception is thrown when configuration not exists"() {
        when:
        params.label = "doesNotExist"
        controller.deleteCsiConfiguration()

        then:
        thrown(IllegalArgumentException)
    }

    void "test exception is thrown if an attempt is made to delete the last csiConfiguration"() {
        given:
        CsiConfiguration.findByLabel(config2.label).delete()

        when:
        params.label = config1.label
        controller.deleteCsiConfiguration()

        then:
        thrown(IllegalStateException)
    }

    void "test validateDelition if all correct"() {
        when:
        controller.validateDeletion()
        def jsonResponse = response.json

        then:
        jsonResponse.errorMessages.isEmpty()
    }

    void "test validateDeletion if only one csiConfiguration is left"() {
        given:
        CsiConfiguration.findByLabel(config2.label).delete()

        when:
        controller.validateDeletion()
        def jsonResponse = response.json
        List errorMessages = jsonResponse.errorMessages as List

        then:
        errorMessages.size() == 1
    }

}
