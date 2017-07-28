//= require /bower_components/d3/d3.min.js
//= require /chartComponents/chartBars.js
//= require /chartComponents/chartBarScore.js
//= require /d3/chartColorProvider.js
//= require /chartComponents/chartLegend.js
//= require /chartComponents/chartSideLabels.js
//= require_self

"use strict";

var OpenSpeedMonitor = OpenSpeedMonitor || {};
OpenSpeedMonitor.ChartModules = OpenSpeedMonitor.ChartModules || {};

OpenSpeedMonitor.ChartModules.PageAggregation = (function (selector) {
    var svg = d3.select(selector);
    var chartBarsComponents = {};
    var chartLegendComponent = OpenSpeedMonitor.ChartComponents.ChartLegend();
    var chartBarScoreComponent = OpenSpeedMonitor.ChartComponents.ChartBarScore();
    var chartSideLabelsComponent = OpenSpeedMonitor.ChartComponents.ChartSideLabels();
    var componentMargin = 15;
    var transitionDuration = 500;
    var chartSideLabelsWidth = 200;
    var chartBarsWidth = 700;
    var fullWidth = chartSideLabelsWidth + chartBarsWidth;
    var chartBarsHeight = 400;
    var measurandDataEntries = {};
    var measurandGroupDataMap = {};
    var sideLabelData = [];

    var setData = function (data) {
        measurandDataEntries = (data && data.series) ? extractMeasurandData(data.series) : measurandDataEntries;
        measurandGroupDataMap = (data && data.series) ? extractMeasurandGroupData(data.series) : measurandGroupDataMap;
        sideLabelData = (data && data.series) ? data.series.map(function (s) { return s.page;}) : sideLabelData;

        var fullWidth = (data && data.width) ? data.width : fullWidth;
        fullWidth = fullWidth < 0 ? svg.node().getBoundingClientRect().width : fullWidth;
        chartSideLabelsWidth = chartSideLabelsComponent.estimateWidth(svg, sideLabelData);
        chartBarsWidth = fullWidth - componentMargin - chartSideLabelsWidth;
        chartBarsHeight = calculateChartBarsHeight(measurandDataEntries[0].values.series.length);

        // setDataForHeader(data);
        setDataForLegend(measurandDataEntries);
        setDataForBarScore();
        setDataForSideLabels();
        setDataForBars();
    };

    var setDataForBarScore = function () {
        chartBarScoreComponent.setData({
            width: chartBarsWidth,
            max: measurandGroupDataMap["LOAD_TIMES"] ? measurandGroupDataMap["LOAD_TIMES"].max : 0
        });
    };

    var setDataForLegend = function () {
        chartLegendComponent.setData({
            entries: measurandDataEntries.map(function (measurandNestEntry) {
                var measurandValue = measurandNestEntry.values;
                return {
                    id: measurandValue.id,
                    color: measurandValue.color,
                    label: measurandValue.label
                };
            }),
            width: chartBarsWidth
        });
    };

    var setDataForSideLabels = function () {
      chartSideLabelsComponent.setData({
         height: chartBarsHeight,
         labels: sideLabelData
      });
    };

    var setDataForBars = function () {
        var componentsToRender = {};
        measurandDataEntries.forEach(function (measurandNestEntry) {
            componentsToRender[measurandNestEntry.key] = chartBarsComponents[measurandNestEntry.key] || OpenSpeedMonitor.ChartComponents.ChartBars();
            componentsToRender[measurandNestEntry.key].setData({
                values: measurandNestEntry.values.series,
                color: measurandNestEntry.values.color,
                min: measurandGroupDataMap[measurandNestEntry.values.measurandGroup].min > 0 ? 0 : measurandGroupDataMap[measurandNestEntry.values.measurandGroup].min,
                max: measurandGroupDataMap[measurandNestEntry.values.measurandGroup].max,
                height: chartBarsHeight,
                width: chartBarsWidth
            });
        });
        chartBarsComponents = componentsToRender;
    };

    var extractMeasurandData = function (series) {
        var colorProvider = OpenSpeedMonitor.ChartColorProvider();
        var colorScales = {};
        return d3.nest()
            .key(function(d) { return d.measurand; })
            .rollup(function (seriesOfMeasurand) {
                var firstValue = seriesOfMeasurand[0];
                var unit = firstValue.unit;
                colorScales[unit] = colorScales[unit] || colorProvider.getColorscaleForMeasurandGroup(unit);
                return {
                    id: firstValue.measurand,
                    label: firstValue.measurandLabel,
                    measurandGroup: firstValue.measurandGroup,
                    color: colorScales[unit](firstValue.measurand),
                    series: seriesOfMeasurand
                };
            })
            .entries(series);
    };

    var extractMeasurandGroupData = function (series) {
        return d3.nest()
            .key(function(d) { return d.measurandGroup; })
            .rollup(function (seriesOfMeasurandGroup) {
                var extent = d3.extent(seriesOfMeasurandGroup, function(entry) { return entry.value; });
                return {
                    min: extent[0],
                    max: extent[1]
                };
            }).map(series);
    };

    var calculateChartBarsHeight = function (numberOfMeasurands) {
        var barBand = OpenSpeedMonitor.ChartComponents.ChartBars.BarBand;
        var barGap = OpenSpeedMonitor.ChartComponents.ChartBars.BarGap;
        return numberOfMeasurands * (barBand + barGap) - barGap;
    };

    var render = function () {
        var shouldShowScore = !!measurandGroupDataMap["LOAD_TIMES"];
        var barScorePosY = chartBarsHeight + componentMargin;
        var barScoreHeight = shouldShowScore ? OpenSpeedMonitor.ChartComponents.ChartBarScore.BarHeight + componentMargin : 0;
        var legendPosY = barScorePosY + barScoreHeight;
        var legendHeight = chartLegendComponent.estimateHeight(svg) + componentMargin;
        var chartHeight = legendPosY + legendHeight;

        svg
            .transition()
            .duration(transitionDuration)
            .style("height", chartHeight);
        renderSideLabels(svg);

        var contentGroup = svg.selectAll(".bars-content-group").data([1]);
        contentGroup.enter()
            .append("g")
            .classed("bars-content-group", true);
        contentGroup
            .transition()
            .duration(transitionDuration)
            .attr("transform", "translate(" + (chartSideLabelsWidth + componentMargin) +", 0)");
        renderBars(contentGroup);
        renderBarScore(contentGroup, shouldShowScore, barScorePosY);
        renderLegend(contentGroup, legendPosY);
    };

    var renderSideLabels = function(svg) {
        var sideLabels = svg.selectAll(".side-labels-group").data([chartSideLabelsComponent]);
        sideLabels.exit()
            .remove();
        sideLabels.enter()
            .append("g")
            .classed("side-labels-group", true);
        sideLabels.call(chartSideLabelsComponent.render)
    };

    var renderBarScore = function (svg, shouldShowScore, posY) {
        var barScore = svg.selectAll(".chart-score-group").data([chartBarScoreComponent]);
        barScore.exit()
            .remove();
        barScore.enter()
            .append("g")
            .attr("class", "chart-score-group")
            .attr("transform", "translate(0, " + posY + ")");
        barScore
            .call(chartBarScoreComponent.render)
            .transition()
            .style("opacity", shouldShowScore ? 1 : 0)
            .duration(transitionDuration)
            .attr("transform", "translate(0, " + posY + ")");
    };

    var renderLegend = function (svg, posY) {
        var legend = svg.selectAll(".chart-legend-group").data([chartLegendComponent]);
        legend.exit()
            .remove();
        legend.enter()
            .append("g")
            .attr("class", "chart-legend-group")
            .attr("transform", "translate(0, " + posY + ")");
        legend.call(chartLegendComponent.render)
            .transition()
            .duration(transitionDuration)
            .attr("transform", "translate(0, " + posY + ")");
    };

    var renderBars = function (svg) {
        var chartBarsGroup = svg.selectAll(".chart-bar-group").data([1]);
        chartBarsGroup.enter()
            .append("g")
            .attr("class", "chart-bar-group");

        var chartBars = chartBarsGroup.selectAll(".chart-bars").data(Object.values(chartBarsComponents));
        chartBars.exit()
            .remove();
        chartBars.enter()
            .append("g")
            .attr("class", "chart-bars");
        chartBars.each(function(chartBarsComponent) {
           chartBarsComponent.render(this);
        });

    };

    var autoWidth = function () {
        setData({width: -1});
    };

    return {
        render: render,
        setData: setData,
        autoWidth: autoWidth
    };

});
