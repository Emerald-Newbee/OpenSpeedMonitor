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

package de.iteratec.osm.report.external.provider;

import de.iteratec.osm.report.external.GraphiteServer;
import de.iteratec.osm.report.external.GraphiteSocket;

/**
 * <p>
 * A provider for {@linkplain GraphiteSocket Graphite sockets}.
 * </p>
 * 
 * <p>
 * Unless otherwise noted passing null to any argument will cause a
 * {@link NullPointerException} to be thrown.
 * </p>
 * 
 * @author mze
 * @since 2013-11-06 / JIRA IT-195
 */
public interface GraphiteSocketProvider {

	/**
	 * Supported protocols for Graphite sockets.
	 * 
	 * @author mze
	 */
	enum Protocol {
		TCP
	}

	/**
	 * Returns a TCP-{@link GraphiteSocket}
	 * 
	 * @param server
	 *            the server to connect to
	 * @return never <code>null</code>.
	 */
	GraphiteSocket getSocket(GraphiteServer server);

	/**
	 * Returns a {@link Protocol} specific {@link GraphiteSocket}
	 * 
	 * @param server
	 *            the server to connect to
	 * @param protocol
	 *            the protocol to use
	 * @return never <code>null</code>.
	 */
	GraphiteSocket getSocket(GraphiteServer server, Protocol protocol);

}
