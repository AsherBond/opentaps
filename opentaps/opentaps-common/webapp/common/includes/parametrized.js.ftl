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
<#compress>

<#-- Parametrized javascript that gets included in the header of a page. -->

<#--
    Function and data to allow state dropdown list to be changed when country changes.
    To activate it, run the stateDropdownData.bsh script.  Then add the following
    event handler to the countryGeoId dropdown:

        onChange="swapStatesInDropdown(this, 'stateProvinceGeoId')"

    Here, stateProvinceGeoId is the name of the state select input element.
    In a form widget, we would do the following:

        <field name="countryGeoId" event="onChange" action="swapStatesInDropdown(this, 'stateProvinceGeoId')"/>

    There is also an Opentaps form macro which transparently integrates this feature:

        <@inputStateCountry/>
-->
        
      <#if stateDropdownData?exists>
        var tmpArray;
        var countriesGlobal = new Array();
        var statesGlobal = new Array();
        <#assign countries = stateDropdownData.keySet() />
        <#list countries as country>
        countriesGlobal[${country_index}] = '${country}';
        tmpArray = new Array();
        <#assign states = stateDropdownData.get(country)/>
        <#list states as state>
        tmpArray[${state_index}] = new Array('${state.geoId}', "${state.geoName}");
        </#list>
        statesGlobal[${country_index}] = tmpArray;
        </#list>

        function swapStatesInDropdown(countryElement, stateElementName) {
            var stateElement = countryElement.form[stateElementName];
            var countryGeoId = countryElement[countryElement.selectedIndex].value;
            for (i = 0; i < countriesGlobal.length; i++)
                if (countriesGlobal[i] == countryGeoId) break;
            var states = statesGlobal[i];
            if (!states) states = new Array();

            // build the state options
            stateElement.options[0] = new Option('', ''); // first element is always empty
            for (i = 0; i < states.length; i++) {
                state = states[i];
                stateElement.options[i+1] = new Option(state[1], state[0]);
            }

            // by setting the length of the select option array, we can truncate it
            stateElement.options.length = states.length;
        }
      </#if>
</#compress>
