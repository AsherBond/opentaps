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

<div class="screenlet-header">
  <#if (acctgTrans?exists) && (acctgTrans.isPosted != "Y")>
    <div style="float: right;"><a href="<@ofbizUrl>createAcctgTransEntryForm?acctgTransId=${acctgTrans.acctgTransId}</@ofbizUrl>" class="buttontext" >${uiLabelMap.FinancialsCreateTransactionEntry}</a></div>
  </#if>
  <span class="boxhead">${uiLabelMap.FinancialsTransactionEntries}</span>
</div>

  <#if acctgTrans?exists>
    <table class="listTable" cellspacing="0" style="border:none;">
      <tr class="listTableHeader">
        <td><span>${uiLabelMap.PartySequenceId}</span></td>
        <td><span>${uiLabelMap.GlAccount}</span></td>
        <td><span>${uiLabelMap.FinancialsDebitCredit}</span></td>
        <td><span>${uiLabelMap.CommonAmount}</span></td>
        <td></td>
      </tr>
      
      <#-- for posted transactions just display the list of entries -->
      <#if acctgTrans.isPosted == "Y">
        <#list acctgTransEntries as entry>
          <tr class="${tableRowClass(entry_index)}">
            <td><a class="linktext" href="<@ofbizUrl>viewAcctgTransEntry?acctgTransId=${entry.acctgTransId}&amp;acctgTransEntrySeqId=${entry.acctgTransEntrySeqId}</@ofbizUrl>">${entry.acctgTransEntrySeqId}</a></td>
            <td><#assign glAccount = entry.getRelatedOneCache("GlAccount")/><a class="linktext" href="<@ofbizUrl>AccountActivitiesDetail?glAccountId=${glAccount.glAccountId}&amp;organizationPartyId=${session.getAttribute("organizationPartyId")}</@ofbizUrl>">${glAccount.accountCode?default(glAccount.glAccountId)}</a>: ${glAccount.accountName?default("")}</td>
            <td>${entry.debitCreditFlag}</td>
            <@displayCurrencyCell amount=entry.amount currencyUomId=entry.currencyUomId class="tabletext" />
            <td>${entry.getRelatedOneCache("StatusItem").description}</td>
          </tr>
          <#-- List possible tags in separate lines -->
          <#list tagTypes as tag>
            <!-- only display tags that are set to something -->
            <#assign fieldName = "acctgTagEnumId${tag.index}"/>
            <#if entry.get(fieldName)?has_content>
              <tr class="${tableRowClass(entry_index)}">
                <td/>
                <td colspan="4"><span style="margin-left:20px">${tag.description} :
                <#list tag.tagValues as tagValue>
                  <#if tagValue.enumId == entry.get(fieldName)!>
                    ${tagValue.description}</span></td>
                    <#break/>
                  </#if>
                </#list>
              </tr>
            </#if>
          </#list>
        </#list>
      <#else/>
        <#-- otherwise allow inline editing and adding of new entries -->
        <#if acctgTransEntries?has_content>
          <@form name="deleteAcctgTransEntryForm" url="deleteAcctgTransEntry" acctgTransId="" acctgTransEntrySeqId=""/>
          <form name="updateAcctgTransEntryForm" method="POST" action="<@ofbizUrl>updateAcctgTransEntryInline</@ofbizUrl>">
            <@inputHidden name="acctgTransId" value="${acctgTrans.acctgTransId}" />
            <@inputHiddenRowCount list=acctgTransEntries />
            <@inputHiddenUseRowSubmit />
            <#list acctgTransEntries as entry>
              <@inputHidden name="acctgTransId" value="${entry.acctgTransId}" index=entry_index/>
              <@inputHidden name="acctgTransEntrySeqId" value="${entry.acctgTransEntrySeqId}" index=entry_index/>
              <@inputHiddenRowSubmit submit=false index=entry_index/>
              <tr class="${tableRowClass(entry_index)}">
                <td><a class="linktext" href="<@ofbizUrl>viewAcctgTransEntry?acctgTransId=${entry.acctgTransId}&amp;acctgTransEntrySeqId=${entry.acctgTransEntrySeqId}</@ofbizUrl>">${entry.acctgTransEntrySeqId}</a></td>
                <td><@inputAutoCompleteGlAccount name="glAccountId" id="glAccountId${entry_index}" default=entry.glAccountId  index=entry_index/></td>
                <@inputSelectCell name="debitCreditFlag" list=debitCreditFlags key="debitCreditFlag" default=entry.debitCreditFlag displayField="description" index=entry_index onChange="opentaps.markRowForSubmit(this.form, ${entry_index})" />
                <@inputTextCell name="amount" default=entry.amount size="8" index=entry_index onChange="opentaps.markRowForSubmit(this.form, ${entry_index})" />
                <td>
                  <@inputSubmitIndexed title="${uiLabelMap.CommonUpdate}" index=entry_index/>
                  <@submitFormLink form="deleteAcctgTransEntryForm" text=uiLabelMap.CommonRemove class="smallSubmit" acctgTransId=entry.acctgTransId acctgTransEntrySeqId=entry.acctgTransEntrySeqId/>
                </td>
              </tr>
              <#-- List possible tags in separate lines -->
              <#list tagTypes as tag>
                <#assign fieldName = "acctgTagEnumId${tag.index}"/>
                <#if tag.isRequired()>
                  <#assign titleClass="requiredField" />
                <#else>
                  <#assign titleClass="" />
                </#if>
                <tr class="${tableRowClass(entry_index)}">
                  <td/>
                  <td><span class="${titleClass}">${tag.description}</span></td>
                  <td colspan="3">
                    <@accountingTagsSelect tag=tag prefix="acctgTagEnumId" index=entry_index entity=entry />
                  </td>
                </tr>
              </#list>
            </#list>
          </form>
          <tr>
            <td colspan="5"><hr class="sepbar"/></td>
          </tr>
        </#if>
      
        <form name="createAcctgTransEntryForm" method="POST" action="<@ofbizUrl>createAcctgTransEntryInline</@ofbizUrl>">
          <tr>
            <td>&nbsp;</td>
            <@inputHidden name="acctgTransId" value="${acctgTrans.acctgTransId}"/>
            <@inputHidden name="acctgTransEntryTypeId" value="_NA_"/>
            <@inputHidden name="organizationPartyId" value="${parameters.organizationPartyId}"/>
            <td><@inputAutoCompleteGlAccount name="glAccountId" id="glAccountId" /></td>
            <@inputSelectCell name="debitCreditFlag" list=debitCreditFlags key="debitCreditFlag" displayField="description"/>
            <@inputTextCell name="amount" size="8"/>
            <@inputSubmitCell title="${uiLabelMap.CommonAdd}"/>
          </tr>
          <#-- List possible tags in separate lines -->
          <#list tagTypes as tag>
            <#assign tagName = "acctgTagEnumId${tag.index}"/>
            <#if tag.isRequired()>
              <#assign titleClass="requiredField" />
            <#else>
              <#assign titleClass="" />
            </#if>
            <tr>
              <td/>
              <td><span class="${titleClass}">${tag.description}</span></td>
              <td colspan="3">
                <@accountingTagsSelect tag=tag prefix="acctgTagEnumId" />
              </td>
            </tr>
          </#list>
        </form>
      </#if>
    </table>
  </#if>
