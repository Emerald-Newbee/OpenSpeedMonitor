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

import de.iteratec.osm.report.chart.CsiAggregationUtilService
import grails.test.mixin.integration.Integration
import grails.transaction.Rollback

import org.joda.time.DateTime

import de.iteratec.osm.measurement.schedule.Job
import de.iteratec.osm.measurement.schedule.JobGroup

import de.iteratec.osm.report.chart.AggregatorType
import de.iteratec.osm.report.chart.MeasurandGroup
import de.iteratec.osm.report.chart.CsiAggregation
import de.iteratec.osm.report.chart.CsiAggregationInterval
import de.iteratec.osm.result.MeasuredEvent
import de.iteratec.osm.measurement.script.Script
import de.iteratec.osm.measurement.environment.Browser
import de.iteratec.osm.measurement.environment.Location
import de.iteratec.osm.measurement.environment.WebPageTestServer

/**
 * Contains tests which test the creation of {@link de.iteratec.osm.report.chart.CsiAggregation}s without the existence of corresponding {@link EventResult}s.<br>
 * For all persisted {@link de.iteratec.osm.report.chart.CsiAggregation}s a {@link CsiAggregationUpdateEvent} should be created, which marks measured value as calculated.
 * @author nkuhn
 *
 */
@Integration
@Rollback
class CreatingYesNoDataMvsIntTests extends NonTransactionalIntegrationSpec {
    /** injected by grails */
    PageCsiAggregationService pageCsiAggregationService
    ShopCsiAggregationService shopCsiAggregationService
    CsiAggregationUtilService csiAggregationUtilService

    AggregatorType job
    AggregatorType page
    AggregatorType shop
    DateTime startOfCreatingHourlyEventValues = new DateTime(2012, 1, 9, 0, 0, 0)
    DateTime startOfCreatingWeeklyPageValues = new DateTime(2012, 2, 6, 0, 0, 0)
    DateTime startOfCreatingWeeklyShopValues = new DateTime(2012, 3, 12, 0, 0, 0)

    /**
     * Creating testdata.
     */

    def setup(){
        CsiAggregationInterval.withNewTransaction {
            TestDataUtil.createCsiAggregationIntervals()
            TestDataUtil.createAggregatorTypes()
            createPagesAndEvents()
            createBrowsers()
            createHoursOfDay()
            createServer()
            createLocations()
            createJobGroups()
            createJobs()
        }
        job = AggregatorType.findByName(AggregatorType.MEASURED_EVENT)
        page = AggregatorType.findByName(AggregatorType.PAGE)
        shop = AggregatorType.findByName(AggregatorType.SHOP)
    }

    /**
     * Creating weekly-page {@link CsiAggregation}s without data.
     */
    void "Creating weekly page values test"() {
        given:
        DateTime endDate = startOfCreatingWeeklyPageValues.plusWeeks(1)
        CsiAggregationInterval weeklyInterval = CsiAggregationInterval.findByIntervalInMinutes(CsiAggregationInterval.WEEKLY);
        Integer countPages = 7
        Integer countWeeks = 2
        when:
        List<CsiAggregation> wpmvs = pageCsiAggregationService.getOrCalculatePageCsiAggregations(startOfCreatingWeeklyPageValues.toDate(), endDate.toDate(), weeklyInterval, JobGroup.findAllByCsiConfigurationIsNotNull(), Page.list())

        then:
        wpmvs.size() == countWeeks * countPages
        wpmvs.each {
            it.isCalculated()
        }
    }

    /**
     * Creating weekly-shop {@link de.iteratec.osm.report.chart.CsiAggregation}s without data.
     */
    void "Creating weekly shop values test"() {
        given:
        DateTime endDate = startOfCreatingWeeklyShopValues.plusWeeks(1)
        Integer countWeeks = 2
        Integer countPages = 7
        when:
        List<CsiAggregation> wsmvs = shopCsiAggregationService.getOrCalculateWeeklyShopCsiAggregations(startOfCreatingWeeklyShopValues.toDate(), endDate.toDate())
        Date endOfLastWeek = csiAggregationUtilService.resetToEndOfActualInterval(endDate, CsiAggregationInterval.WEEKLY).toDate()
        then:
        wsmvs.size() == countWeeks
        wsmvs.each {
            it.isCalculated()
        }
        pageCsiAggregationService.findAll(startOfCreatingWeeklyShopValues.toDate(), endDate.toDate(), CsiAggregationInterval.findByIntervalInMinutes(CsiAggregationInterval.WEEKLY)).size() == countWeeks * countPages

    }

    private static void createJobGroups() {
        CsiDay csiDay = new CsiDay()
        0..23.each {csiDay.setHourWeight(it, 0.5)}
        String csiGroupName = 'CSI'
        CsiConfiguration csiConfiguration = new CsiConfiguration(
            label: "TestLabel",
            description:  "TestDescription",
            csiDay: csiDay,
            browserConnectivityWeights: [],
            pageWeights: [],
            timeToCsMappings: []).save()
        JobGroup.findByName(csiGroupName) ?: new JobGroup(
                name: csiGroupName, csiConfiguration: csiConfiguration ).save(failOnError: true)
    }

    private static void createJobs() {
        Script script = Script.createDefaultScript('Unnamed').save(failOnError: true)
        Location locationFf = Location.findByLabel('ffLocationLabel')
        Location locationIe = Location.findByLabel('ieLocationLabel')
        JobGroup jobGroup = JobGroup.findByName('CSI')
        Page pageHp = Page.findByName('HP')
        Page pageMes = Page.findByName('MES')

        Job testjob_HP = new Job(
                label: 'testjob_HP',
                location: locationFf,
                page: pageHp,
                active: false,
                description: '',
                runs: 1,
                jobGroup: jobGroup,
                script: script,
                maxDownloadTimeInMinutes: 60,
                noTrafficShapingAtAll: true
        ).save(failOnError: true)

        Job testjob_MES = new Job(
                label: 'testjob_MES',
                location: locationIe,
                page: pageMes,
                active: false,
                description: '',
                runs: 1,
                jobGroup: jobGroup,
                script: script,
                maxDownloadTimeInMinutes: 60,
                noTrafficShapingAtAll: true
        ).save(failOnError: true)
    }

    private static void createHoursOfDay() {
        CsiDay day = new CsiDay()
        day.with {
            hour0Weight = 2.9
            hour1Weight = 0.4
            hour2Weight = 0.2
            hour3Weight = 0.1
            hour4Weight = 0.1
            hour5Weight = 0.2
            hour6Weight = 0.7
            hour7Weight = 1.7
            hour8Weight = 3.2
            hour9Weight = 4.8
            hour10Weight = 5.6
            hour11Weight = 5.7
            hour12Weight = 5.5
            hour13Weight = 5.8
            hour14Weight = 5.9
            hour15Weight = 6.0
            hour16Weight = 6.7
            hour17Weight = 7.3
            hour18Weight = 7.7
            hour19Weight = 8.8
            hour20Weight = 9.3
            hour21Weight = 7.0
            hour22Weight = 3.6
            hour23Weight = 0.9
        }
        day.save(failOnError: true)
    }

    private static void createPagesAndEvents() {
        ['HP', 'MES', 'SE', 'ADS', 'WKBS', 'WK', Page.UNDEFINED].each { pageName ->
            Double weight = 0
            switch (pageName) {
                case 'HP': weight = 6; break
                case 'MES': weight = 9; break
                case 'SE': weight = 36; break
                case 'ADS': weight = 43; break
                case 'WKBS': weight = 3; break
                case 'WK': weight = 3; break
            }
            Page page = Page.findByName(pageName) ?: new Page(
                    name: pageName,
                    weight: weight).save(failOnError: true)

            // Simply create one event
            new MeasuredEvent(
                    name: 'CreatingYesNoDataMvsIntTests-' + pageName,
                    testedPage: page
            ).save(failOnError: true)
        }
    }

    private static void createBrowsers() {
        String browserName = "undefined"
        Browser.findByName(browserName) ?: new Browser(
                name: browserName,
                weight: 0)
                .addToBrowserAliases(alias: "undefined")
                .save(failOnError: true)
        browserName = "IE"
        Browser browserIE = Browser.findByName(browserName) ?: new Browser(
                name: browserName,
                weight: 45)
                .addToBrowserAliases(alias: "IE")
                .addToBrowserAliases(alias: "IE8")
                .addToBrowserAliases(alias: "Internet Explorer")
                .addToBrowserAliases(alias: "Internet Explorer 8")
                .save(failOnError: true)
        browserName = "FF"
        Browser browserFF = Browser.findByName(browserName) ?: new Browser(
                name: browserName,
                weight: 55)
                .addToBrowserAliases(alias: "FF")
                .addToBrowserAliases(alias: "FF7")
                .addToBrowserAliases(alias: "Firefox")
                .addToBrowserAliases(alias: "Firefox7")
                .save(failOnError: true)
    }

    private static void createServer() {
        WebPageTestServer server1
        server1 = new WebPageTestServer(
                baseUrl: 'http://wpt.server.de',
                active: true,
                label: 'server 1 - wpt server',
                proxyIdentifier: 'server 1 - wpt server',
                dateCreated: new Date(),
                lastUpdated: new Date()
        ).save(failOnError: true)
    }

    private static void createLocations() {
        WebPageTestServer server1 = WebPageTestServer.findByLabel('server 1 - wpt server')
        Browser browserFF = Browser.findByName("FF")
        Browser browserIE = Browser.findByName("IE")
        Location ffAgent1, ieAgent1
        ffAgent1 = new Location(
                active: true,
                valid: 1,
                location: 'ffLocationLocation',
                label: 'ffLocationLabel',
                browser: browserFF,
                wptServer: server1,
                dateCreated: new Date(),
                lastUpdated: new Date()
        ).save(failOnError: true)
        ieAgent1 = new Location(
                active: true,
                valid: 1,
                location: 'ieLocationLocation',
                label: 'ieLocationLabel',
                browser: browserIE,
                wptServer: server1,
                dateCreated: new Date(),
                lastUpdated: new Date()
        ).save(failOnError: true)
    }
}
