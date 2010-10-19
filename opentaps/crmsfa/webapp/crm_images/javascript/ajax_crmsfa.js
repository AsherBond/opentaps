/*
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
 */

/*
This implementation of autocomplete uses prototype and is designed so that the parameter is passed
as a hidden ID field but the text input shows a search string or an autocompleted result.
For example, if the input is for a Person,



It consists of four components:  a Hidden input, a Search input, a Dropdown div, and a validation Function.  
If the field is partyId, the setup is as follows:

<input type="hidden" name="partyId" id="partyIdHidden" value=""/>
<input type="text" name="partyIdSearch" id="partyIdSearch" value="${defaultValue}"/>
<div id="partyIdDropdown"></div>
<script type="text/javascript"><!--
   var partyIdFunction = initAjaxAutocomplete(${requestUriForAutocomplete}, 'partyId');
//--></script>



*/

// global increment counter
var ajaxAutocompleteId = 0;

// Gets the next ajax autocomplete ID and increments the counter
function getNextAjaxAutocompleteId() {
  autocompleteId = ajaxAutocompleteId;
  ajaxAutocompleteId = ajaxAutocompleteId + 1;
  return autocompleteId;
}

// initialize autocomplete, returns a function which should be saved as follows: var ${paramName}Function = initAjaxAutocomplete()
function initAjaxAutocomplete(autocompleteUri, paramName) {
  return initAjaxAutocompleteWithId(autocompleteUri, paramName, -1);
}
function initAjaxAutocompleteUnique(autocompleteUri, paramName) {
  return initAjaxAutocompleteWithId(autocompleteUri, paramName, getNextAjaxAutocompleteId());
}
function initAjaxAutocompleteWithId(autocompleteUri, paramName, autocompleteId) {
    var param = (autocompleteId >= 0 ? paramName + autocompleteId : paramName);
    var hiddenElementId = param + 'Hidden';
    var searchElementId = param + 'Search';
    var dropdownElementId = param + 'Dropdown';
    var functionName = param + 'Function';

    // when item is selected, record the selected ID and Name and highlight the input to inform user of success
    var updateFunction = function(inputField, selectedItem) {
        document.getElementById(hiddenElementId).value = selectedItem.getAttribute('entityId');
        new Effect.Highlight(searchElementId);
    }

    // Binds an autocomplete event to the searchString input, the dropdown div, the request URI, and function to run when user selects an item
    new Ajax.Autocompleter(searchElementId, dropdownElementId, autocompleteUri, {afterUpdateElement: updateFunction})

    // input validation function TODO: handle case of no input: avoid pulsing (there's a weird bug with entering "&" or other non-alphanum chars)
    var validateResponseFunction = function(request) {
        var update = updateFunction;
        var list = request.responseXML.getElementsByTagName('li');
        if (!list || list.length == 0) {
            // If no results, then warn the user with a pulse and clear hidden field
            new Effect.Pulsate(searchElementId);
            document.getElementById(hiddenElementId).value = '';
        } else if (list && list.length >= 1) {
            // If more than one match, pick the first item and autocomplete it
            var selectedItem = list[0];
            document.getElementById(searchElementId).value = selectedItem.firstChild.nodeValue;
            update(null, selectedItem);
        }
    }

    // Bind the onBlur event of the search input to the validation function
    var searchField = document.getElementById(searchElementId);
    searchField.setAttribute('onBlur', 'ajaxValidateAutocomplete(\'' + searchElementId + '\',\'' + autocompleteUri + '\', ' + functionName + ')');

    // return the validate function, which should be set by the form using the functionName
    return validateResponseFunction;
}

// In order to validate the autocomplete results correctly, use onBlur to call this function.
// Input parameters are the ID of the search field, the URI that will do the validation, and a function to check the results of the URI request.
function ajaxValidateAutocomplete(elementId, uri, validateFunction) {
    var searchString = document.getElementById(elementId).value;
    new Ajax.Updater(elementId, uri, {asynchronous: true, method: 'get', parameters: 'searchString='+searchString, onSuccess: validateFunction});
}
