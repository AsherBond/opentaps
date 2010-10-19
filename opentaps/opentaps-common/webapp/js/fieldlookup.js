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
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* Portions of this file came from Apache OFBIZ.  This file has been modified by Open Source Strategies, Inc. */


// ================= FIELD LOOKUP METHODS ============================
var lookups = [];

var target = null;
var target2 = null;

function getX(popupWidth) {
  return screen.width / 2 - (popupWidth / 2);	
}
function getY(popupHeight) {
  return screen.height / 2 - (popupHeight / 2);
}

function call_fieldlookup(target, viewName, formName, viewWidth, viewheight) {   
  var fieldLookup = new fieldLookup1(target);  
  if (! viewWidth) viewWidth = 350;
  if (! viewheight) viewheight = 200;
  fieldLookup.popup(viewName, formName, viewWidth, viewheight);
}
function call_fieldlookup2(target, viewName) {   
  var fieldLookup = new fieldLookup1(target, arguments);  
  fieldLookup.popup2(viewName);
}
function call_fieldlookup2autocomplete(targetCombo, targetHidden, viewName) {   
  var fieldLookup = new fieldLookup1autocomplete(targetCombo, targetHidden, arguments);  
  fieldLookup.popup2(viewName);
}
function call_fieldlookup3(target, target2, viewName) {
  var fieldLookup = new fieldLookup2(target, target2);
  fieldLookup.popup2(viewName);
}

function fieldLookup1(obj_target, args) {
  this.args = args;
  // passing methods
  this.popup = lookup_popup1;
  this.popup2 = lookup_popup2;
  
  // validate input parameters
  if (!obj_target)
    return lookup_error("Error calling the field lookup: no target control specified");
  if (obj_target.value == null)
    return cal_error("Error calling the field lookup: parameter specified is not valid target control");
  //this.target = obj_target; 
  target = obj_target; 

  // register in global collections
  //this.id = lookups.length;
  //lookups[this.id] = this;
}
function fieldLookup1autocomplete(_targetCombo, _targetHidden, args) {
  this.args = args;
  // passing methods
  this.popup = lookup_popup1;
  this.popup2 = lookup_popup2;
  
  // validate input parameters
  if (!_targetCombo)
    return lookup_error("Error calling the field lookup: no target autocompleter control specified");
  if (_targetCombo.value == null)
    return cal_error("Error calling the field lookup: parameter specified is not valid target autocompleter control");
  target = _targetCombo; 

  if (!_targetHidden)
    return lookup_error("Error calling the field lookup: no hidden target control specified");
  if (_targetHidden.value == null)
    return cal_error("Error calling the field lookup: parameter specified is not valid hidden target control");
  targetHidden = _targetHidden; 

}
function fieldLookup2(obj_target, obj_target2) {
  // passing methods
  this.popup = lookup_popup1;
  this.popup2 = lookup_popup2;

  // validate input parameters
  if (!obj_target)
    return lookup_error("Error calling the field lookup: no target control specified");
  if (obj_target.value == null)
    return cal_error("Error calling the field lookup: parameter specified is not valid target control");
  target = obj_target;
  // validate input parameters
  if (!obj_target2)
    return lookup_error("Error calling the field lookup: no target control specified");
  if (obj_target2.value == null)
    return cal_error("Error calling the field lookup: parameter specified is not valid target control");
  target2 = obj_target2;


  // register in global collections
  //this.id = lookups.length;
  //lookups[this.id] = this;
}

function lookup_popup1 (view_name, form_name, viewWidth, viewHeight) {
  var obj_lookupwindow = window.open(view_name + '?formName=' + form_name + '&id=' + this.id,'FieldLookup', 'width='+viewWidth+',height='+viewHeight+',scrollbars=yes,status=no,resizable=yes,top='+getY(viewHeight)+',left='+getX(viewWidth)+',dependent=yes,alwaysRaised=yes');
  obj_lookupwindow.opener = window;
  obj_lookupwindow.focus();
}
function lookup_popup2 (view_name) {
  var argString = "";
  if (this.args.length > 2) {
    for(var i=2; i < this.args.length; i++) {
      argString += "&parm" + (i-2) + "=" + this.args[i];
    }
  }
  var sep = "?";
  if (view_name.indexOf("?") >= 0) {
    sep = "&";
  }
  var viewWidth = 750;
  var viewHeight = 550;
  var obj_lookupwindow = window.open(view_name + sep + 'id=' + this.id + argString,'FieldLookup', 'width='+viewWidth+',height='+viewHeight+',scrollbars=yes,status=no,resizable=yes,top='+getY(viewHeight)+',left='+getX(viewWidth)+',dependent=yes,alwaysRaised=yes');
  obj_lookupwindow.opener = window;
  obj_lookupwindow.focus();
}
function lookup_error (str_message) {
  alert (str_message);
  return null;
}
