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

<script type="text/javascript">

  checkIntervalMillis = ${internalMessageCheckFrequencySeconds?default(300)} * 1000;
  
  opentaps.addOnLoad(initializeMessageCheck);

  function initializeMessageCheck() {
    checkInterval = setInterval("checkForNewMessages()", checkIntervalMillis);
    checkForNewMessages();
  }

  function checkForNewMessages() {
    var requestData = {'partyIdTo' : '${userLogin.partyId}', 'returnNumberOnly' : 'Y'};
    opentaps.sendRequest("getNewInternalMessagesJSON", requestData, updateMessages)
  }

  function updateMessages(/* Array */ data) {
    if (! data) return;
    var numMessages = document.getElementById('numMessages');
    if (! numMessages) return;
    if (data.numberOfNewMessages == 0) {
        opentaps.replaceChildren(numMessages, opentaps.createSpan('numMessages', '${uiLabelMap.OpentapsNoNewMessages?js_string}'));
    } else {

        // TODO: Localize this properly, without dictating position of the link
        var newNumMessages = opentaps.createSpan('numMessages', '${uiLabelMap.OpentapsNewMessagesPrefix}&nbsp;' + data.numberOfNewMessages + '&nbsp;');
        opentaps.replaceChildren(numMessages, newNumMessages);
        newNumMessages.appendChild(opentaps.createAnchor(null, 'myMessages?isRead=N', '${uiLabelMap.OpentapsNewMessages}.', 'linktext'));
    }
  }
</script>

<div class="tabletext" style="margin-bottom:5px">
<span id="numMessages">${uiLabelMap.OpentapsNoNewMessages}</span>&nbsp;&nbsp;<a class="linktext" href="javascript:sendMessage()">${uiLabelMap.OpentapsSendMessage}</a>&nbsp;&nbsp;<a class="linktext" href="<@ofbizUrl>myMessages</@ofbizUrl>">Inbox</a>
</div>
