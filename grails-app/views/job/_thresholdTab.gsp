<%@ page import="de.iteratec.osm.result.Threshold" %>
<%@ page import="de.iteratec.osm.measurement.schedule.JobGroup" %>
<%@ page import="de.iteratec.osm.result.Measurand" %>

<div id="threshold" :jobId="${job?.id}" :scriptId="${job?.script?.id}">
    <div>
        <div id="thresholdList">
            <div>
                <div v-for="thresholdItem in thresholds">

                    <threshold-measured-event :thresholds="thresholdItem"></threshold-measured-event>

                    <div v-for="threshold in thresholdItem.thresholdList">
                        <threshold-measurand :measurand="threshold.threshold.measurand"></threshold-measurand>
                        <threshold-row :threshold="threshold"></threshold-row>
                    </div>
                    <br>
                </div>
            </div>
        </div>
    </div>

    <br> <br>

    <div>
        <button type="button" class="btn btn-primary"
                @click="changeNewThresholdState()">
            <span v-if="newThresholdState">Hide new Threshold</span>
            <span v-else>New Threshold</span>
        </button>
        <button class="btn btn-default ciButton" type="button" id="copyToClipboard">
            <g:message code="job.threshold.copyToClipboard" default="Copy To Clipboard"/>
        </button><br>

        <span v-if="newThresholdState">
            <div>
                <select id="measuredEvent" name="measuredEvent" class="form-control thresholdSelects"
                        v-model="newThreshold.measuredEvent">
                    <option v-for="measuredEvent in measuredEvents"
                            :value="measuredEvent">{{measuredEvent.name}}</option>
                </select>
            </div>
            <select class="thresholdMeasurand form-control thresholdSelects" id="measurand" name="measurand"
                    v-model="newThreshold.measurand">
                <option v-for="measurand in measurands" :value="measurand">{{measurand.name}}</option>
            </select>

            <div class="thresholdBoundaries form-inline thresholdSelects">
                <label class="labelGood">
                    Good
                </label>
                <label>
                    <
                </label>
                <input id="lowerBoundary" class="form-control" type="number" min="1"
                       name="lowerBoundary" cols="30"
                       rows="5"
                       maxlength="150"
                       v-model="newThreshold.lowerBoundary"/>
                <label>
                    ms <
                </label>
                <label class="labelOk">
                    OK
                </label>
                <label>
                    <
                </label>
                <input id="upperBoundary" class="form-control" type="number" min="1"
                       name="upperBoundary" cols="40"
                       rows="5"
                       maxlength="150"
                       v-model="newThreshold.upperBoundary"/>
                <label>
                    ms <
                </label>
                <label class="labelBad">
                    Bad
                </label>
                <button type="button" class="margins btn btn-default"
                        @click="addThreshold('${job}', '${g.createLink([controller: 'threshold', action: 'createAsync'])}')">
                    <i class="fa text-success fa-lg fa-plus"></i>
                </button>
            </div>
        </span>
    </div>
</div>
<g:render template="thresholdTabThresholdRowVue"/>
<g:render template="thresholdTabMeasurandVue"/>
<g:render template="thresholdTabMeasuredEventVue"/>


<asset:javascript src="job/thresholdForJob.js"/>

%{--

<asset:script type="text/javascript">
    OpenSpeedMonitor.postLoader.loadJavascript('<g:assetPath src="job/thresholdForJob.js"/>');
    $(window).load(function() {
      OpenSpeedMonitor.thresholdforJobs.initVue({jobId: "${job?.id}", scriptId: "${job?.script?.id}"});
    });
</asset:script>--}%
