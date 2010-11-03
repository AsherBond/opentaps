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

/* Initialize the 'namespaces' */
if (! opentaps) var opentaps = {};
if (! opentaps.postalAddress) opentaps.postalAddress = {};

opentaps.postalAddress.validationListener = function(/* String */ errorContainerId, /* String */ address1InputId, /* String */ address2InputId, /* String */ cityInputId, /* String */ stateSelectId, /* String */ countrySelectId, /* String */ postalCodeInputId, /* String */ postalCodeExtInputId, /* Element */ formElement) {
    
    // Check for the existence of the error message container
    var errorContainer = document.getElementById(errorContainerId);
    if (! errorContainer) return true;

    // Collapse the error container before anything else
    opentaps.expandCollapse(errorContainer, null, null, true);
    
    // Get the input elements
    var address1Input = formElement ? formElement[address1InputId] : document.getElementById(address1InputId);
    var address2Input = formElement ? formElement[address2InputId] : document.getElementById(address2InputId);
    var cityInput = formElement ? formElement[cityInputId] : document.getElementById(cityInputId);
    var stateSelect = formElement ? formElement[stateSelectId] : document.getElementById(stateSelectId);
    var countrySelect = formElement ? formElement[countrySelectId] : document.getElementById(countrySelectId);
    var postalCodeInput = formElement ? formElement[postalCodeInputId] : document.getElementById(postalCodeInputId);
    var postalCodeExtInput = formElement ? formElement[postalCodeExtInputId] : document.getElementById(postalCodeExtInputId);
    
    // Ignore non-USA addresses
    if (countrySelect && countrySelect.options[countrySelect.selectedIndex].value != 'USA') return true;
    
    // Retrieve the data from the elements
    var address1 = (address1Input && address1Input.value) ? address1Input.value : '';
    var postalCode = (postalCodeInput && postalCodeInput.value) ? postalCodeInput.value : '';
    var city = (cityInput && cityInput.value) ? cityInput.value : '';
    var state = '';
    if (stateSelect && stateSelect.options && stateSelect.options.length > 0 && stateSelect.options[stateSelect.selectedIndex].value) {
        state = stateSelect.options[stateSelect.selectedIndex].value;
    }
    
    // The validation service requires zip5 or city and state
    if (state == '' && city == '' && postalCode == '') return true;
    if (postalCode == '' && (state == '' || city == '')) return true;
    
    // Assemble the data
    var context = {};
    context['address1'] = address1;
    context['address2'] = address2Input ? address2Input.value : '';
    context['city'] = city;
    context['state'] = state;
    context['zip4'] = postalCodeExtInput ? postalCodeExtInput.value : '';
    context['zip5'] = postalCode;

    // Send the request
    var requestUrl =  (address1 == '') ? 'validatePostalCodeJSON' : 'validatePostalAddressJSON';
    opentaps.sendRequest(requestUrl, context, function(data) {opentaps.postalAddress.handleValidationResponse(errorContainer, address1Input, address2Input, cityInput, stateSelect, postalCodeInput, postalCodeExtInput, data)});
}

opentaps.postalAddress.handleValidationResponse = function(/* Element */ errorContainer, /* Element */ address1Input, /* Element */ address2Input, /* Element */ cityInput, /* Element */ stateSelect, /* Element */ postalCodeInput, /* Element */ postalCodeExtInput, /* Array */ data) {
    if (! data) return true;
    if (! errorContainer) return true;

    // Create a container to hold the data
    var dataContainer = opentaps.createDiv(null, null, 'data');
    var spacer = opentaps.createDiv(null, '&nbsp;');
    spacer.style.height = '10px';
    dataContainer.appendChild(spacer);

    var errorText = data._ERROR_MESSAGE_ ? data._ERROR_MESSAGE_ : '';
    if (errorText) {
        dataContainer.appendChild(opentaps.createDiv(null, errorText, 'error'));
        opentaps.replaceChildren(errorContainer, dataContainer);
    } else {

        // Assemble the data and insert into the container
        var address1Text = data.address1 ? data.address1 : (address1Input && address1Input.value) ? address1Input.value : '';
        if (address1Text) dataContainer.appendChild(opentaps.createDiv(errorContainer.id + '_address1', address1Text));
        var address2Text = data.address2 ? data.address2 : (address2Input && address2Input.value) ? address2Input.value : '';
        if (address2Text) dataContainer.appendChild(opentaps.createDiv(errorContainer.id + '_address2', address2Text));
        var cityStateDiv = opentaps.createDiv();
        var cityText = data.city ? data.city : (cityInput && cityInput.value) ? cityInput.value : '';
        var stateText = data.state ? data.state : (stateSelect && stateSelect.options) ? stateSelect.options[stateSelect.selectedIndex].value : '';
        if (cityText) {
            cityStateDiv.appendChild(opentaps.createSpan(errorContainer.id + '_city', cityText));
        }
        if (cityText && stateText) {
            cityStateDiv.appendChild(opentaps.createSpan(null, ', '));
        }
        if (stateText) cityStateDiv.appendChild(opentaps.createSpan(errorContainer.id + '_state', stateText));
        if (cityText || stateText) dataContainer.appendChild(cityStateDiv);
        var postalCodeDiv = opentaps.createDiv();
        var postalCodeText = data.zip5 ? data.zip5 : (postalCodeInput && postalCodeInput.value) ? postalCodeInput.value : '';
        var postalCodeExtText = data.zip4 ? data.zip4 : (postalCodeExtInput && postalCodeExtInput.value) ? postalCodeExtInput.value : '';
        if (postalCodeText) {
            postalCodeDiv.appendChild(opentaps.createSpan(errorContainer.id + '_zip5', postalCodeText));
        }
        if (postalCodeText && postalCodeExtText) {
            postalCodeDiv.appendChild(opentaps.createSpan(null, '-'));
        }
        if (postalCodeExtText) postalCodeDiv.appendChild(opentaps.createSpan(errorContainer.id + '_zip4', postalCodeExtText));
        if (postalCodeText || postalCodeExtText) dataContainer.appendChild(postalCodeDiv);
    
        var returnText = data.returnText ? data.returnText : '';
        if (returnText) dataContainer.appendChild(opentaps.createDiv(null, returnText, 'error'));

        // Create the action links and attach event handlers
        var linkDiv = opentaps.createDiv(null, null, 'links');
        var updateLink = opentaps.createImg(null, '/opentaps_images/buttons/glass_buttons_green_up.png', null, {'onclick' : 
            function (){
                opentaps.postalAddress.populateAddressFromValidationErrors(errorContainer, address1Input, address2Input, cityInput, stateSelect, postalCodeInput, postalCodeExtInput);
            }});
        linkDiv.appendChild(updateLink);
        var noUpdateLink = opentaps.createImg(null, '/opentaps_images/buttons/glass_buttons_red_X.png', null, {'onclick' : 
            function (){
                opentaps.expandCollapse(errorContainer, null, null, true);
            }});
        linkDiv.appendChild(noUpdateLink);

        // Add the data and action links to the error container and expand it
        opentaps.replaceChildren(errorContainer, dataContainer);
        errorContainer.appendChild(linkDiv);
    }
    // reset the height style attribute before expanding
    errorContainer.style.height = 'auto';
    opentaps.expandCollapse(errorContainer, null, true);

    return true;
}

opentaps.postalAddress.populateAddressFromValidationErrors = function(/* Element */ errorContainer, /* Element */ address1Input, /* Element */ address2Input, /* Element */ cityInput, /* Element */ stateSelect, /* Element */ postalCodeInput, /* Element */ postalCodeExtInput) {
    opentaps.postalAddress.populateFieldFromValidationError(address1Input, errorContainer.id + '_address1');    
    opentaps.postalAddress.populateFieldFromValidationError(address2Input, errorContainer.id + '_address2');    
    opentaps.postalAddress.populateFieldFromValidationError(cityInput, errorContainer.id + '_city');    
    opentaps.postalAddress.populateFieldFromValidationError(stateSelect, errorContainer.id + '_state');    
    opentaps.postalAddress.populateFieldFromValidationError(postalCodeInput, errorContainer.id + '_zip5');    
    opentaps.postalAddress.populateFieldFromValidationError(postalCodeExtInput, errorContainer.id + '_zip4');  

    // Collapse the error container
    opentaps.expandCollapse(errorContainer, null, null, true);
}

opentaps.postalAddress.populateFieldFromValidationError = function(/* Element */ node, /* String */ validationElementId) {
    if (! node) return;
    if (! validationElementId) return;
    var validationElement = document.getElementById(validationElementId);
    if (validationElement == null) return;
    
    // Get the data for the field from the div in the error container
    var errorValue = validationElement.innerHTML;
    
    // Get the value in the field
    var existingValue = opentaps.stripWhitespace(node.value.toUpperCase());
    
    // Replace the field value if the error value is populated and different
    if (errorValue && opentaps.stripWhitespace(errorValue.toUpperCase()) != opentaps.stripWhitespace(existingValue.toUpperCase())) {
        opentaps.replaceFormElementValue(node, errorValue);
    }
}

