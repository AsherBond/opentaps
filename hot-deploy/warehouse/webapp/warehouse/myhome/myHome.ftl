<#if hasManufacturingViewPermission>
  <div style="float: right; " align="right">
    <img src="<@ofbizUrl>showChart?chart=${chartImage?if_exists?html}</@ofbizUrl>" style="margin-right: 15px; "/>
  </div>
</#if>

<table class="headedTable">
<#if hasInventoryViewPermission && (hasStockMovePermission || hasInventoryTransferPermission)>
    <tr class="header"><td colspan="2">${uiLabelMap.WarehouseInventory}</td></tr>

    <#if hasStockMovePermission>
        <tr>
            <td>
                <a href="<@ofbizUrl>stockMoves</@ofbizUrl>">${uiLabelMap.WarehouseStockMovesRequired}</a>: 
            </td>
            <td>
                <#if (totalStockMoves!0) &gt; 0>
                    ${totalStockMoves}
                <#else>
                    ${uiLabelMap.CommonNone}
                </#if>
            </td>
        </tr>
    </#if>

    <#if hasInventoryTransferPermission>
        <tr>
            <td>
                <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}</@ofbizUrl>">${uiLabelMap.WarehouseRequestedInventoryTransfers}</a>:
            </td>
            <td>
                <#if (totalInventoryTransfers!0) &gt; 0>
                    ${totalInventoryTransfers}
                <#else>
                    ${uiLabelMap.CommonNone}
                </#if>
            </td>
        </tr>
    </#if>

    <tr>
        <td>
            <a href="<@ofbizUrl>backOrderedItems</@ofbizUrl>">${uiLabelMap.WarehouseBackOrderedOrderCount}</a>:
        </td>
        <td>
            <#if (backOrderedOrders!0) &gt; 0>
                ${backOrderedOrders}
            <#else>
                ${uiLabelMap.CommonNone}
            </#if>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>backOrderedItems</@ofbizUrl>">${uiLabelMap.WarehouseBackOrderedProductCount}</a>:
        </td>
        <td>
            <#if (backOrderedProducts!0) &gt; 0>
                ${backOrderedProducts}
            <#else>
                ${uiLabelMap.CommonNone}
            </#if>
        </td>
    </tr>

</#if>

<#if hasShippingViewPermission && (hasPicklistViewPermission || hasPackPermission)>
    <tr><td>&nbsp;</td></tr>
    <tr class="header"><td colspan="2">${uiLabelMap.WarehouseShipping}</td></tr>

    <#if hasPackPermission>
        <tr>
            <td>
                <a href="<@ofbizUrl>readyToShip</@ofbizUrl>">${uiLabelMap.WarehouseOrdersReadyToShip}</a>:
            </td>
            <td>
                ${totalReadyOrders!0}
            </td>
        </tr>
        <#list pickMoveByShipmentMethodInfoList?default([]) as pickMoveByShipmentMethodInfo>
            <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType?if_exists/>
            <#assign orderReadyToPickInfo = pickMoveByShipmentMethodInfo.orderReadyToPickInfo?if_exists/>
            <tr>
                <td class="subsidiary"><#if shipmentMethodType?has_content>${shipmentMethodType}</#if></td>
                <td>${orderReadyToPickInfo}</td>
            </tr>
        </#list>
            <tr>
                <td>
                    <a href="<@ofbizUrl>openPicklists</@ofbizUrl>">${uiLabelMap.WarehouseOpenPicklists}</a>:
                </td>
                <td>
                    ${totalOpenPicklists!0}
                </td>
            </tr>

        <tr>
            <td>
                <a href="<@ofbizUrl>pickedPicklists</@ofbizUrl>">${uiLabelMap.WarehousePickedPicklists}</a>:
            </td>
            <td>
                ${totalPickedPicklistIds!0}
            </td>
        </tr>

    </#if>
    
</#if>

<#if hasManufacturingViewPermission>
    <tr><td>&nbsp;</td></tr>
    <tr class="header"><td colspan="2">${uiLabelMap.WarehouseManufacturing}</td></tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>FindProductionRun?currentStatusId=PRUN_CREATED&workEffortTypeId=PROD_ORDER_HEADER</@ofbizUrl>">${uiLabelMap.WarehouseProductionRunsCreated}</a>:
        </td>
        <td>
            ${totalCreatedProdRuns!0}
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>FindProductionRun?currentStatusId=PRUN_SCHEDULED&workEffortTypeId=PROD_ORDER_HEADER</@ofbizUrl>">${uiLabelMap.WarehouseProductionRunsScheduled}</a>:
        </td>
        <td>
            ${totalScheduledProdRuns!0}
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>FindProductionRun?currentStatusId=PRUN_DOC_PRINTED&workEffortTypeId=PROD_ORDER_HEADER</@ofbizUrl>">${uiLabelMap.WarehouseProductionRunsConfirmed}</a>:
        </td>
        <td>
            ${totalConfirmedProdRuns!0}
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>FindProductionRun?currentStatusId=PRUN_RUNNING&workEffortTypeId=PROD_ORDER_HEADER</@ofbizUrl>">${uiLabelMap.WarehouseProductionRunsRunning}</a>:
        </td>
        <td>
            ${totalRunningProdRuns!0}
        </td>
    </tr>
    <tr>
      <td class="subsidiary">
        <#assign updated = Static["org.ofbiz.base.util.UtilMisc"].toMap("lastUpdatedStamp", lastUpdatedStamp)!/>
        <#assign lastUpdated = Static["org.opentaps.common.util.UtilMessage"].expandLabel("OpentapsLastUpdatedAt", locale, updated)!/>
        ${lastUpdated}
      </td>
      <td>
        <a href="<@ofbizUrl>myHomeMain?facilityId=${facilityId}&amp;refresh=1</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefresh}</a>
      </td>
    </tr>
</#if>

</table>
