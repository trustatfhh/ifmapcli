package de.fhhannover.inform.trust.ifmapcli;

/*
 * #%L
 * ====================================================
 *   _____                _     ____  _____ _   _ _   _
 *  |_   _|_ __ _   _ ___| |_  / __ \|  ___| | | | | | |
 *    | | | '__| | | / __| __|/ / _` | |_  | |_| | |_| |
 *    | | | |  | |_| \__ \ |_| | (_| |  _| |  _  |  _  |
 *    |_| |_|   \__,_|___/\__|\ \__,_|_|   |_| |_|_| |_|
 *                             \____/
 * 
 * =====================================================
 * 
 * Fachhochschule Hannover 
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.inform.fh-hannover.de/
 * 
 * This file is part of Ifmapcli, version 0.0.4, implemented by the Trust@FHH 
 * research group at the Fachhochschule Hannover.
 * %%
 * Copyright (C) 2010 - 2013 Trust@FHH
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

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.net.ssl.TrustManager;

import org.w3c.dom.Document;

import de.fhhannover.inform.trust.ifmapcli.common.Common;
import de.fhhannover.inform.trust.ifmapcli.common.Config;
import de.fhhannover.inform.trust.ifmapj.IfmapJ;
import de.fhhannover.inform.trust.ifmapj.IfmapJHelper;
import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.exception.InitializationException;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishRequest;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * A simple tool that publishes or deletes layer2-information metadata on a link<br/>
 * between access-request and device identifiers.<br/>
 * When metadata is published, the lifetime is set to be 'forever'.
 * 
 * @author ib
 *
 */
public class Layer2Info {
	final static String CMD = "layer2-info";
	final static int MIN_ARGS = 6;			// update|delete, ar, dev, vlan, name, port
	final static int EXPECTED_ARGS = 11;	// update|delete, ar, dev, vlan, name, port
											// url, user, pass,
											// keystorePath, keystorePass
	
	// in order to create the necessary objects, make use of the appropriate
	// factory classes
	private static StandardIfmapMetadataFactory mf = IfmapJ
			.createStandardMetadataFactory();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String op, ar, dev, vlan, name, port;
		Config cfg;
		SSRC ssrc;
		PublishRequest req;
		PublishUpdate publishUpdate;
		PublishDelete publishDelete;
		TrustManager[] tms;
		Identifier arIdentifier;
		Identifier devIdentifier;
		Document metadata;
		InputStream is;
		
		// check number of mandatory command line arguments
		if(args.length < MIN_ARGS){
			Layer2Info.usage();
			return;
		}
		
		// parse mandatory command line arguments
		op = args[0];
		ar = args[1];
		dev = args[2];
		vlan = args[3];
		name = args[4];
		port = args[5];
		if(Common.isUpdateorDelete(op) == false){
			Layer2Info.usage();
			return;
		}
		
		// check and load optional parameters
		cfg = Common.checkAndLoadParams(args, EXPECTED_ARGS);
		System.out.println(CMD + " uses config " + cfg);
		
		// prepare identifiers
		arIdentifier = Identifiers.createAr(ar);
		devIdentifier = Identifiers.createDev(dev);
		
		// prepare metadata
		metadata = mf.createLayer2Information(Integer.valueOf(vlan), name, Integer.valueOf(port), null);
		
		// update or delete
		if(Common.isUpdate(op)){
			publishUpdate = Requests.createPublishUpdate(arIdentifier, devIdentifier,
					metadata, MetadataLifetime.forever);			
			req = Requests.createPublishReq(publishUpdate);
		} else {
			String filter = "meta:layer2-information[vlan=" + vlan + " " +
													"and vlan-name='" + name + "' " +
													"and port=" + port + 
													"]";
			System.out.println(filter);
			publishDelete = Requests.createPublishDelete(arIdentifier, devIdentifier, filter);
			publishDelete.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
					IfmapStrings.STD_METADATA_NS_URI);
			req = Requests.createPublishReq(publishDelete);
		}
				
		// publish
		try {
			is = Common.prepareTruststoreIs(cfg.getTruststorePath());
			tms = IfmapJHelper.getTrustManagers(is, cfg.getTruststorePass());
			ssrc = IfmapJ.createSSRC(cfg.getUrl(), cfg.getUser(), cfg.getPass(), tms);
			ssrc.newSession();
			ssrc.publish(req);
			ssrc.endSession();
		} catch (InitializationException e) {
			System.out.println(e.getDescription() + " " + e.getMessage());
		} catch (IfmapErrorResult e) {
			System.out.println(e.getErrorString());
		} catch (IfmapException e) {
			System.out.println(e.getDescription() + " " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private static void usage() {
		System.out.println("usage:\n" +
				"\t" + Layer2Info.CMD + " update|delete ar dev vlan name port " +
				"[url user pass truststore truststorePass]");
		System.out.println(Common.USAGE);
	}
}
