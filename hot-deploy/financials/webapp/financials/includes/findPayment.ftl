<#--
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

<div class="screenlet-header">
    <div class="boxhead">
      ${uiLabelMap.FinancialsFindPayment}
    </div>
</div>
<form method="post" action="<@ofbizUrl>findPayment</@ofbizUrl>" name="findPayment">
<input type="hidden" name="findPaymentTypeId" value="${parameters.findPaymentTypeId?if_exists}">
<#if findDisbursement>
  <input type="hidden" name="partyIdFrom" value="${organizationPartyId?if_exists}">
<#else>
  <input type="hidden" name="partyIdTo" value="${organizationPartyId?if_exists}">
</#if>
  <table border="0" cellpadding="2" cellspacing="0" width="100%">
    <tr>
       <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.FinancialsPaymentId} </div></td>
       <td width="2%">&nbsp;</td>
       <td nowrap><input type="text" class="inputBox" name="paymentId" value="${parameters.paymentId?if_exists}"/></td>
    </tr>
    <tr>
       <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.AccountingPaymentType} </div></td>
       <td width="2%">&nbsp;</td>
       <td colspan="4" align="left">
         <div class="tabletext">
           <select name="paymentTypeId" class="selectBox">
               <option value=""></option>
             <#list paymentTypeList as paymentType>
	           <option value="${paymentType.paymentTypeId}" <#if parameters.paymentTypeId?has_content && parameters.paymentTypeId?default("") == paymentType.paymentTypeId>SELECTED</#if>>${paymentType.get("description")}</option>
	         </#list>
           </select>
         </div>
     </td>
   </tr>
   
   <tr>
     <td width="25%" align="right">
       <div class="tableheadtext">
         <#if findDisbursement>
           ${uiLabelMap.FinancialsPayToParty} 
         <#else>  
           ${uiLabelMap.FinancialsReceiveFromParty} 
         </#if>
       </div>
     </td>
     <td width="2%">&nbsp;</td>
     <td nowrap colspan="4" align="left">
       <#if findDisbursement><#assign partyLookupFieldName="partyIdTo"/><#else><#assign partyLookupFieldName="partyIdFrom"/></#if>
       <@inputAutoCompleteParty name=partyLookupFieldName id="findPaymentFormPartyId" />
     </td>
   </tr>   

   <tr>
     <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.FinancialsStatusId} </div></td>
     <td width="2%">&nbsp;</td>
     <td nowrap colspan="4" align="left">
         <select name="statusId" class="selectBox">
           <option value=""></option>
	       <#list statusList as status>
	         <option value="${status.statusId?if_exists}"  <#if defaultStatusId == status.statusId>SELECTED</#if>>${status.description?if_exists}</option>
	       </#list>
	     </select>
     </td>
   </tr>   
   
   <tr>
     <#if findDisbursement >
       <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.FinancialsPaymentMethod} </div></td>
       <td width="2%">&nbsp;</td>
       <td nowrap>
         <select name="paymentMethodId" class="selectBox">
             <option value=""></option>
	       <#list paymentMethodList as paymentMethod>
	         <option value="${paymentMethod.get("paymentMethodId")?if_exists}"  <#if parameters.paymentMethodId?default("") == paymentMethod.paymentMethodId>SELECTED</#if>>${paymentMethod.description?if_exists} (${paymentMethod.paymentMethodId})</option>
	       </#list>
	     </select>
       </td>
     <#else>
       <td width="20%" align="right" nowrap><div class="tableheadtext">${uiLabelMap.FinancialsPaymentMethodType} </div></td>
       <td width="2%">&nbsp;</td>
       <td nowrap>
         <select name="paymentMethodTypeId" class="selectBox">
             <option value=""></option>
          <#list paymentMethodTypeList as paymentMethodType>
	         <option value="${paymentMethodType.get("paymentMethodTypeId")?if_exists}"  <#if parameters.paymentMethodTypeId?default("") == paymentMethodType.paymentMethodTypeId>SELECTED</#if>>${paymentMethodType.get("description")}</option>
          </#list>
	     </select>
       </td>
     </#if>
   </tr>
   <tr>
     <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.AccountingEffectiveDate} </div></td>
     <td width="2%">&nbsp;</td>
     <td nowrap colspan="4" align="left">
       <div class="tabletext">
           <#assign now=Static['org.ofbiz.base.util.UtilDateTime'].nowTimestamp()>
           ${uiLabelMap.CommonFrom}<@inputDateTime name="fromDate" default=parameters.fromDate?if_exists/>
           ${uiLabelMap.CommonThru}<@inputDateTime name="thruDate" default=parameters.thruDate?if_exists/>
       </div>
     </td>
   </tr>
   <tr>
     <td width="20%" align="right"><div class="tableheadtext">${uiLabelMap.FinancialsPaymentRefNum} </div></td>
     <td width="2%">&nbsp;</td>
     <td nowrap><input type="text" class="inputBox" name="paymentRefNum" value="${parameters.paymentRefNum?if_exists}"/></td>
   </tr>
   
   <tr>
     <td width="20%" align="right"><div class="tableheadtext">&nbsp;</div></td>
     <td width="2%">&nbsp;</td>
     <td nowrap colspan="4" align="left">
         <input type="submit" name="submitButton" value="${uiLabelMap.CommonFind}" class="smallSubmit">
     </td>
   </tr>
</table>
</form>

