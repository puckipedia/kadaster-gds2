/*
 * Copyright (C) 2017 B3Partners B.V.
 */
package nl.b3p.soap.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.xml.namespace.QName;
import jakarta.xml.soap.MimeHeader;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 'TRACE' level logging van de soap berichten.
 *
 * @author mprins
 */
public class LogMessageHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Log LOG = LogFactory.getLog(LogMessageHandler.class);

    @Override
    public Set<QName> getHeaders() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        try {
            if (LOG.isTraceEnabled()) {
                boolean isOutboundMessage=  (Boolean)context.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if(isOutboundMessage){
                    LOG.trace("Uitgaand soap bericht \n================");
                }else{
                    LOG.trace("Inkomend soap bericht \n================");
                }

                Iterator<MimeHeader> i = context.getMessage().getMimeHeaders().getAllHeaders();
                while (i.hasNext()) {
                    MimeHeader h = i.next();
                    LOG.trace("header: " + h.getName() + " = " + h.getValue());
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                context.getMessage().writeTo(bos);
                LOG.trace("bericht: " + bos.toString("UTF-8") +
                        "\n================");
            }
        } catch (SOAPException | IOException ex) {
            LOG.trace(ex);
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }
}
