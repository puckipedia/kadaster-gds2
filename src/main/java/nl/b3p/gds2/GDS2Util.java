/*
 * Copyright (C) 2017 B3Partners B.V.
 */
package nl.b3p.gds2;

import nl.kadaster.schemas.gds2.afgifte_bestandenlijstopvragenresultaat.v20170401.BestandenlijstOpvragenResultaatType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstresultaat.afgifte.v20170401.AfgifteType;
import nl.kadaster.schemas.gds2.imgds.baseurl.v20170401.BaseURLType;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenRequest;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * @author mprins
 */
public class GDS2Util {

    private static final Log LOG = LogFactory.getLog(GDS2Util.class);
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

    /**
     * vraagt de bestandenlijst op in maximaal 2 pogingen met 10000 millisec pauze.
     *
     * @param gds2    afgifte service
     * @param request geconfigureerd verzoek
     * @return opgevraagde bestanden lijst
     * @see #retryBestandenLijstOpvragen(Gds2AfgifteServiceV20170401, BestandenlijstOpvragenRequest, int, int)
     */
    public static BestandenlijstOpvragenResponse retryBestandenLijstOpvragen(Gds2AfgifteServiceV20170401 gds2, BestandenlijstOpvragenRequest request) throws Exception {
        return retryBestandenLijstOpvragen(gds2, request, 2, 10000L);
    }

    /**
     * vraagt de bestandenlijst op.
     *
     * @param gds2      afgifte service
     * @param request   geconfigureerd verzoek
     * @param retries   aantal pogingen om verzoek uit te voeren
     * @param retryWait te wachten milliseconden tussen retries, wordt vermenigvuldigd met retry poging (dus periode steeds langer)
     * @return opgevraagde bestanden lijst
     */
    public static BestandenlijstOpvragenResponse retryBestandenLijstOpvragen(Gds2AfgifteServiceV20170401 gds2, BestandenlijstOpvragenRequest request, int retries, long retryWait) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return gds2.bestandenlijstOpvragen(request);
            } catch (Exception e) {
                attempt++;
                if (attempt == retries) {
                    LOG.error("Fout bij laatste poging ophalen bestandenlijst: " + e.getClass().getName() + ": " + e.getMessage());
                    throw e;
                } else {
                    LOG.warn("Fout bij poging " + attempt + " om bestandenlijst op te halen: " + e.getClass().getName() + ": " + e.getMessage());
                    Thread.sleep(retryWait * attempt);
                    LOG.info("Uitvoeren poging " + (attempt + 1) + " om bestandenlijst op te halen...");
                }
            }
        }
    }

    /**
     * bepaal de "certificaat" url, nodig voor BRK download met PKI.
     *
     * @param antwoord de url
     * @return type of {@code null}
     */
    public static BaseURLType getCertificaatBaseURL(BestandenlijstOpvragenResultaatType antwoord) {
        for (BaseURLType type : antwoord.getBaseURLSet().getBaseURL()) {
            if (type.getType().equalsIgnoreCase("certificaat")) {
                return type;
            }
        }
        return null;
    }

    /**
     * bepaal de "anoniem" url, nodig voor BAG download zonder PKI.
     *
     * @param antwoord de url
     * @return type of {@code null}
     */
    public static BaseURLType getAnoniemBaseURL(BestandenlijstOpvragenResultaatType antwoord) {
        for (BaseURLType type : antwoord.getBaseURLSet().getBaseURL()) {
            if (type.getType().equalsIgnoreCase("anoniem")) {
                return type;
            }
        }
        return null;
    }

    /**
     * bepaal de afgifte url.
     *
     * @param afgifte de afgifte
     * @param type    de base url
     * @return de afgifte url
     */
    public static String getAfgifteURL(AfgifteType afgifte, BaseURLType type) {
        return type.getValue() + "/" + afgifte.getAfgifteID();
    }


    /**
     * parse datum uit string.
     *
     * @param dateStr datum in dd-MM-yyyy formaat
     * @return datum (of null in geval van een parse fout)
     */
    public static GregorianCalendar getDatumTijd(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        Date date;
        final DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        if (dateStr.equalsIgnoreCase("nu")) {
            date = new Date();
        } else {
            try {
                date = format.parse(dateStr);
            } catch (ParseException ex) {
                LOG.error(ex);
                return null;
            }
        }
        GregorianCalendar gregory = new GregorianCalendar();
        gregory.setTime(date);
        return gregory;
    }

    /**
     * parse datum uit string en verschuif {@code dagen}.
     *
     * @param refDate datum in dd-MM-yyyy formaat
     * @param before  aantal dagen dat de datum verschoven moet worden, bijvoorbeeld -3 voor 3 dagen eerder
     * @return datum (of null in geval van een parse fout)
     */
    public static GregorianCalendar getDatumTijd(String refDate, int before) {
        GregorianCalendar ref = getDatumTijd(refDate);
        ref.add(GregorianCalendar.DAY_OF_YEAR, before);
        return ref;
    }

    /**
     * maakt een XML datum die te gebruiken is in een "van" of "tot" criterium, houdt rekening met de juiste maand.
     *
     * @param year  jaartal (4 cijfers, > 2000)
     * @param month maand (waarde van 0 t/m 12)
     * @param day   dag van de maand
     * @return xml datum (of null ingeval van een DatatypeConfigurationException)
     */
    public static XMLGregorianCalendar getXMLDatumTijd(int year, int month, int day) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    new GregorianCalendar(
                            year,
                            month - 1 /* GregorianCalendar heeft 0-based month */,
                            day)
            );
        } catch (DatatypeConfigurationException e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * maakt een XML datum die te gebruiken is in een "van" of "tot" criterium.
     *
     * @param date datum
     * @return xml datum (of null ingeval van een DatatypeConfigurationException)
     */
    public static XMLGregorianCalendar getXMLDatumTijd(Date date) {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (DatatypeConfigurationException e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * maakt een XML datum die te gebruiken is in een "van" of "tot" criterium.
     *
     * @param date datum
     * @return xml datum (of null ingeval van een DatatypeConfigurationException)
     */
    public static XMLGregorianCalendar getXMLDatumTijd(GregorianCalendar date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
        } catch (DatatypeConfigurationException e) {
            LOG.error(e);
            return null;
        }
    }

    private GDS2Util() {
    }
}
