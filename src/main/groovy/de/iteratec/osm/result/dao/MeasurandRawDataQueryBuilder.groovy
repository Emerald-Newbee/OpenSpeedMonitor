package de.iteratec.osm.result.dao

import de.iteratec.osm.result.EventResult
import de.iteratec.osm.result.SelectedMeasurand
import de.iteratec.osm.util.PerformanceLoggingService

/**
 * Created by mwg on 11.10.2017.
 */
class MeasurandRawDataQueryBuilder implements SelectedMeasurandQueryBuilder {

    List<SelectedMeasurand> selectedMeasurands

    @Override
    Closure buildProjection(Set<ProjectionProperty> baseProjections) {
        List<SelectedMeasurand> measurands = selectedMeasurands.findAll { !it.selectedType.isUserTiming() }
        if (!measurands) {
            return null
        }
        return {
            projections {
                measurands.each {
                    property it.databaseRelevantName, it.databaseRelevantName
                }
                baseProjections.each {ProjectionProperty pp ->
                    property pp.dbName, pp.alias
                }
            }
        }
    }

    @Override
    void configureForSelectedMeasurands(List<SelectedMeasurand> selectedMeasurands){
        this.selectedMeasurands = selectedMeasurands
    }

    @Override
    List<EventResultProjection> getResultsForFilter(List<Closure> baseFilters, Set<ProjectionProperty> baseProjections, List<MeasurandTrim> trims, PerformanceLoggingService performanceLoggingService) {
        return createEventResultProjections(getRawQueryResults(baseFilters, baseProjections, trims))
    }

    protected List<Map> getRawQueryResults(List<Closure> baseFilters, Set<ProjectionProperty> baseProjections, List<MeasurandTrim> trims){
        List<Closure> filters = []
        filters.addAll(baseFilters)
        filters.addAll(buildTrim(trims))
        filters.add(buildProjection(baseProjections))
        return executeQuery(filters)
    }

    private List<Closure> buildTrim(List<MeasurandTrim> trims){
        List<Closure> trimClosures = []
        trims = trims?:[]
        trims.each{MeasurandTrim trim->
            this.selectedMeasurands.each{SelectedMeasurand selectedMeasurand ->
                if (selectedMeasurand.measurandGroup == trim.measurandGroup){
                    trimClosures.add({
                        "${trim.qualifier.getGormSyntax()}" "${selectedMeasurand.databaseRelevantName}", trim.value
                    })
                }
            }
        }
        return trimClosures
    }

    List<EventResultProjection> createEventResultProjections(List<Map> dataFromDb) {
        List<EventResultProjection> eventResultProjections = []
        dataFromDb.each {Map dbResult ->
            EventResultProjection eventResultProjection = new EventResultProjection(
                    id: dbResult.id
            )
            dbResult.remove('id')
            eventResultProjection.projectedProperties = dbResult
            eventResultProjections += eventResultProjection
        }
        return eventResultProjections
    }

    List<Map> executeQuery(List<Closure> queryParts) {
        return EventResult.createCriteria().list {
            queryParts.each {
                applyClosure(it, delegate)
            }
        }
    }

    void applyClosure(Closure closure, def criteriaBuilder) {
        closure.delegate = criteriaBuilder
        closure()
    }
}