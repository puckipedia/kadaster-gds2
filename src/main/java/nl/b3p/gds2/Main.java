
package nl.b3p.gds2;

import com.sun.xml.ws.developer.JAXWSProperties;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstopvragen.v20130701.BestandenlijstOpvragenType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstresultaat.afgifte.v20130701.AfgifteType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20130701.AfgifteSelectieCriteriaType;
import nl.kadaster.schemas.gds2.service.afgifte.v20130701.Gds2AfgifteServiceV20130701;
import nl.kadaster.schemas.gds2.service.afgifte.v20130701.Gds2AfgifteServiceV20130701Service;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20130701.BestandenlijstOpvragenRequest;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20130701.BestandenlijstOpvragenResponse;

/**
 *
 * @author Matthijs Laan
 */
public class Main {

    public static void main(String[] args) throws Exception {

        java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        java.lang.System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");
        java.lang.System.setProperty("javax.net.debug", "ssl,plaintext");

        Gds2AfgifteServiceV20130701 gds2 = new Gds2AfgifteServiceV20130701Service().getAGds2AfgifteServiceV20130701();

        BestandenlijstOpvragenRequest request = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType verzoek = new BestandenlijstOpvragenType();
        request.setVerzoek(verzoek);
        AfgifteSelectieCriteriaType criteria = new AfgifteSelectieCriteriaType();
        verzoek.setAfgifteSelectieCriteria(criteria);


        BindingProvider bp = (BindingProvider)gds2;

        Map<String, Object> ctxt = bp.getRequestContext();

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
        ks.load(Main.class.getResourceAsStream("/gds2_key.jks"), "changeit".toCharArray());
        kmf.init(ks, "changeit".toCharArray());

        System.out.println("Initializing SSLContext");
        SSLContext context = SSLContext.getInstance("TLS", "SunJSSE");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ctxt.put(JAXWSProperties.SSL_SOCKET_FACTORY, context.getSocketFactory());

        BestandenlijstOpvragenResponse response = gds2.bestandenlijstOpvragen(request);

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        System.out.println("Aantal afgiftes: " + afgiftes.size());
    }
}
