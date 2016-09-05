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

package de.iteratec.osm.batch

import java.text.DecimalFormat

/**
 * BatchActivity
 * Representation of a batch activity like job deletion
 */
class BatchActivity {
    long id

    String name
    String domain
    Activity activity
    Status status
    Date startDate

    String lastFailureMessage
    Date lastUpdate
    String stageDescription
    int maximumStages
    int actualStage
    int stepInStage
    int maximumStepsInStage
    int failures
    Date endDate

    static mapping = {
    }

    static constraints = {
        name(nullable: false)
        domain(nullable: false)
        activity(nullable: false)
        status(nullable: false)
        startDate(nullable: false)
        lastFailureMessage(nullable: true)
        lastUpdate(nullable: true)
        stageDescription(nullable: true)
        maximumStages(nullable: true)
        actualStage(nullable: true)
        stepInStage(nullable: true)
        maximumStepsInStage(nullable: true)
        failures(nullable: true)
        endDate(nullable: true)
    }

    /**
     * Creates a String representation for BatchActivity progress in stage
     * @return formatted string
     */
    public String calculateProgressInStage() {
        DecimalFormat df = new DecimalFormat("#.##");
        String returnValue
        if (maximumStepsInStage == 0){
            if(this.status == Status.DONE){
                returnValue =  df.format(100) + " %"
            } else{
                returnValue =  df.format(0) + " %"
            }
        } else{
            returnValue = df.format(100.0 / maximumStepsInStage * stepInStage) + " %"
        }
        return returnValue
    }

    @Override
    public String toString() {
        return "Domain: $domain, Name: $name ,Stage: $actualStage/$maximumStages, Step: $stepInStage/$maximumStepsInStage";
    }
}
