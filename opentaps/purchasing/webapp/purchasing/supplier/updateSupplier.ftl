<#--
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
-->
<#-- Copyright (c) Open Source Strategies, Inc. -->

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

