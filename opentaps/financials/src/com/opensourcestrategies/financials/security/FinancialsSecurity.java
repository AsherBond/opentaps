/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
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
