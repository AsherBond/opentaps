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
-->

<#-- 
To use these macros in your page, first put this at the top:

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

Then each one can be used as a macro right in an FTL like this:
<@displayCurrency amount=10.00 currencyUomId="USD" />

For more information, please see documentation/opentapsFormMacros.html
-->


<@import location="component://opentaps-common/webapp/common/includes/lib/flexAreaMacros.ftl"/>


<#-- ###################################################################### -->
<#-- ###                                                                 ## -->
<#-- ###                Atomic "Building Block" Macros                   ## -->
<#-- ###                                                                 ## -->
<#-- ###################################################################### -->


<#-- -------------------------- -->
<#-- --    Display Macros    -- -->
<#-- -------------------------- -->


<#macro display text="" class="tabletext" style="">
  <span class="${class}" style="${style}">${text}</span>
</#macro>

<#macro displayCell text="" class="tabletext" style="" blockStyle="" blockClass="">
  <td class="${blockClass}" style="${blockStyle}"><span class="${class}" style="${style}">${text}</span></td>
</#macro>

<#macro displayTitleCell title titleClass="tableheadtext"><td class="titleCell"><span class="${titleClass}">${title}</span></td></#macro>

<#macro displayRow title text="" class="tabletext" blockClass="" titleClass="tableheadtext">
  <tr>
    <@displayTitleCell title=title titleClass=titleClass />
    <@displayCell text=text class=class blockClass=blockClass />
  </tr>
</#macro>

<#macro displayLink href text class="linktext" target="" id="">
<#compress>
  <#assign idText = id?has_content?string("id=\"" + id + "\"", "")/>
  <#if href?starts_with("/")>
    <a href="${href}<#if externalKeyParam?has_content>${StringUtil.wrapString(externalKeyParam)}</#if>" ${idText} class="${class}" target="${target}">${text}</a>
  <#elseif href?starts_with("javascript:")>
    <a href="${href}" ${idText} class="${class}">${text}</a>
  <#else>
    <a href="<@ofbizUrl>${href}</@ofbizUrl>" ${idText} class="${class}" target="${target}">${text}</a>
  </#if>
</#compress>
</#macro>

<#macro displayLinkCell href text class="linktext" blockClass="" target="" id="">
  <td class="${blockClass}"><@displayLink text=text href=href class=class target=target id=id /></td>
</#macro>

<#macro displayLinkRow href title text class="linktext" blockClass="" titleClass="tableheadtext" target="" id="">
<tr>
  <td class="titleCell"><span class="${titleClass}">${title}</span></td>
  <@displayLinkCell text=text href=href class=class blockClass=blockClass target=target id=id />
</tr>
</#macro>

<#-- for linking to a party view page in CRMSFA or party manager -->

<#macro displayPartyLink partyId text="" class="linktext" target="" id="">
  <#assign idText = id?has_content?string("id=\"" + id + "\"", "")/>
  <#assign href = Static["org.opentaps.common.party.PartyHelper"].createViewPageURL(partyId, delegator) />
  <#if href?has_content>
    <#assign href = StringUtil.wrapString(href) />
  </#if>
  <#assign name = text />
  <#if !name?has_content>
    <#assign name = Static["org.opentaps.common.party.PartyHelper"].getCrmsfaPartyName(partyId, delegator) />
  </#if>
  <@displayLink text=name href=href class=class target=target id=id />
</#macro>

<#macro displayPartyLinkCell partyId text="" class="linktext" blockClass="" target="" id="">
  <td class="${blockClass}"><@displayPartyLink text=text partyId=partyId class=class target=target id=id /></td>
</#macro>

<#macro displayPartyLinkRow partyId title text="" class="linktext" blockClass="" titleClass="tableheadtext" target="" id="">
<tr>
  <td class="titleCell"><span class="${titleClass}">${title}</span></td>
  <@displayPartyLinkCell text=text partyId=partyId class=class blockClass=blockClass target=target id=id />
</tr>
</#macro>

<#macro expandLabel label params={} >
  <#-- If locale is empty, get it from the request -->
  <#if !locale?has_content><#assign locale = Static["org.ofbiz.base.util.UtilHttp"].getLocale(request)/></#if>
  ${Static["org.opentaps.common.util.UtilMessage"].expandLabel( label, locale, params)}
</#macro>

<#-- Format should be one of the DATE_TIME, DATE, TIME, DATE_ONLY -->
<#function getLocalizedDate date="" format="DATE_TIME" encode=false>

  <#-- returns String unchanged -->
  <#if date?is_string>
  	<#return date/>
  </#if>

  <#-- If locale or timeZone are empty, get them from the request -->
  <#if !locale?has_content><#assign locale = Static["org.ofbiz.base.util.UtilHttp"].getLocale(request)/></#if>
  <#if !timeZone?has_content><#assign timeZone = Static["org.ofbiz.base.util.UtilHttp"].getTimeZone(request)/></#if>
  <#if date?has_content && date?is_date>
    <#if format == "DATE_TIME">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getDateTimeFormat(locale)/>
    <#elseif format == "DATE">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)/>
    <#elseif format == "TIME">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getTimeFormat(locale)/>
    <#elseif format == "DATE_ONLY">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)/>

      <#-- If format is DATE_ONLY, just format the date portion and return (this seems to be rendered identically to DATE)-->
      <#return date?date?string(fmt) />
    </#if>
    <#assign dateString = Static["org.ofbiz.base.util.UtilDateTime"].timeStampToString(date?datetime, fmt, timeZone, locale)/>
    <#if encode>
        <#return Static["org.ofbiz.base.util.UtilHttp"].encodeBlanks(dateString)/>
    <#else>
        <#return dateString/>
    </#if>
  <#else>
    <#return ""/>
  </#if> 
</#function>

<#macro displayDate date="" format="DATE_TIME" class="tabletext" style="" default="" compareTo="" highlightAfter=false highlightBefore=false highlightStyle="color:red;">
  <#if compareTo?has_content && compareTo?is_date && date?has_content && date?is_date && ((highlightAfter && date.after(compareTo)) || (highlightBefore && date.before(compareTo)))>
    <#assign applyStyle="${style};${highlightStyle}"/>
  <#else/>
    <#assign applyStyle=style />
  </#if>
  <span class="${class}" style="${applyStyle}"><#if date?has_content>${getLocalizedDate(date, format)}<#else>${default}</#if></span>
</#macro>

<#macro displayDateCell date="" format="DATE_TIME" class="tabletext" style="" blockStyle="" blockClass="" default="" compareTo="" highlightAfter=false highlightBefore=false highlightStyle="color:red;">
  <td class="${blockClass}" style="${blockStyle}"><@displayDate date=date format=format class=class style=style default=default compareTo=compareTo highlightAfter=highlightAfter highlightBefore=highlightBefore highlightStyle=highlightStyle/></td>
</#macro>

<#macro displayDateRow title date="" format="DATE_TIME" class="tabletext" blockClass="" titleClass="tableheadtext" default="" compareTo="" highlightAfter=false highlightBefore=false highlightStyle="color:red;">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@displayDateCell date=date format=format class=class blockClass=blockClass default=default compareTo=compareTo highlightAfter=highlightAfter highlightBefore=highlightBefore highlightStyle=highlightStyle />
  </tr>
</#macro>

 
<#macro displayCurrency currencyUomId="" amount=0 class="tabletext">
  <#if currencyUomId == "">
    <#assign isoCode = Static["org.opentaps.common.util.UtilConfig"].getPropertyValue(opentapsApplicationName?default("opentaps"), "defaultCurrencyUomId") />
  <#else>
    <#assign isoCode = currencyUomId />
  </#if>
  <span class="${class}" style="${(amount?is_number && amount < 0)?string("color: #AA0000", "")}"><@ofbizCurrency amount=amount isoCode=isoCode /></span>
</#macro>

<#macro displayCurrencyCell currencyUomId="" amount=0 class="tabletext">
  <td class="currencyCell"><@displayCurrency currencyUomId=currencyUomId amount=amount class=class /></td>
</#macro>

<#macro displayCurrencyRow title currencyUomId="" amount=0 class="tabletext" titleClass="tableheadtext">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@displayCurrencyCell currencyUomId=currencyUomId amount=amount class=class />
  </tr>
</#macro>


<#macro displayIndicator value="" class="tabletext">
  <span class="${class}"><#if value == "Y">${uiLabelMap.CommonYes}<#elseif value == "N">${uiLabelMap.CommonNo}<#else>${uiLabelMap.OpentapsUnknown}</#if></span>
</#macro>

<#macro displayIndicatorCell value="" class="tabletext" blockClass="">
  <td class="${blockClass}"><@displayIndicator value=value class=class /></td>
</#macro>

<#macro displayIndicatorRow title value="" class="tabletext" blockClass="" titleClass="tableheadtext">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@displayIndicatorCell value=value class=class blockClass=blockClass />
  </tr>
</#macro>


<#-- ------------------------ -->
<#-- --    Input Macros    -- -->
<#-- ------------------------ -->


<#function getIndexedName name index=-1>
  <#if index == -1>
    <#return name>
  <#else>
    <#return name + "_o_" + index>
  </#if>
</#function>

<#function getDefaultValue name default="" index=-1 ignoreParameters=false>
  <#assign key = getIndexedName(name, index)>
  <#if !ignoreParameters && parameters?exists && parameters.containsKey(key)>
    <#return parameters.get(key)?default("")>
  </#if>
  <#return default>
</#function>


<#macro inputHidden name value index=-1>
  <input type="hidden" name="${getIndexedName(name, index)}" value="${value}"/>
</#macro>


<#macro inputText name size=30 maxlength="" default="" index=-1 password=false readonly=false onChange="" id="" ignoreParameters=false errorField="" tabIndex="">
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <input id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" type="<#if password>password<#else>text</#if>" size="${size}" maxlength="${maxlength}" class="inputBox" <#if !password>value="${getDefaultValue(name, default, index, ignoreParameters)}"</#if> <#if readonly>readonly="readonly"</#if> onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>/>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputTextCell name size=30 maxlength="" default="" index=-1 password=false readonly=false onChange="" id="" ignoreParameters=false errorField="" tabIndex="">
  <td><@inputText name=name id=id size=size maxlength=maxlength default=default index=index password=password readonly=readonly onChange=onChange ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex /></td>
</#macro>

<#macro inputTextRow name title size=30 maxlength="" default="" titleClass="tableheadtext" index=-1 rowId="" hidden=false password=false readonly=false onChange="" id="" ignoreParameters=false errorField="" tabIndex="">
  <tr<#if rowId?length != 0> id="${getIndexedName(rowId, index)}"</#if><#if hidden> style="display:none"</#if>>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputTextCell name=name id=id size=size maxlength=maxlength default=default index=index password=password readonly=readonly onChange=onChange ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex />
  </tr>
</#macro>

<#macro inputCheckbox name value="Y" index=-1 onChange="" onClick="" id="" ignoreParameters=false errorField="" tabIndex="" default="">
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <input id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" type="checkbox" value="${value}" onChange="${onChange}" onClick="${onClick}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if> <#if value == getDefaultValue(name, default, index, ignoreParameters)>checked="checked"</#if>/>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputCheckboxCell name value="Y" index=-1 onChange="" onClick="" id="" ignoreParameters=false errorField="" tabIndex="" default="">
  <td><@inputCheckbox name=name value=value id=id default=default index=index onChange=onChange onClick=onClick ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex /></td>
</#macro>

<#macro inputCheckboxRow name title value="Y" index=-1 rowId="" hidden=false onChange="" onClick="" id="" ignoreParameters=false errorField="" tabIndex="" default="" titleClass="tableheadtext">
  <tr<#if rowId?length != 0> id="${getIndexedName(rowId, index)}"</#if><#if hidden> style="display:none"</#if>>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputCheckboxCell name=name value=value id=id default=default index=index onChange=onChange onClick=onClick ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex />
  </tr>
</#macro>

<#macro inputFile name size=30 maxlength="" default="" index=-1 onChange="" id="" errorField="" tabIndex="">
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <input id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" type="file" size="${size}" maxlength="${maxlength}" class="inputBox" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>/>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputFileCell name size=30 maxlength="" default="" index=-1 onChange="" id="" errorField="" tabIndex="">
  <td><@inputFile name=name id=id size=size maxlength=maxlength default=default index=index onChange=onChange errorField=errorField tabIndex=tabIndex /></td>
</#macro>

<#macro inputFileRow name title size=30 maxlength="" default="" titleClass="tableheadtext" index=-1 rowId="" hidden=false onChange="" id="" errorField="" tabIndex="">
  <tr<#if rowId?length != 0> id="${getIndexedName(rowId, index)}"</#if><#if hidden> style="display:none"</#if>>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputFileCell name=name id=id size=size maxlength=maxlength default=default index=index onChange=onChange errorField=errorField tabIndex=tabIndex />
  </tr>
</#macro>

<#macro inputTextarea name rows=5 cols=60 default="" index=-1 ignoreParameters=false errorField="" tabIndex="">
  <textarea rows="${rows}" cols="${cols}" name="${getIndexedName(name, index)}" class="inputBox" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>>${getDefaultValue(name, default, index, ignoreParameters)}</textarea>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputTextareaCell name rows=5 cols=60 default="" index=-1 ignoreParameters=false errorField="" tabIndex="">
  <td><@inputTextarea name=name rows=rows cols=cols default=default index=index ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex /></td>
</#macro>

<#macro inputTextareaRow name title rows=5 cols=60 default="" titleClass="tableheadtext" index=-1 ignoreParameters=false errorField="" tabIndex="">
  <tr>
    <td class="titleCellTop"><span class="${titleClass}">${title}</span></td>
    <@inputTextareaCell name=name rows=rows cols=cols default=default index=index ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex />
  </tr>
</#macro>


<#-- these are duplicated because #nested only goes one level up -->

<#macro inputSelectHash name hash required=true default="" index=-1 onChange="" ignoreParameters=false errorField="" tabIndex="">
 <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
 <select name="${getIndexedName(name, index)}" class="inputBox" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>>
   <#if !required><option value=""></option></#if>
   <#assign keys = hash?keys />
   <#list keys as k>
     <option <#if defaultValue = k>selected="selected"</#if> value="${k}">${hash[k]}</option>
   </#list>
 </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputSelectHashCell name hash required=true default="" index=-1 onChange="" ignoreParameters=false errorField="" tabIndex="">
 <td><@inputSelectHash name=name required=required default=default hash=hash index=index onChange=onChange ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex/></td>
</#macro>

<#macro inputSelectHashRow name title hash required=true default="" index=-1 titleClass="tableheadtext" onChange="" ignoreParameters=false errorField="" tabIndex="">
 <tr>
   <td class="titleCell"><span class="${titleClass}">${title}</span></td>
   <@inputSelectHashCell name=name required=required default=default hash=hash index=index onChange=onChange ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex/>
 </tr>
</#macro>

<#macro inputSelect name list key="" displayField="" default="" index=-1 required=true defaultOptionText="" onChange="" id="" ignoreParameters=false errorField="" tabIndex="" readonly=false>
  <#if key == ""><#assign listKey = name><#else><#assign listKey = key></#if>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if> <#if readonly>disabled="disabled"</#if>>
    <#if !required><option value="">${defaultOptionText}</option></#if>
    <#list list as option>
      <#if option.get(listKey) == defaultValue || listKey == defaultValue><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.get(listKey)}">
        <#if displayField==""><#nested option><#else>${option.get(displayField)?if_exists}</#if>
      </option>
    </#list>
  </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputSelectCell name list key="" displayField="" default="" index=-1 required=true defaultOptionText="" onChange="" id="" ignoreParameters=false errorField="" tabIndex="" readonly=false>
  <#if key == ""><#assign listKey = name><#else><#assign listKey = key></#if>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
  <td>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if> <#if readonly>disabled="disabled"</#if>>
    <#if !required><option value="">${defaultOptionText}</option></#if>
    <#list list as option>
      <#if option.get(listKey) == defaultValue || listKey == defaultValue><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.get(listKey)}">
        <#if displayField==""><#nested option><#else>${option.get(displayField)?if_exists}</#if>
      </option>
    </#list>
  </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
  </td>
</#macro>

<#macro inputSelectRow title name list key="" displayField="" default="" index=-1 required=true defaultOptionText="" titleClass="tableheadtext" onChange="" id="" ignoreParameters=false errorField="" tabIndex="" readonly=false>
  <#if key == ""><#assign listKey = name><#else><#assign listKey = key></#if>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
  <tr>
  <td class="titleCell"><span class="${titleClass}">${title}</span></td>
  <td>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if> <#if readonly>disabled="disabled"</#if>>
    <#if !required><option value="">${defaultOptionText}</option></#if>
    <#list list as option>
      <#if option.get(listKey) == defaultValue || listKey == defaultValue><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.get(listKey)}">
        <#if displayField==""><#nested option><#else>${option.get(displayField)?if_exists}</#if>
      </option>
    </#list>
  </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
  </td>
  </tr>
</#macro>

<#macro inputMultiSelect title name list key="" displayField="" default=[] index=-1 defaultOptionText="" titleClass="tableheadtext" size=5 onChange="" id="" errorField="" tabIndex="">
  <#if key == ""><#assign listKey = name><#else><#assign listKey = key></#if>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox" multiple="multiple" size="${size}" style="height:auto" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>>
    <#list list as option>
      <#if default?seq_contains(option.get(listKey))><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.get(listKey)}">
        <#if displayField==""><#nested option><#else>${option.get(displayField)?if_exists}</#if>
      </option>
    </#list>
  </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputMultiSelectCell title name list key="" displayField="" default=[] index=-1 defaultOptionText="" titleClass="tableheadtext" size=5 onChange="" id="" errorField="" tabIndex="">
  <#if key == ""><#assign listKey = name><#else><#assign listKey = key></#if>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <td>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox" multiple="multiple" size="${size}" style="height:auto" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>>
    <#list list as option>
      <#if default?seq_contains(option.get(listKey))><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.get(listKey)}">
        <#if displayField==""><#nested option><#else>${option.get(displayField)?if_exists}</#if>
      </option>
    </#list>
  </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
  </td>
</#macro>

<#macro inputMultiSelectRow title name list key="" displayField="" default=[] index=-1 defaultOptionText="" titleClass="tableheadtext" size=5 onChange="" id="" errorField="" tabIndex="">
  <#if key == ""><#assign listKey = name><#else><#assign listKey = key></#if>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <tr>
  <td class="titleCellTop"><span class="${titleClass}">${title}</span></td>
  <td>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox" multiple="multiple" size="${size}" style="height:auto" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>>
    <#list list as option>
      <#if default?seq_contains(option.get(listKey))><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.get(listKey)}">
        <#if displayField==""><#nested option><#else>${option.get(displayField)?if_exists}</#if>
      </option>
    </#list>
  </select>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
  </td>
  </tr>
</#macro>


<#macro inputLookup name lookup form default="" size=20 maxlength=20 index=-1 onChange="" ignoreParameters=false errorField="" tabIndex="" readonly=false>
  <#assign indexedName = getIndexedName(name, index)/>
  <input type="text" size="${size}" maxlength="${maxlength}" name="${indexedName}" class="inputBox" value="${getDefaultValue(name, default, index, ignoreParameters)}" onChange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if> <#if readonly>readonly="readonly"</#if>/>
  <#if !readonly>
    <a href="javascript:call_fieldlookup2(document.${form}.${indexedName},'${lookup}');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="14" width="15"></a>
  </#if>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#macro inputLookupCell name lookup form default="" size=20 maxlength=20 index=-1 onChange="" ignoreParameters=false errorField="" tabIndex="" readonly=false>
<td nowrap="nowrap"><@inputLookup name=name lookup=lookup form=form default=default size=size maxlength=maxlength index=index onChange=onChange ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex readonly=readonly/></td>
</#macro>

<#macro inputLookupRow name title lookup form size=20 maxlength=20 default="" titleClass="tableheadtext" onChange="" ignoreParameters=false errorField="" tabIndex="" readonly=false>
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputLookupCell name=name lookup=lookup form=form default=default size=size maxlength=maxlength index=index onChange=onChange ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex readonly=readonly/>
  </tr>
</#macro>


<#-- auto complete -->

<#-- TODO: the way ID is handled here should be done for all macros.  We can remove the form parameter if we use id. -->
<#macro inputAutoComplete name url form="" lookup="" styleClass="inputAutoCompleteQuick" id="" default="" index=-1 size=15 maxlength=20 ignoreParameters=false errorField="" tabIndex="" onChange="">
  <#assign indexedName = getIndexedName(name, index)/>
  <#assign realId = id/>
  <#if !realId?has_content>
    <#assign realId = indexedName/>
  </#if>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)/>
  <div dojoType="dojo.data.JsonItemStoreAutoComplete" 
    jsId="${realId}ComboBoxStore" 
    url="${url}"
    style="display:inline">
  </div>  
  <input dojoType="dijit.form.ComboBox"
    store="${realId}ComboBoxStore"
    id="ComboBox_${realId}"
    hiddenId="${realId}"
    name="ComboBox_${indexedName}"
    hasDownArrow="false"
    autoComplete="false"
    searchAttr="name"
    searchDelay="1000"
    size="${size}"
    maxlength="${maxlength}"
    value="${defaultValue}"
    type="text"
    onValueChanged="${onChange}"
    <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>/>
  <input type="hidden" name="${indexedName}" id="${realId}" value="${defaultValue}" />

  <#if lookup?has_content && realId?has_content>
    <#assign comboElement = "document.getElementById('ComboBox_${realId}')"/>
    <#assign formElement = "document.getElementById('${realId}')"/>
    <a href="javascript:call_fieldlookup2autocomplete(${comboElement},${formElement},'${lookup}');"><img src="/images/fieldlookup.gif" alt="Lookup" border="0" height="14" width="15"/></a>
  </#if>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
</#macro>

<#-- auto complete any Party -->
<#macro inputAutoCompleteParty name id="" url="getAutoCompletePartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupPartyName" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
</#macro>

<#macro inputAutoCompletePartyCell name id="" url="getAutoCompletePartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteParty name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/></td>
</#macro>

<#macro inputAutoCompletePartyRow title name titleClass="tableheadtext" id="" url="getAutoCompletePartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompletePartyCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange />
  </tr>
</#macro>

<#-- auto complete any CRM Party (contact, accout, lead, partner) -->
<#macro inputAutoCompleteCrmParty name id="" url="getAutoCompleteCrmPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupPartyName" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
</#macro>

<#macro inputAutoCompleteCrmPartyCell name id="" url="getAutoCompleteCrmPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteCrmParty name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/></td>
</#macro>

<#macro inputAutoCompleteCrmPartyRow title name titleClass="tableheadtext" id="" url="getAutoCompleteCrmPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompleteCrmPartyCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange />
  </tr>
</#macro>

<#-- auto complete Supplier -->
<#macro inputAutoCompleteSupplier name id="" url="getAutoCompleteSupplierPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupSupplier" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
</#macro>

<#macro inputAutoCompleteSupplierCell name id="" url="getAutoCompleteSupplierPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteSupplier name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange /></td>
</#macro>

<#macro inputAutoCompleteSupplierRow title name titleClass="tableheadtext" id="" url="getAutoCompleteSupplierPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompleteSupplierCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
  </tr>
</#macro>

<#-- auto complete Account, Contact and Prospect -->
<#macro inputAutoCompleteClient name id="" url="getAutoCompleteClientPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupClients" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange />
</#macro>

<#macro inputAutoCompleteClientCell name id="" url="getAutoCompleteClientPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteAccount name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange /></td>
</#macro>

<#macro inputAutoCompleteClientRow title name titleClass="tableheadtext" id="" url="getAutoCompleteClientPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompleteAccountCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
  </tr>
</#macro>

<#-- auto complete Account -->
<#macro inputAutoCompleteAccount name id="" url="getAutoCompleteAccountPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupAccounts" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
</#macro>

<#macro inputAutoCompleteAccountCell name id="" url="getAutoCompleteAccountPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteAccount name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/></td>
</#macro>

<#macro inputAutoCompleteAccountRow title name titleClass="tableheadtext" id="" url="getAutoCompleteAccountPartyIds" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompleteAccountCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange />
  </tr>
</#macro>

<#-- auto complete GlAccount -->
<#macro inputAutoCompleteGlAccount name id="" url="getAutoCompleteGlAccounts" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupGlAccount" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange />
</#macro>

<#macro inputAutoCompleteGlAccountCell name id="" url="getAutoCompleteGlAccounts" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteGlAccount name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/></td>
</#macro>

<#macro inputAutoCompleteGlAccountRow title name titleClass="tableheadtext" id="" url="getAutoCompleteGlAccounts" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompleteGlAccountCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
  </tr>
</#macro>

<#-- auto complete Product -->
<#macro inputAutoCompleteProduct name url="getAutoCompleteProduct" id="" form="" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <@inputAutoComplete name=name url=url id=id form=form lookup="LookupProduct" styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
</#macro>

<#macro inputAutoCompleteProductCell name url="getAutoCompleteProduct" id="" form="" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <td><@inputAutoCompleteProduct name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/></td>
</#macro>

<#macro inputAutoCompleteProductRow title name titleClass="tableheadtext" url="getAutoCompleteProduct" id="" form="" styleClass="inputAutoCompleteQuick" default="" index=-1 size=15 errorField="" tabIndex="" onChange="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputAutoCompleteProductCell name=name id=id url=url styleClass=styleClass default=default index=index size=size errorField=errorField tabIndex=tabIndex onChange=onChange/>
  </tr>
</#macro>


<#macro inputCurrencySelect list=[] name="currencyUomId" defaultCurrencyUomId="" id="" index=-1 useDescription=false ignoreParameters=false>
  <#if id == ""><#assign idVal = name><#else><#assign idVal = id></#if>
  <#assign currencyDefault = getDefaultValue(name, defaultCurrencyUomId, index, ignoreParameters)>
  <#if currencyDefault?length == 0>
    <#assign currencyDefault = Static["org.opentaps.common.util.UtilConfig"].getPropertyValue(opentapsApplicationName?default("opentaps"), "defaultCurrencyUomId")?default("USA") />
  </#if>
  <#assign currencies = list />
  <#if currencies?size == 0><#assign currencies = Static["org.opentaps.common.util.UtilCommon"].getCurrencies(delegator) /></#if>
  <select id="${getIndexedName(idVal, index)}" name="${getIndexedName(name, index)}" class="inputBox">
    <#list currencies as option>
      <#if option.uomId == currencyDefault><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
      <option ${selected} value="${option.uomId}">${option.abbreviation}<#if useDescription> - ${option.description}</#if></option>
    </#list>
  </select>
</#macro>

<#macro inputCurrencySelectCell name="currencyUomId" list=[] defaultCurrencyUomId="" id="" index=-1 useDescription=false ignoreParameters=false>
  <td nowrap="nowrap"><@inputCurrencySelect name=name id=id list=list defaultCurrencyUomId=defaultCurrencyUomId index=index useDescription=useDescription ignoreParameters=ignoreParameters /></td>
</#macro>

<#macro inputCurrencySelectRow title name="currencyUomId" list=[] titleClass="tableheadtext" defaultCurrencyUomId="" id="" index=-1 useDescription=false ignoreParameters=false>
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputCurrencySelectCell name=name id=id list=list defaultCurrencyUomId=defaultCurrencyUomId index=index useDescription=useDescription ignoreParameters=ignoreParameters />
  </tr>
</#macro>


<#macro inputCurrency name list=[] currencyName="currencyUomId" default="" defaultCurrencyUomId="" disableCurrencySelect=false index=-1 ignoreParameters=false>
  <input name="${getIndexedName(name, index)}" type="text" size="6" class="inputBox" value="${getDefaultValue(name, default, index, ignoreParameters)}"/>
  <#if disableCurrencySelect && defaultCurrencyUomId?size != 0>
    <input type="hidden" name="${getIndexedName(currencyName, index)}" value="${defaultCurrencyUomId}"/>
    ${defaultCurrencyUomId}
  <#else>
  <@inputCurrencySelect list=list name=currencyName defaultCurrencyUomId=defaultCurrencyUomId index=index ignoreParameters=ignoreParameters />
  </#if>
</#macro>

<#macro inputCurrencyCell name list=[] currencyName="currencyUomId" default="" defaultCurrencyUomId="" disableCurrencySelect=false index=-1 ignoreParameters=false>
  <td nowrap="nowrap"><@inputCurrency name=name list=list currencyName=currencyName default=default defaultCurrencyUomId=defaultCurrencyUomId disableCurrencySelect=disableCurrencySelect index=index ignoreParameters=ignoreParameters /></td>
</#macro>

<#macro inputCurrencyRow name title list=[] currencyName="currencyUomId" default="" titleClass="tableheadtext" defaultCurrencyUomId="" disableCurrencySelect=false index=-1 ignoreParameters=false>
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputCurrencyCell name=name list=list currencyName=currencyName default=default defaultCurrencyUomId=defaultCurrencyUomId disableCurrencySelect=disableCurrencySelect index=index ignoreParameters=ignoreParameters />
  </tr>
</#macro>

<#macro inputSubmitIndexed title index onClick="submitFormWithSingleClick(this)" class="smallSubmit">
  <input type="submit" value="${title}" class="${class}" onclick="opentaps.markRowForSubmit(form, ${index});${onClick};"/>
</#macro>

<#macro inputSubmitIndexedCell title index onClick="submitFormWithSingleClick(this)" class="smallSubmit" blockClass="">
  <td class="${blockClass}"><input type="submit" value="${title}" class="${class}" onclick="opentaps.markRowForSubmit(form, ${index});${onClick};"/></td>
</#macro>

<#macro inputSubmit title onClick="submitFormWithSingleClick(this)" class="smallSubmit" tabIndex="">
  <input type="submit" value="${title}" class="${class}" onclick="${onClick}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>/>
</#macro>

<#macro inputSubmitCell title onClick="submitFormWithSingleClick(this)" blockClass="" class="smallSubmit" tabIndex="">
  <td class="${blockClass}"><@inputSubmit title=title onClick=onClick class=class tabIndex=tabIndex /></td>
</#macro>

<#macro inputSubmitRow title onClick="submitFormWithSingleClick(this)" blockClass="" class="smallSubmit" tabIndex="">
  <tr>
    <td>&nbsp;</td>
    <@inputSubmitCell title=title onClick=onClick class=class blockClass=blockClass tabIndex=tabIndex />
  </tr>
</#macro>


<#macro inputButton title onClick="" id="">
  <#assign idText = id?has_content?string("id=\"" + id + "\"", "")/>
  <input type="submit" value="${title}" class="smallSubmit" onclick="${onClick}" ${idText} />
</#macro>

<#macro inputButtonCell title onClick="" id="">
  <td><@inputButton title=title onClick=onClick id=id /></td>
</#macro>

<#macro inputButtonRow title onClick="" id="">
  <tr>
    <td>&nbsp;</td>
    <@inputButtonCell title=title onClick=onClick id=id />
  </tr>
</#macro>


<#macro inputIndicator name required=true default="" index=-1 onChange="" ignoreParameters=false>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
  <select name="${getIndexedName(name, index)}" class="inputBox" onChange="${onChange}">
    <#if !required><option value=""></option></#if>
    <option <#if defaultValue == "Y">selected="selected"</#if> value="Y">${uiLabelMap.CommonYes}</option>
    <option <#if defaultValue == "N">selected="selected"</#if> value="N">${uiLabelMap.CommonNo}</option>
  </select>
</#macro>

<#macro inputIndicatorCell name required=true default="" index=-1 onChange="" ignoreParameters=false>
  <td><@inputIndicator name=name required=required default=default index=index onChange=onChange ignoreParameters=ignoreParameters /></td>
</#macro>

<#macro inputIndicatorRow name title required=true default="" titleClass="tableheadtext" index=-1 onChange="" ignoreParameters=false>
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputIndicatorCell name=name required=required default=default index=index onChange=onChange ignoreParameters=ignoreParameters />
  </tr>
</#macro>

<#-- Parameter 'form' is decreated and leaves here for compatibility w/ existent code. Don't use it any more. -->
<#macro inputDate name form="" default="" size=10 index=-1 popup=true weekNumbers=false onChange="" onUpdate="" onDateStatusFunc="" linkedName="" delta=0 id="" ignoreParameters=false errorField="" tabIndex="" calendarTabIndex="">
  <#assign indexedName = getIndexedName(name, index)>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
  <#assign elId = indexedName />
  <#if id?has_content><#assign elId = id /></#if>
  <input id="${elId}" type="text" size="${size}" maxlength="${size}" name="${indexedName}" class="inputBox" value="${getLocalizedDate(defaultValue, "DATE")}" onchange="${onChange}" <#if tabIndex?has_content>tabindex="${tabIndex}"</#if>/>
  <a href="javascript:opentaps.toggleClass(document.getElementById('${elId}-calendar-placeholder'), 'hidden');"><img id="${elId}-button" src="/images/cal.gif" alt="View Calendar" title="View Calendar" border="0" height="16" width="16" <#if tabIndex?has_content>tabindex="${calendarTabIndex}"</#if>/></a>
  <#if !popup><div id="${elId}-calendar-placeholder" style="border: 0px; width: auto;" class="hidden"></div></#if>
  <#if errorField?has_content><@displayError name=errorField index=index /></#if>
  <script type="text/javascript">
  /*<![CDATA[*/
    function ${elId}_onDateChange(calendar) {
      if (calendar.dateClicked) {
        var input = document.getElementById('${elId}');
        if (input) {
          input.value = opentaps.formatDate(calendar.date, '${StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))}');
        }
        opentaps.addClass(document.getElementById('${elId}-calendar-placeholder'), 'hidden');
      }
    };
    <#if linkedName?has_content && !onUpdate?has_content>
    function ${elId}_calcAndApplyDifference(calendar) {
      var linkedInput = document.getElementById('${linkedName}');
      if (!linkedInput || linkedInput.nodeName != 'INPUT') {
        alert('Linked field with name ${linkedName} isn\'t accessible or have wrong type!');
        return;
      }
      
      var date = calendar.date;
      var time = date.getTime();
      time += (Date.DAY * ${delta});
      
      var linkedDate = new Date(time);
      linkedInput.value = opentaps.formatDate(linkedDate, '${StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))}');
    };
    </#if>
    Calendar.setup(
      {
      <#if !popup>
        flat: "${elId}-calendar-placeholder",
        flatCallback: ${elId}_onDateChange,
      <#else>
        inputField: "${elId}",
        ifFormat: "${StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))}",
        button: "${elId}-button",
        align: "Bl",
      </#if>
      <#if onUpdate?has_content>
        onUpdate: ${onUpdate},
      </#if>
      <#if linkedName?has_content && !onUpdate?has_content>
        onUpdate: ${elId}_calcAndApplyDifference,
      </#if>
      <#if weekNumbers?is_boolean>
        weekNumbers: <#if weekNumbers>true<#else>false</#if>,
      </#if>
        showOthers: true,
        cache: true
      }
    );
  /*]]>*/
  </script>
</#macro>

<#macro inputDateCell name form="" default="" size=10 index=-1 popup=true weekNumbers=false onUpdate="" onDateStatusFunc="" linkedName="" delta=0 id="" ignoreParameters=false errorField="" tabIndex="" calendarTabIndex="">
  <td><@inputDate name=name form=form default=default size=size index=index popup=popup weekNumbers=weekNumbers onUpdate=onUpdate onDateStatusFunc=onDateStatusFunc linkedName=linkedName delta=delta id=id ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex calendarTabIndex=calendarTabIndex/></td>
</#macro>

<#macro inputDateRow name title form="" titleClass="tableheadtext" default="" size=10 index=-1 popup=true weekNumbers=false onUpdate="" onDateStatusFunc="" linkedName="" delta=0 id="" ignoreParameters=false errorField="" tabIndex="" calendarTabIndex="">
  <tr>
    <@displayTitleCell title=title titleClass=titleClass/>
    <@inputDateCell name=name form=form default=default size=size index=index popup=popup weekNumbers=weekNumbers onUpdate=onUpdate onDateStatusFunc=onDateStatusFunc linkedName=linkedName delta=delta id=id ignoreParameters=ignoreParameters errorField=errorField tabIndex=tabIndex calendarTabIndex=calendarTabIndex/>
  </tr>
</#macro>

<#-- Enumeration Macros -->

<#macro inputEnumeration name enumTypeId index=-1 required=false default="" onChange="" ignoreParameters=false>
  <#assign enumerations = Static["org.opentaps.common.util.UtilCommon"].getEnumerations(enumTypeId, delegator)>
  <#assign defaultValue = getDefaultValue(name, default, index, ignoreParameters)>
  <select name="${getIndexedName(name, index)}" class="inputBox" onChange="${onChange}">
    <#if !required><option value=""></option></#if>
    <#list enumerations as enum>
      <option <#if defaultValue == enum.enumId>selected="selected"</#if> value="${enum.enumId}">${enum.get("description", locale)}</option>
    </#list>
  </select>
</#macro>

<#macro inputEnumerationCell name enumTypeId index=-1 required=false default="" onChange="" ignoreParameters=false>
  <td><@inputEnumeration name=name enumTypeId=enumTypeId index=index required=required default=default onChange=onChange ignoreParameters=ignoreParameters /></td>
</#macro>

<#macro inputEnumerationRow title name enumTypeId index=-1 required=false titleClass="tableheadtext" default="" onChange="" ignoreParameters=false>
  <td class="titleCell"><span class="${titleClass}">${title}</span></td>
  <@inputEnumerationCell name=name enumTypeId=enumTypeId index=index required=required default=default onChange=onChange ignoreParameters=ignoreParameters />
</#macro>

<#macro displayEnumeration enumId="" class="tabletext" style="">
  <span class="${class}" style="${style}"><#if enumId?has_content>${Static["org.opentaps.common.util.UtilCommon"].getEnumerationDescription(enumId, locale, delegator)}</#if></span>
</#macro>

<#macro displayEnumerationCell enumId="" class="tabletext" style="" blockStyle="" blockClass="">
  <td class="${blockClass}" style="${blockStyle}"><#if enumId?has_content><@displayEnumeration enumId=enumId class=class style=style /></#if></td>
</#macro>


<#-- ###################################################################### -->
<#-- ###                                                                 ## -->
<#-- ###              Larger "Building Block" Macros                     ## -->
<#-- ###                                                                 ## -->
<#-- ###################################################################### -->

<#-- Parameter 'form' is decreated and leaves here for compatibility w/ existent code. Don't use it any more. -->
<#macro inputDateTime name form="" default="" popup=true weekNumbers=false onUpdate="" onDateStatusFunc="" linkedName="" delta=0 ignoreParameters=false errorField="">
  <#assign defaultValue = getDefaultValue(name, default, -1, ignoreParameters)>
  <#assign defaultTime = Static["org.opentaps.common.util.UtilDate"].timestampToAmPm(getLocalizedDate(defaultValue, "DATE_TIME"), Static["org.ofbiz.base.util.UtilHttp"].getTimeZone(request), Static["org.ofbiz.base.util.UtilHttp"].getLocale(request)) />
  <input type="hidden" name="${name}_c_compositeType" value="Timestamp"/>
      <input id="${name}_c_date" type="text" class="inputBox" name="${name}_c_date" size="10" maxlength="10" value="${defaultTime.date?if_exists}"/>
      <#if defaultTime.date?exists>
        <#assign lookupDefault = default>
      <#else>
        <#assign lookupDefault = "">
      </#if>
      <a href="javascript:opentaps.toggleClass(document.getElementById('${name}-calendar-placeholder'), 'hidden');"><img id="${name}-button" src="/images/cal.gif" alt="View Calendar" title="View Calendar" border="0" height="16" width="16"/></a>
      &nbsp;
      <select name="${name}_c_hour" class="inputBox">
        <#list 1..12 as hour>
          <option <#if defaultTime.hour?default(12) == hour>selected="selected"</#if> value="${hour}">${hour}</option>
        </#list>
      </select>
      :
      <select name="${name}_c_minutes" class="inputBox">
        <#list 0..59 as min>
          <option <#if defaultTime.minute?default(-1) == min>selected="selected"</#if> value="${min}">${min?string?left_pad(2,"0")}</option>
        </#list>
      </select>
      <select name="${name}_c_ampm" class="inputBox">
        <option value="AM">AM</option>
        <option <#if defaultTime.ampm?default("") == "PM">selected="selected"</#if> value="PM">PM</option>
      </select>
      <table id="${name}-calendar-placeholder" style="border: 0px; width: auto;" class="hidden"></table>
      <#if errorField?has_content><@displayError name=errorField /></#if>
      <script type="text/javascript">
      /*<![CDATA[*/
        function ${name}_onDateChange(calendar) {
          if (calendar.dateClicked) {
            var input = document.getElementById('${name}_c_date');
            if (input) {
              input.value = opentaps.formatDate(calendar.date, '${StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))}');
            }
            opentaps.addClass(document.getElementById('${name}-calendar-placeholder'), 'hidden');
          }
        };
        <#if linkedName?has_content && !onUpdate?has_content>
        function ${name}_calcAndApplyDifference(calendar) {
          var linkedInput = document.getElementById('${linkedName}_c_date');
          if (!linkedInput || linkedInput.nodeName != 'INPUT') {
            alert('Linked field with name ${linkedName} isn\'t accessible or have wrong type!');
            return;
          }
      
          var date = calendar.date;
          var time = date.getTime();
          time += (Date.DAY * ${delta});
      
          var linkedDate = new Date(time);
          linkedInput.value = opentaps.formatDate(linkedDate, '${StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))}');
        };
        </#if>
        Calendar.setup(
          {
          <#if !popup>
            flat: "${name}-calendar-placeholder",
            flatCallback: ${name}_onDateChange,
          <#else>
            inputField: "${name}_c_date",
            ifFormat: "${StringUtil.wrapString(Static["org.opentaps.common.util.UtilDate"].getJsDateTimeFormat(Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)))}",
            button: "${name}-button",
            align: "Bl",
          </#if>
          <#if onUpdate?has_content>
            onUpdate: ${onUpdate},
          </#if>
          <#if linkedName?has_content && !onUpdate?has_content>
            onUpdate: ${name}_calcAndApplyDifference,
          </#if>
          <#if weekNumbers?is_boolean>
            weekNumbers: <#if weekNumbers>true<#else>false</#if>,
          </#if>
            showOthers: true,
            cache: true
          }
      );
      /*]]>*/
      </script>
</#macro>

<#macro inputDateTimeCell name form="" default="" popup=true weekNumbers=false onUpdate="" onDateStatusFunc="" linkedName="" delta=0 ignoreParameters=false errorField="">
  <td><@inputDateTime name=name form=form default=default popup=popup weekNumbers=weekNumbers onUpdate=onUpdate onDateStatusFunc=onDateStatusFunc linkedName=linkedName delta=delta ignoreParameters=ignoreParameters errorField=errorField/></td>
</#macro>

<#macro inputDateTimeRow name title form="" default="" titleClass="tableheadtext" popup=true weekNumbers=false onUpdate="" onDateStatusFunc="" linkedName="" delta=0 ignoreParameters=false errorField="">
  <tr>
    <td class="titleCell"><span class="${titleClass}">${title}</span></td>
    <@inputDateTimeCell name=name form=form default=default popup=popup weekNumbers=weekNumbers onUpdate=onUpdate onDateStatusFunc=onDateStatusFunc linkedName=linkedName delta=delta ignoreParameters=ignoreParameters errorField=errorField/>
  </tr>
</#macro>


<#macro inputSelectTaxAuthority list defaultGeoId="" defaultPartyId="" required=false>
    <select name="taxAuthGeoId" class="inputBox" onChange="opentaps.changeTaxParty(this.form.taxAuthGeoId, this.form.taxAuthPartyId)">
      <#if !required><option value=""></option></#if>
      <#list list as auth>
        <#if auth.taxAuthGeoId == defaultGeoId><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
        <option ${selected} value="${auth.taxAuthGeoId}" taxAuthPartyId="${auth.taxAuthPartyId}">${auth.geoName}</option>
      </#list>
    </select>

    <select name="taxAuthPartyId" class="inputBox">
      <#if !required><option value=""></option></#if>
      <#list list as auth>
        <#if auth.taxAuthPartyId == defaultPartyId><#assign selected = "selected=\"selected\""><#else><#assign selected = ""></#if>
        <option ${selected} value="${auth.taxAuthPartyId}" taxAuthGeoId="${auth.taxAuthGeoId}">${auth.groupName}</option>
      </#list>
    </select>
</#macro>

<#macro inputSelectTaxAuthorityCell list defaultGeoId="" defaultPartyId="" required=false>
  <td><@inputSelectTaxAuthority list=list defaultGeoId=defaultGeoId defaultPartyId=defaultPartyId required=required /></td>
</#macro>


<#macro inputState name="stateProvinceGeoId" countryInputName="countryGeoId" address={}>
    <#if address?size != 0>
      <#assign defaultStateGeoId = address.stateProvinceGeoId?default("") />
      <#assign defaultCountryGeoId = address.countryGeoId?default(configProperties.defaultCountryGeoId) />
    <#else>
      <#assign defaultStateGeoId = parameters.get(name)?default("") />
      <#assign defaultCountryGeoId = parameters.get(countryInputName)?default(configProperties.defaultCountryGeoId) />
    </#if>
    <#assign defaultStates = Static["org.opentaps.common.util.UtilCommon"].getStates(delegator, defaultCountryGeoId) />
    <select name="${name}" id="${name}" class="selectBox">
        <option></option>
        <#list defaultStates as state>
            <#if defaultStateGeoId == state.geoId><#assign selected="selected=\"selected\""><#else><#assign selected=""></#if>
            <option ${selected} value="${state.geoId}">${state.get("geoName", locale)}</option>
        </#list>
    </select>
</#macro>
<#macro inputCountry name="countryGeoId" stateInputName="stateProvinceGeoId" address={} required=false>
    <#if address?size != 0>
      <#assign defaultCountryGeoId = address.countryGeoId?default(configProperties.defaultCountryGeoId) />
    <#else>
      <#assign defaultCountryGeoId = parameters.get(name)?default(configProperties.defaultCountryGeoId) />
    </#if>
    <#assign countries = Static["org.opentaps.common.util.UtilCommon"].getCountries(delegator) />
    <select name="${name}" id="${name}" class="selectBox" onChange="opentaps.swapStatesInDropdown(this, '${stateInputName}')">
        <#if !required><option value=""></option></#if>    
        <#list countries as country>
            <#if defaultCountryGeoId == country.geoId><#assign selected="selected=\"selected\""><#else><#assign selected=""></#if>
            <option ${selected} value="${country.geoId}">${country.get("geoName", locale)}</option>
        </#list>
    </select>
</#macro>
<#macro inputStateCountry stateInputName="stateProvinceGeoId" countryInputName="countryGeoId" address={} required=false>
  <@inputState name=stateInputName countryInputName=countryInputName address=address /><@inputCountry name=countryInputName stateInputName=stateInputName address=address />
</#macro>
<#macro inputStateCountryCell stateInputName="stateProvinceGeoId" countryInputName="countryGeoId" address={} required=false>
    <td><@inputState name=stateInputName countryInputName=countryInputName address=address /><@inputCountry name=countryInputName stateInputName=stateInputName address=address required=false/></td>
</#macro>
<#macro inputStateCountryRow title stateInputName="stateProvinceGeoId" countryInputName="countryGeoId" titleClass="titleClass" address={} required=false>
    <tr>
        <td class="titleCell"><span class="${titleClass}">${title}</span></td>
        <td><@inputState name=stateInputName countryInputName=countryInputName address=address /><@inputCountry name=countryInputName stateInputName=stateInputName address=address required=false/></td>
    </tr>
</#macro>

<#macro displayGeoName geoId >
  <@display text=Static["org.opentaps.common.util.UtilCommon"].getGeoName(geoId, locale, delegator) />
</#macro>
<#macro displayGeoCode geoId >
  <@display text=Static["org.opentaps.common.util.UtilCommon"].getGeoCode(geoId, locale, delegator) />
</#macro>
<#macro displayGeo geoId >
  <#assign geoName = Static["org.opentaps.common.util.UtilCommon"].getGeoName(geoId, locale, delegator) />
  <#assign geoCode = Static["org.opentaps.common.util.UtilCommon"].getGeoCode(geoId, locale, delegator) />
  <@display text="${geoCode} (${geoName})" />
</#macro>


<#-- This macro is squished because of the way the menu buttons work.  -->
<#macro inputConfirm title href="" form="" confirmText=uiLabelMap.OpentapsAreYouSure class="buttonDangerous" onClick=""><a class="${class}" href="javascript:<#if onClick!="">${onClick}; </#if>opentaps.confirmAction('${confirmText}', '${href}', '${form}')">${title}</a></#macro>

<#macro inputConfirmImage title src href="" form="" confirmText=uiLabelMap.OpentapsAreYouSure class="" border="0"><a class="${class}" href="javascript:opentaps.confirmAction('${confirmText}', '${href}', '${form}')"><img src="${src}" alt="${title}" title="${title}" border="${border}" /></a></#macro>

<#macro inputConfirmCell title href="" form="" confirmText=uiLabelMap.OpentapsAreYouSure class="buttonDangerous">
  <td><@inputConfirm title=title href=href form=form confirmText=confirmText /></td>
</#macro>

<#macro inputForceComplete title forceTitle confirmText=uiLabelMap.OpentapsAreYouSure href="" form="">
  <#if href != "">
    <#if parameters.forceComplete?default("false") == "true">
      <@inputConfirm title=forceTitle confirmText=confirmText href=(href + "&amp;forceComplete=true") />
    <#else>
      <@displayLink href=href text=title class="buttontext" />
    </#if>
  <#elseif form != "">
    <#if parameters.forceComplete?default("false") == "true">
      <@inputHidden name="forceComplete" value="true"/>
      <@inputConfirm title=forceTitle confirmText=confirmText form=form />
    <#else>
      <@inputSubmit title=title />
    </#if>
  </#if>
</#macro>
<#macro inputForceCompleteCell title forceTitle confirmText=uiLabelMap.OpentapsAreYouSure href="" form="">
  <td><@inputForceComplete title=title forceTitle=forceTitle confirmText=confirmText href=href form=form /></td>
</#macro>
<#macro inputForceCompleteRow title forceTitle confirmText=uiLabelMap.OpentapsAreYouSure href="" form="">
  <tr>
    <td>&nbsp;</td>
    <td><@inputForceComplete title=title forceTitle=forceTitle confirmText=confirmText href=href form=form /></td>
  </tr>
</#macro>

<#-- Macro selectAction implements drop-down based menu widget                     -->
<#-- Should be used jointly with macro <@action />                                 -->
<#-- Example:                                                                      -->
<#--    <@selectAction name="ID">                                                  -->
<#--        <@action url="somewhere/someThing?parameter=value" text="Label Text"/> -->
<#--        etc ...                                                          -->
<#--    </@selectAction>                                                           -->

<#macro action url text selected=false>
  <#if url?starts_with("/")>
    <option value="${url}${externalKeyParam?if_exists}" <#if selected>selected="selected"</#if>>${text}</option>
  <#elseif url?starts_with("javascript:")>
    <option value="${url}" <#if selected>selected="selected"</#if>>${text}</option>
  <#else>
    <option value="<@ofbizUrl>${url}</@ofbizUrl>" <#if selected>selected="selected"</#if>>${text}</option>
  </#if>
</#macro>

<#macro selectAction name prompt=uiLabelMap.OpentapsDefaultActionPrompt>
  <select id="${name}" name="${name}" class="inputBox" onchange="opentaps.changeLocation(null, '${name}');">
    <option value="">${prompt}</option>
    <option value="">${uiLabelMap.OpentapsDefaultActionSeparator}</option>
    <#nested>
  </select>
</#macro>


<#-- Macro selectActionForm implements drop-down based menu widget                 -->
<#--  like selectAction but instead of changing the location, it will post the     -->
<#--  given form                                                                   -->
<#-- Should be used jointly with macro <@actionForm />                             -->
<#-- Example:                                                                      -->
<#--    <@selectActionForm name="ID">                                              -->
<#--        <@actionForm form="someFormName" text="Label Text"/>                   -->
<#--        etc ...                                                                -->
<#--    </@selectActionForm>                                                       -->

<#macro actionForm form text selected=false>
  <option value="${form}" <#if selected>selected="selected"</#if>>${text}</option>
</#macro>

<#macro selectActionForm name prompt=uiLabelMap.OpentapsDefaultActionPrompt>
  <select id="${name}" name="${name}" class="inputBox" onchange="opentaps.selectForm('${name}');">
    <option value="">${prompt}</option>
    <option value="">${uiLabelMap.OpentapsDefaultActionSeparator}</option>
    <#nested>
  </select>
</#macro>

<#macro form name url id="" method="post" formParams...>
  <form method="${method}" action="${url}" name="${name}">
    <#if formParams?has_content>
      <#list formParams?keys as param>
        <@inputHidden name="${param}" value="${formParams[param]}" />
      </#list>
    </#if>
    <#nested>
  </form>
</#macro>

<#macro submitFormLink form text class="buttontext" id="" style="" formParams...>
<#compress>
  <#assign extraParams = "null"/>
  <#if formParams?has_content>
    <#assign extraParams>{
      <#list formParams?keys as param>
        '${param}':'${formParams[param]}'
        <#if param_has_next>,</#if>
      </#list>
      }
    </#assign>
  </#if>
  <a class="${class}" <#if style?has_content> style="${style}"</#if> <#if id?has_content> id="${id}"</#if> href="javascript:opentaps.submitForm('${form}', null, ${extraParams});">${text}</a>
</#compress>
</#macro>

<#macro submitFormLinkConfirm form text class="buttonDangerous" id="" style="" confirmText=uiLabelMap.OpentapsAreYouSure formParams...>
<#compress>
  <#assign extraParams = "null"/>
  <#if formParams?has_content>
    <#assign extraParams>{
      <#list formParams?keys as param>
        '${param}':'${formParams[param]}'
        <#if param_has_next>,</#if>
      </#list>
      }
    </#assign>
  </#if>
  <a class="${class}" <#if style?has_content> style="${style}"</#if> <#if id?has_content> id="${id}"</#if> href="javascript:opentaps.confirmAction('${confirmText}', null, '${form}', ${extraParams});">${text}</a>
</#compress>
</#macro>

<#macro paginate name list="" rememberOrderBy=true rememberPage=true renderExcelButton=true params...>
  <@paginateTransform name=name list=list rememberOrderBy=rememberOrderBy rememberPage=rememberPage renderExcelButton=renderExcelButton context=context params=params><#nested></@paginateTransform>
</#macro>


<#-- ###################################################################### -->
<#-- ###                                                                 ## -->
<#-- ###                     Multi Form Macros                           ## -->
<#-- ###                                                                 ## -->
<#-- ###################################################################### -->
        

<#macro inputHiddenRowSubmit index submit=true>
    <input type="hidden" name="_rowSubmit_o_${index}" value="<#if submit>Y<#else>N</#if>"/>
</#macro>

<#macro inputMultiSelectAll form onClick="">
  <input type="checkbox" name="selectAll" value="N" onClick="javascript:toggleAll(this, '${form}'); ${onClick}"/>
</#macro>
<#macro inputMultiSelectAllCell form onClick="">
  <td><@inputMultiSelectAll form=form onClick=onClick /></td>
</#macro>

<#macro inputMultiCheck index onClick="">
  <input type="checkbox" name="_rowSubmit_o_${index}" value="Y"<#if onClick != "">onclick="${onClick}"</#if>/>
</#macro>
<#macro inputMultiCheckCell index onClick="">
  <td><@inputMultiCheck index=index onClick=onClick /></td>
</#macro>

<#macro inputHiddenRowCount list>
  <@inputHidden name="_rowCount" value=list?size />
</#macro>

<#macro inputHiddenUseRowSubmit>
  <@inputHidden name="_useRowSubmit" value="Y" />
</#macro>


<#-- ###################################################################### -->
<#-- ###                                                                 ## -->
<#-- ###                      Global Functions                           ## -->
<#-- ###                                                                 ## -->
<#-- ###################################################################### -->


<#function tableRowClass rowIndex rowClassOdd="rowWhite" rowClassEven="rowLightGray">
  <#return (rowIndex % 2 == 0)?string(rowClassOdd, rowClassEven)/>
</#function>


<#-- ###################################################################### -->
<#-- ###                                                                 ## -->
<#-- ###                      Convenience Macros                         ## -->
<#-- ###                                                                 ## -->
<#-- ###################################################################### -->


<#macro tooltip text="" class="tooltip"><#if text?has_content><span class="tabletext"><span class="${class}">${text}</span></span></#if></#macro>


<#macro displayError name index=-1><@tooltip text=opentapsErrors.field.get(getIndexedName(name,index)) class="errortooltip" /></#macro>


<#-- Leon: I am on the fence about this one, it seems too minimal to be a proper macro and seems to serve more as a mnemonic.  Needs discussion.  -->
<#macro sectionHeader title headerClass="subSectionHeader" titleClass="subSectionTitle">
  <div class="${headerClass}">
    <div class="${titleClass}">${title}</div>
    <#nested>
  </div>
</#macro>

<#macro pagination viewIndex viewSize currentResultSize requestName totalResultSize=-1 extraParameters="">
    <#if ( ! totalResultSize?has_content ) || (totalResultSize?has_content && totalResultSize != 0)>
        <#assign beginIndex = viewIndex/>
        <#if beginIndex == 0>
            <#assign beginIndex = 1/>
        </#if>
        <#assign endIndex = beginIndex + currentResultSize - 1/>
        <#if totalResultSize?has_content && totalResultSize != -1>
            <#assign paginationText = "${StringUtil.wrapString(uiLabelMap.OpentapsPaginationWithTotal)}"?interpret>
        <#else>
            <#assign paginationText = "${StringUtil.wrapString(uiLabelMap.OpentapsPaginationWithoutTotal)}"?interpret>
        </#if>
        <div class="pagination">
            <#if beginIndex &gt; 1>
                <span class="paginationPrevious"><a href="<@ofbizUrl>${requestName}?VIEW_INDEX=${beginIndex - viewSize}&amp;VIEW_SIZE=${viewSize}${extraParameters?html}</@ofbizUrl>">${uiLabelMap.CommonPrevious}</a></span>
            </#if>
            <span class="paginationText"><@paginationText/></span>
            <#if endIndex &lt; totalResultSize>
                <span class="paginationNext"><a href="<@ofbizUrl>${requestName}?VIEW_INDEX=${beginIndex + viewSize}&amp;VIEW_SIZE=${viewSize}${extraParameters?html}</@ofbizUrl>">${uiLabelMap.CommonNext}</a></span>
            </#if>
        </div>
    </#if>
</#macro>

<#macro htmlTextArea textAreaId name="" value="" tagFileLocation="" rows=20 cols=80 class="" style="">
  <textarea name="${name?has_content?string(name, textAreaId)}" id="${textAreaId}" class="${class}" rows="${rows}" cols="${cols}" style="${style}">${value}</textarea>
  <#if tagFileLocation?has_content>
    <@import location = tagFileLocation/>
  </#if>
  <script type="text/javascript" src="/opentaps_js/fckeditor/fckeditor.js"></script>
	<script type="text/javascript">
    opentaps.addOnLoad(function() {
      <#if tagFileLocation?has_content>
        tags = [];
        <#list getTags()?default([]) as tagMap>
          <#list tagMap?default({})?keys as tag>
            tags.push({ 'tag' : '${tag}' , 'description' : '${tagMap[tag]?js_string}' });
          </#list>
        </#list>
        insertTagsLabel = '${uiLabelMap.OpentapsHtmlEditorInsertTagsLabel?js_string}';
      </#if>
      insertTags = ${tagFileLocation?has_content?string};
      var oFCKeditor = new FCKeditor( '${textAreaId}' ) ;
      oFCKeditor.BasePath = '/opentaps_js/fckeditor/' ;
      oFCKeditor.Height	= 400 ;
      oFCKeditor.ToolbarSet = '${tagFileLocation?has_content?string("OpentapsFormMerge", "OpentapsBasic")}';
      oFCKeditor.ReplaceTextarea() ;
    });
	</script>
</#macro>

<#function formatTelecomNumber number={}>
  <#assign telecomNumber>
    <#if number.countryCode?has_content>${number.countryCode}&nbsp;</#if><#if number.areaCode?has_content>(${number.areaCode})&nbsp;</#if><#if number.contactNumber?has_content>${number.contactNumber}</#if>          
  </#assign>
  <#return telecomNumber/>
</#function>

<#macro glAccountTree glAccountTree treeId className="opentaps.GLAccountTree" defaultState="expanded">
<#assign isExpanded = (defaultState?lower_case == "expanded") />
    <script type="text/javascript">/*<![CDATA[*/

        function init_${treeId}() {

            var glAccountData = { identifier: 'glAccountId', label: 'name', items: ${StringUtil.wrapString(glAccountTree.toJSONString())}};
            var glAccountStore = new opentaps.GLAccountJsonStore({data:glAccountData});

            var glAccountTree = new ${className}({
                    id: '${treeId}',
                    store: glAccountStore,
                    query: {type:'root'},
                    labelAttr: "name",
                    typeAttr: "type",
                    defaultExpanded: ${isExpanded?string},
                    currencyUomId: '${StringUtil.wrapString(glAccountTree.getCurrencyUomId())}',
                    organizationPartyId: '${StringUtil.wrapString(glAccountTree.getOrganizationPartyId())}'
                    }, document.getElementById("${treeId}"));
        }

        opentaps.addOnLoad(init_${treeId});

    /*]]>*/</script>

    <div class="tabletext"><div id="${treeId}"></div></div>
</#macro>

<#-- GWT related macros -->

<#--
  loads a given gwt module and widget using a special notation docmented at
  http://opentaps.org/docs/index.php/Opentaps_Google_Web_Toolkit#Adding_a_GWT_Widget_to_a_Screen
-->
<#macro gwtModule widget>
  <#-- widget should be 'module/java_path' -->
  <#assign arr = widget.split("/")>
  <#if arr?size == 2>
    <#assign module=arr[0] />
    <#assign path=arr[1] />
    <script type="text/javascript" language="javascript" src="/${module}/${path}/${path}.nocache.js"></script>
  <#else>
    <@tooltip class="errortooltip" text="gwtModule Error: Must specify module and path of GWT widget to load.  Expected \"module/path\" but got \"${widget}\""/>
  </#if>
</#macro>

<#-- 
  Use in a FTL to load a widget by ID, the ID matching the ID in the entry class.
  Extra parameters are transformed in JS variables that can be retrieved in the GWT widget.

  Example:
  <@gwtWidget id="invoiceItems" invoiceId="10000"/>
-->
<#macro gwtWidget id class="" gwtParameters...>
<div class="gwtWidget<#if class?has_content> ${class}</#if>">
  <#if gwtParameters?has_content>
    <script type="text/javascript">
    /*<![CDATA[*/
    <#list gwtParameters?keys as param>
      var ${param} = "${gwtParameters[param]}";
    </#list>
    /*]]>*/
    </script>
  </#if>
  <div id="${id}" <#if class?has_content>class="${class}"</#if>></div>
</div>
</#macro>

<#-- Accounting tags macros -->
<#macro accountingTagsHidden tags entity prefix="tag" suffix="">
  <#list tags as tag>
    <@inputHidden name="${prefix}${tag.index}${suffix}" value=entity.get("acctgTagEnumId${tag.index}")! />
  </#list>
</#macro>

<#macro accountingTagsSelect tag prefix="tag" suffix="" entity="" tabIndex="" index=-1>
  <#if entity?has_content>
    <#assign default=entity.get("acctgTagEnumId${tag.index}")! />
  <#else>
    <#if tag.hasDefaultValue()>
      <#assign default=tag.defaultValue />
    </#if>
  </#if>
  <@inputSelect name="${prefix}${tag.index}${suffix}" errorField="${prefix}${tag.index}${suffix}" list=tag.tagValues key="enumId" required=false default=default tabIndex=ti index=index ; tagValue>
    ${tagValue.description}
  </@inputSelect>
</#macro>

<#macro accountingTagsSelectRows tags prefix="tag" suffix="" entity="" tabIndex="">
  <#assign ti=tabIndex />
  <#list tags as tag>
    <#if entity?has_content>
      <#assign default=entity.get("acctgTagEnumId${tag.index}")! />
    </#if>
    <#if !default?has_content && tag.hasDefaultValue()>
      <#assign default=tag.defaultValue />
    </#if>
    <#if tag.isRequired()>
      <#assign titleClass="requiredField" />
    <#else>
      <#assign titleClass="tableheadtext" />
    </#if>
    <@inputSelectRow titleClass=titleClass title=tag.description name="${prefix}${tag.index}${suffix}" errorField="${prefix}${tag.index}${suffix}" list=tag.tagValues key="enumId" required=false default=default tabIndex=ti ; tagValue>
      ${tagValue.description}
    </@inputSelectRow>
    <#if ti?has_content>
      <#assign ti=ti+1 />
    </#if>
  </#list>
</#macro>

<#macro accountingTagsSelectRowsForCart tags item index prefix="tag">
  <#list tags as tag>
    <#assign tagName="${prefix}${tag.index}" />
    <#if tag.isRequired()>
      <#assign titleClass="requiredField" />
    <#else>
      <#assign titleClass="tableheadtext" />
    </#if> 
    <#assign default=item.getAttribute(tagName)! />
    <#if !default?has_content && tag.hasDefaultValue()>
      <#assign default=tag.defaultValue />
    </#if>  
    <@inputSelectRow titleClass=titleClass title=tag.description name="${tagName}_${index}" list=tag.tagValues key="enumId" required=false default=default ; tagValue>
      ${tagValue.description}
    </@inputSelectRow>
  </#list>
</#macro>

<#macro accountingTagsDisplayRows tags entity>
  <#list tags as tag>
    <#list tag.tagValues as tagValue>
      <#if tagValue.enumId == entity.get("acctgTagEnumId${tag.index}")!>
        <@displayRow title=tag.description text=tagValue.description />
      </#if>
    </#list>
  </#list>
</#macro>

<#macro accountingTagsDisplay tags entity>
  <#list tags as tag>
    <#list tag.tagValues as tagValue>
      <#if tagValue.enumId == entity.get("acctgTagEnumId${tag.index}")!>
        <b>${tag.description}:</b> ${tagValue.description}
      </#if>
    </#list>
  </#list>
</#macro>

<#macro accountingTagsDisplayForCart tags item>
  <#list tags as tag>
    <#list tag.tagValues as tagValue>
      <#if tagValue.enumId == item.getAttribute("tag${tag.index}")!>
        <b>${tag.description}:</b> ${tagValue.description}
      </#if>
    </#list>
  </#list>
</#macro>
