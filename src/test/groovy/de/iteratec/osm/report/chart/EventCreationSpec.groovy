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

import de.iteratec.osm.api.ApiKey
import de.iteratec.osm.api.CreateEventCommand
import de.iteratec.osm.measurement.schedule.JobGroup
import grails.test.mixin.*
import grails.validation.ValidationException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(EventDaoService)
@Mock([Event, JobGroup])
class EventCreationSpec extends Specification{

    EventDaoService serviceUnderTest

    static JobGroup group1, group2

    void setup(){
        serviceUnderTest = service
        //test data common to all tests
        JobGroup.withTransaction {
            group1 = new JobGroup(name: 'JobGroup1').save(failOnError: true)
            group2 = new JobGroup(name: 'JobGroup2').save(failOnError: true)
        }
    }

    void "successful creation of event"() {
        setup:
        //test specific data
        String shortname = 'shortname'
        DateTime datetime = new DateTime(2015, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        String description = 'description'
        boolean globalVisibility = true

        when:
        serviceUnderTest.createEvent(
                shortname,
                datetime,
                description,
                globalVisibility,
                [group1, group2])

        then:
        //test written event
        List<Event> persistedEvents = Event.list()
        persistedEvents.size() == 1
        Event persistedEvent = persistedEvents[0]
        persistedEvent.shortName == shortname
        persistedEvent.description == description
        new DateTime(persistedEvent.eventDate) == datetime
        persistedEvent.globallyVisible == globalVisibility
        List<JobGroup> associatedJobGroups = persistedEvent.jobGroups
        associatedJobGroups.size() == 2
        associatedJobGroups.contains(group1)
        associatedJobGroups.contains(group2)
    }

    void "successful creation of event with null description"() {
        setup:
        //test specific data
        String shortname = 'shortname'
        DateTime datetime = new DateTime(2015, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        String description
        boolean globalVisibility = true

        when:
        serviceUnderTest.createEvent(
                shortname,
                datetime,
                description,
                globalVisibility,
                [group1, group2])

        then:
        //test written event
        List<Event> persistedEvents = Event.list()
        persistedEvents.size() == 1
        Event persistedEvent = persistedEvents[0]
        persistedEvent.shortName == shortname
        persistedEvent.description == description
        new DateTime(persistedEvent.eventDate) == datetime
        persistedEvent.globallyVisible == globalVisibility
        List<JobGroup> associatedJobGroups = persistedEvent.jobGroups
        associatedJobGroups.size() == 2
        associatedJobGroups.contains(group1)
        associatedJobGroups.contains(group2)
    }

    void "creation of event should fail cause shortname is null"() {
        setup:
        //test specific data
        String shortname = null
        DateTime datetime = new DateTime(2015, 1, 1, 0, 0, 0, DateTimeZone.UTC)
        String description = 'description'
        boolean globalVisibility = true

        when:
        shouldFail(ValidationException) {
            serviceUnderTest.createEvent(
                    shortname,
                    datetime,
                    description,
                    globalVisibility,
                    [group1, group2])
        }

        then:
        //test written event
        Event.list().size() == 0
    }
    void "creation of event should fail cause eventTimestamp is null"() {
        setup:
        //test specific data
        String shortname = 'shortname'
        DateTime datetime
        String description = 'description'
        boolean globalVisibility = true

        when:
        shouldFail(ValidationException) {
            serviceUnderTest.createEvent(
                    shortname,
                    datetime,
                    description,
                    globalVisibility,
                    [group1, group2])
        }

        then:
        //test written event
        Event.list().size() == 0
    }
    void "creation of event should fail cause globallyVisible is null"() {
        setup:
        //test specific data
        String shortname = 'shortname'
        DateTime datetime
        String description = 'description'
        boolean globalVisibility

        when:
        shouldFail(ValidationException) {
            serviceUnderTest.createEvent(
                    shortname,
                    datetime,
                    description,
                    globalVisibility,
                    [group1, group2])
        }

        then:
        //test written event
        Event.list().size() == 0
    }
}
