/*
 * Copyright (C) 2018 B3Partners B.V.
 */
package nl.b3p.gds2;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import java.util.List;
import java.util.Map;

import static nl.b3p.gds2.GDS2Util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mark Prins
 */
public class GDS2MockTest {

    private static final String MOCK_ENDPOINT = "http://localhost:8088/AfgifteService";
    private static final Log LOG = LogFactory.getLog(GDS2MockTest.class);
    private Map<String, Object> ctxt;
    private Gds2AfgifteServiceV20170401 gds2;
    private BestandenlijstOpvragenRequest request;
    private BestandenlijstOpvragenType verzoek;
    private AfgifteSelectieCriteriaType criteria;

    @BeforeEach
    public void initContext() {
        this.gds2 = new Gds2AfgifteServiceV20170401Service().getAGds2AfgifteServiceV20170401();
        BindingProvider bp = (BindingProvider) gds2;
        this.ctxt = bp.getRequestContext();
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, MOCK_ENDPOINT);
        LOG.info("Endpoint protocol gewijzigd naar mock: " + MOCK_ENDPOINT);

        // soap berichten logger inhaken (is actief met TRACE log level)
        List<Handler> handlerChain = bp.getBinding().getHandlerChain();
        handlerChain.add(new LogMessageHandler());
        bp.getBinding().setHandlerChain(handlerChain);

        request = new BestandenlijstOpvragenRequest();
        verzoek = new BestandenlijstOpvragenType();
        criteria = new AfgifteSelectieCriteriaType();
        verzoek.setAfgifteSelectieCriteria(criteria);
        request.setVerzoek(verzoek);
    }

    @AfterEach
    public void cleanupContext() {
        this.gds2 = null;
        this.ctxt = null;
    }

    @Test
    public void alleBestanden(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        criteria.setNogNietGerapporteerd(Boolean.FALSE);
        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(5, response.getAntwoord().getAfgifteAantalInLijst());
        assertFalse(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er moeten geen afgiftes meer beschikbaar zijn");

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/6xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/6xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getAnoniemBaseURL(response.getAntwoord()))
        );
    }

    @Test
    public void nogNietGerapporteerd(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        criteria.setNogNietGerapporteerd(Boolean.TRUE);

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(1, response.getAntwoord().getAfgifteAantalInLijst());
        assertTrue(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden meer afgiftes beschikbaar moeten zijn");

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/7xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/7xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getAnoniemBaseURL(response.getAntwoord()))
        );
    }

    @Test
    public void artikelNummer(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        criteria.setBestandKenmerken(new BestandKenmerkenType());
        criteria.getBestandKenmerken().setArtikelnummer("4");

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(2, response.getAntwoord().getAfgifteAantalInLijst());
        assertTrue(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden meer afgiftes beschikbaar moeten zijn");

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/4xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getCertificaatBaseURL(response.getAntwoord()))
        );
    }

    @Test
    public void contractNummer(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        criteria.setBestandKenmerken(new BestandKenmerkenType());
        criteria.getBestandKenmerken().setContractnummer("0000000002");

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(2, response.getAntwoord().getAfgifteAantalInLijst());
        assertTrue(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden meer afgiftes beschikbaar moeten zijn");

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/6xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/3xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(1), getCertificaatBaseURL(response.getAntwoord()))
        );

        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/6xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getAnoniemBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/3xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(1), getAnoniemBaseURL(response.getAntwoord()))
        );
    }


    @Test
    public void klantAfgifteNummerTot(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        KlantAfgiftenummerReeksType reeks = new KlantAfgiftenummerReeksType();
        reeks.setKlantAfgiftenummerVanaf(java.math.BigInteger.valueOf(0));
        reeks.setKlantAfgiftenummerTotmet(java.math.BigInteger.valueOf(2));
        criteria.setKlantAfgiftenummerReeks(reeks);

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(2, response.getAntwoord().getAfgifteAantalInLijst());
        assertFalse(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden niet meer afgiftes beschikbaar moeten zijn");
        assertEquals(4142, response.getAntwoord().getKlantAfgiftenummerMax());

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/7xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/3xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(1), getCertificaatBaseURL(response.getAntwoord()))
        );

        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/7xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getAnoniemBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/3xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(1), getAnoniemBaseURL(response.getAntwoord()))
        );
    }

    @Test
    public void klantAfgifteNummerVan(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        KlantAfgiftenummerReeksType reeks = new KlantAfgiftenummerReeksType();
        reeks.setKlantAfgiftenummerVanaf(java.math.BigInteger.valueOf(3));

        criteria.setKlantAfgiftenummerReeks(reeks);
        // NB moet op FALSE staan!
        criteria.setNogNietGerapporteerd(Boolean.FALSE);

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(3, response.getAntwoord().getAfgifteAantalInLijst());
        assertFalse(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden niet meer afgiftes beschikbaar moeten zijn");
        assertEquals(4143, response.getAntwoord().getKlantAfgiftenummerMax());

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/3xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/4xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(1), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service30.kadaster.nl/gds2/download/private/5xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(2), getCertificaatBaseURL(response.getAntwoord()))
        );
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/3xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(0), getAnoniemBaseURL(response.getAntwoord()))
        );
    }

    @Test
    public void datumVan(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        criteria.setPeriode(new FilterDatumTijdType());
        criteria.getPeriode().setDatumTijdVanaf(getXMLDatumTijd(2017, 1, 1));
        //criteria.getPeriode().setDatumTijdTotmet(tot);
        criteria.setNogNietGerapporteerd(Boolean.FALSE);

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(5, response.getAntwoord().getAfgifteAantalInLijst());
        assertFalse(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden niet meer afgiftes beschikbaar moeten zijn");
        assertEquals(4140, response.getAntwoord().getKlantAfgiftenummerMax());

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/5xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(4), getAnoniemBaseURL(response.getAntwoord()))
        );
    }

    @Test
    public void datumTot(TestInfo testInfo) throws Exception {
        LOG.info("Test case: " + testInfo.getDisplayName());

        criteria.setPeriode(new FilterDatumTijdType());
        criteria.getPeriode().setDatumTijdVanaf(getXMLDatumTijd(2017, 1, 1));
        criteria.getPeriode().setDatumTijdTotmet(getXMLDatumTijd(2020, 1, 1));
        criteria.setNogNietGerapporteerd(Boolean.FALSE);

        BestandenlijstOpvragenResponse response = retryBestandenLijstOpvragen(gds2, request);

        assertEquals(3, response.getAntwoord().getAfgifteAantalInLijst());
        assertFalse(response.getAntwoord().isMeerAfgiftesbeschikbaar(), "Er zouden niet meer afgiftes beschikbaar moeten zijn");
        assertEquals(41420, response.getAntwoord().getKlantAfgiftenummerMax());

        List<AfgifteType> afgiftes = response.getAntwoord().getBestandenLijst().getAfgifte();
        assertEquals(
                "https://service10.kadaster.nl/gds2/download/public/5xxx-xxxx-xxxx-xxxx",
                getAfgifteURL(afgiftes.get(2), getAnoniemBaseURL(response.getAntwoord()))
        );
    }
}
