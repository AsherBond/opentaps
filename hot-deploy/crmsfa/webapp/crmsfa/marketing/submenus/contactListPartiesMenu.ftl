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
<div class="subSectionHeader">
  <div class="subSectionTitle">${uiLabelMap.CrmContactListParties}</div>
  <div class="subMenuBar">${addLink?if_exists}${addDomestic?if_exists}${addForeign?if_exists}</div>
</div>
