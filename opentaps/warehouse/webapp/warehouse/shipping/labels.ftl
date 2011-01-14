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
<#-- select labels to print and mark as accepted -->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<script>
<!--
  function markAsShipped() {
    document.Labels.action = "<@ofbizUrl>BatchUpdateShipmentRouteSegments</@ofbizUrl>";
    document.Labels.submit();
  }
//-->
</script>

<form name="Labels" method="post" action="BatchPrintShippingLabels">
  <@inputHidden name="facilityId" value=parameters.facilityId />
  <@inputHiddenUseRowSubmit />

<@paginate name="labelsToPrint" list=labelsListBuilder >
<#noparse>

<@inputHiddenRowCount list=pageRows />

<@navigationBar />

<table class="crmsfaListTable">
  <tr class="crmsfaListTableHeader">
    <@headerCell title=uiLabelMap.ProductShipmentId orderBy="shipmentId" blockClass="tableheadtext"/>
    <@headerCell title=uiLabelMap.OrderOrderId orderBy="primaryOrderId" blockClass="tableheadtext"/>
    <@headerCell title=uiLabelMap.FormFieldTitle_shipmentRouteSegmentId orderBy="shipmentRouteSegmentId" blockClass="tableheadtext"/>
    <@headerCell title=uiLabelMap.FormFieldTitle_shipmentPackageSeqId orderBy="shipmentPackageSeqId" blockClass="tableheadtext"/>
    <@headerCell title=uiLabelMap.OpentapsCarrier orderBy="carrierPartyId" blockClass="tableheadtext"/>
    <@headerCell title=uiLabelMap.ProductShipmentMethodType orderBy="shipmentMethodTypeId" blockClass="tableheadtext"/>
    <td></td>
    <#if pageSize != 0>
      <@inputMultiSelectAllCell form="Labels" />
    </#if>
  </tr>
  <#list pageRows as row>
  <tr class="${tableRowClass(row_index)}">
    <@inputHidden name="shipmentId" value=row.shipmentId index=row_index />
    <@inputHidden name="shipmentRouteSegmentId" value=row.shipmentRouteSegmentId index=row_index />
    <@inputHidden name="shipmentPackageSeqId" value=row.shipmentPackageSeqId index=row_index />
    <!-- mark as accepted uses this -->
    <@inputHidden name="carrierServiceStatusId" value="SHRSCS_SHIPPED" index=row_index />
    <@inputHidden name="carrierPartyId" value=row.carrierPartyId index=row_index />

    <@displayLinkCell href="ViewShipment?shipmentId=${row.shipmentId}" text=row.shipmentId />
    <@displayLinkCell href="/crmsfa/control/orderview?orderId=${row.primaryOrderId}" text=row.primaryOrderId />
    <@displayCell text=row.shipmentRouteSegmentId />
    <@displayCell text=row.shipmentPackageSeqId />
    <@displayCell text=row.carrierName />
    <@displayCell text=row.shipmentMethod />
    <@displayLinkCell href="shipmentLabelViewer?shipmentId=${row.shipmentId}&shipmentRouteSegmentId=${row.shipmentRouteSegmentId}&shipmentPackageSeqId=${row.shipmentPackageSeqId}&amp;navigation=Y" text=uiLabelMap.ProductLabel />
    <#if pageSize != 0>
      <@inputMultiCheckCell index=row_index />
    </#if>
  </tr>
  </#list>

  <#if pageSize != 0>
    <tr>
      <td colspan="7" align="right">
        <@inputSubmit title=uiLabelMap.WarehousePrintLabels />
        <@inputSubmit title=uiLabelMap.WarehouseMarkAsShipped onClick="markAsShipped()" />
      </td>
    </tr>
  </#if>
</table>

</#noparse>
</@paginate>

</form>
