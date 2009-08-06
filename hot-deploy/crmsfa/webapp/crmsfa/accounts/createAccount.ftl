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

<@import location="component://opentaps-common/webapp/common/includes/lib/opentapsFormMacros.ftl"/>

${screens.render("component://crmsfa/widget/crmsfa/screens/accounts/AccountsScreens.xml#createAccountForm")}
<#if validatePostalAddresses>
  <div id="postalAddressValidationContainer" class="tabletext">
      <@flexArea targetId="postalAddressValidationError" class="postalAddressValidationError" controlClassOpen="hidden" controlClassClosed="hidden" state="closed" enabled=false><div>lskdjf</div></@flexArea>
  </div>
  <script type="text/javascript">
      opentaps.addListenerToNode(document.forms['createAccountForm'].generalPostalCode, 'onchange', function(){opentaps.postalAddress.validationListener('postalAddressValidationError', 'generalAddress1', 'generalAddress2', 'generalCity', 'generalStateProvinceGeoId', 'generalCountryGeoId', 'generalPostalCode', 'generalPostalCodeExt', document.forms['createAccountForm'])})
      document.forms['createAccountForm'].generalPostalCodeExt.parentNode.appendChild(document.getElementById('postalAddressValidationContainer'));
      document.forms['createAccountForm'].generalPostalCodeExt.parentNode.parentNode.childNodes[1].vAlign = 'top';
  </script>
</#if>