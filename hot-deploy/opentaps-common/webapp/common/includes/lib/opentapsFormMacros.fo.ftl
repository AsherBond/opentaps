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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.fo.ftl"/>

Then each one can be used as a macro right in an FTL like this:
<@displayCurrency amount=10.00 currencyUomId="USD" />

For more information, please see documentation/opentapsFormMacros.html
-->

<#macro displayAddress postalAddress={} partyName="">
    <#if postalAddress?has_content>
        <#if postalAddress.stateProvinceGeoId?has_content>
          <#assign stateGeo = delegator.findByPrimaryKeyCache("Geo", Static["org.ofbiz.base.util.UtilMisc"].toMap("geoId", postalAddress.stateProvinceGeoId))?default({})>
        <#else>
          <#assign stateGeo = {}>
        </#if>
        <#if postalAddress.countryGeoId?has_content>
            <#assign countryGeo = delegator.findByPrimaryKeyCache("Geo", Static["org.ofbiz.base.util.UtilMisc"].toMap("geoId", postalAddress.countryGeoId))?default({})>
        <#else>
            <#assign countryGeo = {}>
        </#if>
        <#assign functionName = Static["org.opentaps.common.util.UtilConfig"].getAddressFormattingFunction(postalAddress.countryGeoId?default(""))>
        ${(functionName + "(postalAddress, stateGeo, countryGeo)")?eval}
    <#elseif partyName?has_content>
        <fo:block>${uiLabelMap.CommonTo}: ${partyName}</fo:block>
    </#if>
</#macro>

<#function displayAddressDefault postalAddress stateGeo countryGeo>
<#escape x as x?xml>
    <#assign text>
    <#if postalAddress.toName?has_content>
        <fo:block>${postalAddress.toName}</fo:block>
    </#if>
    <#if postalAddress.attnName?has_content>
        <fo:block>${uiLabelMap.CommonAttn}: ${postalAddress.attnName}</fo:block>
    </#if>
    <fo:block>${postalAddress.address1?if_exists}</fo:block>
    <#if postalAddress.address2?has_content>
        <fo:block>${postalAddress.address2}</fo:block>
    </#if>
    <fo:block>${postalAddress.city?if_exists} ${stateGeo.abbreviation?default("")} ${postalAddress.postalCode?if_exists}</fo:block>
    <fo:block>${countryGeo.abbreviation?if_exists}</fo:block>
    </#assign>
    <#return text>
</#escape>
</#function>

<#-- Standard header generating macro -->

<#macro opentapsHeaderFO leftWidth="proportional-column-width(3)" rightWidth="proportional-column-width(2)">
<#escape x as x?xml>
    <fo:table table-layout="fixed">
        <fo:table-column column-width="${leftWidth}"/>
        <fo:table-column column-width="${rightWidth}"/>
        <fo:table-body>
            <fo:table-row >
                <fo:table-cell>
                    ${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#OrganizationHeader")}
                </fo:table-cell>
                <fo:table-cell>
                    <#nested>
                </fo:table-cell>
            </fo:table-row>
        </fo:table-body>
    </fo:table>
</#escape>
</#macro>

<#macro opentapsFooterFO>
<#escape x as x?xml>
    ${screens.render("component://opentaps-common/widget/screens/common/CommonScreens.xml#OrganizationFooter")}
</#escape>
</#macro>

<#-- Format should be one of the DATE_TIME, DATE, TIME for Timestamp and DATE_ONLY for Date -->
<#macro displayDateFO date="" format="DATE">
<#escape x as x?xml>
  <#if !locale?has_content><#assign locale = Static["org.ofbiz.base.util.UtilHttp"].getLocale(request)/></#if>
  <#if !timeZone?has_content><#assign timeZone = Static["org.ofbiz.base.util.UtilHttp"].getTimeZone(request)/></#if>
  <#if date?has_content && date?is_date>
    <#if format == "DATE_TIME">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getDateTimeFormat(locale)/>
      ${Static["org.ofbiz.base.util.UtilDateTime"].timeStampToString(date?datetime, fmt, timeZone, locale)}
    <#elseif format == "DATE">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)/>
      ${Static["org.ofbiz.base.util.UtilDateTime"].timeStampToString(date?datetime, fmt, timeZone, locale)}
    <#elseif format == "TIME">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getTimeFormat(locale)/>
      ${Static["org.ofbiz.base.util.UtilDateTime"].timeStampToString(date?datetime, fmt, timeZone, locale)}
    <#elseif format == "DATE_ONLY">
      <#assign fmt = Static["org.opentaps.common.util.UtilDate"].getDateFormat(locale)/>
      ${date?date?string(fmt)}
    </#if>
  </#if> 
</#escape>
</#macro>
