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

package de.iteratec.osm.measurement.environment.wptserverproxy

import de.iteratec.osm.csi.*
import de.iteratec.osm.measurement.environment.*
import de.iteratec.osm.measurement.schedule.Job
import de.iteratec.osm.measurement.schedule.JobGroup
import de.iteratec.osm.measurement.script.Script
import de.iteratec.osm.report.external.MetricReportingService
import de.iteratec.osm.result.*
import de.iteratec.osm.util.PerformanceLoggingService
import de.iteratec.osm.util.ServiceMocker
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.util.slurpersupport.GPathResult
import org.joda.time.DateTime
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * Tests the saving of locations and results. These functions are used in proxy-mechanism.
 * Testing the mapping of load-times to customer satisfactions or the persisting of dependent {@link CsiAggregation}s is not the concern of the tests in this class.
 * @author nkuhn
 * @see {@link ProxyService}
 *
 * TODO: This test is to complicated - make it simpler!
 */
@TestFor(ResultPersisterService)
@Mock([WebPageTestServer, Browser, Location, Job, JobResult, EventResult, BrowserAlias, Page, MeasuredEvent, JobGroup, Script, CsiConfiguration, TimeToCsMapping, CsiDay])
class PersistingNewEventResultsTests {
    private static final ServiceMocker SERVICE_MOCKER = ServiceMocker.create()

    WebPageTestServer server1, server2

    JobGroup undefinedJobGroup;

    Browser undefinedBrowser;

    /**
     * Map with expected values for assertions in test {@link #testListenToSuccessfullyMeasuredResults}.
     * 			Structure of the map:<br>
     * 			[name of expected value 1: expectedValue1,<br>
     * 			name of expected value 2: expectedValue2,<br>
     * 			...<br>
     * 			name of expected value n: expectedValueN]
     */
    static final Map expectedAfterResultListening = [
            'Result_NoMultistep_3Runs.xml'                                         :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'ie_step_testjob',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 3,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Thu, 25 Apr 2013 09:52:21 +0000')),
                     'expectedNumberOfSteps'                  : 1,
                     'expectedNumberOfStepsWithAssociatedPage': 0,
                     'expectedNumberOfCachedViews'            : 2,
                     'expectedWptResultVersion'               : WptXmlResultVersion.BEFORE_MULTISTEP
                    ],
            'Result_Multistep_3Runs_6EventNames.xml'                               :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'http://www.example.de.de - Multiple steps with event names + dom elements',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 3,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Thu, 25 Apr 2013 09:52:21 +0000')),
                     'expectedNumberOfSteps'                  : 6,
                     'expectedNumberOfStepsWithAssociatedPage': 0,
                     'expectedNumberOfCachedViews'            : 2,
                     'expectedWptResultVersion'               : WptXmlResultVersion.MULTISTEP_1
                    ],
            'Result_NoMultistep_1Run_NotCsiRelevantCauseDocTimeTooHighResponse.xml':
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'FF_BV1_Step01_Homepage - netlab',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 1,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Wed, 03 Apr 2013 11:46:22 +0000')),
                     'expectedNumberOfSteps'                  : 1,
                     'expectedNumberOfStepsWithAssociatedPage': 0,
                     'expectedNumberOfCachedViews'            : 2,
                     'expectedWptResultVersion'               : WptXmlResultVersion.BEFORE_MULTISTEP
                    ],
            'Result_NoMultistep_1Run_JustFirstView.xml'                            :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'testjob',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 1,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Sat, 22 Jun 2013 20:33:35 +0000')),
                     'expectedNumberOfSteps'                  : 1,
                     'expectedNumberOfStepsWithAssociatedPage': 0,
                     'expectedNumberOfCachedViews'            : 1,
                     'expectedWptResultVersion'               : WptXmlResultVersion.BEFORE_MULTISTEP
                    ],
            'Result_Multistep_1Run_2EventNamesWithPagePrefix_JustFirstView.xml'    :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'testjob',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 1,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Wed, 30 Jan 2013 12:00:48 +0000')),
                     'expectedNumberOfSteps'                  : 2,
                     'expectedNumberOfStepsWithAssociatedPage': 2,
                     'expectedNumberOfCachedViews'            : 1,
                     'expectedWptResultVersion'               : WptXmlResultVersion.MULTISTEP_1
                    ],
            'Result_Multistep_1Run_2EventNames_PagePrefix.xml'                     :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'FF_BV1_Multistep_2',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 1,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Wed, 11 Dec 2013 15:42:43 +0000')),
                     'expectedNumberOfSteps'                  : 2,
                     'expectedNumberOfStepsWithAssociatedPage': 2,
                     'expectedNumberOfCachedViews'            : 2,
                     'expectedWptResultVersion'               : WptXmlResultVersion.MULTISTEP_1
                    ],
            'Result_wptserver2.15-singlestep_1Run_WithoutVideo.xml'                :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'IE_otto_hp_singlestep',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 1,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Wed, 21 Apr 2016 12:06:53 +0000')),
                     'expectedNumberOfSteps'                  : 1,
                     'expectedNumberOfStepsWithAssociatedPage': 0,
                     'expectedNumberOfCachedViews'            : 2,
                     'expectedWptResultVersion'               : WptXmlResultVersion.BEFORE_MULTISTEP
                    ],
            'Result_wptserver2.15_singlestep_1Run_WithVideo.xml'                   :
                    ['expectedNumberOfLocations'              : 1,
                     'expectedJobLabel'                       : 'IE_otto_hp_singlestep',
                     'expectedNumberOfJobRuns'                : 1,
                     'expectedNumberOfRuns'                   : 1,
                     'expectedResultExecutionDateTime'        : new DateTime(new Date('Wed, 21 Apr 2016 12:03:14 +0000')),
                     'expectedNumberOfSteps'                  : 1,
                     'expectedNumberOfStepsWithAssociatedPage': 0,
                     'expectedNumberOfCachedViews'            : 2,
                     'expectedWptResultVersion'               : WptXmlResultVersion.BEFORE_MULTISTEP
                    ]
    ]

    ResultPersisterService serviceUnderTest

    def doWithSpring = {
        metricReportingService(MetricReportingService)
        performanceLoggingService(PerformanceLoggingService)
        proxyService(ProxyService)
        browserService(BrowserService)
        csiAggregationUpdateService(CsiAggregationUpdateService)
        pageService(PageService)
        csiAggregationTagService(CsiAggregationTagService)
        csiValueService(CsiValueService)
    }

    @Before
    void setUp() {
        serviceUnderTest = service
        createTestDataCommonForAllTests()

        //mocks common for all tests
        SERVICE_MOCKER.mockTTCsMappingService(serviceUnderTest)
        mockMetricReportingService()
        serviceUnderTest.performanceLoggingService = grailsApplication.mainContext.getBean('performanceLoggingService')
    }

    @After
    void tearDown() {
    }

    // tests///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Tests the determination of teststep-count from webpagetest-xml-result. Uses testdata from test/resources.
     * @see #expectedAfterResultListening
     */
    @Test
    void testgetTeststepCount() {
        expectedAfterResultListening.each { k, v ->
            GPathResult xmlResult = new XmlSlurper().parse(new File("test/resources/WptResultXmls/${k}"))
            assertEquals(v['expectedNumberOfSteps'], new WptResultXml(xmlResult).getTestStepCount())
        }
    }

    @Test
    void testCreationOfWptResultXml() {
        expectedAfterResultListening.each { k, v ->
            GPathResult xmlResult = new XmlSlurper().parse(new File("test/resources/WptResultXmls/${k}"))
            assertEquals(v['expectedWptResultVersion'], new WptResultXml(xmlResult).version)
        }
    }

    @Test
    void testCreatedEventsAfterListeningToMultistepResult() {

        //create test-specific data
        String nameOfResultXmlFile = 'Result_Multistep_3Runs_6EventNames.xml'
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution

        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions

        List<MeasuredEvent> events = MeasuredEvent.list()
        assertEquals('Count of events', 6, events.size())

        MeasuredEvent event1 = MeasuredEvent.findByName('Seite laden')
        assertNotNull(event1)
        MeasuredEvent event2 = MeasuredEvent.findByName('Artikel suchen')
        assertNotNull(event2)
        MeasuredEvent event3 = MeasuredEvent.findByName('Artikel-Detailseite laden')
        assertNotNull(event3)
        MeasuredEvent event4 = MeasuredEvent.findByName('Produkt auswaehlen')
        assertNotNull(event4)
        MeasuredEvent event5 = MeasuredEvent.findByName('Produkt in Warenkorb legen')
        assertNotNull(event5)
        MeasuredEvent event6 = MeasuredEvent.findByName('Warenkorb oeffnen')
        assertNotNull(event6)

        //exemplarily testing results of some events
        List<EventResult> medianUncachedResultsOfEvent4 = EventResult.findAllByMeasuredEvent(event4).findAll {
            it.medianValue == true && it.cachedView == CachedView.UNCACHED
        }
        assertEquals('Count of median uncached-EventResults for event 4 - ', 1, medianUncachedResultsOfEvent4.size())
        assertEquals('loadTimeInMillisecs of median uncached-EventResults for event 4 - ', 2218, medianUncachedResultsOfEvent4[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median uncached-EventResults for event 4 - ', 29, medianUncachedResultsOfEvent4[0].docCompleteRequests)
        assertEquals('wptStatus of median uncached-EventResults for event 4 - ', 99999, medianUncachedResultsOfEvent4[0].wptStatus)

        List<EventResult> medianCachedResultsOfEvent2Run1 = EventResult.findAllByMeasuredEvent(event2).findAll {
            it.medianValue == true && it.cachedView == CachedView.CACHED && it.numberOfWptRun == 1
        }
        assertEquals('Count of median cached-EventResults for event 2, first run - ', 1, medianCachedResultsOfEvent2Run1.size())
        assertEquals('loadTimeInMillisecs of median cached-EventResults for event 2, first run  - ', 931, medianCachedResultsOfEvent2Run1[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median cached-EventResults for event 2, first run  - ', 6, medianCachedResultsOfEvent2Run1[0].docCompleteRequests)
        assertEquals('wptStatus of median cached-EventResults for event 2, first run  - ', 99999, medianCachedResultsOfEvent2Run1[0].wptStatus)

        //TestDetailsUrl uncached and without page
        EventResult eventResultUncachedTest = medianUncachedResultsOfEvent4[0]
        String[] tokensUncached = eventResultUncachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensUncached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensUncached[2].contains("1"))
        assertTrue(tokensUncached[3].contains("0#waterfall_viewProdukt auswaehlen"))

        //TestDetailsUrl cached and without page
        EventResult eventResultCachedTest = medianCachedResultsOfEvent2Run1[0]
        String[] tokensCached = eventResultCachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensCached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensCached[2].contains("1"))
        assertTrue(tokensCached[3].contains("1#waterfall_viewArtikel suchen"))
    }

    @Test
    void testCreatedEventsAfterListeningToMultistepResultWithPageName() {

        //create test-specific data
        String nameOfResultXmlFile = 'Result_Multistep_3Runs_6EventNames_WithPageName.xml'
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution

        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions

        List<MeasuredEvent> events = MeasuredEvent.list()
        assertEquals('Count of events', 6, events.size())

        MeasuredEvent event1 = MeasuredEvent.findByName('Seite laden')
        assertNotNull(event1)
        MeasuredEvent event2 = MeasuredEvent.findByName('Artikel suchen')
        assertNotNull(event2)
        MeasuredEvent event3 = MeasuredEvent.findByName('Artikel-Detailseite laden')
        assertNotNull(event3)
        MeasuredEvent event4 = MeasuredEvent.findByName('Produkt ausw.aehlen')
        assertNotNull(event4)
        MeasuredEvent event5 = MeasuredEvent.findByName('Produkt in Warenkorb legen')
        assertNotNull(event5)
        MeasuredEvent event6 = MeasuredEvent.findByName('Warenkorb oeffnen')
        assertNotNull(event6)

        //exemplarily testing results of some events
        List<EventResult> medianUncachedResultsOfEvent4 = EventResult.findAllByMeasuredEvent(event4).findAll {
            it.medianValue == true && it.cachedView == CachedView.UNCACHED
        }
        assertEquals('Count of median uncached-EventResults for event 4 - ', 1, medianUncachedResultsOfEvent4.size())
        assertEquals('loadTimeInMillisecs of median uncached-EventResults for event 4 - ', 2218, medianUncachedResultsOfEvent4[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median uncached-EventResults for event 4 - ', 29, medianUncachedResultsOfEvent4[0].docCompleteRequests)
        assertEquals('wptStatus of median uncached-EventResults for event 4 - ', 99999, medianUncachedResultsOfEvent4[0].wptStatus)

        List<EventResult> medianCachedResultsOfEvent2Run1 = EventResult.findAllByMeasuredEvent(event2).findAll {
            it.medianValue == true && it.cachedView == CachedView.CACHED && it.numberOfWptRun == 1
        }
        assertEquals('Count of median cached-EventResults for event 2, first run - ', 1, medianCachedResultsOfEvent2Run1.size())
        assertEquals('loadTimeInMillisecs of median cached-EventResults for event 2, first run - ', 931, medianCachedResultsOfEvent2Run1[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median cached-EventResults for event 2, first run - ', 6, medianCachedResultsOfEvent2Run1[0].docCompleteRequests)
        assertEquals('wptStatus of median cached-EventResults for event 2, first run - ', 99999, medianCachedResultsOfEvent2Run1[0].wptStatus)

        //TestDetailsUrl uncached and without page
        EventResult eventResultUncachedTest = medianUncachedResultsOfEvent4[0]
        String[] tokensUncached = eventResultUncachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensUncached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensUncached[2].contains("1"))
        //replacing check: in database with a dot 'Produkt ausw.aehlen'
        assertTrue(tokensUncached[3].contains("0#waterfall_viewHPProdukt auswaehlen"))

        //TestDetailsUrl cached and without page
        EventResult eventResultCachedTest = medianCachedResultsOfEvent2Run1[0]
        String[] tokensCached = eventResultCachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensCached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensCached[2].contains("1"))
        assertTrue(tokensCached[3].contains("1#waterfall_viewHPArtikel suchen"))
    }

    @Test
    void testCreatedEventsAfterListeningToMultistepResultHasCsByVisuallyComplete() {

        //create test-specific data
        String nameOfResultXmlFile = 'Result_wptserver2.13-multistep7_5Runs_3Events_JustFirstView_WithVideo.xml'
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution

        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions

        List<EventResult> events = EventResult.list()
        assertEquals('Count of events', 15, events.size())

        //check all EventResults have visualComplete
        boolean allHaveVisualComplete = !events.any { it.visuallyCompleteInMillisecs == null }
        assertTrue(allHaveVisualComplete)

        //check all EventResults have set csByVisualComplete
        boolean allHaveCsByVisualComplete = !events.any { it.csByWptVisuallyCompleteInPercent == null }
        assertTrue(allHaveCsByVisualComplete)
    }

    @Test
    void testPageAssignement() {

        //create test-specific data
        String nameOfResultXmlFile = 'Result_Multistep_1Run_2EventNames_PagePrefix.xml'
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution
        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions
        List<MeasuredEvent> events = MeasuredEvent.list()
        assertEquals('Count of events', 2, events.size())

        MeasuredEvent event1 = MeasuredEvent.findByName('LH_Homepage_2')
        assertNotNull(event1)
        assertEquals('', '')
        MeasuredEvent event2 = MeasuredEvent.findByName('LH_Moduleinstieg_2')
        assertNotNull(event2)

    }

    @Test
    void testCreatedEventsAfterListeningToNonMultistepResult() {

        //create test-specific data
        String nameOfResultXmlFile = 'Result_NoMultistep_3Runs_CsiRelevant.xml'
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution
        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions

        List<MeasuredEvent> events = MeasuredEvent.list()
        assertEquals('Count of events', 1, events.size())

        MeasuredEvent event1 = MeasuredEvent.findByName('FF_BV1_Step01_Homepage - netlab')
        assertNotNull(event1)

        //exemplarily testing some results
        List<EventResult> medianUncachedResultsOfsingleEvent = EventResult.findAllByMeasuredEvent(event1).findAll {
            it.medianValue == true && it.cachedView == CachedView.UNCACHED
        }
        assertEquals('Count of median uncached-EventResults for single event - ', 1, medianUncachedResultsOfsingleEvent.size())
        assertEquals('loadTimeInMillisecs of median uncached-EventResults for single event - ', 5873, medianUncachedResultsOfsingleEvent[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median uncached-EventResults for single event - ', 157, medianUncachedResultsOfsingleEvent[0].docCompleteRequests)
        assertEquals('wptStatus of median uncached-EventResults for single event - ', 0, medianUncachedResultsOfsingleEvent[0].wptStatus)

        List<EventResult> medianCachedResultsOfsingleEventRun3 = EventResult.findAllByMeasuredEvent(event1).findAll {
            it.cachedView == CachedView.CACHED && it.numberOfWptRun == 3
        }
        assertEquals('Count of median cached-EventResults for single event, third run - ', 1, medianCachedResultsOfsingleEventRun3.size())
        assertEquals('loadTimeInMillisecs of median cached-EventResults for single event, third run - ', 3977, medianCachedResultsOfsingleEventRun3[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median cached-EventResults for single event, third run - ', 36, medianCachedResultsOfsingleEventRun3[0].docCompleteRequests)
        assertEquals('wptStatus of median cached-EventResults for single event, third run - ', 0, medianCachedResultsOfsingleEventRun3[0].wptStatus)

        //TestDetailsUrl uncached and without page
        EventResult eventResultUncachedTest = medianUncachedResultsOfsingleEvent[0]
        String[] tokensUncached = eventResultUncachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensUncached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensUncached[2].contains("2"))
        assertTrue(tokensUncached[3].contains("0#waterfall_viewFF_BV1_Step01_Homepage - netlab"))

        //TestDetailsUrl cached and without page
        EventResult eventResultCachedTest = medianCachedResultsOfsingleEventRun3[0]
        String[] tokensCached = eventResultCachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensCached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensCached[2].contains("3"))
        assertTrue(tokensCached[3].contains("1#waterfall_viewFF_BV1_Step01_Homepage - netlab"))
    }

    @Test
    void testCreatedEventsAfterListeningToNonMultistepResultWithPageName() {

        //create test-specific data
        String nameOfResultXmlFile = 'Result_NoMultistep_3Runs_WithPageName_CsiRelevant.xml'
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution
        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions

        List<MeasuredEvent> events = MeasuredEvent.list()
        assertEquals('Count of events', 1, events.size())

        MeasuredEvent event1 = MeasuredEvent.findByName('FF_BV1_Step01_Homepage - netlab')
        assertNotNull(event1)

        //exemplarily testing some results
        List<EventResult> medianUncachedResultsOfsingleEvent = EventResult.findAllByMeasuredEvent(event1).findAll {
            it.medianValue == true && it.cachedView == CachedView.UNCACHED
        }
        assertEquals('Count of median uncached-EventResults for single event - ', 1, medianUncachedResultsOfsingleEvent.size())
        assertEquals('loadTimeInMillisecs of median uncached-EventResults for single event - ', 5873, medianUncachedResultsOfsingleEvent[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median uncached-EventResults for single event - ', 157, medianUncachedResultsOfsingleEvent[0].docCompleteRequests)
        assertEquals('wptStatus of median uncached-EventResults for single event - ', 0, medianUncachedResultsOfsingleEvent[0].wptStatus)

        List<EventResult> medianCachedResultsOfsingleEventRun3 = EventResult.findAllByMeasuredEvent(event1).findAll {
            it.cachedView == CachedView.CACHED && it.numberOfWptRun == 3
        }
        assertEquals('Count of median cached-EventResults for single event, third run - ', 1, medianCachedResultsOfsingleEventRun3.size())
        assertEquals('loadTimeInMillisecs of median cached-EventResults for single event, third run - ', 3977, medianCachedResultsOfsingleEventRun3[0].docCompleteTimeInMillisecs)
        assertEquals('docCompleteRequests of median cached-EventResults for single event, third run - ', 36, medianCachedResultsOfsingleEventRun3[0].docCompleteRequests)
        assertEquals('wptStatus of median cached-EventResults for single event, third run - ', 0, medianCachedResultsOfsingleEventRun3[0].wptStatus)

        //TestDetailsUrl uncached and without page
        EventResult eventResultUncachedTest = medianUncachedResultsOfsingleEvent[0]
        String[] tokensUncached = eventResultUncachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensUncached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensUncached[2].contains("2"))
        assertTrue(tokensUncached[3].contains("0#waterfall_viewHPFF_BV1_Step01_Homepage - netlab"))

        //TestDetailsUrl cached and without page
        EventResult eventResultCachedTest = medianCachedResultsOfsingleEventRun3[0]
        String[] tokensCached = eventResultCachedTest.getTestDetailsWaterfallURL().toString().split("[=]")
        assertTrue(tokensCached[1].contains(xmlResult.responseNode.data.testId.toString()))
        assertTrue(tokensCached[2].contains("3"))
        assertTrue(tokensCached[3].contains("1#waterfall_viewHPFF_BV1_Step01_Homepage - netlab"))
    }

    /**
     * Tests the persisting of various domain-objects while listening to incoming {@link EventResult}s.
     * @see #listenToResult(String)
     * @see #expectedAfterResultListening
     */
    @Test
    void testCreationOfDomainsAfterListenToIncomingResults() {
        mockInnerServices()

        //test execution and assertions
        expectedAfterResultListening.each { k, v ->
            deleteAllRelevantDomains()
            listenToResultAndProofCreatedDomains(k, v)
        }
    }

    /**
     * Tests the persisting of various domain-objects while listening to incoming {@link EventResult}s.
     * @see #listenToResult(String)
     * @see #expectedAfterResultListening
     */
    @Test
    void testCreationOfDomainsAfterListenToIncomingInvalidResults() {
        mockInnerServices()

        //test execution and assertions
        String k = 'Result_NoMultistep_Error_testCompletedButThereWereNoSuccessfulResults.xml'
        Map v = ['expectedNumberOfLocations'              : 1,
                 'expectedJobLabel'                       : 'vb_agent1_IE8_BV1_Step05_Warenkorbbestaetigung',
                 'expectedNumberOfJobRuns'                : 1,
                 'expectedNumberOfRuns'                   : 0,
                 'expectedResultExecutionDateTime'        : new DateTime(new Date('Wed, 30 Jan 2013 12:00:48 +0000')),
                 'expectedNumberOfSteps'                  : 0,
                 'expectedNumberOfStepsWithAssociatedPage': 0,
                 'expectedNumberOfCachedViews'            : 0,
        ]

        deleteAllRelevantDomains()
        shouldFail() {
            listenToResultAndProofCreatedDomains(k, v)
        }
        assertEquals(0, JobResult.count())
    }

    @Test
    void testFetchLocationIfNoneIsFound() {

        //create test-specific data
        String testNameXML = "Result_NoMultistep_1Run_JustFirstView.xml";
        File file = new File("test/resources/WptResultXmls/${testNameXML}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        mockProxyService(xmlResult.responseNode.data.location.toString())

        deleteAllRelevantDomains() // No Locations left!

        //test execution
        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions
        Job job = Job.findByLabel('testjob')
        assertEquals(job.getLocation().getWptServer(), server1);
    }

    @Test
    void testSaveOfWptServerOfJob() {

        //create test-specific data
        String testNameXML = "Result_NoMultistep_1Run_JustFirstView.xml";
        File file = new File("test/resources/WptResultXmls/${testNameXML}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution

        serviceUnderTest.listenToResult(xmlResult, server1)

        //assertions
        Job job = Job.findByLabel('testjob')
        assertEquals(job.getLocation().getWptServer(), server1);
    }

    @Test
    void testUpdateOfWptServerOfJob() {

        //create test-specific data
        String testNameXML = "Result_NoMultistep_1Run_JustFirstView.xml";
        File file = new File("test/resources/WptResultXmls/${testNameXML}")
        WptResultXml xmlResult = new WptResultXml(new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server2);

        //XML-Result (TestID) reset:
        String newTestId = "130622_FA_1AX2"

        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
        deleteAllRelevantDomains()

        //test execution

        serviceUnderTest.listenToResult(xmlResult, server1)

        //reset xmlResultID
        xmlResult.responseNode.data.testId = newTestId;
        serviceUnderTest.listenToResult(xmlResult, server2)

        System.out.println(xmlResult.responseNode.data.testId);

        //assertions

        Job job = Job.findByLabel('testjob')
        assertEquals(server2, job.getLocation().getWptServer());

        List<JobResult> jobResults = JobResult.findAll();
        assertEquals(2, jobResults.size());

        jobResults.each { JobResult jr ->
            if (jr.getTestId() == newTestId) {
                assertEquals(server2.getBaseUrl(), jr.getWptServerBaseurl());
                assertEquals(server2.getLabel(), jr.getWptServerLabel());
            } else {
                assertEquals(server1.getBaseUrl(), jr.getWptServerBaseurl());
                assertEquals(server1.getLabel(), jr.getWptServerLabel());

            }

        }
    }

    /**
     * Executes test for given webpagetest-result-xml-file (nameOfResultXmlFile) and proofs existing domain-objects afterwards:<br>
     * Proofs for {@link Location}:
     * <ul>
     * <li>Number of created objects</li>
     * </ul>
     * Proofs for {@link Job}:
     * <ul>
     * <li>Number of created objects</li>
     * <li>Whether {@link Job#lastRun} is expectedResultExecutionDateTime</li>
     * </ul>
     * Proofs for {@link JobResult}:
     * <ul>
     * <li>Number of created objects</li>
     * <li>Whether {@link JobResult#date} is expectedResultExecutionDateTime</li>
     * </ul>
     * Proofs for {@link MeasuredEvent}:
     * <ul>
     * <li>Number of created objects</li>
     * </ul>
     * Proofs for {@link EventResult}:
     * <ul>
     * <li>various tests</li>
     * </ul>
     * @param nameOfResultXmlFile
     * 			Name of the result-xml-file from webpagetest (testdata from test/resources/).
     * @param expectedValues
     * 			Map with expected values for assertions.
     * 			Structure of the map:<br>
     * 			[name of expected value 1: expectedValue1,<br>
     * 			name of expected value 2: expectedValue2,<br>
     * 			...<br>
     * 			name of expected value n: expectedValueN]
     */
    private void listenToResultAndProofCreatedDomains(String nameOfResultXmlFile, Map expectedValues) {
        //test specific data
        File file = new File("test/resources/WptResultXmls/${nameOfResultXmlFile}")
        WptResultXml xmlResult = new WptResultXml (new XmlSlurper().parse(file))
        String har = new File('test/resources/HARs/singleResult.har').getText()
        createLocationIfNotExistent(xmlResult.responseNode.data.location.toString(), undefinedBrowser, server1);

        //test execution
        serviceUnderTest.listenToResult(xmlResult, server1)

        //check for job-runs
        Collection<JobResult> jobRuns = JobResult.list()
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedNumberOfJobRuns - ", expectedValues['expectedNumberOfJobRuns'], jobRuns.size())

        //check dates of job and job_results
        Job job = Job.findByLabel(expectedValues['expectedJobLabel'])
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedResultExecutionDateTime in job.lastRun - ",
                expectedValues['expectedResultExecutionDateTime'], new DateTime(job.lastRun))
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedResultExecutionDateTime in jobResult.date - ",
                expectedValues['expectedResultExecutionDateTime'], new DateTime(jobRuns[0].date))
        //TODO 2013-10-24: proof all CSI-relevant attributes of job_results

        //check for steps
        List<MeasuredEvent> steps = MeasuredEvent.list()
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedNumberOfSteps - ", expectedValues['expectedNumberOfSteps'], steps.size())
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedNumberOfStepsWithAssociatedPage - ", expectedValues['expectedNumberOfStepsWithAssociatedPage'],
                steps.findAll { !it.testedPage.isUndefinedPage() }.size())

        //check for results
        List<EventResult> allResults = EventResult.list()
        int expectedSizeOfAllResults = expectedValues['expectedNumberOfRuns'] * expectedValues['expectedNumberOfCachedViews'] * expectedValues['expectedNumberOfSteps']
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedSizeOfAllResults - ", expectedSizeOfAllResults, allResults.size())

        int expectedSizeOfAllMedianValues = expectedValues['expectedNumberOfCachedViews'] * expectedValues['expectedNumberOfSteps']
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedSizeOfAllMedianValues - ", expectedSizeOfAllMedianValues, EventResult.findAllByMedianValue(true).size())

        int expectedSizeOfAllNonMedianValues = (expectedValues['expectedNumberOfRuns'] - 1) * expectedValues['expectedNumberOfCachedViews'] * expectedValues['expectedNumberOfSteps']
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedSizeOfAllNonMedianValues - ", expectedSizeOfAllNonMedianValues, EventResult.findAllByMedianValue(false).size())

        expectedValues['expectedNumberOfRuns'].times {
            int expectedSizeOfResults = expectedValues['expectedNumberOfCachedViews'] * expectedValues['expectedNumberOfSteps']
            assertEquals("xml-result '${nameOfResultXmlFile}' expectedSizeOfResults - ", expectedSizeOfResults, EventResult.findAllByNumberOfWptRun(it + 1).size())
        }

        int expectedSizeOfUnCachedViewResults = expectedValues['expectedNumberOfRuns'] * expectedValues['expectedNumberOfSteps']
        assertEquals("xml-result '${nameOfResultXmlFile}' expectedSizeOfUnCachedViewResults - ", expectedSizeOfUnCachedViewResults, EventResult.findAllByCachedView(CachedView.UNCACHED).size())

        // TODO Do not use branches in verification part
        if (expectedValues['expectedNumberOfCachedViews'] == 2) {
            int expectedSizeOfCachedResultsForTwoViews = expectedValues['expectedNumberOfRuns'] * expectedValues['expectedNumberOfSteps']
            assertEquals("xml-result '${nameOfResultXmlFile}' expectedSizeOfCachedResultsForTwoViews - ", expectedSizeOfCachedResultsForTwoViews, EventResult.findAllByCachedView(CachedView.CACHED).size())
        }
    }

    private void deleteAllRelevantDomains() {
//		Location.findAll().each {it.delete(flush: true)}
//		Job.list()*.delete(flush: true)
        JobResult.list()*.delete(flush: true)
        MeasuredEvent.list()*.delete(flush: true)
        EventResult.list()*.delete(flush: true)
    }

    // mocks ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private void mockInnerServices() {
        //mocking of inner services

        mockCsiAggregationUpdateService()
        ServiceMocker.create().mockTTCsMappingService(serviceUnderTest)
        mockPageService()
        mockCsiAggregationTagService('notTheConcernOfThisTest')
    }

    private void createLocationIfNotExistent(String locationIdentifier, Browser browser, WebPageTestServer server) {
        Location alreadyExistent = Location.findByWptServerAndUniqueIdentifierForServer(server, locationIdentifier)
        if (!alreadyExistent) {
            new Location(
                    active: true,
                    valid: 1,
                    uniqueIdentifierForServer: locationIdentifier, // z.B. Agent1-wptdriver:Chrome
                    location: "UNIT_TEST_LOCATION",//z.B. Agent1-wptdriver
                    label: "Unit Test Location: Browser?",//z.B. Agent 1: Windows 7 (S008178178)
                    browser: browser,//z.B. Firefox
                    wptServer: server,
                    dateCreated: new Date(),
                    lastUpdated: new Date()
            ).save(failOnError: true);
        }
    }

    private void mockProxyService(String locationIdentifier) {
        def proxyService = grailsApplication.mainContext.getBean('proxyService')
        proxyService.metaClass.fetchLocations = { WebPageTestServer server ->
            createLocationIfNotExistent(locationIdentifier, undefinedBrowser, server);
        }
        serviceUnderTest.proxyService = proxyService
    }

    private void mockBrowserService() {
        def browserService = grailsApplication.mainContext.getBean('browserService')
        browserService.metaClass.findByNameOrAlias = { String nameOrAlias ->
            //not the concern of this test
            if (nameOrAlias.startsWith("IE"))
                return Browser.findByName('IE');
            else if (nameOrAlias.startsWith("FF") || nameOrAlias.startsWith("Firefox"))
                return Browser.findByName('FF');
            else if (nameOrAlias.startsWith("Chrome"))
                return Browser.findByName('Chrome');
            else {
                return Browser.findByName(Browser.UNDEFINED);
            }
        }
        serviceUnderTest.browserService = browserService
    }

    private void mockCsiAggregationUpdateService() {
        def csiAggregationUpdateServiceMocked = grailsApplication.mainContext.getBean('csiAggregationUpdateService')
        csiAggregationUpdateServiceMocked.metaClass.createOrUpdateDependentMvs = { EventResult newResult ->
            //not the concern of this test
            return 34d
        }
        serviceUnderTest.csiAggregationUpdateService = csiAggregationUpdateServiceMocked
    }

    private void mockPageService() {
        def pageServiceMocked = grailsApplication.mainContext.getBean('pageService')
        pageServiceMocked.metaClass.getPageByStepName = { String pageName ->
            def tokenized = pageName.tokenize(PageService.STEPNAME_DELIMITTER)
            return tokenized.size() == 2 ? Page.findByName(tokenized[0]) : Page.findByName(Page.UNDEFINED)
        }
        pageServiceMocked.metaClass.getDefaultStepNameForPage = { Page page ->
            //not the concern of this test
            return Page.findByName('HP').name + PageService.STEPNAME_DELIMITTER + PageService.STEPNAME_DEFAULT_STEPNUMBER
        }
        pageServiceMocked.metaClass.excludePagenamePart = { String stepName ->
            return stepName.contains(PageService.STEPNAME_DELIMITTER) ?
                    stepName.substring(stepName.indexOf(PageService.STEPNAME_DELIMITTER) + PageService.STEPNAME_DELIMITTER.length(), stepName.length()) :
                    stepName
        }
        serviceUnderTest.pageService = pageServiceMocked
    }

    private void mockCsiAggregationTagService(String tagToReturn) {
        def csiAggregationTagService = grailsApplication.mainContext.getBean('csiAggregationTagService')
        csiAggregationTagService.metaClass.createEventResultTag = {
            JobGroup jobGroup,
            MeasuredEvent measuredEvent,
            Page page,
            Browser browser,
            Location location ->
                return tagToReturn
        }
        csiAggregationTagService.metaClass.findJobGroupOfEventResultTag = {
            String tag ->
                return undefinedJobGroup
        }
        serviceUnderTest.csiAggregationTagService = csiAggregationTagService
    }

    private void mockMetricReportingService() {
        def metricReportingService = grailsApplication.mainContext.getBean('metricReportingService')
        metricReportingService.metaClass.reportEventResultToGraphite = {
            EventResult result ->
                // not the concern of this test
        }
        serviceUnderTest.metricReportingService = metricReportingService
    }

    // create testdata common to all tests /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private createBrowsers() {
        String browserName = Browser.UNDEFINED
        undefinedBrowser = new Browser(
                name: browserName,
                weight: 0)
                .addToBrowserAliases(alias: Browser.UNDEFINED)
                .save(failOnError: true)

        browserName = "IE"
        new Browser(
                name: browserName,
                weight: 45)
                .addToBrowserAliases(alias: "IE")
                .addToBrowserAliases(alias: "IE8")
                .addToBrowserAliases(alias: "Internet Explorer")
                .addToBrowserAliases(alias: "Internet Explorer 8")
                .save(failOnError: true)
        browserName = "FF"
        new Browser(
                name: browserName,
                weight: 55)
                .addToBrowserAliases(alias: "FF")
                .addToBrowserAliases(alias: "FF7")
                .addToBrowserAliases(alias: "Firefox")
                .addToBrowserAliases(alias: "Firefox7")
                .save(failOnError: true)

        browserName = "Chrome"
        new Browser(
                name: browserName,
                weight: 55)
                .addToBrowserAliases(alias: "Chrome")
                .save(failOnError: true)
    }

    private void createPages() {
        ['HP', 'MES', Page.UNDEFINED].each { pageName ->
            Double weight = 0
            switch (pageName) {
                case 'HP': weight = 6; break
                case 'MES': weight = 9; break
                case 'SE': weight = 36; break
                case 'ADS': weight = 43; break
                case 'WKBS': weight = 3; break
                case 'WK': weight = 3; break
            }
            new Page(
                    name: pageName,
                    weight: weight).save(failOnError: true)
        }
    }

    void createTestDataCommonForAllTests() {
        server1 = new WebPageTestServer(
                label: "TestServer 1",
                proxyIdentifier: "TestServer1",
                baseUrl: "http://wptUnitTest.dev.hh.iteratec.local",
                active: true,
                dateCreated: new Date(),
                lastUpdated: new Date()
        ).save(failOnError: true, validate: false)

        server2 = new WebPageTestServer(
                label: "TestServer 2",
                proxyIdentifier: "TestServer2",
                baseUrl: "http://wptUnitTest2.dev.hh.iteratec.local",
                active: 1,
                dateCreated: new Date(),
                lastUpdated: new Date()
        ).save(failOnError: true, validate: false)

        undefinedJobGroup = new JobGroup(
                name: JobGroup.UNDEFINED_CSI
        );
        undefinedJobGroup.save(failOnError: true);

        //creating test-data common to all tests
        createPages()
        createBrowsers()
        Location testLocation = TestDataUtil.createLocation(server1, 'test-location', Browser.findByName('IE'), true)
        Script testScript = TestDataUtil.createScript('test-script', 'description', 'navigate   http://my-url.de', false)
        TestDataUtil.createJob('ie_step_testjob', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('http://www.example.de.de - Multiple steps with event names + dom elements', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('FF_BV1_Step01_Homepage - netlab', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('testjob', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('FF_BV1_Multistep_2', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('IE_otto_hp_singlestep', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('HP:::FF_BV1_Step01_Homepage - netlab', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('example.de - Multiple steps with event names + dom elements', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)
        TestDataUtil.createJob('FF_Otto_multistep', testScript, testLocation, undefinedJobGroup, '', 1, false, 60)

        CsiConfiguration csiConfiguration = TestDataUtil.createCsiConfiguration()
        csiConfiguration.timeToCsMappings = TestDataUtil.createTimeToCsMappingForAllPages(Page.list())
        undefinedJobGroup.csiConfiguration = csiConfiguration
        ServiceMocker mockGenerator = ServiceMocker.create()
        serviceUnderTest.csiValueService = grailsApplication.mainContext.getBean('csiValueService')
        mockGenerator.mockOsmConfigCacheService(serviceUnderTest.csiValueService)
    }
}
