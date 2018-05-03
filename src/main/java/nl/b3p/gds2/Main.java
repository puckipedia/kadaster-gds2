/*
 * Copyright (C) 2015 B3Partners B.V.
 */
package nl.b3p.gds2;

import com.sun.xml.ws.developer.JAXWSProperties;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstgbopvragen.v20130701.BestandenlijstGbOpvragenType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstgbresultaat.afgifte.v20130701.AfgifteGBType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstopvragen.v20130701.BestandenlijstOpvragenType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstresultaat.afgifte.v20130701.AfgifteType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20130701.AfgifteSelectieCriteriaType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20130701.BestandKenmerkenType;
import nl.kadaster.schemas.gds2.afgifte_proces.v20130701.FilterDatumTijdType;
import nl.kadaster.schemas.gds2.service.afgifte.v20130701.Gds2AfgifteServiceV20130701;
import nl.kadaster.schemas.gds2.service.afgifte.v20130701.Gds2AfgifteServiceV20130701Service;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstgbopvragen.v20130701.BestandenlijstGBOpvragenRequest;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstgbopvragen.v20130701.BestandenlijstGBOpvragenResponse;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20130701.BestandenlijstOpvragenRequest;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20130701.BestandenlijstOpvragenResponse;
import nl.logius.digikoppeling.gb._2010._10.DataReference;

/**
 *
 * @author Matthijs Laan
 * @author mprins
 */
public class Main {

    private static final int BESTANDENLIJST_ATTEMPTS = 5;
    private static final int BESTANDENLIJST_RETRY_WAIT = 10000;

    public static void main(String[] args) throws Exception {
        // java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        // java.lang.System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");
        // java.lang.System.setProperty("javax.net.debug", "ssl,plaintext");
        Gds2AfgifteServiceV20130701 gds2 = new Gds2AfgifteServiceV20130701Service().getAGds2AfgifteServiceV20130701();
        BindingProvider bp = (BindingProvider) gds2;
        Map<String, Object> ctxt = bp.getRequestContext();
        String endpoint = (String) ctxt.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        System.out.println("Origineel endpoint: " + endpoint);

        //ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,  "http://localhost:8088/AfgifteService");
        //System.out.println("Endpoint protocol gewijzigd naar mock");
        final char[] thePassword = "changeit".toCharArray();
        System.out.println("Loading keystore");
        KeyStore ks = KeyStore.getInstance("jks");

        ks.load(Main.class.getResourceAsStream("/pkioverheid.jks"), thePassword);
        System.out.println("Initializing TrustManagerFactory");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ks);

        System.out.println("Initializing KeyManagerFactory");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        ks = KeyStore.getInstance("jks");

        if (args.length > 0) {
            String keystore = "/gds2_key.jks";
            ks.load(Main.class.getResourceAsStream(keystore), thePassword);
            kmf.init(ks, thePassword);
        } else {
            ks.load(null);
            PrivateKey privateKey = GDS2Util.getPrivateKeyFromPEM(new String(Files.readAllBytes(Paths.get("private.key"))).trim());
            Certificate certificate = GDS2Util.getCertificateFromPEM(new String(Files.readAllBytes(Paths.get("public.key"))).trim());
            ks.setKeyEntry("thekey", privateKey, thePassword, new Certificate[]{certificate});
            kmf.init(ks, thePassword);
        }
        System.out.println("Initializing SSLContext");
        SSLContext context = SSLContext.getInstance("TLS", "SunJSSE");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLContext.setDefault(context);
        ctxt.put(JAXWSProperties.SSL_SOCKET_FACTORY, context.getSocketFactory());

        BestandenlijstOpvragenRequest request = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType verzoek = new BestandenlijstOpvragenType();
        request.setVerzoek(verzoek);
        AfgifteSelectieCriteriaType criteria = new AfgifteSelectieCriteriaType();
        verzoek.setAfgifteSelectieCriteria(criteria);
        criteria.setBestandKenmerken(new BestandKenmerkenType());
        // alGerapporteerd
        boolean alGerapporteerd = false;
        criteria.setNogNietGerapporteerd(alGerapporteerd);

        // maandelijkse mutaties NL
        // criteria.getBestandKenmerken().setArtikelnummer("2508");
        // dagelijkse mutaties NL
        // criteria.getBestandKenmerken().setArtikelnummer("2516");
        // criteria.getBestandKenmerken().setArtikelnummer("2522");
        // soms met voorloopnullen
        // criteria.getBestandKenmerken().setArtikelnummer("0002522");
        // contractnummer
        // criteria.getBestandKenmerken().setContractnummer("0005014500");
        criteria.getBestandKenmerken().setContractnummer("9700004549");
        // vanaf 1 jan 2018
        GregorianCalendar vanaf = new GregorianCalendar(2018, (1 - 1) /* GregorianCalendar heeft 0-based month */, 1);
        GregorianCalendar tot = new GregorianCalendar();
        // tot vandaag
        tot.setTime(new Date());
        // tot 1 feb 2018
        tot = new GregorianCalendar(2018, 2 - 1, 1);

        System.out.println("Contract nummer: " + criteria.getBestandKenmerken().getContractnummer());
        System.out.println("Artikel nummer: " + criteria.getBestandKenmerken().getArtikelnummer());
        System.out.println("DatumTijdVanaf criterium: " + vanaf.getTime());
        System.out.println("DatumTijdTot criterium: " + tot.getTime());
        System.out.println("alGerapporteerd criterium: " + alGerapporteerd);

        criteria.setPeriode(new FilterDatumTijdType());
        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(vanaf);
        criteria.getPeriode().setDatumTijdVanaf(date);

        BestandenlijstGBOpvragenRequest requestGb = new BestandenlijstGBOpvragenRequest();
        BestandenlijstGbOpvragenType verzoekGb = new BestandenlijstGbOpvragenType();
        requestGb.setVerzoek(verzoekGb);
        verzoekGb.setAfgifteSelectieCriteria(criteria);

        GregorianCalendar currentMoment = null;
        boolean parseblePeriod = false;
        int loopType = Calendar.DAY_OF_MONTH;
        int loopMax = 180;
        int loopNum = 0;
        boolean reducePeriod = false;
        boolean increasePeriod = false;

        if (vanaf != null && tot != null && vanaf.before(tot)) {
            parseblePeriod = true;
            currentMoment = vanaf;
        }

        List<AfgifteGBType> afgiftesGb = new ArrayList<>();
        try {
            boolean morePeriods2Process = false;
            do /* while morePeriods2Process is true */ {
                System.out.println("\n*** start periode ***");
                //zet periode in criteria indien gewenst
                if (parseblePeriod) {
                    //check of de periodeduur verkleind moet worden
                    if (reducePeriod) {
                        switch (loopType) {
                            case Calendar.DAY_OF_MONTH:
                                currentMoment.add(loopType, -1);
                                loopType = Calendar.HOUR_OF_DAY;
                                System.out.println("* Verklein loop periode naar uur");

                                break;
                            case Calendar.HOUR_OF_DAY:
                                currentMoment.add(loopType, -1);
                                loopType = Calendar.MINUTE;
                                System.out.println("* Verklein loop periode naar minuut");

                                break;
                            case Calendar.MINUTE:
                            default:
                                /*
                                 * Hier kom je alleen als binnen een minuut meer
                                 * dan 2000 berichten zijn aangamaakt en het
                                 * vinkje "al rapporteerde berichten
                                 * ophalen" ook staat aan.
                                 */
                                System.out.println("Niet alle gevraagde berichten zijn opgehaald");
                        }
                        reducePeriod = false;
                    }

                    //check of de periodeduur vergroot moet worden
                    if (increasePeriod) {
                        switch (loopType) {
                            case Calendar.HOUR_OF_DAY:
                                loopType = Calendar.DAY_OF_MONTH;
                                System.out.println("* Vergroot loop periode naar dag");

                                break;
                            case Calendar.MINUTE:
                                loopType = Calendar.HOUR_OF_DAY;
                                System.out.println("* Vergroot loop periode naar uur");

                                break;
                            case Calendar.DAY_OF_MONTH:
                            default:
                            //not possible
                        }
                        increasePeriod = false;
                    }

                    FilterDatumTijdType d = new FilterDatumTijdType();
                    d.setDatumTijdVanaf(DatatypeFactory.newInstance().newXMLGregorianCalendar(currentMoment));
                    System.out.println(String.format("Datum vanaf: %tc", currentMoment.getTime()));

                    currentMoment.add(loopType, 1);
                    d.setDatumTijdTot(DatatypeFactory.newInstance().newXMLGregorianCalendar(currentMoment));
                    System.out.println(String.format("Datum tot: %tc", currentMoment.getTime()));

                    criteria.setPeriode(d);

                    switch (loopType) {
                        case Calendar.HOUR_OF_DAY:
                            //0-23
                            if (currentMoment.get(loopType) == 0) {
                                increasePeriod = true;
                            }
                            break;
                        case Calendar.MINUTE:
                            //0-59
                            if (currentMoment.get(loopType) == 0) {
                                increasePeriod = true;
                            }
                            break;
                        case Calendar.DAY_OF_MONTH:
                        default:
                            //alleen dagen tellen, uur en minuut altijd helemaal
                            loopNum++;
                    }

                    //bereken of einde van periode bereikt is
                    if (currentMoment.before(tot) && loopNum < loopMax) {
                        morePeriods2Process = true;
                    } else {
                        morePeriods2Process = false;
                    }
                }

                verzoekGb.setAfgifteSelectieCriteria(criteria);
                BestandenlijstGBOpvragenResponse responseGb = retryBestandenLijstGBOpvragen(gds2, requestGb);

                int aantalInAntwoord = responseGb.getAntwoord().getBestandenLijstGB().getAfgifteGB().size();
                System.out.println("Aantal in antwoord: " + aantalInAntwoord);

                // opletten; in de xsd staat een default value van 'J' voor meerAfgiftesbeschikbaar
                boolean hasMore = responseGb.getAntwoord().getMeerAfgiftesbeschikbaar().equalsIgnoreCase("true");
                System.out.println("Meer afgiftes beschikbaar: " + hasMore);


                /*
                 * Als "al gerapporteerde berichten" moeten worden opgehaald en
                 * er zitten dan 2000 berichten in het antwoord dan heeft het
                 * geen zin om meer keer de berichten op te halen, je krijgt
                 * telkens dezelfde.
                 */
                if (hasMore && alGerapporteerd) {
                    reducePeriod = true;
                    morePeriods2Process = true;
                    increasePeriod = false;
                    // als er geen parsable periode is
                    // (geen periode ingevuld en alGerapporteerd is true
                    // dan moet morePeriods2Process false worden om een
                    // eindloze while loop te voorkomen
                    if (!parseblePeriod) {
                        morePeriods2Process = false;
                    } else {
                        continue;
                    }
                }

                afgiftesGb.addAll(responseGb.getAntwoord().getBestandenLijstGB().getAfgifteGB());

                /*
                 * Indicatie nog niet gerapporteerd: Met deze indicatie wordt
                 * aangegeven of uitsluitend de nog niet gerapporteerde
                 * bestanden moeten worden opgenomen in de lijst, of dat alle
                 * beschikbare bestanden worden genoemd. Niet gerapporteerd
                 * betekent in dit geval ‘niet eerder opgevraagd in deze
                 * bestandenlijst’. Als deze indicator wordt gebruikt, dan
                 * worden na terugmelding van de bestandenlijst de bijbehorende
                 * bestanden gemarkeerd als zijnde ‘gerapporteerd’ in het
                 * systeem van GDS.
                 */
                boolean dontGetMoreConfig = false;
                while (hasMore && !dontGetMoreConfig) {
                    criteria.setNogNietGerapporteerd(true);
                    responseGb = retryBestandenLijstGBOpvragen(gds2, requestGb);

                    List<AfgifteGBType> afgiftes = responseGb.getAntwoord().getBestandenLijstGB().getAfgifteGB();
                    afgiftesGb.addAll(afgiftes);
                    aantalInAntwoord = afgiftes.size();
                    System.out.println("Aantal in antwoord: " + aantalInAntwoord);

                    hasMore = responseGb.getAntwoord().getMeerAfgiftesbeschikbaar().equalsIgnoreCase("true");
                    System.out.println("Nog meer afgiftes beschikbaar: " + hasMore);
                }
            } while (morePeriods2Process);
            System.out.println("Totaal aantal op te halen berichten: " + afgiftesGb.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
        verwerkAfgiftesGb(afgiftesGb);

        System.out.println("\n\n**** resultaat ****\n");
        System.out.println("Aantal afgiftes grote bestanden: " + (afgiftesGb == null ? "<fout>" : afgiftesGb.size()));

        List<AfgifteType> afgiftes = null;
        try {
            BestandenlijstOpvragenResponse response = gds2.bestandenlijstOpvragen(request);
            afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Aantal afgiftes: " + (afgiftes == null ? "<fout>" : afgiftes.size()));
    }

    private static void verwerkAfgiftesGb(List<AfgifteGBType> afgiftesGb) {
        System.out.println("Afgiftegegevens, bestandskenmerken en Digikoppeling datareference gegevens van de bestandenlijst.");
        // tab gescheiden output, of kvp
        System.out.println("ID\treferentie\tbestandsnaam\tbestandref\tbeschikbaarTot\tcontractnr\tartikelnr\tartikelomschrijving\tcontextId\tcreationTime\texpirationTime\tfilename\tchecksum\ttype\tsize\tsenderUrl\treceiverUrl");
        for (AfgifteGBType a : afgiftesGb) {
            // String kenmerken = "(geen)";
            String kenmerken = "-\t-\t-";
            if (a.getBestandKenmerken() != null) {
                // kenmerken = String.format("contractnr: %s, artikelnr: %s, artikelomschrijving: %s",
                kenmerken = String.format("%s\t%s\t%s",
                        a.getBestandKenmerken().getContractnummer(),
                        a.getBestandKenmerken().getArtikelnummer(),
                        a.getBestandKenmerken().getArtikelomschrijving());
            }
            // System.out.printf("ID: %s, referentie: %s, bestandsnaam: %s, bestandref: %s, beschikbaarTot: %s, kenmerken: %s",
            System.out.printf("%s\t%s\t%s\t%s\t%s\t%s",
                    a.getAfgifteID(),
                    a.getAfgiftereferentie(),
                    a.getBestand().getBestandsnaam(),
                    a.getBestand().getBestandsreferentie(),
                    a.getBeschikbaarTot(),
                    kenmerken);
            if (a.getDigikoppelingExternalDatareferences() != null
                    && a.getDigikoppelingExternalDatareferences().getDataReference() != null) {
                for (DataReference dr : a.getDigikoppelingExternalDatareferences().getDataReference()) {
                    // System.out.printf("   Digikoppeling datareference: contextId: %s, creationTime: %s, expirationTime: %s, filename: %s, checksum: %s, size: %d, type: %s, senderUrl: %s, receiverUrl: %s\n",
                    System.out.printf("\t%s\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\n",
                            dr.getContextId(),
                            dr.getLifetime().getCreationTime().getValue(),
                            dr.getLifetime().getExpirationTime().getValue(),
                            dr.getContent().getFilename(),
                            dr.getContent().getChecksum().getValue(),
                            dr.getContent().getSize(),
                            dr.getContent().getContentType(),
                            dr.getTransport().getLocation().getSenderUrl() == null ? "-" : dr.getTransport().getLocation().getSenderUrl().getValue(),
                            dr.getTransport().getLocation().getReceiverUrl() == null ? "-" : dr.getTransport().getLocation().getReceiverUrl().getValue());
                }
            }
        }
    }

    private static BestandenlijstGBOpvragenResponse retryBestandenLijstGBOpvragen(Gds2AfgifteServiceV20130701 gds2, BestandenlijstGBOpvragenRequest requestGb) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return gds2.bestandenlijstGBOpvragen(requestGb);
            } catch (Exception e) {
                attempt++;
                if (attempt == BESTANDENLIJST_ATTEMPTS) {
                    System.out.println("Fout bij laatste poging ophalen bestandenlijst: " + e.getClass().getName() + ": " + e.getMessage());
                    throw e;
                } else {
                    System.out.println("Fout bij poging " + attempt + " om bestandenlijst op te halen: " + e.getClass().getName() + ": " + e.getMessage());
                    Thread.sleep(BESTANDENLIJST_RETRY_WAIT);
                    System.out.println("Uitvoeren poging " + (attempt + 1) + " om bestandenlijst op te halen...");
                }
            }
        }
    }
}
