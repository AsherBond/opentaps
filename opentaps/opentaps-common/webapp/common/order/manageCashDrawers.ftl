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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

<#macro displayCashDrawerTransaction cashDrawerTrans index=-1>
    <tr class="${tableRowClass(index)}">
      <@displayDateCell date=cashDrawerTrans.effectiveDate format="DATE"/>
      <@displayLinkCell href="viewPayment?paymentId=${cashDrawerTrans.paymentId}" text="${cashDrawerTrans.paymentId}" target="_blank"/>
      <@displayCell text=cashDrawerTrans.customerName?if_exists/>
      <@displayCell text=cashDrawerTrans.description?if_exists/>
      <@displayCell text=cashDrawerTrans.paymentRefNum?if_exists/>
      <td class="currencyCell">
        <@displayCurrency currencyUomId=cashDrawerTrans.currencyUomId?if_exists amount=cashDrawerTrans.amount?default(0)/>
      </td>
    </tr>
</#macro>

<#macro listCashDrawerTransactions cashDrawer>
  <table class="listTable" cellspacing="0" cellpadding="2" style="margin-bottom: 15px">
    <tbody>
      <tr class="listTableHeader" style="background-color:white">
        <td><span class="tableHeadText">${uiLabelMap.CommonDate}</span></td>
        <td><span class="tableHeadText">${uiLabelMap.OpentapsPaymentId}</span></td>
        <td><span class="tableHeadText">${uiLabelMap.OpentapsCustomer}</span></td>
        <td><span class="tableHeadText">${uiLabelMap.CommonMethod}</span></td>
        <td><span class="tableHeadText">${uiLabelMap.OpentapsPaymentRefNum}</span></td>
        <td class="currencyCell"><span class="tableHeadText">${uiLabelMap.CommonAmount}</span></td>
      </tr>
      <#-- display opening balance -->
      <tr>
        <td class="tabletext" colspan="5">${cashDrawer.openTimestamp?string.short}</td>
        <td class="currencyCell"><b><@displayCurrency currencyUomId=cashDrawer.currencyUomId amount=cashDrawer.initialAmount?default(0)/></b></td>
      </tr>
      <#-- show all cash transactions -->
      <#list cashDrawer.cashTransactions?default([]) as trans>
         <@displayCashDrawerTransaction cashDrawerTrans=trans index=cashDrawerTrans_index/>
      </#list>
      <#if (cashDrawer.closingVarianceAmount?has_content) && (cashDrawer.closingVarianceAmount != 0)>
      <tr>
          <td class="tabletext">${cashDrawer.closeTimestamp?string.short}</td>
          <td class="tabletext" colspan="4">${uiLabelMap.OpentapsCashDrawerVariance}</td>
          <td class="currencyCell"><b><@displayCurrency currencyUomId=cashDrawer.currencyUomId amount=cashDrawer.closingVarianceAmount?default(0)/></b></td>
      </tr>
      </#if>
      <#-- show the final balance This should be the same as the closed balance if it is closed -->
      <tr>
          <td class="tabletext" colspan="5">${cashDrawer.closeTimestamp?default(now)?string.short}</td>
          <#assign finalBalance = cashDrawer.balance?default(0) + cashDrawer.closingVarianceAmount?default(0)/>
          <td class="currencyCell"><b><@displayCurrency currencyUomId=cashDrawer.currencyUomId amount=finalBalance/></b></td>
      </tr>
      <tr><td>&nbsp;</td></tr>
      <#-- now list all non-cash transactions -->
      <#list cashDrawer.otherTransactions?default([]) as trans>
         <@displayCashDrawerTransaction cashDrawerTrans=trans index=cashDrawerTrans_index/>
      </#list>
    </tbody>
  </table>
</#macro>

<div style="margin-bottom: 15px">
  <@flexArea targetId="openCashDrawer" title=uiLabelMap.OpentapsCashDrawerOpen save=false state="closed">
    <form method="post" action="<@ofbizUrl>getOrCreateCashDrawer</@ofbizUrl>" name="createCashDrawer">
      <table>
        <@inputDateTimeRow title=uiLabelMap.OpentapsCashDrawerOpenTime name="openTimestamp" form="createCashDrawer" default=now/>
        <@inputLookupRow title=uiLabelMap.PartyUserLogin name="operatorUserLoginId" lookup="LookupUserLoginAndPartyDetails" form="createCashDrawer"/>
        <@inputCurrencyRow name="initialAmount" title=uiLabelMap.OpentapsCashDrawerInitialAmount list=currencies currencyName="currencyUomId"/>
        <@inputTextareaRow name="openingComments" title=uiLabelMap.OpentapsCashDrawerOpenComments/> 
        <@inputSubmitRow title=uiLabelMap.OpentapsOpen />
      </table>
    </form>
  </@flexArea>
</div>

<div style="margin-bottom: 15px">
  <@flexArea targetId="activeCashDrawers" title=uiLabelMap.OpentapsCashDrawerActive style="border:0" save=true defaultState="open">
    <table class="listTable" cellspacing="0" style="margin-top:15px; margin-bottom: 15px">
      <tbody>
        <tr class="listTableHeader">
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerOpened}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerOpenedBy}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerUser}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerOpenComments}</span></td>
          <td class="currencyCell"><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerInitial}</span></td>
          <td class="currencyCell"><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerCurrent}</span></td>
          <td>&nbsp;</td>
        </tr>
        <#list activeCashDrawers?default([]) as cashDrawer>
          <#assign mustForceClosed = (cashDrawer.cashDrawerId == mustForceCloseCashDrawerId?default(""))/>
          <#assign state = mustForceClosed?string("open", "closed")/>
          <tr class="${tableRowClass(cashDrawer_index)}">
            <@displayCell text=cashDrawer.openTimestamp?default("")?string.short/>
            <@displayCell text=cashDrawer.openUserLoginId?if_exists/>
            <@displayCell text=cashDrawer.operatorUserLoginId?if_exists/>
            <@displayCell text=cashDrawer.openingComments?if_exists/>
            <@displayCurrencyCell currencyUomId=cashDrawer.currencyUomId?if_exists amount=cashDrawer.initialAmount?default(0)/>
            <@displayCurrencyCell currencyUomId=cashDrawer.currencyUomId?if_exists amount=cashDrawer.balance?default(0)/>
            <td>
              <@displayLink href="javascript:opentaps.expandCollapse('openCashDrawer_${cashDrawer.cashDrawerId}')" text=uiLabelMap.OpentapsShowHideDetails/>
              <@displayLink id="closeFormLink_${cashDrawer.cashDrawerId}" href="javascript:opentaps.expandCollapse('closeCashDrawer_${cashDrawer.cashDrawerId}');opentaps.addClass(document.getElementById('closeFormLink_${cashDrawer.cashDrawerId}'),'hidden')" text=uiLabelMap.CommonClose class="buttontext ${mustForceClosed?string(' hidden','')}"/>
            </td>
          </tr>
          <tr class="${tableRowClass(cashDrawer_index)}">
            <td colspan="7">
                <@flexArea title="" targetId="closeCashDrawer_${cashDrawer.cashDrawerId}" style="border:none" controlClassOpen="hidden" controlClassClosed="hidden" state="${state}" enabled=false>
                  <form method="post" action="<@ofbizUrl>closeCashDrawer</@ofbizUrl>" name="closeCashDrawer_${cashDrawer.cashDrawerId}">
                    <@inputHidden name="cashDrawerId" value="${cashDrawer.cashDrawerId}"/>
                    <table>
                      <#if mustForceClosed>
                        <@inputHidden name="finalAmount" value="${parameters.finalAmount?default(0)}"/>
                        <@inputHidden name="forceClose" value="true"/>
                        <tr>
                          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsCashDrawerFinalCashAmount}</span></td>
                          <td>
                            <@displayCurrency currencyUomId=cashDrawer.currencyUomId amount=parameters.finalAmount?default(0)/>
                          </td>
                        </tr>
                        <tr>
                          <td class="titleCell"><span class="tableheadtext">${uiLabelMap.OpentapsCashDrawerVariance}</span></td>
                          <td>
                            <@displayCurrency currencyUomId=cashDrawer.currencyUomId amount=cashVariance?default(0)/>
                          </td>
                        </tr>
                        <@inputTextareaRow name="closingComments" title=uiLabelMap.OpentapsCashDrawerCloseComments/>
                        <tr>
                          <td>&nbsp;</td>
                          <td>
                            <@inputConfirm title=uiLabelMap.OpentapsCashDrawerForceClose form="closeCashDrawer_${cashDrawer.cashDrawerId}"/>&nbsp;
                            <@inputConfirm title=uiLabelMap.OpentapsCashDrawerCancelClose href="manageCashDrawers"/>
                          </td>
                        </tr>
                      <#else>
                        <@inputTextRow title=uiLabelMap.OpentapsCashDrawerFinalCashAmount name="finalAmount" size=10/>
                        <@inputTextareaRow name="closingComments" title=uiLabelMap.OpentapsCashDrawerCloseComments/>
                        <tr>
                          <td>&nbsp;</td>
                          <td>
                            <@inputConfirm title=uiLabelMap.OpentapsCashDrawerCloseDrawer form="closeCashDrawer_${cashDrawer.cashDrawerId}"/>
                            <@displayLink href="javascript:opentaps.expandCollapse('closeCashDrawer_${cashDrawer.cashDrawerId}');opentaps.removeClass(document.getElementById('closeFormLink_${cashDrawer.cashDrawerId}'),'hidden')" text=uiLabelMap.OpentapsCashDrawerCancelClose class="buttontext"/>
                          </td>
                        </tr>
                      </#if>
                    </table>
                  </form>
                </@flexArea>
            </td>
          </tr>
          <tr class="${tableRowClass(cashDrawer_index)}" style="margin-top:15px;">
            <td colspan="7">
              <@flexArea title="" targetId="openCashDrawer_${cashDrawer.cashDrawerId}" style="border:none;margin-top:15px;" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false>
                <@listCashDrawerTransactions cashDrawer=cashDrawer/>
              </@flexArea>
            </td>
          </tr>
        </#list>
      </tbody>
    </table>
  </@flexArea>
</div>

<div style="margin-bottom: 15px">
  <@flexArea targetId="closedCashDrawers" title=uiLabelMap.OpentapsClosedCashDrawers style="border:0" save=true>
    <form method="post" action="<@ofbizUrl>manageCashDrawers</@ofbizUrl>" name="findPastCashDrawers">
      <table>
        <@inputHidden name="performFind" value="Y"/>
        <@inputLookupRow title=uiLabelMap.PartyUserLogin name="userLoginId" lookup="LookupUserLoginAndPartyDetails" form="findPastCashDrawers"/>
        <@inputSubmitRow title=uiLabelMap.CommonFind />
      </table>
    </form>
    <#if closedCashDrawers?default([])?has_content>
      <table class="listTable" cellspacing="0">
        <tbody>
        <tr class="listTableHeader">
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerOpened}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerClosed}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerOpenedBy}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerUser}</span></td>
          <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerClosedBy}</span></td>
          <td class="currencyCell"><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerInitial}</span></td>
          <td class="currencyCell"><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerFinal}</span></td>
          <td class="currencyCell"><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerVariance}</span></td>
          <td>&nbsp;</td>
        </tr>
          <#list closedCashDrawers as cashDrawer>
            <tr class="${tableRowClass(cashDrawer_index)}">
              <@displayCell text=cashDrawer.openTimestamp?default("")?string.short/>
              <@displayCell text=cashDrawer.closeTimestamp?default("")?string.short/>
              <@displayCell text=cashDrawer.openUserLoginId?if_exists/>
              <@displayCell text=cashDrawer.operatorUserLoginId?if_exists/>
              <@displayCell text=cashDrawer.closeUserLoginId?if_exists/>
              <@displayCurrencyCell currencyUomId=cashDrawer.currencyUomId?if_exists amount=cashDrawer.initialAmount?if_exists/>
              <@displayCurrencyCell currencyUomId=cashDrawer.currencyUomId?if_exists amount=cashDrawer.finalAmount?if_exists/>
              <@displayCurrencyCell currencyUomId=cashDrawer.currencyUomId?if_exists amount=cashDrawer.closingVarianceAmount?if_exists/>
              <@displayLinkCell href="javascript:opentaps.expandCollapse('openCashDrawer_${cashDrawer.cashDrawerId}');" text=uiLabelMap.OpentapsShowHideDetails/>
            </tr>
            <tr class="${tableRowClass(cashDrawer_index)}">
              <td colspan="9">
                <@flexArea title="" targetId="openCashDrawer_${cashDrawer.cashDrawerId}" style="border:none" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false>
                  <table cellspacing="0" style="margin-top:15px;">
                    <tr>
                      <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerOpenComments}:</span></td>
                      <td>${cashDrawer.openingComments?if_exists}</td>
                    </tr>
                    <tr>
                      <td><span class="tableHeadText">${uiLabelMap.OpentapsCashDrawerCloseComments}:</span></td>
                      <td>${cashDrawer.closingComments?if_exists}</td>
                    </tr>
                  </table>
                  <@listCashDrawerTransactions cashDrawer=cashDrawer/>
                </@flexArea>
              </td>
            </tr>
          </#list>
        </tbody>
      </table>
    </#if>
  </@flexArea>
</div>
