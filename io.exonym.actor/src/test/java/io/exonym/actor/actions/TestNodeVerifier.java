package io.exonym.actor.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.exonym.lite.exceptions.UxException;

public class TestNodeVerifier {

	private static final Logger logger = LogManager.getLogger(TestNodeVerifier.class);

	public TestNodeVerifier() {
		
	}

	public void name() throws UxException, MalformedURLException {
		URL url0 = new URL("https://existence.global/jti/x-source");
		URL url1 = new URL("https://existence.global/jti/x-source/");
		URL url2 = new URL("https://existence.global/jti/x-source/abc.xml");
		URL url3 = new URL("https://existence.global/jti/x-node");
		URL url4 = new URL("https://existence.global/jti/x-node/");
		URL url5 = new URL("https://existence.global/jti/x-node/abc.xml/");

		assert(url1.equals(NodeVerifier.trainAtFolder(url0)));
		assert(url1.equals(NodeVerifier.trainAtFolder(url1)));
		assert(url1.equals(NodeVerifier.trainAtFolder(url2)));
		
		assert(url4.equals(NodeVerifier.trainAtFolder(url3)));
		assert(url4.equals(NodeVerifier.trainAtFolder(url4)));
		assert(url4.equals(NodeVerifier.trainAtFolder(url5)));
		
		try {
			NodeVerifier.trainAtFolder(new URL("https://www.existence.global/jti/"));
			assert(false);
			
		} catch (Exception e) {
			logger.info("Correctly produced error " + e.getMessage());
			assert(true);
			
		}
	}
	
	// No files at node
	// Node has no signature
	// Non IDMX / Non-existence xml
	// binary file
	// binary file masquerading as xml
	// use different public key
	// change xml file
	// flip a signature byte
	// 
	


}
