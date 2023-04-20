package io.exonym.utils.crypto;

import io.exonym.lite.parallel.ResourceGenerator;
import io.exonym.lite.standard.AsymStoreKey;

public class KeyGenerator implements ResourceGenerator<AsymStoreKey> {

	public KeyGenerator() {
	}

	@Override
	public AsymStoreKey generateResource() throws Exception {
		return new AsymStoreKey();
		
	}
}
