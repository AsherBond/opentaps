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

<script type="text/javascript">
/*<![CDATA[*/
var isFullTextOpen = false;

function showAgreementFullText() {
    opentaps.expandCollapse('agreementText');
    var showButton = document.getElementById('showFullTextButton');
    if (showButton != null) {
        if (isFullTextOpen) {
            showButton.firstChild.nodeValue = '${uiLabelMap.OpentapsViewTextData?js_string}';
            isFullTextOpen = false;
        } else {
            showButton.firstChild.nodeValue = '${uiLabelMap.OpentapsHideTextData?js_string}';
            isFullTextOpen = true;
        }
    }
}

function createAndAppendInputCells(/* String */ title, /* Element */ row, /* Element */ tbody) {

    // Title cell
    var titleCell = opentaps.createTableCell(null, 'titleCell');
    var titleSpan = opentaps.createSpan(null, title, 'tableheadtext');
    titleCell.appendChild(titleSpan);
    row.appendChild(titleCell);

    // Input cell
    var inputCell = opentaps.createTableCell(null, null, null, null, 2);

    return inputCell;
}

function createAndAppendSelect(/* String */ title, /* String */ fieldName, /* Element */ tbody, /* Array */ options, /* String */ selectedValue) {

    var row = opentaps.createTableRow();

    // Input cell
    var inputCell = createAndAppendInputCells(title, row, tbody);
    var input = opentaps.createSelect(null, fieldName, 'inputBox', options, selectedValue);
    inputCell.appendChild(input);
    row.appendChild(inputCell);

    tbody.appendChild(row);
}

function createAndAppendLookup(/* String */ title, /* String */ fieldName, /*String*/formName, /*String*/lookupName, /* Element */ tbody, /* Number */ fieldSize, /* String */ fieldWidth) {

    var row = opentaps.createTableRow();

    // Input cell
    var inputCell = createAndAppendInputCells(title, row, tbody);
    var input = opentaps.createInput(null, fieldName, 'text', 'inputBox');
    if (fieldSize) input.size = fieldSize;
    if (fieldWidth) input.style.width = fieldWidth;
    inputCell.appendChild(input);
    var href = "javascript:call_fieldlookup2(document." + formName + "." + fieldName + ", '" + lookupName + "');";
    var anchor = opentaps.createAnchor(null, href);
    var icon = opentaps.createImg(null, '/images/fieldlookup.gif', null, null, 'Lookup');
    anchor.appendChild(icon);
    inputCell.appendChild(anchor);
    row.appendChild(inputCell);

    tbody.appendChild(row);
}

function createAndAppendTextInput(/* String */ title, /* String */ fieldName, /* Element */ tbody, /* Number */ fieldSize, /* String */ fieldWidth) {

    var row = opentaps.createTableRow();

    // Input cell
    var inputCell = createAndAppendInputCells(title, row, tbody);
    var input = opentaps.createInput(null, fieldName, 'text', 'inputBox');
    if (fieldSize) input.size = fieldSize;
    if (fieldWidth) input.style.width = fieldWidth;
    inputCell.appendChild(input);
    row.appendChild(inputCell);

    tbody.appendChild(row);
}

// Handle AJAX response
function onFormLoad(resp) {
    if (!resp) return;

    var tbody = document.getElementById("flexForm_" + resp.item);
    if (!tbody) return;
    opentaps.removeChildNodes(tbody);

    var fields = resp.fields;
    if (!fields) {
        alert('${uiLabelMap.OpentapsAgreementTermEmptyResponse?js_string}');
    }

    for (var i = 0; i < fields.length; i++) {
        var fieldName = fields[i];
        if ('termDays' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermDays?js_string}', fieldName, tbody, 8);
        } else if ('termValue' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermValue?js_string}', fieldName, tbody, 8);
        } else if ('textValue' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermText?js_string}', fieldName, tbody, null, '25%');
        } else if ('productCategoryId' == fieldName) {
            createAndAppendLookup('${uiLabelMap.OpentapsAgreementTermProdCatId?js_string}', fieldName, 'createAgreementTerm_' + resp.item, 'LookupProductCategory', tbody, 20);
        } else if ('minQuantity' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermMinQty?js_string}', fieldName, tbody, 8);
        } else if ('maxQuantity' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermMaxQty?js_string}', fieldName, tbody, 8);
        } else if ('description' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermDescription?js_string}', fieldName, tbody, null, '25%');
        } else if ('partyClassificationGroupId' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermClsGroupId?js_string}', fieldName, tbody, 20);
        } else if ('partyId' == fieldName) {
            createAndAppendLookup('${uiLabelMap.OpentapsAgreementTermPartyId?js_string}', fieldName, 'createAgreementTerm_' + resp.item, 'LookupPartyName', tbody, 20);
        } else if ('roleTypeId' == fieldName) {
            createAndAppendTextInput('${uiLabelMap.OpentapsAgreementTermRoleTypeId?js_string}', fieldName, tbody, 20);
        } else if ('productId' == fieldName) {
            createAndAppendLookup('${uiLabelMap.OpentapsAgreementTermProductId?js_string}', fieldName, 'createAgreementTerm_' + resp.item, 'LookupProduct', tbody, 20);
        } else if ('currencyUomId' == fieldName) {
            var defaultCurrencyId = resp.defaultCurrencyId;
            if (defaultCurrencyId == null) defaultCurrencyId = 'USD';
            var currencies = resp.currencies;
            var options = new Array();
            for (var n = 0; n < currencies.length; n++) {
                options.push(currencies[n].uomId);
                options.push(currencies[n].abbreviation);
            }
            createAndAppendSelect('${uiLabelMap.OpentapsAgreementTermCurrency?js_string}', fieldName, tbody, options, defaultCurrencyId);
        } else if ('valueEnumId' == fieldName) {
            var title = resp.enumTitle;
            var values = resp.enumValues;
            var options = new Array();
            for (var n = 0; n < values.length; n++) {
                options.push(values[n].enumId);
                options.push(values[n].description);
            }
            createAndAppendSelect(title, fieldName, tbody, options, null);
        } else {
            var newRow = opentaps.createTableRow();
            newRow.appendChild(opentaps.createTableCell(null, 'titleCell'));
            newRow.appendChild(opentaps.createTableCell(null, null, '${uiLabelMap.OpentapsAgreementTermNotImplemented?js_string}', null, 4));
            tbody.appendChild(newRow);
        }
    }
}

// termTypeId "onchange" event handler
function onTermTypeChanged(item) {
    if (item == null) {
        return;
    }

    var sb = document.getElementById('termType_' + item);
    if (sb && sb.value) {

        var content = {'termType' : sb.value, 'item' : item};
        opentaps.sendRequest('getAgreementTermValidFieldsJSON', content, onFormLoad, {target: 'flexFormContainer_' + item});
    }
}

// agreementItemTypeId "onchange" event handler
function onItemTypeChanged() {
    var sb = document.getElementById('agrItemType');
    if (sb && sb.value) {
        var currencyRow = document.getElementById('itemCurrencyRow');
        if (currencyRow != null) {
            if ('AGREEMENT_PRICING_PR' == sb.value) {
                opentaps.removeClass(currencyRow, 'hidden');
            } else {
                opentaps.addClass(currencyRow, 'hidden');
                var curr = document.getElementById('selCurrency');
                if (curr) {
                    curr.selectedIndex = 0;
                }
            }
        }
    }
}

/*]]>*/
</script>

<#assign agreementId = agreementHeader.agreementId/>

<#--
 Local macro library. Format different agreement terms.
 -->

 <#-- Renders buttons for individual terms or empty cells -->
<#macro RenderButtons agreementTermId="">
  <#if agreementTermId?if_exists?has_content && isEditable>
    <td style="width: 70px; text-align: right"><@inputSubmit title="${uiLabelMap.CommonUpdate}"/></td>
    <td style="width: 70px; text-align: right"><@submitFormLinkConfirm form="${removeTermTargetAction}_${agreementId}_${agreementTermId}" text=uiLabelMap.CommonRemove class="buttonDangerous" /></td>
  <#else>
    <td style="width: 70px">&nbsp;</td>
    <td style="width: 70px">&nbsp;</td>
  </#if>
</#macro>

<#-- Renders "Commission for all products in a category" term row -->
<#macro ProdGrpCommissionTerm agreementTerm="" index="">
    <tr class="${tableRowClass(index)}">
        <@displayCell text=agreementTerm.termTypeDescription blockStyle="width: 40%" />
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_termValue}
            <input name="termValue" type="text" size="8" class="inputBox" value="${agreementTerm.termValue?if_exists}" />
        </td>
        <@RenderButtons agreementTermId=agreementTerm.agreementTermId />
    </tr>
    <tr class="${tableRowClass(index)}">
        <td rowspan="3" valign="top" style="width: 40%">
            <textarea rows="3" cols="60" name="description" class="inputBox">${agreementTerm.description?default("")}</textarea>
        </td>
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_productCategoryId}
            <input type="text" size="20" maxlength="20" name="productCategoryId" class="inputBox" value="${agreementTerm.productCategoryId?if_exists}">
            <a href="javascript:call_fieldlookup2(document.termRowForm_${agreementTerm.agreementTermId}.productCategoryId,'LookupProductCategory');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="16" width="16"></a>
        </td>
        <@RenderButtons />
    </tr>
    <tr class="${tableRowClass(index)}">
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_minQuantity}
            <input name="minQuantity" type="text" size="8" class="inputBox" value="${agreementTerm.minQuantity?if_exists}" />
        </td>
        <@RenderButtons />
    </tr>
    <tr class="${tableRowClass(index)}">
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_maxQuantity}
            <input name="maxQuantity" type="text" size="8" class="inputBox" value="${agreementTerm.maxQuantity?if_exists}" />
        </td>
        <@RenderButtons />
    </tr>
</#macro>

<#-- Renders "Commission for each product in a category" term row -->
<#macro ProdCatCommissionTerm agreementTerm="" index="">
    <tr class="${tableRowClass(index)}">
        <@displayCell text=agreementTerm.termTypeDescription blockStyle="width: 40%" />
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_termValue}
            <input name="termValue" type="text" size="8" class="inputBox" value="${agreementTerm.termValue?if_exists}" />
        </td>
        <@RenderButtons agreementTermId=agreementTerm.agreementTermId />
    </tr>
    <tr class="${tableRowClass(index)}">
        <td rowspan="3" valign="top" style="width: 40%">
            <textarea rows="3" cols="60" name="description" class="inputBox">${agreementTerm.description?default("")}</textarea>
        </td>
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_productCategoryId}
            <input type="text" size="20" maxlength="20" name="productCategoryId" class="inputBox" value="${agreementTerm.productCategoryId?if_exists}">
            <a href="javascript:call_fieldlookup2(document.termRowForm_${agreementTerm.agreementTermId}.productCategoryId,'LookupProductCategory');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="16" width="16"></a>
        </td>
        <@RenderButtons />
    </tr>
    <tr class="${tableRowClass(index)}">
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_minQuantity}
            <input name="minQuantity" type="text" size="8" class="inputBox" value="${agreementTerm.minQuantity?if_exists}" />
        </td>
        <@RenderButtons />
    </tr>
    <tr class="${tableRowClass(index)}">
        <td>
            ${uiLabelMap.OpentapsAgreementTermName_maxQuantity}
            <input name="maxQuantity" type="text" size="8" class="inputBox" value="${agreementTerm.maxQuantity?if_exists}" />
        </td>
        <@RenderButtons />
    </tr>
</#macro>

<#--
    Agreement Header form
-->
<div class="subSectionBlock">
    <div class="form">
        <table width="100%">
            <tr>
                <@displayTitleCell title="${uiLabelMap.CommonType}"/>
                <#if agreementType?has_content><@displayCell text=agreementType.get("description", locale)/><#else><td>&nbsp;</td></#if>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <@displayTitleCell title="${uiLabelMap.CommonStatus}"/>
                <#if status?has_content><@displayCell text=status.get("description", locale)/><#else><td>&nbsp;</td></#if>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <@displayTitleCell title="${uiLabelMap.AccountingFromParty}" />
                <@displayPartyLinkCell partyId=agreementHeader.partyIdFrom/>

                <#if partyNameTo?has_content>
                    <@displayTitleCell title="${uiLabelMap.AccountingToParty}"/>
                <#elseif agreementHeader.toPartyClassGroupId?has_content>
                    <@displayTitleCell title="${uiLabelMap.CommonTo}&nbsp;${uiLabelMap.PartyClassificationGroup}"/>
                </#if>
                <td>
                    <#if partyNameTo?has_content>
                      <@displayPartyLink partyId=agreementHeader.partyIdTo/>
                    <#elseif agreementHeader.toPartyClassGroupId?has_content>
                        <#if partyClsGroup?has_content>
                            <@display text=partyClsGroup.get("description", locale)/>
                        </#if>
                    </#if>
                </td>
            </tr>
            <tr>
                <@displayTitleCell title="${uiLabelMap.CommonFromDate}" />
                <@displayDateCell date=agreementHeader.fromDate?if_exists format="DATE"/>
                <@displayTitleCell title="${uiLabelMap.CommonThruDate}"/>
                <@displayDateCell date=agreementHeader.thruDate?if_exists format="DATE"/>
            </tr>
            <tr>
                <@displayTitleCell title="${uiLabelMap.CommonDescription}"/>
                <td colspan="3"><@display text=agreementHeader.description/></td>
            </tr>
            <tr><td colspan="4">&nbsp;</td></tr>
            <tr>
                <td></td>
                <td>
                   <#if agreementHeader.textData?has_content>
                      <a id="showFullTextButton" href="javascript:showAgreementFullText();" class="buttontext">${uiLabelMap.OpentapsViewTextData}</a>
                   <#else>
                      ${uiLabelMap.OpentapsNoTextData}
                   </#if>
                </td>
                <td colspan="2">&nbsp;</td>
            </tr>
        </table>
    </div>
</div>

<@flexArea targetId="agreementText" title="" controlClassClosed="hidden" controlClassOpen="subSectionBlock" style="border:none; padding:0; margin-right:0" state="closed" save="true" enabled="false">
<div id="fullTextSubsectionContainer" style="margin-left:-10px;">
    <@sectionHeader title="${uiLabelMap.OpentapsTextData}"/>
    <div id="fullTextFormContainer" class="form">
        <table width="100%">
            <tr>
                <td>
                    <textarea rows="15" name="agreementFullText" class="inputBox" style="width: 100%;" readonly="readonly">${agreementHeader.textData?if_exists}</textarea>
                </td>
            </tr>
        </table>
    </div>
</div>
</@flexArea>

<#--
    List of Agreement Items and Terms
    IMPORTANT NOTE! Don't use macros if you add code for new term. This can provoke undesirable result.
-->
<#list agreementItems as item>

<#assign agreementItemSeqId = item.agreementItemSeqId/>
<#assign agreementItemTypeId = item.agreementItemTypeId/>

<div class="subSectionBlock"></div>
<div class="subSectionBlock">
    <#assign agreementItemTitle = item.get("description")/>
    <#if agreementItemTypeId == "AGREEMENT_PRICING_PR">
       <#assign agreementItemTitle = agreementItemTitle + " " + uiLabelMap.CommonIn + " " + item.get("currencyUomId")!/>
    </#if>
    <@sectionHeader title=agreementItemTitle>
      <#if isEditable>
        <#assign removeItemTargetAction = removeItemAction?default("processRemoveAgreementItem")/>
        <@form name="${removeItemTargetAction}Action_${item_index}" url=removeItemTargetAction agreementId=agreementId agreementItemSeqId=agreementItemSeqId />
        <div class="subMenuBar"><@submitFormLinkConfirm form="${removeItemTargetAction}Action_${item_index}" text=uiLabelMap.CommonRemove class="subMenuButtonDangerous" /></div>
      </#if>
    </@sectionHeader>
    <table class="listTable" cellspacing="0" cellpadding="2">
    <#assign showType = true/>
    <#list agreementTerms as term>
      <#if term.agreementId == parameters.agreementId && term.agreementItemSeqId?default("0") == item.agreementItemSeqId>

        <#if enumData?has_content>
          <#list enumData as termTypeEnum>
            <#if termTypeEnum.termTypeId == term.termTypeId>
              <#assign enumTitle = termTypeEnum.enumTitle/>
              <#assign enumValues = termTypeEnum.enumValues/>
            </#if>
          </#list>
        </#if>

        <#assign updateTermTargetAction = updateTermAction?default("processUpdateAgreementTerm")/>
        <#assign removeTermTargetAction = removeTermAction?default("processRemoveAgreementTerm")/>

        <#if isEditable>
          <#-- prepare delete forms -->
          <@form name="${removeTermTargetAction}_${term.agreementId}_${term.agreementTermId}" url=removeTermTargetAction agreementId=term.agreementId agreementTermId=term.agreementTermId />
          <form name="termRowForm_${term.agreementTermId}" method="POST" action="${updateTermTargetAction}">
        </#if>
        <#assign validValues = Static["org.opentaps.common.agreement.UtilAgreement"].getValidFields(term.termTypeId)?default([])/>
        <#assign listSize = validValues.size()/>
        <#assign firstColumnWidth = 40/> <#-- relative width for 1st column in percent-->

        <#if term.termTypeId == "PROD_CAT_COMMISSION">
          <@ProdCatCommissionTerm agreementTerm=term index=term_index />
        <#elseif term.termTypeId == "PROD_GRP_COMMISSION">
          <@ProdGrpCommissionTerm agreementTerm=term index=term_index />
        <#else>
          <#list validValues as vvItem>
            <#if vvItem != "currencyUomId">
              <tr class="${tableRowClass(term_index)}">

                <#-- What size must have input text box? -->
                <#assign defaultFieldSize = "25%"/>
                <#if vvItem == "termDays" || vvItem == "termValue" || vvItem == "minQuantity" || vvItem == "maxQuantity">
                  <#assign fieldSize = 8/>
                <#elseif vvItem == "productCategoryId" || vvItem == "productId" || vvItem == "roleTypeId" || vvItem == "partyId" || vvItem == "partyClassificationGroupId">
                  <#assign fieldSize = 20/>
                <#else>
                  <#assign fieldSize = 30/>
                </#if>

                <#if vvItem_index == 0> <#-- First row of term with multile values must have term desc -->
                  <@displayCell text=term.termTypeDescription blockStyle="width: ${firstColumnWidth}%"/>
                <#else>
                  <td style="width: ${firstColumnWidth}%">&nbsp;</td>
                </#if>

                <#-- this formatting needs to be very precisely controlled, so use direct html instead of a form macro -->
                <td>
                  <#-- only some term types need to have their descriptive (min/max/days) displayed, and then where it is displayed depends on the language
                                syntatically this might be optimized just for English: perhaps someone can make it better -->
                  <#if (vvItem == "valueEnumId")>
                    ${enumTitle?if_exists}
                    <@inputSelect name="${vvItem}" list=enumValues?default([]) key="enumId" default=term.valueEnumId readonly=!isEditable ; option>
                      ${option.get("description", "OpentapsEntityLabels", locale)}
                    </@inputSelect>
                  <#elseif (vvItem == "partyId")>
                    ${uiLabelMap.get("OpentapsAgreementTermName_${vvItem}")}
                    <@inputLookup name="partyId" default="${term.partyId?if_exists}" lookup="LookupPartyName" form="termRowForm_${term.agreementTermId}" size=20 maxlength=20 ignoreParameters=true readonly=!isEditable/>
                  <#elseif (vvItem == "productId")>
                    ${uiLabelMap.get("OpentapsAgreementTermName_${vvItem}")}
                    <@inputLookup name="productId" default="${term.productId?if_exists}" lookup="LookupProduct" form="termRowForm_${term.agreementTermId}" size=20 maxlength=20 ignoreParameters=true readonly=!isEditable/>
                  <#elseif (vvItem == "productCategoryId")>
                    ${uiLabelMap.get("OpentapsAgreementTermName_${vvItem}")}
                    <@inputLookup name="productCategoryId" default="${term.productCategoryId?if_exists}" lookup="LookupProductCategory" form="termRowForm_${term.agreementTermId}" size=20 maxlength=20 ignoreParameters=true readonly=!isEditable/>
                  <#else>
                    <#if (vvItem == "termValue") || (vvItem == "minQuantity") || (vvItem == "maxQuantity") || (vvItem == "roleTypeId")>
                      ${uiLabelMap.get("OpentapsAgreementTermName_${vvItem}")}
                    </#if>
                    <@inputText name="${vvItem}" size="${fieldSize}" default="${term.get(vvItem)?if_exists}" ignoreParameters=true readonly=!isEditable/>
                    <#if vvItem == "termValue" && validValues?seq_contains("currencyUomId")>
                      <#assign currencyDefault = term.get("currencyUomId")?default(parameters.orgCurrencyUomId) />
                      <#if currencyDefault?length == 0>
                        <#assign currencyDefault = Static["org.opentaps.common.util.UtilConfig"].getPropertyValue(opentapsApplicationName?default("opentaps"), "defaultCurrencyUomId")?default("USA") />
                      </#if>
                      <#assign currencies = Static["org.opentaps.common.util.UtilCommon"].getCurrencies(delegator) />
                      <@inputSelect name="currencyUomId" list=currencies?default([]) key="uomId" default=currencyDefault readonly=!isEditable ; option>
                        ${option.abbreviation}
                      </@inputSelect>
                    </#if>
                    <#if (vvItem == "termDays")>
                      ${uiLabelMap.get("OpentapsAgreementTermName_${vvItem}")}
                    </#if>
                  </#if>
                </td>

                <#if vvItem_index == 0> <#-- ... and buttons -->
                  <@RenderButtons agreementTermId=term.agreementTermId />
                <#else>
                  <@RenderButtons />
                </#if>

                <#assign fieldSize = defaultFieldSize/>

              </tr>
            </#if> <#-- if vvItem != "currencyUomId" -->
          </#list> <#-- validValues as vvItem -->
        </#if> <#-- term.termTypeId -->

        <#if isEditable>
          <#-- hidden fields -->
          <@inputHidden name="agreementTermId" value=term.agreementTermId/>
          <@inputHidden name="agreementId" value=agreementId/>
          <@inputHidden name="agreementItemSeqId" value=agreementItemSeqId/>
          <@inputHidden name="agreementItemTypeId" value=agreementItemTypeId/>
          </form>
        </#if>
      </#if>
    </#list> <#-- agreementTerms -->
    </table>

    <#-- Agreement Item Text Notes -->
    <#if item.agreementText?has_content>
        <div style="margin-bottom: 17px;">
            <@flexArea targetId="itemTextFlexArea_${agreementItemSeqId}" title="${uiLabelMap.OpentapsTextData}" controlStyle="font-size: 10pt;" >
                <textarea rows="3" name="agreementText" class="inputBox" style="border: 0px; width: 100%;">${item.agreementText}</textarea>
            </@flexArea>
        </div>
    </#if>

    <#--  Form to add new agreement term -->
    <#assign listTermTypes = Static["org.opentaps.common.agreement.UtilAgreement"].getTermsByItemType(agreementItemTypeId, agreementId, agreementItemSeqId, delegator)/>
    <#if hasCreateAgreementPermission && listTermTypes?has_content && isEditable>
    <div class="form">
        <#assign createTermTargetAction = createTermAction?default("processCreateAgreementTerm")/>
        <form name="createAgreementTerm_${agreementItemSeqId}" method="POST" action="<@ofbizUrl>${createTermTargetAction}</@ofbizUrl>">
            <@inputHidden name="agreementId" value=agreementId/>
            <@inputHidden name="agreementItemSeqId" value=agreementItemSeqId/>
            <div id="flexFormContainer_${agreementItemSeqId}">
              <table width="100%">
                  <tr>
                      <@displayCell text="Term Type" blockClass="titleCell" class="tableheadtext"/>
                      <td>
                          <select id="termType_${agreementItemSeqId}" name="termTypeId" class="inputBox" onchange="onTermTypeChanged(${agreementItemSeqId});">
                          <#list listTermTypes?default([]) as termType>
                              <option value="${termType.get("termTypeId")}">${termType.get("description", locale)}</option>
                          </#list>
                          </select>
                      </td>
                  </tr>
              </table>
              <table width="100%">
                <tbody id="flexForm_${agreementItemSeqId}">
                  <#--
                      variable part of form (depended on termTypeId) requests via AJAX call
                      and inserts children of flexForm_*
                  -->
                </tbody>
              </table>
              <table width="100%">
                  <tr>
                      <td class="titleCell"></td>
                      <@inputSubmitCell title="${uiLabelMap.AccountingNewAgreementTerm}"/>
                  </tr>
              </table>
          </div>
        </form>
    </div>

    <script  type="text/javascript">
      /*<![CDATA[*/
      onTermTypeChanged('${agreementItemSeqId?js_string}');
      /*]]>*/
    </script>

    </#if>
</div>
</#list>

<#--
    Create Agreement Item form
-->
<#if isEditable && allowItems?has_content>
<div class="subSectionBlock">
        <#assign createItemTargetAction = createItemAction?default("processCreateAgreementItem")/>
        <form name="createAgreementItem" method="POST" action="<@ofbizUrl>${createItemTargetAction}</@ofbizUrl>">
            <@inputHidden name="agreementId" value=parameters.agreementId/>
            <table class="twoColumnForm">
                <tr>
                    <@displayCell text="${uiLabelMap.CommonType}" class="tableheadtext" blockClass="titleCell"/>
                    <td>
                        <select id="agrItemType" name="agreementItemTypeId" class="inputBox" onchange="onItemTypeChanged();">
                        <#list allowItems?default([]) as allowItem>
                            <option value="${allowItem.get("agreementItemTypeId")}">${allowItem.get("description", locale)}</option>
                        </#list>
                        </select>
                    </td>
                </tr>
                <tr id="itemCurrencyRow" class="hidden">
                    <@displayCell text="${uiLabelMap.CommonCurrency}" blockClass="titleCell" class="tableheadtext"/>
                    <td>
                        <#assign currencies = Static["org.opentaps.common.util.UtilCommon"].getCurrencies(delegator)?default([])/>
                        <select id="selCurrency" name="currencyUomId" class="inputBox">
                        <option value=""></option>
                        <#list currencies as currency>
                            <option value="${currency.uomId}">${currency.uomId} - ${currency.get("description", locale)}</option>
                        </#list>
                        </select>
                    </td>
                </tr>
                <@inputTextareaRow title="${uiLabelMap.FormFieldTitle_agreementText}" name="agreementText"/>
                <@inputSubmitRow title="${uiLabelMap.AccountingNewAgreementItem}"/>
            </table>
        </form>
</div>

<script  type="text/javascript">
/*<![CDATA[*/
onItemTypeChanged();
/*]]>*/
</script>

</#if>
