<%@ page defaultCodec="none" %></page>
<%@ page contentType="text/html;charset=UTF-8" %>
<% def springSecurityService %>
<%@ page import="de.iteratec.osm.report.UserspecificEventResultDashboard" %>
<g:set var="userspecificDashboardService" bean="userspecificDashboardService"/>
<html>
<head>
    <meta name="layout" content="kickstart_osm"/>
    <title><g:message code="de.iteratec.isocsi.eventResultDashboard"/></title>

    <asset:stylesheet src="rickshaw/rickshaw_custom.css"/>

</head>

<body>

<%-- main menu --%>
<g:render template="/layouts/mainMenu"/>

<%-- userspecific dashboards --%>
<div class="row">
    <div class="span12">
        <div class="btn-toolbar" style="margin: 0;">
            <div class="btn-group">
                <a class="btn btn-primary btn-small dropdown-toggle" data-toggle="dropdown" href="#">
                    <g:message code="de.iteratec.isocsi.dashBoardControllers.custom.select.label"
                               default="Dashboard-Ansicht ausw&auml;hlen"/>
                    <span class="caret"></span>
                </a>
                <ul class="dropdown-menu" id="customDashBoardSelection">
                    <g:set var="availableDashboards"
                           value="${userspecificDashboardService.getListOfAvailableEventResultDashboards()}"/>
                    <g:if test="${availableDashboards.size() > 0}">
                        <g:each in="${availableDashboards}" var="availableDashboard">
                            <li><g:link action="showAll"
                                        params="[dashboardID: availableDashboard.dashboardID]">${availableDashboard.dashboardName}</g:link></li>
                        </g:each>
                    </g:if>
                    <g:else>
                        <g:set var="anchorDashboardCreation" value="#"/>
                        <sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_SUPER_ADMIN">
                            <g:set var="anchorDashboardCreation" value="#bottomCommitButtons"/>
                        </sec:ifAnyGranted>
                        <li><a href="${anchorDashboardCreation}"><g:message
                                code="de.iteratec.isocsi.dashBoardControllers.custom.select.error.noneAvailable"
                                default="Es sind keine verf&uuml;gbar - bitte legen Sie eine an!"/></a></li>
                    </g:else>
                </ul>
            </div>
        </div>
    </div>
</div>
<br/>

<div class="row">
    <div class="span12">
        <g:if test="${command}">
            <g:hasErrors bean="${command}">
                <div class="alert alert-error">
                    <strong><g:message code="de.iteratec.isocsi.CsiDashboardController.selectionErrors.title"/></strong>
                    <ul>
                        <g:eachError var="eachError" bean="${command}">
                            <li><g:message error="${eachError}"/></li>
                        </g:eachError>
                    </ul>
                </div>
            </g:hasErrors>
        </g:if>
    </div>
</div>

<div class="row">
    <div class="span12">
        <form method="get" action="" id="dashBoardParamsForm">
            <div class="alert alert-success renderInvisible" id="saveDashboardSuccessDiv"><g:message
                    code="de.iteratec.ism.ui.labels.save.success"
                    default="Successfully saved these settings as custom dashboard."/></div>

            <div class="alert alert-error renderInvisible" id="saveDashboardErrorDiv"></div>
            <g:if test="${warnAboutLongProcessingTime}">
                <div class="alert">
                    <strong><g:message
                            code="de.iteratec.isocsi.CsiDashboardController.warnAboutLongProcessingTime.title"/></strong>

                    <p>
                        <g:message
                                code="de.iteratec.isocsi.CsiDashboardController.warnAboutLongProcessingTime.message"/>
                    </p>

                    <p>
                        <g:checkBox name="overwriteWarningAboutLongProcessingTime" value="${true}" checked="${true}"
                                    style="display:none;"/>
                        <g:actionSubmit id="override-long-processing-time"
                                        value="${g.message(code: 'de.iteratec.isocsi.CsiDashboardController.warnAboutLongProcessingTime.checkbox.label', 'default': 'Go on')}"
                                        action="showAll" class="btn btn-warning"/>
                    </p>
                </div>
            </g:if>


            <div class="accordion">
                <div class="accordion-group">
                    <div class="accordion-heading accordion-custom-heading">
                        <div class="row">
                            <div class="span12">
                                <div class="row">
                                    <div class="span3">
                                        <a class="accordion-toggle accordion-link fa fa-chevron-up"
                                           data-toggle="collapse" data-parent="#accordion2" href="#collapseOne">
                                            <g:message code="de.iteratec.sri.wptrd.time.filter.heading"
                                                       default="Zeitraum ausw&auml;hlen"/>
                                        </a>
                                    </div>

                                    <div class="span2 accordion-info text-right">
                                        <g:message code="de.iteratec.isocsi.csi.aggreator.heading"
                                                   default="Aggregation"/>:<br>
                                        <g:message code="de.iteratec.isocsi.csi.timeframe.heading"
                                                   default="Timeframe"/>:
                                    </div>

                                    <div class="span7 accordion-info" id="accordion-info-date"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <g:if test="${request.queryString}"><div id="collapseOne" class="accordion-body collapse"></g:if>
                    <g:else><div id="collapseOne" class="accordion-body collapse in"></g:else>
                    <div class="accordion-inner" id="accordion-inner-date">

                        <div class="row">
                            <div class="span5">
                                <legend><g:message code="de.iteratec.isocsi.csi.aggreator.heading"
                                                   default="Aggregation"/></legend>

                                <div class="row">
                                    <div class="span1">
                                        <g:message
                                                code="de.iteratec.isr.wptrd.labels.timeframes.interval"
                                                default="Interval"/>:
                                    </div>

                                    <div class="span2">
                                        <g:select id="selectedIntervalHtmlId" class="input-medium"
                                                  name="selectedInterval" from="${csiAggregationIntervals}"
                                                  valueMessagePrefix="de.iteratec.isr.wptrd.intervals"
                                                  value="${selectedInterval}"/>
                                    </div>
                                </div>
                            </div>

                            <div class="span5">
                                <g:render template="/dateSelection/startAndEnddateSelection"
                                          model="${['selectedTimeFrameInterval': selectedTimeFrameInterval, 'from': from, 'fromHour': fromHour, 'to': to, 'toHour': toHour]}"/>
                            </div>
                        </div>
                    </div>
                </div>
                </div>
                    <div class="accordion-group">
                        <div class="accordion-heading accordion-custom-heading">
                            <div class="row">
                                <div class="span12">
                                    <div class="row">
                                        <div class="span3">
                                            <a class="accordion-toggle accordion-link fa fa-chevron-up"
                                               data-toggle="collapse" data-parent="#accordion2" href="#collapseTwo">
                                                <g:message code="de.iteratec.sri.wptrd.jobs.filter.heading"
                                                           default="Jobs filtern"/>
                                            </a>
                                        </div>

                                        <div class="span2 accordion-info text-right">
                                            <g:message code="de.iteratec.isr.wptrd.labels.filterFolder"
                                                       default="Job Group"/>:<br>
                                            <g:message code="de.iteratec.osm.result.page.label"
                                                       default="Page"/>&nbsp;|&nbsp;<g:message
                                                    code="de.iteratec.osm.result.measured-event.label"
                                                    default="Measured step"/>:<br>
                                            <g:message code="browser.label" default="Browser"/>&nbsp;|&nbsp;<g:message
                                                    code="job.location.label" default="Location"/>:<br>
                                            <g:message code="de.iteratec.osm.result.connectivity.label"
                                                       default="Connectivity"/>:<br>
                                        </div>

                                        <div class="span7 accordion-info" id="accordion-info-jobs"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <g:if test="${request.queryString}"><div id="collapseTwo" class="accordion-body collapse"></g:if>
                        <g:else><div id="collapseTwo" class="accordion-body collapse in"></g:else>
                        <div class="accordion-inner" style="margin: 0px; padding: 4px;">
                            <g:render template="selectMeasurings"
                                      model="${['locationsOfBrowsers'             : locationsOfBrowsers,
                                                'eventsOfPages'                   : eventsOfPages,
                                                'folders'                         : folders,
                                                'selectedFolder'                  : selectedFolder,
                                                'pages'                           : pages,
                                                'selectedPage'                    : selectedPage,
                                                'measuredEvents'                  : measuredEvents,
                                                'selectedAllMeasuredEvents'       : selectedAllMeasuredEvents,
                                                'selectedMeasuredEvents'          : selectedMeasuredEvents,
                                                'browsers'                        : browsers,
                                                'selectedBrowsers'                : selectedBrowsers,
                                                'selectedAllBrowsers'             : selectedAllBrowsers,
                                                'locations'                       : locations,
                                                'selectedLocations'               : selectedLocations,
                                                'selectedAllLocations'            : selectedAllLocations,
                                                'connectivityProfiles'            : connectivityProfiles,
                                                'selectedConnectivityProfiles'    : selectedConnectivityProfiles,
                                                'selectedAllConnectivityProfiles' : selectedAllConnectivityProfiles,
                                                'showExtendedConnectivitySettings': true]}"/>
                        </div>
                    </div>
                    </div>
                        <div class="accordion-group">
                            <div class="accordion-heading accordion-custom-heading">
                                <div class="row">
                                    <div class="span12">
                                        <div class="row">
                                            <div class="span3">
                                                <a class="accordion-toggle accordion-link fa fa-chevron-up"
                                                   data-toggle="collapse" data-parent="#accordion2"
                                                   href="#collapseThree">
                                                    <g:message code="de.iteratec.sri.wptrd.measurement.filter.heading"
                                                               default="Messwerte auw&auml;hlen"/>
                                                </a>
                                            </div>

                                            <div class="span2 accordion-info text-right">
                                                <g:message code="job.firstView.label" default="First View"/>:<br>
                                                <g:message code="job.repeatedView.label" default="Repeated View"/>:<br>
                                            </div>

                                            <div class="span7 accordion-info" id="accordion-info-measurements"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <g:if test="${request.queryString}"><div id="collapseThree" class="accordion-body collapse"></g:if>
                            <g:else><div id="collapseThree" class="accordion-body collapse in"></g:else>
                            <div class="accordion-inner">
                                <div class="span4">
                                    <label for="selectAggregatorUncachedHtmlId"><g:message
                                            code="de.iteratec.isr.wptrd.labels.filterFirstView"
                                            default="First View:"/></label>
                                    <g:if test="${selectedAggrGroupValuesUnCached.size() == 0}"><g:set
                                            var="selectedAggrGroupValuesUnCached"
                                            value="${['docCompleteTimeInMillisecsUncached']}"/></g:if>
                                    <iteratec:optGroupedSelect dataMap="${aggrGroupValuesUnCached}"
                                                               id="selectAggregatorUncachedHtmlId"
                                                               class="iteratec-element-select-higher"
                                                               name="selectedAggrGroupValuesUnCached"
                                                               optionKey="value" optionValue="value"
                                                               multiple="true"
                                                               value="${selectedAggrGroupValuesUnCached}"/>
                                </div>

                                <div class="span4">
                                    <label for="selectAggregatorCachedHtmlId"><g:message
                                            code="de.iteratec.isr.wptrd.labels.filterRepeateView"
                                            default="Repeated View:"/></label>
                                    <iteratec:optGroupedSelect id="selectAggregatorCachedHtmlId"
                                                               dataMap="${aggrGroupValuesCached}"
                                                               multiple="true" id="selectAggregatorCachedHtmlId"
                                                               class="iteratec-element-select-higher"
                                                               name="selectedAggrGroupValuesCached" optionKey="value"
                                                               optionValue="value"
                                                               value="${selectedAggrGroupValuesCached}"/>
                                </div>

                                <div class="span3">
                                    <h6><g:message code="de.iteratec.isr.measurand.group.LOAD_TIMES"
                                                   default="Ladezeiten [ms]"/></h6>

                                    <div class="input-append">
                                        <label for="appendedInputBelowLoadTimes">
                                            <g:message code="de.iteratec.isr.wptrd.labels.trimbelow"
                                                       default="Trim below"/>
                                        </label>
                                        <input name="trimBelowLoadTimes" id="appendedInputBelowLoadTimes"
                                               value="${trimBelowLoadTimes}" class="span1 content-box" type="text"
                                               placeholder="...">
                                        <span class="add-on">ms</span>
                                    </div>

                                    <div class="input-append">
                                        <label for="appendedInputAboveLoadTimes">
                                            <g:message code="de.iteratec.isr.wptrd.labels.trimabove"
                                                       default="Trim above"/>
                                        </label>
                                        <input name="trimAboveLoadTimes" id="appendedInputAboveLoadTimes"
                                               value="${trimAboveLoadTimes}" class="span1 content-box" type="text"
                                               placeholder="...">
                                        <span class="add-on">ms</span>
                                    </div>
                                    <h6><g:message code="de.iteratec.isr.measurand.group.REQUEST_COUNTS"
                                                   default="Anzahl Requests [c]"/></h6>

                                    <div class="input-append">
                                        <label for="appendedInputBelowRequestCounts">
                                            <g:message code="de.iteratec.isr.wptrd.labels.trimbelow"
                                                       default="Trim below"/>
                                        </label>
                                        <input name="trimBelowRequestCounts" id="appendedInputBelowRequestCounts"
                                               value="${trimBelowRequestCounts}" class="span1 content-box" type="text"
                                               placeholder="...">
                                        <span class="add-on">REQ</span>
                                    </div>

                                    <div class="input-append">
                                        <label for="appendedInputAboveRequestCounts">
                                            <g:message code="de.iteratec.isr.wptrd.labels.trimabove"
                                                       default="Trim above"/>
                                        </label>
                                        <input name="trimAboveRequestCounts" id="appendedInputAboveRequestCounts"
                                               value="${trimAboveRequestCounts}" class="span1 content-box" type="text"
                                               placeholder="...">
                                        <span class="add-on">REQ</span>
                                    </div>
                                    <h6><g:message code="de.iteratec.isr.measurand.group.REQUEST_SIZES"
                                                   default="Gr&ouml;&szlig;e Requests [kb]"/></h6>

                                    <div class="input-append">
                                        <label for="appendedInputBelowRequestSizes">
                                            <g:message code="de.iteratec.isr.wptrd.labels.trimbelow"
                                                       default="Trim below"/>
                                        </label>
                                        <input name="trimBelowRequestSizes" id="appendedInputBelowRequestSizes"
                                               value="${trimBelowRequestSizes}" class="span1 content-box" type="text"
                                               placeholder="...">
                                        <span class="add-on">KB</span>
                                    </div>

                                    <div class="input-append">
                                        <label for="appendedInputAboveRequestSizes">
                                            <g:message code="de.iteratec.isr.wptrd.labels.trimabove"
                                                       default="Trim above"/>
                                        </label>
                                        <input name="trimAboveRequestSizes" id="appendedInputAboveRequestSizes"
                                               value="${trimAboveRequestSizes}" class="span1 content-box" type="text"
                                               placeholder="...">
                                        <span class="add-on">KB</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        </div>
                            <div class="row">
                                <div class="span12" id="bottomCommitButtons">
                                    <g:actionSubmit
                                            value="${g.message(code: 'de.iteratec.ism.ui.labels.show.graph', 'default': 'Show')}"
                                            action="showAll"
                                            id="graphButtonHtmlId" class="btn btn-primary"
                                            style="margin-top: 16px;"/>
                                    <g:actionSubmit
                                            value="${g.message(code: 'de.iteratec.ism.ui.labels.download.csv', 'default': 'As CSV')}"
                                            action="downloadCsv"
                                            class="btn btn-primary" style="margin-top: 16px;"/>
                                    <sec:ifLoggedIn>
                                        <sec:ifAnyGranted roles="ROLE_ADMIN, ROLE_SUPER_ADMIN">
                                            <a href="#CreateUserspecifiedDashboardModal" role="button"
                                               class="btn btn-primary"
                                               style="margin-top: 16px;" data-toggle="modal">
                                                ${message(code: 'de.iteratec.ism.ui.labels.save.custom.dashboard', default: 'Save these settings as custom dashboard')}
                                            </a>
                                        </sec:ifAnyGranted>
                                    </sec:ifLoggedIn>
                                    <g:if test="${params.dashboardID}">
                                        <g:if test="${userspecificDashboardService.isCurrentUserDashboardOwner(params.dashboardID)}">
                                            <a href="#" role="button" class="btn btn-primary"
                                               style="margin-top: 16px;"
                                               onclick="updateCustomDashboard('${dashboardName}', '${publiclyVisible}')">${message(code: 'de.iteratec.ism.ui.labels.update.custom.dashboard', default: 'Update custom dashboard')}</a>
                                            <g:render template="/_common/modals/deleteCustomDashboard"/>

                                        </g:if>
                                    </g:if>
                                </div>

                                <div class="span3" style="display: none;">
                                    <%-- Not used as the point chatType isn't requested.
                                         To reactivate remove display: none; from outer div
                                         and set selectedChartType from="${[0,1]}"
                                     --%>
                                    <g:message
                                            code="de.iteratec.isr.wptrd.labels.timeframes.charttype"
                                            default="Timeframe:" style="margin-top: 16px"/>
                                    <g:select id="charttypeSelect" class="input-medium"
                                              name="selectedChartType" from="${[0]}"
                                              valueMessagePrefix="de.iteratec.isr.wptrd.charttypes"
                                              value="${selectedChartType}"
                                              style="margin-top: 16px;margin-left:10px;"/>
                                </div>
                            </div>
                        </div>
        </form>
    </div>
    <g:if test="${request.queryString && command && !command.hasErrors() && !eventResultValues}">
        <div class="span12">
            <div class="alert alert-danger">
                <strong><g:message code="de.iteratec.ism.no.data.on.current.selection.heading"/></strong>
                <g:message code="de.iteratec.ism.no.data.on.current.selection"/>
            </div>
        </div>
    </g:if>

    <g:if test="${warnAboutExceededPointsPerGraphLimit}">
        <div class="span12">
            <div class="alert alert-danger">
                <strong><g:message
                        code="de.iteratec.isr.EventResultDashboardController.warnAboutExceededPointsPerGraphLimit.title"/></strong>

                <p>
                    <g:message
                            code="de.iteratec.isr.EventResultDashboardController.warnAboutExceededPointsPerGraphLimit"/>
                </p>
            </div>
        </div>
    </g:if>

    <g:if test="${eventResultValues}">
        <a name="chart-table"></a>

        <div id="chartbox">
            <div class="span12 well">
                <g:render template="/highchart/chart"
                          model="[
                                  chartData                    : eventResultValues,
                                  chartTitle                   : chartTitle,
                                  yAxisLabel                   : g.message(code: 'de.iteratec.isocsi.CsiDashboardController.chart.yType.label'),
                                  initialChartWidth            : chartWidth,
                                  initialChartHeight           : chartHeight,
                                  chartUnit                    : '%',
                                  globalLineWidth              : '2',
                                  xAxisMin                     : fromTimestampForHighChart,
                                  xAxisMax                     : toTimestampForHighChart,
                                  markerEnabled                : markerShouldBeEnabled,
                                  dataLabelsActivated          : labelShouldBeEnabled,
                                  yAxisScalable                : 'false',
                                  optimizeForExport            : 'false',
                                  openDataPointLinksInNewWindow: openDataPointLinksInNewWindow,
                                  annotations                  : annotations]"/>
            </div>
        </div>
    </g:if>
    <g:else>
        <g:if test="${request.queryString}">
            <g:if test="${!warnAboutLongProcessingTime}">
                <div class="span12">
                    <strong><g:message
                            code="de.iteratec.isocsi.CsiDashboardController.no.data.on.current.selection"/></strong>
                </div>
            </g:if>
        </g:if>
    </g:else>
</div>
<g:render template="/_common/modals/createUserspecifiedDashboard" model="[item: item]"/>

<content tag="include.bottom">
    <asset:javascript src="eventresultdashboard/eventResultDashboard.js"/>
    <asset:javascript src="iteratecChartRickshaw.js"/>
    <asset:script type="text/javascript">

        var pagesToEvents = [];
        <g:each var="page" in="${pages}">
        <g:if test="${eventsOfPages[page.id] != null}">
        pagesToEvents[${page.id}] = [<g:each var="event" in="${eventsOfPages[page.id]}">${event}, </g:each>];
        </g:if>
        </g:each>

        var browserToLocation = [];
        <g:each var="browser" in="${browsers}">
        <g:if test="${locationsOfBrowsers[browser.id] != null}">
        browserToLocation[${browser.id}] = [<g:each var="location"
                                                           in="${locationsOfBrowsers[browser.id]}">${location}, </g:each>];
        </g:if>
        </g:each>

        function setAdjustments() {
            var chartTitle = "${chartTitle}";
            var chartWidth = "${chartWidth}";
            var chartHeight = "${chartHeight}";
            var loadTimeMinimum = "${loadTimeMinimum}";
            var loadTimeMaximum = "${loadTimeMaximum}";
            var showDataMarkers = "${showDataMarkers}";
            var showDataLabels = "${showDataLabels}";
            var optimizeForWideScreen = "${showDataLabels}"
            var graphNameAliases =
            ${graphNameAliases}
            var graphColors = ${graphColors}
                    $("#dia-title").val(chartTitle);
            $("#dia-width").val(chartWidth);
            $("#dia-height").val(chartHeight);
            $("#dia-y-axis-max").val(loadTimeMaximum);
            $("#dia-y-axis-min").val(loadTimeMinimum);
            initGraphNameAliases(graphNameAliases);
            initGraphColors(graphColors);

            if (eval(showDataMarkers)) {
                $("#to-enable-marker").click();
            }
            if (eval(showDataLabels)) {
                $("#to-enable-label").click();
            }
        }

        $(document).ready(function () {

            initSelectMeasuringsControls(pagesToEvents, browserToLocation, allMeasuredEventElements, allBrowsers, allLocations);

            doOnDomReady(
                    '${dateFormat}',
                    ${weekStart},
                    '${g.message(code: 'web.gui.jquery.chosen.multiselect.noresultstext', 'default': 'Keine Eintr&auml;ge gefunden f&uuml;r ')}'
            );

            <g:if test="${dashboardName}">
                updateDateTimePicker(${from.getTime()}, ${to.getTime()}, '${fromHour}', '${toHour}', ${selectedTimeFrameInterval});
                updateSelectedInterval(${selectedInterval});
            </g:if>

            if (navigator.userAgent.indexOf('MSIE') !== -1 || navigator.appVersion.indexOf('Trident/') > 0 || navigator.appVersion.indexOf('Edge/') > 0) {
                $("#dia-save-chart-as-png").removeClass("btn-primary");
                $("#dia-save-chart-as-png").addClass("btn-primary.disabled");
                $("#dia-save-chart-as-png").attr("disabled", "disabled");
                $("#dia-save-chart-as-png").attr("title", "<g:message
            code="de.iteratec.ism.ui.button.save.disabled.tooltip"/>");
            }
            setAdjustments();
        });

    </asset:script>
</content>

</body>
</html>
