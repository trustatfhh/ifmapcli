/*
 * #%L
 * =====================================================
 *   _____                _     ____  _   _       _   _
 *  |_   _|_ __ _   _ ___| |_  / __ \| | | | ___ | | | |
 *    | | | '__| | | / __| __|/ / _` | |_| |/ __|| |_| |
 *    | | | |  | |_| \__ \ |_| | (_| |  _  |\__ \|  _  |
 *    |_| |_|   \__,_|___/\__|\ \__,_|_| |_||___/|_| |_|
 *                             \____/
 * 
 * =====================================================
 * 
 * Hochschule Hannover
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.f4.hs-hannover.de
 * 
 * This file is part of ifmapcli (purge), version 0.0.6, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2013 Trust@HsH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.hshannover.f4.trust.ifmapcli;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import de.hshannover.f4.trust.ifmapcli.common.AbstractClient;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;

/**
 * A simple tool that purges a publisher </br>.
 *
 * @author ib
 *
 */
public class Purge extends AbstractClient {

	public static void main(String[] args) {
		command = "purge";
		
		final String KEY_PUBLISHER_ID = "publisherId";

		ArgumentParser parser = createDefaultParser();
		parser.addArgument("--publisher-id", "-p")
			.type(String.class)
			.dest(KEY_PUBLISHER_ID)
			.help("the publisher id");

		parseParameters(parser, args);
		
		printParameters(new String[] {KEY_PUBLISHER_ID});
		
		String publisherId = resource.getString(KEY_PUBLISHER_ID);
		
		// purge
		try {
			SSRC ssrc = createSSRC();
			ssrc.newSession();
			if (publisherId != null) {
				ssrc.purgePublisher(publisherId);
			} else {
				ssrc.purgePublisher();
			}
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
