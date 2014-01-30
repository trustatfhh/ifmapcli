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
 * This file is part of ifmapcli (cap), version 0.0.6, implemented by the Trust@HsH
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

import java.io.InputStream;

import javax.net.ssl.TrustManager;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.w3c.dom.Document;

import de.hshannover.f4.trust.ifmapcli.common.Common;
import de.hshannover.f4.trust.ifmapcli.common.ParserUtil;
import de.hshannover.f4.trust.ifmapj.IfmapJ;
import de.hshannover.f4.trust.ifmapj.IfmapJHelper;
import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishRequest;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * A simple tool that publishes or deletes capability metadata.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 *
 * @author ib
 *
 */
public class Cap {
	final static String CMD = "cap";
	
	// in order to create the necessary objects, make use of the appropriate
	// factory classes
	private static StandardIfmapMetadataFactory mf = IfmapJ
			.createStandardMetadataFactory();

	public static void main(String[] args) {
		final String KEY_OPERATION = "publishOperation";
		final String KEY_AR = "accessRequest";
		final String KEY_CAP_NAME = "capability";
		final String KEY_ADMINISTRATIVE_DOMAIN = "administrative-domain";

		ArgumentParser parser = ArgumentParsers.newArgumentParser(CMD);
		parser.addArgument("publish-operation")
			.type(String.class)
			.dest(KEY_OPERATION)
			.choices("update", "delete")
			.help("the publish operation");
		parser.addArgument("access-request")
			.type(String.class)
			.dest(KEY_AR)
			.help("name of the access-request identifier");
		parser.addArgument("capability")
			.type(String.class)
			.dest(KEY_CAP_NAME)
			.help("name of the capability metadatum");
		parser.addArgument("--administrative-domain")
			.type(String.class)
			.dest(KEY_ADMINISTRATIVE_DOMAIN)
			.help("value of the administrative domain");
		ParserUtil.addConnectionArgumentsTo(parser);
		ParserUtil.addCommonArgumentsTo(parser);

		Namespace res = null;
		try {
			res = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}

		if (res.getBoolean(ParserUtil.VERBOSE)) {
			StringBuilder sb = new StringBuilder();
			
			sb.append(CMD).append(" ");
			sb.append(res.getString(KEY_OPERATION)).append(" ");
			sb.append(KEY_AR).append("=").append(res.getString(KEY_AR)).append(" ");
			sb.append(KEY_CAP_NAME).append("=").append(res.getString(KEY_CAP_NAME)).append(" ");
			if (res.getString(KEY_ADMINISTRATIVE_DOMAIN) != null) {
				sb.append(KEY_ADMINISTRATIVE_DOMAIN).append("=").append(res.getString(KEY_ADMINISTRATIVE_DOMAIN)).append(" ");
			}
			
			ParserUtil.printConnectionArguments(sb, res);
			System.out.println(sb.toString());
		}

		PublishRequest req;
		PublishUpdate publishUpdate;
		PublishDelete publishDelete;

		// prepare identifiers
		Identifier arIdentifier = Identifiers.createAr(res.getString(KEY_AR));

		Document metadata = null;
		// prepare metadata
		if (res.getString(KEY_ADMINISTRATIVE_DOMAIN) != null) {			
			metadata = mf.createCapability(res.getString(KEY_CAP_NAME), res.getString(KEY_ADMINISTRATIVE_DOMAIN));
		} else {			
			metadata = mf.createCapability(res.getString(KEY_CAP_NAME));
		}

		// update or delete
		if (res.getString(KEY_OPERATION).equals("update")) {
			publishUpdate = Requests.createPublishUpdate(arIdentifier,
					metadata, MetadataLifetime.forever);
			req = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:capability[name='" + res.getString(KEY_CAP_NAME) + "']";
			publishDelete = Requests.createPublishDelete(arIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			req = Requests.createPublishReq(publishDelete);
		}

		// publish
		try {
			InputStream is = Common.prepareTruststoreIs(res.getString(ParserUtil.KEYSTORE_PATH));
			TrustManager[] tms = IfmapJHelper.getTrustManagers(is, res.getString(ParserUtil.KEYSTORE_PASS));
			SSRC ssrc = IfmapJ.createSSRC(
				res.getString(ParserUtil.URL),
				res.getString(ParserUtil.USER),
				res.getString(ParserUtil.PASS),
				tms);
			ssrc.newSession();
			ssrc.publish(req);
			ssrc.endSession();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

