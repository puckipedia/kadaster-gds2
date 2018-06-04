/*
 * Copyright (C) 2018 B3Partners B.V.
 */
package nl.b3p.gds2;

import java.util.List;
import java.util.Map;
import javax.xml.ws.BindingProvider;

import nl.b3p.soap.logging.LogMessageHandler;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstopvragenresultaat.v20170401.BestandenlijstOpvragenResultaatType;
import nl.kadaster.schemas.gds2.afgifte_proces.v20170401.SorteringType;
import nl.kadaster.schemas.gds2.imgds.baseurl.v20170401.BaseURLType;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sun.xml.ws.developer.JAXWSProperties;

import java.io.IOException;
import java.math.BigInteger;
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
import javax.xml.ws.handler.Handler;

import nl.b3p.soap.logging.LogMessageHandler;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstresultaat.afgifte.v20170401.AfgifteType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20170401.AfgifteSelectieCriteriaType;
import nl.kadaster.schemas.gds2.afgifte_bestandenlijstselectie.v20170401.BestandKenmerkenType;
import nl.kadaster.schemas.gds2.afgifte_proces.v20170401.FilterDatumTijdType;
import nl.kadaster.schemas.gds2.afgifte_proces.v20170401.KlantAfgiftenummerReeksType;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401;
import nl.kadaster.schemas.gds2.service.afgifte.v20170401.Gds2AfgifteServiceV20170401Service;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenRequest;
import nl.kadaster.schemas.gds2.service.afgifte_bestandenlijstopvragen.v20170401.BestandenlijstOpvragenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Mark Prins
 */
public class GDS2MockTest {

    private static final String MOCK_ENDPOINT = "http://localhost:8088/AfgifteService";
    private static final Log LOG = LogFactory.getLog(GDS2MockTest.class);
    private Map<String, Object> ctxt;
    private Gds2AfgifteServiceV20170401 gds2;

    /**
     *
     */
    @BeforeEach
    public void initContext() {
        this.gds2 = new Gds2AfgifteServiceV20170401Service().getAGds2AfgifteServiceV20170401();
        BindingProvider bp = (BindingProvider) gds2;
        this.ctxt = bp.getRequestContext();
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, MOCK_ENDPOINT);
        LOG.info("Endpoint protocol gewijzigd naar mock: " + MOCK_ENDPOINT);

        // soap berichten logger inhaken (actief met TRACE level)
        List<Handler> handlerChain = bp.getBinding().getHandlerChain();
        handlerChain.add(new LogMessageHandler());
        bp.getBinding().setHandlerChain(handlerChain);


    }

    @AfterEach
    public void cleanupContext() {
        this.gds2 = null;
        this.ctxt = null;
    }

    @Test
    public void alleBestanden() {
        BestandenlijstOpvragenRequest request = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType verzoek = new BestandenlijstOpvragenType();
        request.setVerzoek(verzoek);

        AfgifteSelectieCriteriaType criteria = new AfgifteSelectieCriteriaType();
        verzoek.setAfgifteSelectieCriteria(criteria);
        criteria.setNogNietGerapporteerd(Boolean.FALSE);
//        SorteringType sortering = new SorteringType();
//        sortering.setVolgorde("DESC");
//        sortering.setKolom("DATUM_AANMELDING");
//        criteria.setSortering(sortering);

    BestandenlijstOpvragenResponse response = this.executeRequest(request);

        List<AfgifteType> afgiftes=response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(5, afgiftes.size());
        assertFalse(response.getAntwoord().isMeerAfgiftesbeschikbaar(),"Er moeten geen afgiftes meer beschikbaar zijn");

        assertEquals("https://service30.kadaster.nl/gds2/download/privateMock_bestand1.txt", getAfgifteURL(afgiftes.get(0), this.getBaseURL(response.getAntwoord())));
    }

    @Test
    public void nogNietGerapporteerd() {
        BestandenlijstOpvragenRequest request = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType verzoek = new BestandenlijstOpvragenType();
        request.setVerzoek(verzoek);

        AfgifteSelectieCriteriaType criteria = new AfgifteSelectieCriteriaType();
        verzoek.setAfgifteSelectieCriteria(criteria);
        criteria.setNogNietGerapporteerd(Boolean.TRUE);

        BestandenlijstOpvragenResponse response = this.executeRequest(request);

        List<AfgifteType> afgiftes=response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(1, afgiftes.size());
        assertTrue(response.getAntwoord().isMeerAfgiftesbeschikbaar(),"Er zouden meer afgiftes beschikbaar moeten zijn");

        assertEquals("https://service30.kadaster.nl/gds2/download/privateMock_bestand2.txt", getAfgifteURL(afgiftes.get(0), this.getBaseURL(response.getAntwoord())));
    }

    @Test
    public void artikelNummer() {
        BestandenlijstOpvragenRequest request = new BestandenlijstOpvragenRequest();
        BestandenlijstOpvragenType verzoek = new BestandenlijstOpvragenType();
        request.setVerzoek(verzoek);

        AfgifteSelectieCriteriaType criteria = new AfgifteSelectieCriteriaType();
        verzoek.setAfgifteSelectieCriteria(criteria);
        criteria.setBestandKenmerken(new BestandKenmerkenType());
        criteria.getBestandKenmerken().setArtikelnummer("4");

        BestandenlijstOpvragenResponse response = this.executeRequest(request);

        List<AfgifteType> afgiftes=response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(2, afgiftes.size());
        assertTrue(response.getAntwoord().isMeerAfgiftesbeschikbaar(),"Er zouden meer afgiftes beschikbaar moeten zijn");

        assertEquals("https://service30.kadaster.nl/gds2/download/privateMock_bestand4.txt", getAfgifteURL(afgiftes.get(0), this.getBaseURL(response.getAntwoord())));
    }

    private BestandenlijstOpvragenResponse   executeRequest(BestandenlijstOpvragenRequest request) {
        BestandenlijstOpvragenResponse response = gds2.bestandenlijstOpvragen(request);
        return response;
    }

    private BaseURLType getBaseURL(BestandenlijstOpvragenResultaatType antwoord){
        for (BaseURLType type: antwoord.getBaseURLSet ().getBaseURL() ){
            if(type.getType().equalsIgnoreCase("certificaat")){
                return type;
            }
        }
        return null;
    }

    private String getAfgifteURL(AfgifteType afgifte, BaseURLType type){
        return type.getValue() + afgifte.getBestand().getBestandsnaam();
    }
}
