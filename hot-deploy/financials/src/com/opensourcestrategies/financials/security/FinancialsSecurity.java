/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package com.opensourcestrategies.financials.security;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.opentaps.common.security.OpentapsSecurity;

public class FinancialsSecurity extends OpentapsSecurity {

    protected String organizationPartyId = null;

    public FinancialsSecurity(Security security, GenericValue userLogin, String organizationPartyId) {
        super(security, userLogin);
        setOrganizationPartyId(organizationPartyId);
    }

    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    public String getOrganizationPartyId() {
        return organizationPartyId;
    }

    public boolean hasViewPartnerPermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER", "_VIEW", organizationPartyId);
    }

    public boolean hasViewPartnerAgreementPermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER_AGREEMENT", "_VIEW", organizationPartyId);
    }

    public boolean hasCreatePartnerAgreementPermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER_AGREEMENT", "_CREATE", organizationPartyId);
    }

    public boolean hasUpdatePartnerAgreementPermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER_AGREEMENT", "_UPDATE", organizationPartyId);
    }

    public boolean hasViewPartnerInvoicePermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER_INVOICE", "_VIEW", organizationPartyId);
    }

    public boolean hasCreatePartnerInvoicePermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER_INVOICE", "_CREATE", organizationPartyId);
    }

    public boolean hasUpdatePartnerInvoicePermission() {
       return hasPartyRelationSecurity("FINANCIALS_PARTNER_INVOICE", "_UPDATE", organizationPartyId);
    }
}
