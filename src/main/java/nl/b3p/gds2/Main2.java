/*
 * Copyright (C) 2018 B3Partners B.V.
 */
package nl.b3p.gds2;

import com.sun.xml.ws.developer.JAXWSProperties;
import nl.b3p.soap.logging.LogMessageHandler;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstresultaat.afgifte.v20170401.AfgifteType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstresultaat.afgifte.v20170401.BestandenLijstType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20170401.AfgifteSelectieCriteriaType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20170401.BestandKenmerkenType;
import nl.kadaster.schemas.gds2.afgifte_proces.v20170401.FilterDatumTijdType;
import nl.kadaster.schemas.gds2.afgifte_proces.v20170401.KlantAfgiftenummerReeksType;
import nl.kadaster.schemas.gds2.imgds.baseurl.v20170401.BaseURLType;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401Service;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenRequest;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenResponse;
import nl.logius.digikoppeling.gb._2010._10.DataReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static nl.b3p.gds2.GDS2Util.*;


/**
 * Test klasse om gds2 te bevragen.
 * <p>
 * quick run:
 * <code>
 * cp some.private.key private.key && cp some.public.key public.key
 * mvn clean install -DskipTests
 * java -Dlog4j.configuration=file:///home/mark/dev/projects/kadaster-gds2/target/test-classes/log4j.xml -cp ./target/kadaster-gds2-2.0-SNAPSHOT.jar:./target/lib/* nl.b3p.gds2.Main2
 * <code/>
 *
 * @author mprins
 */
public class Main2 {
    private static final Log LOG = LogFactory.getLog(Main2.class);
    private static final String MOCK_ENDPOINT = "http://localhost:8088/AfgifteService";

    public static void main(String[] args) throws Exception {
        // java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        // java.lang.System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");
        // java.lang.System.setProperty("javax.net.debug", "ssl,plaintext");
        Gds2AfgifteServiceV20170401 gds2 = new Gds2AfgifteServiceV20170401Service().getAGds2AfgifteServiceV20170401();
        BindingProvider bp = (BindingProvider) gds2;
        Map<String, Object> ctxt = bp.getRequestContext();

        // soap berichten logger inhaken (actief met TRACE level)
        List<Handler> handlerChain = bp.getBinding().getHandlerChain();
        handlerChain.add(new LogMessageHandler());
        bp.getBinding().setHandlerChain(handlerChain);

        String endpoint = (String) ctxt.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        LOG.info("Origineel endpoint: " + endpoint);
        // om tegen soapui mock te praten
        // ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, MOCK_ENDPOINT);
        // LOG.info("Endpoint protocol gewijzigd naar mock: " + MOCK_ENDPOINT);

        final char[] thePassword = "changeit".toCharArray();
        LOG.debug("Loading keystore");
        KeyStore ks = KeyStore.getInstance("jks");

        ks.load(Main2.class.getResourceAsStream("/pkioverheid.jks"), thePassword);
        LOG.debug("Initializing TrustManagerFactory");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ks);

        LOG.debug("Initializing KeyManagerFactory");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        ks = KeyStore.getInstance("jks");

        if (args.length > 0) {
            String keystore = "/gds2_key.jks";
            ks.load(Main2.class.getResourceAsStream(keystore), thePassword);
            kmf.init(ks, thePassword);
        } else {
            ks.load(null);
            PrivateKey privateKey = GDS2Util.getPrivateKeyFromPEM(new String(Files.readAllBytes(Paths.get("private.key"))).trim());
            Certificate certificate = GDS2Util.getCertificateFromPEM(new String(Files.readAllBytes(Paths.get("public.key"))).trim());
            ks.setKeyEntry("thekey", privateKey, thePassword, new Certificate[]{certificate});
            kmf.init(ks, thePassword);
        }
        LOG.debug("Initializing SSLContext");
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

        // Indicatie nog niet gerapporteerd
        // Met deze indicatie wordt aangegeven of uitsluitend de nog niet gerapporteerde afgiftes
        // moeten worden opgenomen in de lijst, of dat alle beschikbare afgiftes worden genoemd.
        // Als deze indicator wordt gebruikt, dan worden na terugmelding van de bestandenlijst
        // de bijbehorende bestanden gemarkeerd als zijnde ‘gerapporteerd’ in het systeem van GDS.
        // alGerapporteerd
        final Boolean alGerapporteerd = Boolean.FALSE;
        criteria.setNogNietGerapporteerd(alGerapporteerd);

        // vanaf 1 jan 2018
        XMLGregorianCalendar vanaf = getXMLDatumTijd(2018, 1, 1);
        XMLGregorianCalendar tot;
        // tot vandaag
        tot = getXMLDatumTijd(new Date());
        // tot 1 aug 2019
        // tot = getXMLDatumTijd(2019, 9, 1);

        // om bepaalde periode te selecteren; kan/mag niet samen met afgiftenummers
        criteria.setPeriode(new FilterDatumTijdType());
        // Indien vermeld worden alleen de afgiftes opgenomen in de lijst met een
        // aanmeldingstijdstip NA genoemde datum/tijd.
        criteria.getPeriode().setDatumTijdVanaf(vanaf);
        // Idem, met een aanmeldingstijdstip TOT en MET genoemde datum/tijd
        // (tijdstip op seconde nauwkeurig).
        criteria.getPeriode().setDatumTijdTotmet(tot);

        // om bepaalde afgiftenummers te selecteren; kan/mag niet samen met periode
        KlantAfgiftenummerReeksType afgiftenummers = new KlantAfgiftenummerReeksType();

        // Indien vermeld worden alleen afgiftes opgenomen in de lijst met een klantAfgiftenummer
        // groter of gelijk aan het genoemde nummer
        afgiftenummers.setKlantAfgiftenummerVanaf(BigInteger.ONE);
        // Indien vermeld worden alleen afgiftes opgenomen in de lijst met een klantAfgiftenummer
        // kleiner of gelijk aan het genoemde nummer.
        // Deze bovengrens is alleen mogelijk in combinatie met een ondergrens (klantAfgiftenummer vanaf).
        afgiftenummers.setKlantAfgiftenummerTotmet(BigInteger.valueOf(10000));
//        criteria.setKlantAfgiftenummerReeks(afgiftenummers);

        //
        // artikelnummer
        // Indien vermeld dan worden alleen de afgiftes opgenomen in de lijst die
        // horen bij het genoemde artikelnummer
        //
        // maandelijkse BAG mutaties NL
        // criteria.getBestandKenmerken().setArtikelnummer("2508");
        // dagelijkse BAG mutaties NL
        criteria.getBestandKenmerken().setArtikelnummer("2516");
        //
        // contractnummer voor BRK
        //
        // Indien vermeld dan worden alleen de afgiftes opgenomen in de lijst die
        // gekoppeld zijn aan het genoemde contractnummer
        //
        // criteria.getBestandKenmerken().setContractnummer("9700005117");

        LOG.info("Contract nummer: " + criteria.getBestandKenmerken().getContractnummer());
        LOG.info("Artikel nummer: " + criteria.getBestandKenmerken().getArtikelnummer());
        LOG.info("DatumTijdVanaf criterium: " + vanaf.toGregorianCalendar().getTime());
        LOG.info("DatumTijdTot criterium: " + tot.toGregorianCalendar().getTime());
        LOG.info("alGerapporteerd criterium: " + alGerapporteerd);

        BestandenlijstOpvragenRequest bestandenlijstOpvragenRequest = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType bestandenlijstOpvragenVerzoek = new BestandenlijstOpvragenType();
        bestandenlijstOpvragenRequest.setVerzoek(bestandenlijstOpvragenVerzoek);
        bestandenlijstOpvragenVerzoek.setAfgifteSelectieCriteria(criteria);

        BestandenlijstOpvragenResponse bestandenlijstOpvragenResponse = retryBestandenLijstOpvragen(
                gds2, bestandenlijstOpvragenRequest, 1, 10000
        );

        // basis meta info van antwoord
        int afgifteAantalInLijst = bestandenlijstOpvragenResponse.getAntwoord().getAfgifteAantalInLijst();
        LOG.info("Aantal geselecteerde afgiftes in de lijst: " + afgifteAantalInLijst);
        int hoogsteAfgifteNummer = bestandenlijstOpvragenResponse.getAntwoord().getKlantAfgiftenummerMax();
        LOG.info("Hoogst toegekende klant afgiftenummer:" + hoogsteAfgifteNummer);
        BaseURLType baseUrlAnon = getAnoniemBaseURL(bestandenlijstOpvragenResponse.getAntwoord());
        BaseURLType baseUrlCert = getCertificaatBaseURL(bestandenlijstOpvragenResponse.getAntwoord());
        LOG.info("Baseurl te gebruiken als prefix voor anon download urls: " + baseUrlAnon.getValue());
        LOG.info("Baseurl te gebruiken als prefix voor anon certificaat urls: " + baseUrlCert.getValue());
        // true als er meer dan het maximum aantal afgiftes zijn gevonden _voor de selectie criteria_
        boolean hasMore = bestandenlijstOpvragenResponse.getAntwoord().isMeerAfgiftesbeschikbaar();
        LOG.info("Meer afgiftes beschikbaar? " + hasMore);

        // afgiftes
        BestandenLijstType bestandenLijst = bestandenlijstOpvragenResponse.getAntwoord().getBestandenLijst();
        if (criteria.getBestandKenmerken().getContractnummer() == null) {
            // bag
            printAfgiftes(bestandenLijst.getAfgifte(), baseUrlAnon);
        } else {
            // brk
            printAfgiftes(bestandenLijst.getAfgifte(), baseUrlCert);
        }

        // TODO logica om datum periode of nummerreeks kleiner te maken dan max aantal

//        GregorianCalendar currentMoment = null;
//        boolean parseblePeriod = false;
//        int loopType = Calendar.DAY_OF_MONTH;
//        int loopMax = 180;
//        int loopNum = 0;
//        boolean reducePeriod = false;
//        boolean increasePeriod = false;

//        if (vanaf != null && tot != null && vanaf.before(tot)) {
//            parseblePeriod = true;
//            currentMoment = vanaf;
//        }
//
//        List<AfgifteType> afgiftesGb = new ArrayList<>();
//        try {
//            boolean morePeriods2Process = false;
//            do /* while morePeriods2Process is true */ {
//                System.out.println("\n*** start periode ***");
//                // zet periode in criteria indien gewenst
//                if (parseblePeriod) {
//                    //check of de periodeduur verkleind moet worden
//                    if (reducePeriod) {
//                        switch (loopType) {
//                            case Calendar.DAY_OF_MONTH:
//                                currentMoment.add(loopType, -1);
//                                loopType = Calendar.HOUR_OF_DAY;
//                                System.out.println("* Verklein loop periode naar uur");
//
//                                break;
//                            case Calendar.HOUR_OF_DAY:
//                                currentMoment.add(loopType, -1);
//                                loopType = Calendar.MINUTE;
//                                System.out.println("* Verklein loop periode naar minuut");
//
//                                break;
//                            case Calendar.MINUTE:
//                            default:
//                                /*
//                                 * Hier kom je alleen als binnen een minuut meer
//                                 * dan 2000 berichten zijn aangamaakt en het
//                                 * vinkje "al rapporteerde berichten
//                                 * ophalen" ook staat aan.
//                                 */
//                                System.out.println("Niet alle gevraagde berichten zijn opgehaald");
//                        }
//                        reducePeriod = false;
//                    }
//
//                    //check of de periodeduur vergroot moet worden
//                    if (increasePeriod) {
//                        switch (loopType) {
//                            case Calendar.HOUR_OF_DAY:
//                                loopType = Calendar.DAY_OF_MONTH;
//                                System.out.println("* Vergroot loop periode naar dag");
//
//                                break;
//                            case Calendar.MINUTE:
//                                loopType = Calendar.HOUR_OF_DAY;
//                                System.out.println("* Vergroot loop periode naar uur");
//
//                                break;
//                            case Calendar.DAY_OF_MONTH:
//                            default:
//                                //not possible
//                        }
//                        increasePeriod = false;
//                    }
//
//                    FilterDatumTijdType d = new FilterDatumTijdType();
//                    d.setDatumTijdVanaf(DatatypeFactory.newInstance().newXMLGregorianCalendar(currentMoment));
//                    System.out.println(String.format("Datum vanaf: %tc", currentMoment.getTime()));
//
//                    currentMoment.add(loopType, 1);
//                    d.setDatumTijdTotmet((DatatypeFactory.newInstance().newXMLGregorianCalendar(currentMoment)));
//                    System.out.println(String.format("Datum tot: %tc", currentMoment.getTime()));
//
//                    criteria.setPeriode(d);
//
//                    switch (loopType) {
//                        case Calendar.HOUR_OF_DAY:
//                            //0-23
//                            if (currentMoment.get(loopType) == 0) {
//                                increasePeriod = true;
//                            }
//                            break;
//                        case Calendar.MINUTE:
//                            //0-59
//                            if (currentMoment.get(loopType) == 0) {
//                                increasePeriod = true;
//                            }
//                            break;
//                        case Calendar.DAY_OF_MONTH:
//                        default:
//                            //alleen dagen tellen, uur en minuut altijd helemaal
//                            loopNum++;
//                    }
//
//                    //bereken of einde van periode bereikt is
//                    if (currentMoment.before(tot) && loopNum < loopMax) {
//                        morePeriods2Process = true;
//                    } else {
//                        morePeriods2Process = false;
//                    }
//                }
//
//                verzoekGb.setAfgifteSelectieCriteria(criteria);
//                BestandenlijstOpvragenResponse responseGb = retryBestandenLijstGBOpvragen(gds2, requestGb);
//
//                int aantalInAntwoord = responseGb.getAntwoord().getBestandenLijst().getAfgifte().size();
//                System.out.println("Aantal in antwoord: " + aantalInAntwoord);
//
//                // opletten; in de xsd staat een default value van 'J' voor meerAfgiftesbeschikbaar
//                boolean hasMore = responseGb.getAntwoord().isMeerAfgiftesbeschikbaar();
//                System.out.println("Meer afgiftes beschikbaar: " + hasMore);
//
//
//                /*
//                 * Als "al gerapporteerde berichten" moeten worden opgehaald en
//                 * er zitten dan 2000 berichten in het antwoord dan heeft het
//                 * geen zin om meer keer de berichten op te halen, je krijgt
//                 * telkens dezelfde.
//                 */
//                if (hasMore && alGerapporteerd) {
//                    reducePeriod = true;
//                    morePeriods2Process = true;
//                    increasePeriod = false;
//                    // als er geen parsable periode is
//                    // (geen periode ingevuld en alGerapporteerd is true
//                    // dan moet morePeriods2Process false worden om een
//                    // eindloze while loop te voorkomen
//                    if (!parseblePeriod) {
//                        morePeriods2Process = false;
//                    } else {
//                        continue;
//                    }
//                }
//
//                afgiftesGb.addAll(responseGb.getAntwoord().getBestandenLijst().getAfgifte());
//
//                /*
//                 * Indicatie nog niet gerapporteerd: Met deze indicatie wordt
//                 * aangegeven of uitsluitend de nog niet gerapporteerde
//                 * bestanden moeten worden opgenomen in de lijst, of dat alle
//                 * beschikbare bestanden worden genoemd. Niet gerapporteerd
//                 * betekent in dit geval ‘niet eerder opgevraagd in deze
//                 * bestandenlijst’. Als deze indicator wordt gebruikt, dan
//                 * worden na terugmelding van de bestandenlijst de bijbehorende
//                 * bestanden gemarkeerd als zijnde ‘gerapporteerd’ in het
//                 * systeem van GDS.
//                 */
//                boolean dontGetMoreConfig = false;
//                while (hasMore && !dontGetMoreConfig) {
//                    criteria.setNogNietGerapporteerd(true);
//                    responseGb = retryBestandenLijstGBOpvragen(gds2, requestGb);
//
//                    List<AfgifteType> afgiftes = responseGb.getAntwoord().getBestandenLijst().getAfgifte();
//                    afgiftesGb.addAll(afgiftes);
//                    aantalInAntwoord = afgiftes.size();
//                    System.out.println("Aantal in antwoord: " + aantalInAntwoord);
//
//                    hasMore = responseGb.getAntwoord().isMeerAfgiftesbeschikbaar();
//                    System.out.println("Nog meer afgiftes beschikbaar: " + hasMore);
//                }
//            } while (morePeriods2Process);
//            System.out.println("Totaal aantal op te halen berichten: " + afgiftesGb.size());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


//        System.out.println("\n\n**** resultaat ****\n");
//        System.out.println("Aantal afgiftes : " + (afgiftesGb == null ? "<fout>" : afgiftesGb.size()));
//
//        List<AfgifteType> afgiftes = null;
//        try {
//            BestandenlijstOpvragenResponse response = gds2.bestandenlijstOpvragen(request);
//            afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println("Aantal afgiftes: " + (afgiftes == null ? "<fout>" : afgiftes.size()));
    }

    private static void printAfgiftes(List<AfgifteType> afgiftes, BaseURLType baseUrl) {
        LOG.info("Schrijf afgiftegegevens, bestandskenmerken en Digikoppeling datareference gegevens van de afgiftes in CSV.");
        try (PrintWriter out = new PrintWriter("afgiftelijst.csv")) {
            out.println("ID\treferentie\tdownloadUrl\tbestandsnaam\tbestandref\tbeschikbaarTot\tcontractafgiftenr\tklantafgiftenr\tcontractnr\tartikelnr\tartikelomschrijving\tcontextId\tcreationTime\texpirationTime\tfilename\tchecksum\ttype\tsize\tsenderUrl\treceiverUrl");
            for (AfgifteType a : afgiftes) {
                String kenmerken = "-\t-\t-";
                if (a.getBestandKenmerken() != null) {
                    kenmerken = String.format("%s\t%s\t%s",
                            a.getBestandKenmerken().getContractnummer(),
                            a.getBestandKenmerken().getArtikelnummer(),
                            a.getBestandKenmerken().getArtikelomschrijving()
                    );
                }
                LOG.debug("Afgifte id: " + a.getAfgifteID());
                out.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t",
                        a.getAfgifteID(),
                        a.getAfgiftereferentie(),
                        getAfgifteURL(a, baseUrl),
                        a.getBestand().getBestandsnaam(),
                        a.getBestand().getBestandsreferentie(),
                        a.getBeschikbaarTot(),
                        a.getContractAfgiftenummer(),
                        a.getKlantAfgiftenummer(),
                        kenmerken);

                if (a.getDigikoppelingExternalDatareferences() != null
                        && a.getDigikoppelingExternalDatareferences().getDataReference() != null) {
                    for (DataReference dr : a.getDigikoppelingExternalDatareferences().getDataReference()) {
                        out.printf("%s\t%s\t%s\t%s\t%s\t%s\t%d\t%s\t%s\n",
                                dr.getContextId() == null ? "-" : dr.getContextId(),
                                dr.getLifetime().getCreationTime().getValue(),
                                dr.getLifetime().getExpirationTime().getValue(),
                                dr.getContent().getFilename(),
                                dr.getContent().getChecksum().getValue(),
                                dr.getContent().getContentType(),
                                dr.getContent().getSize(),
                                dr.getTransport().getLocation().getSenderUrl() == null ? "-" : dr.getTransport().getLocation().getSenderUrl().getValue(),
                                dr.getTransport().getLocation().getReceiverUrl() == null ? "-" : dr.getTransport().getLocation().getReceiverUrl().getValue());
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOG.error("CSV maken is mislukt", e);
        }
    }
}
