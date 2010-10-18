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

<#macro displayAddress member>
  <fo:block text-align="right" font-size="6pt">${member.sortResult}</fo:block>
  <fo:block>${member.toName}</fo:block>
  <#if member.attnName?exists>
  <fo:block>${member.attnName}</fo:block>
  </#if>
  <fo:block>${member.address1}</fo:block>
  <#if member.address2?exists>
  <fo:block>${member.address2}</fo:block>
  </#if>
  <fo:block>${member.city?if_exists} ${member.stateProvinceGeoId?if_exists} ${member.postalCode?if_exists}</fo:block>
</#macro>

<#escape x as x?xml>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format"
    <#if defaultFontFamily?has_content>font-family="${defaultFontFamily}"</#if>
>

  <#-- master layout specifies the overall layout of the pages and its different sections. -->
  <fo:layout-master-set>
    <fo:simple-page-master master-name="label-body"
      page-width="${pageWidth}${uom}" page-height="${pageHeight}${uom}"
      margin-top="${topMargin}${uom}" margin-bottom="${bottomMargin}${uom}"
      margin-left="${leftMargin}${uom}" margin-right="${rightMargin}${uom}">
      <fo:region-body/>
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="label-body" initial-page-number="1" font-size="9pt" font-family="Arial,sans-serif">
    <fo:flow flow-name="xsl-region-body">
      <fo:table>

        <#-- create label and spacing columns -->
        <#list 1..pageColumns as column>
        <fo:table-column column-width="${labelWidth}${uom}"/>
        <#if column != pageColumns>
        <fo:table-column column-width="${widthBtwLabel}${uom}"/>
        </#if>
        </#list>

        <fo:table-body>
          <#assign index = 0/>
          <#assign row = 0/>
          <#if members?has_content>
            <#list members as member>
              <#if (index % pageColumns) == 0> <#-- is it the first cell of this column? -->
                <fo:table-row height="${labelHeight}${uom}"> <#-- note: height maps onto block-progression-dimension.maximum, optimum and minimum -->
              </#if>
              <fo:table-cell>
                <fo:block margin="0.0625in">
                  <@displayAddress member=member/>
                </fo:block>
              </fo:table-cell>
              <#if (index % pageColumns) == (pageColumns-1)>  <#-- row break -->
                <#assign row=row+1 />
                </fo:table-row>
                <#if heightBtwLabel != 0>
                  <fo:table-row height="${heightBtwLabel}${uom}"><fo:table-cell><fo:block/></fo:table-cell></fo:table-row>
                </#if>
              <#else>  <#-- otherwise insert a spacing column -->
                <fo:table-cell><fo:block/></fo:table-cell>
              </#if>
              <#assign index = index + 1/>
            </#list>

            <#if index != 0 && index % pageColumns != 0> <#-- row break on the final row if say we have 2 final columns -->
              <#assign row=row+1 />
              </fo:table-row>
            </#if>
          <#else>
            <fo:table-row height="${labelHeight}${uom}"><fo:table-cell><fo:block margin="0.0625in">No members to print.</fo:block></fo:table-cell></fo:table-row>
          </#if>
        </fo:table-body>
      </fo:table>
    </fo:flow>
  </fo:page-sequence>
</fo:root>
</#escape>
