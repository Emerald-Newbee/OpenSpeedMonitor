package de.iteratec.osm.measurement.schedule

import de.iteratec.osm.csi.CsiConfiguration
import de.iteratec.osm.csi.transformation.DefaultTimeToCsMappingService
import de.iteratec.osm.csi.transformation.TimeToCsMappingService
import de.iteratec.osm.d3Data.BarChartData
import de.iteratec.osm.d3Data.ChartEntry
import de.iteratec.osm.d3Data.MatrixViewData
import de.iteratec.osm.d3Data.MatrixViewEntry
import de.iteratec.osm.d3Data.MultiLineChart
import de.iteratec.osm.d3Data.TreemapData
import de.iteratec.osm.measurement.environment.Browser
import de.iteratec.osm.report.external.GraphiteServer
import de.iteratec.osm.util.I18nService
import grails.converters.JSON
import org.springframework.dao.DataIntegrityViolationException
import static org.springframework.http.HttpStatus.*

//TODO: This controller was generated due to a scaffolding bug (https://github.com/grails3-plugins/scaffolding/issues/24). The dynamically scaffolded controllers cannot handle database exceptions
//TODO: save, show, update and tags were NOT generated
class JobGroupController {

    static scaffold = JobGroup
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    I18nService i18nService
    DefaultTimeToCsMappingService defaultTimeToCsMappingService
    TimeToCsMappingService timeToCsMappingService

    def save() {
        String configurationLabel = params.remove("csiConfiguration")
        def tagParam = params.remove('tags')
        def jobGroup = new JobGroup(params)

        CsiConfiguration configuration = CsiConfiguration.findByLabel(configurationLabel)
        if (configuration) {
            jobGroup.csiConfiguration = configuration
        }

        if (!jobGroup.save(flush: true)) {
            render(view: "create", model: [jobGroup: jobGroup])
            return
        } else {
            // Tags can only be set after first successful save.
            // This is why Job needs to be saved again.
            def tags = [tagParam].flatten()
            jobGroup.tags = tags
            jobGroup.save(flush: true)

            flash.message = message(code: 'default.created.message', args: [message(code: 'jobGroup.label', default: 'JobGroup'), jobGroup.id])
            redirect(action: "show", id: jobGroup.id)
        }
    }

    def show() {
        def jobGroup = JobGroup.get(params.id)
        def modelToRender = [:]

        CsiConfiguration config = jobGroup.csiConfiguration

        if (config) {
            //Labels for charts
            String zeroWeightLabel = i18nService.msg("de.iteratec.osm.d3Data.treemap.zeroWeightLabel", "Pages ohne Gewichtung")
            String dataLabel = i18nService.msg("de.iteratec.osm.d3Data.treemap.dataLabel", "Page")
            String weightLabel = i18nService.msg("de.iteratec.osm.d3Data.treemap.weightLabel", "Gewichtung")
            String xAxisLabel = i18nService.msg("de.iteratec.osm.d3Data.barChart.xAxisLabel", "Tageszeit")
            String yAxisLabel = i18nService.msg("de.iteratec.osm.d3Data.barChart.yAxisLabel", "Gewichtung")
            String matrixViewXLabel = i18nService.msg("de.iteratec.osm.d3Data.matrixView.xLabel", "Browser")
            String matrixViewYLabel = i18nService.msg("de.iteratec.osm.d3Data.matrixView.yLabel", "Conn")
            String matrixViewWeightLabel = i18nService.msg("de.iteratec.osm.d3Data.matrixView.weightLabel", "Weight")
            String colorBrightLabel = i18nService.msg("de.iteratec.osm.d3Data.matrixView.colorBrightLabel", "less")
            String colorDarkLabel = i18nService.msg("de.iteratec.osm.d3Data.matrixView.colorDarkLabel", "more")
            String matrixZeroWeightLabel = i18nService.msg("de.iteratec.osm.d3Data.matrixView.zeroWeightLabel", "Im CSI nicht berücksichtigt")

            // arrange matrixViewData
            MatrixViewData matrixViewData = new MatrixViewData(weightLabel: matrixViewWeightLabel, rowLabel: matrixViewYLabel, columnLabel: matrixViewXLabel, colorBrightLabel: colorBrightLabel, colorDarkLabel: colorDarkLabel, zeroWeightLabel: matrixZeroWeightLabel)
            matrixViewData.addColumns(Browser.findAll()*.name as Set)
            matrixViewData.addRows(ConnectivityProfile.findAll()*.name as Set)
            config.browserConnectivityWeights.each {
                matrixViewData.addEntry(new MatrixViewEntry(weight: it.weight, columnName: it.browser.name, rowName: it.connectivity.name))
            }
            def matrixViewDataJSON = matrixViewData as JSON

            // arrange treemap data
            TreemapData treemapData = new TreemapData(zeroWeightLabel: zeroWeightLabel, dataName: dataLabel, weightName: weightLabel);
            config.pageWeights.each { pageWeight -> treemapData.addNode(new ChartEntry(name: pageWeight.page.name, weight: pageWeight.weight)) }
            def treemapDataJSON = treemapData as JSON

            // arrange barchart data
            BarChartData barChartData = new BarChartData(xLabel: xAxisLabel, yLabel: yAxisLabel)
            (0..23).each {
                barChartData.addDatum(new ChartEntry(name: it, weight: config.csiDay.getHourWeight(it)))
            }
            def barChartJSON = barChartData as JSON

            MultiLineChart defaultTimeToCsMappingsChart = defaultTimeToCsMappingService.getDefaultMappingsAsChart(10000)

            // arrange page time to cs mapping chart data
            MultiLineChart pageTimeToCsMappingsChart
            if (config.timeToCsMappings) {
                pageTimeToCsMappingsChart = timeToCsMappingService.getPageMappingsAsChart(10000, config)
            }


            modelToRender = [matrixViewData          : matrixViewDataJSON,
                             treemapData             : treemapDataJSON,
                             barchartData            : barChartJSON,
                             defaultTimeToCsMappings : defaultTimeToCsMappingsChart as JSON,
                             selectedCsiConfiguration: config,
                             pageTimeToCsMappings    : pageTimeToCsMappingsChart as JSON,
                             pageMappingsExist       : pageTimeToCsMappingsChart ? true : false]
        }

        modelToRender.put("jobGroup", jobGroup)

        return modelToRender
    }

    def update() {
        def jobGroup = JobGroup.get(params.id)
        if (!jobGroup) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'jobGroup.label', default: 'JobGroup'), params.id])
            redirect(action: "list")
            return
        }

        jobGroup.graphiteServers.clear()
        if (params.version) {
            def version = params.version.toLong()
            if (jobGroup.version > version) {
                jobGroup.errors.rejectValue("version", "default.optimistic.locking.failure",
                        [message(code: 'jobGroup.label', default: 'JobGroup')] as Object[],
                        "Another user has updated this JobGroup while you were editing")
                render(view: "edit", model: [jobGroup: jobGroup])
                return
            }
        }
        String csiConfigLabel = params.remove("csiConfiguration")
        if (csiConfigLabel != null) {
            CsiConfiguration config = CsiConfiguration.findByLabel(csiConfigLabel)
            jobGroup.csiConfiguration = config
        } else {
            jobGroup.csiConfiguration = null
        }


        jobGroup.graphiteServers.clear()
        params.list('graphiteServers').each{
            jobGroup.graphiteServers.add(GraphiteServer.findById(it))
        }
        params.remove('graphiteServers')
        def tagParam = params.remove('tags')
        def tags = [tagParam].flatten()
        jobGroup.tags = tags
        jobGroup.properties = params
        if (!jobGroup.save(flush: true)) {
            render(view: "edit", model: [jobGroup: jobGroup])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'jobGroup.label', default: 'JobGroup'), jobGroup.id])
        redirect(action: "show", id: jobGroup.id)
    }

    /**
     * List tags starting with term.
     */
    def tags(String term) {
        render JobGroup.findAllTagsWithCriteria([max: 5]) { ilike('name', "${term}%") } as JSON
    }




    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond JobGroup.list(params), model:[jobGroupCount: JobGroup.count()]
    }



    def create() {
        respond new JobGroup(params)
    }



    def edit(JobGroup jobGroup) {
        respond jobGroup
    }



    def delete(JobGroup jobGroup) {

        if (jobGroup == null) {
            notFound()
            return
        }

        try {
            jobGroup.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'jobGroup.label', default: 'JobGroup'), params.id])
            redirect(action: "index")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'jobGroup.label', default: 'JobGroup'), params.id])
            redirect(action: "show", id: params.id)
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'jobGroup.label', default: 'JobGroup'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
