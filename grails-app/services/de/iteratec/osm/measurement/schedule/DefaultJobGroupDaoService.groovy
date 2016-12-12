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

package de.iteratec.osm.measurement.schedule

import de.iteratec.osm.measurement.schedule.dao.JobGroupDaoService
import de.iteratec.osm.result.JobResult
import org.hibernate.FetchMode

/**
 * <p>
 * Default implementation of {@link de.iteratec.osm.measurement.schedule.dao.JobGroupDaoService}.
 * </p>
 * 
 * @author nkuhn
 * @author mze
 */
class DefaultJobGroupDaoService implements JobGroupDaoService {

	@Override
	Map<Serializable, JobGroup> getIdToObjectMap(){
		return JobGroup.list().collectEntries { JobGroup eachJobGroup ->
			[
				eachJobGroup.ident(),
				eachJobGroup
			]
		}
	}

	@Override
	public Set<JobGroup> findAll() {
		Set<JobGroup> result = Collections.checkedSet(new HashSet<JobGroup>(), JobGroup.class);
		result.addAll(JobGroup.list());
		return Collections.unmodifiableSet(result);
	}
	
	@Override
	public Set<JobGroup> findCSIGroups() {
		Set<JobGroup> result = Collections.checkedSet(new HashSet<JobGroup>(), JobGroup.class);
		result.addAll(JobGroup.findAllByCsiConfigurationIsNotNull());
		return Collections.unmodifiableSet(result);
	}

    @Override
    public List<String> getAllUniqueTags(){
        return JobGroup.allTags
    }

    @Override
    public List<String> getMaxUniqueTags(int maxNumberOfTags){
        return JobGroup.findAllTagsWithCriteria([max:maxNumberOfTags]) {}
    }

    @Override
    public Map<String, List<String>> getTagToJobGroupNameMap(){
        return getAllUniqueTags().inject([:]){map, tag->
            map[tag] = JobGroup.findAllByTag(tag)*.name
            return map
        }
    }

	@Override
	public Collection<JobGroup> findByJobResultsInTimeFrame(Date from, Date to) {
		return (Collection<JobGroup>) JobResult.createCriteria().list {
			fetchMode('job', FetchMode.JOIN)
			fetchMode('job.jobGroup', FetchMode.JOIN)
			between("date", from, to)
			eq("httpStatusCode", 200)
			projections {
				job {
					distinct('jobGroup')
				}
			}
		}
	}
}
