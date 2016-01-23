/*
 * Copyright (C) 2011 Vex Software LLC
 * This file is part of Votifier.
 * 
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vexsoftware.votifier.net.protocol.v1crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;

/**
 * An RSA key pair generator.
 *
 * @author Blake Beaupain
 */
public class RSAKeygen {
    /**
     * Generates an RSA key pair.
     *
     * @param bits The amount of bits
     * @return The key pair
     */
    public static KeyPair generate(int bits) throws Exception {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(bits,
                RSAKeyGenParameterSpec.F4);
        keygen.initialize(spec);
        return keygen.generateKeyPair();
    }

}
