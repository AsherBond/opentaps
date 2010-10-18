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
		    <td><span class="boxhead">${uiLabelMap.CommonNote}</span></td>
			<#if tagTypes?has_content>
			    <@accountingTagsDisplayColumns tags=tagTypes/>
			</#if>		    
		    <td><span class="boxhead"></span></td>
           </tr>
            <#list taxAuthPaymentApplicationList as row>
    <form name="addPaymentApplicationTax_${row_index}" action="createPaymentApplication" method="POST" class="basic-form">
           <tr class="viewManyTR2">
                <@inputHidden name="paymentId" value=row.paymentId />
                <@inputHidden name="checkForOverApplication" value="true" />
                <@inputSelectCell class="inputBoxFixedWidth" name="taxAuthGeoId" list=taxAuthGeoIds key="geoId" displayField="geoName" default=row.taxAuthGeoId?if_exists/>
   			    <td></td>
   			    <td></td>
                <@inputTextCell name="amountApplied" default=row.amountToApply/>
                <@inputTextCell name="note" default=row.note/>
	            <@displayLinkCell class="buttontext" href="javascript:document.addPaymentApplicationTax_${row_index}.submit();" text=uiLabelMap.CommonApply/>  
            </tr>
            <#if tagTypes?has_content && allocatePaymentTagsToApplications>
			    <@accountingTagsInputCells tags=tagTypes prefix="acctgTagEnumId" tagColSpan="1" />
			</#if>	
    </form>
            </#list>
        </table>
