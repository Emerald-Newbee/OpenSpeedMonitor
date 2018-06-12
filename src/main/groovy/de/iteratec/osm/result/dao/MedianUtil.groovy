package de.iteratec.osm.result.dao

class MedianUtil {

    static def getMedianFrom(List data) {
        data.removeAll([null])
        data.sort()
        if (data) {
            if (data.size() == 1) {
                return data.get(0)
            }
            if ((data.size() % 2) != 0) {
                return data.get((Integer) ((data.size() - 1) / 2))
            } else {
                return (data.get((Integer) (data.size() / 2)) +
                        data.get((Integer) ((data.size() - 1) / 2))) / 2
            }
        }
    }

    private static Set<String> getAggregatorAliases(Set<ProjectionProperty> projectionPropertySet){
        return projectionPropertySet.collect{it.alias}
    }

    static String generateGroupKeyForMedianAggregators(Map eventResultProjection, Set<ProjectionProperty> projectionPropertySet){
        Set aggregators = getAggregatorAliases(projectionPropertySet)
        String key = ""
        aggregators.each {
            key += "_" + eventResultProjection."$it"
        }
        return key
    }

    static Map getMetaDataSample(Map eventResultProjection, Set<ProjectionProperty> projectionPropertySet){
        return eventResultProjection.subMap(getAggregatorAliases(projectionPropertySet))
    }
}
