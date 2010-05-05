<#if firstName?has_content || lastName?has_content>
Dear ${firstName?if_exists}<#if lastName?has_content>&nbsp;</#if>${lastName?if_exists},<p>
</#if>

According to your request the catalog was sent to address:<p>

&nbsp;&nbsp;${address1}<br>
<#if address2?has_content>${address2?if_exists}&nbsp;&nbsp;<br></#if>
&nbsp;&nbsp;${city}, ${stateProvinceGeoId?if_exists} ${postalCode?if_exists}, ${countryName?if_exists}
<p>
Best regards,<br>
&nbsp;&nbsp;&nbsp;Sales Team