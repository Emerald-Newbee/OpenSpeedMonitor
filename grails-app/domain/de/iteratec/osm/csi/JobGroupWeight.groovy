package de.iteratec.osm.csi

import de.iteratec.osm.measurement.schedule.JobGroup

class JobGroupWeight {
    JobGroup jobGroup
    Double weight

    static belongsTo = [csiSystem: CsiSystem]
    static constraints = {
        jobGroup validator: {return it.csiConfiguration != null}
    }
}
