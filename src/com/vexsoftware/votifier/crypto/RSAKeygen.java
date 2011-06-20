package com.vexsoftware.votifier.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.logging.Logger;

/**
 * An RSA key pair generator.
 * 
 * @author Blake Beaupain
 */
public class RSAKeygen {

	/** The logger instance. */
	private static final Logger log = Logger.getLogger("RSAKeygen");

	/**
	 * Generates an RSA key pair.
	 * 
	 * @param bits
	 *            The amount of bits
	 * @return The key pair
	 */
	public static KeyPair generate(int bits) throws Exception {
		log.info("Generating RSA key pair...");
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);
		return keygen.generateKeyPair();
	}

}
