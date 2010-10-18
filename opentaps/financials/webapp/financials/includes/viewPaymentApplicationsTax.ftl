<#--
 * Copyright (c) Open Source Strategies, Inc.
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
 *  
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

        <table class="listTable" >
            <tr class="boxtop">
		    <td><span class="boxhead">${uiLabelMap.FinancialsTaxForRegion}</span></td>
		    <td><span class="boxhead"></span></td>
		    <td><span class="boxhead"></span></td>		    
		    <td><span class="boxhead">${uiLabelMap.FinancialsAmountOutstanding}</span></td>
           </tr>
            <#list paymentApplications as row>
           <tr class="viewManyTR2">
   			    <@displayCell text=row.taxAuthGeoId?if_exists/>
   			    <td></td>
   			    <td></td>                
                <@displayCurrencyCell amount=row.amountApplied currencyUomId=row.currencyUomId/>
            </tr>
            <#if tagTypes?has_content && allocatePaymentTagsToApplications>
			    <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="1" entity=row! readonly="true"/>
			</#if>           
            </#list>
        </table>
