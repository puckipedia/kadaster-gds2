/*
 * Copyright (C) 2020 B3Partners B.V.
 */
package nl.b3p.gds2;

import org.junit.jupiter.api.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * testcases voor {@link GDS2Util}.
 * @author mprins
 */
public class GDS2UtilTest {
    private static final String DATE = "23-03-2020";

    @Test
    public void testGetDatumTijd() {
        final GregorianCalendar cal = GDS2Util.getDatumTijd(DATE);

        assertEquals(2020, cal.get(Calendar.YEAR));
        // GregorianCalendar maanden beginnen met 0, niet met 1
        assertEquals(3 - 1, cal.get(Calendar.MONTH));
        assertEquals(23, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGetDatumTijd_null() {
        final GregorianCalendar cal = GDS2Util.getDatumTijd(null);

        assertNull(cal);
    }


    @Test
    public void testGetDatumTijd_nu() {
        final GregorianCalendar cal = GDS2Util.getDatumTijd("nu");

        final Date d = new Date();

        assertEquals(d.getYear() + 1900, cal.get(Calendar.YEAR));
        assertEquals(d.getMonth(), cal.get(Calendar.MONTH));
        assertEquals(d.getDate(), cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGetDatumTijd_before() {
        final GregorianCalendar cal = GDS2Util.getDatumTijd(DATE, -3);

        assertEquals(2020, cal.get(Calendar.YEAR));
        // GregorianCalendar maanden beginnen met 0, niet met 1
        assertEquals(3 - 1, cal.get(Calendar.MONTH));
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGetXMLDatumTijd_int_int_int() {
        final XMLGregorianCalendar cal = GDS2Util.getXMLDatumTijd(2020, 3, 23);

        assertEquals(2020, cal.getYear());
        assertEquals(3, cal.getMonth());
        assertEquals(23, cal.getDay());
    }

    @Test
    public void testGetXMLDatumTijd_gregDate() {
        final XMLGregorianCalendar cal = GDS2Util.getXMLDatumTijd(GDS2Util.getDatumTijd(DATE));

        assertEquals(2020, cal.getYear());
        // GregorianCalendar maanden beginnen met 0, XMLGregorianCalendar met 1
        assertEquals(3, cal.getMonth());
        assertEquals(23, cal.getDay());
    }
}
