/*
 * $Id: FormsFopViewHandler.java,v 1.2 2007/03/23 17:56:31 Richard Coss Exp $
 *
 * Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.opensourcestrategies.crmsfa.handlers;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.opentaps.common.webapp.view.MergeFormsFopViewHandler;

import javax.servlet.http.HttpServletRequest;

import com.opensourcestrategies.crmsfa.party.PartyHelper;

/**
 *
 * @author     <a href="mailto:richard.a.coss@state.or.us">Richard Coss</a>
 */
public class CrmsfaMergeFormsFopViewHandler extends MergeFormsFopViewHandler {
    
    public static final String module = CrmsfaMergeFormsFopViewHandler.class.getName();
    public static String crmsfaResource = "crmsfa";

    public Map getFormMergeContext(HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        String targetPartyId = (String) request.getParameter("targetPartyId");
        String orderId = (String) request.getParameter("orderId");
        String shipGroupSeqId = (String) request.getParameter("shipGroupSeqId");
        String shipmentId = (String) request.getParameter("shipmentId");
        return PartyHelper.assembleCrmsfaFormMergeContext(delegator, locale, targetPartyId, orderId, shipGroupSeqId, shipmentId, timeZone);
    }

    public String getFilename(HttpServletRequest request) {
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));
        String targetPartyId = (String) request.getParameter("targetPartyId");
        String partyId = (String) request.getParameter("partyId");
        String filename = UtilProperties.getMessage(crmsfaResource, "crmsfa.formMerge.fileName", UtilMisc.toMap("targetPartyId", targetPartyId, "partyId", partyId), locale);    
        return (UtilValidate.isNotEmpty(filename)) ? filename : MergeFormsFopViewHandler.defaultFileName;
    }
}
