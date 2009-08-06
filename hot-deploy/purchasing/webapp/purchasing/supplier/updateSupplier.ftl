<#--
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
-->
<#-- Copyright (c) 2005-2006 Open Source Strategies, Inc. -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<form name="updateSupplier" method="post" action="<@ofbizUrl>updateSupplier</@ofbizUrl>">
<table class="twoColumnForm">
  <@inputHidden name="partyId" value=partySummary.partyId />
  <@inputTextRow name="groupName" title=uiLabelMap.CommonName default=partySummary.groupName />
  <@inputTextRow name="federalTaxId" title=uiLabelMap.OpentapsTaxAuthPartyId default=partySummary.federalTaxId />
  <@inputIndicatorRow name="requires1099" title=uiLabelMap.OpentapsRequires1099 default=partySummary.requires1099 />
  <@inputIndicatorRow name="isIncorporated" title=uiLabelMap.OpentapsIsIncorporated default=partySummary.isIncorporated />
  <@inputSubmitRow title=uiLabelMap.CommonUpdate />
</table>
</form>

