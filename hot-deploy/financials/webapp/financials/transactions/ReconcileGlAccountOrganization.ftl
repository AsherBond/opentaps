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
 *  @author Leon Torres leon@opensourcestrategies.com
-->

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#-- TODO: security validation, uiLabelMap -->

<#-- custom CSS formatting -->
<style type="text/css">
<!--
div.scroll {
  height: 400px;
  width: 95%%;
  overflow: auto;
  border: 0px;
  background-color: #FFFFFF;
  padding: 0px;
}

.currencyinput {
  font-size: 10px;
  background-color: #FFFFFF;
  border: 1px solid #000000;
  padding: 2px;
  text-align: right;
}

.textinput {
  font-size: 10px;
  color: #000000;
  background-color: #FFFFFF;
  border: 1px solid #000000;
  padding: 2px;
}

.textlabel {
  font-size: 10px;
  color: #000000;
  background-color: #FFFFFF;
  border: 0px;
  padding: 2px;

}

.textlabelright {
  font-size: 10px;
  color: #000000;
  background-color: #FFFFFF;
  border: 0px;
  padding: 2px;
  text-align: right;
}

.smallSubmitDisabled { 
  color: #666666;
  border: #666666 solid 1px;
} 

.smallSubmitDisabled:hover {
  color: #666666;
  border: #666666 solid 1px;
}

.firstnotice {
  font-size: 10px;
  color: #333333;
}

td.gray1 { background:  #EEEEEE; }
td.gray2 { background:  #FCFCFC; }
td.red1  { background:  #FFDDDD; }
td.red2  { background:  #FFEEEE; }
td.blue1 { background:  #DDDDFF; }
td.blue2 { background:  #EEEEFF; }

-->
</style>

<#-- handy little macro to display the various order by links -->
<#macro displayReorderLink orderByStr titleDescription>
<#--
Turning this off for now: we need to find a way to do this in the browser
<span class="tableheadtext">${titleDescription}</span>
-->
<a href="javascript:submitSaveAndReturn(document.reconcileForm, 'orderBy=${orderByStr}')" class="buttontext">${titleDescription}</a>
</#macro>


<form name="reconcileForm" method="POST" action="<@ofbizUrl>reconcileGlAccount</@ofbizUrl>">

<#-- outline box -->
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
<tr><td width='100%'>

<#-- Main Header with Title -->
<table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
<tr>
<td><div class='boxhead'>Reconciliation for ${glAccount.accountCode} - ${glAccount.accountName}</div></td>
</tr>
</table>

<#-- Rest of stuff Box-->
<table width='100%' border='0' cellspacing='0' cellpadding='2' class='boxbottom'>
<tr><td>


<#if (entries?exists) && (entries.size() > 0)> <#-- BEGIN ENTRIES EXIST -->

<#-- Description and balance table -->

<table border="0" cellpadding="2" cellspacing="0" class="calendarTable">


  <#-- Balance subtable -->

  <#-- store our last reconciled balance for form script use -->
  <input type="hidden" name="lastReconciledBalance" value="${lastReconciledBalance?string("0.00")}"/>
  <input type="hidden" name="organizationPartyId" value="${organizationPartyId}"/>
  <#-- Beginning Balance -->
  <tr>
    <td align="right" width="200px"><span class="tableheadtext">Beginning Balance</span></td>
    <td>&nbsp;</td>
    <td><input class="textlabelright" type="textbox" disabled="true" size="12" name="beginningBalance" value="${lastReconciledBalance}" onChange="javascript:recalculateBalance(this.form)"/></td>
    <td>&nbsp;</td>

    <#-- display reconcile date if exists -->
    <#if lastReconciledDate?exists>
      <td><span class="tableheadtext">as of</span></td>
      <td>&nbsp;</td>
      <td>
        <table border='0' cellspacing='0' cellpadding='0'>
          <tr>
            <td nowrap>
              <input type='text' size='30' class='textlabel' disabled="true" name='lastReconciledDate' value='${getLocalizedDate(lastReconciledDate)}'/>
            </td>
          </tr>
        </table>
      </td>
    <#else>
      <td colspan="3"><span class="firstnotice">(First Reconciliation for this GL Account)</span></td>
    </#if>
  </tr>

  <#-- Ending Balance -->
  <tr>
    <td align="right" width="200px"><span class="tableheadtext">Ending Balance</span></td>
    <td>&nbsp;</td>
    <td><input class="textlabelright" type="textbox" disabled="true" size="12" name="endingBalance" value="${reconciledBalance}" onChange="javascript:recalculateBalance(this.form)"/></td>
    <td>&nbsp;</td>
    <td><span class="tableheadtext">as of</span></td>
    <td>&nbsp;</td>
    <td>
      <table border='0' cellspacing='0' cellpadding='0'>
        <tr>
          <td nowrap>
            <input type='hidden' name='reconciledDate' value='${getLocalizedDate(reconciledDate)}'/>
            <input type='textbox' size='30' class='textlabel' disabled="true" value='${getLocalizedDate(reconciledDate)}'/>
          </td>
        </tr>
      </table>
    </td>

  </tr>

  <#-- Calculated Balance -->
  <#--
       This is inefficient, but we need to loop through the list once to add up the partly reconciled entries for the calculated balance.
       TODO: refactor this into the main loop somehow.
  -->
  <#assign calcBalance = lastReconciledBalance/>
  <#list entries as entry>
    <#if entry.reconcileStatusId?exists && (entry.reconcileStatusId == "AES_PARTLY_RECON")>
      <#assign flag = entry.debitCreditFlag/>
      <#if ((flag == "D") && (accountIsDebit == "TRUE")) || ((flag == "C") && (accountIsDebit == "FALSE"))>
          <#assign calcBalance = calcBalance + entry.amount/>
      <#else>
          <#assign calcBalance = calcBalance - entry.amount/>
      </#if>
    </#if>
  </#list>
  <tr>
    <td align="right" width="200px"><span class="tableheadtext">Calculated Balance</span></td>
    <td>&nbsp;</td>
    <td><input class="textlabelright" type="textbox" disabled="true" size="12" name="reconciledBalance" value="${calcBalance?string("0.00")}"/></td>
    <td>&nbsp;</td>

    <#-- Refresh and Save -->
    <td colspan="3" rowspan="2" valign="center" align="center">
      <table border="0" cellpadding="0" cellspacing="0" class="calendarTable">
        <input type="button" class="smallSubmit" name="recalculate" value="Refresh" onClick="javascript:recalculateBalance(this.form)"/>
        &nbsp;
        <input type="button" class="smallSubmit" name="save" value="Check All" onClick="javascript:checkAll(this.form)"/>
        &nbsp;
        <input type="button" class="smallSubmit" name="save" value="Uncheck All" onClick="javascript:uncheckAll(this.form)"/>
      </table>
    </td>

  </tr>

  <#-- Difference -->
  <#-- TODO: reconciledBalance is passed in as a what? it complains about this not being numeric -->
  <#assign difference = reconciledBalance?number - calcBalance/>
  <tr>
    <td align="right" width="200px"><span class="tableheadtext">${uiLabelMap.OpentapsDifference}</span></td>
    <td>&nbsp;</td>
    <td><input class="textlabelright" type="textbox" disabled="true" size="12" name="difference" value="${difference?string("0.00")}"/></td>
  </tr>

</table>

<table border="0" cellpadding="2" cellspacing="0" class="calendarTable">

  <#-- Save button -->
  <tr>
    <td colspan="2">&nbsp;</td>
    <td><input type="button" class="smallSubmit" name="save" value="Save For Later" onClick="javascript:submitSave(this.form)"/></td>
  </tr>

  <#-- Name and description -->
  <tr>
    <td align="right" width="200px"><span class="tableheadtext">Name</span></td>
    <td>&nbsp;</td>
    <td><input class="textinput" type="textbox" size="30" maxlength="100" name="glReconciliationName" value="${uiLabelMap.FinancialsReconciledAsOf} ${getLocalizedDate(reconciledDate)}"/></td>
  </tr>
  <tr>
    <td align="right" width="200px"><span class="tableheadtext">Description</span></td>
    <td>&nbsp;</td>
    <td><input class="textinput" type="textbox" size="30" maxlength="255" name="description" value=""/></td>
  </tr>

  <#-- reconcile button -->
  <tr>
    <td colspan="2">&nbsp;</td>
    <td>
      <input type="button" class="smallSubmit smallSubmitDisabled" name="reconcile" value="Reconcile" disabled="true" onClick="javascript:submitReconcile(this.form)"/>
    </td>
  </tr>
</table>

<p/>


<#-- Entry Header Table -->
<div class="scroll">
<table border="1" cellpadding="2" cellspacing="0" class="calendarTable" width="98%">
  <tr align="center">
    <td><@displayReorderLink orderByStr="acctgTransId" titleDescription="Transaction"/></td>
    <td><span class="tableheadtext">Party</span></td>
    <td><@displayReorderLink orderByStr="acctgTransTypeId" titleDescription="Transaction Type"/></td>
    <td><@displayReorderLink orderByStr="refNum" titleDescription="Reference Num"/></td>
    <td><@displayReorderLink orderByStr="transactionDate" titleDescription="Transaction Date"/></td>
    <#-- little trick: if you click once, it sorts by descending amount, if you click again, it sorts by ascending amount -->
    <td><#if (parameters.orderBy?has_content) && (parameters.orderBy == "debitDESC")><@displayReorderLink orderByStr="debitASC" titleDescription="Debit"/><#else><@displayReorderLink orderByStr="debitDESC" titleDescription="Debit"/></#if></td>
    <td><#if (parameters.orderBy?has_content) && (parameters.orderBy == "creditDESC")><@displayReorderLink orderByStr="creditASC" titleDescription="Credit"/><#else><@displayReorderLink orderByStr="creditDESC" titleDescription="Credit"/></#if></td>
    <td><@displayReorderLink orderByStr="reconcileStatusId" titleDescription="Reconcile"/></td>
  </tr>

  <#-- Entries -->

  <#assign rowCount = 0 />
  <#list entries as entry>

    <#-- assign some alternating colors -->
    <#if rowCount % 2 == 0>
      <#assign graycycle = "gray1"/>
      <#assign redcycle  = "red1"/>
      <#assign bluecycle = "blue1"/>
    <#else>
      <#assign graycycle = "gray2"/>
      <#assign redcycle  = "red2"/>
      <#assign bluecycle = "blue2"/>
    </#if>

    <#-- special form data for service-multi updateAcctTransEntries -->
    <input type="hidden" name="acctgTransId_o_${rowCount}" value="${entry.acctgTransId}"/>
    <input type="hidden" name="acctgTransEntrySeqId_o_${rowCount}" value="${entry.acctgTransEntrySeqId}"/>
    <input type="hidden" name="amount_o_${rowCount}" value="${entry.amount?string("0.00")}"/>
    <input type="hidden" name="reconcileStatusId_o_${rowCount}" value="${entry.reconcileStatusId?if_exists}"/>

    <#-- special form data for reconcileGlAccount: entry_id_(index) stores the transaction ID pair -->
    <input type="hidden" name="entry_id_${rowCount}" value="${entry.acctgTransId}|${entry.acctgTransEntrySeqId}"/>

    <#-- check entry by default if partly reconciled and set special input fields _atei to transaction IDs if checked -->
    <#if entry.reconcileStatusId?exists && (entry.reconcileStatusId == "AES_PARTLY_RECON")>
      <#assign checked="checked"/>
      <input type="hidden" name="${rowCount}_atei" value="${entry.acctgTransId}|${entry.acctgTransEntrySeqId}"/>
    <#else>
      <#assign checked=""/>
      <input type="hidden" name="${rowCount}_atei" value=""/>
    </#if>

    <#-- entry display (note: checkbox value is the row/entry index) -->
    <tr>
      <td class="${graycycle}"><span class="tabletext">
        <a target="_blank" href="<@ofbizUrl>viewAcctgTrans?acctgTransId=${entry.acctgTransId}</@ofbizUrl>">${entry.acctgTransId}</a>
      </span></td>
      <td class="${graycycle}"><span class="tabletext"><#if entry.partyId?exists>${Static['org.ofbiz.party.party.PartyHelper'].getPartyName(delegator, entry.get("partyId"), false)} (${entry.partyId})<#else>&nbsp;</#if></span></td>
      <td class="${graycycle}"><span class="tabletext">
      <#if entry.acctgTransTypeId?has_content>
          ${entry.getRelatedOneCache("AcctgTransType").getString("description")}
      <#else>
          ${uiLabelMap.CommonNA}
      </#if>
          <#if entry.paymentId?has_content>
              <#if entry.paymentTypeId?has_content>(${entry.getRelatedOneCache("PaymentType").description})</#if>
              <a href="<@ofbizUrl>viewPayment?paymentId=${entry.paymentId}</@ofbizUrl>">${entry.paymentId}</a>
          </#if>
      </span></td>
      <td class="${graycycle}">
           <span class="tabletext">${entry.refNum?if_exists}</span>
      </td>
      <td class="${graycycle}"><span class="tabletext">${getLocalizedDate(entry.transactionDate)}</span></td>
      <#if entry.debitCreditFlag == "D">
        <td class="${redcycle}"><span class="tabletext">${entry.amount?string("0.00")}</span></td>
        <td class="${bluecycle}"><span class="tabletext">&nbsp;</span></td>
        <td class="${graycycle}" align="center"><span class="tabletext">
          <input type="checkbox" ${checked} name="_rowSubmit_o_${rowCount}" value="${rowCount}" onClick="javascript:toggleEntry(this)"/>
        </span></td>
      <#else>
        <td class="${redcycle}"><span class="tabletext">&nbsp;</span></td>
        <td class="${bluecycle}"><span class="tabletextright">${entry.amount?string("0.00")}</span></td>
        <td class="${graycycle}" align="center"><span class="tabletext">
          <input type="checkbox" ${checked} name="_rowSubmit_o_${rowCount}" value="${rowCount}" onClick="javascript:toggleEntry(this)"/>
        </span></td>
      </#if>
      <input type="hidden" name="debitCreditFlag${rowCount}" value="${entry.debitCreditFlag}"/>
    </tr>

    <#assign rowCount = rowCount + 1 />
  </#list>

  <#-- some final form data -->
  <input type="hidden" name="_rowCount" value="${rowCount}"/>
  <input type="hidden" name="_useRowSubmit" value="N"/>
  <input type="hidden" name="glAccountId" value="${glAccount.glAccountId}"/>

</table>
</div>

<#-- javascript goes here due to dependancy on rowCount value -->
<script type="text/javascript">

function uncheckAll(form) {
  for (var i = 0; i < ${rowCount}; i++) {
    form.elements['_rowSubmit_o_' + i].checked = false;
    form.elements['reconcileStatusId_o_' + i].value = "AES_NOT_RECONCILED";
    form.elements[i + '_atei'].value = "";
  }
  recalculateEntries(form);
}

function checkAll(form) {
  for (var i = 0; i < ${rowCount}; i++) {
    form.elements['_rowSubmit_o_' + i].checked = true;
    form.elements['reconcileStatusId_o_' + i].value = "AES_PARTLY_RECON";
    form.elements[i + '_atei'].value = form.elements['entry_id_' + i].value;
  }
  recalculateEntries(form);
}

<#-- 
  This called is when users work with the balance; It does javascript validation on the number format,
  then recomputes the balances. The last step is to call recalculateEntries to add the entries.
-->
function recalculateBalance(form) {
  beginningBalance = (form.beginningBalance.value - 0);
  if (isNaN(beginningBalance)) {
    beginningBalance = (form.lastReconciledBalance.value - 0);
  }
  form.beginningBalance.value = beginningBalance.toFixed(2);

  endingBalance = (form.endingBalance.value - 0);
  if (isNaN(endingBalance)) {
    endingBalance = 0;
  }
  form.endingBalance.value = endingBalance.toFixed(2);

  recalculateEntries(form); 
}

<#--
  This method is called whenever any form fields are toggled/changed by user.
  Computes net balance and difference for each checked acctgTransEntry.
  Afterwards, determines whether to enable/disable Reconcile button.
-->
function recalculateEntries(form) {
  reconciledBalance = (form.beginningBalance.value - 0);
  if (isNaN(reconciledBalance)) {
    reconciledBalance = form.lastReconciledBalance.value;
  }
  checked = 0;
  for (var i=0; i < ${rowCount}; i++) {    
    if (form.elements['_rowSubmit_o_' + i].checked) { 
      checked += 1;
      <#-- 
        To cut down on javascript, we determine the type of account in freemarker and pick which javascript logic to use.
        In the case of debit accounts (first case), we want the balance to add debits and subtract credits.
      -->
      <#if accountIsDebit == "TRUE">
      if (form.elements['debitCreditFlag' + i].value == 'D') {
        // increase in debit is increase in balance for accounts of type DEBIT
        reconciledBalance += (form.elements['amount_o_' + i].value - 0);
      } else {
        // increase in credit is descrease in balance for accounts of type DEBIT
        reconciledBalance -= (form.elements['amount_o_' + i].value - 0);
      }
      <#else> <#-- otherwise we want balance to add credits and subtract debits -->
      if (form.elements['debitCreditFlag' + i].value == 'D') {
        // increase in debit is descrease in balance for accounts of type CREDIT
        reconciledBalance -= (form.elements['amount_o_' + i].value - 0);
      } else {
        // increase in credit is increase in balance for accounts of type CREDIT
        reconciledBalance += (form.elements['amount_o_' + i].value - 0);
      }
      </#if>
    }
  }
  form.reconciledBalance.value = reconciledBalance.toFixed(2);
  difference = form.endingBalance.value - form.reconciledBalance.value;
  form.difference.value = difference.toFixed(2);

  // finally, we check if we should let user Reconcile
  if (checked > 0) {
    checkAllowReconcile(form);
  } else { // handle the false case of checkAllowReconcile here (because it doesn't count checked boxes)
    form.reconcile.disabled = true;
    form.reconcile.className = "smallSubmit smallSubmitDisabled";
  }
}

<#-- Checks to see if Reconcile button should be enabled, but does not count checkboxes. -->
function checkAllowReconcile(form) {
  difference = (form.difference.value - 0);
  if ((difference == 0) && (form.reconciledDate.value.length > 0)) {
    form.reconcile.disabled = false;
    form.reconcile.className = "smallSubmit";
  } else {
    form.reconcile.disabled = true;
    form.reconcile.className = "smallSubmit smallSubmitDisabled";
  }
}

<#-- 
  Called when user clicks on a checkbox. If checked, this will set the reconcileStatusId  to partly reconciled
  and sets the special field sequence index_atei to the transaction ID pair. If unchecked, it nulls out the
  relevant _atei field and sets status to not reconciled. Finally, it recalculates the balance.
-->
function toggleEntry(element) {
  form = element.form;
  index = (element.value - 0); // note: checkbox value is the row index
  if (element.checked) {
    form.elements['reconcileStatusId_o_' + index].value = "AES_PARTLY_RECON";
    form.elements[index + '_atei'].value = form.elements['entry_id_' + index].value;
  } else {
    form.elements['reconcileStatusId_o_' + index].value = "AES_NOT_RECONCILED";
    form.elements[index + '_atei'].value = "";
  }
  recalculateEntries(element.form);
}

<#-- on save, call updateAcctgTransEntry to update changes in reconciled status -->
function submitSave(form) {
  form.action = "<@ofbizUrl>updateAcctgTransEntries</@ofbizUrl>";
  form.submit();
}

<#-- on reconcile, call reconcileGlAccount, which takes care of everything from here -->
function submitReconcile(form) {
  // tricky problem: must enable reconciledBalance or it won't get submitted!
  form.reconciledBalance.disabled = false;
  form.action = "<@ofbizUrl>reconcileGlAccount</@ofbizUrl>";
  form.submit();
}

<#-- on an action that requires returning to this page, use this function to save the state -->
function submitSaveAndReturn(form, params) {
  form.reconciledBalance.disabled = false;
  form.action = "updateAndRefreshReconciliation?"+params;
  form.submit();
}

</script>

<#else>
  <p><span class="tableheadtext">No accounting transactions to reconcile for this account.</span></p>
</#if> <#-- END ENTRIES EXIST -->

</td></tr>
</table>
</td></tr>
</table>
</form>

