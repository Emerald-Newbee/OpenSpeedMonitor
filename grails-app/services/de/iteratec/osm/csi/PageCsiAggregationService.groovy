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

import de.iteratec.osm.measurement.schedule.ConnectivityProfile
import grails.transaction.Transactional

import static de.iteratec.osm.util.Constants.*

import org.joda.time.DateTime

import de.iteratec.osm.report.chart.CsiAggregationDaoService
import de.iteratec.osm.report.chart.CsiAggregationUtilService
import de.iteratec.osm.measurement.schedule.JobGroup

import de.iteratec.osm.measurement.schedule.JobService
import de.iteratec.osm.report.chart.AggregatorType
import de.iteratec.osm.report.chart.CsiAggregation
import de.iteratec.osm.report.chart.CsiAggregationInterval
import de.iteratec.osm.report.chart.CsiAggregationUpdateEvent
import de.iteratec.osm.report.chart.CsiAggregationUpdateEventDaoService
import de.iteratec.osm.csi.weighting.WeightFactor
import de.iteratec.osm.csi.weighting.WeightedCsiValue
import de.iteratec.osm.csi.weighting.WeightingService
import de.iteratec.osm.result.EventResult
import de.iteratec.osm.result.CsiAggregationTagService
import de.iteratec.osm.result.MvQueryParams
import de.iteratec.osm.result.dao.MeasuredEventDaoService
import de.iteratec.osm.util.PerformanceLoggingService
import de.iteratec.osm.util.PerformanceLoggingService.IndentationDepth
import de.iteratec.osm.util.PerformanceLoggingService.LogLevel
import de.iteratec.osm.measurement.environment.Browser
import de.iteratec.osm.measurement.environment.Location

@Transactional
class PageCsiAggregationService {

    EventCsiAggregationService eventCsiAggregationService
    JobService jobService
    CustomerSatisfactionWeightService customerSatisfactionWeightService
    CsiHelperService csiHelperService
    MeanCalcService meanCalcService
    CsiAggregationTagService csiAggregationTagService
    PerformanceLoggingService performanceLoggingService
    MeasuredEventDaoService measuredEventDaoService
    CsiAggregationDaoService csiAggregationDaoService
    CsiAggregationUtilService csiAggregationUtilService
    WeightingService weightingService
    CsiAggregationUpdateEventDaoService csiAggregationUpdateEventDaoService

    /**
     * Just gets {@link CsiAggregation}s from DB. No creation or calculation.
     * @param fromDate
     * @param toDate
     * @param targetInterval
     * @return
     */
    List<CsiAggregation> findAll(Date fromDate, Date toDate, CsiAggregationInterval targetInterval) {
        List<CsiAggregation> result = []

        def query = CsiAggregation.where {
            started >= fromDate
            started <= toDate
            interval == targetInterval
            aggregator == AggregatorType.findByName(AggregatorType.PAGE)
        }
        result = query.list()
        return result
    }

    /**
     * <p>
     * Finds page {@link CsiAggregation}s from DB.
     * No creation or calculation is performed.
     * </p>
     *
     * <p>
     * <em>Note:</em> Passing an empty list to {@code groups} or {@code pages}
     * at least one of the arguments will cause that no result is found.
     * </p>
     *
     * @param fromDate
     *         First {@link Date} (inclusive) of a test start date for that
     *         result CsiAggregations to find,
     *         not <code>null</code>.
     * @param toDate
     *         Last {@link Date} (inclusive) of a test start date for that
     *         result CsiAggregations to find,
     *         not <code>null</code>.
     * @param targetInterval
     *         The interval of the page CsiAggregations to find, currently only
     * {@link CsiAggregationInterval#WEEKLY} is supported,
     *         not <code>null</code> (this argument is deprecated).
     * @param groups
     *         The groups for that CsiAggregations should be found,
     *         not <code>null</code>.
     * @param pages
     *         The pages for that CsiAggregations should be found,
     *         not <code>null</code>.
     *
     * @return Found (weekly) page CsiAggregations, not <code>null</code>.
     *
     * @see CsiAggregation#getStarted()
     */
    public List<CsiAggregation> findAll(Date fromDate, Date toDate,
                                        @Deprecated
                                               CsiAggregationInterval targetInterval,
                                        List<JobGroup> groups, List<Page> pages) {
        List<CsiAggregation> result = []
        if (groups.size() == 0 || pages.size() == 0) {
            return result
        }
        String tagPattern = csiAggregationTagService.getTagPatternForWeeklyPageCasWithJobGroupsAndPages(groups, pages)
        result = csiAggregationDaoService.getMvs(
                fromDate,
                toDate,
                tagPattern,
                targetInterval,
                AggregatorType.findByName(AggregatorType.PAGE))
        return result
    }

    /**
     * Marks {@link CsiAggregation}s which depend from param newResult and who's interval contains newResult as outdated.
     * @param startich bin hier auch gerade nur
     * 			00:00:00 of the respective interval.
     * @param newResult
     * 			New {@link EventResult}.
     */
    void markMvAsOutdated(DateTime start, EventResult newResult, CsiAggregationInterval interval) {
        String pageTag = csiAggregationTagService.createPageAggregatorTagByEventResult(newResult)
        if (pageTag) {
            CsiAggregation pageMv = ensurePresence(start, interval, pageTag)
            csiAggregationUpdateEventDaoService.createUpdateEvent(pageMv.ident(), CsiAggregationUpdateEvent.UpdateCause.OUTDATED)
        }
    }

    /**
     * Provides {@link CsiAggregation}s for given {@link Page}s, {@link JobGroup}s and a {@link CsiAggregationInterval} between toDate and fromDate.
     * Non-existent {@link CsiAggregation}s will be created.
     * All {@link CsiAggregation}s with @{link CsiAggregation.Calculated.Not} will be calculated and persisted with @{link CsiAggregation.Calculated.Yes}* or @{link CsiAggregation.Calculated.YesNoData}.
     * @param fromDateTime
     * @param toDateTime
     * @param interval
     * @param csiGroups
     * @param pages
     * @return
     */
    List<CsiAggregation> getOrCalculatePageCsiAggregations(Date fromDate, Date toDate, CsiAggregationInterval interval, List<JobGroup> csiGroups, List<Page> pages = Page.list()) {

        DateTime toDateTime = new DateTime(toDate)
        DateTime fromDateTime = new DateTime(fromDate)
        if (fromDateTime.isAfter(toDateTime)) {
            throw new IllegalArgumentException("toDate must not be later than fromDate: fromDate=${fromDate}; toDate=${toDate}")
        }

        Integer numberOfIntervals = csiAggregationUtilService.getNumberOfIntervals(fromDateTime, toDateTime, interval)
        List<CsiAggregation> existingCsiAggregations = findAll(fromDateTime.toDate(), toDateTime.toDate(), interval, csiGroups, pages)

        List<CsiAggregation> openCsiAggregations = existingCsiAggregations.findAll { !it.closedAndCalculated }
        Boolean allCsiAggregationsExist = existingCsiAggregations.size() == numberOfIntervals * csiGroups.size() * pages.size()
        if (allCsiAggregationsExist && openCsiAggregations.size() == 0) {
            return existingCsiAggregations
        }

        List<CsiAggregationUpdateEvent> updateEvents = []
        if (openCsiAggregations.size() > 0) updateEvents.addAll(csiAggregationDaoService.getUpdateEvents(openCsiAggregations*.ident()))
        List<CsiAggregation> csiAggregationsToBeCalculated = openCsiAggregations.findAll {
            it.hasToBeCalculatedAccordingEvents(updateEvents)
        }

        if (allCsiAggregationsExist && csiAggregationsToBeCalculated.size() == 0) {

            return existingCsiAggregations;

        } else {

            List<CsiAggregation> calculatedCsiAggregations = []
            DateTime currentDateTime = fromDateTime
            while (!currentDateTime.isAfter(toDateTime)) {
                performanceLoggingService.logExecutionTime(LogLevel.INFO, " get/create/calculate ${interval.name} page-CsiAggregation for: ${currentDateTime}", IndentationDepth.TWO) {
                    List<CsiAggregation> existingMvsOfCurrentTime = existingCsiAggregations.findAll {
                        new DateTime(it.started) == currentDateTime
                    }
                    List<CsiAggregation> mvsToBeCalculatedOfCurrentTime = csiAggregationsToBeCalculated.findAll {
                        new DateTime(it.started) == currentDateTime
                    }
                    if (existingMvsOfCurrentTime.size() == csiGroups.size() * pages.size() &&
                            mvsToBeCalculatedOfCurrentTime.size() == 0) {
                        calculatedCsiAggregations.addAll(existingMvsOfCurrentTime)

                    } else {
                        calculatedCsiAggregations.addAll(
                                getOrCalculatePageMvs(currentDateTime, interval, csiGroups, pages, updateEvents)
                        )

                    }
                }
                currentDateTime = csiAggregationUtilService.addOneInterval(currentDateTime, interval.intervalInMinutes)

            }
            return calculatedCsiAggregations
        }
    }

    private List<CsiAggregation> getOrCalculatePageMvs(DateTime toGetMvsFor, CsiAggregationInterval interval, List<JobGroup> csiGroups, List<Page> csiPages, List<CsiAggregationUpdateEvent> updateEvents) {
        List<CsiAggregation> calculatedPageMvs = []
        CsiAggregationCachingContainer cachingContainer = new CsiAggregationCachingContainer()
        cachingContainer.hCsiAggregationsByCsiGroupPageCombination = getHmvsByCsiGroupPageCombinationMap(csiGroups, csiPages, toGetMvsFor, toGetMvsFor.plusMinutes(interval.getIntervalInMinutes()))
        csiGroups.each { JobGroup group ->
            cachingContainer.csiGroupToCalcCsiAggregationFor = group
            csiPages.each { Page page ->
                cachingContainer.pageToCalcCsiAggregationFor = page
                String tag = csiAggregationTagService.createPageAggregatorTag(group, page)
                calculatedPageMvs.add(ensurePresenceAndCalculation(toGetMvsFor, interval, tag, cachingContainer, updateEvents))
            }
        }
        return calculatedPageMvs
    }

    Map<String, List<CsiAggregation>> getHmvsByCsiGroupPageCombinationMap(List<JobGroup> csiGroups, List<Page> csiPages, DateTime startDateTime, DateTime endDateTime) {
        List<CsiAggregation> hourlyCsiAggregations
        performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "  calcMvForPageAggregator - getHmvs: getting", IndentationDepth.FOUR) {
            MvQueryParams queryParams = new MvQueryParams();
            queryParams.jobGroupIds.addAll(csiGroups*.ident());
            queryParams.pageIds.addAll(csiPages*.ident());
            queryParams.measuredEventIds.addAll(measuredEventDaoService.getEventsFor(csiPages)*.ident());
            queryParams.browserIds.addAll(Browser.list()*.ident());
            queryParams.locationIds.addAll(Location.list()*.ident());
            queryParams.connectivityProfileIds.addAll(ConnectivityProfile.list()*.ident())
            hourlyCsiAggregations = eventCsiAggregationService.getHourlyCsiAggregations(startDateTime.toDate(), endDateTime.toDate(), queryParams)
        }
        Map<String, List<CsiAggregation>> hmvsByCsiGroupPageCombination
        performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "  calcMvForPageAggregator - getHmvs: iterate and write to map", IndentationDepth.FOUR) {
            hmvsByCsiGroupPageCombination = [:].withDefault { [] }
            hourlyCsiAggregations.each { hmv ->
                JobGroup groupOfHmv = csiAggregationTagService.findJobGroupOfHourlyEventTag(hmv.tag)
                Page pageOfHmv = csiAggregationTagService.findPageOfHourlyEventTag(hmv.tag)
                if (groupOfHmv && pageOfHmv && csiGroups.contains(groupOfHmv) && csiPages.contains(pageOfHmv)) {
                    hmvsByCsiGroupPageCombination["${groupOfHmv.ident() + UNIQUE_STRING_DELIMITTER + pageOfHmv.ident()}"].add(hmv)
                }
            }
        }
        return hmvsByCsiGroupPageCombination
    }
    /**
     * Creates respective {@link CsiAggregation} if it doesn't exist and calculates it.
     * After calculation status is {@link de.iteratec.osm.report.chart.CsiAggregation.Calculated.Yes} or {@link CsiAggregation.Calculated.YesNoData}.
     * @param startDate
     * @param interval
     * @param tag
     * @param cachingContainer
     * @return
     */
    CsiAggregation ensurePresenceAndCalculation(DateTime startDate, CsiAggregationInterval interval, String tag, CsiAggregationCachingContainer cachingContainer, List<CsiAggregationUpdateEvent> updateEvents) {
        CsiAggregation toCreateAndOrCalculate
        performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "ensurePresence", IndentationDepth.THREE) {
            toCreateAndOrCalculate = ensurePresence(startDate, interval, tag)
        }
        if (toCreateAndOrCalculate.hasToBeCalculatedAccordingEvents(updateEvents)) {
            performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "calculateCustomerSatisfactionCsiAggregation (interval=${interval.intervalInMinutes}; aggregator=page)", IndentationDepth.THREE) {
                toCreateAndOrCalculate = calcMv(toCreateAndOrCalculate, cachingContainer)
            }
        }
        return toCreateAndOrCalculate
    }

    private CsiAggregation ensurePresence(DateTime startDate, CsiAggregationInterval interval, String tag) {
        CsiAggregation toCreateAndOrCalculate
        AggregatorType pageAggregator = AggregatorType.findByName(AggregatorType.PAGE)
        performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "ensurePresence.findByStarted", IndentationDepth.FOUR) {
            toCreateAndOrCalculate = CsiAggregation.findByStartedAndIntervalAndAggregatorAndTag(startDate.toDate(), interval, pageAggregator, tag)
            log.debug("CsiAggregation.findByStartedAndIntervalAndAggregatorAndTag delivered ${toCreateAndOrCalculate ? 'a' : 'no'} result")
        }
        if (!toCreateAndOrCalculate) {
            toCreateAndOrCalculate = new CsiAggregation(
                    started: startDate.toDate(),
                    interval: interval,
                    aggregator: pageAggregator,
                    tag: tag,
                    csByWptDocCompleteInPercent: null,
                    underlyingEventResultsByWptDocComplete: ''
            ).save(failOnError: true)
        }
        return toCreateAndOrCalculate
    }
    /**
     * Calculates the given {@link CsiAggregation} toBeCalculated.
     * @return The calculated {@link CsiAggregation}.
     */
    CsiAggregation calcMv(CsiAggregation toBeCalculated, CsiAggregationCachingContainer cachingContainer) {
        JobGroup targetCsiGroup
        Page targetPage
        performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "  calcMvForPageAggregator - getJobGroupAndPageFromCachingContainer", IndentationDepth.FOUR) {
            targetCsiGroup = cachingContainer.csiGroupToCalcCsiAggregationFor
            targetPage = cachingContainer.pageToCalcCsiAggregationFor
        }
        if (toBeCalculated == null || !targetCsiGroup || !targetPage) {
            log.error("CsiAggregation can't be calculated: ${toBeCalculated}. targetCsiGroup=${targetCsiGroup}, targetPage=${targetPage}")
            return toBeCalculated
        }
        List<CsiAggregation> hmvsOfTargetCsiGroupAndPage = cachingContainer.hCsiAggregationsByCsiGroupPageCombination["${targetCsiGroup.ident() + UNIQUE_STRING_DELIMITTER + targetPage.ident()}"]
        log.debug("Calculating Page-CsiAggregation: Calculation-database are ${hmvsOfTargetCsiGroupAndPage.size()} hourly Event-CsiAggregations.")

        List<WeightedCsiValue> weightedCsiValuesByDocComplete = []
        List<WeightedCsiValue> weightedCsiValuesByVisuallyComplete = []
        if (hmvsOfTargetCsiGroupAndPage.size() > 0) {
            CsiConfiguration csiConfiguration = targetCsiGroup.csiConfiguration
            weightedCsiValuesByDocComplete = weightingService.getWeightedCsiValues(hmvsOfTargetCsiGroupAndPage, [WeightFactor.HOUROFDAY, WeightFactor.BROWSER_CONNECTIVITY_COMBINATION] as Set, csiConfiguration)
            weightedCsiValuesByVisuallyComplete = weightingService.getWeightedCsiValuesByVisuallyComplete(hmvsOfTargetCsiGroupAndPage, [WeightFactor.HOUROFDAY, WeightFactor.BROWSER_CONNECTIVITY_COMBINATION] as Set, csiConfiguration)
            log.debug("weightedCsiValuesByDocComplete.size()=${weightedCsiValuesByDocComplete.size()}")
            log.debug("weightedCsiValuesByVisuallyComplete.size()=${weightedCsiValuesByVisuallyComplete.size()}")
        }

        performanceLoggingService.logExecutionTime(LogLevel.DEBUG, "  calcMvForPageAggregator - calculation wmv: calc weighted mean", IndentationDepth.FOUR) {
            if (weightedCsiValuesByDocComplete.size() > 0) {
                toBeCalculated.csByWptDocCompleteInPercent = meanCalcService.calculateWeightedMean(weightedCsiValuesByDocComplete*.weightedValue)
            }
            if(weightedCsiValuesByVisuallyComplete.size() > 0) {
                toBeCalculated.csByWptVisuallyCompleteInPercent = meanCalcService.calculateWeightedMean(weightedCsiValuesByVisuallyComplete*.weightedValue)
            }
            csiAggregationUpdateEventDaoService.createUpdateEvent(toBeCalculated.ident(), CsiAggregationUpdateEvent.UpdateCause.CALCULATED)
        }
        return toBeCalculated
    }
}
