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

<#if hasLotViewPermission>
  <@sectionHeader title=uiLabelMap.WarehouseFindLot>
    <div class="subMenuBar"><a href="<@ofbizUrl>createLotForm</@ofbizUrl>" class="subMenuButton">${uiLabelMap.CommonCreateNew}</a></div>
  </@sectionHeader>

  <div class="form">
    <form method="get" name="lookupLotForm" action="<@ofbizUrl>manageLots</@ofbizUrl>">
      <table border="0">
        <tr>
          <@inputTextRow name="lotId" title=uiLabelMap.ProductLotId default=(parameters.lotId)?if_exists />
          <@inputLookupRow name="supplierPartyId" title=uiLabelMap.ProductSupplier lookup="LookupPartyName" form="lookupLotForm" default=(parameters.supplierPartyId)?if_exists />
          <@inputSubmitRow title=uiLabelMap.WarehouseFindLot />
          <@inputHidden name="doLookup" value="Y"/>
        </tr>
      </table>
    </form>
  </div>

  <br/>
  <@sectionHeader title=uiLabelMap.WarehouseLots />

  <table class="listTable" cellspacing="0">

    <tr class="listTableHeader">
      <td><span class="tableheadtext">${uiLabelMap.ProductLotId}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductSupplier}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductCreatedDate}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductExpireDate}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonComments}</span></td>
    </tr>

    <#if lotList?has_content>
      <#list lotList as lot>
        <tr class="${tableRowClass(lot_index)}">
          <@displayLinkCell href="lotDetails?lotId=${lot.lotId}" text=lot.lotId />
          <#assign supplierInfo = ""/>
          <#if lot.supplierPartyId?has_content>
             <#assign supplierInfo = lot.supplierPartyName?default("") + " (" + lot.supplierPartyId + ")" />
          </#if>
          <@displayCell text=supplierInfo />
          <@displayDateCell date=lot.creationDate />
          <@displayDateCell date=lot.expirationDate />
          <@displayCell text=lot.comments?if_exists />
        </tr>
      </#list>          
    </#if>
  </table>

  <#assign exParams = "&doLookup=Y&supplierPartyId=" + parameters.supplierPartyId?if_exists/>
  <@pagination viewIndex=viewIndex viewSize=viewSize currentResultSize=lotList?size requestName="manageLots" totalResultSize=lotsTotalSize extraParameters=exParams/>

<#else>
  ${uiLabelMap.OpentapsError_PermissionDenied}
</#if>

