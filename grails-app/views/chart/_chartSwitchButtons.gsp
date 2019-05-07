<g:set var="chartSwitchLinks" value="${[
        ['name': 'timeSeries', 'text': message(code: 'eventResultDashboard.label', default: 'Time Series'), 'icon': 'fa-chart-line'],
        ['name': 'aggregation', 'text': message(code: 'de.iteratec.isocsi.aggregationChart', default: 'Aggregation'), 'icon': 'fa-chart-bar'],
        ['name': 'distribution', 'text': message(code: 'de.iteratec.osm.distributionChart', default: 'Distribution Chart'), 'icon': 'fa-chart-area'],
        ['name': 'pageComparison', 'text': message(code: 'de.iteratec.isocsi.pageComparision.title', default: 'Page Comparison'), 'icon': 'fa-balance-scale'],
        ['name': 'detailAnalysis', 'text': message(code: 'de.iteratec.isocsi.detailAnalysis', default: 'Detail Analysis'), 'icon': 'fa-chart-pie'],
        ['name': 'resultList', 'text': message(code: 'de.iteratec.result.title', default: 'Result List'), 'icon': 'fa-th-list']

]}"/>

<h1>
<g:each in="${chartSwitchLinks}">
    <g:if test="${currentChartName == it.name}">
        ${it.text}
    </g:if>
    <g:else>
        %{--show detailAnalysis link only if detailAnalysis is enabled--}%
        <g:if test="${it.name != 'detailAnalysis' || grailsApplication.config.getProperty('grails.de.iteratec.osm.detailAnalysis.enablePersistenceOfDetailAnalysisData')?.equals("true")}">
            <a href="#" class="btn hidden" id="${it.name}WithDataLink"><i class="fas ${it.icon}"></i></a>
        </g:if>
    </g:else>
</g:each>
</h1>
