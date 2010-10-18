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

${screens.render("component://crmsfa/widget/crmsfa/screens/leads/LeadsScreens.xml#createLeadForm")}
<#if validatePostalAddresses>
  <div id="postalAddressValidationContainer" class="tabletext">
      <@flexArea targetId="postalAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false><div>lskdjf</div></@flexArea>
  </div>
  <script type="text/javascript">
      opentaps.addListenerToNode(document.forms['createLeadForm'].generalPostalCode, 'onchange', function(){opentaps.postalAddress.validationListener('postalAddressValidationError', 'generalAddress1', 'generalAddress2', 'generalCity', 'generalStateProvinceGeoId', 'generalCountryGeoId', 'generalPostalCode', 'generalPostalCodeExt', document.forms['createLeadForm'])})
      document.forms['createLeadForm'].generalPostalCodeExt.parentNode.appendChild(document.getElementById('postalAddressValidationContainer'));
      document.forms['createLeadForm'].generalPostalCodeExt.parentNode.parentNode.childNodes[1].vAlign = 'top';
  </script>
</#if>