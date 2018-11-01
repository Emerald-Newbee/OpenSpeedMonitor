package de.iteratec.osm.persistence

import de.iteratec.osm.ConfigService
import de.iteratec.osm.InMemoryConfigService
import org.joda.time.DateTime

class DbCleanupOldJobResultsWithDependenciesJob {

    DbCleanupService dbCleanupService
    ConfigService configService
    InMemoryConfigService inMemoryConfigService

    boolean createBatchActivity = true

    static triggers = {
        /**
         * Each Day at 3:00 am.
         */
        cron(name: 'DailyOldJobResultsWithDependenciesCleanup', cronExpression: '0 0 3 ? * *')
    }

    def execute() {
        if(inMemoryConfigService.isDatabaseCleanupEnabled()){
            Date toDeleteResultsBefore = new DateTime().minusMonths(configService.getMaxDataStorageTimeInMonths()).toDate()
            dbCleanupService.deleteResultsDataBefore(toDeleteResultsBefore, createBatchActivity)
            dbCleanupService.deleteResultSelectionInformationBefore(toDeleteResultsBefore, createBatchActivity)
        }
    }
}
