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

package de.iteratec.osm.measurement.environment


class BrowserAlias {

    static belongsTo = [browser: Browser]

    String alias

    static constraints = {
        alias(unique: true, nullable: false, maxSize: 255, validator: {String currentAlias ->
            if(currentAlias.trim() == "") {
                return [
                    'de.iteratec.osm.measurement.environment.browserAlias.alias.validator.error'
                ]
            }
        })
    }

    @Override
    String toString() {
        return alias
    }
}
