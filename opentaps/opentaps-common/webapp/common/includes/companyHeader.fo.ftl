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
<#escape x as x?xml>
<fo:block text-align="left">
    <#if organizationLogoImageUrl?has_content><fo:external-graphic src="${organizationLogoImageUrl}" overflow="hidden" height="40px" content-height="scale-to-fit"/></#if>
</fo:block>
<#if organizationPartyId?exists>
<fo:block white-space-collapse="false" ></fo:block> 
<fo:table>
    <fo:table-column column-width="3.5in"/>
    <fo:table-column column-width="3in"/>
       <fo:table-body>              
          <fo:table-row >
             <fo:table-cell>
                <fo:block font-size="9pt">${organizationCompanyName}</fo:block>
                   <#if organizationPostalAddress?has_content>
                      <fo:block font-size="9pt">${organizationPostalAddress.address1?if_exists}</fo:block>
                      <#if organizationPostalAddress.address2?has_content><fo:block font-size="9pt">${organizationPostalAddress.address2?if_exists}</fo:block></#if>
                      <fo:block font-size="9pt">${organizationPostalAddress.city?if_exists}, ${stateProvinceAbbrv?if_exists} ${organizationPostalAddress.postalCode?if_exists}, ${countryName?if_exists}</fo:block>
                   </#if>
             </fo:table-cell>
          </fo:table-row>
        <#if website?exists>
          <fo:table-row >
            <fo:table-cell>
              <fo:block font-size="9pt">${website}</fo:block>
            </fo:table-cell>
          </fo:table-row >
        </#if>
        <#if primaryPhone?exists>
          <fo:table-row >
            <fo:table-cell>
              <fo:block font-size="9pt">${uiLabelMap.CrmPrimaryPhone}: ${primaryPhone}</fo:block>
            </fo:table-cell>
          </fo:table-row >
        </#if>
        <#if primaryFax?exists>
          <fo:table-row >
            <fo:table-cell>
              <fo:block font-size="9pt">${uiLabelMap.PartyContactFaxPhoneNumber}: ${primaryFax}</fo:block>
            </fo:table-cell>
          </fo:table-row >
        </#if>
       </fo:table-body>
</fo:table>
</#if>
</#escape>
