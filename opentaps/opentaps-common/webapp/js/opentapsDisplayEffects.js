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

/* Initialize the 'namespace' */
if (! opentaps) var opentaps = {};


/* Opentaps display functions */

var times = {};
opentaps.expansionListener = function(/* Event */ evt) {
    var targetControl = evt.target;
    if (! targetControl.id.match('.*_flexAreaControl$')) return;
    var targetId = targetControl.id.replace('_flexAreaControl','');
    var target = document.getElementById(targetId);
    if (! target) return false;
    var last = times[targetControl.id];
    if (! last) last = 0;
    var now = new Date().getTime();
    if (now - last <= 300) return false;
    times[targetControl.id] = now;
    opentaps.expandCollapse(target, targetControl);
}

opentaps.expandCollapse = function(/* Object */ target, /* Object */ targetControl, /* Boolean */ forceOpen, /* Boolean */ forceClosed) {
    if (! target) {
        return false;
    } else if (typeof(target) == 'string') {
        var theTarget = document.getElementById(target);
        if (! theTarget) return false;
        target = theTarget;
    } else if (typeof(target) != 'object') {
        return false;
    }

    if (target.anim) {
      target.anim.stop();
    }
    
    if (! targetControl) {
        targetControl = target.id + '_flexAreaControl';
    }
    if (typeof(targetControl) == 'string') {
        var theTargetControl = document.getElementById(targetControl);
        if (! theTargetControl) {
            return false;
        }
        targetControl = theTargetControl;
    } else if (typeof(targetControl) != 'object') {
        return false;
    }

    var open = 'true' == target.getAttribute('open');
    if (forceOpen && open) return true;
    if (forceClosed && ! open) return true;
    var openContainerClass = target.getAttribute('openContainerClass') ? target.getAttribute('openContainerClass') : 'flexAreaContainer_open';
    var closedContainerClass = target.getAttribute('closedContainerClass') ? target.getAttribute('closedContainerClass') : 'flexAreaContainer_closed';
    opentaps.replaceClass(target, open ? closedContainerClass : openContainerClass, open ? openContainerClass : closedContainerClass);
    var openControlClass = targetControl.getAttribute('openControlClass') ? targetControl.getAttribute('openControlClass') : 'flexAreaControl_open';
    var closedControlClass = targetControl.getAttribute('closedControlClass') ? targetControl.getAttribute('closedControlClass') : 'flexAreaControl_closed';
    opentaps.replaceClass(targetControl, open ? closedControlClass : openControlClass, open ? openControlClass : closedControlClass);
    target.anim = opentaps.shrinkAndFade(target);
    target.setAttribute('open', open?'false':'true');
    var save = ('true' == targetControl.getAttribute('save'));
    if (save) {
        var applicationName = targetControl.getAttribute('application');
        var screenName = targetControl.getAttribute('screenName');
        opentaps.sendRequest('persistViewExpansionState', {'domId' : target.id, "application" : applicationName, "screenName" : screenName, 'viewState' : open ? 'closed' : 'open'});
    }
    return target.anim;
}


/* Events for every page load */

opentaps.addOnLoad(function(){opentaps.addListenerToNode(document.body, 'onclick', opentaps.expansionListener)});
