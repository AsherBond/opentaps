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

<table class="crmsfaListTable" cellspacing="0">
  <tr class="crmsfaListTableHeader">
    <td><span class="tableheadtext">${uiLabelMap.FormFieldTitle_noteInfo}</span></td>
    <td><span class="tableheadtext">${uiLabelMap.CommonCreatedBy}</span></td>
    <td><span class="tableheadtext">${uiLabelMap.FormFieldTitle_noteDateTime}</span></td>
    <td colspan="2"></td>
  </tr>

<#if notesList?has_content>
  <#assign counter = 0>
  <#list notesList as note>
    <#if (counter % 2 == 0)>
      <#assign trclass = "rowWhite"/>
    <#else>
      <#assign trclass = "rowLightGray"/>
    </#if>
    <tr class="${trclass}" id="${getIndexedName('noteInfoRow', counter)}"  name="${getIndexedName('noteInfoRow', counter)}">
      <td><span class="tabletext" id="${getIndexedName('noteInfo', counter)}"  name="${getIndexedName('noteInfo', counter)}">
          ${note.noteInfo?replace("(\r\n|\r|\n|\n\r)", "<br/>", "r")?if_exists}
      </span></td>
      <#assign name = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, note.noteParty, false)>
      <td><span class="tabletext">${name}</span></td>
      <td><span class="tabletext"><@displayDate date=note.noteDateTime format="DATE_TIME"/></span></td>
      <#if Static["com.opensourcestrategies.crmsfa.security.CrmsfaSecurity"].hasNotePermission(security, hasModulePermission!"", "_UPDATE", userLogin, note.noteId, note.targetPartyId!, note.custRequestId!)>
        <td>
          <a style="display: block;" id="${getIndexedName('editNoteInfo', counter)}" name="${getIndexedName('editNoteInfo', counter)}" href="javascript:editNotes(${counter})">
            <img src="<@ofbizContentUrl>/opentaps_images/edit.gif</@ofbizContentUrl>" width="22" height="21" border="0" alt="${uiLabelMap.CommonEdit}"/>
          </a>
          <a style="display: none;" id="${getIndexedName('saveNoteInfo', counter)}" name="${getIndexedName('saveNoteInfo', counter)}" href="javascript:saveNotes(${counter}, ${note.noteId})">
            <img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/save.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonSave}"/>
          </a>
        </td>
      <#else>
        <td/>
      </#if>

      <#if Static["com.opensourcestrategies.crmsfa.security.CrmsfaSecurity"].hasNotePermission(security, hasModulePermission!"", "_DELETE", userLogin, note.noteId, note.targetPartyId!, note.custRequestId!)>
        <td>
          <a style="display: block;" id="${getIndexedName('deleteNoteInfo', counter)}" name="${getIndexedName('deleteNoteInfo', counter)}" href="javascript:deleteNotes(${counter}, ${note.noteId})">
            <img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/delete.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonDelete}"/>
          </a>
          <a style="display: none;" id="${getIndexedName('cancelNoteInfo', counter)}" name="${getIndexedName('cancelNoteInfo', counter)}" href="javascript:cancelNotes(${counter})">
            <img src="<@ofbizContentUrl>/images/dojo/src/widget/templates/buttons/cancel.gif</@ofbizContentUrl>" width="18" height="18" border="0" alt="${uiLabelMap.CommonCancel}"/>
          </a>
        </td>
      <#else>
        <td/>
      </#if>
    </tr>
    <#assign counter = counter + 1>
    <#if note.targetPartyId?exists>
      <#assign targetId = note.targetPartyId/>
      <#assign paramName = "partyId"/>
    <#elseif note.custRequestId?exists>
      <#assign targetId = note.custRequestId>
      <#assign paramName = "custRequestId"/>
    </#if>
  </#list>

  <script type="text/javascript">
  /*<![CDATA[*/
    function displayEditDelete(i) {
      var edit = document.getElementById('editNoteInfo_o_' + i);
      var save = document.getElementById('saveNoteInfo_o_' + i);
      var cancel = document.getElementById('cancelNoteInfo_o_' + i);
      var del = document.getElementById('deleteNoteInfo_o_' + i);
      edit.style.display = 'block';
      save.style.display = 'none';
      cancel.style.display = 'none';
      del.style.display = 'block';
    }

    function displaySaveCancel(i) {
      var edit = document.getElementById('editNoteInfo_o_' + i);
      var save = document.getElementById('saveNoteInfo_o_' + i);
      var cancel = document.getElementById('cancelNoteInfo_o_' + i);
      var del = document.getElementById('deleteNoteInfo_o_' + i);
      edit.style.display = 'none';
      save.style.display = 'block';
      cancel.style.display = 'block';
      del.style.display = 'none';
    }

    function editNotes(i) {
      var node = document.getElementById('noteInfo_o_' + i);
      var text = opentaps.trim(node.innerHTML);
      node.innerHTML = '';
      var newInput = opentaps.createTextarea('edit' + node.id, 'edit' + node.id, 'inputBox', null, text, 2, 60);
      node.appendChild(newInput);
      newInput = opentaps.createInput('hidden' + node.id, 'hidden' + node.id, 'hidden', 'inputBox', null, text, null);
      node.appendChild(newInput);
      displaySaveCancel(i);
    }

    function saveNotes(i, noteId) {
      var input = document.getElementById('editnoteInfo_o_' + i);
      if (input == null)
        return;
      var text = input.value;
      var requestData = {'counter' : i, 'modulePermission' : '${hasModulePermission?default("")}', 'noteId' : noteId, '${paramName}' : '${targetId?default("")}', 'text' : text};
      opentaps.sendRequest('saveNotesJSON', requestData, saveNotesCallBack);
    }

    function saveNotesCallBack(/* Array */ data) {
      if (! data) return;
      if (data.counter == -1) {
        alert(data.text);
      }
      uneditNotes(data.counter, data.text);
    }

    function cancelNotes(i) {
      var input = document.getElementById('hiddennoteInfo_o_' + i);
      if (input == null)
        return;
      var text = input.value;
      uneditNotes(i, text);
    }

    function uneditNotes(i, text) {
      var node = document.getElementById('noteInfo_o_' + i);
      node.innerHTML = text;
      displayEditDelete(i);
    }

    function deleteNotes(i, noteId) {
      var answer = confirm('${StringUtil.wrapString(uiLabelMap.OpentapsAreYouSure)}');
      if (answer) {
        var requestData = {'counter' : i, 'modulePermission' : '${hasModulePermission?default("")}', 'noteId' : noteId, '${paramName}' : '${targetId?default("")}'};
        opentaps.sendRequest('deleteNotesJSON', requestData, deleteNotesCallBack);
      }
    }

    function deleteNotesCallBack(/* Array */ data) {
      if (! data) return;
      if (data.counter == -1) {
        alert(data.text);
      }
      var node = document.getElementById('noteInfoRow_o_' + data.counter);
      node.innerHTML = '';
    }
  /*]]>*/
  </script>

</#if>

</table>
