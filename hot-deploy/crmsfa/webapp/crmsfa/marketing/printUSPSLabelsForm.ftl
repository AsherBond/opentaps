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

<form method="get" action="<@ofbizUrl>uspsBulkMailLabels.pdf</@ofbizUrl>">

  <input type="hidden" name="contactListId" value="${contactList.contactListId}"/>
  <select name="addressLabelId" size="1" class="inputBox">
    <#list addressLabelSpecs as addressLabelSpec>
    <option value="${addressLabelSpec.addressLabelId}">${addressLabelSpec.description}</option>
    </#list>
  </select>
  <input type="submit" class="smallSubmit" value="${uiLabelMap.CrmPrintLabels}"/>

</form>

<div class="spacer">&nbsp;</div>
<form method="get" name="completeCatalogMailing" action="<@ofbizUrl>completeCatalogMailing</@ofbizUrl>">
  <input type="hidden" name="contactListId" value="${contactList.contactListId}"/>
  <@inputConfirm title=uiLabelMap.CrmMarkMailingSent form="completeCatalogMailing"/>
</form>
