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
 *  
-->
<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>
<#if acctgTrans?exists>
    <@form name="postAcctgTransForm" url="postAcctgTrans" acctgTransId=acctgTrans.acctgTransId />
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
                </#if><#t>
            </#if><#t/>
        </div><#t/>
        <div class="boxhead">
            ${uiLabelMap.FinancialsTransaction}
        </div>
    </div>
</#if>
