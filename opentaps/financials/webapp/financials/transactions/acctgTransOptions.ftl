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
 *  
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if acctgTrans?exists>
    <@form name="postAcctgTransForm" url="postAcctgTrans" acctgTransId=acctgTrans.acctgTransId skipCheckAcctgTags=""/>
    <@form name="reverseAcctgTransForm" url="reverseAcctgTrans" acctgTransId=acctgTrans.acctgTransId />
    <@form name="deleteAcctgTransForm" url="deleteAcctgTrans" acctgTransId=acctgTrans.acctgTransId />
    <div class="screenlet-header">
        <div style="float: right;"><#t/>
            <#if accountingTransaction.isPosted()><#t/>
                <@submitFormLink form="reverseAcctgTransForm" text=uiLabelMap.FinancialsReverseTransaction class="buttontext" /><#t/>
            <#else><#t/>
                <a class="buttontext" href="updateAcctgTransForm?acctgTransId=${acctgTrans.acctgTransId}">${uiLabelMap.CommonEdit}</a><#t/>
                <#if canDeleteTrans>
                  <@submitFormLink form="deleteAcctgTransForm" text=uiLabelMap.CommonDelete class="buttontext" />
                </#if><#t/>          
                <#if accountingTransaction.canPost()>
                  <@submitFormLink form="postAcctgTransForm" text=uiLabelMap.AccountingPostTransaction class="buttontext" />
                <#elseif accountingTransaction.canPost(true)>
                  <@submitFormLinkConfirm form="postAcctgTransForm" text=uiLabelMap.AccountingPostTransaction confirmText=uiLabelMap.FinancialsPostTransactionConfirmSkipCheckTags skipCheckAcctgTags="Y" />
                </#if><#t/>
            </#if><#t/>
        </div><#t/>
        <div class="boxhead">
            ${uiLabelMap.FinancialsTransaction}
        </div>
    </div>
</#if>
