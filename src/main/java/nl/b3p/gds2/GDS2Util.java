/*
 * Copyright (C) 2017 B3Partners B.V.
 */
package nl.b3p.gds2;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 *
 * @author mprins
 */
public class GDS2Util {

    private static final String PEM_KEY_START = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_KEY_END = "-----END PRIVATE KEY-----";
    private static final String PEM_CERT_START = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_CERT_END = "-----END CERTIFICATE-----";

    public static Certificate getCertificateFromPEM(String pem) throws CertificateException, UnsupportedEncodingException {
        if (!pem.startsWith(PEM_CERT_START)) {
            throw new IllegalArgumentException("Certificaat moet beginnen met " + PEM_CERT_START);
        }
        if (!pem.endsWith(PEM_CERT_END)) {
            throw new IllegalArgumentException("Certificaat moet eindigen met " + PEM_CERT_END);
        }
        return CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(pem.getBytes("US-ASCII")));
    }

    public static PrivateKey getPrivateKeyFromPEM(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (!pem.startsWith(PEM_KEY_START)) {
            throw new IllegalArgumentException("Private key moet beginnen met " + PEM_KEY_START);
        }
        while (pem.endsWith("\n")) {
            pem = pem.substring(0, pem.length() - 1);
        }
        if (!pem.endsWith(PEM_KEY_END)) {
            throw new IllegalArgumentException("Private key moet eindigen met " + PEM_KEY_END);
        }
        pem = pem.replace(PEM_KEY_START, "").replace(PEM_KEY_END, "");

        byte[] decoded = Base64.getMimeDecoder().decode(pem);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return kf.generatePrivate(spec);
    }

    private GDS2Util() {
    }
}
