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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#if hasUpdatePermission?exists>
  <#assign addLink = "<a class='subMenuButton' href='addContactListPartiesForm?contactListId=" + contactList.contactListId + "'>" + uiLabelMap.CommonAddNew + "</a>">
  <#if contactList.contactMechTypeId?default("none") == "POSTAL_ADDRESS">
    <@form name="addDomesticCatalogRequestForm" url="addNewCatalogRequestsToContactList" contactListId="${contactList.contactListId}" includeCountryGeoId="${configProperties.defaultCountryGeoId}"/>
    <@form name="addForeignCatalogRequestForm" url="addNewCatalogRequestsToContactList" contactListId="${contactList.contactListId}" excludeCountryGeoId="${configProperties.defaultCountryGeoId}"/>
    <#assign addDomestic><@submitFormLink form="addDomesticCatalogRequestForm" text="${uiLabelMap.CrmCatalogRequestAddDomestic}"/></#assign>
    <#assign addForeign><@submitFormLink form="addForeignCatalogRequestForm" text="${uiLabelMap.CrmCatalogRequestAddForeign}"/></#assign>
  </#if>
</#if>

<a name="ListContactListParties"></a>
<@frameSectionHeader title=uiLabelMap.CrmContactListParties extra="${addLink?if_exists}${addDomestic?if_exists}${addForeign?if_exists}" />
