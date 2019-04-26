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

import java.io.IOException;
import java.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Static utility methods for saving and loading RSA key pairs.
 */
public class RSAIO {

    /**
     * Saves the key pair to the disk.
     *
     * @param directory The directory to save to
     * @param keyPair   The key pair to save
     * @throws Exception If an error occurs
     */
    public static void save(File directory, KeyPair keyPair) throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Store the public and private keys.
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(
                publicKey.getEncoded());
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(
                privateKey.getEncoded());
        try (FileOutputStream publicOut = new FileOutputStream(directory + "/public.key");
             FileOutputStream privateOut = new FileOutputStream(directory + "/private.key")) {

            publicOut.write(Base64.getEncoder().encode(publicSpec.getEncoded()));
            privateOut.write(Base64.getEncoder().encode(privateSpec.getEncoded()));
        }
    }

    public static byte[] readB64File(File directory, String name) throws IOException {
        File f = new File(directory, name);
        byte[] contents = Files.readAllBytes(f.toPath());
        String strContents = new String(contents);
        strContents = strContents.trim();
        try {
            return Base64.getDecoder().decode(strContents);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Base64 decoding exception: This is probably due to a corrupted file, " +
                    "but in case it isn't, here is a b64 representation of what we read: " + new String(Base64.getEncoder().encode(contents), StandardCharsets.UTF_8), e);
        }
    }

    /**
     * Loads an RSA key pair from a directory. The directory must have the files
     * "public.key" and "private.key".
     *
     * @param directory The directory to load from
     * @return The key pair
     * @throws Exception If an error occurs
     */
    public static KeyPair load(File directory) throws Exception {
        // Read the public key file.
        byte[] encodedPublicKey = readB64File(directory, "public.key");

        // Read the private key file.
        byte[] encodedPrivateKey = readB64File(directory, "private.key");

        // Instantiate and return the key pair.
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return new KeyPair(publicKey, privateKey);
    }

}
