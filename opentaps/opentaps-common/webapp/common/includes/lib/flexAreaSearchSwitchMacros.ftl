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

<#-- 
Macros to help build Forms with multiple searrch options with different input field for each option.

To use these macros in your page, first put this at the top:

<@import location="component://opentaps-common/webapp/common/includes/lib/flexAreaSearchSwitchMacros.ftl"/>

Then include the JS code and give the selected search option.
<#assign selectedSearchOption = "..." >
<@searchFlexAreaJS searchOption=selectedSearchOption />

Define the possible choices:
<#assign searchChoices = {"option1":"label1", ... } >

You can now build the Form.
Here is an example, don't forget to include the search option field as below.

<form ... >
    [ important ]
    <input type="hidden" id="searchOption" name="searchOption" value="${selectedSearchOption}"/>

    [ each form section is defined in a flex area, only one section will be visible at any time ]
    <@searchFlexArea searchBy="option1" searchOption=selectedSearchOption >
      <table ="twoColumnForm">
        [ this is present in all sections, display the drop down box where the user can switch to another section ]
        <@switchSearchRow title=uiLabelMap.OpentapsFindBy selected="option1" choices=searchChoices />
        ...
        <@inputSubmitRow ... />
      </table>
    </@searchFlexArea>

-->

<#macro searchFlexAreaJS searchOption>
<script type="text/javascript">
/* <![CDATA[ */

  var currentSearchMode = '${searchOption}';

  function switchSearchMode(/* Element */ modeSwitch) {
    var selected = modeSwitch.options[modeSwitch.selectedIndex].value;
    if (selected == currentSearchMode) return;

    var currentElement = document.getElementById('searchBy_' + currentSearchMode);
    var targetElement  = document.getElementById('searchBy_' + selected);

    var hiddenSearchOption = document.getElementById('searchOption');
    var inputs = targetElement.getElementsByTagName('input');
    otherModeSwitch = document.getElementById('modeSwitch_' + selected);
    hiddenSearchOption.value = selected;
    
    for (var x = 0; x < inputs.length; x++) {
      if (inputs[x].type != 'submit') inputs[x].value = '';
    }

    otherModeSwitch.selectedIndex = modeSwitch.selectedIndex;

    opentaps.expandCollapse(currentElement);
    opentaps.expandCollapse(targetElement);

    currentSearchMode = selected;    
    
  }
/* ]]> */
</script>
</#macro>

<#macro searchFlexArea searchBy searchOption>
  <@flexArea targetId="searchBy_${searchBy}" save=false state=(searchOption == searchBy)?string("open", "closed") enabled=false controlClassClosed="hidden" controlClassOpen="hidden" style="border:0px">
    <#nested >
  </@flexArea>
</#macro>

<#macro switchSearchCell selected choices>
<td>
  <select id="modeSwitch_${selected}" class="inputBox" onchange="switchSearchMode(this)">
  <#list choices?keys as k>
    <option <#if selected == k>selected="selected"</#if> value="${k}">${choices[k]}</option>
  </#list>
  </select>
</td>
</#macro>

<#macro switchSearchRow title selected choices>
<tr>
  <@displayTitleCell title=title />
  <@switchSearchCell selected=selected choices=choices />
  <td/>
</tr>
</#macro>
