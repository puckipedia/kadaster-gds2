
package nl.b3p.gds2;

import com.sun.xml.ws.developer.JAXWSProperties;
import java.security.KeyStore;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
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
 */
public class Main {

    public static void main(String[] args) throws Exception {

        //java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        //java.lang.System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");
        //java.lang.System.setProperty("javax.net.debug", "ssl,plaintext");

        Gds2AfgifteServiceV20130701 gds2 = new Gds2AfgifteServiceV20130701Service().getAGds2AfgifteServiceV20130701();

        BestandenlijstOpvragenRequest request = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType verzoek = new BestandenlijstOpvragenType();
        request.setVerzoek(verzoek);
        AfgifteSelectieCriteriaType criteria = new AfgifteSelectieCriteriaType();

        criteria.setPeriode(new FilterDatumTijdType());
        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar(2017, 1, 1));
        //criteria.getPeriode().setDatumTijdVanaf(date);

        //criteria.getBestandKenmerken().setArtikelnummer("2508");
        //criteria.getBestandKenmerken().setContractnummer("");
        criteria.setBestandKenmerken(new BestandKenmerkenType());

        verzoek.setAfgifteSelectieCriteria(criteria);

        BestandenlijstGBOpvragenRequest requestGb = new BestandenlijstGBOpvragenRequest();
        BestandenlijstGbOpvragenType verzoekGb = new BestandenlijstGbOpvragenType();
        requestGb.setVerzoek(verzoekGb);
        verzoekGb.setAfgifteSelectieCriteria(criteria);

        BindingProvider bp = (BindingProvider)gds2;

        Map<String, Object> ctxt = bp.getRequestContext();

        String keystore = "/gds2_key.jks";

        String endpoint = (String)ctxt.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        System.out.println("Origineel endpoint: " + endpoint);

        //ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,  "http://localhost:8088/AfgifteService");
        //System.out.println("Endpoint protocol gewijzigd naar mock");

        System.out.println("Loading keystore");
        KeyStore ks = KeyStore.getInstance("jks");

        ks.load(Main.class.getResourceAsStream("/pkioverheid.jks"), "changeit".toCharArray());

        System.out.println("Initializing TrustManagerFactory");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ks);

        System.out.println("Initializing KeyManagerFactory");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        ks = KeyStore.getInstance("jks");
        ks.load(Main.class.getResourceAsStream(keystore), "changeit".toCharArray());
        kmf.init(ks, "changeit".toCharArray());

        System.out.println("Initializing SSLContext");
        SSLContext context = SSLContext.getInstance("TLS", "SunJSSE");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLContext.setDefault(context);
        ctxt.put(JAXWSProperties.SSL_SOCKET_FACTORY, context.getSocketFactory());

        List<AfgifteType> afgiftes = null;
        List<AfgifteGBType> afgiftesGb = null;

        try {
            BestandenlijstGBOpvragenResponse responseGb = gds2.bestandenlijstGBOpvragen(requestGb);

            afgiftesGb = responseGb.getAntwoord().getBestandenLijstGB().getAfgifteGB();
            for(AfgifteGBType a: afgiftesGb) {
                String kenmerken = "(geen)";
                if(a.getBestandKenmerken() != null) {
                    kenmerken = String.format("contractnr: %s, artikelnr: %s, artikelomschrijving: %s",
                            a.getBestandKenmerken().getContractnummer(),
                            a.getBestandKenmerken().getArtikelnummer(),
                            a.getBestandKenmerken().getArtikelomschrijving());
                }
                System.out.printf("ID: %s, referentie: %s, bestandsnaam: %s, bestandref: %s, beschikbaarTot: %s, kenmerken: %s\n",
                        a.getAfgifteID(),
                        a.getAfgiftereferentie(),
                        a.getBestand().getBestandsnaam(),
                        a.getBestand().getBestandsreferentie(),
                        a.getBeschikbaarTot(),
                        kenmerken);
                if(a.getDigikoppelingExternalDatareferences() != null
                        && a.getDigikoppelingExternalDatareferences().getDataReference() != null) {
                    for(DataReference dr: a.getDigikoppelingExternalDatareferences().getDataReference()) {
                        System.out.println(dr.getTransport().getLocation().getSenderUrl().getValue());
                        System.out.printf("   Digikoppeling datareference: contextId: %s, creationTime: %s, expirationTime: %s, filename: %s, checksum: %s, size: %d, type: %s, senderUrl: %s, receiverUrl: %s\n",
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

            System.out.println("Meer afgiftes beschikbaar: " + responseGb.getAntwoord().getMeerAfgiftesbeschikbaar());
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            BestandenlijstOpvragenResponse response = gds2.bestandenlijstOpvragen(request);

            afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n\n**** resultaat ****\n");
        System.out.println("Aantal afgiftes: " + (afgiftes == null ? "<fout>" :  afgiftes.size()));
        System.out.println("Aantal afgiftes grote bestanden: " + (afgiftesGb == null ? "<fout>" : afgiftesGb.size()));

    }
}
