/*
	Copyright (c) 2004-2007, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

/*
	This is a compiled version of Dojo, built for deployment and not for
	development. To get an editable version, please visit:

		http://dojotoolkit.org

	for documentation and information on getting the source.
*/

if(!dojo._hasResource["dojo.fx"]){dojo._hasResource["dojo.fx"]=true;dojo.provide("dojo.fx");dojo.provide("dojo.fx.Toggler");dojo.fx.chain=function(_1){var _2=_1.shift();var _3=_2;dojo.forEach(_1,function(_4){dojo.connect(_3,"onEnd",_4,"play");_3=_4;});return _2;};dojo.fx.combine=function(_5){var _6=_5.shift();dojo.forEach(_5,function(_7){dojo.forEach(["play","pause","stop"],function(_8){if(_7[_8]){dojo.connect(_6,_8,_7,_8);}},this);});return _6;};dojo.declare("dojo.fx.Toggler",null,{constructor:function(_9){var _t=this;dojo.mixin(_t,_9);_t.node=_9.node;_t._showArgs=dojo.mixin({},_9);_t._showArgs.node=_t.node;_t._showArgs.duration=_t.showDuration;_t.showAnim=_t.showFunc(_t._showArgs);_t._hideArgs=dojo.mixin({},_9);_t._hideArgs.node=_t.node;_t._hideArgs.duration=_t.hideDuration;_t.hideAnim=_t.hideFunc(_t._hideArgs);dojo.connect(_t.showAnim,"beforeBegin",dojo.hitch(_t.hideAnim,"stop",true));dojo.connect(_t.hideAnim,"beforeBegin",dojo.hitch(_t.showAnim,"stop",true));},node:null,showFunc:dojo.fadeIn,hideFunc:dojo.fadeOut,showDuration:200,hideDuration:200,_showArgs:null,_showAnim:null,_hideArgs:null,_hideAnim:null,_isShowing:false,_isHiding:false,show:function(_b){_b=_b||0;return this.showAnim.play(_b);},hide:function(_c){_c=_c||0;return this.hideAnim.play(_c);}});dojo.fx.wipeIn=function(_d){_d.node=dojo.byId(_d.node);var _e=_d.node,s=_e.style;var _10=dojo.animateProperty(dojo.mixin({properties:{height:{start:function(){s.overflow="hidden";if(s.visibility=="hidden"||s.display=="none"){s.height="1px";s.display="";s.visibility="";return 1;}else{var _11=dojo.style(_e,"height");return Math.max(_11,1);}},end:function(){return _e.scrollHeight;}}}},_d));dojo.connect(_10,"onEnd",_10,function(){s.height="auto";});return _10;};dojo.fx.wipeOut=function(_12){var _13=(_12.node=dojo.byId(_12.node));var _14=dojo.animateProperty(dojo.mixin({properties:{height:{end:1}}},_12));dojo.connect(_14,"beforeBegin",_14,function(){var s=_13.style;s.overflow="hidden";s.display="";});dojo.connect(_14,"onEnd",_14,function(){var s=this.node.style;s.height="auto";s.display="none";});return _14;};dojo.fx.slideTo=function(_17){var _18=_17.node=dojo.byId(_17.node);var _19=dojo.getComputedStyle;var top=null;var _1b=null;var _1c=(function(){var _1d=_18;return function(){var pos=_19(_1d).position;top=(pos=="absolute"?_18.offsetTop:parseInt(_19(_18).top)||0);_1b=(pos=="absolute"?_18.offsetLeft:parseInt(_19(_18).left)||0);if(pos!="absolute"&&pos!="relative"){var ret=dojo.coords(_1d,true);top=ret.y;_1b=ret.x;_1d.style.position="absolute";_1d.style.top=top+"px";_1d.style.left=_1b+"px";}};})();_1c();var _20=dojo.animateProperty(dojo.mixin({properties:{top:{start:top,end:_17.top||0},left:{start:_1b,end:_17.left||0}}},_17));dojo.connect(_20,"beforeBegin",_20,_1c);return _20;};}if(!dojo._hasResource["dojo.dnd.common"]){dojo._hasResource["dojo.dnd.common"]=true;dojo.provide("dojo.dnd.common");dojo.dnd._copyKey=navigator.appVersion.indexOf("Macintosh")<0?"ctrlKey":"metaKey";dojo.dnd.getCopyKeyState=function(e){return e[dojo.dnd._copyKey];};dojo.dnd._uniqueId=0;dojo.dnd.getUniqueId=function(){var id;do{id="dojoUnique"+(++dojo.dnd._uniqueId);}while(dojo.byId(id));return id;};dojo.dnd._empty={};dojo.dnd.isFormElement=function(e){var t=e.target;if(t.nodeType==3){t=t.parentNode;}return " button textarea input select option ".indexOf(" "+t.tagName.toLowerCase()+" ")>=0;};}if(!dojo._hasResource["dojo.date.stamp"]){dojo._hasResource["dojo.date.stamp"]=true;dojo.provide("dojo.date.stamp");dojo.date.stamp.fromISOString=function(_25,_26){if(!dojo.date.stamp._isoRegExp){dojo.date.stamp._isoRegExp=/^(?:(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(.\d+)?)?((?:[+-](\d{2}):(\d{2}))|Z)?)?$/;}var _27=dojo.date.stamp._isoRegExp.exec(_25);var _28=null;if(_27){_27.shift();_27[1]&&_27[1]--;_27[6]&&(_27[6]*=1000);if(_26){_26=new Date(_26);dojo.map(["FullYear","Month","Date","Hours","Minutes","Seconds","Milliseconds"],function(_29){return _26["get"+_29]();}).forEach(function(_2a,_2b){if(_27[_2b]===undefined){_27[_2b]=_2a;}});}_28=new Date(_27[0]||1970,_27[1]||0,_27[2]||0,_27[3]||0,_27[4]||0,_27[5]||0,_27[6]||0);var _2c=0;var _2d=_27[7]&&_27[7].charAt(0);if(_2d!="Z"){_2c=((_27[8]||0)*60)+(Number(_27[9])||0);if(_2d!="-"){_2c*=-1;}}if(_2d){_2c-=_28.getTimezoneOffset();}if(_2c){_28.setTime(_28.getTime()+_2c*60000);}}return _28;};dojo.date.stamp.toISOString=function(_2e,_2f){var _=function(n){return (n<10)?"0"+n:n;};_2f=_2f||{};var _32=[];var _33=_2f.zulu?"getUTC":"get";var _34="";if(_2f.selector!="time"){_34=[_2e[_33+"FullYear"](),_(_2e[_33+"Month"]()+1),_(_2e[_33+"Date"]())].join("-");}_32.push(_34);if(_2f.selector!="date"){var _35=[_(_2e[_33+"Hours"]()),_(_2e[_33+"Minutes"]()),_(_2e[_33+"Seconds"]())].join(":");var _36=_2e[_33+"Milliseconds"]();if(_2f.milliseconds){_35+="."+(_36<100?"0":"")+_(_36);}if(_2f.zulu){_35+="Z";}else{var _37=_2e.getTimezoneOffset();var _38=Math.abs(_37);_35+=(_37>0?"-":"+")+_(Math.floor(_38/60))+":"+_(_38%60);}_32.push(_35);}return _32.join("T");};}if(!dojo._hasResource["dojo.parser"]){dojo._hasResource["dojo.parser"]=true;dojo.provide("dojo.parser");dojo.parser=new function(){var d=dojo;function val2type(_3a){if(d.isString(_3a)){return "string";}if(typeof _3a=="number"){return "number";}if(typeof _3a=="boolean"){return "boolean";}if(d.isFunction(_3a)){return "function";}if(d.isArray(_3a)){return "array";}if(_3a instanceof Date){return "date";}if(_3a instanceof d._Url){return "url";}return "object";};function str2obj(_3b,_3c){switch(_3c){case "string":return _3b;case "number":return _3b.length?Number(_3b):NaN;case "boolean":return typeof _3b=="boolean"?_3b:!(_3b.toLowerCase()=="false");case "function":if(d.isFunction(_3b)){_3b=_3b.toString();_3b=d.trim(_3b.substring(_3b.indexOf("{")+1,_3b.length-1));}try{if(_3b.search(/[^\w\.]+/i)!=-1){_3b=d.parser._nameAnonFunc(new Function(_3b),this);}return d.getObject(_3b,false);}catch(e){return new Function();}case "array":return _3b.split(/\s*,\s*/);case "date":switch(_3b){case "":return new Date("");case "now":return new Date();default:return d.date.stamp.fromISOString(_3b);}case "url":return d.baseUrl+_3b;default:return d.fromJson(_3b);}};var _3d={};function getClassInfo(_3e){if(!_3d[_3e]){var cls=d.getObject(_3e);if(!d.isFunction(cls)){throw new Error("Could not load class '"+_3e+"'. Did you spell the name correctly and use a full path, like 'dijit.form.Button'?");}var _40=cls.prototype;var _41={};for(var _42 in _40){if(_42.charAt(0)=="_"){continue;}var _43=_40[_42];_41[_42]=val2type(_43);}_3d[_3e]={cls:cls,params:_41};}return _3d[_3e];};this._functionFromScript=function(_44){var _45="";var _46="";var _47=_44.getAttribute("args");if(_47){d.forEach(_47.split(/\s*,\s*/),function(_48,idx){_45+="var "+_48+" = arguments["+idx+"]; ";});}var _4a=_44.getAttribute("with");if(_4a&&_4a.length){d.forEach(_4a.split(/\s*,\s*/),function(_4b){_45+="with("+_4b+"){";_46+="}";});}return new Function(_45+_44.innerHTML+_46);};this._wireUpMethod=function(_4c,_4d){var nf=this._functionFromScript(_4d);var _4f=_4d.getAttribute("event");if(_4f){var _50=_4d.getAttribute("type");if(_50&&(_50=="dojo/connect")){d.connect(_4c,_4f,null,nf);}else{_4c[_4f]=nf;}}else{nf.call(_4c);}};this.instantiate=function(_51){var _52=[];d.forEach(_51,function(_53){if(!_53){return;}var _54=_53.getAttribute("dojoType");if((!_54)||(!_54.length)){return;}var _55=getClassInfo(_54);var _56=_55.cls;var ps=_56._noScript||_56.prototype._noScript;var _58={};var _59=_53.attributes;for(var _5a in _55.params){var _5b=_59.getNamedItem(_5a);if(!_5b||(!_5b.specified&&(!dojo.isIE||_5a.toLowerCase()!="value"))){continue;}var _5c=_55.params[_5a];_58[_5a]=str2obj(_5b.value,_5c);}if(!ps){var _5d=d.query("> script[type='dojo/method'][event='preamble']",_53).orphan();if(_5d.length){_58.preamble=d.parser._functionFromScript(_5d[0]);}var _5e=d.query("> script[type^='dojo/']",_53).orphan();}var _5f=_56["markupFactory"];if(!_5f&&_56["prototype"]){_5f=_56.prototype["markupFactory"];}var _60=_5f?_5f(_58,_53,_56):new _56(_58,_53);_52.push(_60);var _61=_53.getAttribute("jsId");if(_61){d.setObject(_61,_60);}if(!ps){_5e.forEach(function(_62){d.parser._wireUpMethod(_60,_62);});}});d.forEach(_52,function(_63){if(_63&&(_63.startup)&&((!_63.getParent)||(!_63.getParent()))){_63.startup();}});return _52;};this.parse=function(_64){var _65=d.query("[dojoType]",_64);var _66=this.instantiate(_65);return _66;};}();(function(){var _67=function(){if(djConfig["parseOnLoad"]==true){dojo.parser.parse();}};if(dojo.exists("dijit.wai.onload")&&(dijit.wai.onload===dojo._loaders[0])){dojo._loaders.splice(1,0,_67);}else{dojo._loaders.unshift(_67);}})();dojo.parser._anonCtr=0;dojo.parser._anon={};dojo.parser._nameAnonFunc=function(_68,_69){var jpn="$joinpoint";var nso=(_69||dojo.parser._anon);if(dojo.isIE){var cn=_68["__dojoNameCache"];if(cn&&nso[cn]===_68){return _68["__dojoNameCache"];}}var ret="__"+dojo.parser._anonCtr++;while(typeof nso[ret]!="undefined"){ret="__"+dojo.parser._anonCtr++;}nso[ret]=_68;return ret;};}if(!dojo._hasResource["dojo.dnd.container"]){dojo._hasResource["dojo.dnd.container"]=true;dojo.provide("dojo.dnd.container");dojo.declare("dojo.dnd.Container",null,{constructor:function(_6e,_6f){this.node=dojo.byId(_6e);this.creator=_6f&&_6f.creator||null;this.defaultCreator=dojo.dnd._defaultCreator(this.node);this.map={};this.current=null;this.containerState="";dojo.addClass(this.node,"dojoDndContainer");if(!(_6f&&_6f._skipStartup)){this.startup();}this.events=[dojo.connect(this.node,"onmouseover",this,"onMouseOver"),dojo.connect(this.node,"onmouseout",this,"onMouseOut"),dojo.connect(this.node,"ondragstart",dojo,"stopEvent"),dojo.connect(this.node,"onselectstart",dojo,"stopEvent")];},creator:function(){},getItem:function(key){return this.map[key];},setItem:function(key,_72){this.map[key]=_72;},delItem:function(key){delete this.map[key];},forInItems:function(f,o){o=o||dojo.global;var m=this.map,e=dojo.dnd._empty;for(var i in this.map){if(i in e){continue;}f.call(o,m[i],i,m);}},clearItems:function(){this.map={};},getAllNodes:function(){return dojo.query("> .dojoDndItem",this.parent);},insertNodes:function(_79,_7a,_7b){if(!this.parent.firstChild){_7b=null;}else{if(_7a){if(!_7b){_7b=this.parent.firstChild;}}else{if(_7b){_7b=_7b.nextSibling;}}}if(_7b){for(var i=0;i<_79.length;++i){var t=this._normalizedCreator(_79[i]);this.setItem(t.node.id,{data:t.data,type:t.type});this.parent.insertBefore(t.node,_7b);}}else{for(var i=0;i<_79.length;++i){var t=this._normalizedCreator(_79[i]);this.setItem(t.node.id,{data:t.data,type:t.type});this.parent.appendChild(t.node);}}return this;},destroy:function(){dojo.forEach(this.events,dojo.disconnect);this.clearItems();this.node=this.parent=this.current;},markupFactory:function(_7e,_7f){_7e._skipStartup=true;return new dojo.dnd.Container(_7f,_7e);},startup:function(){this.parent=this.node;if(this.parent.tagName.toLowerCase()=="table"){var c=this.parent.getElementsByTagName("tbody");if(c&&c.length){this.parent=c[0];}}dojo.query("> .dojoDndItem",this.parent).forEach(function(_81){if(!_81.id){_81.id=dojo.dnd.getUniqueId();}var _82=_81.getAttribute("dndType"),_83=_81.getAttribute("dndData");this.setItem(_81.id,{data:_83?_83:_81.innerHTML,type:_82?_82.split(/\s*,\s*/):["text"]});},this);},onMouseOver:function(e){var n=e.relatedTarget;while(n){if(n==this.node){break;}try{n=n.parentNode;}catch(x){n=null;}}if(!n){this._changeState("Container","Over");this.onOverEvent();}n=this._getChildByEvent(e);if(this.current==n){return;}if(this.current){this._removeItemClass(this.current,"Over");}if(n){this._addItemClass(n,"Over");}this.current=n;},onMouseOut:function(e){for(var n=e.relatedTarget;n;){if(n==this.node){return;}try{n=n.parentNode;}catch(x){n=null;}}if(this.current){this._removeItemClass(this.current,"Over");this.current=null;}this._changeState("Container","");this.onOutEvent();},onOverEvent:function(){},onOutEvent:function(){},_changeState:function(_88,_89){var _8a="dojoDnd"+_88;var _8b=_88.toLowerCase()+"State";dojo.removeClass(this.node,_8a+this[_8b]);dojo.addClass(this.node,_8a+_89);this[_8b]=_89;},_addItemClass:function(_8c,_8d){dojo.addClass(_8c,"dojoDndItem"+_8d);},_removeItemClass:function(_8e,_8f){dojo.removeClass(_8e,"dojoDndItem"+_8f);},_getChildByEvent:function(e){var _91=e.target;if(_91){for(var _92=_91.parentNode;_92;_91=_92,_92=_91.parentNode){if(_92==this.parent&&dojo.hasClass(_91,"dojoDndItem")){return _91;}}}return null;},_normalizedCreator:function(_93,_94){var t=(this.creator?this.creator:this.defaultCreator)(_93,_94);if(!dojo.isArray(t.type)){t.type=["text"];}if(!t.node.id){t.node.id=dojo.dnd.getUniqueId();}dojo.addClass(t.node,"dojoDndItem");return t;}});dojo.dnd._createNode=function(tag){if(!tag){return dojo.dnd._createSpan;}return function(_97){var n=dojo.doc.createElement(tag);n.innerHTML=_97;return n;};};dojo.dnd._createTrTd=function(_99){var tr=dojo.doc.createElement("tr");var td=dojo.doc.createElement("td");td.innerHTML=_99;tr.appendChild(td);return tr;};dojo.dnd._createSpan=function(_9c){var n=dojo.doc.createElement("span");n.innerHTML=_9c;return n;};dojo.dnd._defaultCreatorNodes={ul:"li",ol:"li",div:"div",p:"div"};dojo.dnd._defaultCreator=function(_9e){var tag=_9e.tagName.toLowerCase();var c=tag=="table"?dojo.dnd._createTrTd:dojo.dnd._createNode(dojo.dnd._defaultCreatorNodes[tag]);return function(_a1,_a2){var _a3=dojo.isObject(_a1)&&_a1;var _a4=(_a3&&_a1.data)?_a1.data:_a1;var _a5=(_a3&&_a1.type)?_a1.type:["text"];var t=String(_a4),n=(_a2=="avatar"?dojo.dnd._createSpan:c)(t);n.id=dojo.dnd.getUniqueId();return {node:n,data:_a4,type:_a5};};};}if(!dojo._hasResource["dojo.dnd.selector"]){dojo._hasResource["dojo.dnd.selector"]=true;dojo.provide("dojo.dnd.selector");dojo.declare("dojo.dnd.Selector",dojo.dnd.Container,{constructor:function(_a8,_a9){this.singular=_a9&&_a9.singular;this.selection={};this.anchor=null;this.simpleSelection=false;this.events.push(dojo.connect(this.node,"onmousedown",this,"onMouseDown"),dojo.connect(this.node,"onmouseup",this,"onMouseUp"));},singular:false,getSelectedNodes:function(){var t=new dojo.NodeList();var e=dojo.dnd._empty;for(var i in this.selection){if(i in e){continue;}t.push(dojo.byId(i));}return t;},selectNone:function(){return this._removeSelection()._removeAnchor();},selectAll:function(){this.forInItems(function(_ad,id){this._addItemClass(dojo.byId(id),"Selected");this.selection[id]=1;},this);return this._removeAnchor();},deleteSelectedNodes:function(){var e=dojo.dnd._empty;for(var i in this.selection){if(i in e){continue;}var n=dojo.byId(i);this.delItem(i);dojo._destroyElement(n);}this.anchor=null;this.selection={};return this;},insertNodes:function(_b2,_b3,_b4,_b5){var _b6=this._normalizedCreator;this._normalizedCreator=function(_b7,_b8){var t=_b6.call(this,_b7,_b8);if(_b2){if(!this.anchor){this.anchor=t.node;this._removeItemClass(t.node,"Selected");this._addItemClass(this.anchor,"Anchor");}else{if(this.anchor!=t.node){this._removeItemClass(t.node,"Anchor");this._addItemClass(t.node,"Selected");}}this.selection[t.node.id]=1;}else{this._removeItemClass(t.node,"Selected");this._removeItemClass(t.node,"Anchor");}return t;};dojo.dnd.Selector.superclass.insertNodes.call(this,_b3,_b4,_b5);this._normalizedCreator=_b6;return this;},destroy:function(){dojo.dnd.Selector.superclass.destroy.call(this);this.selection=this.anchor=null;},markupFactory:function(_ba,_bb){_ba._skipStartup=true;return new dojo.dnd.Selector(_bb,_ba);},onMouseDown:function(e){if(!this.current){return;}if(!this.singular&&!dojo.dnd.getCopyKeyState(e)&&!e.shiftKey&&(this.current.id in this.selection)){this.simpleSelection=true;dojo.stopEvent(e);return;}if(!this.singular&&e.shiftKey){if(!dojo.dnd.getCopyKeyState(e)){this._removeSelection();}var c=dojo.query("> .dojoDndItem",this.parent);if(c.length){if(!this.anchor){this.anchor=c[0];this._addItemClass(this.anchor,"Anchor");}this.selection[this.anchor.id]=1;if(this.anchor!=this.current){var i=0;for(;i<c.length;++i){var _bf=c[i];if(_bf==this.anchor||_bf==this.current){break;}}for(++i;i<c.length;++i){var _bf=c[i];if(_bf==this.anchor||_bf==this.current){break;}this._addItemClass(_bf,"Selected");this.selection[_bf.id]=1;}this._addItemClass(this.current,"Selected");this.selection[this.current.id]=1;}}}else{if(this.singular){if(this.anchor==this.current){if(dojo.dnd.getCopyKeyState(e)){this.selectNone();}}else{this.selectNone();this.anchor=this.current;this._addItemClass(this.anchor,"Anchor");this.selection[this.current.id]=1;}}else{if(dojo.dnd.getCopyKeyState(e)){if(this.anchor==this.current){delete this.selection[this.anchor.id];this._removeAnchor();}else{if(this.current.id in this.selection){this._removeItemClass(this.current,"Selected");delete this.selection[this.current.id];}else{if(this.anchor){this._removeItemClass(this.anchor,"Anchor");this._addItemClass(this.anchor,"Selected");}this.anchor=this.current;this._addItemClass(this.current,"Anchor");this.selection[this.current.id]=1;}}}else{if(!(this.current.id in this.selection)){this.selectNone();this.anchor=this.current;this._addItemClass(this.current,"Anchor");this.selection[this.current.id]=1;}}}}dojo.stopEvent(e);},onMouseUp:function(e){if(!this.simpleSelection){return;}this.simpleSelection=false;this.selectNone();if(this.current){this.anchor=this.current;this._addItemClass(this.anchor,"Anchor");this.selection[this.current.id]=1;}},onMouseMove:function(e){this.simpleSelection=false;},onOverEvent:function(){this.onmousemoveEvent=dojo.connect(this.node,"onmousemove",this,"onMouseMove");},onOutEvent:function(){dojo.disconnect(this.onmousemoveEvent);delete this.onmousemoveEvent;},_removeSelection:function(){var e=dojo.dnd._empty;for(var i in this.selection){if(i in e){continue;}var _c4=dojo.byId(i);if(_c4){this._removeItemClass(_c4,"Selected");}}this.selection={};return this;},_removeAnchor:function(){if(this.anchor){this._removeItemClass(this.anchor,"Anchor");this.anchor=null;}return this;}});}if(!dojo._hasResource["dojo.dnd.autoscroll"]){dojo._hasResource["dojo.dnd.autoscroll"]=true;dojo.provide("dojo.dnd.autoscroll");dojo.dnd.getViewport=function(){var d=dojo.doc,dd=d.documentElement,w=window,b=dojo.body();if(dojo.isMozilla){return {w:dd.clientWidth,h:w.innerHeight};}else{if(!dojo.isOpera&&w.innerWidth){return {w:w.innerWidth,h:w.innerHeight};}else{if(!dojo.isOpera&&dd&&dd.clientWidth){return {w:dd.clientWidth,h:dd.clientHeight};}else{if(b.clientWidth){return {w:b.clientWidth,h:b.clientHeight};}}}}return null;};dojo.dnd.V_TRIGGER_AUTOSCROLL=32;dojo.dnd.H_TRIGGER_AUTOSCROLL=32;dojo.dnd.V_AUTOSCROLL_VALUE=16;dojo.dnd.H_AUTOSCROLL_VALUE=16;dojo.dnd.autoScroll=function(e){var v=dojo.dnd.getViewport(),dx=0,dy=0;if(e.clientX<dojo.dnd.H_TRIGGER_AUTOSCROLL){dx=-dojo.dnd.H_AUTOSCROLL_VALUE;}else{if(e.clientX>v.w-dojo.dnd.H_TRIGGER_AUTOSCROLL){dx=dojo.dnd.H_AUTOSCROLL_VALUE;}}if(e.clientY<dojo.dnd.V_TRIGGER_AUTOSCROLL){dy=-dojo.dnd.V_AUTOSCROLL_VALUE;}else{if(e.clientY>v.h-dojo.dnd.V_TRIGGER_AUTOSCROLL){dy=dojo.dnd.V_AUTOSCROLL_VALUE;}}window.scrollBy(dx,dy);};dojo.dnd._validNodes={"div":1,"p":1,"td":1};dojo.dnd._validOverflow={"auto":1,"scroll":1};dojo.dnd.autoScrollNodes=function(e){for(var n=e.target;n;){if(n.nodeType==1&&(n.tagName.toLowerCase() in dojo.dnd._validNodes)){var s=dojo.getComputedStyle(n);if(s.overflow.toLowerCase() in dojo.dnd._validOverflow){var b=dojo._getContentBox(n,s),t=dojo._abs(n,true);console.debug(b.l,b.t,t.x,t.y,n.scrollLeft,n.scrollTop);b.l+=t.x+n.scrollLeft;b.t+=t.y+n.scrollTop;var w=Math.min(dojo.dnd.H_TRIGGER_AUTOSCROLL,b.w/2),h=Math.min(dojo.dnd.V_TRIGGER_AUTOSCROLL,b.h/2),rx=e.pageX-b.l,ry=e.pageY-b.t,dx=0,dy=0;if(rx>0&&rx<b.w){if(rx<w){dx=-dojo.dnd.H_AUTOSCROLL_VALUE;}else{if(rx>b.w-w){dx=dojo.dnd.H_AUTOSCROLL_VALUE;}}}if(ry>0&&ry<b.h){if(ry<h){dy=-dojo.dnd.V_AUTOSCROLL_VALUE;}else{if(ry>b.h-h){dy=dojo.dnd.V_AUTOSCROLL_VALUE;}}}var _d8=n.scrollLeft,_d9=n.scrollTop;n.scrollLeft=n.scrollLeft+dx;n.scrollTop=n.scrollTop+dy;if(dx||dy){console.debug(_d8+", "+_d9+"\n"+dx+", "+dy+"\n"+n.scrollLeft+", "+n.scrollTop);}if(_d8!=n.scrollLeft||_d9!=n.scrollTop){return;}}}try{n=n.parentNode;}catch(x){n=null;}}dojo.dnd.autoScroll(e);};}if(!dojo._hasResource["dojo.dnd.avatar"]){dojo._hasResource["dojo.dnd.avatar"]=true;dojo.provide("dojo.dnd.avatar");dojo.dnd.Avatar=function(_da){this.manager=_da;this.construct();};dojo.extend(dojo.dnd.Avatar,{construct:function(){var a=dojo.doc.createElement("table");a.className="dojoDndAvatar";a.style.position="absolute";a.style.zIndex=1999;a.style.margin="0px";var b=dojo.doc.createElement("tbody");var tr=dojo.doc.createElement("tr");tr.className="dojoDndAvatarHeader";var td=dojo.doc.createElement("td");td.innerHTML=this._generateText();tr.appendChild(td);dojo.style(tr,"opacity",0.9);b.appendChild(tr);var k=Math.min(5,this.manager.nodes.length);var _e0=this.manager.source;for(var i=0;i<k;++i){tr=dojo.doc.createElement("tr");tr.className="dojoDndAvatarItem";td=dojo.doc.createElement("td");var _e2=_e0.creator?_e2=_e0._normalizedCreator(_e0.getItem(this.manager.nodes[i].id).data,"avatar").node:_e2=this.manager.nodes[i].cloneNode(true);_e2.id="";td.appendChild(_e2);tr.appendChild(td);dojo.style(tr,"opacity",(9-i)/10);b.appendChild(tr);}a.appendChild(b);this.node=a;},destroy:function(){dojo._destroyElement(this.node);this.node=false;},update:function(){dojo[(this.manager.canDropFlag?"add":"remove")+"Class"](this.node,"dojoDndAvatarCanDrop");var t=this.node.getElementsByTagName("td");for(var i=0;i<t.length;++i){var n=t[i];if(dojo.hasClass(n.parentNode,"dojoDndAvatarHeader")){n.innerHTML=this._generateText();break;}}},_generateText:function(){return this.manager.nodes.length.toString();}});}if(!dojo._hasResource["dojo.dnd.manager"]){dojo._hasResource["dojo.dnd.manager"]=true;dojo.provide("dojo.dnd.manager");dojo.dnd.Manager=function(){this.avatar=null;this.source=null;this.nodes=[];this.copy=true;this.target=null;this.canDropFlag=false;this.events=[];};dojo.extend(dojo.dnd.Manager,{OFFSET_X:16,OFFSET_Y:16,overSource:function(_e6){if(this.avatar){this.target=(_e6&&_e6.targetState!="Disabled")?_e6:null;this.avatar.update();}dojo.publish("/dnd/source/over",[_e6]);},outSource:function(_e7){if(this.avatar){if(this.target==_e7){this.target=null;this.canDropFlag=false;this.avatar.update();dojo.publish("/dnd/source/over",[null]);}}else{dojo.publish("/dnd/source/over",[null]);}},startDrag:function(_e8,_e9,_ea){this.source=_e8;this.nodes=_e9;this.copy=Boolean(_ea);this.avatar=this.makeAvatar();dojo.body().appendChild(this.avatar.node);dojo.publish("/dnd/start",[_e8,_e9,this.copy]);this.events=[dojo.connect(dojo.doc,"onmousemove",this,"onMouseMove"),dojo.connect(dojo.doc,"onmouseup",this,"onMouseUp"),dojo.connect(dojo.doc,"onkeydown",this,"onKeyDown"),dojo.connect(dojo.doc,"onkeyup",this,"onKeyUp")];var c="dojoDnd"+(_ea?"Copy":"Move");dojo.addClass(dojo.body(),c);},canDrop:function(_ec){var _ed=this.target&&_ec;if(this.canDropFlag!=_ed){this.canDropFlag=_ed;this.avatar.update();}},stopDrag:function(){dojo.removeClass(dojo.body(),"dojoDndCopy");dojo.removeClass(dojo.body(),"dojoDndMove");dojo.forEach(this.events,dojo.disconnect);this.events=[];this.avatar.destroy();this.avatar=null;this.source=null;this.nodes=[];},makeAvatar:function(){return new dojo.dnd.Avatar(this);},updateAvatar:function(){this.avatar.update();},onMouseMove:function(e){var a=this.avatar;if(a){dojo.dnd.autoScroll(e);dojo.marginBox(a.node,{l:e.pageX+this.OFFSET_X,t:e.pageY+this.OFFSET_Y});var _f0=Boolean(this.source.copyState(dojo.dnd.getCopyKeyState(e)));if(this.copy!=_f0){this._setCopyStatus(_f0);}}},onMouseUp:function(e){if(this.avatar){if(this.target&&this.canDropFlag){dojo.publish("/dnd/drop",[this.source,this.nodes,Boolean(this.source.copyState(dojo.dnd.getCopyKeyState(e)))]);}else{dojo.publish("/dnd/cancel");}this.stopDrag();}},onKeyDown:function(e){if(this.avatar){switch(e.keyCode){case dojo.keys.CTRL:var _f3=Boolean(this.source.copyState(true));if(this.copy!=_f3){this._setCopyStatus(_f3);}break;case dojo.keys.ESCAPE:dojo.publish("/dnd/cancel");this.stopDrag();break;}}},onKeyUp:function(e){if(this.avatar&&e.keyCode==dojo.keys.CTRL){var _f5=Boolean(this.source.copyState(false));if(this.copy!=_f5){this._setCopyStatus(_f5);}}},_setCopyStatus:function(_f6){this.copy=_f6;this.source._markDndStatus(this.copy);this.updateAvatar();dojo.removeClass(dojo.body(),"dojoDnd"+(this.copy?"Move":"Copy"));dojo.addClass(dojo.body(),"dojoDnd"+(this.copy?"Copy":"Move"));}});dojo.dnd._manager=null;dojo.dnd.manager=function(){if(!dojo.dnd._manager){dojo.dnd._manager=new dojo.dnd.Manager();}return dojo.dnd._manager;};}if(!dojo._hasResource["dojo.dnd.source"]){dojo._hasResource["dojo.dnd.source"]=true;dojo.provide("dojo.dnd.source");dojo.declare("dojo.dnd.Source",dojo.dnd.Selector,{isSource:true,horizontal:false,copyOnly:false,skipForm:false,accept:["text"],constructor:function(_f7,_f8){if(!_f8){_f8={};}this.isSource=typeof _f8.isSource=="undefined"?true:_f8.isSource;var _f9=_f8.accept instanceof Array?_f8.accept:["text"];this.accept=null;if(_f9.length){this.accept={};for(var i=0;i<_f9.length;++i){this.accept[_f9[i]]=1;}}this.horizontal=_f8.horizontal;this.copyOnly=_f8.copyOnly;this.skipForm=_f8.skipForm;this.isDragging=false;this.mouseDown=false;this.targetAnchor=null;this.targetBox=null;this.before=true;this.sourceState="";if(this.isSource){dojo.addClass(this.node,"dojoDndSource");}this.targetState="";if(this.accept){dojo.addClass(this.node,"dojoDndTarget");}if(this.horizontal){dojo.addClass(this.node,"dojoDndHorizontal");}this.topics=[dojo.subscribe("/dnd/source/over",this,"onDndSourceOver"),dojo.subscribe("/dnd/start",this,"onDndStart"),dojo.subscribe("/dnd/drop",this,"onDndDrop"),dojo.subscribe("/dnd/cancel",this,"onDndCancel")];},checkAcceptance:function(_fb,_fc){if(this==_fb){return true;}for(var i=0;i<_fc.length;++i){var _fe=_fb.getItem(_fc[i].id).type;var _ff=false;for(var j=0;j<_fe.length;++j){if(_fe[j] in this.accept){_ff=true;break;}}if(!_ff){return false;}}return true;},copyState:function(_101){return this.copyOnly||_101;},destroy:function(){dojo.dnd.Source.superclass.destroy.call(this);dojo.forEach(this.topics,dojo.unsubscribe);this.targetAnchor=null;},markupFactory:function(_102,node){_102._skipStartup=true;return new dojo.dnd.Source(node,_102);},onMouseMove:function(e){if(this.isDragging&&this.targetState=="Disabled"){return;}dojo.dnd.Source.superclass.onMouseMove.call(this,e);var m=dojo.dnd.manager();if(this.isDragging){var _106=false;if(this.current){if(!this.targetBox||this.targetAnchor!=this.current){this.targetBox={xy:dojo.coords(this.current,true),w:this.current.offsetWidth,h:this.current.offsetHeight};}if(this.horizontal){_106=(e.pageX-this.targetBox.xy.x)<(this.targetBox.w/2);}else{_106=(e.pageY-this.targetBox.xy.y)<(this.targetBox.h/2);}}if(this.current!=this.targetAnchor||_106!=this.before){this._markTargetAnchor(_106);m.canDrop(!this.current||m.source!=this||!(this.current.id in this.selection));}}else{if(this.mouseDown&&this.isSource){var _107=this.getSelectedNodes();if(_107.length){m.startDrag(this,_107,this.copyState(dojo.dnd.getCopyKeyState(e)));}}}},onMouseDown:function(e){if(!this.skipForm||!dojo.dnd.isFormElement(e)){this.mouseDown=true;dojo.dnd.Source.superclass.onMouseDown.call(this,e);}},onMouseUp:function(e){if(this.mouseDown){this.mouseDown=false;dojo.dnd.Source.superclass.onMouseUp.call(this,e);}},onDndSourceOver:function(_10a){if(this!=_10a){this.mouseDown=false;if(this.targetAnchor){this._unmarkTargetAnchor();}}else{if(this.isDragging){var m=dojo.dnd.manager();m.canDrop(this.targetState!="Disabled"&&(!this.current||m.source!=this||!(this.current.id in this.selection)));}}},onDndStart:function(_10c,_10d,copy){if(this.isSource){this._changeState("Source",this==_10c?(copy?"Copied":"Moved"):"");}var _10f=this.accept&&this.checkAcceptance(_10c,_10d);this._changeState("Target",_10f?"":"Disabled");if(_10f){dojo.dnd.manager().overSource(this);}this.isDragging=true;},onDndDrop:function(_110,_111,copy){do{if(this.containerState!="Over"){break;}var _113=this._normalizedCreator;if(this!=_110){if(this.creator){this._normalizedCreator=function(node,hint){return _113.call(this,_110.getItem(node.id).data,hint);};}else{if(copy){this._normalizedCreator=function(node,hint){var t=_110.getItem(node.id);var n=node.cloneNode(true);n.id=dojo.dnd.getUniqueId();return {node:n,data:t.data,type:t.type};};}else{this._normalizedCreator=function(node,hint){var t=_110.getItem(node.id);_110.delItem(node.id);return {node:node,data:t.data,type:t.type};};}}}else{if(this.current&&this.current.id in this.selection){break;}if(this.creator){if(copy){this._normalizedCreator=function(node,hint){return _113.call(this,_110.getItem(node.id).data,hint);};}else{this._normalizedCreator=function(node,hint){var t=_110.getItem(node.id);return {node:node,data:t.data,type:t.type};};}}else{if(copy){this._normalizedCreator=function(node,hint){var t=_110.getItem(node.id);var n=node.cloneNode(true);n.id=dojo.dnd.getUniqueId();return {node:n,data:t.data,type:t.type};};}else{this._normalizedCreator=function(node,hint){var t=_110.getItem(node.id);return {node:node,data:t.data,type:t.type};};}}}this._removeSelection();if(this!=_110){this._removeAnchor();}if(this!=_110&&!copy&&!this.creator){_110.selectNone();}this.insertNodes(true,_111,this.before,this.current);if(this!=_110&&!copy&&this.creator){_110.deleteSelectedNodes();}this._normalizedCreator=_113;}while(false);this.onDndCancel();},onDndCancel:function(){if(this.targetAnchor){this._unmarkTargetAnchor();this.targetAnchor=null;}this.before=true;this.isDragging=false;this.mouseDown=false;this._changeState("Source","");this._changeState("Target","");},onOverEvent:function(){dojo.dnd.Source.superclass.onOverEvent.call(this);dojo.dnd.manager().overSource(this);},onOutEvent:function(){dojo.dnd.Source.superclass.onOutEvent.call(this);dojo.dnd.manager().outSource(this);},_markTargetAnchor:function(_129){if(this.current==this.targetAnchor&&this.before==_129){return;}if(this.targetAnchor){this._removeItemClass(this.targetAnchor,this.before?"Before":"After");}this.targetAnchor=this.current;this.targetBox=null;this.before=_129;if(this.targetAnchor){this._addItemClass(this.targetAnchor,this.before?"Before":"After");}},_unmarkTargetAnchor:function(){if(!this.targetAnchor){return;}this._removeItemClass(this.targetAnchor,this.before?"Before":"After");this.targetAnchor=null;this.targetBox=null;this.before=true;},_markDndStatus:function(copy){this._changeState("Source",copy?"Copied":"Moved");}});dojo.declare("dojo.dnd.Target",dojo.dnd.Source,{constructor:function(node,_12c){this.isSource=false;dojo.removeClass(this.node,"dojoDndSource");},markupFactory:function(_12d,node){_12d._skipStartup=true;return new dojo.dnd.Target(node,_12d);}});}if(!dojo._hasResource["dojo.dnd.move"]){dojo._hasResource["dojo.dnd.move"]=true;dojo.provide("dojo.dnd.move");dojo.dnd.Mover=function(node,e){this.node=dojo.byId(node);this.marginBox={l:e.pageX,t:e.pageY};var d=node.ownerDocument,_132=dojo.connect(d,"onmousemove",this,"onFirstMove");this.events=[dojo.connect(d,"onmousemove",this,"onMouseMove"),dojo.connect(d,"onmouseup",this,"destroy"),dojo.connect(d,"ondragstart",dojo,"stopEvent"),dojo.connect(d,"onselectstart",dojo,"stopEvent"),_132];dojo.publish("/dnd/move/start",[this.node]);dojo.addClass(dojo.body(),"dojoMove");dojo.addClass(this.node,"dojoMoveItem");};dojo.extend(dojo.dnd.Mover,{onMouseMove:function(e){dojo.dnd.autoScroll(e);var m=this.marginBox;dojo.marginBox(this.node,{l:m.l+e.pageX,t:m.t+e.pageY});},onFirstMove:function(){this.node.style.position="absolute";var m=dojo.marginBox(this.node);m.l-=this.marginBox.l;m.t-=this.marginBox.t;this.marginBox=m;dojo.disconnect(this.events.pop());},destroy:function(){dojo.forEach(this.events,dojo.disconnect);dojo.publish("/dnd/move/stop",[this.node]);dojo.removeClass(dojo.body(),"dojoMove");dojo.removeClass(this.node,"dojoMoveItem");this.events=this.node=null;}});dojo.dnd.Moveable=function(node,_137){this.node=dojo.byId(node);this.handle=(_137&&_137.handle)?dojo.byId(_137.handle):null;if(!this.handle){this.handle=this.node;}this.delay=(_137&&_137.delay>0)?_137.delay:0;this.skip=_137&&_137.skip;this.mover=(_137&&_137.mover)?_137.mover:dojo.dnd.Mover;this.events=[dojo.connect(this.handle,"onmousedown",this,"onMouseDown"),dojo.connect(this.handle,"ondragstart",dojo,"stopEvent"),dojo.connect(this.handle,"onselectstart",dojo,"stopEvent")];};dojo.extend(dojo.dnd.Moveable,{handle:"",delay:0,skip:false,markupFactory:function(_138,node){return new dojo.dnd.Moveable(node,_138);},destroy:function(){dojo.forEach(this.events,dojo.disconnect);this.events=this.node=this.handle=null;},onMouseDown:function(e){if(this.skip&&dojo.dnd.isFormElement(e)){return;}if(this.delay){this.events.push(dojo.connect(this.handle,"onmousemove",this,"onMouseMove"));this.events.push(dojo.connect(this.handle,"onmouseup",this,"onMouseUp"));this._lastX=e.pageX;this._lastY=e.pageY;}else{new this.mover(this.node,e);}dojo.stopEvent(e);},onMouseMove:function(e){if(Math.abs(e.pageX-this._lastX)>this.delay||Math.abs(e.pageY-this._lastY)>this.delay){this.onMouseUp(e);new this.mover(this.node,e);}dojo.stopEvent(e);},onMouseUp:function(e){dojo.disconnect(this.events.pop());dojo.disconnect(this.events.pop());}});dojo.dnd.constrainedMover=function(fun,_13e){var _13f=function(node,e){dojo.dnd.Mover.call(this,node,e);};dojo.extend(_13f,dojo.dnd.Mover.prototype);dojo.extend(_13f,{onMouseMove:function(e){var m=this.marginBox,c=this.constraintBox,l=m.l+e.pageX,t=m.t+e.pageY;l=l<c.l?c.l:c.r<l?c.r:l;t=t<c.t?c.t:c.b<t?c.b:t;dojo.marginBox(this.node,{l:l,t:t});},onFirstMove:function(){dojo.dnd.Mover.prototype.onFirstMove.call(this);var c=this.constraintBox=fun.call(this),m=this.marginBox;c.r=c.l+c.w-(_13e?m.w:0);c.b=c.t+c.h-(_13e?m.h:0);}});return _13f;};dojo.dnd.boxConstrainedMover=function(box,_14a){return dojo.dnd.constrainedMover(function(){return box;},_14a);};dojo.dnd.parentConstrainedMover=function(area,_14c){var fun=function(){var n=this.node.parentNode,s=dojo.getComputedStyle(n),mb=dojo._getMarginBox(n,s);if(area=="margin"){return mb;}var t=dojo._getMarginExtents(n,s);mb.l+=t.l,mb.t+=t.t,mb.w-=t.w,mb.h-=t.h;if(area=="border"){return mb;}t=dojo._getBorderExtents(n,s);mb.l+=t.l,mb.t+=t.t,mb.w-=t.w,mb.h-=t.h;if(area=="padding"){return mb;}t=dojo._getPadExtents(n,s);mb.l+=t.l,mb.t+=t.t,mb.w-=t.w,mb.h-=t.h;return mb;};return dojo.dnd.constrainedMover(fun,_14c);};}if(!dojo._hasResource["dojo.i18n"]){dojo._hasResource["dojo.i18n"]=true;dojo.provide("dojo.i18n");dojo.i18n.getLocalization=function(_152,_153,_154){_154=dojo.i18n.normalizeLocale(_154);var _155=_154.split("-");var _156=[_152,"nls",_153].join(".");var _157=dojo._loadedModules[_156];if(_157){var _158;for(var i=_155.length;i>0;i--){var loc=_155.slice(0,i).join("_");if(_157[loc]){_158=_157[loc];break;}}if(!_158){_158=_157.ROOT;}if(_158){var _15b=function(){};_15b.prototype=_158;return new _15b();}}throw new Error("Bundle not found: "+_153+" in "+_152+" , locale="+_154);};dojo.i18n.normalizeLocale=function(_15c){var _15d=_15c?_15c.toLowerCase():dojo.locale;if(_15d=="root"){_15d="ROOT";}return _15d;};dojo.i18n._requireLocalization=function(_15e,_15f,_160,_161){var _162=dojo.i18n.normalizeLocale(_160);var _163=[_15e,"nls",_15f].join(".");var _164="";if(_161){var _165=_161.split(",");for(var i=0;i<_165.length;i++){if(_162.indexOf(_165[i])==0){if(_165[i].length>_164.length){_164=_165[i];}}}if(!_164){_164="ROOT";}}var _167=_161?_164:_162;var _168=dojo._loadedModules[_163];var _169=null;if(_168){if(djConfig.localizationComplete&&_168._built){return;}var _16a=_167.replace(/-/g,"_");var _16b=_163+"."+_16a;_169=dojo._loadedModules[_16b];}if(!_169){_168=dojo["provide"](_163);var syms=dojo._getModuleSymbols(_15e);var _16d=syms.concat("nls").join("/");var _16e;dojo.i18n._searchLocalePath(_167,_161,function(loc){var _170=loc.replace(/-/g,"_");var _171=_163+"."+_170;var _172=false;if(!dojo._loadedModules[_171]){dojo["provide"](_171);var _173=[_16d];if(loc!="ROOT"){_173.push(loc);}_173.push(_15f);var _174=_173.join("/")+".js";_172=dojo._loadPath(_174,null,function(hash){var _176=function(){};_176.prototype=_16e;_168[_170]=new _176();for(var j in hash){_168[_170][j]=hash[j];}});}else{_172=true;}if(_172&&_168[_170]){_16e=_168[_170];}else{_168[_170]=_16e;}if(_161){return true;}});}if(_161&&_162!=_164){_168[_162.replace(/-/g,"_")]=_168[_164.replace(/-/g,"_")];}};(function(){var _178=djConfig.extraLocale;if(_178){if(!_178 instanceof Array){_178=[_178];}var req=dojo.i18n._requireLocalization;dojo.i18n._requireLocalization=function(m,b,_17c,_17d){req(m,b,_17c,_17d);if(_17c){return;}for(var i=0;i<_178.length;i++){req(m,b,_178[i],_17d);}};}})();dojo.i18n._searchLocalePath=function(_17f,down,_181){_17f=dojo.i18n.normalizeLocale(_17f);var _182=_17f.split("-");var _183=[];for(var i=_182.length;i>0;i--){_183.push(_182.slice(0,i).join("-"));}_183.push(false);if(down){_183.reverse();}for(var j=_183.length-1;j>=0;j--){var loc=_183[j]||"ROOT";var stop=_181(loc);if(stop){break;}}};dojo.i18n._preloadLocalizations=function(_188,_189){function preload(_18a){_18a=dojo.i18n.normalizeLocale(_18a);dojo.i18n._searchLocalePath(_18a,true,function(loc){for(var i=0;i<_189.length;i++){if(_189[i]==loc){dojo["require"](_188+"_"+loc);return true;}}return false;});};preload();var _18d=djConfig.extraLocale||[];for(var i=0;i<_18d.length;i++){preload(_18d[i]);}};}if(!dojo._hasResource["dojo.string"]){dojo._hasResource["dojo.string"]=true;dojo.provide("dojo.string");dojo.string.pad=function(text,size,ch,end){var out=String(text);if(!ch){ch="0";}while(out.length<size){if(end){out+=ch;}else{out=ch+out;}}return out;};dojo.string.substitute=function(_194,map,_196,_197){return _194.replace(/\$\{([^\s\:\}]+)(?:\:([^\s\:\}]+))?\}/g,function(_198,key,_19a){var _19b=dojo.getObject(key,false,map);if(_19a){_19b=dojo.getObject(_19a,false,_197)(_19b);}if(_196){_19b=_196(_19b,key);}return _19b.toString();});};dojo.string.trim=function(str){str=str.replace(/^\s+/,"");for(var i=str.length-1;i>0;i--){if(/\S/.test(str.charAt(i))){str=str.substring(0,i+1);break;}}return str;};}if(!dojo._hasResource["dojo.regexp"]){dojo._hasResource["dojo.regexp"]=true;dojo.provide("dojo.regexp");dojo.regexp.escapeString=function(str,_19f){return str.replace(/([\.$?*!=:|{}\(\)\[\]\\\/^])/g,function(ch){if(_19f&&_19f.indexOf(ch)!=-1){return ch;}return "\\"+ch;});};dojo.regexp.buildGroupRE=function(a,re,_1a3){if(!(a instanceof Array)){return re(a);}var b=[];for(var i=0;i<a.length;i++){b.push(re(a[i]));}return dojo.regexp.group(b.join("|"),_1a3);};dojo.regexp.group=function(_1a6,_1a7){return "("+(_1a7?"?:":"")+_1a6+")";};}if(!dojo._hasResource["dojo.number"]){dojo._hasResource["dojo.number"]=true;dojo.provide("dojo.number");dojo.number.format=function(_1a8,_1a9){_1a9=dojo.mixin({},_1a9||{});var _1aa=dojo.i18n.normalizeLocale(_1a9.locale);var _1ab=dojo.i18n.getLocalization("dojo.cldr","number",_1aa);_1a9.customs=_1ab;var _1ac=_1a9.pattern||_1ab[(_1a9.type||"decimal")+"Format"];if(isNaN(_1a8)){return null;}return dojo.number._applyPattern(_1a8,_1ac,_1a9);};dojo.number._numberPatternRE=/[#0,]*[#0](?:\.0*#*)?/;dojo.number._applyPattern=function(_1ad,_1ae,_1af){_1af=_1af||{};var _1b0=_1af.customs.group;var _1b1=_1af.customs.decimal;var _1b2=_1ae.split(";");var _1b3=_1b2[0];_1ae=_1b2[(_1ad<0)?1:0]||("-"+_1b3);if(_1ae.indexOf("%")!=-1){_1ad*=100;}else{if(_1ae.indexOf("\u2030")!=-1){_1ad*=1000;}else{if(_1ae.indexOf("\xa4")!=-1){_1b0=_1af.customs.currencyGroup||_1b0;_1b1=_1af.customs.currencyDecimal||_1b1;_1ae=_1ae.replace(/\u00a4{1,3}/,function(_1b4){var prop=["symbol","currency","displayName"][_1b4.length-1];return _1af[prop]||_1af.currency||"";});}else{if(_1ae.indexOf("E")!=-1){throw new Error("exponential notation not supported");}}}}var _1b6=dojo.number._numberPatternRE;var _1b7=_1b3.match(_1b6);if(!_1b7){throw new Error("unable to find a number expression in pattern: "+_1ae);}return _1ae.replace(_1b6,dojo.number._formatAbsolute(_1ad,_1b7[0],{decimal:_1b1,group:_1b0,places:_1af.places}));};dojo.number.round=function(_1b8,_1b9,_1ba){var _1bb=String(_1b8).split(".");var _1bc=(_1bb[1]&&_1bb[1].length)||0;if(_1bc>_1b9){var _1bd=Math.pow(10,_1b9);if(_1ba>0){_1bd*=10/_1ba;_1b9++;}_1b8=Math.round(_1b8*_1bd)/_1bd;_1bb=String(_1b8).split(".");_1bc=(_1bb[1]&&_1bb[1].length)||0;if(_1bc>_1b9){_1bb[1]=_1bb[1].substr(0,_1b9);_1b8=Number(_1bb.join("."));}}return _1b8;};dojo.number._formatAbsolute=function(_1be,_1bf,_1c0){_1c0=_1c0||{};if(_1c0.places===true){_1c0.places=0;}if(_1c0.places===Infinity){_1c0.places=6;}var _1c1=_1bf.split(".");var _1c2=(_1c0.places>=0)?_1c0.places:(_1c1[1]&&_1c1[1].length)||0;if(!(_1c0.round<0)){_1be=dojo.number.round(_1be,_1c2,_1c0.round);}var _1c3=String(Math.abs(_1be)).split(".");var _1c4=_1c3[1]||"";if(_1c0.places){_1c3[1]=dojo.string.pad(_1c4.substr(0,_1c0.places),_1c0.places,"0",true);}else{if(_1c1[1]&&_1c0.places!==0){var pad=_1c1[1].lastIndexOf("0")+1;if(pad>_1c4.length){_1c3[1]=dojo.string.pad(_1c4,pad,"0",true);}var _1c6=_1c1[1].length;if(_1c6<_1c4.length){_1c3[1]=_1c4.substr(0,_1c6);}}else{if(_1c3[1]){_1c3.pop();}}}var _1c7=_1c1[0].replace(",","");pad=_1c7.indexOf("0");if(pad!=-1){pad=_1c7.length-pad;if(pad>_1c3[0].length){_1c3[0]=dojo.string.pad(_1c3[0],pad);}if(_1c7.indexOf("#")==-1){_1c3[0]=_1c3[0].substr(_1c3[0].length-pad);}}var _1c8=_1c1[0].lastIndexOf(",");var _1c9,_1ca;if(_1c8!=-1){_1c9=_1c1[0].length-_1c8-1;var _1cb=_1c1[0].substr(0,_1c8);_1c8=_1cb.lastIndexOf(",");if(_1c8!=-1){_1ca=_1cb.length-_1c8-1;}}var _1cc=[];for(var _1cd=_1c3[0];_1cd;){var off=_1cd.length-_1c9;_1cc.push((off>0)?_1cd.substr(off):_1cd);_1cd=(off>0)?_1cd.slice(0,off):"";if(_1ca){_1c9=_1ca;delete _1ca;}}_1c3[0]=_1cc.reverse().join(_1c0.group||",");return _1c3.join(_1c0.decimal||".");};dojo.number.regexp=function(_1cf){return dojo.number._parseInfo(_1cf).regexp;};dojo.number._parseInfo=function(_1d0){_1d0=_1d0||{};var _1d1=dojo.i18n.normalizeLocale(_1d0.locale);var _1d2=dojo.i18n.getLocalization("dojo.cldr","number",_1d1);var _1d3=_1d0.pattern||_1d2[(_1d0.type||"decimal")+"Format"];var _1d4=_1d2.group;var _1d5=_1d2.decimal;var _1d6=1;if(_1d3.indexOf("%")!=-1){_1d6/=100;}else{if(_1d3.indexOf("\u2030")!=-1){_1d6/=1000;}else{var _1d7=_1d3.indexOf("\xa4")!=-1;if(_1d7){_1d4=_1d2.currencyGroup||_1d4;_1d5=_1d2.currencyDecimal||_1d5;}}}var _1d8=_1d3.split(";");if(_1d8.length==1){_1d8.push("-"+_1d8[0]);}var re=dojo.regexp.buildGroupRE(_1d8,function(_1da){_1da="(?:"+dojo.regexp.escapeString(_1da,".")+")";return _1da.replace(dojo.number._numberPatternRE,function(_1db){var _1dc={signed:false,separator:_1d0.strict?_1d4:[_1d4,""],fractional:_1d0.fractional,decimal:_1d5,exponent:false};var _1dd=_1db.split(".");var _1de=_1d0.places;if(_1dd.length==1||_1de===0){_1dc.fractional=false;}else{if(typeof _1de=="undefined"){_1de=_1dd[1].lastIndexOf("0")+1;}if(_1de&&_1d0.fractional==undefined){_1dc.fractional=true;}if(!_1d0.places&&(_1de<_1dd[1].length)){_1de+=","+_1dd[1].length;}_1dc.places=_1de;}var _1df=_1dd[0].split(",");if(_1df.length>1){_1dc.groupSize=_1df.pop().length;if(_1df.length>1){_1dc.groupSize2=_1df.pop().length;}}return "("+dojo.number._realNumberRegexp(_1dc)+")";});},true);if(_1d7){re=re.replace(/(\s*)(\u00a4{1,3})(\s*)/g,function(_1e0,_1e1,_1e2,_1e3){var prop=["symbol","currency","displayName"][_1e2.length-1];var _1e5=dojo.regexp.escapeString(_1d0[prop]||_1d0.currency||"");_1e1=_1e1?"\\s":"";_1e3=_1e3?"\\s":"";if(!_1d0.strict){if(_1e1){_1e1+="*";}if(_1e3){_1e3+="*";}return "(?:"+_1e1+_1e5+_1e3+")?";}return _1e1+_1e5+_1e3;});}return {regexp:re.replace(/[\xa0 ]/g,"[\\s\\xa0]"),group:_1d4,decimal:_1d5,factor:_1d6};};dojo.number.parse=function(_1e6,_1e7){var info=dojo.number._parseInfo(_1e7);var _1e9=(new RegExp("^"+info.regexp+"$")).exec(_1e6);if(!_1e9){return NaN;}var _1ea=_1e9[1];if(!_1e9[1]){if(!_1e9[2]){return NaN;}_1ea=_1e9[2];info.factor*=-1;}_1ea=_1ea.replace(new RegExp("["+info.group+"\\s\\xa0"+"]","g"),"").replace(info.decimal,".");return Number(_1ea)*info.factor;};dojo.number._realNumberRegexp=function(_1eb){_1eb=_1eb||{};if(typeof _1eb.places=="undefined"){_1eb.places=Infinity;}if(typeof _1eb.decimal!="string"){_1eb.decimal=".";}if(typeof _1eb.fractional=="undefined"||/^0/.test(_1eb.places)){_1eb.fractional=[true,false];}if(typeof _1eb.exponent=="undefined"){_1eb.exponent=[true,false];}if(typeof _1eb.eSigned=="undefined"){_1eb.eSigned=[true,false];}var _1ec=dojo.number._integerRegexp(_1eb);var _1ed=dojo.regexp.buildGroupRE(_1eb.fractional,function(q){var re="";if(q&&(_1eb.places!==0)){re="\\"+_1eb.decimal;if(_1eb.places==Infinity){re="(?:"+re+"\\d+)?";}else{re+="\\d{"+_1eb.places+"}";}}return re;},true);var _1f0=dojo.regexp.buildGroupRE(_1eb.exponent,function(q){if(q){return "([eE]"+dojo.number._integerRegexp({signed:_1eb.eSigned})+")";}return "";});var _1f2=_1ec+_1ed;if(_1ed){_1f2="(?:(?:"+_1f2+")|(?:"+_1ed+"))";}return _1f2+_1f0;};dojo.number._integerRegexp=function(_1f3){_1f3=_1f3||{};if(typeof _1f3.signed=="undefined"){_1f3.signed=[true,false];}if(typeof _1f3.separator=="undefined"){_1f3.separator="";}else{if(typeof _1f3.groupSize=="undefined"){_1f3.groupSize=3;}}var _1f4=dojo.regexp.buildGroupRE(_1f3.signed,function(q){return q?"[-+]":"";},true);var _1f6=dojo.regexp.buildGroupRE(_1f3.separator,function(sep){if(!sep){return "(?:0|[1-9]\\d*)";}sep=dojo.regexp.escapeString(sep);if(sep==" "){sep="\\s";}else{if(sep=="\xa0"){sep="\\s\\xa0";}}var grp=_1f3.groupSize,grp2=_1f3.groupSize2;if(grp2){var _1fa="(?:0|[1-9]\\d{0,"+(grp2-1)+"}(?:["+sep+"]\\d{"+grp2+"})*["+sep+"]\\d{"+grp+"})";return ((grp-grp2)>0)?"(?:"+_1fa+"|(?:0|[1-9]\\d{0,"+(grp-1)+"}))":_1fa;}return "(?:0|[1-9]\\d{0,"+(grp-1)+"}(?:["+sep+"]\\d{"+grp+"})*)";},true);return _1f4+_1f6;};}if(!dojo._hasResource["dojo.cldr.monetary"]){dojo._hasResource["dojo.cldr.monetary"]=true;dojo.provide("dojo.cldr.monetary");dojo.cldr.monetary.getData=function(code){var _1fc={ADP:0,BHD:3,BIF:0,BYR:0,CLF:0,CLP:0,DJF:0,ESP:0,GNF:0,IQD:3,ITL:0,JOD:3,JPY:0,KMF:0,KRW:0,KWD:3,LUF:0,LYD:3,MGA:0,MGF:0,OMR:3,PYG:0,RWF:0,TND:3,TRL:0,VUV:0,XAF:0,XOF:0,XPF:0};var _1fd={CHF:5};var _1fe=_1fc[code],_1ff=_1fd[code];if(typeof _1fe=="undefined"){_1fe=2;}if(typeof _1ff=="undefined"){_1ff=0;}return {places:_1fe,round:_1ff};};}if(!dojo._hasResource["dojo.currency"]){dojo._hasResource["dojo.currency"]=true;dojo.provide("dojo.currency");dojo.currency._mixInDefaults=function(_200){_200=_200||{};_200.type="currency";var _201=dojo.i18n.getLocalization("dojo.cldr","currency",_200.locale)||{};var iso=_200.currency;var data=dojo.cldr.monetary.getData(iso);dojo.forEach(["displayName","symbol","group","decimal"],function(prop){data[prop]=_201[iso+"_"+prop];});data.fractional=[true,false];return dojo.mixin(data,_200);};dojo.currency.format=function(_205,_206){return dojo.number.format(_205,dojo.currency._mixInDefaults(_206));};dojo.currency.regexp=function(_207){return dojo.number.regexp(dojo.currency._mixInDefaults(_207));};dojo.currency.parse=function(_208,_209){return dojo.number.parse(_208,dojo.currency._mixInDefaults(_209));};}if(!dojo._hasResource["dojo.data.util.filter"]){dojo._hasResource["dojo.data.util.filter"]=true;dojo.provide("dojo.data.util.filter");dojo.data.util.filter.patternToRegExp=function(_20a,_20b){var rxp="^";var c=null;for(var i=0;i<_20a.length;i++){c=_20a.charAt(i);switch(c){case "\\":rxp+=c;i++;rxp+=_20a.charAt(i);break;case "*":rxp+=".*";break;case "?":rxp+=".";break;case "$":case "^":case "/":case "+":case ".":case "|":case "(":case ")":case "{":case "}":case "[":case "]":rxp+="\\";default:rxp+=c;}}rxp+="$";if(_20b){return new RegExp(rxp,"i");}else{return new RegExp(rxp);}};}if(!dojo._hasResource["dojo.data.util.sorter"]){dojo._hasResource["dojo.data.util.sorter"]=true;dojo.provide("dojo.data.util.sorter");dojo.data.util.sorter.basicComparator=function(a,b){var ret=0;if(a>b||typeof a==="undefined"){ret=1;}else{if(a<b||typeof b==="undefined"){ret=-1;}}return ret;};dojo.data.util.sorter.createSortFunction=function(_212,_213){var _214=[];function createSortFunction(attr,dir){return function(_217,_218){var a=_213.getValue(_217,attr);var b=_213.getValue(_218,attr);var _21b=null;if(_213.comparatorMap){if(typeof attr!=="string"){attr=_213.getIdentity(attr);}_21b=_213.comparatorMap[attr]||dojo.data.util.sorter.basicComparator;}_21b=_21b||dojo.data.util.sorter.basicComparator;return dir*_21b(a,b);};};for(var i=0;i<_212.length;i++){sortAttribute=_212[i];if(sortAttribute.attribute){var _21d=(sortAttribute.descending)?-1:1;_214.push(createSortFunction(sortAttribute.attribute,_21d));}}return function(rowA,rowB){var i=0;while(i<_214.length){var ret=_214[i++](rowA,rowB);if(ret!==0){return ret;}}return 0;};};}if(!dojo._hasResource["dojo.data.util.simpleFetch"]){dojo._hasResource["dojo.data.util.simpleFetch"]=true;dojo.provide("dojo.data.util.simpleFetch");dojo.data.util.simpleFetch.fetch=function(_222){_222=_222||{};if(!_222.store){_222.store=this;}var self=this;var _224=function(_225,_226){if(_226.onError){var _227=_226.scope||dojo.global;_226.onError.call(_227,_225,_226);}};var _228=function(_229,_22a){var _22b=_22a.abort||null;var _22c=false;var _22d=_22a.start?_22a.start:0;var _22e=_22a.count?(_22d+_22a.count):_229.length;_22a.abort=function(){_22c=true;if(_22b){_22b.call(_22a);}};var _22f=_22a.scope||dojo.global;if(!_22a.store){_22a.store=self;}if(_22a.onBegin){_22a.onBegin.call(_22f,_229.length,_22a);}if(_22a.sort){_229.sort(dojo.data.util.sorter.createSortFunction(_22a.sort,self));}if(_22a.onItem){for(var i=_22d;(i<_229.length)&&(i<_22e);++i){var item=_229[i];if(!_22c){_22a.onItem.call(_22f,item,_22a);}}}if(_22a.onComplete&&!_22c){var _232=null;if(!_22a.onItem){_232=_229.slice(_22d,_22e);}_22a.onComplete.call(_22f,_232,_22a);}};this._fetchItems(_222,_228,_224);return _222;};}if(!dojo._hasResource["dojo.data.JsonItemStore"]){dojo._hasResource["dojo.data.JsonItemStore"]=true;dojo.provide("dojo.data.JsonItemStore");dojo.declare("dojo.data.JsonItemStore",null,{constructor:function(_233){this._arrayOfAllItems=[];this._loadFinished=false;this._jsonFileUrl=_233.url;this._jsonData=_233.data;this._features={"dojo.data.api.Read":true};this._itemsByIdentity=null;this._storeRef="_S";this._itemId="_0";},url:"",_assertIsItem:function(item){if(!this.isItem(item)){throw new Error("dojo.data.JsonItemStore: a function was passed an item argument that was not an item");}},_assertIsAttribute:function(_235){if(typeof _235!=="string"){throw new Error("dojo.data.JsonItemStore: a function was passed an attribute argument that was not an attribute name string");}},getValue:function(item,_237,_238){var _239=this.getValues(item,_237);return (_239.length>0)?_239[0]:_238;},getValues:function(item,_23b){this._assertIsItem(item);this._assertIsAttribute(_23b);return item[_23b]||[];},getAttributes:function(item){this._assertIsItem(item);var _23d=[];for(var key in item){if((key!==this._storeRef)&&(key!==this._itemId)){_23d.push(key);}}return _23d;},hasAttribute:function(item,_240){return this.getValues(item,_240).length>0;},containsValue:function(item,_242,_243){var _244=undefined;if(typeof _243==="string"){_244=dojo.data.util.filter.patternToRegExp(_243,false);}return this._containsValue(item,_242,_243,_244);},_containsValue:function(item,_246,_247,_248){var _249=this.getValues(item,_246);for(var i=0;i<_249.length;++i){var _24b=_249[i];if(typeof _24b==="string"&&_248){return (_24b.match(_248)!==null);}else{if(_247===_24b){return true;}}}return false;},isItem:function(_24c){if(_24c&&_24c[this._storeRef]===this){if(this._arrayOfAllItems[_24c[this._itemId]]===_24c){return true;}}return false;},isItemLoaded:function(_24d){return this.isItem(_24d);},loadItem:function(_24e){this._assertIsItem(_24e.item);},getFeatures:function(){if(!this._loadFinished){this._forceLoad();}return this._features;},getLabel:function(item){if(this._labelAttr&&this.isItem(item)){return this.getValue(item,this._labelAttr);}return undefined;},getLabelAttributes:function(item){if(this._labelAttr){return [this._labelAttr];}return null;},_fetchItems:function(_251,_252,_253){var self=this;var _255=function(_256,_257){var _258=null;if(_256.query){var _259=_256.queryOptions?_256.queryOptions.ignoreCase:false;_258=[];var _25a={};for(var key in _256.query){var _25c=_256.query[key];if(typeof _25c==="string"){_25a[key]=dojo.data.util.filter.patternToRegExp(_25c,_259);}}for(var i=0;i<_257.length;++i){var _25e=true;var _25f=_257[i];for(var key in _256.query){var _25c=_256.query[key];if(!self._containsValue(_25f,key,_25c,_25a[key])){_25e=false;}}if(_25e){_258.push(_25f);}}_252(_258,_256);}else{if(self._arrayOfAllItems.length>0){_258=self._arrayOfAllItems.slice(0,self._arrayOfAllItems.length);}_252(_258,_256);}};if(this._loadFinished){_255(_251,this._arrayOfAllItems);}else{if(this._jsonFileUrl){var _260={url:self._jsonFileUrl,handleAs:"json-comment-optional"};var _261=dojo.xhrGet(_260);_261.addCallback(function(data){self._loadFinished=true;try{self._arrayOfAllItems=self._getItemsFromLoadedData(data);_255(_251,self._arrayOfAllItems);}catch(e){_253(e,_251);}});_261.addErrback(function(_263){_253(_263,_251);});}else{if(this._jsonData){try{this._loadFinished=true;this._arrayOfAllItems=this._getItemsFromLoadedData(this._jsonData);this._jsonData=null;_255(_251,this._arrayOfAllItems);}catch(e){_253(e,_251);}}else{_253(new Error("dojo.data.JsonItemStore: No JSON source data was provided as either URL or a nested Javascript object."),_251);}}}},close:function(_264){},_getItemsFromLoadedData:function(_265){var _266=_265.items;var i;var item;var _269={};this._labelAttr=_265.label;for(i=0;i<_266.length;++i){item=_266[i];for(var key in item){var _26b=item[key];if(_26b!==null){if(!dojo.isArray(_26b)){item[key]=[_26b];}}else{item[key]=[null];}_269[key]=key;}}while(_269[this._storeRef]){this._storeRef+="_";}while(_269[this._itemId]){this._itemId+="_";}var _26c=_265.identifier;var _26d=null;if(_26c){this._features["dojo.data.api.Identity"]=_26c;this._itemsByIdentity={};for(var i=0;i<_266.length;++i){item=_266[i];_26d=item[_26c];identity=_26d[0];if(!this._itemsByIdentity[identity]){this._itemsByIdentity[identity]=item;}else{if(this._jsonFileUrl){throw new Error("dojo.data.JsonItemStore:  The json data as specified by: ["+this._jsonFileUrl+"] is malformed.  Items within the list have identifier: ["+_26c+"].  Value collided: ["+identity+"]");}else{if(this._jsonData){throw new Error("dojo.data.JsonItemStore:  The json data provided by the creation arguments is malformed.  Items within the list have identifier: ["+_26c+"].  Value collided: ["+identity+"]");}}}}}for(i=0;i<_266.length;++i){item=_266[i];item[this._storeRef]=this;item[this._itemId]=i;for(key in item){_26d=item[key];for(var j=0;j<_26d.length;++j){_26b=_26d[j];if(_26b!==null&&typeof _26b=="object"&&_26b.reference){var _26f=_26b.reference;if(dojo.isString(_26f)){_26d[j]=this._itemsByIdentity[_26f];}else{for(var k=0;k<_266.length;++k){var _271=_266[k];var _272=true;for(var _273 in _26f){if(_271[_273]!=_26f[_273]){_272=false;}}if(_272){_26d[j]=_271;}}}}}}}return _266;},getIdentity:function(item){var _275=this._features["dojo.data.api.Identity"];var _276=item[_275];if(_276){return _276[0];}return null;},fetchItemByIdentity:function(_277){if(!this._loadFinished){var self=this;if(this._jsonFileUrl){var _279={url:self._jsonFileUrl,handleAs:"json-comment-optional"};var _27a=dojo.xhrGet(_279);_27a.addCallback(function(data){var _27c=_277.scope?_277.scope:dojo.global;try{self._arrayOfAllItems=self._getItemsFromLoadedData(data);self._loadFinished=true;var item=self._getItemByIdentity(_277.identity);if(_277.onItem){_277.onItem.call(_27c,item);}}catch(error){if(_277.onError){_277.onError.call(_27c,error);}}});_27a.addErrback(function(_27e){if(_277.onError){var _27f=_277.scope?_277.scope:dojo.global;_277.onError.call(_27f,_27e);}});}else{if(this._jsonData){self._arrayOfAllItems=self._getItemsFromLoadedData(self._jsonData);self._jsonData=null;self._loadFinished=true;var item=self._getItemByIdentity(_277.identity);if(_277.onItem){var _281=_277.scope?_277.scope:dojo.global;_277.onItem.call(_281,item);}}}}else{var item=this._getItemByIdentity(_277.identity);if(_277.onItem){var _281=_277.scope?_277.scope:dojo.global;_277.onItem.call(_281,item);}}},_getItemByIdentity:function(_282){var item=null;if(this._itemsByIdentity){item=this._itemsByIdentity[_282];if(item===undefined){item=null;}}return item;},getIdentityAttributes:function(item){var _285=this._features["dojo.data.api.Identity"];if(_285){return [_285];}return null;},_forceLoad:function(){var self=this;if(this._jsonFileUrl){var _287={url:self._jsonFileUrl,handleAs:"json-comment-optional",sync:true};var _288=dojo.xhrGet(_287);_288.addCallback(function(data){try{self._arrayOfAllItems=self._getItemsFromLoadedData(data);self._loadFinished=true;}catch(e){console.log(e);throw e;}});_288.addErrback(function(_28a){throw _28a;});}else{if(this._jsonData){self._arrayOfAllItems=self._getItemsFromLoadedData(self._jsonData);self._jsonData=null;self._loadFinished=true;}}}});dojo.extend(dojo.data.JsonItemStore,dojo.data.util.simpleFetch);}if(!dojo._hasResource["dojo.data.JsonItemStoreAutoComplete"]){dojo._hasResource["dojo.data.JsonItemStoreAutoComplete"]=true;dojo.provide("dojo.data.JsonItemStoreAutoComplete");dojo.declare("dojo.data.JsonItemStoreAutoComplete",dojo.data.JsonItemStore,{_fetchItems:function(_28b,_28c,_28d){var self=this;var _28f=function(_290,_291){var _292=null;if(_290.query){var _293=_290.queryOptions?_290.queryOptions.ignoreCase:false;_292=[];var _294={};for(var key in _290.query){var _296=_290.query[key];if(typeof _296==="string"){_294[key]=dojo.data.util.filter.patternToRegExp(_296,_293);}}for(var i=0;i<_291.length;++i){var _298=true;var _299=_291[i];if(_298){_292.push(_299);}}_28c(_292,_290);}else{if(self._arrayOfAllItems.length>0){_292=self._arrayOfAllItems.slice(0,self._arrayOfAllItems.length);}_28c(_292,_290);}};var _29a=null;if(_28b.query){_29a=_28b.query["name"];}if(_29a){var pos=_29a.lastIndexOf("*");_29a=_29a.substr(0,pos);}if(_29a){if(this._jsonFileUrl){var _29c={url:self._jsonFileUrl+"?keyword="+_29a,handleAs:"json-comment-optional"};var _29d=dojo.xhrGet(_29c);_29d.addCallback(function(data){self._loadFinished=true;try{self._arrayOfAllItems=self._getItemsFromLoadedData(data);_28f(_28b,self._arrayOfAllItems);}catch(e){_28d(e,_28b);}});_29d.addErrback(function(_29f){_28d(_29f,_28b);});}else{if(this._jsonData){try{this._loadFinished=true;this._arrayOfAllItems=this._getItemsFromLoadedData(this._jsonData);this._jsonData=null;_28f(_28b,this._arrayOfAllItems);}catch(e){_28d(e,_28b);}}else{_28d(new Error("dojo.data.JsonItemStoreAutoComplete: No JSON source data was provided as either URL or a nested Javascript object."),_28b);}}}}});dojo.extend(dojo.data.JsonItemStoreAutoComplete,dojo.data.util.simpleFetch);}if(!dojo._hasResource["dijit._base.focus"]){dojo._hasResource["dijit._base.focus"]=true;dojo.provide("dijit._base.focus");dojo.mixin(dijit,{_curFocus:null,_prevFocus:null,isCollapsed:function(){var _2a0=dojo.global;var _2a1=dojo.doc;if(_2a1.selection){return !_2a1.selection.createRange().text;}else{if(_2a0.getSelection){var _2a2=_2a0.getSelection();if(dojo.isString(_2a2)){return !_2a2;}else{return _2a2.isCollapsed||!_2a2.toString();}}}},getBookmark:function(){var _2a3,_2a4=dojo.doc.selection;if(_2a4){var _2a5=_2a4.createRange();if(_2a4.type.toUpperCase()=="CONTROL"){_2a3=_2a5.length?dojo._toArray(_2a5):null;}else{_2a3=_2a5.getBookmark();}}else{if(dojo.global.getSelection){_2a4=dojo.global.getSelection();if(_2a4){var _2a5=_2a4.getRangeAt(0);_2a3=_2a5.cloneRange();}}else{console.debug("No idea how to store the current selection for this browser!");}}return _2a3;},moveToBookmark:function(_2a6){var _2a7=dojo.doc;if(_2a7.selection){var _2a8;if(dojo.isArray(_2a6)){_2a8=_2a7.body.createControlRange();dojo.forEach(_2a6,_2a8.addElement);}else{_2a8=_2a7.selection.createRange();_2a8.moveToBookmark(_2a6);}_2a8.select();}else{var _2a9=dojo.global.getSelection&&dojo.global.getSelection();if(_2a9&&_2a9.removeAllRanges){_2a9.removeAllRanges();_2a9.addRange(_2a6);}else{console.debug("No idea how to restore selection for this browser!");}}},getFocus:function(menu,_2ab){return {node:menu&&dojo.isDescendant(dijit._curFocus,menu.domNode)?dijit._prevFocus:dijit._curFocus,bookmark:!dojo.withGlobal(_2ab||dojo.global,dijit.isCollapsed)?dojo.withGlobal(_2ab||dojo.global,dijit.getBookmark):null,openedForWindow:_2ab};},focus:function(_2ac){if(!_2ac){return;}var node="node" in _2ac?_2ac.node:_2ac,_2ae=_2ac.bookmark,_2af=_2ac.openedForWindow;if(node){var _2b0=(node.tagName.toLowerCase()=="iframe")?node.contentWindow:node;if(_2b0&&_2b0.focus){try{_2b0.focus();}catch(e){}}dijit._onFocusNode(node);}if(_2ae&&dojo.withGlobal(_2af||dojo.global,dijit.isCollapsed)){if(_2af){_2af.focus();}try{dojo.withGlobal(_2af||dojo.global,moveToBookmark,null,[_2ae]);}catch(e){}}},_activeStack:[],registerWin:function(_2b1){if(!_2b1){_2b1=window;}dojo.connect(_2b1.document,"onmousedown",null,function(evt){dijit._ignoreNextBlurEvent=true;setTimeout(function(){dijit._ignoreNextBlurEvent=false;},0);dijit._onTouchNode(evt.target||evt.srcElement);});var body=_2b1.document.body||_2b1.document.getElementsByTagName("body")[0];if(body){if(dojo.isIE){body.attachEvent("onactivate",function(evt){if(evt.srcElement.tagName.toLowerCase()!="body"){dijit._onFocusNode(evt.srcElement);}});body.attachEvent("ondeactivate",function(evt){dijit._onBlurNode();});}else{body.addEventListener("focus",function(evt){dijit._onFocusNode(evt.target);},true);body.addEventListener("blur",function(evt){dijit._onBlurNode();},true);}}},_onBlurNode:function(){if(dijit._ignoreNextBlurEvent){dijit._ignoreNextBlurEvent=false;return;}dijit._prevFocus=dijit._curFocus;dijit._curFocus=null;if(dijit._blurAllTimer){clearTimeout(dijit._blurAllTimer);}dijit._blurAllTimer=setTimeout(function(){delete dijit._blurAllTimer;dijit._setStack([]);},100);},_onTouchNode:function(node){if(dijit._blurAllTimer){clearTimeout(dijit._blurAllTimer);delete dijit._blurAllTimer;}var _2b9=[];try{while(node){if(node.dijitPopupParent){node=dijit.byId(node.dijitPopupParent).domNode;}else{if(node.tagName&&node.tagName.toLowerCase()=="body"){if(node===dojo.body()){break;}node=dojo.query("iframe").filter(function(_2ba){return _2ba.contentDocument.body===node;})[0];}else{var id=node.getAttribute&&node.getAttribute("widgetId");if(id){_2b9.unshift(id);}node=node.parentNode;}}}}catch(e){}dijit._setStack(_2b9);},_onFocusNode:function(node){if(node&&node.tagName&&node.tagName.toLowerCase()=="body"){return;}dijit._onTouchNode(node);if(node==dijit._curFocus){return;}dijit._prevFocus=dijit._curFocus;dijit._curFocus=node;dojo.publish("focusNode",[node]);var w=dijit.byId(node.id);if(w&&w._setStateClass){w._focused=true;w._setStateClass();var _2be=dojo.connect(node,"onblur",function(){w._focused=false;w._setStateClass();dojo.disconnect(_2be);});}},_setStack:function(_2bf){var _2c0=dijit._activeStack;dijit._activeStack=_2bf;for(var _2c1=0;_2c1<Math.min(_2c0.length,_2bf.length);_2c1++){if(_2c0[_2c1]!=_2bf[_2c1]){break;}}for(var i=_2c0.length-1;i>=_2c1;i--){var _2c3=dijit.byId(_2c0[i]);if(_2c3){dojo.publish("widgetBlur",[_2c3]);if(_2c3._onBlur){_2c3._onBlur();}}}for(var i=_2c1;i<_2bf.length;i++){var _2c3=dijit.byId(_2bf[i]);if(_2c3){dojo.publish("widgetFocus",[_2c3]);if(_2c3._onFocus){_2c3._onFocus();}}}}});dojo.addOnLoad(dijit.registerWin);}if(!dojo._hasResource["dijit._base.manager"]){dojo._hasResource["dijit._base.manager"]=true;dojo.provide("dijit._base.manager");dojo.declare("dijit.WidgetSet",null,{constructor:function(){this._hash={};},add:function(_2c4){this._hash[_2c4.id]=_2c4;},remove:function(id){delete this._hash[id];},forEach:function(func){for(var id in this._hash){func(this._hash[id]);}},filter:function(_2c8){var res=new dijit.WidgetSet();this.forEach(function(_2ca){if(_2c8(_2ca)){res.add(_2ca);}});return res;},byId:function(id){return this._hash[id];},byClass:function(cls){return this.filter(function(_2cd){return _2cd.declaredClass==cls;});}});dijit.registry=new dijit.WidgetSet();dijit._widgetTypeCtr={};dijit.getUniqueId=function(_2ce){var id;do{id=_2ce+"_"+(dijit._widgetTypeCtr[_2ce]!==undefined?++dijit._widgetTypeCtr[_2ce]:dijit._widgetTypeCtr[_2ce]=0);}while(dijit.byId(id));return id;};if(dojo.isIE){dojo.addOnUnload(function(){dijit.registry.forEach(function(_2d0){_2d0.destroy();});});}dijit.byId=function(id){return (dojo.isString(id))?dijit.registry.byId(id):id;};dijit.byNode=function(node){return dijit.registry.byId(node.getAttribute("widgetId"));};}if(!dojo._hasResource["dijit._base.place"]){dojo._hasResource["dijit._base.place"]=true;dojo.provide("dijit._base.place");dijit.getViewport=function(){var _2d3=dojo.global;var _2d4=dojo.doc;var w=0,h=0;if(dojo.isMozilla){w=_2d4.documentElement.clientWidth;h=_2d3.innerHeight;}else{if(!dojo.isOpera&&_2d3.innerWidth){w=_2d3.innerWidth;h=_2d3.innerHeight;}else{if(dojo.isIE&&_2d4.documentElement&&_2d4.documentElement.clientHeight){w=_2d4.documentElement.clientWidth;h=_2d4.documentElement.clientHeight;}else{if(dojo.body().clientWidth){w=dojo.body().clientWidth;h=dojo.body().clientHeight;}}}}var _2d7=dojo._docScroll();return {w:w,h:h,l:_2d7.x,t:_2d7.y};};dijit.placeOnScreen=function(node,pos,_2da,_2db){var _2dc=dojo.map(_2da,function(_2dd){return {corner:_2dd,pos:pos};});return dijit._place(node,_2dc);};dijit._place=function(node,_2df,_2e0){var view=dijit.getViewport();if(!node.parentNode||String(node.parentNode.tagName).toLowerCase()!="body"){dojo.body().appendChild(node);}var best=null;for(var i=0;i<_2df.length;i++){var _2e4=_2df[i].corner;var pos=_2df[i].pos;if(_2e0){_2e0(_2e4);}var _2e6=node.style.display;var _2e7=node.style.visibility;node.style.visibility="hidden";node.style.display="";var mb=dojo.marginBox(node);node.style.display=_2e6;node.style.visibility=_2e7;var _2e9=(_2e4.charAt(1)=="L"?pos.x:Math.max(view.l,pos.x-mb.w)),_2ea=(_2e4.charAt(0)=="T"?pos.y:Math.max(view.t,pos.y-mb.h)),endX=(_2e4.charAt(1)=="L"?Math.min(view.l+view.w,_2e9+mb.w):pos.x),endY=(_2e4.charAt(0)=="T"?Math.min(view.t+view.h,_2ea+mb.h):pos.y),_2ed=endX-_2e9,_2ee=endY-_2ea,_2ef=(mb.w-_2ed)+(mb.h-_2ee);if(best==null||_2ef<best.overflow){best={corner:_2e4,aroundCorner:_2df[i].aroundCorner,x:_2e9+view.l,y:_2ea+view.t,w:_2ed,h:_2ee,overflow:_2ef};}if(_2ef==0){break;}}node.style.left=best.x+"px";node.style.top=best.y+"px";return best;};dijit.placeOnScreenAroundElement=function(node,_2f1,_2f2,_2f3){_2f1=dojo.byId(_2f1);var _2f4=_2f1.style.display;_2f1.style.display="";var _2f5=_2f1.offsetWidth;var _2f6=_2f1.offsetHeight;var _2f7=dojo.coords(_2f1,true);_2f1.style.display=_2f4;var _2f8=[];for(var _2f9 in _2f2){_2f8.push({aroundCorner:_2f9,corner:_2f2[_2f9],pos:{x:_2f7.x+(_2f9.charAt(1)=="L"?0:_2f5),y:_2f7.y+(_2f9.charAt(0)=="T"?0:_2f6)}});}return dijit._place(node,_2f8,_2f3);};}if(!dojo._hasResource["dijit._base.window"]){dojo._hasResource["dijit._base.window"]=true;dojo.provide("dijit._base.window");dijit.getDocumentWindow=function(doc){if(dojo.isSafari&&!doc._parentWindow){var fix=function(win){win.document._parentWindow=win;for(var i=0;i<win.frames.length;i++){fix(win.frames[i]);}};fix(window.top);}if(dojo.isIE&&window!==document.parentWindow&&!doc._parentWindow){doc.parentWindow.execScript("document._parentWindow = window;","Javascript");var win=doc._parentWindow;doc._parentWindow=null;return win;}return doc._parentWindow||doc.parentWindow||doc.defaultView;};}if(!dojo._hasResource["dijit._base.popup"]){dojo._hasResource["dijit._base.popup"]=true;dojo.provide("dijit._base.popup");dijit.popup=new function(){var _2ff=[],_300=1000,_301=1;this.open=function(args){var _303=args.popup,_304=args.orient||{"BL":"TL","TL":"BL"},_305=args.around,id=(args.around&&args.around.id)?(args.around.id+"_dropdown"):("popup_"+_301++);if(!args.submenu){this.closeAll();}var _307=dojo.doc.createElement("div");_307.id=id;_307.className="dijitPopup";_307.style.zIndex=_300+_2ff.length;if(args.parent){_307.dijitPopupParent=args.parent.id;}dojo.body().appendChild(_307);_303.domNode.style.display="";_307.appendChild(_303.domNode);var _308=new dijit.BackgroundIframe(_307);var best=_305?dijit.placeOnScreenAroundElement(_307,_305,_304,_303.orient?dojo.hitch(_303,"orient"):null):dijit.placeOnScreen(_307,args,_304=="R"?["TR","BR","TL","BL"]:["TL","BL","TR","BR"]);var _30a=[];_30a.push(dojo.connect(_307,"onkeypress",this,function(evt){if(evt.keyCode==dojo.keys.ESCAPE){args.onCancel();}}));if(_303.onCancel){_30a.push(dojo.connect(_303,"onCancel",null,args.onCancel));}_30a.push(dojo.connect(_303,_303.onExecute?"onExecute":"onChange",null,function(){if(_2ff[0]&&_2ff[0].onExecute){_2ff[0].onExecute();}}));_2ff.push({wrapper:_307,iframe:_308,widget:_303,onExecute:args.onExecute,onCancel:args.onCancel,onClose:args.onClose,handlers:_30a});if(_303.onOpen){_303.onOpen(best);}return best;};this.close=function(){var _30c=_2ff[_2ff.length-1].widget;if(_30c.onClose){_30c.onClose();}if(!_2ff.length){return;}var top=_2ff.pop();var _30e=top.wrapper,_30f=top.iframe,_30c=top.widget,_310=top.onClose;dojo.forEach(top.handlers,dojo.disconnect);if(!_30c||!_30c.domNode){return;}dojo.style(_30c.domNode,"display","none");dojo.body().appendChild(_30c.domNode);_30f.destroy();dojo._destroyElement(_30e);if(_310){_310();}};this.closeAll=function(){while(_2ff.length){this.close();}};this.closeTo=function(_311){while(_2ff.length&&_2ff[_2ff.length-1].widget.id!=_311.id){this.close();}};}();dijit._frames=new function(){var _312=[];this.pop=function(){var _313;if(_312.length){_313=_312.pop();_313.style.display="";}else{if(dojo.isIE){var html="<iframe src='javascript:\"\"'"+" style='position: absolute; left: 0px; top: 0px;"+"z-index: -1; filter:Alpha(Opacity=\"0\");'>";_313=dojo.doc.createElement(html);}else{var _313=dojo.doc.createElement("iframe");_313.src="javascript:\"\"";_313.className="dijitBackgroundIframe";}_313.tabIndex=-1;dojo.body().appendChild(_313);}return _313;};this.push=function(_315){_315.style.display="";if(dojo.isIE){_315.style.removeExpression("width");_315.style.removeExpression("height");}_312.push(_315);};}();if(dojo.isIE&&dojo.isIE<7){dojo.addOnLoad(function(){var f=dijit._frames;dojo.forEach([f.pop()],f.push);});}dijit.BackgroundIframe=function(node){if(!node.id){throw new Error("no id");}if((dojo.isIE&&dojo.isIE<7)||(dojo.isFF&&dojo.isFF<3&&dojo.hasClass(dojo.body(),"dijit_a11y"))){var _318=dijit._frames.pop();node.appendChild(_318);if(dojo.isIE){_318.style.setExpression("width","document.getElementById('"+node.id+"').offsetWidth");_318.style.setExpression("height","document.getElementById('"+node.id+"').offsetHeight");}this.iframe=_318;}};dojo.extend(dijit.BackgroundIframe,{destroy:function(){if(this.iframe){dijit._frames.push(this.iframe);delete this.iframe;}}});}if(!dojo._hasResource["dijit._base.scroll"]){dojo._hasResource["dijit._base.scroll"]=true;dojo.provide("dijit._base.scroll");dijit.scrollIntoView=function(node){if(dojo.isIE){if(dojo.marginBox(node.parentNode).h<=node.parentNode.scrollHeight){node.scrollIntoView(false);}}else{if(dojo.isMozilla){node.scrollIntoView(false);}else{var _31a=node.parentNode;var _31b=_31a.scrollTop+dojo.marginBox(_31a).h;var _31c=node.offsetTop+dojo.marginBox(node).h;if(_31b<_31c){_31a.scrollTop+=(_31c-_31b);}else{if(_31a.scrollTop>node.offsetTop){_31a.scrollTop-=(_31a.scrollTop-node.offsetTop);}}}}};}if(!dojo._hasResource["dijit._base.sniff"]){dojo._hasResource["dijit._base.sniff"]=true;dojo.provide("dijit._base.sniff");(function(){var d=dojo;var ie=d.isIE;var _31f=d.isOpera;var maj=Math.floor;var _321={dj_ie:ie,dj_ie6:maj(ie)==6,dj_ie7:maj(ie)==7,dj_iequirks:ie&&d.isQuirks,dj_opera:_31f,dj_opera8:maj(_31f)==8,dj_opera9:maj(_31f)==9,dj_khtml:d.isKhtml,dj_safari:d.isSafari,dj_gecko:d.isMozilla};for(var p in _321){if(_321[p]){var html=dojo.doc.documentElement;if(html.className){html.className+=" "+p;}else{html.className=p;}}}})();}if(!dojo._hasResource["dijit._base.bidi"]){dojo._hasResource["dijit._base.bidi"]=true;dojo.provide("dijit._base.bidi");dojo.addOnLoad(function(){if(!dojo._isBodyLtr()){dojo.addClass(dojo.body(),"dijitRtl");}});}if(!dojo._hasResource["dijit._base.typematic"]){dojo._hasResource["dijit._base.typematic"]=true;dojo.provide("dijit._base.typematic");dijit.typematic={_fireEventAndReload:function(){this._timer=null;this._callback(++this._count,this._node,this._evt);this._currentTimeout=(this._currentTimeout<0)?this._initialDelay:((this._subsequentDelay>1)?this._subsequentDelay:Math.round(this._currentTimeout*this._subsequentDelay));this._timer=setTimeout(dojo.hitch(this,"_fireEventAndReload"),this._currentTimeout);},trigger:function(evt,_325,node,_327,obj,_329,_32a){if(obj!=this._obj){this.stop();this._initialDelay=_32a?_32a:500;this._subsequentDelay=_329?_329:0.9;this._obj=obj;this._evt=evt;this._node=node;this._currentTimeout=-1;this._count=-1;this._callback=dojo.hitch(_325,_327);this._fireEventAndReload();}},stop:function(){if(this._timer){clearTimeout(this._timer);this._timer=null;}if(this._obj){this._callback(-1,this._node,this._evt);this._obj=null;}},addKeyListener:function(node,_32c,_32d,_32e,_32f,_330){var ary=[];ary.push(dojo.connect(node,"onkeypress",this,function(evt){if(evt.keyCode==_32c.keyCode&&(!_32c.charCode||_32c.charCode==evt.charCode)&&((typeof _32c.ctrlKey=="undefined")||_32c.ctrlKey==evt.ctrlKey)&&((typeof _32c.altKey=="undefined")||_32c.altKey==evt.ctrlKey)&&((typeof _32c.shiftKey=="undefined")||_32c.shiftKey==evt.ctrlKey)){dojo.stopEvent(evt);dijit.typematic.trigger(_32c,_32d,node,_32e,_32c,_32f,_330);}else{if(dijit.typematic._obj==_32c){dijit.typematic.stop();}}}));ary.push(dojo.connect(node,"onkeyup",this,function(evt){if(dijit.typematic._obj==_32c){dijit.typematic.stop();}}));return ary;},addMouseListener:function(node,_335,_336,_337,_338){var ary=[];ary.push(dojo.connect(node,"mousedown",this,function(evt){dojo.stopEvent(evt);dijit.typematic.trigger(evt,_335,node,_336,node,_337,_338);}));ary.push(dojo.connect(node,"mouseup",this,function(evt){dojo.stopEvent(evt);dijit.typematic.stop();}));ary.push(dojo.connect(node,"mouseout",this,function(evt){dojo.stopEvent(evt);dijit.typematic.stop();}));ary.push(dojo.connect(node,"mousemove",this,function(evt){dojo.stopEvent(evt);}));ary.push(dojo.connect(node,"dblclick",this,function(evt){dojo.stopEvent(evt);if(dojo.isIE){dijit.typematic.trigger(evt,_335,node,_336,node,_337,_338);setTimeout("dijit.typematic.stop()",50);}}));return ary;},addListener:function(_33f,_340,_341,_342,_343,_344,_345){return this.addKeyListener(_340,_341,_342,_343,_344,_345).concat(this.addMouseListener(_33f,_342,_343,_344,_345));}};}if(!dojo._hasResource["dijit._base.wai"]){dojo._hasResource["dijit._base.wai"]=true;dojo.provide("dijit._base.wai");dijit.waiNames=["waiRole","waiState"];dijit.wai={waiRole:{name:"waiRole","namespace":"http://www.w3.org/TR/xhtml2",alias:"x2",prefix:"wairole:"},waiState:{name:"waiState","namespace":"http://www.w3.org/2005/07/aaa",alias:"aaa",prefix:""},setAttr:function(node,ns,attr,_349){if(dojo.isIE){node.setAttribute(this[ns].alias+":"+attr,this[ns].prefix+_349);}else{node.setAttributeNS(this[ns]["namespace"],attr,this[ns].prefix+_349);}},getAttr:function(node,ns,attr){if(dojo.isIE){return node.getAttribute(this[ns].alias+":"+attr);}else{return node.getAttributeNS(this[ns]["namespace"],attr);}},removeAttr:function(node,ns,attr){var _350=true;if(dojo.isIE){_350=node.removeAttribute(this[ns].alias+":"+attr);}else{node.removeAttributeNS(this[ns]["namespace"],attr);}return _350;},onload:function(){var div=document.createElement("div");div.id="a11yTestNode";div.style.cssText="border: 1px solid;"+"border-color:red green;"+"position: absolute;"+"left: -999px;"+"top: -999px;"+"background-image: url(\""+dojo.moduleUrl("dijit","form/templates/blank.gif")+"\");";dojo.body().appendChild(div);function check(){var cs=dojo.getComputedStyle(div);if(cs){var _353=cs.backgroundImage;var _354=(cs.borderTopColor==cs.borderRightColor)||(_353!=null&&(_353=="none"||_353=="url(invalid-url:)"));dojo[_354?"addClass":"removeClass"](dojo.body(),"dijit_a11y");}};check();if(dojo.isIE){setInterval(check,4000);}}};if(dojo.isIE||dojo.isMoz){dojo._loaders.unshift(dijit.wai.onload);}}if(!dojo._hasResource["dijit._base"]){dojo._hasResource["dijit._base"]=true;dojo.provide("dijit._base");}if(!dojo._hasResource["dijit._Widget"]){dojo._hasResource["dijit._Widget"]=true;dojo.provide("dijit._Widget");dojo.declare("dijit._Widget",null,{constructor:function(_355,_356){this.create(_355,_356);},id:"",lang:"",dir:"",srcNodeRef:null,domNode:null,create:function(_357,_358){this.srcNodeRef=dojo.byId(_358);this._connects=[];this._attaches=[];if(this.srcNodeRef&&(typeof this.srcNodeRef.id=="string")){this.id=this.srcNodeRef.id;}if(_357){dojo.mixin(this,_357);}this.postMixInProperties();if(!this.id){this.id=dijit.getUniqueId(this.declaredClass.replace(/\./g,"_"));}dijit.registry.add(this);this.buildRendering();if(this.domNode){this.domNode.setAttribute("widgetId",this.id);if(this.srcNodeRef&&this.srcNodeRef.dir){this.domNode.dir=this.srcNodeRef.dir;}}this.postCreate();if(this.srcNodeRef&&!this.srcNodeRef.parentNode){delete this.srcNodeRef;}},postMixInProperties:function(){},buildRendering:function(){this.domNode=this.srcNodeRef;},postCreate:function(){},startup:function(){},destroyRecursive:function(_359){this.destroyDescendants();this.destroy();},destroy:function(_35a){this.uninitialize();dojo.forEach(this._connects,function(_35b){dojo.forEach(_35b,dojo.disconnect);});this.destroyRendering(_35a);dijit.registry.remove(this.id);},destroyRendering:function(_35c){if(this.bgIframe){this.bgIframe.destroy();delete this.bgIframe;}if(this.domNode){dojo._destroyElement(this.domNode);delete this.domNode;}if(this.srcNodeRef){dojo._destroyElement(this.srcNodeRef);delete this.srcNodeRef;}},destroyDescendants:function(){dojo.forEach(this.getDescendants(),function(_35d){_35d.destroy();});},uninitialize:function(){return false;},toString:function(){return "[Widget "+this.declaredClass+", "+(this.id||"NO ID")+"]";},getDescendants:function(){var list=dojo.query("[widgetId]",this.domNode);return list.map(dijit.byNode);},nodesWithKeyClick:["input","button"],connect:function(obj,_360,_361){var _362=[];if(_360=="ondijitclick"){var w=this;if(!this.nodesWithKeyClick[obj.nodeName]){_362.push(dojo.connect(obj,"onkeydown",this,function(e){if(e.keyCode==dojo.keys.ENTER){return (dojo.isString(_361))?w[_361](e):_361.call(w,e);}else{if(e.keyCode==dojo.keys.SPACE){dojo.stopEvent(e);}}}));_362.push(dojo.connect(obj,"onkeyup",this,function(e){if(e.keyCode==dojo.keys.SPACE){return dojo.isString(_361)?w[_361](e):_361.call(w,e);}}));}_360="onclick";}_362.push(dojo.connect(obj,_360,this,_361));this._connects.push(_362);return _362;},disconnect:function(_366){for(var i=0;i<this._connects.length;i++){if(this._connects[i]==_366){dojo.forEach(_366,dojo.disconnect);this._connects.splice(i,1);return;}}},isLeftToRight:function(){if(typeof this._ltr=="undefined"){this._ltr=(this.dir||dojo.getComputedStyle(this.domNode).direction)!="rtl";}return this._ltr;}});}if(!dojo._hasResource["dijit._Templated"]){dojo._hasResource["dijit._Templated"]=true;dojo.provide("dijit._Templated");dojo.declare("dijit._Templated",null,{templateNode:null,templateString:null,templatePath:null,widgetsInTemplate:false,containerNode:null,buildRendering:function(){var _368=dijit._Templated.getCachedTemplate(this.templatePath,this.templateString);var node;if(dojo.isString(_368)){var _36a=this.declaredClass,_36b=this;var tstr=dojo.string.substitute(_368,this,function(_36d,key){if(key.charAt(0)=="!"){_36d=_36b[key.substr(1)];}if(typeof _36d=="undefined"){throw new Error(_36a+" template:"+key);}return key.charAt(0)=="!"?_36d:_36d.toString().replace(/"/g,"&quot;");},this);node=dijit._Templated._createNodesFromText(tstr)[0];}else{node=_368.cloneNode(true);}this._attachTemplateNodes(node);if(this.srcNodeRef){dojo.style(this.styleNode||node,"cssText",this.srcNodeRef.style.cssText);if(this.srcNodeRef.className){node.className+=" "+this.srcNodeRef.className;}}this.domNode=node;if(this.srcNodeRef&&this.srcNodeRef.parentNode){this.srcNodeRef.parentNode.replaceChild(this.domNode,this.srcNodeRef);}if(this.widgetsInTemplate){var _36f=dojo.parser.parse(this.domNode);this._attachTemplateNodes(_36f,function(n,p){return n[p];});}this._fillContent(this.srcNodeRef);},_fillContent:function(_372){var dest=this.containerNode;if(_372&&dest){while(_372.hasChildNodes()){dest.appendChild(_372.firstChild);}}},_attachTemplateNodes:function(_374,_375){_375=_375||function(n,p){return n.getAttribute(p);};var _378=dojo.isArray(_374)?_374:(_374.all||_374.getElementsByTagName("*"));var x=dojo.isArray(_374)?0:-1;for(;x<_378.length;x++){var _37a=(x==-1)?_374:_378[x];if(this.widgetsInTemplate&&_375(_37a,"dojoType")){continue;}var _37b=_375(_37a,"dojoAttachPoint");if(_37b){var _37c,_37d=_37b.split(/\s*,\s*/);while(_37c=_37d.shift()){if(dojo.isArray(this[_37c])){this[_37c].push(_37a);}else{this[_37c]=_37a;}}}var _37e=_375(_37a,"dojoAttachEvent");if(_37e){var _37f,_380=_37e.split(/\s*,\s*/);var trim=dojo.trim;while(_37f=_380.shift()){if(_37f){var _382=null;if(_37f.indexOf(":")!=-1){var _383=_37f.split(":");_37f=trim(_383[0]);_382=trim(_383[1]);}else{_37f=trim(_37f);}if(!_382){_382=_37f;}this.connect(_37a,_37f,_382);}}}var name,_385=["waiRole","waiState"];while(name=_385.shift()){var wai=dijit.wai[name];var _387=_375(_37a,wai.name);if(_387){var role="role";var val;_387=_387.split(/\s*,\s*/);while(val=_387.shift()){if(val.indexOf("-")!=-1){var _38a=val.split("-");role=_38a[0];val=_38a[1];}dijit.wai.setAttr(_37a,wai.name,role,val);}}}}}});dijit._Templated._templateCache={};dijit._Templated.getCachedTemplate=function(_38b,_38c){var _38d=dijit._Templated._templateCache;var key=_38c||_38b;var _38f=_38d[key];if(_38f){return _38f;}if(!_38c){_38c=dijit._Templated._sanitizeTemplateString(dojo._getText(_38b));}_38c=dojo.string.trim(_38c);if(_38c.match(/\$\{([^\}]+)\}/g)){return (_38d[key]=_38c);}else{return (_38d[key]=dijit._Templated._createNodesFromText(_38c)[0]);}};dijit._Templated._sanitizeTemplateString=function(_390){if(_390){_390=_390.replace(/^\s*<\?xml(\s)+version=[\'\"](\d)*.(\d)*[\'\"](\s)*\?>/im,"");var _391=_390.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);if(_391){_390=_391[1];}}else{_390="";}return _390;};if(dojo.isIE){dojo.addOnUnload(function(){var _392=dijit._Templated._templateCache;for(var key in _392){var _394=_392[key];if(!isNaN(_394.nodeType)){dojo._destroyElement(_394);}_392[key]=null;}});}(function(){var _395={cell:{re:/^<t[dh][\s\r\n>]/i,pre:"<table><tbody><tr>",post:"</tr></tbody></table>"},row:{re:/^<tr[\s\r\n>]/i,pre:"<table><tbody>",post:"</tbody></table>"},section:{re:/^<(thead|tbody|tfoot)[\s\r\n>]/i,pre:"<table>",post:"</table>"}};var tn;dijit._Templated._createNodesFromText=function(text){if(!tn){tn=dojo.doc.createElement("div");tn.style.display="none";}var _398="none";var _399=text.replace(/^\s+/,"");for(var type in _395){var map=_395[type];if(map.re.test(_399)){_398=type;text=map.pre+text+map.post;break;}}tn.innerHTML=text;dojo.body().appendChild(tn);if(tn.normalize){tn.normalize();}var tag={cell:"tr",row:"tbody",section:"table"}[_398];var _39d=(typeof tag!="undefined")?tn.getElementsByTagName(tag)[0]:tn;var _39e=[];while(_39d.firstChild){_39e.push(_39d.removeChild(_39d.firstChild));}tn.innerHTML="";return _39e;};})();dojo.extend(dijit._Widget,{dojoAttachEvent:"",dojoAttachPoint:"",waiRole:"",waiState:""});}if(!dojo._hasResource["dijit._Container"]){dojo._hasResource["dijit._Container"]=true;dojo.provide("dijit._Container");dojo.declare("dijit._Contained",null,{getParent:function(){for(var p=this.domNode.parentNode;p;p=p.parentNode){var id=p.getAttribute&&p.getAttribute("widgetId");if(id){var _3a1=dijit.byId(id);return _3a1.isContainer?_3a1:null;}}return null;},_getSibling:function(_3a2){var node=this.domNode;do{node=node[_3a2+"Sibling"];}while(node&&node.nodeType!=1);if(!node){return null;}var id=node.getAttribute("widgetId");return dijit.byId(id);},getPreviousSibling:function(){return this._getSibling("previous");},getNextSibling:function(){return this._getSibling("next");}});dojo.declare("dijit._Container",null,{isContainer:true,addChild:function(_3a5,_3a6){if(typeof _3a6=="undefined"){_3a6="last";}dojo.place(_3a5.domNode,this.containerNode||this.domNode,_3a6);if(this._started&&!_3a5._started){_3a5.startup();}},removeChild:function(_3a7){var node=_3a7.domNode;node.parentNode.removeChild(node);},_nextElement:function(node){do{node=node.nextSibling;}while(node&&node.nodeType!=1);return node;},_firstElement:function(node){node=node.firstChild;if(node&&node.nodeType!=1){node=this._nextElement(node);}return node;},getChildren:function(){return dojo.query("> [widgetId]",this.containerNode||this.domNode).map(dijit.byNode);},hasChildren:function(){var cn=this.containerNode||this.domNode;return !!this._firstElement(cn);}});}if(!dojo._hasResource["dijit._tree.Controller"]){dojo._hasResource["dijit._tree.Controller"]=true;dojo.provide("dijit._tree.Controller");dojo.declare("dijit._tree.Controller",[dijit._Widget],{treeId:"",postMixInProperties:function(){if(this.store._features["dojo.data.api.Notification"]){dojo.connect(this.store,"onNew",this,"onNew");dojo.connect(this.store,"onDelete",this,"onDelete");dojo.connect(this.store,"onSet",this,"onSet");}dojo.subscribe(this.treeId,this,"_listener");},_listener:function(_3ac){var _3ad=_3ac.event;var _3ae="on"+_3ad.charAt(0).toUpperCase()+_3ad.substr(1);if(this[_3ae]){this[_3ae](_3ac);}},onBeforeTreeDestroy:function(_3af){dojo.unsubscribe(_3af.tree.id);},onExecute:function(_3b0){_3b0.node.tree.focusNode(_3b0.node);console.log("execute message for "+_3b0.node+": ",_3b0);},onNext:function(_3b1){var _3b2=this._navToNextNode(_3b1.node);if(_3b2&&_3b2.isTreeNode){_3b2.tree.focusNode(_3b2);return _3b2;}},onNew:function(item,_3b4){if(_3b4){var _3b5=this._itemNodeMap[this.store.getIdentity(_3b4.item)];}var _3b6={item:item};if(_3b5){if(!_3b5.isFolder){_3b5.makeFolder();}if(_3b5.state=="LOADED"||_3b5.isExpanded){var _3b7=_3b5.addChildren([_3b6]);}}else{var _3b7=this.tree.addChildren([_3b6]);}if(_3b7){dojo.mixin(this._itemNodeMap,_3b7);}},onDelete:function(_3b8){var _3b9=this.store.getIdentity(_3b8);var node=this._itemNodeMap[_3b9];if(node){parent=node.getParent();parent.deleteNode(node);this._itemNodeMap[_3b9]=null;}},onSet:function(_3bb){var _3bc=this.store.getIdentity(_3bb);var node=this._itemNodeMap[_3bc];node.setLabelNode(this.store.getLabel(_3bb));},_navToNextNode:function(node){var _3bf;if(node.isFolder&&node.isExpanded&&node.hasChildren()){_3bf=node.getChildren()[0];}else{while(node.isTreeNode){_3bf=node.getNextSibling();if(_3bf){break;}node=node.getParent();}}return _3bf;},onPrevious:function(_3c0){var _3c1=_3c0.node;var _3c2=_3c1;var _3c3=_3c1.getPreviousSibling();if(_3c3){_3c1=_3c3;while(_3c1.isFolder&&_3c1.isExpanded&&_3c1.hasChildren()){_3c2=_3c1;var _3c4=_3c1.getChildren();_3c1=_3c4[_3c4.length-1];}}else{_3c1=_3c1.getParent();}if(_3c1&&_3c1.isTreeNode){_3c2=_3c1;}if(_3c2&&_3c2.isTreeNode){_3c2.tree.focusNode(_3c2);return _3c2;}},onZoomIn:function(_3c5){var _3c6=_3c5.node;var _3c7=_3c6;if(_3c6.isFolder&&!_3c6.isExpanded){this._expand(_3c6);}else{if(_3c6.hasChildren()){_3c6=_3c6.getChildren()[0];}}if(_3c6&&_3c6.isTreeNode){_3c7=_3c6;}if(_3c7&&_3c7.isTreeNode){_3c7.tree.focusNode(_3c7);return _3c7;}},onZoomOut:function(_3c8){var node=_3c8.node;var _3ca=node;if(node.isFolder&&node.isExpanded){this._collapse(node);}else{node=node.getParent();}if(node&&node.isTreeNode){_3ca=node;}if(_3ca&&_3ca.isTreeNode){_3ca.tree.focusNode(_3ca);return _3ca;}},onFirst:function(_3cb){var _3cc=this._navToFirstNode(_3cb.tree);if(_3cc){_3cc.tree.focusNode(_3cc);return _3cc;}},_navToFirstNode:function(tree){var _3ce;if(tree){_3ce=tree.getChildren()[0];if(_3ce&&_3ce.isTreeNode){return _3ce;}}},onLast:function(_3cf){var _3d0=_3cf.node.tree;var _3d1=_3d0;while(_3d1.isExpanded){var c=_3d1.getChildren();_3d1=c[c.length-1];if(_3d1.isTreeNode){_3d0=_3d1;}}if(_3d0&&_3d0.isTreeNode){_3d0.tree.focusNode(_3d0);return _3d0;}},onToggleOpen:function(_3d3){var node=_3d3.node;if(node.isExpanded){this._collapse(node);}else{this._expand(node);}},onLetterKeyNav:function(_3d5){var node=startNode=_3d5.node;var tree=_3d5.tree;var key=_3d5.key;do{node=this._navToNextNode(node);if(!node){node=this._navToFirstNode(tree);}}while(node!==startNode&&(node.label.charAt(0).toLowerCase()!=key));if(node&&node.isTreeNode){if(node!==startNode){node.tree.focusNode(node);}return node;}},_expand:function(node){if(node.isFolder){node.expand();var t=node.tree;if(t.lastFocused){t.focusNode(t.lastFocused);}}},_collapse:function(node){if(node.isFolder){if(dojo.query("[tabindex=0]",node.domNode).length>0){node.tree.focusNode(node);}node.collapse();}}});dojo.declare("dijit._tree.DataController",dijit._tree.Controller,{onAfterTreeCreate:function(_3dc){var tree=this.tree=_3dc.tree;this._itemNodeMap={};var _3de=this;function onComplete(_3df){var _3e0=dojo.map(_3df,function(item){return {item:item,isFolder:_3de.store.hasAttribute(item,_3de.childrenAttr)};});_3de._itemNodeMap=tree.setChildren(_3e0);};this.store.fetch({query:this.query,onComplete:onComplete});},_expand:function(node){var _3e3=this.store;var _3e4=this.store.getValue;switch(node.state){case "LOADING":return;case "UNCHECKED":var _3e5=node.item;var _3e6=_3e3.getValues(_3e5,this.childrenAttr);var _3e7=0;dojo.forEach(_3e6,function(item){if(!_3e3.isItemLoaded(item)){_3e7++;}});if(_3e7==0){this._onLoadAllItems(node,_3e6);}else{node.markProcessing();var _3e9=this;function onItem(item){if(--_3e7==0){node.unmarkProcessing();_3e9._onLoadAllItems(node,_3e6);}};dojo.forEach(_3e6,function(item){if(!_3e3.isItemLoaded(item)){_3e3.loadItem({item:item,onItem:onItem});}});}break;default:dijit._tree.Controller.prototype._expand.apply(this,arguments);break;}},_onLoadAllItems:function(node,_3ed){var _3ee=dojo.map(_3ed,function(item){return {item:item,isFolder:this.store.hasAttribute(item,this.childrenAttr)};},this);dojo.mixin(this._itemNodeMap,node.setChildren(_3ee));dijit._tree.Controller.prototype._expand.apply(this,arguments);},_collapse:function(node){if(node.state=="LOADING"){return;}dijit._tree.Controller.prototype._collapse.apply(this,arguments);}});}if(!dojo._hasResource["dijit.Tree"]){dojo._hasResource["dijit.Tree"]=true;dojo.provide("dijit.Tree");dojo.declare("dijit._TreeBase",[dijit._Widget,dijit._Templated,dijit._Container,dijit._Contained],{state:"UNCHECKED",locked:false,lock:function(){this.locked=true;},unlock:function(){if(!this.locked){throw new Error(this.declaredClass+" unlock: not locked");}this.locked=false;},isLocked:function(){var node=this;while(true){if(node.lockLevel){return true;}if(!node.getParent()||node.isTree){break;}node=node.getParent();}return false;},setChildren:function(_3f2){this.destroyDescendants();this.state="LOADED";var _3f3={};if(_3f2&&_3f2.length>0){this.isFolder=true;if(!this.containerNode){this.containerNode=this.tree.containerNodeTemplate.cloneNode(true);this.domNode.appendChild(this.containerNode);}dojo.forEach(_3f2,function(_3f4){var _3f5=new dijit._TreeNode(dojo.mixin({tree:this.tree,label:this.tree.store.getLabel(_3f4.item)},_3f4));this.addChild(_3f5);_3f3[this.tree.store.getIdentity(_3f4.item)]=_3f5;},this);dojo.forEach(this.getChildren(),function(_3f6,idx){_3f6._updateLayout();});}else{this.isFolder=false;}if(this.isTree){var fc=this.getChildren()[0];var _3f9=fc?fc.labelNode:this.domNode;_3f9.setAttribute("tabIndex","0");}return _3f3;},addChildren:function(_3fa){var _3fb={};if(_3fa&&_3fa.length>0){dojo.forEach(_3fa,function(_3fc){var _3fd=new dijit._TreeNode(dojo.mixin({tree:this.tree,label:this.tree.store.getLabel(_3fc.item)},_3fc));this.addChild(_3fd);_3fb[this.tree.store.getIdentity(_3fc.item)]=_3fd;},this);dojo.forEach(this.getChildren(),function(_3fe,idx){_3fe._updateLayout();});}return _3fb;},deleteNode:function(node){node.destroy();dojo.forEach(this.getChildren(),function(_401,idx){_401._updateLayout();});},makeFolder:function(){this.isFolder=true;this._setExpando(false);}});dojo.declare("dijit.Tree",dijit._TreeBase,{store:null,query:null,childrenAttr:"children",templateString:"<div class=\"dijitTreeContainer\" style=\"\" waiRole=\"tree\"\n\tdojoAttachEvent=\"onclick:_onClick,onkeypress:_onKeyPress\"\n></div>\n",isExpanded:true,isTree:true,_publish:function(_403,_404){dojo.publish(this.id,[dojo.mixin({tree:this,event:_403},_404||{})]);},postMixInProperties:function(){this.tree=this;var _405={};_405[dojo.keys.ENTER]="execute";_405[dojo.keys.LEFT_ARROW]="zoomOut";_405[dojo.keys.RIGHT_ARROW]="zoomIn";_405[dojo.keys.UP_ARROW]="previous";_405[dojo.keys.DOWN_ARROW]="next";_405[dojo.keys.HOME]="first";_405[dojo.keys.END]="last";this._keyTopicMap=_405;},postCreate:function(){this.containerNode=this.domNode;var div=document.createElement("div");div.style.display="none";div.className="dijitTreeContainer";dijit.wai.setAttr(div,"waiRole","role","presentation");this.containerNodeTemplate=div;this._controller=new dijit._tree.DataController({store:this.store,treeId:this.id,query:this.query,childrenAttr:this.childrenAttr});this._publish("afterTreeCreate");},destroy:function(){this._publish("beforeTreeDestroy");return dijit._Widget.prototype.destroy.apply(this,arguments);},toString:function(){return "["+this.declaredClass+" ID:"+this.id+"]";},getIconClass:function(item){},_domElement2TreeNode:function(_408){var ret;do{ret=dijit.byNode(_408);}while(!ret&&(_408=_408.parentNode));return ret;},_onClick:function(e){var _40b=e.target;var _40c=this._domElement2TreeNode(_40b);if(!_40c||!_40c.isTreeNode){return;}if(_40b==_40c.expandoNode||_40b==_40c.expandoNodeText){if(_40c.isFolder){this._publish("toggleOpen",{node:_40c});}}else{this._publish("execute",{item:_40c.item,node:_40c});this.onClick(_40c.item,_40c);}dojo.stopEvent(e);},onClick:function(item){console.log("default onclick handler",item);},_onKeyPress:function(e){if(e.altKey){return;}var _40f=this._domElement2TreeNode(e.target);if(!_40f){return;}if(e.charCode){var _410=e.charCode;if(!e.altKey&&!e.ctrlKey&&!e.shiftKey&&!e.metaKey){_410=(String.fromCharCode(_410)).toLowerCase();this._publish("letterKeyNav",{node:_40f,key:_410});dojo.stopEvent(e);}}else{if(this._keyTopicMap[e.keyCode]){this._publish(this._keyTopicMap[e.keyCode],{node:_40f,item:_40f.item});dojo.stopEvent(e);}}},blurNode:function(){var node=this.lastFocused;if(!node){return;}var _412=node.labelNode;dojo.removeClass(_412,"dijitTreeLabelFocused");_412.setAttribute("tabIndex","-1");this.lastFocused=null;},focusNode:function(node){this.blurNode();var _414=node.labelNode;_414.setAttribute("tabIndex","0");this.lastFocused=node;dojo.addClass(_414,"dijitTreeLabelFocused");_414.focus();},_onBlur:function(){if(this.lastFocused){var _415=this.lastFocused.labelNode;dojo.removeClass(_415,"dijitTreeLabelFocused");}},_onFocus:function(){if(this.lastFocused){var _416=this.lastFocused.labelNode;dojo.addClass(_416,"dijitTreeLabelFocused");}}});dojo.declare("dijit._TreeNode",dijit._TreeBase,{templateString:"<div class=\"dijitTreeNode dijitTreeExpandLeaf dijitTreeChildrenNo\" waiRole=\"presentation\"\n\t><span dojoAttachPoint=\"expandoNode\" class=\"dijitTreeExpando\" waiRole=\"presentation\"\n\t></span\n\t><span dojoAttachPoint=\"expandoNodeText\" class=\"dijitExpandoText\" waiRole=\"presentation\"\n\t></span\n\t>\n\t<div dojoAttachPoint=\"contentNode\" class=\"dijitTreeContent\" waiRole=\"presentation\">\n\t\t<div dojoAttachPoint=\"iconNode\" class=\"dijitInline dijitTreeIcon\" waiRole=\"presentation\"></div>\n\t\t<span dojoAttachPoint=labelNode class=\"dijitTreeLabel\" wairole=\"treeitem\" expanded=\"true\" tabindex=\"-1\"></span>\n\t</div>\n</div>\n",item:null,isTreeNode:true,label:"",isFolder:null,isExpanded:false,postCreate:function(){this.labelNode.innerHTML="";this.labelNode.appendChild(document.createTextNode(this.label));this._setExpando();dojo.addClass(this.iconNode,this.tree.getIconClass(this.item));},markProcessing:function(){this.state="LOADING";this._setExpando(true);},unmarkProcessing:function(){this._setExpando(false);},_updateLayout:function(){dojo.removeClass(this.domNode,"dijitTreeIsRoot");if(this.getParent()["isTree"]){dojo.addClass(this.domNode,"dijitTreeIsRoot");}dojo.removeClass(this.domNode,"dijitTreeIsLast");if(!this.getNextSibling()){dojo.addClass(this.domNode,"dijitTreeIsLast");}},_setExpando:function(_417){var _418=["dijitTreeExpandoLoading","dijitTreeExpandoOpened","dijitTreeExpandoClosed","dijitTreeExpandoLeaf"];var idx=_417?0:(this.isFolder?(this.isExpanded?1:2):3);dojo.forEach(_418,function(s){dojo.removeClass(this.expandoNode,s);},this);dojo.addClass(this.expandoNode,_418[idx]);this.expandoNodeText.innerHTML=_417?"*":(this.isFolder?(this.isExpanded?"-":"+"):"*");},setChildren:function(_41b){var ret=dijit.Tree.superclass.setChildren.apply(this,arguments);this._wipeIn=dojo.fx.wipeIn({node:this.containerNode,duration:250});dojo.connect(this.wipeIn,"onEnd",dojo.hitch(this,"_afterExpand"));this._wipeOut=dojo.fx.wipeOut({node:this.containerNode,duration:250});dojo.connect(this.wipeOut,"onEnd",dojo.hitch(this,"_afterCollapse"));return ret;},expand:function(){if(this.isExpanded){return;}if(this._wipeOut.status()=="playing"){this._wipeOut.stop();}this.isExpanded=true;dijit.wai.setAttr(this.labelNode,"waiState","expanded","true");dijit.wai.setAttr(this.containerNode,"waiRole","role","group");this._setExpando();this._wipeIn.play();},_afterExpand:function(){this.onShow();this._publish("afterExpand",{node:this});},collapse:function(){if(!this.isExpanded){return;}if(this._wipeIn.status()=="playing"){this._wipeIn.stop();}this.isExpanded=false;dijit.wai.setAttr(this.labelNode,"waiState","expanded","false");this._setExpando();this._wipeOut.play();},_afterCollapse:function(){this.onHide();this._publish("afterCollapse",{node:this});},setLabelNode:function(_41d){this.labelNode.innerHTML="";this.labelNode.appendChild(document.createTextNode(_41d));},toString:function(){return "["+this.declaredClass+", "+this.label+"]";}});}if(!dojo._hasResource["dijit.layout.ContentPane"]){dojo._hasResource["dijit.layout.ContentPane"]=true;dojo.provide("dijit.layout.ContentPane");dojo.declare("dijit.layout.ContentPane",dijit._Widget,{href:"",extractContent:false,parseOnLoad:true,preventCache:false,preload:false,refreshOnShow:false,loadingMessage:"<span class='dijitContentPaneLoading'>${loadingState}</span>",errorMessage:"<span class='dijitContentPaneError'>${errorState}</span>",isLoaded:false,"class":"dijitContentPane",postCreate:function(){this.domNode.title="";if(this.preload){this._loadCheck();}var _41e=dojo.i18n.getLocalization("dijit","loading",this.lang);this.loadingMessage=dojo.string.substitute(this.loadingMessage,_41e);this.errorMessage=dojo.string.substitute(this.errorMessage,_41e);dojo.addClass(this.domNode,this["class"]);},startup:function(){if(!this._started){this._loadCheck();this._started=true;}},refresh:function(){return this._prepareLoad(true);},setHref:function(href){this.href=href;return this._prepareLoad();},setContent:function(data){if(!this._isDownloaded){this.href="";this._onUnloadHandler();}this._setContent(data||"");this._isDownloaded=false;if(this.parseOnLoad){this._createSubWidgets();}this._onLoadHandler();},cancel:function(){if(this._xhrDfd&&(this._xhrDfd.fired==-1)){this._xhrDfd.cancel();}delete this._xhrDfd;},destroy:function(){if(this._beingDestroyed){return;}this._onUnloadHandler();this._beingDestroyed=true;dijit.layout.ContentPane.superclass.destroy.call(this);},resize:function(size){dojo.marginBox(this.domNode,size);},_prepareLoad:function(_422){this.cancel();this.isLoaded=false;this._loadCheck(_422);},_loadCheck:function(_423){var _424=((this.open!==false)&&(this.domNode.style.display!="none"));if(this.href&&(_423||(this.preload&&!this._xhrDfd)||(this.refreshOnShow&&_424&&!this._xhrDfd)||(!this.isLoaded&&_424&&!this._xhrDfd))){this._downloadExternalContent();}},_downloadExternalContent:function(){this._onUnloadHandler();this._setContent(this.onDownloadStart.call(this));var self=this;var _426={preventCache:(this.preventCache||this.refreshOnShow),url:this.href,handleAs:"text"};if(dojo.isObject(this.ioArgs)){dojo.mixin(_426,this.ioArgs);}var hand=this._xhrDfd=(this.ioMethod||dojo.xhrGet)(_426);hand.addCallback(function(html){try{self.onDownloadEnd.call(self);self._isDownloaded=true;self.setContent.call(self,html);}catch(err){self._onError.call(self,"Content",err);}delete self._xhrDfd;return html;});hand.addErrback(function(err){if(!hand.cancelled){self._onError.call(self,"Download",err);}delete self._xhrDfd;return err;});},_onLoadHandler:function(){this.isLoaded=true;try{this.onLoad.call(this);}catch(e){console.error("Error "+this.widgetId+" running custom onLoad code");}},_onUnloadHandler:function(){this.isLoaded=false;this.cancel();try{this.onUnload.call(this);}catch(e){console.error("Error "+this.widgetId+" running custom onUnload code");}},_setContent:function(cont){this.destroyDescendants();try{var node=this.containerNode||this.domNode;while(node.firstChild){dojo._destroyElement(node.firstChild);}if(typeof cont=="string"){if(this.extractContent){match=cont.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);if(match){cont=match[1];}}node.innerHTML=cont;}else{if(cont.nodeType){node.appendChild(cont);}else{dojo.forEach(cont,function(n){node.appendChild(n.cloneNode(true));});}}}catch(e){var _42d=this.onContentError(e);try{node.innerHTML=_42d;}catch(e){console.error("Fatal "+this.id+" could not change content due to "+e.message,e);}}},_onError:function(type,err,_430){var _431=this["on"+type+"Error"].call(this,err);if(_430){console.error(_430,err);}else{if(_431){this._setContent.call(this,_431);}}},_createSubWidgets:function(){var _432=this.containerNode||this.domNode;try{dojo.parser.parse(_432,true);}catch(e){this._onError("Content",e,"Couldn't create widgets in "+this.id+(this.href?" from "+this.href:""));}},onLoad:function(e){},onUnload:function(e){},onDownloadStart:function(){return this.loadingMessage;},onContentError:function(_435){},onDownloadError:function(_436){return this.errorMessage;},onDownloadEnd:function(){}});}if(!dojo._hasResource["dijit.form.Form"]){dojo._hasResource["dijit.form.Form"]=true;dojo.provide("dijit.form.Form");dojo.declare("dijit.form._FormMixin",null,{execute:function(_437){},onCancel:function(){},onExecute:function(){},templateString:"<form dojoAttachPoint='containerNode' dojoAttachEvent='onsubmit:_onSubmit' enctype='multipart/form-data'></form>",_onSubmit:function(e){dojo.stopEvent(e);this.onExecute();this.execute(this.getValues());},submit:function(){this.containerNode.submit();},setValues:function(obj){var map={};dojo.forEach(this.getDescendants(),function(_43b){if(!_43b.name){return;}var _43c=map[_43b.name]||(map[_43b.name]=[]);_43c.push(_43b);});for(var name in map){var _43e=map[name],_43f=dojo.getObject(name,false,obj);if(!dojo.isArray(_43f)){_43f=[_43f];}if(_43e[0].setChecked){dojo.forEach(_43e,function(w,i){w.setChecked(dojo.indexOf(_43f,w.value)!=-1);});}else{dojo.forEach(_43e,function(w,i){w.setValue(_43f[i]);});}}},getValues:function(){var obj={};dojo.forEach(this.getDescendants(),function(_445){var _446=_445.getValue?_445.getValue():_445.value;var name=_445.name;if(!name){return;}if(_445.setChecked){if(/Radio/.test(_445.declaredClass)){if(_445.checked){dojo.setObject(name,_446,obj);}}else{var ary=dojo.getObject(name,false,obj);if(!ary){ary=[];dojo.setObject(name,ary,obj);}if(_445.checked){ary.push(_446);}}}else{dojo.setObject(name,_446,obj);}});return obj;},isValid:function(){return dojo.every(this.getDescendants(),function(_449){return !_449.isValid||_449.isValid();});}});dojo.declare("dijit.form.Form",[dijit._Widget,dijit._Templated,dijit.form._FormMixin],null);}if(!dojo._hasResource["dijit.Dialog"]){dojo._hasResource["dijit.Dialog"]=true;dojo.provide("dijit.Dialog");dojo.declare("dijit.DialogUnderlay",[dijit._Widget,dijit._Templated],{templateString:"<div class=dijitDialogUnderlayWrapper id='${id}_underlay'><div class=dijitDialogUnderlay dojoAttachPoint='node'></div></div>",postCreate:function(){dojo.body().appendChild(this.domNode);this.bgIframe=new dijit.BackgroundIframe(this.domNode);},layout:function(){var _44a=dijit.getViewport();var is=this.node.style,os=this.domNode.style;os.top=_44a.t+"px";os.left=_44a.l+"px";is.width=_44a.w+"px";is.height=_44a.h+"px";var _44d=dijit.getViewport();if(_44a.w!=_44d.w){is.width=_44d.w+"px";}if(_44a.h!=_44d.h){is.height=_44d.h+"px";}},show:function(){this.domNode.style.display="block";this.layout();if(this.bgIframe.iframe){this.bgIframe.iframe.style.display="block";}this._resizeHandler=this.connect(window,"onresize","layout");},hide:function(){this.domNode.style.display="none";this.domNode.style.width=this.domNode.style.height="1px";if(this.bgIframe.iframe){this.bgIframe.iframe.style.display="none";}this.disconnect(this._resizeHandler);},uninitialize:function(){if(this.bgIframe){this.bgIframe.destroy();}}});dojo.declare("dijit.Dialog",[dijit.layout.ContentPane,dijit._Templated,dijit.form._FormMixin],{templateString:null,templateString:"<div class=\"dijitDialog\">\n\t\t<div dojoAttachPoint=\"titleBar\" class=\"dijitDialogTitleBar\" tabindex=\"0\" waiRole=\"dialog\" title=\"${title}\">\n\t\t<span dojoAttachPoint=\"titleNode\" class=\"dijitDialogTitle\">${title}</span>\n\t\t<span dojoAttachPoint=\"closeButtonNode\" class=\"dijitDialogCloseIcon\" dojoAttachEvent=\"onclick: hide\">\n\t\t\t<span dojoAttachPoint=\"closeText\" class=\"closeText\">x</span>\n\t\t</span>\n\t</div>\n\t\t<div dojoAttachPoint=\"containerNode\" class=\"dijitDialogPaneContent\"></div>\n\t<span dojoAttachPoint=\"tabEnd\" dojoAttachEvent=\"onfocus:_cycleFocus\" tabindex=\"0\"></span>\n</div>\n",title:"",duration:400,_lastFocusItem:null,postCreate:function(){dojo.body().appendChild(this.domNode);dijit.Dialog.superclass.postCreate.apply(this,arguments);this.domNode.style.display="none";this.connect(this,"onExecute","hide");this.connect(this,"onCancel","hide");},onLoad:function(){this._position();dijit.Dialog.superclass.onLoad.call(this);},_setup:function(){this._modalconnects=[];if(this.titleBar){this._moveable=new dojo.dnd.Moveable(this.domNode,{handle:this.titleBar});}this._underlay=new dijit.DialogUnderlay();var node=this.domNode;this._fadeIn=dojo.fx.combine([dojo.fadeIn({node:node,duration:this.duration}),dojo.fadeIn({node:this._underlay.domNode,duration:this.duration,onBegin:dojo.hitch(this._underlay,"show")})]);this._fadeOut=dojo.fx.combine([dojo.fadeOut({node:node,duration:this.duration,onEnd:function(){node.style.display="none";}}),dojo.fadeOut({node:this._underlay.domNode,duration:this.duration,onEnd:dojo.hitch(this._underlay,"hide")})]);},uninitialize:function(){if(this._underlay){this._underlay.destroy();}},_position:function(){var _44f=dijit.getViewport();var mb=dojo.marginBox(this.domNode);var _451=this.domNode.style;_451.left=(_44f.l+(_44f.w-mb.w)/2)+"px";_451.top=(_44f.t+(_44f.h-mb.h)/2)+"px";},_findLastFocus:function(evt){this._lastFocused=evt.target;},_cycleFocus:function(evt){if(!this._lastFocusItem){this._lastFocusItem=this._lastFocused;}this.titleBar.focus();},_onKey:function(evt){if(evt.keyCode){var node=evt.target;if(node==this.titleBar&&evt.shiftKey&&evt.keyCode==dojo.keys.TAB){if(this._lastFocusItem){this._lastFocusItem.focus();}dojo.stopEvent(evt);}else{while(node){if(node==this.domNode){if(evt.keyCode==dojo.keys.ESCAPE){this.hide();}else{return;}}node=node.parentNode;}if(evt.keyCode!=dojo.keys.TAB){dojo.stopEvent(evt);}else{if(!dojo.isOpera){try{this.titleBar.focus();}catch(e){}}}}}},show:function(){if(!this._alreadyInitialized){this._setup();this._alreadyInitialized=true;}if(this._fadeOut.status()=="playing"){this._fadeOut.stop();}this._modalconnects.push(dojo.connect(window,"onscroll",this,"layout"));this._modalconnects.push(dojo.connect(document.documentElement,"onkeypress",this,"_onKey"));var ev=typeof (document.ondeactivate)=="object"?"ondeactivate":"onblur";this._modalconnects.push(dojo.connect(this.containerNode,ev,this,"_findLastFocus"));dojo.style(this.domNode,"opacity",0);this.domNode.style.display="block";this._loadCheck();this._position();this._fadeIn.play();this._savedFocus=dijit.getFocus(this);setTimeout(dojo.hitch(this,function(){dijit.focus(this.titleBar);}),50);},hide:function(){if(!this._alreadyInitialized){return;}if(this._fadeIn.status()=="playing"){this._fadeIn.stop();}this._fadeOut.play();if(this._scrollConnected){this._scrollConnected=false;}dojo.forEach(this._modalconnects,dojo.disconnect);this._modalconnects=[];dijit.focus(this._savedFocus);},layout:function(){if(this.domNode.style.display=="block"){this._underlay.layout();this._position();}}});dojo.declare("dijit.TooltipDialog",[dijit.layout.ContentPane,dijit._Templated,dijit.form._FormMixin],{title:"",_lastFocusItem:null,templateString:null,templateString:"<div id=\"${id}\" class=\"dijitTooltipDialog\" >\n\t<div class=\"dijitTooltipContainer\">\n\t\t<div  class =\"dijitTooltipContents dijitTooltipFocusNode\" dojoAttachPoint=\"containerNode\" tabindex=\"0\" waiRole=\"dialog\"></div>\n\t</div>\n\t<span dojoAttachPoint=\"tabEnd\" tabindex=\"0\" dojoAttachEvent=\"focus:_cycleFocus\"></span>\n\t<div class=\"dijitTooltipConnector\" ></div>\n</div>\n",postCreate:function(){dijit.TooltipDialog.superclass.postCreate.apply(this,arguments);this.connect(this.containerNode,"onkeypress","_onKey");var ev=typeof (document.ondeactivate)=="object"?"ondeactivate":"onblur";this.connect(this.containerNode,ev,"_findLastFocus");this.containerNode.title=this.title;},orient:function(_458){this.domNode.className="dijitTooltipDialog "+" dijitTooltipAB"+(_458.charAt(1)=="L"?"Left":"Right")+" dijitTooltip"+(_458.charAt(0)=="T"?"Below":"Above");},onOpen:function(pos){this.orient(pos.corner);this._loadCheck();this.containerNode.focus();},_onKey:function(evt){if(evt.keyCode==dojo.keys.ESCAPE){this.onCancel();}else{if(evt.target==this.containerNode&&evt.shiftKey&&evt.keyCode==dojo.keys.TAB){if(this._lastFocusItem){this._lastFocusItem.focus();}dojo.stopEvent(evt);}}},_findLastFocus:function(evt){this._lastFocused=evt.target;},_cycleFocus:function(evt){if(!this._lastFocusItem){this._lastFocusItem=this._lastFocused;}this.containerNode.focus();}});}if(!dojo._hasResource["dijit.form._FormWidget"]){dojo._hasResource["dijit.form._FormWidget"]=true;dojo.provide("dijit.form._FormWidget");dojo.declare("dijit.form._FormWidget",[dijit._Widget,dijit._Templated],{baseClass:"",value:"",name:"",id:"",alt:"",type:"text",tabIndex:"0",disabled:false,intermediateChanges:false,setDisabled:function(_45d){this.domNode.disabled=this.disabled=_45d;if(this.focusNode){this.focusNode.disabled=_45d;}if(_45d){this._hovering=false;this._active=false;}dijit.wai.setAttr(this.focusNode||this.domNode,"waiState","disabled",_45d);this._setStateClass();},_onMouse:function(_45e){var _45f=_45e.target;if(!this.disabled){switch(_45e.type){case "mouseover":this._hovering=true;var _460,node=_45f;while(node.nodeType===1&&!(_460=node.getAttribute("baseClass"))&&node!=this.domNode){node=node.parentNode;}this.baseClass=_460||"dijit"+this.declaredClass.replace(/.*\./g,"");break;case "mouseout":this._hovering=false;this.baseClass=null;break;case "mousedown":this._active=true;var self=this;var _463=this.connect(dojo.body(),"onmouseup",function(){self._active=false;self._setStateClass();self.disconnect(_463);});break;}this._setStateClass();}},focus:function(){dijit.focus(this.focusNode);},_setStateClass:function(base){var _465=(this.styleNode||this.domNode).className;var base=this.baseClass||this.domNode.getAttribute("baseClass")||"dijitFormWidget";_465=_465.replace(new RegExp("\\b"+base+"(Checked)?(Selected)?(Disabled|Active|Focused|Hover)?\\b\\s*","g"),"");var _466=[base];function multiply(_467){_466=_466.concat(dojo.map(_466,function(c){return c+_467;}));};if(this.checked){multiply("Checked");}if(this.selected){multiply("Selected");}if(this.disabled){multiply("Disabled");}else{if(this._active){multiply("Active");}else{if(this._focused){multiply("Focused");}else{if(this._hovering){multiply("Hover");}}}}(this.styleNode||this.domNode).className=_465+" "+_466.join(" ");},onChange:function(_469){},postCreate:function(){this.setValue(this.value,true);this.setDisabled(this.disabled);this._setStateClass();},setValue:function(_46a,_46b){this._lastValue=_46a;dijit.wai.setAttr(this.focusNode||this.domNode,"waiState","valuenow",this.forWaiValuenow());if((this.intermediateChanges||_46b)&&_46a!=this._lastValueReported){this._lastValueReported=_46a;this.onChange(_46a);}},getValue:function(){return this._lastValue;},undo:function(){this.setValue(this._lastValueReported,false);},_onKeyPress:function(e){if(e.keyCode==dojo.keys.ESCAPE&&!e.shiftKey&&!e.ctrlKey&&!e.altKey){var v=this.getValue();var lv=this._lastValueReported;if(lv!=undefined&&v.toString()!=lv.toString()){this.undo();dojo.stopEvent(e);return false;}}return true;},forWaiValuenow:function(){return this.getValue();}});}if(!dojo._hasResource["dijit.form.Button"]){dojo._hasResource["dijit.form.Button"]=true;dojo.provide("dijit.form.Button");dojo.declare("dijit.form.Button",dijit.form._FormWidget,{label:"",showLabel:true,iconClass:"",type:"button",baseClass:"dijitButton",templateString:"<div class=\"dijit dijitLeft dijitInline dijitButton\" baseClass=\"${baseClass}\"\n\tdojoAttachEvent=\"onclick:_onButtonClick,onmouseover:_onMouse,onmouseout:_onMouse,onmousedown:_onMouse\"\n\t><div class='dijitRight'\n\t><button class=\"dijitStretch dijitButtonNode dijitButtonContents\" dojoAttachPoint=\"focusNode,titleNode\"\n\t\ttabIndex=\"${tabIndex}\" type=\"${type}\" id=\"${id}\" name=\"${name}\" waiRole=\"button\" waiState=\"labelledby-${id}_label\"\n\t\t><div class=\"dijitInline ${iconClass}\"></div\n\t\t><span class=\"dijitButtonText\" id=\"${id}_label\" dojoAttachPoint=\"containerNode\">${label}</span\n\t></button\n></div></div>\n",_onButtonClick:function(e){dojo.stopEvent(e);if(this.disabled){return;}return this.onClick(e);},postCreate:function(){if(this.showLabel==false){var _470="";this.label=this.containerNode.innerHTML;_470=dojo.trim(this.containerNode.innerText||this.containerNode.textContent);this.titleNode.title=_470;dojo.addClass(this.containerNode,"dijitDisplayNone");}dijit.form._FormWidget.prototype.postCreate.apply(this,arguments);},onClick:function(e){if(this.type=="submit"){for(var node=this.domNode;node;node=node.parentNode){var _473=dijit.byNode(node);if(_473&&_473._onSubmit){_473._onSubmit(e);break;}if(node.tagName.toLowerCase()=="form"){node.submit();break;}}}},setLabel:function(_474){this.containerNode.innerHTML=this.label=_474;if(dojo.isMozilla){var _475=dojo.getComputedStyle(this.domNode).display;this.domNode.style.display="none";var _476=this;setTimeout(function(){_476.domNode.style.display=_475;},1);}if(this.showLabel==false){this.titleNode.title=dojo.trim(this.containerNode.innerText||this.containerNode.textContent);}}});dojo.declare("dijit.form.DropDownButton",[dijit.form.Button,dijit._Container],{baseClass:"dijitDropDownButton",templateString:"<div class=\"dijit dijitLeft dijitInline dijitDropDownButton\" baseClass=\"dijitDropDownButton\"\n\tdojoAttachEvent=\"onmouseover:_onMouse,onmouseout:_onMouse,onmousedown:_onMouse,onclick:_onArrowClick,onkeypress:_onKey\"\n\t><div class='dijitRight'>\n\t<button tabIndex=\"${tabIndex}\" class=\"dijitStretch dijitButtonNode dijitButtonContents\" type=\"${type}\" id=\"${id}\" name=\"${name}\"\n\t\tdojoAttachPoint=\"focusNode,titleNode\" waiRole=\"button\" waiState=\"haspopup-true,labelledby-${id}_label\"\n\t\t><div class=\"dijitInline ${iconClass}\"></div\n\t\t><span class=\"dijitButtonText\" \tdojoAttachPoint=\"containerNode,popupStateNode\"\n\t\tid=\"${id}_label\">${label}</span\n\t\t><span class='dijitA11yDownArrow'>&#9660;</span>\n\t</button>\n</div></div>\n",_fillContent:function(){if(this.srcNodeRef){var _477=dojo.query("*",this.srcNodeRef);dijit.form.DropDownButton.superclass._fillContent.call(this,_477[0]);this.dropDownContainer=this.srcNodeRef;}},startup:function(){if(!this.dropDown){var _478=dojo.query("[widgetId]",this.dropDownContainer)[0];this.dropDown=dijit.byNode(_478);delete this.dropDownContainer;}dojo.body().appendChild(this.dropDown.domNode);this.dropDown.domNode.style.display="none";},_onArrowClick:function(e){if(this.disabled){return;}this._toggleDropDown();},_onKey:function(e){if(this.disabled){return;}if(e.keyCode==dojo.keys.DOWN_ARROW){if(!this.dropDown||this.dropDown.domNode.style.display=="none"){dojo.stopEvent(e);return this._toggleDropDown();}}},_onBlur:function(){dijit.popup.closeAll();},_toggleDropDown:function(){if(this.disabled){return;}dijit.focus(this.popupStateNode);var _47b=this.dropDown;if(!_47b){return false;}if(!_47b.isShowingNow){if(_47b.href&&!_47b.isLoaded){var self=this;var _47d=dojo.connect(_47b,"onLoad",function(){dojo.disconnect(_47d);self._openDropDown();});_47b._loadCheck(true);return;}else{this._openDropDown();}}else{dijit.popup.closeAll();this._opened=false;}},_openDropDown:function(){var _47e=this.dropDown;var _47f=_47e.domNode.style.width;var self=this;dijit.popup.open({parent:this,popup:_47e,around:this.domNode,orient:this.isLeftToRight()?{"BL":"TL","BR":"TR","TL":"BL","TR":"BR"}:{"BR":"TR","BL":"TL","TR":"BR","TL":"BL"},onExecute:function(){dijit.popup.closeAll();self.focus();},onCancel:function(){dijit.popup.closeAll();self.focus();},onClose:function(){_47e.domNode.style.width=_47f;self.popupStateNode.removeAttribute("popupActive");}});if(this.domNode.offsetWidth>_47e.domNode.offsetWidth){var _481=null;if(!this.isLeftToRight()){_481=_47e.domNode.parentNode;var _482=_481.offsetLeft+_481.offsetWidth;}dojo.marginBox(_47e.domNode,{w:this.domNode.offsetWidth});if(_481){_481.style.left=_482-this.domNode.offsetWidth+"px";}}this.popupStateNode.setAttribute("popupActive","true");this._opened=true;if(_47e.focus){_47e.focus();}}});dojo.declare("dijit.form.ComboButton",dijit.form.DropDownButton,{templateString:"<table class='dijit dijitReset dijitInline dijitLeft dijitComboButton'  baseClass='dijitComboButton'\n\tid=\"${id}\" name=\"${name}\" cellspacing='0' cellpadding='0'\n\tdojoAttachEvent=\"onmouseover:_onMouse,onmouseout:_onMouse,onmousedown:_onMouse\">\n\t<tr>\n\t\t<td\tclass=\"dijitStretch dijitButtonContents dijitButtonNode\"\n\t\t\ttabIndex=\"${tabIndex}\"\n\t\t\tdojoAttachEvent=\"ondijitclick:_onButtonClick\"  dojoAttachPoint=\"titleNode\"\n\t\t\twaiRole=\"button\" waiState=\"labelledby-${id}_label\">\n\t\t\t<div class=\"dijitInline ${iconClass}\"></div>\n\t\t\t<span class=\"dijitButtonText\" id=\"${id}_label\" dojoAttachPoint=\"containerNode\">${label}</span>\n\t\t</td>\n\t\t<td class='dijitReset dijitRight dijitButtonNode dijitDownArrowButton'\n\t\t\tdojoAttachPoint=\"popupStateNode,focusNode\"\n\t\t\tdojoAttachEvent=\"onmouseover:_onMouse,onmouseout:_onMouse,onmousedown:_onMouse,ondijitclick:_onArrowClick, onkeypress:_onKey\"\n\t\t\tbaseClass=\"dijitComboButtonDownArrow\"\n\t\t\ttitle=\"${optionsTitle}\"\n\t\t\ttabIndex=\"${tabIndex}\"\n\t\t\twaiRole=\"button\" waiState=\"haspopup-true\"\n\t\t><div waiRole=\"presentation\">&#9660;</div>\n\t</td></tr>\n</table>\n",optionsTitle:"",baseClass:"dijitComboButton"});dojo.declare("dijit.form.ToggleButton",dijit.form.Button,{baseClass:"dijitToggleButton",checked:false,onClick:function(evt){this.setChecked(!this.checked);},setChecked:function(_484){this.checked=_484;this._setStateClass();this.onChange(_484);}});}if(!dojo._hasResource["dojo.data.ItemFileReadStore"]){dojo._hasResource["dojo.data.ItemFileReadStore"]=true;dojo.provide("dojo.data.ItemFileReadStore");dojo.declare("dojo.data.ItemFileReadStore",null,{constructor:function(_485){this._arrayOfAllItems=[];this._arrayOfTopLevelItems=[];this._loadFinished=false;this._jsonFileUrl=_485.url;this._jsonData=_485.data;this._datatypeMap=_485.typeMap||{};if(!this._datatypeMap["Date"]){this._datatypeMap["Date"]={type:Date,deserialize:function(_486){return dojo.date.stamp.fromISOString(_486);}};}this._features={"dojo.data.api.Read":true,"dojo.data.api.Identity":true};this._itemsByIdentity=null;this._storeRefPropName="_S";this._itemNumPropName="_0";this._rootItemPropName="_RI";this._loadInProgress=false;this._queuedFetches=[];},url:"",_assertIsItem:function(item){if(!this.isItem(item)){throw new Error("dojo.data.ItemFileReadStore: a function was passed an item argument that was not an item");}},_assertIsAttribute:function(_488){if(typeof _488!=="string"){throw new Error("dojo.data.ItemFileReadStore: a function was passed an attribute argument that was not an attribute name string");}},getValue:function(item,_48a,_48b){var _48c=this.getValues(item,_48a);return (_48c.length>0)?_48c[0]:_48b;},getValues:function(item,_48e){this._assertIsItem(item);this._assertIsAttribute(_48e);return item[_48e]||[];},getAttributes:function(item){this._assertIsItem(item);var _490=[];for(var key in item){if((key!==this._storeRefPropName)&&(key!==this._itemNumPropName)&&(key!==this._rootItemPropName)){_490.push(key);}}return _490;},hasAttribute:function(item,_493){return this.getValues(item,_493).length>0;},containsValue:function(item,_495,_496){var _497=undefined;if(typeof _496==="string"){_497=dojo.data.util.filter.patternToRegExp(_496,false);}return this._containsValue(item,_495,_496,_497);},_containsValue:function(item,_499,_49a,_49b){var _49c=this.getValues(item,_499);for(var i=0;i<_49c.length;++i){var _49e=_49c[i];if(typeof _49e==="string"&&_49b){return (_49e.match(_49b)!==null);}else{if(_49a===_49e){return true;}}}return false;},isItem:function(_49f){if(_49f&&_49f[this._storeRefPropName]===this){if(this._arrayOfAllItems[_49f[this._itemNumPropName]]===_49f){return true;}}return false;},isItemLoaded:function(_4a0){return this.isItem(_4a0);},loadItem:function(_4a1){this._assertIsItem(_4a1.item);},getFeatures:function(){return this._features;},getLabel:function(item){if(this._labelAttr&&this.isItem(item)){return this.getValue(item,this._labelAttr);}return undefined;},getLabelAttributes:function(item){if(this._labelAttr){return [this._labelAttr];}return null;},_fetchItems:function(_4a4,_4a5,_4a6){var self=this;var _4a8=function(_4a9,_4aa){var _4ab=[];if(_4a9.query){var _4ac=_4a9.queryOptions?_4a9.queryOptions.ignoreCase:false;var _4ad={};for(var key in _4a9.query){var _4af=_4a9.query[key];if(typeof _4af==="string"){_4ad[key]=dojo.data.util.filter.patternToRegExp(_4af,_4ac);}}for(var i=0;i<_4aa.length;++i){var _4b1=true;var _4b2=_4aa[i];if(_4b2===null){_4b1=false;}else{for(var key in _4a9.query){var _4af=_4a9.query[key];if(!self._containsValue(_4b2,key,_4af,_4ad[key])){_4b1=false;}}}if(_4b1){_4ab.push(_4b2);}}_4a5(_4ab,_4a9);}else{for(var i=0;i<_4aa.length;++i){var item=_4aa[i];if(item!==null){_4ab.push(item);}}_4a5(_4ab,_4a9);}};if(this._loadFinished){_4a8(_4a4,this._getItemsArray(_4a4.queryOptions));}else{if(this._jsonFileUrl){if(this._loadInProgress){this._queuedFetches.push({args:_4a4,filter:_4a8});}else{this._loadInProgress=true;var _4b4={url:self._jsonFileUrl,handleAs:"json-comment-optional"};var _4b5=dojo.xhrGet(_4b4);_4b5.addCallback(function(data){try{self._getItemsFromLoadedData(data);self._loadFinished=true;self._loadInProgress=false;_4a8(_4a4,self._getItemsArray(_4a4.queryOptions));self._handleQueuedFetches();}catch(e){self._loadFinished=true;self._loadInProgress=false;_4a6(e,_4a4);}});_4b5.addErrback(function(_4b7){self._loadInProgress=false;_4a6(_4b7,_4a4);});}}else{if(this._jsonData){try{this._loadFinished=true;this._getItemsFromLoadedData(this._jsonData);this._jsonData=null;_4a8(_4a4,this._getItemsArray(_4a4.queryOptions));}catch(e){_4a6(e,_4a4);}}else{_4a6(new Error("dojo.data.ItemFileReadStore: No JSON source data was provided as either URL or a nested Javascript object."),_4a4);}}}},_handleQueuedFetches:function(){if(this._queuedFetches.length>0){for(var i=0;i<this._queuedFetches.length;i++){var _4b9=this._queuedFetches[i];var _4ba=_4b9.args;var _4bb=_4b9.filter;if(_4bb){_4bb(_4ba,this._getItemsArray(_4ba.queryOptions));}else{this.fetchItemByIdentity(_4ba);}}this._queuedFetches=[];}},_getItemsArray:function(_4bc){if(_4bc&&_4bc.deep){return this._arrayOfAllItems;}return this._arrayOfTopLevelItems;},close:function(_4bd){},_getItemsFromLoadedData:function(_4be){function valueIsAnItem(_4bf){var _4c0=((_4bf!=null)&&(typeof _4bf=="object")&&(!dojo.isArray(_4bf))&&(!dojo.isFunction(_4bf))&&(_4bf.constructor==Object)&&(typeof _4bf._reference=="undefined")&&(typeof _4bf._type=="undefined")&&(typeof _4bf._value=="undefined"));return _4c0;};var self=this;function addItemAndSubItemsToArrayOfAllItems(_4c2){self._arrayOfAllItems.push(_4c2);for(var _4c3 in _4c2){var _4c4=_4c2[_4c3];if(_4c4){if(dojo.isArray(_4c4)){var _4c5=_4c4;for(var k=0;k<_4c5.length;++k){var _4c7=_4c5[k];if(valueIsAnItem(_4c7)){addItemAndSubItemsToArrayOfAllItems(_4c7);}}}else{if(valueIsAnItem(_4c4)){addItemAndSubItemsToArrayOfAllItems(_4c4);}}}}};this._labelAttr=_4be.label;var i;var item;this._arrayOfAllItems=[];this._arrayOfTopLevelItems=_4be.items;for(i=0;i<this._arrayOfTopLevelItems.length;++i){item=this._arrayOfTopLevelItems[i];addItemAndSubItemsToArrayOfAllItems(item);item[this._rootItemPropName]=true;}var _4ca={};var key;for(i=0;i<this._arrayOfAllItems.length;++i){item=this._arrayOfAllItems[i];for(key in item){if(key!==this._rootItemPropName){var _4cc=item[key];if(_4cc!==null){if(!dojo.isArray(_4cc)){item[key]=[_4cc];}}else{item[key]=[null];}}_4ca[key]=key;}}while(_4ca[this._storeRefPropName]){this._storeRefPropName+="_";}while(_4ca[this._itemNumPropName]){this._itemNumPropName+="_";}var _4cd;var _4ce=_4be.identifier;this._itemsByIdentity={};if(_4ce){this._features["dojo.data.api.Identity"]=_4ce;for(i=0;i<this._arrayOfAllItems.length;++i){item=this._arrayOfAllItems[i];_4cd=item[_4ce];var _4cf=_4cd[0];if(!this._itemsByIdentity[_4cf]){this._itemsByIdentity[_4cf]=item;}else{if(this._jsonFileUrl){throw new Error("dojo.data.ItemFileReadStore:  The json data as specified by: ["+this._jsonFileUrl+"] is malformed.  Items within the list have identifier: ["+_4ce+"].  Value collided: ["+_4cf+"]");}else{if(this._jsonData){throw new Error("dojo.data.ItemFileReadStore:  The json data provided by the creation arguments is malformed.  Items within the list have identifier: ["+_4ce+"].  Value collided: ["+_4cf+"]");}}}}}else{this._features["dojo.data.api.Identity"]=Number;}for(i=0;i<this._arrayOfAllItems.length;++i){item=this._arrayOfAllItems[i];item[this._storeRefPropName]=this;item[this._itemNumPropName]=i;}for(i=0;i<this._arrayOfAllItems.length;++i){item=this._arrayOfAllItems[i];for(key in item){_4cd=item[key];for(var j=0;j<_4cd.length;++j){_4cc=_4cd[j];if(_4cc!==null&&typeof _4cc=="object"){if(_4cc._type&&_4cc._value){var type=_4cc._type;var _4d2=this._datatypeMap[type];if(!_4d2){throw new Error("dojo.data.ItemFileReadStore: in the typeMap constructor arg, no object class was specified for the datatype '"+type+"'");}else{if(dojo.isFunction(_4d2)){_4cd[j]=new _4d2(_4cc._value);}else{if(dojo.isFunction(_4d2.deserialize)){_4cd[j]=_4d2.deserialize(_4cc._value);}else{throw new Error("dojo.data.ItemFileReadStore: Value provided in typeMap was neither a constructor, nor a an object with a deserialize function");}}}}if(_4cc._reference){var _4d3=_4cc._reference;if(dojo.isString(_4d3)){_4cd[j]=this._itemsByIdentity[_4d3];}else{for(var k=0;k<this._arrayOfAllItems.length;++k){var _4d5=this._arrayOfAllItems[k];var _4d6=true;for(var _4d7 in _4d3){if(_4d5[_4d7]!=_4d3[_4d7]){_4d6=false;}}if(_4d6){_4cd[j]=_4d5;}}}}}}}}},getIdentity:function(item){var _4d9=this._features["dojo.data.api.Identity"];if(_4d9===Number){return item[this._itemNumPropName];}else{var _4da=item[_4d9];if(_4da){return _4da[0];}}return null;},fetchItemByIdentity:function(_4db){if(!this._loadFinished){var self=this;if(this._jsonFileUrl){if(this._loadInProgress){this._queuedFetches.push({args:_4db});}else{var _4dd={url:self._jsonFileUrl,handleAs:"json-comment-optional"};var _4de=dojo.xhrGet(_4dd);_4de.addCallback(function(data){var _4e0=_4db.scope?_4db.scope:dojo.global;try{self._getItemsFromLoadedData(data);self._loadFinished=true;self._loadInProgress=false;var item=self._getItemByIdentity(_4db.identity);if(_4db.onItem){_4db.onItem.call(_4e0,item);}self._handleQueuedFetches();}catch(error){self._loadInProgress=false;if(_4db.onError){_4db.onError.call(_4e0,error);}}});_4de.addErrback(function(_4e2){self._loadInProgress=false;if(_4db.onError){var _4e3=_4db.scope?_4db.scope:dojo.global;_4db.onError.call(_4e3,_4e2);}});}}else{if(this._jsonData){self._getItemsFromLoadedData(self._jsonData);self._jsonData=null;self._loadFinished=true;var item=self._getItemByIdentity(_4db.identity);if(_4db.onItem){var _4e5=_4db.scope?_4db.scope:dojo.global;_4db.onItem.call(_4e5,item);}}}}else{var item=this._getItemByIdentity(_4db.identity);if(_4db.onItem){var _4e5=_4db.scope?_4db.scope:dojo.global;_4db.onItem.call(_4e5,item);}}},_getItemByIdentity:function(_4e6){var item=null;if(this._itemsByIdentity){item=this._itemsByIdentity[_4e6];if(item===undefined){item=null;}}else{this._arrayOfAllItems[_4e6];}return item;},getIdentityAttributes:function(item){var _4e9=this._features["dojo.data.api.Identity"];if(_4e9===Number){return null;}else{return [_4e9];}},_forceLoad:function(){var self=this;if(this._jsonFileUrl){var _4eb={url:self._jsonFileUrl,handleAs:"json-comment-optional",sync:true};var _4ec=dojo.xhrGet(_4eb);_4ec.addCallback(function(data){try{if(self._loadInProgress!==true&&!self._loadFinished){self._getItemsFromLoadedData(data);self._loadFinished=true;}}catch(e){console.log(e);throw e;}});_4ec.addErrback(function(_4ee){throw _4ee;});}else{if(this._jsonData){self._getItemsFromLoadedData(self._jsonData);self._jsonData=null;self._loadFinished=true;}}}});dojo.extend(dojo.data.ItemFileReadStore,dojo.data.util.simpleFetch);}if(!dojo._hasResource["dijit.form._DropDownTextBox"]){dojo._hasResource["dijit.form._DropDownTextBox"]=true;dojo.provide("dijit.form._DropDownTextBox");dojo.declare("dijit.form._DropDownTextBox",null,{templateString:"<table class=\"dijit dijitReset dijitInline dijitLeft\" baseClass=\"${baseClass}\" cellspacing=\"0\" cellpadding=\"0\"\n\tid=\"widget_${id}\" name=\"${name}\" dojoAttachEvent=\"onmouseover:_onMouse,onmouseout:_onMouse\" waiRole=\"presentation\"\n\t><tr\n\t\t><td class='dijitReset dijitStretch dijitComboBoxInput'\n\t\t\t><input class='XdijitInputField' type=\"text\" autocomplete=\"off\" name=\"${name}\"\n\t\t\tdojoAttachEvent=\"onkeypress, onkeyup, onfocus, onblur, compositionend\"\n\t\t\tdojoAttachPoint=\"textbox,focusNode\" id='${id}'\n\t\t\ttabIndex='${tabIndex}' size='${size}' maxlength='${maxlength}'\n\t\t\twaiRole=\"combobox\"\n\t\t></td\n\t\t><td class='dijitReset dijitRight dijitButtonNode dijitDownArrowButton'\n\t\t\tdojoAttachPoint=\"downArrowNode\"\n\t\t\tdojoAttachEvent=\"ondijitclick:_onArrowClick,onmousedown:_onMouse,onmouseup:_onMouse,onmouseover:_onMouse,onmouseout:_onMouse\"\n\t\t><div class=\"dijitDownArrowButtonInner\" waiRole=\"presentation\" tabIndex=\"-1\">\n\t\t\t<div class=\"dijit_a11y dijitDownArrowButtonChar\">&#9660;</div>\n\t\t</div>\n\t</td></tr>\n</table>\n",baseClass:"dijitComboBox",hasDownArrow:true,_popupWidget:null,_hasMasterPopup:false,_popupClass:"",_popupArgs:{},_hasFocus:false,_arrowPressed:function(){if(!this.disabled&&this.hasDownArrow){dojo.addClass(this.downArrowNode,"dijitArrowButtonActive");}},_arrowIdle:function(){if(!this.disabled&&this.hasDownArrow){dojo.removeClass(this.downArrowNode,"dojoArrowButtonPushed");}},makePopup:function(){var _4ef=this;function _createNewPopup(){var node=document.createElement("div");document.body.appendChild(node);var _4f1=dojo.getObject(_4ef._popupClass,false);return new _4f1(_4ef._popupArgs,node);};if(!this._popupWidget){if(this._hasMasterPopup){var _4f2=dojo.getObject(this.declaredClass,false);if(!_4f2.prototype._popupWidget){_4f2.prototype._popupWidget=_createNewPopup();}this._popupWidget=_4f2.prototype._popupWidget;}else{this._popupWidget=_createNewPopup();}}},_onArrowClick:function(){if(this.disabled){return;}this.focus();this.makePopup();if(this._isShowingNow){this._hideResultList();}else{this._openResultList();}},_hideResultList:function(){if(this._isShowingNow){dijit.popup.close();this._arrowIdle();this._isShowingNow=false;}},_openResultList:function(){this._showResultList();},onfocus:function(){this._hasFocus=true;},onblur:function(){this._arrowIdle();this._hasFocus=false;dojo.removeClass(this.nodeWithBorder,"dijitInputFieldFocused");this.validate(false);},onkeypress:function(evt){if(evt.ctrlKey||evt.altKey){return;}switch(evt.keyCode){case dojo.keys.PAGE_DOWN:case dojo.keys.DOWN_ARROW:if(!this._isShowingNow||this._prev_key_esc){this.makePopup();this._arrowPressed();this._openResultList();}dojo.stopEvent(evt);this._prev_key_backspace=false;this._prev_key_esc=false;break;case dojo.keys.PAGE_UP:case dojo.keys.UP_ARROW:case dojo.keys.ENTER:dojo.stopEvent(evt);case dojo.keys.ESCAPE:case dojo.keys.TAB:if(this._isShowingNow){this._prev_key_backspace=false;this._prev_key_esc=(evt.keyCode==dojo.keys.ESCAPE);this._hideResultList();}break;}},compositionend:function(evt){this.onkeypress({charCode:-1});},_showResultList:function(){this._hideResultList();var _4f5=this._popupWidget.getListLength?this._popupWidget.getItems():[this._popupWidget.domNode];if(_4f5.length){var _4f6=Math.min(_4f5.length,this.maxListLength);with(this._popupWidget.domNode.style){display="";width="";height="";}this._arrowPressed();this._displayMessage("");var best=this.open();var _4f8=dojo.marginBox(this._popupWidget.domNode);this._popupWidget.domNode.style.overflow=((best.h==_4f8.h)&&(best.w==_4f8.w))?"hidden":"auto";dojo.marginBox(this._popupWidget.domNode,{h:best.h,w:Math.max(best.w,this.domNode.offsetWidth)});}},getDisplayedValue:function(){return this.textbox.value;},setDisplayedValue:function(_4f9){this.textbox.value=_4f9;},uninitialize:function(){if(this._popupWidget){this._hideResultList();this._popupWidget.destroy();}},open:function(){this.makePopup();var self=this;self._isShowingNow=true;return dijit.popup.open({popup:this._popupWidget,around:this.domNode,parent:this});},_onBlur:function(){this._hideResultList();},postMixInProperties:function(){this.baseClass=this.hasDownArrow?this.baseClass:this.baseClass+"NoArrow";}});}if(!dojo._hasResource["dijit.form.TextBox"]){dojo._hasResource["dijit.form.TextBox"]=true;dojo.provide("dijit.form.TextBox");dojo.declare("dijit.form.TextBox",dijit.form._FormWidget,{trim:false,uppercase:false,lowercase:false,propercase:false,size:"20",maxlength:"999999",templateString:"<input dojoAttachPoint='textbox,focusNode' dojoAttachEvent='onfocus,onkeyup,onkeypress:_onKeyPress' autocomplete=\"off\"\n\tid='${id}' name='${name}' class=\"dijitInputField\" type='${type}' size='${size}' maxlength='${maxlength}' tabIndex='${tabIndex}'>\n",getTextValue:function(){return this.filter(this.textbox.value);},getValue:function(){return this.parse(this.getTextValue(),this.constraints);},setValue:function(_4fb,_4fc,_4fd){if(_4fb==null){_4fb="";}_4fb=this.filter(_4fb);if(typeof _4fd=="undefined"){_4fd=(typeof _4fb=="undefined"||_4fb==null||_4fb==NaN)?null:this.format(_4fb,this.constraints);}if(_4fd!=null){var _4fe=this;this.textbox.value=_4fd;}dijit.form.TextBox.superclass.setValue.call(this,_4fb,_4fc);},forWaiValuenow:function(){return this.getTextValue();},format:function(_4ff,_500){return _4ff;},parse:function(_501,_502){return _501;},postCreate:function(){if(typeof this.nodeWithBorder!="object"){this.nodeWithBorder=this.textbox;}this.textbox.setAttribute("value",this.getTextValue());this.inherited("postCreate",arguments);},filter:function(val){if(val==null){return null;}if(this.trim){val=dojo.trim(val);}if(this.uppercase){val=val.toUpperCase();}if(this.lowercase){val=val.toLowerCase();}if(this.propercase){val=val.replace(/[^\s]+/g,function(word){return word.substring(0,1).toUpperCase()+word.substring(1);});}return val;},onfocus:function(){dojo.addClass(this.nodeWithBorder,"dijitInputFieldFocused");},_onBlur:function(){dojo.removeClass(this.nodeWithBorder,"dijitInputFieldFocused");this.setValue(this.getValue(),true);},onkeyup:function(){}});}if(!dojo._hasResource["dijit.Tooltip"]){dojo._hasResource["dijit.Tooltip"]=true;dojo.provide("dijit.Tooltip");dojo.declare("dijit._MasterTooltip",[dijit._Widget,dijit._Templated],{duration:200,templateString:"<div class=\"dijitTooltip dijitTooltipLeft\" id=\"dojoTooltip\">\n\t<div class=\"dijitTooltipContainer dijitTooltipContents\" dojoAttachPoint=\"containerNode\" waiRole='alert'></div>\n\t<div class=\"dijitTooltipConnector\"></div>\n</div>\n",postCreate:function(){dojo.body().appendChild(this.domNode);this.bgIframe=new dijit.BackgroundIframe(this.domNode);this.fadeIn=dojo.fadeIn({node:this.domNode,duration:this.duration,onEnd:dojo.hitch(this,"_onShow")}),this.fadeOut=dojo.fadeOut({node:this.domNode,duration:this.duration,onEnd:dojo.hitch(this,"_onHide")});},show:function(_505,_506){if(this.fadeOut.status()=="playing"){this._onDeck=arguments;return;}this.containerNode.innerHTML=_505;this.domNode.style.top=(this.domNode.offsetTop+1)+"px";var _507=this.isLeftToRight()?{"BR":"BL","BL":"BR"}:{"BL":"BR","BR":"BL"};var pos=dijit.placeOnScreenAroundElement(this.domNode,_506,_507);this.domNode.className="dijitTooltip dijitTooltip"+(pos.corner=="BL"?"Right":"Left");dojo.style(this.domNode,"opacity",0);this.fadeIn.play();this.isShowingNow=true;},_onShow:function(){if(dojo.isIE){this.domNode.style.filter="";}},hide:function(){if(this._onDeck){this._onDeck=null;return;}this.fadeIn.stop();this.isShowingNow=false;this.fadeOut.play();},_onHide:function(){this.domNode.style.cssText="";if(this._onDeck){this.show.apply(this,this._onDeck);this._onDeck=null;}}});dojo.addOnLoad(function(){dijit.MasterTooltip=new dijit._MasterTooltip();});dojo.declare("dijit.Tooltip",dijit._Widget,{label:"",showDelay:400,connectId:"",postCreate:function(){this.srcNodeRef.style.display="none";this._connectNode=dojo.byId(this.connectId);dojo.forEach(["onMouseOver","onHover","onMouseOut","onUnHover"],function(_509){this.connect(this._connectNode,_509.toLowerCase(),"_"+_509);},this);},_onMouseOver:function(e){this._onHover(e);},_onMouseOut:function(e){if(dojo.isDescendant(e.relatedTarget,this._connectNode)){return;}this._onUnHover(e);},_onHover:function(e){if(this._hover){return;}this._hover=true;if(!this.isShowingNow&&!this._showTimer){this._showTimer=setTimeout(dojo.hitch(this,"open"),this.showDelay);}},_onUnHover:function(e){if(!this._hover){return;}this._hover=false;if(this._showTimer){clearTimeout(this._showTimer);delete this._showTimer;}else{this.close();}},open:function(){if(this.isShowingNow){return;}if(this._showTimer){clearTimeout(this._showTimer);delete this._showTimer;}dijit.MasterTooltip.show(this.label||this.domNode.innerHTML,this._connectNode);this.isShowingNow=true;},close:function(){if(!this.isShowingNow){return;}dijit.MasterTooltip.hide();this.isShowingNow=false;},uninitialize:function(){this.close();}});}if(!dojo._hasResource["dijit.form.ValidationTextBox"]){dojo._hasResource["dijit.form.ValidationTextBox"]=true;dojo.provide("dijit.form.ValidationTextBox");dojo.declare("dijit.form.ValidationTextBox",dijit.form.TextBox,{required:false,promptMessage:"",invalidMessage:"",constraints:{},regExp:".*",regExpGen:function(_50e){return this.regExp;},setValue:function(){this.inherited("setValue",arguments);this.validate(false);},validator:function(_50f,_510){return (new RegExp("^("+this.regExpGen(_510)+")"+(this.required?"":"?")+"$")).test(_50f)&&(!this.required||!this._isEmpty(_50f));},isValid:function(_511){return this.validator(this.textbox.value,this.constraints);},_isEmpty:function(_512){return /^\s*$/.test(_512);},getErrorMessage:function(_513){return this.invalidMessage;},getPromptMessage:function(_514){return this.promptMessage;},validate:function(_515){var _516="";var _517=this.isValid(_515);var _518=_517?"Normal":"Error";if(!dojo.hasClass(this.nodeWithBorder,"dijitInputFieldValidation"+_518)){dojo.removeClass(this.nodeWithBorder,"dijitInputFieldValidation"+((_518=="Normal")?"Error":"Normal"));dojo.addClass(this.nodeWithBorder,"dijitInputFieldValidation"+_518);}dijit.wai.setAttr(this.focusNode,"waiState","invalid",(_517?"false":"true"));if(_515){if(this._isEmpty(this.textbox.value)){_516=this.getPromptMessage(true);}if(!_516&&!_517){_516=this.getErrorMessage(true);}}this._displayMessage(_516);},_message:"",_displayMessage:function(_519){if(this._message==_519){return;}this._message=_519;this.displayMessage(_519);},displayMessage:function(_51a){if(_51a){dijit.MasterTooltip.show(_51a,this.domNode);}else{dijit.MasterTooltip.hide();}},_onBlur:function(evt){this.validate(false);this.inherited("_onBlur",arguments);},onfocus:function(evt){this.inherited("onfocus",arguments);this.validate(true);},onkeyup:function(evt){this.onfocus(evt);},postMixInProperties:function(){if(this.constraints==dijit.form.ValidationTextBox.prototype.constraints){this.constraints={};}this.inherited("postMixInProperties",arguments);this.constraints.locale=this.lang;this.messages=dojo.i18n.getLocalization("dijit.form","validate",this.lang);dojo.forEach(["invalidMessage","missingMessage"],function(prop){if(!this[prop]){this[prop]=this.messages[prop];}},this);var p=this.regExpGen(this.constraints);this.regExp=p;}});dojo.declare("dijit.form.MappedTextBox",dijit.form.ValidationTextBox,{serialize:function(val){return val.toString();},toString:function(){var val=this.getValue();return (val!=null)?((typeof val=="string")?val:this.serialize(val,this.constraints)):"";},validate:function(){this.valueNode.value=this.toString();this.inherited("validate",arguments);},postCreate:function(){var _522=this.textbox;var _523=(this.valueNode=document.createElement("input"));_523.setAttribute("type",_522.type);_523.setAttribute("value",this.toString());dojo.style(_523,"display","none");_523.name=this.textbox.name;this.textbox.removeAttribute("name");dojo.place(_523,_522,"after");this.inherited("postCreate",arguments);}});dojo.declare("dijit.form.RangeBoundTextBox",dijit.form.MappedTextBox,{rangeMessage:"",compare:function(val1,val2){return val1-val2;},rangeCheck:function(_526,_527){var _528=(typeof _527.min!="undefined");var _529=(typeof _527.max!="undefined");if(_528||_529){return (!_528||this.compare(_526,_527.min)>=0)&&(!_529||this.compare(_526,_527.max)<=0);}else{return true;}},isInRange:function(_52a){return this.rangeCheck(this.getValue(),this.constraints);},isValid:function(_52b){return this.inherited("isValid",arguments)&&((this._isEmpty(this.textbox.value)&&!this.required)||this.isInRange(_52b));},getErrorMessage:function(_52c){if(dijit.form.RangeBoundTextBox.superclass.isValid.call(this,false)&&!this.isInRange(_52c)){return this.rangeMessage;}else{return this.inherited("getErrorMessage",arguments);}},postMixInProperties:function(){this.inherited("postMixInProperties",arguments);if(!this.rangeMessage){this.messages=dojo.i18n.getLocalization("dijit.form","validate",this.lang);this.rangeMessage=this.messages.rangeMessage;}},postCreate:function(){this.inherited("postCreate",arguments);if(typeof this.constraints.min!="undefined"){dijit.wai.setAttr(this.domNode,"waiState","valuemin",this.constraints.min);}if(typeof this.constraints.max!="undefined"){dijit.wai.setAttr(this.domNode,"waiState","valuemax",this.constraints.max);}}});}if(!dojo._hasResource["dijit.form.ComboBox"]){dojo._hasResource["dijit.form.ComboBox"]=true;dojo.provide("dijit.form.ComboBox");dojo.declare("dijit.form.ComboBoxMixin",dijit.form._DropDownTextBox,{pageSize:30,store:null,query:{},autoComplete:true,searchDelay:100,searchAttr:"name",ignoreCase:true,_hasMasterPopup:true,_popupClass:"dijit.form._ComboBoxMenu",getValue:function(){return dijit.form.TextBox.superclass.getValue.apply(this,arguments);},setDisplayedValue:function(_52d){this.setValue(_52d,true);},_getCaretPos:function(_52e){if(typeof (_52e.selectionStart)=="number"){return _52e.selectionStart;}else{if(dojo.isIE){var tr=document.selection.createRange().duplicate();var ntr=_52e.createTextRange();tr.move("character",0);ntr.move("character",0);try{ntr.setEndPoint("EndToEnd",tr);return String(ntr.text).replace(/\r/g,"").length;}catch(e){return 0;}}}},_setCaretPos:function(_531,_532){_532=parseInt(_532);this._setSelectedRange(_531,_532,_532);},_setSelectedRange:function(_533,_534,end){if(!end){end=_533.value.length;}if(_533.setSelectionRange){dijit.focus(_533);_533.setSelectionRange(_534,end);}else{if(_533.createTextRange){var _536=_533.createTextRange();with(_536){collapse(true);moveEnd("character",end);moveStart("character",_534);select();}}else{_533.value=_533.value;_533.blur();dijit.focus(_533);var dist=parseInt(_533.value.length)-end;var _538=String.fromCharCode(37);var tcc=_538.charCodeAt(0);for(var x=0;x<dist;x++){var te=document.createEvent("KeyEvents");te.initKeyEvent("keypress",true,true,null,false,false,false,false,tcc,tcc);_533.dispatchEvent(te);}}}},onkeypress:function(evt){if(evt.ctrlKey||evt.altKey){return;}var _53d=false;if(this._isShowingNow){this._popupWidget.handleKey(evt);}switch(evt.keyCode){case dojo.keys.PAGE_DOWN:case dojo.keys.DOWN_ARROW:if(!this._isShowingNow||this._prev_key_esc){this._arrowPressed();_53d=true;}else{this._announceOption(this._popupWidget.getHighlightedOption());}dojo.stopEvent(evt);this._prev_key_backspace=false;this._prev_key_esc=false;break;case dojo.keys.PAGE_UP:case dojo.keys.UP_ARROW:if(this._isShowingNow){this._announceOption(this._popupWidget.getHighlightedOption());}dojo.stopEvent(evt);this._prev_key_backspace=false;this._prev_key_esc=false;break;case dojo.keys.ENTER:if(this._isShowingNow){var _53e=this._popupWidget.getHighlightedOption();if(_53e==this._popupWidget.nextButton){this._nextSearch(1);dojo.stopEvent(evt);break;}else{if(_53e==this._popupWidget.previousButton){this._nextSearch(-1);dojo.stopEvent(evt);break;}}}case dojo.keys.TAB:if(this._isShowingNow){this._prev_key_backspace=false;this._prev_key_esc=false;if(this._isShowingNow&&this._popupWidget.getHighlightedOption()){this._popupWidget.setValue({target:this._popupWidget.getHighlightedOption()},true);}else{this.setDisplayedValue(this.getDisplayedValue());}this._hideResultList();}else{this.setDisplayedValue(this.getDisplayedValue());}break;case dojo.keys.SPACE:this._prev_key_backspace=false;this._prev_key_esc=false;if(this._isShowingNow&&this._popupWidget.getHighlightedOption()){dojo.stopEvent(evt);this._selectOption();this._hideResultList();}else{_53d=true;}break;case dojo.keys.ESCAPE:this._prev_key_backspace=false;this._prev_key_esc=true;this._hideResultList();this.setValue(this.getValue());break;case dojo.keys.DELETE:case dojo.keys.BACKSPACE:this._prev_key_esc=false;this._prev_key_backspace=true;_53d=true;break;case dojo.keys.RIGHT_ARROW:case dojo.keys.LEFT_ARROW:this._prev_key_backspace=false;this._prev_key_esc=false;break;default:this._prev_key_backspace=false;this._prev_key_esc=false;if(evt.charCode!=0){_53d=true;}}if(this.searchTimer){clearTimeout(this.searchTimer);}if(_53d){this.searchTimer=setTimeout(dojo.hitch(this,this._startSearchFromInput),this.searchDelay);}},_autoCompleteText:function(text){this._setSelectedRange(this.focusNode,this.focusNode.value.length,this.focusNode.value.length);if(new RegExp("^"+escape(this.focusNode.value),this.ignoreCase?"i":"").test(escape(text))){var cpos=this._getCaretPos(this.focusNode);if((cpos+1)>this.focusNode.value.length){this.focusNode.value=text;this._setSelectedRange(this.focusNode,cpos,this.focusNode.value.length);}}else{this.focusNode.value=text;this._setSelectedRange(this.focusNode,0,this.focusNode.value.length);}},_openResultList:function(_541,_542){if(this.disabled||_542.query[this.searchAttr]!=this._lastQuery){return;}this._popupWidget.clearResultList();if(!_541.length){this._hideResultList();return;}var _543=new String(this.store.getValue(_541[0],this.searchAttr));if(_543&&(this.autoComplete)&&(!this._prev_key_backspace)&&(_542.query[this.searchAttr]!="*")){this._autoCompleteText(_543);dijit.wai.setAttr(this.focusNode||this.domNode,"waiState","valuenow",_543);}this._popupWidget.createOptions(_541,_542,dojo.hitch(this,this._getMenuLabelFromItem));this._showResultList();},onfocus:function(){dijit.form._DropDownTextBox.prototype.onfocus.apply(this,arguments);this.inherited("onfocus",arguments);},onblur:function(){dijit.form._DropDownTextBox.prototype.onblur.apply(this,arguments);if(!this._isShowingNow){this.setDisplayedValue(this.getDisplayedValue());}},_announceOption:function(node){if(node==null){return;}var _545;if(node==this._popupWidget.nextButton||node==this._popupWidget.previousButton){_545=node.innerHTML;}else{_545=this.store.getValue(node.item,this.searchAttr);}this.focusNode.value=this.focusNode.value.substring(0,this._getCaretPos(this.focusNode));this._autoCompleteText(_545);},_selectOption:function(evt){var tgt=null;if(!evt){evt={target:this._popupWidget.getHighlightedOption()};}if(!evt.target){this.setDisplayedValue(this.getDisplayedValue());return;}else{tgt=evt.target;}if(!evt.noHide){this._hideResultList();this._setCaretPos(this.focusNode,this.store.getValue(tgt.item,this.searchAttr).length);}this._doSelect(tgt);},_doSelect:function(tgt){this.setValue(this.store.getIdentity(tgt.item));},_onArrowClick:function(){if(this.disabled){return;}this.focus();this.makePopup();if(this._isShowingNow){this._hideResultList();}else{this._startSearch("");}},_startSearchFromInput:function(){this._startSearch(this.focusNode.value);},_startSearch:function(key){this.makePopup();var _54a=this.query;this._lastQuery=_54a[this.searchAttr]=key+"*";var _54b=this.store.fetch({queryOptions:{ignoreCase:this.ignoreCase,deep:true},query:_54a,onComplete:dojo.hitch(this,"_openResultList"),start:0,count:this.pageSize});function nextSearch(_54c,_54d){_54c.start+=_54c.count*_54d;_54c.store.fetch(_54c);};this._nextSearch=this._popupWidget.onPage=dojo.hitch(this,nextSearch,_54b);},_getValueField:function(){return this.searchAttr;},postMixInProperties:function(){dijit.form._DropDownTextBox.prototype.postMixInProperties.apply(this,arguments);if(!this.store){var _54e=dojo.query("> option",this.srcNodeRef).map(function(node){node.style.display="none";return {value:node.getAttribute("value"),name:String(node.innerHTML)};});this.store=new dojo.data.ItemFileReadStore({data:{identifier:this._getValueField(),items:_54e}});if(_54e&&_54e.length&&!this.value){this.value=_54e[this.srcNodeRef.selectedIndex!=-1?this.srcNodeRef.selectedIndex:0][this._getValueField()];}}if(this.query==dijit.form.ComboBoxMixin.prototype.query){this.query={};}},postCreate:function(){this.inherited("postCreate",arguments);},_getMenuLabelFromItem:function(item){return {html:false,label:this.store.getValue(item,this.searchAttr)};},open:function(){this._popupWidget.onChange=dojo.hitch(this,this._selectOption);this._popupWidget._onkeypresshandle=this._popupWidget.connect(this._popupWidget.domNode,"onkeypress",dojo.hitch(this,this.onkeypress));return dijit.form._DropDownTextBox.prototype.open.apply(this,arguments);}});dojo.declare("dijit.form._ComboBoxMenu",[dijit._Widget,dijit._Templated],{templateString:"<div class='dijitMenu' dojoAttachEvent='onclick,onmouseover,onmouseout' tabIndex='-1' style='display:none; position:absolute; overflow:\"auto\";'>"+"<div class='dijitMenuItem' dojoAttachPoint='previousButton'></div>"+"<div class='dijitMenuItem' dojoAttachPoint='nextButton'></div>"+"</div>",_onkeypresshandle:null,_messages:null,_comboBox:null,postMixInProperties:function(){this._messages=dojo.i18n.getLocalization("dijit.form","ComboBox",this.lang);this.inherited("postMixInProperties",arguments);},setValue:function(_551){this.value=_551;this.onChange(_551);},onChange:function(_552){},onPage:function(_553){},postCreate:function(){this.previousButton.innerHTML=this._messages["previousMessage"];this.nextButton.innerHTML=this._messages["nextMessage"];this.inherited("postCreate",arguments);},onClose:function(){this.disconnect(this._onkeypresshandle);this._blurOptionNode();},_createOption:function(item,_555){var _556=_555(item);var _557=document.createElement("div");if(_556.html){_557.innerHTML=_556.label;}else{_557.appendChild(document.createTextNode(_556.label));}if(_557.innerHTML==""){_557.innerHTML="&nbsp;";}_557.item=item;return _557;},createOptions:function(_558,_559,_55a){this.previousButton.style.display=_559.start==0?"none":"";var _55b=this;dojo.forEach(_558,function(item){var _55d=_55b._createOption(item,_55a);_55d.className="dijitMenuItem";_55b.domNode.insertBefore(_55d,_55b.nextButton);});this.nextButton.style.display=_559.count==_558.length?"":"none";},clearResultList:function(){while(this.domNode.childNodes.length>2){this.domNode.removeChild(this.domNode.childNodes[this.domNode.childNodes.length-2]);}},getItems:function(){return this.domNode.childNodes;},getListLength:function(){return this.domNode.childNodes.length-2;},onclick:function(evt){if(evt.target===this.domNode){return;}else{if(evt.target==this.previousButton){this.onPage(-1);}else{if(evt.target==this.nextButton){this.onPage(1);}else{var tgt=evt.target;while(!tgt.item){tgt=tgt.parentNode;}this.setValue({target:tgt},true);}}}},onmouseover:function(evt){if(evt.target===this.domNode){return;}this._focusOptionNode(evt.target);},onmouseout:function(evt){if(evt.target===this.domNode){return;}this._blurOptionNode();},_focusOptionNode:function(node){if(this._highlighted_option!=node){this._blurOptionNode();this._highlighted_option=node;dojo.addClass(this._highlighted_option,"dijitMenuItemHover");}},_blurOptionNode:function(){if(this._highlighted_option){dojo.removeClass(this._highlighted_option,"dijitMenuItemHover");this._highlighted_option=null;}},_highlightNextOption:function(){if(!this.getHighlightedOption()){this._focusOptionNode(this.domNode.firstChild.style.display=="none"?this.domNode.firstChild.nextSibling:this.domNode.firstChild);}else{if(this._highlighted_option.nextSibling&&this._highlighted_option.nextSibling.style.display!="none"){this._focusOptionNode(this._highlighted_option.nextSibling);}}dijit.scrollIntoView(this._highlighted_option);},_highlightPrevOption:function(){if(!this.getHighlightedOption()){this._focusOptionNode(this.domNode.lastChild.style.display=="none"?this.domNode.lastChild.previousSibling:this.domNode.lastChild);}else{if(this._highlighted_option.previousSibling&&this._highlighted_option.previousSibling.style.display!="none"){this._focusOptionNode(this._highlighted_option.previousSibling);}}dijit.scrollIntoView(this._highlighted_option);},_page:function(up){var _564=0;var _565=this.domNode.scrollTop;var _566=parseInt(dojo.getComputedStyle(this.domNode).height);if(!this.getHighlightedOption()){this._highlightNextOption();}while(_564<_566){if(up){if(!this.getHighlightedOption().previousSibling||this._highlighted_option.previousSibling.style.display=="none"){break;}this._highlightPrevOption();}else{if(!this.getHighlightedOption().nextSibling||this._highlighted_option.nextSibling.style.display=="none"){break;}this._highlightNextOption();}var _567=this.domNode.scrollTop;_564+=(_567-_565)*(up?-1:1);_565=_567;}},pageUp:function(){this._page(true);},pageDown:function(){this._page(false);},getHighlightedOption:function(){return this._highlighted_option&&this._highlighted_option.parentNode?this._highlighted_option:null;},handleKey:function(evt){switch(evt.keyCode){case dojo.keys.DOWN_ARROW:this._highlightNextOption();break;case dojo.keys.PAGE_DOWN:this.pageDown();break;case dojo.keys.UP_ARROW:this._highlightPrevOption();break;case dojo.keys.PAGE_UP:this.pageUp();break;}}});dojo.declare("dijit.form.ComboBox",[dijit.form.ValidationTextBox,dijit.form.ComboBoxMixin],{});}({loadingState:"Loading...",errorState:"Sorry, an error occurred"});dojo.i18n._preloadLocalizations("dojo.nls.opentaps-dojo",["ROOT","es-es","es","it-it","pt-br","de","fr-fr","zh-cn","pt","en-us","zh","fr","zh-tw","it","en-gb","xx","de-de","ko-kr","ja-jp","ko","en","ja"]);
var Prototype={Version:"1.6.1",Browser:(function(){var B=navigator.userAgent;var A=Object.prototype.toString.call(window.opera)=="[object Opera]";return{IE:!!window.attachEvent&&!A,Opera:A,WebKit:B.indexOf("AppleWebKit/")>-1,Gecko:B.indexOf("Gecko")>-1&&B.indexOf("KHTML")===-1,MobileSafari:/Apple.*Mobile.*Safari/.test(B)}})(),BrowserFeatures:{XPath:!!document.evaluate,SelectorsAPI:!!document.querySelector,ElementExtensions:(function(){var A=window.Element||window.HTMLElement;return !!(A&&A.prototype)})(),SpecificElementExtensions:(function(){if(typeof window.HTMLDivElement!=="undefined"){return true}var C=document.createElement("div");var B=document.createElement("form");var A=false;if(C.__proto__&&(C.__proto__!==B.__proto__)){A=true}C=B=null;return A})()},ScriptFragment:"<script[^>]*>([\\S\\s]*?)<\/script>",JSONFilter:/^\/\*-secure-([\s\S]*)\*\/\s*$/,emptyFunction:function(){},K:function(A){return A}};if(Prototype.Browser.MobileSafari){Prototype.BrowserFeatures.SpecificElementExtensions=false}var Abstract={};var Try={these:function(){var C;for(var B=0,D=arguments.length;B<D;B++){var A=arguments[B];try{C=A();break}catch(E){}}return C}};var Class=(function(){function A(){}function B(){var G=null,F=$A(arguments);if(Object.isFunction(F[0])){G=F.shift()}function D(){this.initialize.apply(this,arguments)}Object.extend(D,Class.Methods);D.superclass=G;D.subclasses=[];if(G){A.prototype=G.prototype;D.prototype=new A;G.subclasses.push(D)}for(var E=0;E<F.length;E++){D.addMethods(F[E])}if(!D.prototype.initialize){D.prototype.initialize=Prototype.emptyFunction}D.prototype.constructor=D;return D}function C(J){var F=this.superclass&&this.superclass.prototype;var E=Object.keys(J);if(!Object.keys({toString:true}).length){if(J.toString!=Object.prototype.toString){E.push("toString")}if(J.valueOf!=Object.prototype.valueOf){E.push("valueOf")}}for(var D=0,G=E.length;D<G;D++){var I=E[D],H=J[I];if(F&&Object.isFunction(H)&&H.argumentNames().first()=="$super"){var K=H;H=(function(L){return function(){return F[L].apply(this,arguments)}})(I).wrap(K);H.valueOf=K.valueOf.bind(K);H.toString=K.toString.bind(K)}this.prototype[I]=H}return this}return{create:B,Methods:{addMethods:C}}})();(function(){var D=Object.prototype.toString;function I(Q,S){for(var R in S){Q[R]=S[R]}return Q}function L(Q){try{if(E(Q)){return"undefined"}if(Q===null){return"null"}return Q.inspect?Q.inspect():String(Q)}catch(R){if(R instanceof RangeError){return"..."}throw R}}function K(Q){var S=typeof Q;switch(S){case"undefined":case"function":case"unknown":return ;case"boolean":return Q.toString()}if(Q===null){return"null"}if(Q.toJSON){return Q.toJSON()}if(H(Q)){return }var R=[];for(var U in Q){var T=K(Q[U]);if(!E(T)){R.push(U.toJSON()+": "+T)}}return"{"+R.join(", ")+"}"}function C(Q){return $H(Q).toQueryString()}function F(Q){return Q&&Q.toHTML?Q.toHTML():String.interpret(Q)}function O(Q){var R=[];for(var S in Q){R.push(S)}return R}function M(Q){var R=[];for(var S in Q){R.push(Q[S])}return R}function J(Q){return I({},Q)}function H(Q){return !!(Q&&Q.nodeType==1)}function G(Q){return D.call(Q)=="[object Array]"}function P(Q){return Q instanceof Hash}function B(Q){return typeof Q==="function"}function A(Q){return D.call(Q)=="[object String]"}function N(Q){return D.call(Q)=="[object Number]"}function E(Q){return typeof Q==="undefined"}I(Object,{extend:I,inspect:L,toJSON:K,toQueryString:C,toHTML:F,keys:O,values:M,clone:J,isElement:H,isArray:G,isHash:P,isFunction:B,isString:A,isNumber:N,isUndefined:E})})();Object.extend(Function.prototype,(function(){var K=Array.prototype.slice;function D(O,L){var N=O.length,M=L.length;while(M--){O[N+M]=L[M]}return O}function I(M,L){M=K.call(M,0);return D(M,L)}function G(){var L=this.toString().match(/^[\s\(]*function[^(]*\(([^)]*)\)/)[1].replace(/\/\/.*?[\r\n]|\/\*(?:.|[\r\n])*?\*\//g,"").replace(/\s+/g,"").split(",");return L.length==1&&!L[0]?[]:L}function H(N){if(arguments.length<2&&Object.isUndefined(arguments[0])){return this}var L=this,M=K.call(arguments,1);return function(){var O=I(M,arguments);return L.apply(N,O)}}function F(N){var L=this,M=K.call(arguments,1);return function(P){var O=D([P||window.event],M);return L.apply(N,O)}}function J(){if(!arguments.length){return this}var L=this,M=K.call(arguments,0);return function(){var N=I(M,arguments);return L.apply(this,N)}}function E(N){var L=this,M=K.call(arguments,1);N=N*1000;return window.setTimeout(function(){return L.apply(L,M)},N)}function A(){var L=D([0.01],arguments);return this.delay.apply(this,L)}function C(M){var L=this;return function(){var N=D([L.bind(this)],arguments);return M.apply(this,N)}}function B(){if(this._methodized){return this._methodized}var L=this;return this._methodized=function(){var M=D([this],arguments);return L.apply(null,M)}}return{argumentNames:G,bind:H,bindAsEventListener:F,curry:J,delay:E,defer:A,wrap:C,methodize:B}})());Date.prototype.toJSON=function(){return'"'+this.getUTCFullYear()+"-"+(this.getUTCMonth()+1).toPaddedString(2)+"-"+this.getUTCDate().toPaddedString(2)+"T"+this.getUTCHours().toPaddedString(2)+":"+this.getUTCMinutes().toPaddedString(2)+":"+this.getUTCSeconds().toPaddedString(2)+'Z"'};RegExp.prototype.match=RegExp.prototype.test;RegExp.escape=function(A){return String(A).replace(/([.*+?^=!:${}()|[\]\/\\])/g,"\\$1")};var PeriodicalExecuter=Class.create({initialize:function(B,A){this.callback=B;this.frequency=A;this.currentlyExecuting=false;this.registerCallback()},registerCallback:function(){this.timer=setInterval(this.onTimerEvent.bind(this),this.frequency*1000)},execute:function(){this.callback(this)},stop:function(){if(!this.timer){return }clearInterval(this.timer);this.timer=null},onTimerEvent:function(){if(!this.currentlyExecuting){try{this.currentlyExecuting=true;this.execute();this.currentlyExecuting=false}catch(A){this.currentlyExecuting=false;throw A}}}});Object.extend(String,{interpret:function(A){return A==null?"":String(A)},specialChar:{"\b":"\\b","\t":"\\t","\n":"\\n","\f":"\\f","\r":"\\r","\\":"\\\\"}});Object.extend(String.prototype,(function(){function prepareReplacement(replacement){if(Object.isFunction(replacement)){return replacement}var template=new Template(replacement);return function(match){return template.evaluate(match)}}function gsub(pattern,replacement){var result="",source=this,match;replacement=prepareReplacement(replacement);if(Object.isString(pattern)){pattern=RegExp.escape(pattern)}if(!(pattern.length||pattern.source)){replacement=replacement("");return replacement+source.split("").join(replacement)+replacement}while(source.length>0){if(match=source.match(pattern)){result+=source.slice(0,match.index);result+=String.interpret(replacement(match));source=source.slice(match.index+match[0].length)}else{result+=source,source=""}}return result}function sub(pattern,replacement,count){replacement=prepareReplacement(replacement);count=Object.isUndefined(count)?1:count;return this.gsub(pattern,function(match){if(--count<0){return match[0]}return replacement(match)})}function scan(pattern,iterator){this.gsub(pattern,iterator);return String(this)}function truncate(length,truncation){length=length||30;truncation=Object.isUndefined(truncation)?"...":truncation;return this.length>length?this.slice(0,length-truncation.length)+truncation:String(this)}function strip(){return this.replace(/^\s+/,"").replace(/\s+$/,"")}function stripTags(){return this.replace(/<\w+(\s+("[^"]*"|'[^']*'|[^>])+)?>|<\/\w+>/gi,"")}function stripScripts(){return this.replace(new RegExp(Prototype.ScriptFragment,"img"),"")}function extractScripts(){var matchAll=new RegExp(Prototype.ScriptFragment,"img");var matchOne=new RegExp(Prototype.ScriptFragment,"im");return(this.match(matchAll)||[]).map(function(scriptTag){return(scriptTag.match(matchOne)||["",""])[1]})}function evalScripts(){return this.extractScripts().map(function(script){return eval(script)})}function escapeHTML(){return this.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;")}function unescapeHTML(){return this.stripTags().replace(/&lt;/g,"<").replace(/&gt;/g,">").replace(/&amp;/g,"&")}function toQueryParams(separator){var match=this.strip().match(/([^?#]*)(#.*)?$/);if(!match){return{}}return match[1].split(separator||"&").inject({},function(hash,pair){if((pair=pair.split("="))[0]){var key=decodeURIComponent(pair.shift());var value=pair.length>1?pair.join("="):pair[0];if(value!=undefined){value=decodeURIComponent(value)}if(key in hash){if(!Object.isArray(hash[key])){hash[key]=[hash[key]]}hash[key].push(value)}else{hash[key]=value}}return hash})}function toArray(){return this.split("")}function succ(){return this.slice(0,this.length-1)+String.fromCharCode(this.charCodeAt(this.length-1)+1)}function times(count){return count<1?"":new Array(count+1).join(this)}function camelize(){var parts=this.split("-"),len=parts.length;if(len==1){return parts[0]}var camelized=this.charAt(0)=="-"?parts[0].charAt(0).toUpperCase()+parts[0].substring(1):parts[0];for(var i=1;i<len;i++){camelized+=parts[i].charAt(0).toUpperCase()+parts[i].substring(1)}return camelized}function capitalize(){return this.charAt(0).toUpperCase()+this.substring(1).toLowerCase()}function underscore(){return this.replace(/::/g,"/").replace(/([A-Z]+)([A-Z][a-z])/g,"$1_$2").replace(/([a-z\d])([A-Z])/g,"$1_$2").replace(/-/g,"_").toLowerCase()}function dasherize(){return this.replace(/_/g,"-")}function inspect(useDoubleQuotes){var escapedString=this.replace(/[\x00-\x1f\\]/g,function(character){if(character in String.specialChar){return String.specialChar[character]}return"\\u00"+character.charCodeAt().toPaddedString(2,16)});if(useDoubleQuotes){return'"'+escapedString.replace(/"/g,'\\"')+'"'}return"'"+escapedString.replace(/'/g,"\\'")+"'"}function toJSON(){return this.inspect(true)}function unfilterJSON(filter){return this.replace(filter||Prototype.JSONFilter,"$1")}function isJSON(){var str=this;if(str.blank()){return false}str=this.replace(/\\./g,"@").replace(/"[^"\\\n\r]*"/g,"");return(/^[,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]*$/).test(str)}function evalJSON(sanitize){var json=this.unfilterJSON();try{if(!sanitize||json.isJSON()){return eval("("+json+")")}}catch(e){}throw new SyntaxError("Badly formed JSON string: "+this.inspect())}function include(pattern){return this.indexOf(pattern)>-1}function startsWith(pattern){return this.indexOf(pattern)===0}function endsWith(pattern){var d=this.length-pattern.length;return d>=0&&this.lastIndexOf(pattern)===d}function empty(){return this==""}function blank(){return/^\s*$/.test(this)}function interpolate(object,pattern){return new Template(this,pattern).evaluate(object)}return{gsub:gsub,sub:sub,scan:scan,truncate:truncate,strip:String.prototype.trim?String.prototype.trim:strip,stripTags:stripTags,stripScripts:stripScripts,extractScripts:extractScripts,evalScripts:evalScripts,escapeHTML:escapeHTML,unescapeHTML:unescapeHTML,toQueryParams:toQueryParams,parseQuery:toQueryParams,toArray:toArray,succ:succ,times:times,camelize:camelize,capitalize:capitalize,underscore:underscore,dasherize:dasherize,inspect:inspect,toJSON:toJSON,unfilterJSON:unfilterJSON,isJSON:isJSON,evalJSON:evalJSON,include:include,startsWith:startsWith,endsWith:endsWith,empty:empty,blank:blank,interpolate:interpolate}})());var Template=Class.create({initialize:function(A,B){this.template=A.toString();this.pattern=B||Template.Pattern},evaluate:function(A){if(A&&Object.isFunction(A.toTemplateReplacements)){A=A.toTemplateReplacements()}return this.template.gsub(this.pattern,function(D){if(A==null){return(D[1]+"")}var F=D[1]||"";if(F=="\\"){return D[2]}var B=A,G=D[3];var E=/^([^.[]+|\[((?:.*?[^\\])?)\])(\.|\[|$)/;D=E.exec(G);if(D==null){return F}while(D!=null){var C=D[1].startsWith("[")?D[2].replace(/\\\\]/g,"]"):D[1];B=B[C];if(null==B||""==D[3]){break}G=G.substring("["==D[3]?D[1].length:D[0].length);D=E.exec(G)}return F+String.interpret(B)})}});Template.Pattern=/(^|.|\r|\n)(#\{(.*?)\})/;var $break={};var Enumerable=(function(){function C(Y,X){var W=0;try{this._each(function(a){Y.call(X,a,W++)})}catch(Z){if(Z!=$break){throw Z}}return this}function R(Z,Y,X){var W=-Z,a=[],b=this.toArray();if(Z<1){return b}while((W+=Z)<b.length){a.push(b.slice(W,W+Z))}return a.collect(Y,X)}function B(Y,X){Y=Y||Prototype.K;var W=true;this.each(function(a,Z){W=W&&!!Y.call(X,a,Z);if(!W){throw $break}});return W}function I(Y,X){Y=Y||Prototype.K;var W=false;this.each(function(a,Z){if(W=!!Y.call(X,a,Z)){throw $break}});return W}function J(Y,X){Y=Y||Prototype.K;var W=[];this.each(function(a,Z){W.push(Y.call(X,a,Z))});return W}function T(Y,X){var W;this.each(function(a,Z){if(Y.call(X,a,Z)){W=a;throw $break}});return W}function H(Y,X){var W=[];this.each(function(a,Z){if(Y.call(X,a,Z)){W.push(a)}});return W}function G(Z,Y,X){Y=Y||Prototype.K;var W=[];if(Object.isString(Z)){Z=new RegExp(RegExp.escape(Z))}this.each(function(b,a){if(Z.match(b)){W.push(Y.call(X,b,a))}});return W}function A(W){if(Object.isFunction(this.indexOf)){if(this.indexOf(W)!=-1){return true}}var X=false;this.each(function(Y){if(Y==W){X=true;throw $break}});return X}function Q(X,W){W=Object.isUndefined(W)?null:W;return this.eachSlice(X,function(Y){while(Y.length<X){Y.push(W)}return Y})}function L(W,Y,X){this.each(function(a,Z){W=Y.call(X,W,a,Z)});return W}function V(X){var W=$A(arguments).slice(1);return this.map(function(Y){return Y[X].apply(Y,W)})}function P(Y,X){Y=Y||Prototype.K;var W;this.each(function(a,Z){a=Y.call(X,a,Z);if(W==null||a>=W){W=a}});return W}function N(Y,X){Y=Y||Prototype.K;var W;this.each(function(a,Z){a=Y.call(X,a,Z);if(W==null||a<W){W=a}});return W}function E(Z,X){Z=Z||Prototype.K;var Y=[],W=[];this.each(function(b,a){(Z.call(X,b,a)?Y:W).push(b)});return[Y,W]}function F(X){var W=[];this.each(function(Y){W.push(Y[X])});return W}function D(Y,X){var W=[];this.each(function(a,Z){if(!Y.call(X,a,Z)){W.push(a)}});return W}function M(X,W){return this.map(function(Z,Y){return{value:Z,criteria:X.call(W,Z,Y)}}).sort(function(d,c){var Z=d.criteria,Y=c.criteria;return Z<Y?-1:Z>Y?1:0}).pluck("value")}function O(){return this.map()}function S(){var X=Prototype.K,W=$A(arguments);if(Object.isFunction(W.last())){X=W.pop()}var Y=[this].concat(W).map($A);return this.map(function(a,Z){return X(Y.pluck(Z))})}function K(){return this.toArray().length}function U(){return"#<Enumerable:"+this.toArray().inspect()+">"}return{each:C,eachSlice:R,all:B,every:B,any:I,some:I,collect:J,map:J,detect:T,findAll:H,select:H,filter:H,grep:G,include:A,member:A,inGroupsOf:Q,inject:L,invoke:V,max:P,min:N,partition:E,pluck:F,reject:D,sortBy:M,toArray:O,entries:O,zip:S,size:K,inspect:U,find:T}})();function $A(C){if(!C){return[]}if("toArray" in Object(C)){return C.toArray()}var B=C.length||0,A=new Array(B);while(B--){A[B]=C[B]}return A}function $w(A){if(!Object.isString(A)){return[]}A=A.strip();return A?A.split(/\s+/):[]}Array.from=$A;(function(){var S=Array.prototype,M=S.slice,O=S.forEach;function B(W){for(var V=0,X=this.length;V<X;V++){W(this[V])}}if(!O){O=B}function L(){this.length=0;return this}function D(){return this[0]}function G(){return this[this.length-1]}function I(){return this.select(function(V){return V!=null})}function U(){return this.inject([],function(W,V){if(Object.isArray(V)){return W.concat(V.flatten())}W.push(V);return W})}function H(){var V=M.call(arguments,0);return this.select(function(W){return !V.include(W)})}function F(V){return(V!==false?this:this.toArray())._reverse()}function K(V){return this.inject([],function(Y,X,W){if(0==W||(V?Y.last()!=X:!Y.include(X))){Y.push(X)}return Y})}function P(V){return this.uniq().findAll(function(W){return V.detect(function(X){return W===X})})}function Q(){return M.call(this,0)}function J(){return this.length}function T(){return"["+this.map(Object.inspect).join(", ")+"]"}function R(){var V=[];this.each(function(W){var X=Object.toJSON(W);if(!Object.isUndefined(X)){V.push(X)}});return"["+V.join(", ")+"]"}function A(X,V){V||(V=0);var W=this.length;if(V<0){V=W+V}for(;V<W;V++){if(this[V]===X){return V}}return -1}function N(W,V){V=isNaN(V)?this.length:(V<0?this.length+V:V)+1;var X=this.slice(0,V).reverse().indexOf(W);return(X<0)?X:V-X-1}function C(){var a=M.call(this,0),Y;for(var W=0,X=arguments.length;W<X;W++){Y=arguments[W];if(Object.isArray(Y)&&!("callee" in Y)){for(var V=0,Z=Y.length;V<Z;V++){a.push(Y[V])}}else{a.push(Y)}}return a}Object.extend(S,Enumerable);if(!S._reverse){S._reverse=S.reverse}Object.extend(S,{_each:O,clear:L,first:D,last:G,compact:I,flatten:U,without:H,reverse:F,uniq:K,intersect:P,clone:Q,toArray:Q,size:J,inspect:T,toJSON:R});var E=(function(){return[].concat(arguments)[0][0]!==1})(1,2);if(E){S.concat=C}if(!S.indexOf){S.indexOf=A}if(!S.lastIndexOf){S.lastIndexOf=N}})();function $H(A){return new Hash(A)}var Hash=Class.create(Enumerable,(function(){function E(Q){this._object=Object.isHash(Q)?Q.toObject():Object.clone(Q)}function F(R){for(var Q in this._object){var S=this._object[Q],T=[Q,S];T.key=Q;T.value=S;R(T)}}function K(Q,R){return this._object[Q]=R}function C(Q){if(this._object[Q]!==Object.prototype[Q]){return this._object[Q]}}function N(Q){var R=this._object[Q];delete this._object[Q];return R}function P(){return Object.clone(this._object)}function O(){return this.pluck("key")}function M(){return this.pluck("value")}function G(R){var Q=this.detect(function(S){return S.value===R});return Q&&Q.key}function I(Q){return this.clone().update(Q)}function D(Q){return new Hash(Q).inject(this,function(R,S){R.set(S.key,S.value);return R})}function B(Q,R){if(Object.isUndefined(R)){return Q}return Q+"="+encodeURIComponent(String.interpret(R))}function A(){return this.inject([],function(S,T){var R=encodeURIComponent(T.key),Q=T.value;if(Q&&typeof Q=="object"){if(Object.isArray(Q)){return S.concat(Q.map(B.curry(R)))}}else{S.push(B(R,Q))}return S}).join("&")}function L(){return"#<Hash:{"+this.map(function(Q){return Q.map(Object.inspect).join(": ")}).join(", ")+"}>"}function J(){return Object.toJSON(this.toObject())}function H(){return new Hash(this)}return{initialize:E,_each:F,set:K,get:C,unset:N,toObject:P,toTemplateReplacements:P,keys:O,values:M,index:G,merge:I,update:D,toQueryString:A,inspect:L,toJSON:J,clone:H}})());Hash.from=$H;Object.extend(Number.prototype,(function(){function D(){return this.toPaddedString(2,16)}function E(){return this+1}function A(K,J){$R(0,this,true).each(K,J);return this}function B(L,K){var J=this.toString(K||10);return"0".times(L-J.length)+J}function F(){return isFinite(this)?this.toString():"null"}function I(){return Math.abs(this)}function H(){return Math.round(this)}function G(){return Math.ceil(this)}function C(){return Math.floor(this)}return{toColorPart:D,succ:E,times:A,toPaddedString:B,toJSON:F,abs:I,round:H,ceil:G,floor:C}})());function $R(C,A,B){return new ObjectRange(C,A,B)}var ObjectRange=Class.create(Enumerable,(function(){function B(F,D,E){this.start=F;this.end=D;this.exclusive=E}function C(D){var E=this.start;while(this.include(E)){D(E);E=E.succ()}}function A(D){if(D<this.start){return false}if(this.exclusive){return D<this.end}return D<=this.end}return{initialize:B,_each:C,include:A}})());var Ajax={getTransport:function(){return Try.these(function(){return new XMLHttpRequest()},function(){return new ActiveXObject("Msxml2.XMLHTTP")},function(){return new ActiveXObject("Microsoft.XMLHTTP")})||false},activeRequestCount:0};Ajax.Responders={responders:[],_each:function(A){this.responders._each(A)},register:function(A){if(!this.include(A)){this.responders.push(A)}},unregister:function(A){this.responders=this.responders.without(A)},dispatch:function(D,B,C,A){this.each(function(E){if(Object.isFunction(E[D])){try{E[D].apply(E,[B,C,A])}catch(F){}}})}};Object.extend(Ajax.Responders,Enumerable);Ajax.Responders.register({onCreate:function(){Ajax.activeRequestCount++},onComplete:function(){Ajax.activeRequestCount--}});Ajax.Base=Class.create({initialize:function(A){this.options={method:"post",asynchronous:true,contentType:"application/x-www-form-urlencoded",encoding:"UTF-8",parameters:"",evalJSON:true,evalJS:true};Object.extend(this.options,A||{});this.options.method=this.options.method.toLowerCase();if(Object.isString(this.options.parameters)){this.options.parameters=this.options.parameters.toQueryParams()}else{if(Object.isHash(this.options.parameters)){this.options.parameters=this.options.parameters.toObject()}}}});Ajax.Request=Class.create(Ajax.Base,{_complete:false,initialize:function($super,B,A){$super(A);this.transport=Ajax.getTransport();this.request(B)},request:function(B){this.url=B;this.method=this.options.method;var D=Object.clone(this.options.parameters);if(!["get","post"].include(this.method)){D._method=this.method;this.method="post"}this.parameters=D;if(D=Object.toQueryString(D)){if(this.method=="get"){this.url+=(this.url.include("?")?"&":"?")+D}else{if(/Konqueror|Safari|KHTML/.test(navigator.userAgent)){D+="&_="}}}try{var A=new Ajax.Response(this);if(this.options.onCreate){this.options.onCreate(A)}Ajax.Responders.dispatch("onCreate",this,A);this.transport.open(this.method.toUpperCase(),this.url,this.options.asynchronous);if(this.options.asynchronous){this.respondToReadyState.bind(this).defer(1)}this.transport.onreadystatechange=this.onStateChange.bind(this);this.setRequestHeaders();this.body=this.method=="post"?(this.options.postBody||D):null;this.transport.send(this.body);if(!this.options.asynchronous&&this.transport.overrideMimeType){this.onStateChange()}}catch(C){this.dispatchException(C)}},onStateChange:function(){var A=this.transport.readyState;if(A>1&&!((A==4)&&this._complete)){this.respondToReadyState(this.transport.readyState)}},setRequestHeaders:function(){var E={"X-Requested-With":"XMLHttpRequest","X-Prototype-Version":Prototype.Version,Accept:"text/javascript, text/html, application/xml, text/xml, */*"};if(this.method=="post"){E["Content-type"]=this.options.contentType+(this.options.encoding?"; charset="+this.options.encoding:"");if(this.transport.overrideMimeType&&(navigator.userAgent.match(/Gecko\/(\d{4})/)||[0,2005])[1]<2005){E.Connection="close"}}if(typeof this.options.requestHeaders=="object"){var C=this.options.requestHeaders;if(Object.isFunction(C.push)){for(var B=0,D=C.length;B<D;B+=2){E[C[B]]=C[B+1]}}else{$H(C).each(function(F){E[F.key]=F.value})}}for(var A in E){this.transport.setRequestHeader(A,E[A])}},success:function(){var A=this.getStatus();return !A||(A>=200&&A<300)},getStatus:function(){try{return this.transport.status||0}catch(A){return 0}},respondToReadyState:function(A){var C=Ajax.Request.Events[A],B=new Ajax.Response(this);if(C=="Complete"){try{this._complete=true;(this.options["on"+B.status]||this.options["on"+(this.success()?"Success":"Failure")]||Prototype.emptyFunction)(B,B.headerJSON)}catch(D){this.dispatchException(D)}var E=B.getHeader("Content-type");if(this.options.evalJS=="force"||(this.options.evalJS&&this.isSameOrigin()&&E&&E.match(/^\s*(text|application)\/(x-)?(java|ecma)script(;.*)?\s*$/i))){this.evalResponse()}}try{(this.options["on"+C]||Prototype.emptyFunction)(B,B.headerJSON);Ajax.Responders.dispatch("on"+C,this,B,B.headerJSON)}catch(D){this.dispatchException(D)}if(C=="Complete"){this.transport.onreadystatechange=Prototype.emptyFunction}},isSameOrigin:function(){var A=this.url.match(/^\s*https?:\/\/[^\/]*/);return !A||(A[0]=="#{protocol}//#{domain}#{port}".interpolate({protocol:location.protocol,domain:document.domain,port:location.port?":"+location.port:""}))},getHeader:function(A){try{return this.transport.getResponseHeader(A)||null}catch(B){return null}},evalResponse:function(){try{return eval((this.transport.responseText||"").unfilterJSON())}catch(e){this.dispatchException(e)}},dispatchException:function(A){(this.options.onException||Prototype.emptyFunction)(this,A);Ajax.Responders.dispatch("onException",this,A)}});Ajax.Request.Events=["Uninitialized","Loading","Loaded","Interactive","Complete"];Ajax.Response=Class.create({initialize:function(C){this.request=C;var D=this.transport=C.transport,A=this.readyState=D.readyState;if((A>2&&!Prototype.Browser.IE)||A==4){this.status=this.getStatus();this.statusText=this.getStatusText();this.responseText=String.interpret(D.responseText);this.headerJSON=this._getHeaderJSON()}if(A==4){var B=D.responseXML;this.responseXML=Object.isUndefined(B)?null:B;this.responseJSON=this._getResponseJSON()}},status:0,statusText:"",getStatus:Ajax.Request.prototype.getStatus,getStatusText:function(){try{return this.transport.statusText||""}catch(A){return""}},getHeader:Ajax.Request.prototype.getHeader,getAllHeaders:function(){try{return this.getAllResponseHeaders()}catch(A){return null}},getResponseHeader:function(A){return this.transport.getResponseHeader(A)},getAllResponseHeaders:function(){return this.transport.getAllResponseHeaders()},_getHeaderJSON:function(){var A=this.getHeader("X-JSON");if(!A){return null}A=decodeURIComponent(escape(A));try{return A.evalJSON(this.request.options.sanitizeJSON||!this.request.isSameOrigin())}catch(B){this.request.dispatchException(B)}},_getResponseJSON:function(){var A=this.request.options;if(!A.evalJSON||(A.evalJSON!="force"&&!(this.getHeader("Content-type")||"").include("application/json"))||this.responseText.blank()){return null}try{return this.responseText.evalJSON(A.sanitizeJSON||!this.request.isSameOrigin())}catch(B){this.request.dispatchException(B)}}});Ajax.Updater=Class.create(Ajax.Request,{initialize:function($super,A,C,B){this.container={success:(A.success||A),failure:(A.failure||(A.success?null:A))};B=Object.clone(B);var D=B.onComplete;B.onComplete=(function(E,F){this.updateContent(E.responseText);if(Object.isFunction(D)){D(E,F)}}).bind(this);$super(C,B)},updateContent:function(D){var C=this.container[this.success()?"success":"failure"],A=this.options;if(!A.evalScripts){D=D.stripScripts()}if(C=$(C)){if(A.insertion){if(Object.isString(A.insertion)){var B={};B[A.insertion]=D;C.insert(B)}else{A.insertion(C,D)}}else{C.update(D)}}}});Ajax.PeriodicalUpdater=Class.create(Ajax.Base,{initialize:function($super,A,C,B){$super(B);this.onComplete=this.options.onComplete;this.frequency=(this.options.frequency||2);this.decay=(this.options.decay||1);this.updater={};this.container=A;this.url=C;this.start()},start:function(){this.options.onComplete=this.updateComplete.bind(this);this.onTimerEvent()},stop:function(){this.updater.options.onComplete=undefined;clearTimeout(this.timer);(this.onComplete||Prototype.emptyFunction).apply(this,arguments)},updateComplete:function(A){if(this.options.decay){this.decay=(A.responseText==this.lastText?this.decay*this.options.decay:1);this.lastText=A.responseText}this.timer=this.onTimerEvent.bind(this).delay(this.decay*this.frequency)},onTimerEvent:function(){this.updater=new Ajax.Updater(this.container,this.url,this.options)}});function $(B){if(arguments.length>1){for(var A=0,D=[],C=arguments.length;A<C;A++){D.push($(arguments[A]))}return D}if(Object.isString(B)){B=document.getElementById(B)}return Element.extend(B)}if(Prototype.BrowserFeatures.XPath){document._getElementsByXPath=function(F,A){var C=[];var E=document.evaluate(F,$(A)||document,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);for(var B=0,D=E.snapshotLength;B<D;B++){C.push(Element.extend(E.snapshotItem(B)))}return C}}if(!window.Node){var Node={}}if(!Node.ELEMENT_NODE){Object.extend(Node,{ELEMENT_NODE:1,ATTRIBUTE_NODE:2,TEXT_NODE:3,CDATA_SECTION_NODE:4,ENTITY_REFERENCE_NODE:5,ENTITY_NODE:6,PROCESSING_INSTRUCTION_NODE:7,COMMENT_NODE:8,DOCUMENT_NODE:9,DOCUMENT_TYPE_NODE:10,DOCUMENT_FRAGMENT_NODE:11,NOTATION_NODE:12})}(function(C){var B=(function(){var F=document.createElement("form");var E=document.createElement("input");var D=document.documentElement;E.setAttribute("name","test");F.appendChild(E);D.appendChild(F);var G=F.elements?(typeof F.elements.test=="undefined"):null;D.removeChild(F);F=E=null;return G})();var A=C.Element;C.Element=function(F,E){E=E||{};F=F.toLowerCase();var D=Element.cache;if(B&&E.name){F="<"+F+' name="'+E.name+'">';delete E.name;return Element.writeAttribute(document.createElement(F),E)}if(!D[F]){D[F]=Element.extend(document.createElement(F))}return Element.writeAttribute(D[F].cloneNode(false),E)};Object.extend(C.Element,A||{});if(A){C.Element.prototype=A.prototype}})(this);Element.cache={};Element.idCounter=1;Element.Methods={visible:function(A){return $(A).style.display!="none"},toggle:function(A){A=$(A);Element[Element.visible(A)?"hide":"show"](A);return A},hide:function(A){A=$(A);A.style.display="none";return A},show:function(A){A=$(A);A.style.display="";return A},remove:function(A){A=$(A);A.parentNode.removeChild(A);return A},update:(function(){var B=(function(){var E=document.createElement("select"),F=true;E.innerHTML='<option value="test">test</option>';if(E.options&&E.options[0]){F=E.options[0].nodeName.toUpperCase()!=="OPTION"}E=null;return F})();var A=(function(){try{var E=document.createElement("table");if(E&&E.tBodies){E.innerHTML="<tbody><tr><td>test</td></tr></tbody>";var G=typeof E.tBodies[0]=="undefined";E=null;return G}}catch(F){return true}})();var D=(function(){var E=document.createElement("script"),G=false;try{E.appendChild(document.createTextNode(""));G=!E.firstChild||E.firstChild&&E.firstChild.nodeType!==3}catch(F){G=true}E=null;return G})();function C(F,G){F=$(F);if(G&&G.toElement){G=G.toElement()}if(Object.isElement(G)){return F.update().insert(G)}G=Object.toHTML(G);var E=F.tagName.toUpperCase();if(E==="SCRIPT"&&D){F.text=G;return F}if(B||A){if(E in Element._insertionTranslations.tags){while(F.firstChild){F.removeChild(F.firstChild)}Element._getContentFromAnonymousElement(E,G.stripScripts()).each(function(H){F.appendChild(H)})}else{F.innerHTML=G.stripScripts()}}else{F.innerHTML=G.stripScripts()}G.evalScripts.bind(G).defer();return F}return C})(),replace:function(B,C){B=$(B);if(C&&C.toElement){C=C.toElement()}else{if(!Object.isElement(C)){C=Object.toHTML(C);var A=B.ownerDocument.createRange();A.selectNode(B);C.evalScripts.bind(C).defer();C=A.createContextualFragment(C.stripScripts())}}B.parentNode.replaceChild(C,B);return B},insert:function(C,E){C=$(C);if(Object.isString(E)||Object.isNumber(E)||Object.isElement(E)||(E&&(E.toElement||E.toHTML))){E={bottom:E}}var D,F,B,G;for(var A in E){D=E[A];A=A.toLowerCase();F=Element._insertionTranslations[A];if(D&&D.toElement){D=D.toElement()}if(Object.isElement(D)){F(C,D);continue}D=Object.toHTML(D);B=((A=="before"||A=="after")?C.parentNode:C).tagName.toUpperCase();G=Element._getContentFromAnonymousElement(B,D.stripScripts());if(A=="top"||A=="after"){G.reverse()}G.each(F.curry(C));D.evalScripts.bind(D).defer()}return C},wrap:function(B,C,A){B=$(B);if(Object.isElement(C)){$(C).writeAttribute(A||{})}else{if(Object.isString(C)){C=new Element(C,A)}else{C=new Element("div",C)}}if(B.parentNode){B.parentNode.replaceChild(C,B)}C.appendChild(B);return C},inspect:function(B){B=$(B);var A="<"+B.tagName.toLowerCase();$H({id:"id",className:"class"}).each(function(F){var E=F.first(),C=F.last();var D=(B[E]||"").toString();if(D){A+=" "+C+"="+D.inspect(true)}});return A+">"},recursivelyCollect:function(A,C){A=$(A);var B=[];while(A=A[C]){if(A.nodeType==1){B.push(Element.extend(A))}}return B},ancestors:function(A){return Element.recursivelyCollect(A,"parentNode")},descendants:function(A){return Element.select(A,"*")},firstDescendant:function(A){A=$(A).firstChild;while(A&&A.nodeType!=1){A=A.nextSibling}return $(A)},immediateDescendants:function(A){if(!(A=$(A).firstChild)){return[]}while(A&&A.nodeType!=1){A=A.nextSibling}if(A){return[A].concat($(A).nextSiblings())}return[]},previousSiblings:function(A){return Element.recursivelyCollect(A,"previousSibling")},nextSiblings:function(A){return Element.recursivelyCollect(A,"nextSibling")},siblings:function(A){A=$(A);return Element.previousSiblings(A).reverse().concat(Element.nextSiblings(A))},match:function(B,A){if(Object.isString(A)){A=new Selector(A)}return A.match($(B))},up:function(B,D,A){B=$(B);if(arguments.length==1){return $(B.parentNode)}var C=Element.ancestors(B);return Object.isNumber(D)?C[D]:Selector.findElement(C,D,A)},down:function(B,C,A){B=$(B);if(arguments.length==1){return Element.firstDescendant(B)}return Object.isNumber(C)?Element.descendants(B)[C]:Element.select(B,C)[A||0]},previous:function(B,D,A){B=$(B);if(arguments.length==1){return $(Selector.handlers.previousElementSibling(B))}var C=Element.previousSiblings(B);return Object.isNumber(D)?C[D]:Selector.findElement(C,D,A)},next:function(C,D,B){C=$(C);if(arguments.length==1){return $(Selector.handlers.nextElementSibling(C))}var A=Element.nextSiblings(C);return Object.isNumber(D)?A[D]:Selector.findElement(A,D,B)},select:function(B){var A=Array.prototype.slice.call(arguments,1);return Selector.findChildElements(B,A)},adjacent:function(B){var A=Array.prototype.slice.call(arguments,1);return Selector.findChildElements(B.parentNode,A).without(B)},identify:function(A){A=$(A);var B=Element.readAttribute(A,"id");if(B){return B}do{B="anonymous_element_"+Element.idCounter++}while($(B));Element.writeAttribute(A,"id",B);return B},readAttribute:function(C,A){C=$(C);if(Prototype.Browser.IE){var B=Element._attributeTranslations.read;if(B.values[A]){return B.values[A](C,A)}if(B.names[A]){A=B.names[A]}if(A.include(":")){return(!C.attributes||!C.attributes[A])?null:C.attributes[A].value}}return C.getAttribute(A)},writeAttribute:function(E,C,F){E=$(E);var B={},D=Element._attributeTranslations.write;if(typeof C=="object"){B=C}else{B[C]=Object.isUndefined(F)?true:F}for(var A in B){C=D.names[A]||A;F=B[A];if(D.values[A]){C=D.values[A](E,F)}if(F===false||F===null){E.removeAttribute(C)}else{if(F===true){E.setAttribute(C,C)}else{E.setAttribute(C,F)}}}return E},getHeight:function(A){return Element.getDimensions(A).height},getWidth:function(A){return Element.getDimensions(A).width},classNames:function(A){return new Element.ClassNames(A)},hasClassName:function(A,B){if(!(A=$(A))){return }var C=A.className;return(C.length>0&&(C==B||new RegExp("(^|\\s)"+B+"(\\s|$)").test(C)))},addClassName:function(A,B){if(!(A=$(A))){return }if(!Element.hasClassName(A,B)){A.className+=(A.className?" ":"")+B}return A},removeClassName:function(A,B){if(!(A=$(A))){return }A.className=A.className.replace(new RegExp("(^|\\s+)"+B+"(\\s+|$)")," ").strip();return A},toggleClassName:function(A,B){if(!(A=$(A))){return }return Element[Element.hasClassName(A,B)?"removeClassName":"addClassName"](A,B)},cleanWhitespace:function(B){B=$(B);var C=B.firstChild;while(C){var A=C.nextSibling;if(C.nodeType==3&&!/\S/.test(C.nodeValue)){B.removeChild(C)}C=A}return B},empty:function(A){return $(A).innerHTML.blank()},descendantOf:function(B,A){B=$(B),A=$(A);if(B.compareDocumentPosition){return(B.compareDocumentPosition(A)&8)===8}if(A.contains){return A.contains(B)&&A!==B}while(B=B.parentNode){if(B==A){return true}}return false},scrollTo:function(A){A=$(A);var B=Element.cumulativeOffset(A);window.scrollTo(B[0],B[1]);return A},getStyle:function(B,C){B=$(B);C=C=="float"?"cssFloat":C.camelize();var D=B.style[C];if(!D||D=="auto"){var A=document.defaultView.getComputedStyle(B,null);D=A?A[C]:null}if(C=="opacity"){return D?parseFloat(D):1}return D=="auto"?null:D},getOpacity:function(A){return $(A).getStyle("opacity")},setStyle:function(B,C){B=$(B);var E=B.style,A;if(Object.isString(C)){B.style.cssText+=";"+C;return C.include("opacity")?B.setOpacity(C.match(/opacity:\s*(\d?\.?\d*)/)[1]):B}for(var D in C){if(D=="opacity"){B.setOpacity(C[D])}else{E[(D=="float"||D=="cssFloat")?(Object.isUndefined(E.styleFloat)?"cssFloat":"styleFloat"):D]=C[D]}}return B},setOpacity:function(A,B){A=$(A);A.style.opacity=(B==1||B==="")?"":(B<0.00001)?0:B;return A},getDimensions:function(C){C=$(C);var G=Element.getStyle(C,"display");if(G!="none"&&G!=null){return{width:C.offsetWidth,height:C.offsetHeight}}var B=C.style;var F=B.visibility;var D=B.position;var A=B.display;B.visibility="hidden";if(D!="fixed"){B.position="absolute"}B.display="block";var H=C.clientWidth;var E=C.clientHeight;B.display=A;B.position=D;B.visibility=F;return{width:H,height:E}},makePositioned:function(A){A=$(A);var B=Element.getStyle(A,"position");if(B=="static"||!B){A._madePositioned=true;A.style.position="relative";if(Prototype.Browser.Opera){A.style.top=0;A.style.left=0}}return A},undoPositioned:function(A){A=$(A);if(A._madePositioned){A._madePositioned=undefined;A.style.position=A.style.top=A.style.left=A.style.bottom=A.style.right=""}return A},makeClipping:function(A){A=$(A);if(A._overflow){return A}A._overflow=Element.getStyle(A,"overflow")||"auto";if(A._overflow!=="hidden"){A.style.overflow="hidden"}return A},undoClipping:function(A){A=$(A);if(!A._overflow){return A}A.style.overflow=A._overflow=="auto"?"":A._overflow;A._overflow=null;return A},cumulativeOffset:function(B){var A=0,C=0;do{A+=B.offsetTop||0;C+=B.offsetLeft||0;B=B.offsetParent}while(B);return Element._returnOffset(C,A)},positionedOffset:function(B){var A=0,D=0;do{A+=B.offsetTop||0;D+=B.offsetLeft||0;B=B.offsetParent;if(B){if(B.tagName.toUpperCase()=="BODY"){break}var C=Element.getStyle(B,"position");if(C!=="static"){break}}}while(B);return Element._returnOffset(D,A)},absolutize:function(B){B=$(B);if(Element.getStyle(B,"position")=="absolute"){return B}var D=Element.positionedOffset(B);var F=D[1];var E=D[0];var C=B.clientWidth;var A=B.clientHeight;B._originalLeft=E-parseFloat(B.style.left||0);B._originalTop=F-parseFloat(B.style.top||0);B._originalWidth=B.style.width;B._originalHeight=B.style.height;B.style.position="absolute";B.style.top=F+"px";B.style.left=E+"px";B.style.width=C+"px";B.style.height=A+"px";return B},relativize:function(A){A=$(A);if(Element.getStyle(A,"position")=="relative"){return A}A.style.position="relative";var C=parseFloat(A.style.top||0)-(A._originalTop||0);var B=parseFloat(A.style.left||0)-(A._originalLeft||0);A.style.top=C+"px";A.style.left=B+"px";A.style.height=A._originalHeight;A.style.width=A._originalWidth;return A},cumulativeScrollOffset:function(B){var A=0,C=0;do{A+=B.scrollTop||0;C+=B.scrollLeft||0;B=B.parentNode}while(B);return Element._returnOffset(C,A)},getOffsetParent:function(A){if(A.offsetParent){return $(A.offsetParent)}if(A==document.body){return $(A)}while((A=A.parentNode)&&A!=document.body){if(Element.getStyle(A,"position")!="static"){return $(A)}}return $(document.body)},viewportOffset:function(D){var A=0,C=0;var B=D;do{A+=B.offsetTop||0;C+=B.offsetLeft||0;if(B.offsetParent==document.body&&Element.getStyle(B,"position")=="absolute"){break}}while(B=B.offsetParent);B=D;do{if(!Prototype.Browser.Opera||(B.tagName&&(B.tagName.toUpperCase()=="BODY"))){A-=B.scrollTop||0;C-=B.scrollLeft||0}}while(B=B.parentNode);return Element._returnOffset(C,A)},clonePosition:function(B,D){var A=Object.extend({setLeft:true,setTop:true,setWidth:true,setHeight:true,offsetTop:0,offsetLeft:0},arguments[2]||{});D=$(D);var E=Element.viewportOffset(D);B=$(B);var F=[0,0];var C=null;if(Element.getStyle(B,"position")=="absolute"){C=Element.getOffsetParent(B);F=Element.viewportOffset(C)}if(C==document.body){F[0]-=document.body.offsetLeft;F[1]-=document.body.offsetTop}if(A.setLeft){B.style.left=(E[0]-F[0]+A.offsetLeft)+"px"}if(A.setTop){B.style.top=(E[1]-F[1]+A.offsetTop)+"px"}if(A.setWidth){B.style.width=D.offsetWidth+"px"}if(A.setHeight){B.style.height=D.offsetHeight+"px"}return B}};Object.extend(Element.Methods,{getElementsBySelector:Element.Methods.select,childElements:Element.Methods.immediateDescendants});Element._attributeTranslations={write:{names:{className:"class",htmlFor:"for"},values:{}}};if(Prototype.Browser.Opera){Element.Methods.getStyle=Element.Methods.getStyle.wrap(function(D,B,C){switch(C){case"left":case"top":case"right":case"bottom":if(D(B,"position")==="static"){return null}case"height":case"width":if(!Element.visible(B)){return null}var E=parseInt(D(B,C),10);if(E!==B["offset"+C.capitalize()]){return E+"px"}var A;if(C==="height"){A=["border-top-width","padding-top","padding-bottom","border-bottom-width"]}else{A=["border-left-width","padding-left","padding-right","border-right-width"]}return A.inject(E,function(F,G){var H=D(B,G);return H===null?F:F-parseInt(H,10)})+"px";default:return D(B,C)}});Element.Methods.readAttribute=Element.Methods.readAttribute.wrap(function(C,A,B){if(B==="title"){return A.title}return C(A,B)})}else{if(Prototype.Browser.IE){Element.Methods.getOffsetParent=Element.Methods.getOffsetParent.wrap(function(C,B){B=$(B);try{B.offsetParent}catch(E){return $(document.body)}var A=B.getStyle("position");if(A!=="static"){return C(B)}B.setStyle({position:"relative"});var D=C(B);B.setStyle({position:A});return D});$w("positionedOffset viewportOffset").each(function(A){Element.Methods[A]=Element.Methods[A].wrap(function(E,C){C=$(C);try{C.offsetParent}catch(G){return Element._returnOffset(0,0)}var B=C.getStyle("position");if(B!=="static"){return E(C)}var D=C.getOffsetParent();if(D&&D.getStyle("position")==="fixed"){D.setStyle({zoom:1})}C.setStyle({position:"relative"});var F=E(C);C.setStyle({position:B});return F})});Element.Methods.cumulativeOffset=Element.Methods.cumulativeOffset.wrap(function(B,A){try{A.offsetParent}catch(C){return Element._returnOffset(0,0)}return B(A)});Element.Methods.getStyle=function(A,B){A=$(A);B=(B=="float"||B=="cssFloat")?"styleFloat":B.camelize();var C=A.style[B];if(!C&&A.currentStyle){C=A.currentStyle[B]}if(B=="opacity"){if(C=(A.getStyle("filter")||"").match(/alpha\(opacity=(.*)\)/)){if(C[1]){return parseFloat(C[1])/100}}return 1}if(C=="auto"){if((B=="width"||B=="height")&&(A.getStyle("display")!="none")){return A["offset"+B.capitalize()]+"px"}return null}return C};Element.Methods.setOpacity=function(B,E){function F(G){return G.replace(/alpha\([^\)]*\)/gi,"")}B=$(B);var A=B.currentStyle;if((A&&!A.hasLayout)||(!A&&B.style.zoom=="normal")){B.style.zoom=1}var D=B.getStyle("filter"),C=B.style;if(E==1||E===""){(D=F(D))?C.filter=D:C.removeAttribute("filter");return B}else{if(E<0.00001){E=0}}C.filter=F(D)+"alpha(opacity="+(E*100)+")";return B};Element._attributeTranslations=(function(){var B="className";var A="for";var C=document.createElement("div");C.setAttribute(B,"x");if(C.className!=="x"){C.setAttribute("class","x");if(C.className==="x"){B="class"}}C=null;C=document.createElement("label");C.setAttribute(A,"x");if(C.htmlFor!=="x"){C.setAttribute("htmlFor","x");if(C.htmlFor==="x"){A="htmlFor"}}C=null;return{read:{names:{"class":B,className:B,"for":A,htmlFor:A},values:{_getAttr:function(D,E){return D.getAttribute(E)},_getAttr2:function(D,E){return D.getAttribute(E,2)},_getAttrNode:function(D,F){var E=D.getAttributeNode(F);return E?E.value:""},_getEv:(function(){var D=document.createElement("div");D.onclick=Prototype.emptyFunction;var F=D.getAttribute("onclick");var E;if(String(F).indexOf("{")>-1){E=function(G,H){H=G.getAttribute(H);if(!H){return null}H=H.toString();H=H.split("{")[1];H=H.split("}")[0];return H.strip()}}else{if(F===""){E=function(G,H){H=G.getAttribute(H);if(!H){return null}return H.strip()}}}D=null;return E})(),_flag:function(D,E){return $(D).hasAttribute(E)?E:null},style:function(D){return D.style.cssText.toLowerCase()},title:function(D){return D.title}}}}})();Element._attributeTranslations.write={names:Object.extend({cellpadding:"cellPadding",cellspacing:"cellSpacing"},Element._attributeTranslations.read.names),values:{checked:function(A,B){A.checked=!!B},style:function(A,B){A.style.cssText=B?B:""}}};Element._attributeTranslations.has={};$w("colSpan rowSpan vAlign dateTime accessKey tabIndex encType maxLength readOnly longDesc frameBorder").each(function(A){Element._attributeTranslations.write.names[A.toLowerCase()]=A;Element._attributeTranslations.has[A.toLowerCase()]=A});(function(A){Object.extend(A,{href:A._getAttr2,src:A._getAttr2,type:A._getAttr,action:A._getAttrNode,disabled:A._flag,checked:A._flag,readonly:A._flag,multiple:A._flag,onload:A._getEv,onunload:A._getEv,onclick:A._getEv,ondblclick:A._getEv,onmousedown:A._getEv,onmouseup:A._getEv,onmouseover:A._getEv,onmousemove:A._getEv,onmouseout:A._getEv,onfocus:A._getEv,onblur:A._getEv,onkeypress:A._getEv,onkeydown:A._getEv,onkeyup:A._getEv,onsubmit:A._getEv,onreset:A._getEv,onselect:A._getEv,onchange:A._getEv})})(Element._attributeTranslations.read.values);if(Prototype.BrowserFeatures.ElementExtensions){(function(){function A(E){var B=E.getElementsByTagName("*"),D=[];for(var C=0,F;F=B[C];C++){if(F.tagName!=="!"){D.push(F)}}return D}Element.Methods.down=function(C,D,B){C=$(C);if(arguments.length==1){return C.firstDescendant()}return Object.isNumber(D)?A(C)[D]:Element.select(C,D)[B||0]}})()}}else{if(Prototype.Browser.Gecko&&/rv:1\.8\.0/.test(navigator.userAgent)){Element.Methods.setOpacity=function(A,B){A=$(A);A.style.opacity=(B==1)?0.999999:(B==="")?"":(B<0.00001)?0:B;return A}}else{if(Prototype.Browser.WebKit){Element.Methods.setOpacity=function(A,B){A=$(A);A.style.opacity=(B==1||B==="")?"":(B<0.00001)?0:B;if(B==1){if(A.tagName.toUpperCase()=="IMG"&&A.width){A.width++;A.width--}else{try{var D=document.createTextNode(" ");A.appendChild(D);A.removeChild(D)}catch(C){}}}return A};Element.Methods.cumulativeOffset=function(B){var A=0,C=0;do{A+=B.offsetTop||0;C+=B.offsetLeft||0;if(B.offsetParent==document.body){if(Element.getStyle(B,"position")=="absolute"){break}}B=B.offsetParent}while(B);return Element._returnOffset(C,A)}}}}}if("outerHTML" in document.documentElement){Element.Methods.replace=function(C,E){C=$(C);if(E&&E.toElement){E=E.toElement()}if(Object.isElement(E)){C.parentNode.replaceChild(E,C);return C}E=Object.toHTML(E);var D=C.parentNode,B=D.tagName.toUpperCase();if(Element._insertionTranslations.tags[B]){var F=C.next();var A=Element._getContentFromAnonymousElement(B,E.stripScripts());D.removeChild(C);if(F){A.each(function(G){D.insertBefore(G,F)})}else{A.each(function(G){D.appendChild(G)})}}else{C.outerHTML=E.stripScripts()}E.evalScripts.bind(E).defer();return C}}Element._returnOffset=function(B,C){var A=[B,C];A.left=B;A.top=C;return A};Element._getContentFromAnonymousElement=function(C,B){var D=new Element("div"),A=Element._insertionTranslations.tags[C];if(A){D.innerHTML=A[0]+B+A[1];A[2].times(function(){D=D.firstChild})}else{D.innerHTML=B}return $A(D.childNodes)};Element._insertionTranslations={before:function(A,B){A.parentNode.insertBefore(B,A)},top:function(A,B){A.insertBefore(B,A.firstChild)},bottom:function(A,B){A.appendChild(B)},after:function(A,B){A.parentNode.insertBefore(B,A.nextSibling)},tags:{TABLE:["<table>","</table>",1],TBODY:["<table><tbody>","</tbody></table>",2],TR:["<table><tbody><tr>","</tr></tbody></table>",3],TD:["<table><tbody><tr><td>","</td></tr></tbody></table>",4],SELECT:["<select>","</select>",1]}};(function(){var A=Element._insertionTranslations.tags;Object.extend(A,{THEAD:A.TBODY,TFOOT:A.TBODY,TH:A.TD})})();Element.Methods.Simulated={hasAttribute:function(A,C){C=Element._attributeTranslations.has[C]||C;var B=$(A).getAttributeNode(C);return !!(B&&B.specified)}};Element.Methods.ByTag={};Object.extend(Element,Element.Methods);(function(A){if(!Prototype.BrowserFeatures.ElementExtensions&&A.__proto__){window.HTMLElement={};window.HTMLElement.prototype=A.__proto__;Prototype.BrowserFeatures.ElementExtensions=true}A=null})(document.createElement("div"));Element.extend=(function(){function C(G){if(typeof window.Element!="undefined"){var I=window.Element.prototype;if(I){var K="_"+(Math.random()+"").slice(2);var H=document.createElement(G);I[K]="x";var J=(H[K]!=="x");delete I[K];H=null;return J}}return false}function B(H,G){for(var J in G){var I=G[J];if(Object.isFunction(I)&&!(J in H)){H[J]=I.methodize()}}}var D=C("object");if(Prototype.BrowserFeatures.SpecificElementExtensions){if(D){return function(H){if(H&&typeof H._extendedByPrototype=="undefined"){var G=H.tagName;if(G&&(/^(?:object|applet|embed)$/i.test(G))){B(H,Element.Methods);B(H,Element.Methods.Simulated);B(H,Element.Methods.ByTag[G.toUpperCase()])}}return H}}return Prototype.K}var A={},E=Element.Methods.ByTag;var F=Object.extend(function(I){if(!I||typeof I._extendedByPrototype!="undefined"||I.nodeType!=1||I==window){return I}var G=Object.clone(A),H=I.tagName.toUpperCase();if(E[H]){Object.extend(G,E[H])}B(I,G);I._extendedByPrototype=Prototype.emptyFunction;return I},{refresh:function(){if(!Prototype.BrowserFeatures.ElementExtensions){Object.extend(A,Element.Methods);Object.extend(A,Element.Methods.Simulated)}}});F.refresh();return F})();Element.hasAttribute=function(A,B){if(A.hasAttribute){return A.hasAttribute(B)}return Element.Methods.Simulated.hasAttribute(A,B)};Element.addMethods=function(C){var J=Prototype.BrowserFeatures,D=Element.Methods.ByTag;if(!C){Object.extend(Form,Form.Methods);Object.extend(Form.Element,Form.Element.Methods);Object.extend(Element.Methods.ByTag,{FORM:Object.clone(Form.Methods),INPUT:Object.clone(Form.Element.Methods),SELECT:Object.clone(Form.Element.Methods),TEXTAREA:Object.clone(Form.Element.Methods)})}if(arguments.length==2){var B=C;C=arguments[1]}if(!B){Object.extend(Element.Methods,C||{})}else{if(Object.isArray(B)){B.each(H)}else{H(B)}}function H(F){F=F.toUpperCase();if(!Element.Methods.ByTag[F]){Element.Methods.ByTag[F]={}}Object.extend(Element.Methods.ByTag[F],C)}function A(M,L,F){F=F||false;for(var O in M){var N=M[O];if(!Object.isFunction(N)){continue}if(!F||!(O in L)){L[O]=N.methodize()}}}function E(N){var F;var M={OPTGROUP:"OptGroup",TEXTAREA:"TextArea",P:"Paragraph",FIELDSET:"FieldSet",UL:"UList",OL:"OList",DL:"DList",DIR:"Directory",H1:"Heading",H2:"Heading",H3:"Heading",H4:"Heading",H5:"Heading",H6:"Heading",Q:"Quote",INS:"Mod",DEL:"Mod",A:"Anchor",IMG:"Image",CAPTION:"TableCaption",COL:"TableCol",COLGROUP:"TableCol",THEAD:"TableSection",TFOOT:"TableSection",TBODY:"TableSection",TR:"TableRow",TH:"TableCell",TD:"TableCell",FRAMESET:"FrameSet",IFRAME:"IFrame"};if(M[N]){F="HTML"+M[N]+"Element"}if(window[F]){return window[F]}F="HTML"+N+"Element";if(window[F]){return window[F]}F="HTML"+N.capitalize()+"Element";if(window[F]){return window[F]}var L=document.createElement(N);var O=L.__proto__||L.constructor.prototype;L=null;return O}var I=window.HTMLElement?HTMLElement.prototype:Element.prototype;if(J.ElementExtensions){A(Element.Methods,I);A(Element.Methods.Simulated,I,true)}if(J.SpecificElementExtensions){for(var K in Element.Methods.ByTag){var G=E(K);if(Object.isUndefined(G)){continue}A(D[K],G.prototype)}}Object.extend(Element,Element.Methods);delete Element.ByTag;if(Element.extend.refresh){Element.extend.refresh()}Element.cache={}};document.viewport={getDimensions:function(){return{width:this.getWidth(),height:this.getHeight()}},getScrollOffsets:function(){return Element._returnOffset(window.pageXOffset||document.documentElement.scrollLeft||document.body.scrollLeft,window.pageYOffset||document.documentElement.scrollTop||document.body.scrollTop)}};(function(C){var H=Prototype.Browser,F=document,D,E={};function A(){if(H.WebKit&&!F.evaluate){return document}if(H.Opera&&window.parseFloat(window.opera.version())<9.5){return document.body}return document.documentElement}function G(B){if(!D){D=A()}E[B]="client"+B;C["get"+B]=function(){return D[E[B]]};return C["get"+B]()}C.getWidth=G.curry("Width");C.getHeight=G.curry("Height")})(document.viewport);Element.Storage={UID:1};Element.addMethods({getStorage:function(B){if(!(B=$(B))){return }var A;if(B===window){A=0}else{if(typeof B._prototypeUID==="undefined"){B._prototypeUID=[Element.Storage.UID++]}A=B._prototypeUID[0]}if(!Element.Storage[A]){Element.Storage[A]=$H()}return Element.Storage[A]},store:function(B,A,C){if(!(B=$(B))){return }if(arguments.length===2){Element.getStorage(B).update(A)}else{Element.getStorage(B).set(A,C)}return B},retrieve:function(C,B,A){if(!(C=$(C))){return }var E=Element.getStorage(C),D=E.get(B);if(Object.isUndefined(D)){E.set(B,A);D=A}return D},clone:function(C,A){if(!(C=$(C))){return }var E=C.cloneNode(A);E._prototypeUID=void 0;if(A){var D=Element.select(E,"*"),B=D.length;while(B--){D[B]._prototypeUID=void 0}}return Element.extend(E)}});var Selector=Class.create({initialize:function(A){this.expression=A.strip();if(this.shouldUseSelectorsAPI()){this.mode="selectorsAPI"}else{if(this.shouldUseXPath()){this.mode="xpath";this.compileXPathMatcher()}else{this.mode="normal";this.compileMatcher()}}},shouldUseXPath:(function(){var A=(function(){var E=false;if(document.evaluate&&window.XPathResult){var D=document.createElement("div");D.innerHTML="<ul><li></li></ul><div><ul><li></li></ul></div>";var C=".//*[local-name()='ul' or local-name()='UL']//*[local-name()='li' or local-name()='LI']";var B=document.evaluate(C,D,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null);E=(B.snapshotLength!==2);D=null}return E})();return function(){if(!Prototype.BrowserFeatures.XPath){return false}var B=this.expression;if(Prototype.Browser.WebKit&&(B.include("-of-type")||B.include(":empty"))){return false}if((/(\[[\w-]*?:|:checked)/).test(B)){return false}if(A){return false}return true}})(),shouldUseSelectorsAPI:function(){if(!Prototype.BrowserFeatures.SelectorsAPI){return false}if(Selector.CASE_INSENSITIVE_CLASS_NAMES){return false}if(!Selector._div){Selector._div=new Element("div")}try{Selector._div.querySelector(this.expression)}catch(A){return false}return true},compileMatcher:function(){var e=this.expression,ps=Selector.patterns,h=Selector.handlers,c=Selector.criteria,le,p,m,len=ps.length,name;if(Selector._cache[e]){this.matcher=Selector._cache[e];return }this.matcher=["this.matcher = function(root) {","var r = root, h = Selector.handlers, c = false, n;"];while(e&&le!=e&&(/\S/).test(e)){le=e;for(var i=0;i<len;i++){p=ps[i].re;name=ps[i].name;if(m=e.match(p)){this.matcher.push(Object.isFunction(c[name])?c[name](m):new Template(c[name]).evaluate(m));e=e.replace(m[0],"");break}}}this.matcher.push("return h.unique(n);\n}");eval(this.matcher.join("\n"));Selector._cache[this.expression]=this.matcher},compileXPathMatcher:function(){var G=this.expression,H=Selector.patterns,C=Selector.xpath,F,B,A=H.length,D;if(Selector._cache[G]){this.xpath=Selector._cache[G];return }this.matcher=[".//*"];while(G&&F!=G&&(/\S/).test(G)){F=G;for(var E=0;E<A;E++){D=H[E].name;if(B=G.match(H[E].re)){this.matcher.push(Object.isFunction(C[D])?C[D](B):new Template(C[D]).evaluate(B));G=G.replace(B[0],"");break}}}this.xpath=this.matcher.join("");Selector._cache[this.expression]=this.xpath},findElements:function(A){A=A||document;var C=this.expression,B;switch(this.mode){case"selectorsAPI":if(A!==document){var D=A.id,E=$(A).identify();E=E.replace(/([\.:])/g,"\\$1");C="#"+E+" "+C}B=$A(A.querySelectorAll(C)).map(Element.extend);A.id=D;return B;case"xpath":return document._getElementsByXPath(this.xpath,A);default:return this.matcher(A)}},match:function(I){this.tokens=[];var M=this.expression,A=Selector.patterns,E=Selector.assertions;var B,D,F,L=A.length,C;while(M&&B!==M&&(/\S/).test(M)){B=M;for(var H=0;H<L;H++){D=A[H].re;C=A[H].name;if(F=M.match(D)){if(E[C]){this.tokens.push([C,Object.clone(F)]);M=M.replace(F[0],"")}else{return this.findElements(document).include(I)}}}}var K=true,C,J;for(var H=0,G;G=this.tokens[H];H++){C=G[0],J=G[1];if(!Selector.assertions[C](I,J)){K=false;break}}return K},toString:function(){return this.expression},inspect:function(){return"#<Selector:"+this.expression.inspect()+">"}});if(Prototype.BrowserFeatures.SelectorsAPI&&document.compatMode==="BackCompat"){Selector.CASE_INSENSITIVE_CLASS_NAMES=(function(){var C=document.createElement("div"),A=document.createElement("span");C.id="prototype_test_id";A.className="Test";C.appendChild(A);var B=(C.querySelector("#prototype_test_id .test")!==null);C=A=null;return B})()}Object.extend(Selector,{_cache:{},xpath:{descendant:"//*",child:"/*",adjacent:"/following-sibling::*[1]",laterSibling:"/following-sibling::*",tagName:function(A){if(A[1]=="*"){return""}return"[local-name()='"+A[1].toLowerCase()+"' or local-name()='"+A[1].toUpperCase()+"']"},className:"[contains(concat(' ', @class, ' '), ' #{1} ')]",id:"[@id='#{1}']",attrPresence:function(A){A[1]=A[1].toLowerCase();return new Template("[@#{1}]").evaluate(A)},attr:function(A){A[1]=A[1].toLowerCase();A[3]=A[5]||A[6];return new Template(Selector.xpath.operators[A[2]]).evaluate(A)},pseudo:function(A){var B=Selector.xpath.pseudos[A[1]];if(!B){return""}if(Object.isFunction(B)){return B(A)}return new Template(Selector.xpath.pseudos[A[1]]).evaluate(A)},operators:{"=":"[@#{1}='#{3}']","!=":"[@#{1}!='#{3}']","^=":"[starts-with(@#{1}, '#{3}')]","$=":"[substring(@#{1}, (string-length(@#{1}) - string-length('#{3}') + 1))='#{3}']","*=":"[contains(@#{1}, '#{3}')]","~=":"[contains(concat(' ', @#{1}, ' '), ' #{3} ')]","|=":"[contains(concat('-', @#{1}, '-'), '-#{3}-')]"},pseudos:{"first-child":"[not(preceding-sibling::*)]","last-child":"[not(following-sibling::*)]","only-child":"[not(preceding-sibling::* or following-sibling::*)]",empty:"[count(*) = 0 and (count(text()) = 0)]",checked:"[@checked]",disabled:"[(@disabled) and (@type!='hidden')]",enabled:"[not(@disabled) and (@type!='hidden')]",not:function(E){var H=E[6],C=Selector.patterns,I=Selector.xpath,A,J,G=C.length,B;var D=[];while(H&&A!=H&&(/\S/).test(H)){A=H;for(var F=0;F<G;F++){B=C[F].name;if(E=H.match(C[F].re)){J=Object.isFunction(I[B])?I[B](E):new Template(I[B]).evaluate(E);D.push("("+J.substring(1,J.length-1)+")");H=H.replace(E[0],"");break}}}return"[not("+D.join(" and ")+")]"},"nth-child":function(A){return Selector.xpath.pseudos.nth("(count(./preceding-sibling::*) + 1) ",A)},"nth-last-child":function(A){return Selector.xpath.pseudos.nth("(count(./following-sibling::*) + 1) ",A)},"nth-of-type":function(A){return Selector.xpath.pseudos.nth("position() ",A)},"nth-last-of-type":function(A){return Selector.xpath.pseudos.nth("(last() + 1 - position()) ",A)},"first-of-type":function(A){A[6]="1";return Selector.xpath.pseudos["nth-of-type"](A)},"last-of-type":function(A){A[6]="1";return Selector.xpath.pseudos["nth-last-of-type"](A)},"only-of-type":function(A){var B=Selector.xpath.pseudos;return B["first-of-type"](A)+B["last-of-type"](A)},nth:function(E,C){var F,G=C[6],B;if(G=="even"){G="2n+0"}if(G=="odd"){G="2n+1"}if(F=G.match(/^(\d+)$/)){return"["+E+"= "+F[1]+"]"}if(F=G.match(/^(-?\d*)?n(([+-])(\d+))?/)){if(F[1]=="-"){F[1]=-1}var D=F[1]?Number(F[1]):1;var A=F[2]?Number(F[2]):0;B="[((#{fragment} - #{b}) mod #{a} = 0) and ((#{fragment} - #{b}) div #{a} >= 0)]";return new Template(B).evaluate({fragment:E,a:D,b:A})}}}},criteria:{tagName:'n = h.tagName(n, r, "#{1}", c);      c = false;',className:'n = h.className(n, r, "#{1}", c);    c = false;',id:'n = h.id(n, r, "#{1}", c);           c = false;',attrPresence:'n = h.attrPresence(n, r, "#{1}", c); c = false;',attr:function(A){A[3]=(A[5]||A[6]);return new Template('n = h.attr(n, r, "#{1}", "#{3}", "#{2}", c); c = false;').evaluate(A)},pseudo:function(A){if(A[6]){A[6]=A[6].replace(/"/g,'\\"')}return new Template('n = h.pseudo(n, "#{1}", "#{6}", r, c); c = false;').evaluate(A)},descendant:'c = "descendant";',child:'c = "child";',adjacent:'c = "adjacent";',laterSibling:'c = "laterSibling";'},patterns:[{name:"laterSibling",re:/^\s*~\s*/},{name:"child",re:/^\s*>\s*/},{name:"adjacent",re:/^\s*\+\s*/},{name:"descendant",re:/^\s/},{name:"tagName",re:/^\s*(\*|[\w\-]+)(\b|$)?/},{name:"id",re:/^#([\w\-\*]+)(\b|$)/},{name:"className",re:/^\.([\w\-\*]+)(\b|$)/},{name:"pseudo",re:/^:((first|last|nth|nth-last|only)(-child|-of-type)|empty|checked|(en|dis)abled|not)(\((.*?)\))?(\b|$|(?=\s|[:+~>]))/},{name:"attrPresence",re:/^\[((?:[\w-]+:)?[\w-]+)\]/},{name:"attr",re:/\[((?:[\w-]*:)?[\w-]+)\s*(?:([!^$*~|]?=)\s*((['"])([^\4]*?)\4|([^'"][^\]]*?)))?\]/}],assertions:{tagName:function(A,B){return B[1].toUpperCase()==A.tagName.toUpperCase()},className:function(A,B){return Element.hasClassName(A,B[1])},id:function(A,B){return A.id===B[1]},attrPresence:function(A,B){return Element.hasAttribute(A,B[1])},attr:function(B,C){var A=Element.readAttribute(B,C[1]);return A&&Selector.operators[C[2]](A,C[5]||C[6])}},handlers:{concat:function(B,A){for(var C=0,D;D=A[C];C++){B.push(D)}return B},mark:function(A){var D=Prototype.emptyFunction;for(var B=0,C;C=A[B];B++){C._countedByPrototype=D}return A},unmark:(function(){var A=(function(){var B=document.createElement("div"),E=false,D="_countedByPrototype",C="x";B[D]=C;E=(B.getAttribute(D)===C);B=null;return E})();return A?function(B){for(var C=0,D;D=B[C];C++){D.removeAttribute("_countedByPrototype")}return B}:function(B){for(var C=0,D;D=B[C];C++){D._countedByPrototype=void 0}return B}})(),index:function(A,D,G){A._countedByPrototype=Prototype.emptyFunction;if(D){for(var B=A.childNodes,E=B.length-1,C=1;E>=0;E--){var F=B[E];if(F.nodeType==1&&(!G||F._countedByPrototype)){F.nodeIndex=C++}}}else{for(var E=0,C=1,B=A.childNodes;F=B[E];E++){if(F.nodeType==1&&(!G||F._countedByPrototype)){F.nodeIndex=C++}}}},unique:function(B){if(B.length==0){return B}var D=[],E;for(var C=0,A=B.length;C<A;C++){if(typeof (E=B[C])._countedByPrototype=="undefined"){E._countedByPrototype=Prototype.emptyFunction;D.push(Element.extend(E))}}return Selector.handlers.unmark(D)},descendant:function(A){var D=Selector.handlers;for(var C=0,B=[],E;E=A[C];C++){D.concat(B,E.getElementsByTagName("*"))}return B},child:function(A){var E=Selector.handlers;for(var D=0,C=[],F;F=A[D];D++){for(var B=0,G;G=F.childNodes[B];B++){if(G.nodeType==1&&G.tagName!="!"){C.push(G)}}}return C},adjacent:function(A){for(var C=0,B=[],E;E=A[C];C++){var D=this.nextElementSibling(E);if(D){B.push(D)}}return B},laterSibling:function(A){var D=Selector.handlers;for(var C=0,B=[],E;E=A[C];C++){D.concat(B,Element.nextSiblings(E))}return B},nextElementSibling:function(A){while(A=A.nextSibling){if(A.nodeType==1){return A}}return null},previousElementSibling:function(A){while(A=A.previousSibling){if(A.nodeType==1){return A}}return null},tagName:function(A,H,C,B){var I=C.toUpperCase();var E=[],G=Selector.handlers;if(A){if(B){if(B=="descendant"){for(var F=0,D;D=A[F];F++){G.concat(E,D.getElementsByTagName(C))}return E}else{A=this[B](A)}if(C=="*"){return A}}for(var F=0,D;D=A[F];F++){if(D.tagName.toUpperCase()===I){E.push(D)}}return E}else{return H.getElementsByTagName(C)}},id:function(A,I,B,C){var H=$(B),G=Selector.handlers;if(I==document){if(!H){return[]}if(!A){return[H]}}else{if(!I.sourceIndex||I.sourceIndex<1){var A=I.getElementsByTagName("*");for(var E=0,D;D=A[E];E++){if(D.id===B){return[D]}}}}if(A){if(C){if(C=="child"){for(var F=0,D;D=A[F];F++){if(H.parentNode==D){return[H]}}}else{if(C=="descendant"){for(var F=0,D;D=A[F];F++){if(Element.descendantOf(H,D)){return[H]}}}else{if(C=="adjacent"){for(var F=0,D;D=A[F];F++){if(Selector.handlers.previousElementSibling(H)==D){return[H]}}}else{A=G[C](A)}}}}for(var F=0,D;D=A[F];F++){if(D==H){return[H]}}return[]}return(H&&Element.descendantOf(H,I))?[H]:[]},className:function(B,A,C,D){if(B&&D){B=this[D](B)}return Selector.handlers.byClassName(B,A,C)},byClassName:function(C,B,F){if(!C){C=Selector.handlers.descendant([B])}var H=" "+F+" ";for(var E=0,D=[],G,A;G=C[E];E++){A=G.className;if(A.length==0){continue}if(A==F||(" "+A+" ").include(H)){D.push(G)}}return D},attrPresence:function(C,B,A,G){if(!C){C=B.getElementsByTagName("*")}if(C&&G){C=this[G](C)}var E=[];for(var D=0,F;F=C[D];D++){if(Element.hasAttribute(F,A)){E.push(F)}}return E},attr:function(A,I,H,J,C,B){if(!A){A=I.getElementsByTagName("*")}if(A&&B){A=this[B](A)}var K=Selector.operators[C],F=[];for(var E=0,D;D=A[E];E++){var G=Element.readAttribute(D,H);if(G===null){continue}if(K(G,J)){F.push(D)}}return F},pseudo:function(B,C,E,A,D){if(B&&D){B=this[D](B)}if(!B){B=A.getElementsByTagName("*")}return Selector.pseudos[C](B,E,A)}},pseudos:{"first-child":function(B,F,A){for(var D=0,C=[],E;E=B[D];D++){if(Selector.handlers.previousElementSibling(E)){continue}C.push(E)}return C},"last-child":function(B,F,A){for(var D=0,C=[],E;E=B[D];D++){if(Selector.handlers.nextElementSibling(E)){continue}C.push(E)}return C},"only-child":function(B,G,A){var E=Selector.handlers;for(var D=0,C=[],F;F=B[D];D++){if(!E.previousElementSibling(F)&&!E.nextElementSibling(F)){C.push(F)}}return C},"nth-child":function(B,C,A){return Selector.pseudos.nth(B,C,A)},"nth-last-child":function(B,C,A){return Selector.pseudos.nth(B,C,A,true)},"nth-of-type":function(B,C,A){return Selector.pseudos.nth(B,C,A,false,true)},"nth-last-of-type":function(B,C,A){return Selector.pseudos.nth(B,C,A,true,true)},"first-of-type":function(B,C,A){return Selector.pseudos.nth(B,"1",A,false,true)},"last-of-type":function(B,C,A){return Selector.pseudos.nth(B,"1",A,true,true)},"only-of-type":function(B,D,A){var C=Selector.pseudos;return C["last-of-type"](C["first-of-type"](B,D,A),D,A)},getIndices:function(B,A,C){if(B==0){return A>0?[A]:[]}return $R(1,C).inject([],function(D,E){if(0==(E-A)%B&&(E-A)/B>=0){D.push(E)}return D})},nth:function(A,L,N,K,C){if(A.length==0){return[]}if(L=="even"){L="2n+0"}if(L=="odd"){L="2n+1"}var J=Selector.handlers,I=[],B=[],E;J.mark(A);for(var H=0,D;D=A[H];H++){if(!D.parentNode._countedByPrototype){J.index(D.parentNode,K,C);B.push(D.parentNode)}}if(L.match(/^\d+$/)){L=Number(L);for(var H=0,D;D=A[H];H++){if(D.nodeIndex==L){I.push(D)}}}else{if(E=L.match(/^(-?\d*)?n(([+-])(\d+))?/)){if(E[1]=="-"){E[1]=-1}var O=E[1]?Number(E[1]):1;var M=E[2]?Number(E[2]):0;var P=Selector.pseudos.getIndices(O,M,A.length);for(var H=0,D,F=P.length;D=A[H];H++){for(var G=0;G<F;G++){if(D.nodeIndex==P[G]){I.push(D)}}}}}J.unmark(A);J.unmark(B);return I},empty:function(B,F,A){for(var D=0,C=[],E;E=B[D];D++){if(E.tagName=="!"||E.firstChild){continue}C.push(E)}return C},not:function(A,D,I){var G=Selector.handlers,J,C;var H=new Selector(D).findElements(I);G.mark(H);for(var F=0,E=[],B;B=A[F];F++){if(!B._countedByPrototype){E.push(B)}}G.unmark(H);return E},enabled:function(B,F,A){for(var D=0,C=[],E;E=B[D];D++){if(!E.disabled&&(!E.type||E.type!=="hidden")){C.push(E)}}return C},disabled:function(B,F,A){for(var D=0,C=[],E;E=B[D];D++){if(E.disabled){C.push(E)}}return C},checked:function(B,F,A){for(var D=0,C=[],E;E=B[D];D++){if(E.checked){C.push(E)}}return C}},operators:{"=":function(B,A){return B==A},"!=":function(B,A){return B!=A},"^=":function(B,A){return B==A||B&&B.startsWith(A)},"$=":function(B,A){return B==A||B&&B.endsWith(A)},"*=":function(B,A){return B==A||B&&B.include(A)},"~=":function(B,A){return(" "+B+" ").include(" "+A+" ")},"|=":function(B,A){return("-"+(B||"").toUpperCase()+"-").include("-"+(A||"").toUpperCase()+"-")}},split:function(B){var A=[];B.scan(/(([\w#:.~>+()\s-]+|\*|\[.*?\])+)\s*(,|$)/,function(C){A.push(C[1].strip())});return A},matchElements:function(F,G){var E=$$(G),D=Selector.handlers;D.mark(E);for(var C=0,B=[],A;A=F[C];C++){if(A._countedByPrototype){B.push(A)}}D.unmark(E);return B},findElement:function(B,C,A){if(Object.isNumber(C)){A=C;C=false}return Selector.matchElements(B,C||"*")[A||0]},findChildElements:function(E,G){G=Selector.split(G.join(","));var D=[],F=Selector.handlers;for(var C=0,B=G.length,A;C<B;C++){A=new Selector(G[C].strip());F.concat(D,A.findElements(E))}return(B>1)?F.unique(D):D}});if(Prototype.Browser.IE){Object.extend(Selector.handlers,{concat:function(B,A){for(var C=0,D;D=A[C];C++){if(D.tagName!=="!"){B.push(D)}}return B}})}function $$(){return Selector.findChildElements(document,$A(arguments))}var Form={reset:function(A){A=$(A);A.reset();return A},serializeElements:function(G,B){if(typeof B!="object"){B={hash:!!B}}else{if(Object.isUndefined(B.hash)){B.hash=true}}var C,F,A=false,E=B.submit;var D=G.inject({},function(H,I){if(!I.disabled&&I.name){C=I.name;F=$(I).getValue();if(F!=null&&I.type!="file"&&(I.type!="submit"||(!A&&E!==false&&(!E||C==E)&&(A=true)))){if(C in H){if(!Object.isArray(H[C])){H[C]=[H[C]]}H[C].push(F)}else{H[C]=F}}}return H});return B.hash?D:Object.toQueryString(D)}};Form.Methods={serialize:function(B,A){return Form.serializeElements(Form.getElements(B),A)},getElements:function(E){var F=$(E).getElementsByTagName("*"),D,A=[],C=Form.Element.Serializers;for(var B=0;D=F[B];B++){A.push(D)}return A.inject([],function(G,H){if(C[H.tagName.toLowerCase()]){G.push(Element.extend(H))}return G})},getInputs:function(G,C,D){G=$(G);var A=G.getElementsByTagName("input");if(!C&&!D){return $A(A).map(Element.extend)}for(var E=0,H=[],F=A.length;E<F;E++){var B=A[E];if((C&&B.type!=C)||(D&&B.name!=D)){continue}H.push(Element.extend(B))}return H},disable:function(A){A=$(A);Form.getElements(A).invoke("disable");return A},enable:function(A){A=$(A);Form.getElements(A).invoke("enable");return A},findFirstElement:function(B){var C=$(B).getElements().findAll(function(D){return"hidden"!=D.type&&!D.disabled});var A=C.findAll(function(D){return D.hasAttribute("tabIndex")&&D.tabIndex>=0}).sortBy(function(D){return D.tabIndex}).first();return A?A:C.find(function(D){return/^(?:input|select|textarea)$/i.test(D.tagName)})},focusFirstElement:function(A){A=$(A);A.findFirstElement().activate();return A},request:function(B,A){B=$(B),A=Object.clone(A||{});var D=A.parameters,C=B.readAttribute("action")||"";if(C.blank()){C=window.location.href}A.parameters=B.serialize(true);if(D){if(Object.isString(D)){D=D.toQueryParams()}Object.extend(A.parameters,D)}if(B.hasAttribute("method")&&!A.method){A.method=B.method}return new Ajax.Request(C,A)}};Form.Element={focus:function(A){$(A).focus();return A},select:function(A){$(A).select();return A}};Form.Element.Methods={serialize:function(A){A=$(A);if(!A.disabled&&A.name){var B=A.getValue();if(B!=undefined){var C={};C[A.name]=B;return Object.toQueryString(C)}}return""},getValue:function(A){A=$(A);var B=A.tagName.toLowerCase();return Form.Element.Serializers[B](A)},setValue:function(A,B){A=$(A);var C=A.tagName.toLowerCase();Form.Element.Serializers[C](A,B);return A},clear:function(A){$(A).value="";return A},present:function(A){return $(A).value!=""},activate:function(A){A=$(A);try{A.focus();if(A.select&&(A.tagName.toLowerCase()!="input"||!(/^(?:button|reset|submit)$/i.test(A.type)))){A.select()}}catch(B){}return A},disable:function(A){A=$(A);A.disabled=true;return A},enable:function(A){A=$(A);A.disabled=false;return A}};var Field=Form.Element;var $F=Form.Element.Methods.getValue;Form.Element.Serializers={input:function(A,B){switch(A.type.toLowerCase()){case"checkbox":case"radio":return Form.Element.Serializers.inputSelector(A,B);default:return Form.Element.Serializers.textarea(A,B)}},inputSelector:function(A,B){if(Object.isUndefined(B)){return A.checked?A.value:null}else{A.checked=!!B}},textarea:function(A,B){if(Object.isUndefined(B)){return A.value}else{A.value=B}},select:function(C,F){if(Object.isUndefined(F)){return this[C.type=="select-one"?"selectOne":"selectMany"](C)}else{var B,D,G=!Object.isArray(F);for(var A=0,E=C.length;A<E;A++){B=C.options[A];D=this.optionValue(B);if(G){if(D==F){B.selected=true;return }}else{B.selected=F.include(D)}}}},selectOne:function(B){var A=B.selectedIndex;return A>=0?this.optionValue(B.options[A]):null},selectMany:function(D){var A,E=D.length;if(!E){return null}for(var C=0,A=[];C<E;C++){var B=D.options[C];if(B.selected){A.push(this.optionValue(B))}}return A},optionValue:function(A){return Element.extend(A).hasAttribute("value")?A.value:A.text}};Abstract.TimedObserver=Class.create(PeriodicalExecuter,{initialize:function($super,A,B,C){$super(C,B);this.element=$(A);this.lastValue=this.getValue()},execute:function(){var A=this.getValue();if(Object.isString(this.lastValue)&&Object.isString(A)?this.lastValue!=A:String(this.lastValue)!=String(A)){this.callback(this.element,A);this.lastValue=A}}});Form.Element.Observer=Class.create(Abstract.TimedObserver,{getValue:function(){return Form.Element.getValue(this.element)}});Form.Observer=Class.create(Abstract.TimedObserver,{getValue:function(){return Form.serialize(this.element)}});Abstract.EventObserver=Class.create({initialize:function(A,B){this.element=$(A);this.callback=B;this.lastValue=this.getValue();if(this.element.tagName.toLowerCase()=="form"){this.registerFormCallbacks()}else{this.registerCallback(this.element)}},onElementEvent:function(){var A=this.getValue();if(this.lastValue!=A){this.callback(this.element,A);this.lastValue=A}},registerFormCallbacks:function(){Form.getElements(this.element).each(this.registerCallback,this)},registerCallback:function(A){if(A.type){switch(A.type.toLowerCase()){case"checkbox":case"radio":Event.observe(A,"click",this.onElementEvent.bind(this));break;default:Event.observe(A,"change",this.onElementEvent.bind(this));break}}}});Form.Element.EventObserver=Class.create(Abstract.EventObserver,{getValue:function(){return Form.Element.getValue(this.element)}});Form.EventObserver=Class.create(Abstract.EventObserver,{getValue:function(){return Form.serialize(this.element)}});(function(){var V={KEY_BACKSPACE:8,KEY_TAB:9,KEY_RETURN:13,KEY_ESC:27,KEY_LEFT:37,KEY_UP:38,KEY_RIGHT:39,KEY_DOWN:40,KEY_DELETE:46,KEY_HOME:36,KEY_END:35,KEY_PAGEUP:33,KEY_PAGEDOWN:34,KEY_INSERT:45,cache:{}};var E=document.documentElement;var W="onmouseenter" in E&&"onmouseleave" in E;var O;if(Prototype.Browser.IE){var H={0:1,1:4,2:2};O=function(Y,X){return Y.button===H[X]}}else{if(Prototype.Browser.WebKit){O=function(Y,X){switch(X){case 0:return Y.which==1&&!Y.metaKey;case 1:return Y.which==1&&Y.metaKey;default:return false}}}else{O=function(Y,X){return Y.which?(Y.which===X+1):(Y.button===X)}}}function R(X){return O(X,0)}function Q(X){return O(X,1)}function K(X){return O(X,2)}function C(Z){Z=V.extend(Z);var Y=Z.target,X=Z.type,a=Z.currentTarget;if(a&&a.tagName){if(X==="load"||X==="error"||(X==="click"&&a.tagName.toLowerCase()==="input"&&a.type==="radio")){Y=a}}if(Y.nodeType==Node.TEXT_NODE){Y=Y.parentNode}return Element.extend(Y)}function M(Y,a){var X=V.element(Y);if(!a){return X}var Z=[X].concat(X.ancestors());return Selector.findElement(Z,a,0)}function P(X){return{x:B(X),y:A(X)}}function B(Z){var Y=document.documentElement,X=document.body||{scrollLeft:0};return Z.pageX||(Z.clientX+(Y.scrollLeft||X.scrollLeft)-(Y.clientLeft||0))}function A(Z){var Y=document.documentElement,X=document.body||{scrollTop:0};return Z.pageY||(Z.clientY+(Y.scrollTop||X.scrollTop)-(Y.clientTop||0))}function N(X){V.extend(X);X.preventDefault();X.stopPropagation();X.stopped=true}V.Methods={isLeftClick:R,isMiddleClick:Q,isRightClick:K,element:C,findElement:M,pointer:P,pointerX:B,pointerY:A,stop:N};var T=Object.keys(V.Methods).inject({},function(X,Y){X[Y]=V.Methods[Y].methodize();return X});if(Prototype.Browser.IE){function G(Y){var X;switch(Y.type){case"mouseover":X=Y.fromElement;break;case"mouseout":X=Y.toElement;break;default:return null}return Element.extend(X)}Object.extend(T,{stopPropagation:function(){this.cancelBubble=true},preventDefault:function(){this.returnValue=false},inspect:function(){return"[object Event]"}});V.extend=function(Y,X){if(!Y){return false}if(Y._extendedByPrototype){return Y}Y._extendedByPrototype=Prototype.emptyFunction;var Z=V.pointer(Y);Object.extend(Y,{target:Y.srcElement||X,relatedTarget:G(Y),pageX:Z.x,pageY:Z.y});return Object.extend(Y,T)}}else{V.prototype=window.Event.prototype||document.createEvent("HTMLEvents").__proto__;Object.extend(V.prototype,T);V.extend=Prototype.K}function L(b,a,c){var Z=Element.retrieve(b,"prototype_event_registry");if(Object.isUndefined(Z)){D.push(b);Z=Element.retrieve(b,"prototype_event_registry",$H())}var X=Z.get(a);if(Object.isUndefined(X)){X=[];Z.set(a,X)}if(X.pluck("handler").include(c)){return false}var Y;if(a.include(":")){Y=function(d){if(Object.isUndefined(d.eventName)){return false}if(d.eventName!==a){return false}V.extend(d,b);c.call(b,d)}}else{if(!W&&(a==="mouseenter"||a==="mouseleave")){if(a==="mouseenter"||a==="mouseleave"){Y=function(f){V.extend(f,b);var d=f.relatedTarget;while(d&&d!==b){try{d=d.parentNode}catch(g){d=b}}if(d===b){return }c.call(b,f)}}}else{Y=function(d){V.extend(d,b);c.call(b,d)}}}Y.handler=c;X.push(Y);return Y}function F(){for(var X=0,Y=D.length;X<Y;X++){V.stopObserving(D[X]);D[X]=null}}var D=[];if(Prototype.Browser.IE){window.attachEvent("onunload",F)}if(Prototype.Browser.WebKit){window.addEventListener("unload",Prototype.emptyFunction,false)}var J=Prototype.K;if(!W){J=function(Y){var X={mouseenter:"mouseover",mouseleave:"mouseout"};return Y in X?X[Y]:Y}}function S(a,Z,b){a=$(a);var Y=L(a,Z,b);if(!Y){return a}if(Z.include(":")){if(a.addEventListener){a.addEventListener("dataavailable",Y,false)}else{a.attachEvent("ondataavailable",Y);a.attachEvent("onfilterchange",Y)}}else{var X=J(Z);if(a.addEventListener){a.addEventListener(X,Y,false)}else{a.attachEvent("on"+X,Y)}}return a}function I(c,a,d){c=$(c);var Z=Element.retrieve(c,"prototype_event_registry");if(Object.isUndefined(Z)){return c}if(a&&!d){var b=Z.get(a);if(Object.isUndefined(b)){return c}b.each(function(e){Element.stopObserving(c,a,e.handler)});return c}else{if(!a){Z.each(function(g){var e=g.key,f=g.value;f.each(function(h){Element.stopObserving(c,e,h.handler)})});return c}}var b=Z.get(a);if(!b){return }var Y=b.find(function(e){return e.handler===d});if(!Y){return c}var X=J(a);if(a.include(":")){if(c.removeEventListener){c.removeEventListener("dataavailable",Y,false)}else{c.detachEvent("ondataavailable",Y);c.detachEvent("onfilterchange",Y)}}else{if(c.removeEventListener){c.removeEventListener(X,Y,false)}else{c.detachEvent("on"+X,Y)}}Z.set(a,b.without(Y));return c}function U(a,Z,Y,X){a=$(a);if(Object.isUndefined(X)){X=true}if(a==document&&document.createEvent&&!a.dispatchEvent){a=document.documentElement}var b;if(document.createEvent){b=document.createEvent("HTMLEvents");b.initEvent("dataavailable",true,true)}else{b=document.createEventObject();b.eventType=X?"ondataavailable":"onfilterchange"}b.eventName=Z;b.memo=Y||{};if(document.createEvent){a.dispatchEvent(b)}else{a.fireEvent(b.eventType,b)}return V.extend(b)}Object.extend(V,V.Methods);Object.extend(V,{fire:U,observe:S,stopObserving:I});Element.addMethods({fire:U,observe:S,stopObserving:I});Object.extend(document,{fire:U.methodize(),observe:S.methodize(),stopObserving:I.methodize(),loaded:false});if(window.Event){Object.extend(window.Event,V)}else{window.Event=V}})();(function(){var D;function A(){if(document.loaded){return }if(D){window.clearTimeout(D)}document.loaded=true;document.fire("dom:loaded")}function C(){if(document.readyState==="complete"){document.stopObserving("readystatechange",C);A()}}function B(){try{document.documentElement.doScroll("left")}catch(E){D=B.defer();return }A()}if(document.addEventListener){document.addEventListener("DOMContentLoaded",A,false)}else{document.observe("readystatechange",C);if(window==top){D=B.defer()}}Event.observe(window,"load",A)})();Element.addMethods();Hash.toQueryString=Object.toQueryString;var Toggle={display:Element.toggle};Element.Methods.childOf=Element.Methods.descendantOf;var Insertion={Before:function(A,B){return Element.insert(A,{before:B})},Top:function(A,B){return Element.insert(A,{top:B})},Bottom:function(A,B){return Element.insert(A,{bottom:B})},After:function(A,B){return Element.insert(A,{after:B})}};var $continue=new Error('"throw $continue" is deprecated, use "return" instead');var Position={includeScrollOffsets:false,prepare:function(){this.deltaX=window.pageXOffset||document.documentElement.scrollLeft||document.body.scrollLeft||0;this.deltaY=window.pageYOffset||document.documentElement.scrollTop||document.body.scrollTop||0},within:function(B,A,C){if(this.includeScrollOffsets){return this.withinIncludingScrolloffsets(B,A,C)}this.xcomp=A;this.ycomp=C;this.offset=Element.cumulativeOffset(B);return(C>=this.offset[1]&&C<this.offset[1]+B.offsetHeight&&A>=this.offset[0]&&A<this.offset[0]+B.offsetWidth)},withinIncludingScrolloffsets:function(B,A,D){var C=Element.cumulativeScrollOffset(B);this.xcomp=A+C[0]-this.deltaX;this.ycomp=D+C[1]-this.deltaY;this.offset=Element.cumulativeOffset(B);return(this.ycomp>=this.offset[1]&&this.ycomp<this.offset[1]+B.offsetHeight&&this.xcomp>=this.offset[0]&&this.xcomp<this.offset[0]+B.offsetWidth)},overlap:function(B,A){if(!B){return 0}if(B=="vertical"){return((this.offset[1]+A.offsetHeight)-this.ycomp)/A.offsetHeight}if(B=="horizontal"){return((this.offset[0]+A.offsetWidth)-this.xcomp)/A.offsetWidth}},cumulativeOffset:Element.Methods.cumulativeOffset,positionedOffset:Element.Methods.positionedOffset,absolutize:function(A){Position.prepare();return Element.absolutize(A)},relativize:function(A){Position.prepare();return Element.relativize(A)},realOffset:Element.Methods.cumulativeScrollOffset,offsetParent:Element.Methods.getOffsetParent,page:Element.Methods.viewportOffset,clone:function(B,C,A){A=A||{};return Element.clonePosition(C,B,A)}};if(!document.getElementsByClassName){document.getElementsByClassName=function(B){function A(C){return C.blank()?null:"[contains(concat(' ', @class, ' '), ' "+C+" ')]"}B.getElementsByClassName=Prototype.BrowserFeatures.XPath?function(C,E){E=E.toString().strip();var D=/\s/.test(E)?$w(E).map(A).join(""):A(E);return D?document._getElementsByXPath(".//*"+D,C):[]}:function(E,F){F=F.toString().strip();var G=[],H=(/\s/.test(F)?$w(F):null);if(!H&&!F){return G}var C=$(E).getElementsByTagName("*");F=" "+F+" ";for(var D=0,J,I;J=C[D];D++){if(J.className&&(I=" "+J.className+" ")&&(I.include(F)||(H&&H.all(function(K){return !K.toString().blank()&&I.include(" "+K+" ")})))){G.push(Element.extend(J))}}return G};return function(D,C){return $(C||document.body).getElementsByClassName(D)}}(Element.Methods)}Element.ClassNames=Class.create();Element.ClassNames.prototype={initialize:function(A){this.element=$(A)},_each:function(A){this.element.className.split(/\s+/).select(function(B){return B.length>0})._each(A)},set:function(A){this.element.className=A},add:function(A){if(this.include(A)){return }this.set($A(this).concat(A).join(" "))},remove:function(A){if(!this.include(A)){return }this.set($A(this).without(A).join(" "))},toString:function(){return $A(this).join(" ")}};Object.extend(Element.ClassNames.prototype,Enumerable);var Scriptaculous={Version:"1.8.2",require:function(A){document.write('<script type="text/javascript" src="'+A+'"><\/script>')},REQUIRED_PROTOTYPE:"1.6.0.3",load:function(){function A(B){var C=B.replace(/_.*|\./g,"");C=parseInt(C+"0".times(4-C.length));return B.indexOf("_")>-1?C-1:C}if((typeof Prototype=="undefined")||(typeof Element=="undefined")||(typeof Element.Methods=="undefined")||(A(Prototype.Version)<A(Scriptaculous.REQUIRED_PROTOTYPE))){throw ("script.aculo.us requires the Prototype JavaScript framework >= "+Scriptaculous.REQUIRED_PROTOTYPE)}}};Scriptaculous.load();var Builder={NODEMAP:{AREA:"map",CAPTION:"table",COL:"table",COLGROUP:"table",LEGEND:"fieldset",OPTGROUP:"select",OPTION:"select",PARAM:"object",TBODY:"table",TD:"table",TFOOT:"table",TH:"table",THEAD:"table",TR:"table"},node:function(A){A=A.toUpperCase();var F=this.NODEMAP[A]||"div";var B=document.createElement(F);try{B.innerHTML="<"+A+"></"+A+">"}catch(E){}var D=B.firstChild||null;if(D&&(D.tagName.toUpperCase()!=A)){D=D.getElementsByTagName(A)[0]}if(!D){D=document.createElement(A)}if(!D){return }if(arguments[1]){if(this._isStringOrNumber(arguments[1])||(arguments[1] instanceof Array)||arguments[1].tagName){this._children(D,arguments[1])}else{var C=this._attributes(arguments[1]);if(C.length){try{B.innerHTML="<"+A+" "+C+"></"+A+">"}catch(E){}D=B.firstChild||null;if(!D){D=document.createElement(A);for(attr in arguments[1]){D[attr=="class"?"className":attr]=arguments[1][attr]}}if(D.tagName.toUpperCase()!=A){D=B.getElementsByTagName(A)[0]}}}}if(arguments[2]){this._children(D,arguments[2])}return $(D)},_text:function(A){return document.createTextNode(A)},ATTR_MAP:{className:"class",htmlFor:"for"},_attributes:function(A){var B=[];for(attribute in A){B.push((attribute in this.ATTR_MAP?this.ATTR_MAP[attribute]:attribute)+'="'+A[attribute].toString().escapeHTML().gsub(/"/,"&quot;")+'"')}return B.join(" ")},_children:function(B,A){if(A.tagName){B.appendChild(A);return }if(typeof A=="object"){A.flatten().each(function(C){if(typeof C=="object"){B.appendChild(C)}else{if(Builder._isStringOrNumber(C)){B.appendChild(Builder._text(C))}}})}else{if(Builder._isStringOrNumber(A)){B.appendChild(Builder._text(A))}}},_isStringOrNumber:function(A){return(typeof A=="string"||typeof A=="number")},build:function(B){var A=this.node("div");$(A).update(B.strip());return A.down()},dump:function(B){if(typeof B!="object"&&typeof B!="function"){B=window}var A=("A ABBR ACRONYM ADDRESS APPLET AREA B BASE BASEFONT BDO BIG BLOCKQUOTE BODY BR BUTTON CAPTION CENTER CITE CODE COL COLGROUP DD DEL DFN DIR DIV DL DT EM FIELDSET FONT FORM FRAME FRAMESET H1 H2 H3 H4 H5 H6 HEAD HR HTML I IFRAME IMG INPUT INS ISINDEX KBD LABEL LEGEND LI LINK MAP MENU META NOFRAMES NOSCRIPT OBJECT OL OPTGROUP OPTION P PARAM PRE Q S SAMP SCRIPT SELECT SMALL SPAN STRIKE STRONG STYLE SUB SUP TABLE TBODY TD TEXTAREA TFOOT TH THEAD TITLE TR TT U UL VAR").split(/\s+/);A.each(function(C){B[C]=function(){return Builder.node.apply(Builder,[C].concat($A(arguments)))}})}};String.prototype.parseColor=function(){var A="#";if(this.slice(0,4)=="rgb("){var C=this.slice(4,this.length-1).split(",");var B=0;do{A+=parseInt(C[B]).toColorPart()}while(++B<3)}else{if(this.slice(0,1)=="#"){if(this.length==4){for(var B=1;B<4;B++){A+=(this.charAt(B)+this.charAt(B)).toLowerCase()}}if(this.length==7){A=this.toLowerCase()}}}return(A.length==7?A:(arguments[0]||this))};Element.collectTextNodes=function(A){return $A($(A).childNodes).collect(function(B){return(B.nodeType==3?B.nodeValue:(B.hasChildNodes()?Element.collectTextNodes(B):""))}).flatten().join("")};Element.collectTextNodesIgnoreClass=function(A,B){return $A($(A).childNodes).collect(function(C){return(C.nodeType==3?C.nodeValue:((C.hasChildNodes()&&!Element.hasClassName(C,B))?Element.collectTextNodesIgnoreClass(C,B):""))}).flatten().join("")};Element.setContentZoom=function(A,B){A=$(A);A.setStyle({fontSize:(B/100)+"em"});if(Prototype.Browser.WebKit){window.scrollBy(0,0)}return A};Element.getInlineOpacity=function(A){return $(A).style.opacity||""};Element.forceRerendering=function(A){try{A=$(A);var C=document.createTextNode(" ");A.appendChild(C);A.removeChild(C)}catch(B){}};var Effect={_elementDoesNotExistError:{name:"ElementDoesNotExistError",message:"The specified DOM element does not exist, but is required for this effect to operate"},Transitions:{linear:Prototype.K,sinoidal:function(A){return(-Math.cos(A*Math.PI)/2)+0.5},reverse:function(A){return 1-A},flicker:function(A){var A=((-Math.cos(A*Math.PI)/4)+0.75)+Math.random()/4;return A>1?1:A},wobble:function(A){return(-Math.cos(A*Math.PI*(9*A))/2)+0.5},pulse:function(B,A){return(-Math.cos((B*((A||5)-0.5)*2)*Math.PI)/2)+0.5},spring:function(A){return 1-(Math.cos(A*4.5*Math.PI)*Math.exp(-A*6))},none:function(A){return 0},full:function(A){return 1}},DefaultOptions:{duration:1,fps:100,sync:false,from:0,to:1,delay:0,queue:"parallel"},tagifyText:function(A){var B="position:relative";if(Prototype.Browser.IE){B+=";zoom:1"}A=$(A);$A(A.childNodes).each(function(C){if(C.nodeType==3){C.nodeValue.toArray().each(function(D){A.insertBefore(new Element("span",{style:B}).update(D==" "?String.fromCharCode(160):D),C)});Element.remove(C)}})},multiple:function(B,C){var E;if(((typeof B=="object")||Object.isFunction(B))&&(B.length)){E=B}else{E=$(B).childNodes}var A=Object.extend({speed:0.1,delay:0},arguments[2]||{});var D=A.delay;$A(E).each(function(G,F){new C(G,Object.extend(A,{delay:F*A.speed+D}))})},PAIRS:{slide:["SlideDown","SlideUp"],blind:["BlindDown","BlindUp"],appear:["Appear","Fade"]},toggle:function(B,C){B=$(B);C=(C||"appear").toLowerCase();var A=Object.extend({queue:{position:"end",scope:(B.id||"global"),limit:1}},arguments[2]||{});Effect[B.visible()?Effect.PAIRS[C][1]:Effect.PAIRS[C][0]](B,A)}};Effect.DefaultOptions.transition=Effect.Transitions.sinoidal;Effect.ScopedQueue=Class.create(Enumerable,{initialize:function(){this.effects=[];this.interval=null},_each:function(A){this.effects._each(A)},add:function(B){var C=new Date().getTime();var A=Object.isString(B.options.queue)?B.options.queue:B.options.queue.position;switch(A){case"front":this.effects.findAll(function(D){return D.state=="idle"}).each(function(D){D.startOn+=B.finishOn;D.finishOn+=B.finishOn});break;case"with-last":C=this.effects.pluck("startOn").max()||C;break;case"end":C=this.effects.pluck("finishOn").max()||C;break}B.startOn+=C;B.finishOn+=C;if(!B.options.queue.limit||(this.effects.length<B.options.queue.limit)){this.effects.push(B)}if(!this.interval){this.interval=setInterval(this.loop.bind(this),15)}},remove:function(A){this.effects=this.effects.reject(function(B){return B==A});if(this.effects.length==0){clearInterval(this.interval);this.interval=null}},loop:function(){var C=new Date().getTime();for(var B=0,A=this.effects.length;B<A;B++){this.effects[B]&&this.effects[B].loop(C)}}});Effect.Queues={instances:$H(),get:function(A){if(!Object.isString(A)){return A}return this.instances.get(A)||this.instances.set(A,new Effect.ScopedQueue())}};Effect.Queue=Effect.Queues.get("global");Effect.Base=Class.create({position:null,start:function(A){function B(D,C){return((D[C+"Internal"]?"this.options."+C+"Internal(this);":"")+(D[C]?"this.options."+C+"(this);":""))}if(A&&A.transition===false){A.transition=Effect.Transitions.linear}this.options=Object.extend(Object.extend({},Effect.DefaultOptions),A||{});this.currentFrame=0;this.state="idle";this.startOn=this.options.delay*1000;this.finishOn=this.startOn+(this.options.duration*1000);this.fromToDelta=this.options.to-this.options.from;this.totalTime=this.finishOn-this.startOn;this.totalFrames=this.options.fps*this.options.duration;this.render=(function(){function C(E,D){if(E.options[D+"Internal"]){E.options[D+"Internal"](E)}if(E.options[D]){E.options[D](E)}}return function(D){if(this.state==="idle"){this.state="running";C(this,"beforeSetup");if(this.setup){this.setup()}C(this,"afterSetup")}if(this.state==="running"){D=(this.options.transition(D)*this.fromToDelta)+this.options.from;this.position=D;C(this,"beforeUpdate");if(this.update){this.update(D)}C(this,"afterUpdate")}}})();this.event("beforeStart");if(!this.options.sync){Effect.Queues.get(Object.isString(this.options.queue)?"global":this.options.queue.scope).add(this)}},loop:function(C){if(C>=this.startOn){if(C>=this.finishOn){this.render(1);this.cancel();this.event("beforeFinish");if(this.finish){this.finish()}this.event("afterFinish");return }var B=(C-this.startOn)/this.totalTime,A=(B*this.totalFrames).round();if(A>this.currentFrame){this.render(B);this.currentFrame=A}}},cancel:function(){if(!this.options.sync){Effect.Queues.get(Object.isString(this.options.queue)?"global":this.options.queue.scope).remove(this)}this.state="finished"},event:function(A){if(this.options[A+"Internal"]){this.options[A+"Internal"](this)}if(this.options[A]){this.options[A](this)}},inspect:function(){var A=$H();for(property in this){if(!Object.isFunction(this[property])){A.set(property,this[property])}}return"#<Effect:"+A.inspect()+",options:"+$H(this.options).inspect()+">"}});Effect.Parallel=Class.create(Effect.Base,{initialize:function(A){this.effects=A||[];this.start(arguments[1])},update:function(A){this.effects.invoke("render",A)},finish:function(A){this.effects.each(function(B){B.render(1);B.cancel();B.event("beforeFinish");if(B.finish){B.finish(A)}B.event("afterFinish")})}});Effect.Tween=Class.create(Effect.Base,{initialize:function(C,F,E){C=Object.isString(C)?$(C):C;var B=$A(arguments),D=B.last(),A=B.length==5?B[3]:null;this.method=Object.isFunction(D)?D.bind(C):Object.isFunction(C[D])?C[D].bind(C):function(G){C[D]=G};this.start(Object.extend({from:F,to:E},A||{}))},update:function(A){this.method(A)}});Effect.Event=Class.create(Effect.Base,{initialize:function(){this.start(Object.extend({duration:0},arguments[0]||{}))},update:Prototype.emptyFunction});Effect.Opacity=Class.create(Effect.Base,{initialize:function(B){this.element=$(B);if(!this.element){throw (Effect._elementDoesNotExistError)}if(Prototype.Browser.IE&&(!this.element.currentStyle.hasLayout)){this.element.setStyle({zoom:1})}var A=Object.extend({from:this.element.getOpacity()||0,to:1},arguments[1]||{});this.start(A)},update:function(A){this.element.setOpacity(A)}});Effect.Move=Class.create(Effect.Base,{initialize:function(B){this.element=$(B);if(!this.element){throw (Effect._elementDoesNotExistError)}var A=Object.extend({x:0,y:0,mode:"relative"},arguments[1]||{});this.start(A)},setup:function(){this.element.makePositioned();this.originalLeft=parseFloat(this.element.getStyle("left")||"0");this.originalTop=parseFloat(this.element.getStyle("top")||"0");if(this.options.mode=="absolute"){this.options.x=this.options.x-this.originalLeft;this.options.y=this.options.y-this.originalTop}},update:function(A){this.element.setStyle({left:(this.options.x*A+this.originalLeft).round()+"px",top:(this.options.y*A+this.originalTop).round()+"px"})}});Effect.MoveBy=function(B,A,C){return new Effect.Move(B,Object.extend({x:C,y:A},arguments[3]||{}))};Effect.Scale=Class.create(Effect.Base,{initialize:function(B,C){this.element=$(B);if(!this.element){throw (Effect._elementDoesNotExistError)}var A=Object.extend({scaleX:true,scaleY:true,scaleContent:true,scaleFromCenter:false,scaleMode:"box",scaleFrom:100,scaleTo:C},arguments[2]||{});this.start(A)},setup:function(){this.restoreAfterFinish=this.options.restoreAfterFinish||false;this.elementPositioning=this.element.getStyle("position");this.originalStyle={};["top","left","width","height","fontSize"].each(function(B){this.originalStyle[B]=this.element.style[B]}.bind(this));this.originalTop=this.element.offsetTop;this.originalLeft=this.element.offsetLeft;var A=this.element.getStyle("font-size")||"100%";["em","px","%","pt"].each(function(B){if(A.indexOf(B)>0){this.fontSize=parseFloat(A);this.fontSizeType=B}}.bind(this));this.factor=(this.options.scaleTo-this.options.scaleFrom)/100;this.dims=null;if(this.options.scaleMode=="box"){this.dims=[this.element.offsetHeight,this.element.offsetWidth]}if(/^content/.test(this.options.scaleMode)){this.dims=[this.element.scrollHeight,this.element.scrollWidth]}if(!this.dims){this.dims=[this.options.scaleMode.originalHeight,this.options.scaleMode.originalWidth]}},update:function(A){var B=(this.options.scaleFrom/100)+(this.factor*A);if(this.options.scaleContent&&this.fontSize){this.element.setStyle({fontSize:this.fontSize*B+this.fontSizeType})}this.setDimensions(this.dims[0]*B,this.dims[1]*B)},finish:function(A){if(this.restoreAfterFinish){this.element.setStyle(this.originalStyle)}},setDimensions:function(A,D){var E={};if(this.options.scaleX){E.width=D.round()+"px"}if(this.options.scaleY){E.height=A.round()+"px"}if(this.options.scaleFromCenter){var C=(A-this.dims[0])/2;var B=(D-this.dims[1])/2;if(this.elementPositioning=="absolute"){if(this.options.scaleY){E.top=this.originalTop-C+"px"}if(this.options.scaleX){E.left=this.originalLeft-B+"px"}}else{if(this.options.scaleY){E.top=-C+"px"}if(this.options.scaleX){E.left=-B+"px"}}}this.element.setStyle(E)}});Effect.Highlight=Class.create(Effect.Base,{initialize:function(B){this.element=$(B);if(!this.element){throw (Effect._elementDoesNotExistError)}var A=Object.extend({startcolor:"#ffff99"},arguments[1]||{});this.start(A)},setup:function(){if(this.element.getStyle("display")=="none"){this.cancel();return }this.oldStyle={};if(!this.options.keepBackgroundImage){this.oldStyle.backgroundImage=this.element.getStyle("background-image");this.element.setStyle({backgroundImage:"none"})}if(!this.options.endcolor){this.options.endcolor=this.element.getStyle("background-color").parseColor("#ffffff")}if(!this.options.restorecolor){this.options.restorecolor=this.element.getStyle("background-color")}this._base=$R(0,2).map(function(A){return parseInt(this.options.startcolor.slice(A*2+1,A*2+3),16)}.bind(this));this._delta=$R(0,2).map(function(A){return parseInt(this.options.endcolor.slice(A*2+1,A*2+3),16)-this._base[A]}.bind(this))},update:function(A){this.element.setStyle({backgroundColor:$R(0,2).inject("#",function(B,C,D){return B+((this._base[D]+(this._delta[D]*A)).round().toColorPart())}.bind(this))})},finish:function(){this.element.setStyle(Object.extend(this.oldStyle,{backgroundColor:this.options.restorecolor}))}});Effect.ScrollTo=function(C){var B=arguments[1]||{},A=document.viewport.getScrollOffsets(),D=$(C).cumulativeOffset();if(B.offset){D[1]+=B.offset}return new Effect.Tween(null,A.top,D[1],B,function(E){scrollTo(A.left,E.round())})};Effect.Fade=function(C){C=$(C);var A=C.getInlineOpacity();var B=Object.extend({from:C.getOpacity()||1,to:0,afterFinishInternal:function(D){if(D.options.to!=0){return }D.element.hide().setStyle({opacity:A})}},arguments[1]||{});return new Effect.Opacity(C,B)};Effect.Appear=function(B){B=$(B);var A=Object.extend({from:(B.getStyle("display")=="none"?0:B.getOpacity()||0),to:1,afterFinishInternal:function(C){C.element.forceRerendering()},beforeSetup:function(C){C.element.setOpacity(C.options.from).show()}},arguments[1]||{});return new Effect.Opacity(B,A)};Effect.Puff=function(B){B=$(B);var A={opacity:B.getInlineOpacity(),position:B.getStyle("position"),top:B.style.top,left:B.style.left,width:B.style.width,height:B.style.height};return new Effect.Parallel([new Effect.Scale(B,200,{sync:true,scaleFromCenter:true,scaleContent:true,restoreAfterFinish:true}),new Effect.Opacity(B,{sync:true,to:0})],Object.extend({duration:1,beforeSetupInternal:function(C){Position.absolutize(C.effects[0].element)},afterFinishInternal:function(C){C.effects[0].element.hide().setStyle(A)}},arguments[1]||{}))};Effect.BlindUp=function(A){A=$(A);A.makeClipping();return new Effect.Scale(A,0,Object.extend({scaleContent:false,scaleX:false,restoreAfterFinish:true,afterFinishInternal:function(B){B.element.hide().undoClipping()}},arguments[1]||{}))};Effect.BlindDown=function(B){B=$(B);var A=B.getDimensions();return new Effect.Scale(B,100,Object.extend({scaleContent:false,scaleX:false,scaleFrom:0,scaleMode:{originalHeight:A.height,originalWidth:A.width},restoreAfterFinish:true,afterSetup:function(C){C.element.makeClipping().setStyle({height:"0px"}).show()},afterFinishInternal:function(C){C.element.undoClipping()}},arguments[1]||{}))};Effect.SwitchOff=function(B){B=$(B);var A=B.getInlineOpacity();return new Effect.Appear(B,Object.extend({duration:0.4,from:0,transition:Effect.Transitions.flicker,afterFinishInternal:function(C){new Effect.Scale(C.element,1,{duration:0.3,scaleFromCenter:true,scaleX:false,scaleContent:false,restoreAfterFinish:true,beforeSetup:function(D){D.element.makePositioned().makeClipping()},afterFinishInternal:function(D){D.element.hide().undoClipping().undoPositioned().setStyle({opacity:A})}})}},arguments[1]||{}))};Effect.DropOut=function(B){B=$(B);var A={top:B.getStyle("top"),left:B.getStyle("left"),opacity:B.getInlineOpacity()};return new Effect.Parallel([new Effect.Move(B,{x:0,y:100,sync:true}),new Effect.Opacity(B,{sync:true,to:0})],Object.extend({duration:0.5,beforeSetup:function(C){C.effects[0].element.makePositioned()},afterFinishInternal:function(C){C.effects[0].element.hide().undoPositioned().setStyle(A)}},arguments[1]||{}))};Effect.Shake=function(D){D=$(D);var B=Object.extend({distance:20,duration:0.5},arguments[1]||{});var E=parseFloat(B.distance);var C=parseFloat(B.duration)/10;var A={top:D.getStyle("top"),left:D.getStyle("left")};return new Effect.Move(D,{x:E,y:0,duration:C,afterFinishInternal:function(F){new Effect.Move(F.element,{x:-E*2,y:0,duration:C*2,afterFinishInternal:function(G){new Effect.Move(G.element,{x:E*2,y:0,duration:C*2,afterFinishInternal:function(H){new Effect.Move(H.element,{x:-E*2,y:0,duration:C*2,afterFinishInternal:function(I){new Effect.Move(I.element,{x:E*2,y:0,duration:C*2,afterFinishInternal:function(J){new Effect.Move(J.element,{x:-E,y:0,duration:C,afterFinishInternal:function(K){K.element.undoPositioned().setStyle(A)}})}})}})}})}})}})};Effect.SlideDown=function(C){C=$(C).cleanWhitespace();var A=C.down().getStyle("bottom");var B=C.getDimensions();return new Effect.Scale(C,100,Object.extend({scaleContent:false,scaleX:false,scaleFrom:window.opera?0:1,scaleMode:{originalHeight:B.height,originalWidth:B.width},restoreAfterFinish:true,afterSetup:function(D){D.element.makePositioned();D.element.down().makePositioned();if(window.opera){D.element.setStyle({top:""})}D.element.makeClipping().setStyle({height:"0px"}).show()},afterUpdateInternal:function(D){D.element.down().setStyle({bottom:(D.dims[0]-D.element.clientHeight)+"px"})},afterFinishInternal:function(D){D.element.undoClipping().undoPositioned();D.element.down().undoPositioned().setStyle({bottom:A})}},arguments[1]||{}))};Effect.SlideUp=function(C){C=$(C).cleanWhitespace();var A=C.down().getStyle("bottom");var B=C.getDimensions();return new Effect.Scale(C,window.opera?0:1,Object.extend({scaleContent:false,scaleX:false,scaleMode:"box",scaleFrom:100,scaleMode:{originalHeight:B.height,originalWidth:B.width},restoreAfterFinish:true,afterSetup:function(D){D.element.makePositioned();D.element.down().makePositioned();if(window.opera){D.element.setStyle({top:""})}D.element.makeClipping().show()},afterUpdateInternal:function(D){D.element.down().setStyle({bottom:(D.dims[0]-D.element.clientHeight)+"px"})},afterFinishInternal:function(D){D.element.hide().undoClipping().undoPositioned();D.element.down().undoPositioned().setStyle({bottom:A})}},arguments[1]||{}))};Effect.Squish=function(A){return new Effect.Scale(A,window.opera?1:0,{restoreAfterFinish:true,beforeSetup:function(B){B.element.makeClipping()},afterFinishInternal:function(B){B.element.hide().undoClipping()}})};Effect.Grow=function(C){C=$(C);var B=Object.extend({direction:"center",moveTransition:Effect.Transitions.sinoidal,scaleTransition:Effect.Transitions.sinoidal,opacityTransition:Effect.Transitions.full},arguments[1]||{});var A={top:C.style.top,left:C.style.left,height:C.style.height,width:C.style.width,opacity:C.getInlineOpacity()};var G=C.getDimensions();var H,F;var E,D;switch(B.direction){case"top-left":H=F=E=D=0;break;case"top-right":H=G.width;F=D=0;E=-G.width;break;case"bottom-left":H=E=0;F=G.height;D=-G.height;break;case"bottom-right":H=G.width;F=G.height;E=-G.width;D=-G.height;break;case"center":H=G.width/2;F=G.height/2;E=-G.width/2;D=-G.height/2;break}return new Effect.Move(C,{x:H,y:F,duration:0.01,beforeSetup:function(I){I.element.hide().makeClipping().makePositioned()},afterFinishInternal:function(I){new Effect.Parallel([new Effect.Opacity(I.element,{sync:true,to:1,from:0,transition:B.opacityTransition}),new Effect.Move(I.element,{x:E,y:D,sync:true,transition:B.moveTransition}),new Effect.Scale(I.element,100,{scaleMode:{originalHeight:G.height,originalWidth:G.width},sync:true,scaleFrom:window.opera?1:0,transition:B.scaleTransition,restoreAfterFinish:true})],Object.extend({beforeSetup:function(J){J.effects[0].element.setStyle({height:"0px"}).show()},afterFinishInternal:function(J){J.effects[0].element.undoClipping().undoPositioned().setStyle(A)}},B))}})};Effect.Shrink=function(C){C=$(C);var B=Object.extend({direction:"center",moveTransition:Effect.Transitions.sinoidal,scaleTransition:Effect.Transitions.sinoidal,opacityTransition:Effect.Transitions.none},arguments[1]||{});var A={top:C.style.top,left:C.style.left,height:C.style.height,width:C.style.width,opacity:C.getInlineOpacity()};var F=C.getDimensions();var E,D;switch(B.direction){case"top-left":E=D=0;break;case"top-right":E=F.width;D=0;break;case"bottom-left":E=0;D=F.height;break;case"bottom-right":E=F.width;D=F.height;break;case"center":E=F.width/2;D=F.height/2;break}return new Effect.Parallel([new Effect.Opacity(C,{sync:true,to:0,from:1,transition:B.opacityTransition}),new Effect.Scale(C,window.opera?1:0,{sync:true,transition:B.scaleTransition,restoreAfterFinish:true}),new Effect.Move(C,{x:E,y:D,sync:true,transition:B.moveTransition})],Object.extend({beforeStartInternal:function(G){G.effects[0].element.makePositioned().makeClipping()},afterFinishInternal:function(G){G.effects[0].element.hide().undoClipping().undoPositioned().setStyle(A)}},B))};Effect.Pulsate=function(C){C=$(C);var B=arguments[1]||{},A=C.getInlineOpacity(),E=B.transition||Effect.Transitions.linear,D=function(F){return 1-E((-Math.cos((F*(B.pulses||5)*2)*Math.PI)/2)+0.5)};return new Effect.Opacity(C,Object.extend(Object.extend({duration:2,from:0,afterFinishInternal:function(F){F.element.setStyle({opacity:A})}},B),{transition:D}))};Effect.Fold=function(B){B=$(B);var A={top:B.style.top,left:B.style.left,width:B.style.width,height:B.style.height};B.makeClipping();return new Effect.Scale(B,5,Object.extend({scaleContent:false,scaleX:false,afterFinishInternal:function(C){new Effect.Scale(B,1,{scaleContent:false,scaleY:false,afterFinishInternal:function(D){D.element.hide().undoClipping().setStyle(A)}})}},arguments[1]||{}))};Effect.Morph=Class.create(Effect.Base,{initialize:function(C){this.element=$(C);if(!this.element){throw (Effect._elementDoesNotExistError)}var A=Object.extend({style:{}},arguments[1]||{});if(!Object.isString(A.style)){this.style=$H(A.style)}else{if(A.style.include(":")){this.style=A.style.parseStyle()}else{this.element.addClassName(A.style);this.style=$H(this.element.getStyles());this.element.removeClassName(A.style);var B=this.element.getStyles();this.style=this.style.reject(function(D){return D.value==B[D.key]});A.afterFinishInternal=function(D){D.element.addClassName(D.options.style);D.transforms.each(function(E){D.element.style[E.style]=""})}}}this.start(A)},setup:function(){function A(B){if(!B||["rgba(0, 0, 0, 0)","transparent"].include(B)){B="#ffffff"}B=B.parseColor();return $R(0,2).map(function(C){return parseInt(B.slice(C*2+1,C*2+3),16)})}this.transforms=this.style.map(function(G){var F=G[0],E=G[1],D=null;if(E.parseColor("#zzzzzz")!="#zzzzzz"){E=E.parseColor();D="color"}else{if(F=="opacity"){E=parseFloat(E);if(Prototype.Browser.IE&&(!this.element.currentStyle.hasLayout)){this.element.setStyle({zoom:1})}}else{if(Element.CSS_LENGTH.test(E)){var C=E.match(/^([\+\-]?[0-9\.]+)(.*)$/);E=parseFloat(C[1]);D=(C.length==3)?C[2]:null}}}var B=this.element.getStyle(F);return{style:F.camelize(),originalValue:D=="color"?A(B):parseFloat(B||0),targetValue:D=="color"?A(E):E,unit:D}}.bind(this)).reject(function(B){return((B.originalValue==B.targetValue)||(B.unit!="color"&&(isNaN(B.originalValue)||isNaN(B.targetValue))))})},update:function(A){var D={},B,C=this.transforms.length;while(C--){D[(B=this.transforms[C]).style]=B.unit=="color"?"#"+(Math.round(B.originalValue[0]+(B.targetValue[0]-B.originalValue[0])*A)).toColorPart()+(Math.round(B.originalValue[1]+(B.targetValue[1]-B.originalValue[1])*A)).toColorPart()+(Math.round(B.originalValue[2]+(B.targetValue[2]-B.originalValue[2])*A)).toColorPart():(B.originalValue+(B.targetValue-B.originalValue)*A).toFixed(3)+(B.unit===null?"":B.unit)}this.element.setStyle(D,true)}});Effect.Transform=Class.create({initialize:function(A){this.tracks=[];this.options=arguments[1]||{};this.addTracks(A)},addTracks:function(A){A.each(function(B){B=$H(B);var C=B.values().first();this.tracks.push($H({ids:B.keys().first(),effect:Effect.Morph,options:{style:C}}))}.bind(this));return this},play:function(){return new Effect.Parallel(this.tracks.map(function(A){var D=A.get("ids"),C=A.get("effect"),B=A.get("options");var E=[$(D)||$$(D)].flatten();return E.map(function(F){return new C(F,Object.extend({sync:true},B))})}).flatten(),this.options)}});Element.CSS_PROPERTIES=$w("backgroundColor backgroundPosition borderBottomColor borderBottomStyle borderBottomWidth borderLeftColor borderLeftStyle borderLeftWidth borderRightColor borderRightStyle borderRightWidth borderSpacing borderTopColor borderTopStyle borderTopWidth bottom clip color fontSize fontWeight height left letterSpacing lineHeight marginBottom marginLeft marginRight marginTop markerOffset maxHeight maxWidth minHeight minWidth opacity outlineColor outlineOffset outlineWidth paddingBottom paddingLeft paddingRight paddingTop right textIndent top width wordSpacing zIndex");Element.CSS_LENGTH=/^(([\+\-]?[0-9\.]+)(em|ex|px|in|cm|mm|pt|pc|\%))|0$/;String.__parseStyleElement=document.createElement("div");String.prototype.parseStyle=function(){var B,A=$H();if(Prototype.Browser.WebKit){B=new Element("div",{style:this}).style}else{String.__parseStyleElement.innerHTML='<div style="'+this+'"></div>';B=String.__parseStyleElement.childNodes[0].style}Element.CSS_PROPERTIES.each(function(C){if(B[C]){A.set(C,B[C])}});if(Prototype.Browser.IE&&this.include("opacity")){A.set("opacity",this.match(/opacity:\s*((?:0|1)?(?:\.\d*)?)/)[1])}return A};if(document.defaultView&&document.defaultView.getComputedStyle){Element.getStyles=function(B){var A=document.defaultView.getComputedStyle($(B),null);return Element.CSS_PROPERTIES.inject({},function(C,D){C[D]=A[D];return C})}}else{Element.getStyles=function(B){B=$(B);var A=B.currentStyle,C;C=Element.CSS_PROPERTIES.inject({},function(D,E){D[E]=A[E];return D});if(!C.opacity){C.opacity=B.getOpacity()}return C}}Effect.Methods={morph:function(A,B){A=$(A);new Effect.Morph(A,Object.extend({style:B},arguments[2]||{}));return A},visualEffect:function(C,E,B){C=$(C);var D=E.dasherize().camelize(),A=D.charAt(0).toUpperCase()+D.substring(1);new Effect[A](C,B);return C},highlight:function(B,A){B=$(B);new Effect.Highlight(B,A);return B}};$w("fade appear grow shrink fold blindUp blindDown slideUp slideDown pulsate shake puff squish switchOff dropOut").each(function(A){Effect.Methods[A]=function(C,B){C=$(C);Effect[A.charAt(0).toUpperCase()+A.substring(1)](C,B);return C}});$w("getInlineOpacity forceRerendering setContentZoom collectTextNodes collectTextNodesIgnoreClass getStyles").each(function(A){Effect.Methods[A]=Element[A]});Element.addMethods(Effect.Methods);if(Object.isUndefined(Effect)){throw ("dragdrop.js requires including script.aculo.us' effects.js library")}var Droppables={drops:[],remove:function(A){this.drops=this.drops.reject(function(B){return B.element==$(A)})},add:function(B){B=$(B);var A=Object.extend({greedy:true,hoverclass:null,tree:false},arguments[1]||{});if(A.containment){A._containers=[];var C=A.containment;if(Object.isArray(C)){C.each(function(D){A._containers.push($(D))})}else{A._containers.push($(C))}}if(A.accept){A.accept=[A.accept].flatten()}Element.makePositioned(B);A.element=B;this.drops.push(A)},findDeepestChild:function(A){deepest=A[0];for(i=1;i<A.length;++i){if(Element.isParent(A[i].element,deepest.element)){deepest=A[i]}}return deepest},isContained:function(B,A){var C;if(A.tree){C=B.treeNode}else{C=B.parentNode}return A._containers.detect(function(D){return C==D})},isAffected:function(A,C,B){return((B.element!=C)&&((!B._containers)||this.isContained(C,B))&&((!B.accept)||(Element.classNames(C).detect(function(D){return B.accept.include(D)})))&&Position.within(B.element,A[0],A[1]))},deactivate:function(A){if(A.hoverclass){Element.removeClassName(A.element,A.hoverclass)}this.last_active=null},activate:function(A){if(A.hoverclass){Element.addClassName(A.element,A.hoverclass)}this.last_active=A},show:function(A,C){if(!this.drops.length){return }var B,D=[];this.drops.each(function(E){if(Droppables.isAffected(A,C,E)){D.push(E)}});if(D.length>0){B=Droppables.findDeepestChild(D)}if(this.last_active&&this.last_active!=B){this.deactivate(this.last_active)}if(B){Position.within(B.element,A[0],A[1]);if(B.onHover){B.onHover(C,B.element,Position.overlap(B.overlap,B.element))}if(B!=this.last_active){Droppables.activate(B)}}},fire:function(B,A){if(!this.last_active){return }Position.prepare();if(this.isAffected([Event.pointerX(B),Event.pointerY(B)],A,this.last_active)){if(this.last_active.onDrop){this.last_active.onDrop(A,this.last_active.element,B);return true}}},reset:function(){if(this.last_active){this.deactivate(this.last_active)}}};var Draggables={drags:[],observers:[],register:function(A){if(this.drags.length==0){this.eventMouseUp=this.endDrag.bindAsEventListener(this);this.eventMouseMove=this.updateDrag.bindAsEventListener(this);this.eventKeypress=this.keyPress.bindAsEventListener(this);Event.observe(document,"mouseup",this.eventMouseUp);Event.observe(document,"mousemove",this.eventMouseMove);Event.observe(document,"keypress",this.eventKeypress)}this.drags.push(A)},unregister:function(A){this.drags=this.drags.reject(function(B){return B==A});if(this.drags.length==0){Event.stopObserving(document,"mouseup",this.eventMouseUp);Event.stopObserving(document,"mousemove",this.eventMouseMove);Event.stopObserving(document,"keypress",this.eventKeypress)}},activate:function(A){if(A.options.delay){this._timeout=setTimeout(function(){Draggables._timeout=null;window.focus();Draggables.activeDraggable=A}.bind(this),A.options.delay)}else{window.focus();this.activeDraggable=A}},deactivate:function(){this.activeDraggable=null},updateDrag:function(A){if(!this.activeDraggable){return }var B=[Event.pointerX(A),Event.pointerY(A)];if(this._lastPointer&&(this._lastPointer.inspect()==B.inspect())){return }this._lastPointer=B;this.activeDraggable.updateDrag(A,B)},endDrag:function(A){if(this._timeout){clearTimeout(this._timeout);this._timeout=null}if(!this.activeDraggable){return }this._lastPointer=null;this.activeDraggable.endDrag(A);this.activeDraggable=null},keyPress:function(A){if(this.activeDraggable){this.activeDraggable.keyPress(A)}},addObserver:function(A){this.observers.push(A);this._cacheObserverCallbacks()},removeObserver:function(A){this.observers=this.observers.reject(function(B){return B.element==A});this._cacheObserverCallbacks()},notify:function(B,A,C){if(this[B+"Count"]>0){this.observers.each(function(D){if(D[B]){D[B](B,A,C)}})}if(A.options[B]){A.options[B](A,C)}},_cacheObserverCallbacks:function(){["onStart","onEnd","onDrag"].each(function(A){Draggables[A+"Count"]=Draggables.observers.select(function(B){return B[A]}).length})}};var Draggable=Class.create({initialize:function(B){var C={handle:false,reverteffect:function(F,E,D){var G=Math.sqrt(Math.abs(E^2)+Math.abs(D^2))*0.02;new Effect.Move(F,{x:-D,y:-E,duration:G,queue:{scope:"_draggable",position:"end"}})},endeffect:function(E){var D=Object.isNumber(E._opacity)?E._opacity:1;new Effect.Opacity(E,{duration:0.2,from:0.7,to:D,queue:{scope:"_draggable",position:"end"},afterFinish:function(){Draggable._dragging[E]=false}})},zindex:1000,revert:false,quiet:false,scroll:false,scrollSensitivity:20,scrollSpeed:15,snap:false,delay:0};if(!arguments[1]||Object.isUndefined(arguments[1].endeffect)){Object.extend(C,{starteffect:function(D){D._opacity=Element.getOpacity(D);Draggable._dragging[D]=true;new Effect.Opacity(D,{duration:0.2,from:D._opacity,to:0.7})}})}var A=Object.extend(C,arguments[1]||{});this.element=$(B);if(A.handle&&Object.isString(A.handle)){this.handle=this.element.down("."+A.handle,0)}if(!this.handle){this.handle=$(A.handle)}if(!this.handle){this.handle=this.element}if(A.scroll&&!A.scroll.scrollTo&&!A.scroll.outerHTML){A.scroll=$(A.scroll);this._isScrollChild=Element.childOf(this.element,A.scroll)}Element.makePositioned(this.element);this.options=A;this.dragging=false;this.eventMouseDown=this.initDrag.bindAsEventListener(this);Event.observe(this.handle,"mousedown",this.eventMouseDown);Draggables.register(this)},destroy:function(){Event.stopObserving(this.handle,"mousedown",this.eventMouseDown);Draggables.unregister(this)},currentDelta:function(){return([parseInt(Element.getStyle(this.element,"left")||"0"),parseInt(Element.getStyle(this.element,"top")||"0")])},initDrag:function(A){if(!Object.isUndefined(Draggable._dragging[this.element])&&Draggable._dragging[this.element]){return }if(Event.isLeftClick(A)){var C=Event.element(A);if((tag_name=C.tagName.toUpperCase())&&(tag_name=="INPUT"||tag_name=="SELECT"||tag_name=="OPTION"||tag_name=="BUTTON"||tag_name=="TEXTAREA")){return }var B=[Event.pointerX(A),Event.pointerY(A)];var D=Position.cumulativeOffset(this.element);this.offset=[0,1].map(function(E){return(B[E]-D[E])});Draggables.activate(this);Event.stop(A)}},startDrag:function(B){this.dragging=true;if(!this.delta){this.delta=this.currentDelta()}if(this.options.zindex){this.originalZ=parseInt(Element.getStyle(this.element,"z-index")||0);this.element.style.zIndex=this.options.zindex}if(this.options.ghosting){this._clone=this.element.cloneNode(true);this._originallyAbsolute=(this.element.getStyle("position")=="absolute");if(!this._originallyAbsolute){Position.absolutize(this.element)}this.element.parentNode.insertBefore(this._clone,this.element)}if(this.options.scroll){if(this.options.scroll==window){var A=this._getWindowScroll(this.options.scroll);this.originalScrollLeft=A.left;this.originalScrollTop=A.top}else{this.originalScrollLeft=this.options.scroll.scrollLeft;this.originalScrollTop=this.options.scroll.scrollTop}}Draggables.notify("onStart",this,B);if(this.options.starteffect){this.options.starteffect(this.element)}},updateDrag:function(event,pointer){if(!this.dragging){this.startDrag(event)}if(!this.options.quiet){Position.prepare();Droppables.show(pointer,this.element)}Draggables.notify("onDrag",this,event);this.draw(pointer);if(this.options.change){this.options.change(this)}if(this.options.scroll){this.stopScrolling();var p;if(this.options.scroll==window){with(this._getWindowScroll(this.options.scroll)){p=[left,top,left+width,top+height]}}else{p=Position.page(this.options.scroll);p[0]+=this.options.scroll.scrollLeft+Position.deltaX;p[1]+=this.options.scroll.scrollTop+Position.deltaY;p.push(p[0]+this.options.scroll.offsetWidth);p.push(p[1]+this.options.scroll.offsetHeight)}var speed=[0,0];if(pointer[0]<(p[0]+this.options.scrollSensitivity)){speed[0]=pointer[0]-(p[0]+this.options.scrollSensitivity)}if(pointer[1]<(p[1]+this.options.scrollSensitivity)){speed[1]=pointer[1]-(p[1]+this.options.scrollSensitivity)}if(pointer[0]>(p[2]-this.options.scrollSensitivity)){speed[0]=pointer[0]-(p[2]-this.options.scrollSensitivity)}if(pointer[1]>(p[3]-this.options.scrollSensitivity)){speed[1]=pointer[1]-(p[3]-this.options.scrollSensitivity)}this.startScrolling(speed)}if(Prototype.Browser.WebKit){window.scrollBy(0,0)}Event.stop(event)},finishDrag:function(B,E){this.dragging=false;if(this.options.quiet){Position.prepare();var D=[Event.pointerX(B),Event.pointerY(B)];Droppables.show(D,this.element)}if(this.options.ghosting){if(!this._originallyAbsolute){Position.relativize(this.element)}delete this._originallyAbsolute;Element.remove(this._clone);this._clone=null}var F=false;if(E){F=Droppables.fire(B,this.element);if(!F){F=false}}if(F&&this.options.onDropped){this.options.onDropped(this.element)}Draggables.notify("onEnd",this,B);var A=this.options.revert;if(A&&Object.isFunction(A)){A=A(this.element)}var C=this.currentDelta();if(A&&this.options.reverteffect){if(F==0||A!="failure"){this.options.reverteffect(this.element,C[1]-this.delta[1],C[0]-this.delta[0])}}else{this.delta=C}if(this.options.zindex){this.element.style.zIndex=this.originalZ}if(this.options.endeffect){this.options.endeffect(this.element)}Draggables.deactivate(this);Droppables.reset()},keyPress:function(A){if(A.keyCode!=Event.KEY_ESC){return }this.finishDrag(A,false);Event.stop(A)},endDrag:function(A){if(!this.dragging){return }this.stopScrolling();this.finishDrag(A,true);Event.stop(A)},draw:function(A){var F=Position.cumulativeOffset(this.element);if(this.options.ghosting){var C=Position.realOffset(this.element);F[0]+=C[0]-Position.deltaX;F[1]+=C[1]-Position.deltaY}var E=this.currentDelta();F[0]-=E[0];F[1]-=E[1];if(this.options.scroll&&(this.options.scroll!=window&&this._isScrollChild)){F[0]-=this.options.scroll.scrollLeft-this.originalScrollLeft;F[1]-=this.options.scroll.scrollTop-this.originalScrollTop}var D=[0,1].map(function(G){return(A[G]-F[G]-this.offset[G])}.bind(this));if(this.options.snap){if(Object.isFunction(this.options.snap)){D=this.options.snap(D[0],D[1],this)}else{if(Object.isArray(this.options.snap)){D=D.map(function(G,H){return(G/this.options.snap[H]).round()*this.options.snap[H]}.bind(this))}else{D=D.map(function(G){return(G/this.options.snap).round()*this.options.snap}.bind(this))}}}var B=this.element.style;if((!this.options.constraint)||(this.options.constraint=="horizontal")){B.left=D[0]+"px"}if((!this.options.constraint)||(this.options.constraint=="vertical")){B.top=D[1]+"px"}if(B.visibility=="hidden"){B.visibility=""}},stopScrolling:function(){if(this.scrollInterval){clearInterval(this.scrollInterval);this.scrollInterval=null;Draggables._lastScrollPointer=null}},startScrolling:function(A){if(!(A[0]||A[1])){return }this.scrollSpeed=[A[0]*this.options.scrollSpeed,A[1]*this.options.scrollSpeed];this.lastScrolled=new Date();this.scrollInterval=setInterval(this.scroll.bind(this),10)},scroll:function(){var current=new Date();var delta=current-this.lastScrolled;this.lastScrolled=current;if(this.options.scroll==window){with(this._getWindowScroll(this.options.scroll)){if(this.scrollSpeed[0]||this.scrollSpeed[1]){var d=delta/1000;this.options.scroll.scrollTo(left+d*this.scrollSpeed[0],top+d*this.scrollSpeed[1])}}}else{this.options.scroll.scrollLeft+=this.scrollSpeed[0]*delta/1000;this.options.scroll.scrollTop+=this.scrollSpeed[1]*delta/1000}Position.prepare();Droppables.show(Draggables._lastPointer,this.element);Draggables.notify("onDrag",this);if(this._isScrollChild){Draggables._lastScrollPointer=Draggables._lastScrollPointer||$A(Draggables._lastPointer);Draggables._lastScrollPointer[0]+=this.scrollSpeed[0]*delta/1000;Draggables._lastScrollPointer[1]+=this.scrollSpeed[1]*delta/1000;if(Draggables._lastScrollPointer[0]<0){Draggables._lastScrollPointer[0]=0}if(Draggables._lastScrollPointer[1]<0){Draggables._lastScrollPointer[1]=0}this.draw(Draggables._lastScrollPointer)}if(this.options.change){this.options.change(this)}},_getWindowScroll:function(w){var T,L,W,H;with(w.document){if(w.document.documentElement&&documentElement.scrollTop){T=documentElement.scrollTop;L=documentElement.scrollLeft}else{if(w.document.body){T=body.scrollTop;L=body.scrollLeft}}if(w.innerWidth){W=w.innerWidth;H=w.innerHeight}else{if(w.document.documentElement&&documentElement.clientWidth){W=documentElement.clientWidth;H=documentElement.clientHeight}else{W=body.offsetWidth;H=body.offsetHeight}}}return{top:T,left:L,width:W,height:H}}});Draggable._dragging={};var SortableObserver=Class.create({initialize:function(B,A){this.element=$(B);this.observer=A;this.lastValue=Sortable.serialize(this.element)},onStart:function(){this.lastValue=Sortable.serialize(this.element)},onEnd:function(){Sortable.unmark();if(this.lastValue!=Sortable.serialize(this.element)){this.observer(this.element)}}});var Sortable={SERIALIZE_RULE:/^[^_\-](?:[A-Za-z0-9\-\_]*)[_](.*)$/,sortables:{},_findRootElement:function(A){while(A.tagName.toUpperCase()!="BODY"){if(A.id&&Sortable.sortables[A.id]){return A}A=A.parentNode}},options:function(A){A=Sortable._findRootElement($(A));if(!A){return }return Sortable.sortables[A.id]},destroy:function(A){A=$(A);var B=Sortable.sortables[A.id];if(B){Draggables.removeObserver(B.element);B.droppables.each(function(C){Droppables.remove(C)});B.draggables.invoke("destroy");delete Sortable.sortables[B.element.id]}},create:function(C){C=$(C);var B=Object.extend({element:C,tag:"li",dropOnEmpty:false,tree:false,treeTag:"ul",overlap:"vertical",constraint:"vertical",containment:C,handle:false,only:false,delay:0,hoverclass:null,ghosting:false,quiet:false,scroll:false,scrollSensitivity:20,scrollSpeed:15,format:this.SERIALIZE_RULE,elements:false,handles:false,onChange:Prototype.emptyFunction,onUpdate:Prototype.emptyFunction},arguments[1]||{});this.destroy(C);var A={revert:true,quiet:B.quiet,scroll:B.scroll,scrollSpeed:B.scrollSpeed,scrollSensitivity:B.scrollSensitivity,delay:B.delay,ghosting:B.ghosting,constraint:B.constraint,handle:B.handle};if(B.starteffect){A.starteffect=B.starteffect}if(B.reverteffect){A.reverteffect=B.reverteffect}else{if(B.ghosting){A.reverteffect=function(F){F.style.top=0;F.style.left=0}}}if(B.endeffect){A.endeffect=B.endeffect}if(B.zindex){A.zindex=B.zindex}var D={overlap:B.overlap,containment:B.containment,tree:B.tree,hoverclass:B.hoverclass,onHover:Sortable.onHover};var E={onHover:Sortable.onEmptyHover,overlap:B.overlap,containment:B.containment,hoverclass:B.hoverclass};Element.cleanWhitespace(C);B.draggables=[];B.droppables=[];if(B.dropOnEmpty||B.tree){Droppables.add(C,E);B.droppables.push(C)}(B.elements||this.findElements(C,B)||[]).each(function(H,F){var G=B.handles?$(B.handles[F]):(B.handle?$(H).select("."+B.handle)[0]:H);B.draggables.push(new Draggable(H,Object.extend(A,{handle:G})));Droppables.add(H,D);if(B.tree){H.treeNode=C}B.droppables.push(H)});if(B.tree){(Sortable.findTreeElements(C,B)||[]).each(function(F){Droppables.add(F,E);F.treeNode=C;B.droppables.push(F)})}this.sortables[C.id]=B;Draggables.addObserver(new SortableObserver(C,B.onUpdate))},findElements:function(B,A){return Element.findChildren(B,A.only,A.tree?true:false,A.tag)},findTreeElements:function(B,A){return Element.findChildren(B,A.only,A.tree?true:false,A.treeTag)},onHover:function(E,D,A){if(Element.isParent(D,E)){return }if(A>0.33&&A<0.66&&Sortable.options(D).tree){return }else{if(A>0.5){Sortable.mark(D,"before");if(D.previousSibling!=E){var B=E.parentNode;E.style.visibility="hidden";D.parentNode.insertBefore(E,D);if(D.parentNode!=B){Sortable.options(B).onChange(E)}Sortable.options(D.parentNode).onChange(E)}}else{Sortable.mark(D,"after");var C=D.nextSibling||null;if(C!=E){var B=E.parentNode;E.style.visibility="hidden";D.parentNode.insertBefore(E,C);if(D.parentNode!=B){Sortable.options(B).onChange(E)}Sortable.options(D.parentNode).onChange(E)}}}},onEmptyHover:function(E,G,H){var I=E.parentNode;var A=Sortable.options(G);if(!Element.isParent(G,E)){var F;var C=Sortable.findElements(G,{tag:A.tag,only:A.only});var B=null;if(C){var D=Element.offsetSize(G,A.overlap)*(1-H);for(F=0;F<C.length;F+=1){if(D-Element.offsetSize(C[F],A.overlap)>=0){D-=Element.offsetSize(C[F],A.overlap)}else{if(D-(Element.offsetSize(C[F],A.overlap)/2)>=0){B=F+1<C.length?C[F+1]:null;break}else{B=C[F];break}}}}G.insertBefore(E,B);Sortable.options(I).onChange(E);A.onChange(E)}},unmark:function(){if(Sortable._marker){Sortable._marker.hide()}},mark:function(B,A){var D=Sortable.options(B.parentNode);if(D&&!D.ghosting){return }if(!Sortable._marker){Sortable._marker=($("dropmarker")||Element.extend(document.createElement("DIV"))).hide().addClassName("dropmarker").setStyle({position:"absolute"});document.getElementsByTagName("body").item(0).appendChild(Sortable._marker)}var C=Position.cumulativeOffset(B);Sortable._marker.setStyle({left:C[0]+"px",top:C[1]+"px"});if(A=="after"){if(D.overlap=="horizontal"){Sortable._marker.setStyle({left:(C[0]+B.clientWidth)+"px"})}else{Sortable._marker.setStyle({top:(C[1]+B.clientHeight)+"px"})}}Sortable._marker.show()},_tree:function(E,B,F){var D=Sortable.findElements(E,B)||[];for(var C=0;C<D.length;++C){var A=D[C].id.match(B.format);if(!A){continue}var G={id:encodeURIComponent(A?A[1]:null),element:E,parent:F,children:[],position:F.children.length,container:$(D[C]).down(B.treeTag)};if(G.container){this._tree(G.container,B,G)}F.children.push(G)}return F},tree:function(D){D=$(D);var C=this.options(D);var B=Object.extend({tag:C.tag,treeTag:C.treeTag,only:C.only,name:D.id,format:C.format},arguments[1]||{});var A={id:null,parent:null,children:[],container:D,position:0};return Sortable._tree(D,B,A)},_constructIndex:function(B){var A="";do{if(B.id){A="["+B.position+"]"+A}}while((B=B.parent)!=null);return A},sequence:function(B){B=$(B);var A=Object.extend(this.options(B),arguments[1]||{});return $(this.findElements(B,A)||[]).map(function(C){return C.id.match(A.format)?C.id.match(A.format)[1]:""})},setSequence:function(B,C){B=$(B);var A=Object.extend(this.options(B),arguments[2]||{});var D={};this.findElements(B,A).each(function(E){if(E.id.match(A.format)){D[E.id.match(A.format)[1]]=[E,E.parentNode]}E.parentNode.removeChild(E)});C.each(function(E){var F=D[E];if(F){F[1].appendChild(F[0]);delete D[E]}})},serialize:function(C){C=$(C);var B=Object.extend(Sortable.options(C),arguments[1]||{});var A=encodeURIComponent((arguments[1]&&arguments[1].name)?arguments[1].name:C.id);if(B.tree){return Sortable.tree(C,arguments[1]).children.map(function(D){return[A+Sortable._constructIndex(D)+"[id]="+encodeURIComponent(D.id)].concat(D.children.map(arguments.callee))}).flatten().join("&")}else{return Sortable.sequence(C,arguments[1]).map(function(D){return A+"[]="+encodeURIComponent(D)}).join("&")}}};Element.isParent=function(B,A){if(!B.parentNode||B==A){return false}if(B.parentNode==A){return true}return Element.isParent(B.parentNode,A)};Element.findChildren=function(D,B,A,C){if(!D.hasChildNodes()){return null}C=C.toUpperCase();if(B){B=[B].flatten()}var E=[];$A(D.childNodes).each(function(G){if(G.tagName&&G.tagName.toUpperCase()==C&&(!B||(Element.classNames(G).detect(function(H){return B.include(H)})))){E.push(G)}if(A){var F=Element.findChildren(G,B,A,C);if(F){E.push(F)}}});return(E.length>0?E.flatten():[])};Element.offsetSize=function(A,B){return A["offset"+((B=="vertical"||B=="height")?"Height":"Width")]};if(typeof Effect=="undefined"){throw ("controls.js requires including script.aculo.us' effects.js library")}var Autocompleter={};Autocompleter.Base=Class.create({baseInitialize:function(B,C,A){B=$(B);this.element=B;this.update=$(C);this.hasFocus=false;this.changed=false;this.active=false;this.index=0;this.entryCount=0;this.oldElementValue=this.element.value;if(this.setOptions){this.setOptions(A)}else{this.options=A||{}}this.options.paramName=this.options.paramName||this.element.name;this.options.tokens=this.options.tokens||[];this.options.frequency=this.options.frequency||0.4;this.options.minChars=this.options.minChars||1;this.options.onShow=this.options.onShow||function(D,E){if(!E.style.position||E.style.position=="absolute"){E.style.position="absolute";Position.clone(D,E,{setHeight:false,offsetTop:D.offsetHeight})}Effect.Appear(E,{duration:0.15})};this.options.onHide=this.options.onHide||function(D,E){new Effect.Fade(E,{duration:0.15})};if(typeof (this.options.tokens)=="string"){this.options.tokens=new Array(this.options.tokens)}if(!this.options.tokens.include("\n")){this.options.tokens.push("\n")}this.observer=null;this.element.setAttribute("autocomplete","off");Element.hide(this.update);Event.observe(this.element,"blur",this.onBlur.bindAsEventListener(this));Event.observe(this.element,"keydown",this.onKeyPress.bindAsEventListener(this))},show:function(){if(Element.getStyle(this.update,"display")=="none"){this.options.onShow(this.element,this.update)}if(!this.iefix&&(Prototype.Browser.IE)&&(Element.getStyle(this.update,"position")=="absolute")){new Insertion.After(this.update,'<iframe id="'+this.update.id+'_iefix" style="display:none;position:absolute;filter:progid:DXImageTransform.Microsoft.Alpha(opacity=0);" src="javascript:false;" frameborder="0" scrolling="no"></iframe>');this.iefix=$(this.update.id+"_iefix")}if(this.iefix){setTimeout(this.fixIEOverlapping.bind(this),50)}},fixIEOverlapping:function(){Position.clone(this.update,this.iefix,{setTop:(!this.update.style.height)});this.iefix.style.zIndex=1;this.update.style.zIndex=2;Element.show(this.iefix)},hide:function(){this.stopIndicator();if(Element.getStyle(this.update,"display")!="none"){this.options.onHide(this.element,this.update)}if(this.iefix){Element.hide(this.iefix)}},startIndicator:function(){if(this.options.indicator){Element.show(this.options.indicator)}},stopIndicator:function(){if(this.options.indicator){Element.hide(this.options.indicator)}},onKeyPress:function(A){if(this.active){switch(A.keyCode){case Event.KEY_TAB:case Event.KEY_RETURN:this.selectEntry();Event.stop(A);case Event.KEY_ESC:this.hide();this.active=false;Event.stop(A);return ;case Event.KEY_LEFT:case Event.KEY_RIGHT:return ;case Event.KEY_UP:this.markPrevious();this.render();Event.stop(A);return ;case Event.KEY_DOWN:this.markNext();this.render();Event.stop(A);return }}else{if(A.keyCode==Event.KEY_TAB||A.keyCode==Event.KEY_RETURN||(Prototype.Browser.WebKit>0&&A.keyCode==0)){return }}this.changed=true;this.hasFocus=true;if(this.observer){clearTimeout(this.observer)}this.observer=setTimeout(this.onObserverEvent.bind(this),this.options.frequency*1000)},activate:function(){this.changed=false;this.hasFocus=true;this.getUpdatedChoices()},onHover:function(B){var A=Event.findElement(B,"LI");if(this.index!=A.autocompleteIndex){this.index=A.autocompleteIndex;this.render()}Event.stop(B)},onClick:function(B){var A=Event.findElement(B,"LI");this.index=A.autocompleteIndex;this.selectEntry();this.hide()},onBlur:function(A){setTimeout(this.hide.bind(this),250);this.hasFocus=false;this.active=false},render:function(){if(this.entryCount>0){for(var A=0;A<this.entryCount;A++){this.index==A?Element.addClassName(this.getEntry(A),"selected"):Element.removeClassName(this.getEntry(A),"selected")}if(this.hasFocus){this.show();this.active=true}}else{this.active=false;this.hide()}},markPrevious:function(){if(this.index>0){this.index--}else{this.index=this.entryCount-1}this.getEntry(this.index).scrollIntoView(true)},markNext:function(){if(this.index<this.entryCount-1){this.index++}else{this.index=0}this.getEntry(this.index).scrollIntoView(false)},getEntry:function(A){return this.update.firstChild.childNodes[A]},getCurrentEntry:function(){return this.getEntry(this.index)},selectEntry:function(){this.active=false;this.updateElement(this.getCurrentEntry())},updateElement:function(F){if(this.options.updateElement){this.options.updateElement(F);return }var D="";if(this.options.select){var A=$(F).select("."+this.options.select)||[];if(A.length>0){D=Element.collectTextNodes(A[0],this.options.select)}}else{D=Element.collectTextNodesIgnoreClass(F,"informal")}var C=this.getTokenBounds();if(C[0]!=-1){var E=this.element.value.substr(0,C[0]);var B=this.element.value.substr(C[0]).match(/^\s+/);if(B){E+=B[0]}this.element.value=E+D+this.element.value.substr(C[1])}else{this.element.value=D}this.oldElementValue=this.element.value;this.element.focus();if(this.options.afterUpdateElement){this.options.afterUpdateElement(this.element,F)}},updateChoices:function(C){if(!this.changed&&this.hasFocus){this.update.innerHTML=C;Element.cleanWhitespace(this.update);Element.cleanWhitespace(this.update.down());if(this.update.firstChild&&this.update.down().childNodes){this.entryCount=this.update.down().childNodes.length;for(var A=0;A<this.entryCount;A++){var B=this.getEntry(A);B.autocompleteIndex=A;this.addObservers(B)}}else{this.entryCount=0}this.stopIndicator();this.index=0;if(this.entryCount==1&&this.options.autoSelect){this.selectEntry();this.hide()}else{this.render()}}},addObservers:function(A){Event.observe(A,"mouseover",this.onHover.bindAsEventListener(this));Event.observe(A,"click",this.onClick.bindAsEventListener(this))},onObserverEvent:function(){this.changed=false;this.tokenBounds=null;if(this.getToken().length>=this.options.minChars){this.getUpdatedChoices()}else{this.active=false;this.hide()}this.oldElementValue=this.element.value},getToken:function(){var A=this.getTokenBounds();return this.element.value.substring(A[0],A[1]).strip()},getTokenBounds:function(){if(null!=this.tokenBounds){return this.tokenBounds}var E=this.element.value;if(E.strip().empty()){return[-1,0]}var F=arguments.callee.getFirstDifferencePos(E,this.oldElementValue);var H=(F==this.oldElementValue.length?1:0);var D=-1,C=E.length;var G;for(var B=0,A=this.options.tokens.length;B<A;++B){G=E.lastIndexOf(this.options.tokens[B],F+H-1);if(G>D){D=G}G=E.indexOf(this.options.tokens[B],F+H);if(-1!=G&&G<C){C=G}}return(this.tokenBounds=[D+1,C])}});Autocompleter.Base.prototype.getTokenBounds.getFirstDifferencePos=function(C,A){var D=Math.min(C.length,A.length);for(var B=0;B<D;++B){if(C[B]!=A[B]){return B}}return D};Ajax.Autocompleter=Class.create(Autocompleter.Base,{initialize:function(C,D,B,A){this.baseInitialize(C,D,A);this.options.asynchronous=true;this.options.onComplete=this.onComplete.bind(this);this.options.defaultParams=this.options.parameters||null;this.url=B},getUpdatedChoices:function(){this.startIndicator();var A=encodeURIComponent(this.options.paramName)+"="+encodeURIComponent(this.getToken());this.options.parameters=this.options.callback?this.options.callback(this.element,A):A;if(this.options.defaultParams){this.options.parameters+="&"+this.options.defaultParams}new Ajax.Request(this.url,this.options)},onComplete:function(A){this.updateChoices(A.responseText)}});Autocompleter.Local=Class.create(Autocompleter.Base,{initialize:function(B,D,C,A){this.baseInitialize(B,D,A);this.options.array=C},getUpdatedChoices:function(){this.updateChoices(this.options.selector(this))},setOptions:function(A){this.options=Object.extend({choices:10,partialSearch:true,partialChars:2,ignoreCase:true,fullSearch:false,selector:function(B){var D=[];var C=[];var H=B.getToken();var G=0;for(var E=0;E<B.options.array.length&&D.length<B.options.choices;E++){var F=B.options.array[E];var I=B.options.ignoreCase?F.toLowerCase().indexOf(H.toLowerCase()):F.indexOf(H);while(I!=-1){if(I==0&&F.length!=H.length){D.push("<li><strong>"+F.substr(0,H.length)+"</strong>"+F.substr(H.length)+"</li>");break}else{if(H.length>=B.options.partialChars&&B.options.partialSearch&&I!=-1){if(B.options.fullSearch||/\s/.test(F.substr(I-1,1))){C.push("<li>"+F.substr(0,I)+"<strong>"+F.substr(I,H.length)+"</strong>"+F.substr(I+H.length)+"</li>");break}}}I=B.options.ignoreCase?F.toLowerCase().indexOf(H.toLowerCase(),I+1):F.indexOf(H,I+1)}}if(C.length){D=D.concat(C.slice(0,B.options.choices-D.length))}return"<ul>"+D.join("")+"</ul>"}},A||{})}});Field.scrollFreeActivate=function(A){setTimeout(function(){Field.activate(A)},1)};Ajax.InPlaceEditor=Class.create({initialize:function(C,B,A){this.url=B;this.element=C=$(C);this.prepareOptions();this._controls={};arguments.callee.dealWithDeprecatedOptions(A);Object.extend(this.options,A||{});if(!this.options.formId&&this.element.id){this.options.formId=this.element.id+"-inplaceeditor";if($(this.options.formId)){this.options.formId=""}}if(this.options.externalControl){this.options.externalControl=$(this.options.externalControl)}if(!this.options.externalControl){this.options.externalControlOnly=false}this._originalBackground=this.element.getStyle("background-color")||"transparent";this.element.title=this.options.clickToEditText;this._boundCancelHandler=this.handleFormCancellation.bind(this);this._boundComplete=(this.options.onComplete||Prototype.emptyFunction).bind(this);this._boundFailureHandler=this.handleAJAXFailure.bind(this);this._boundSubmitHandler=this.handleFormSubmission.bind(this);this._boundWrapperHandler=this.wrapUp.bind(this);this.registerListeners()},checkForEscapeOrReturn:function(A){if(!this._editing||A.ctrlKey||A.altKey||A.shiftKey){return }if(Event.KEY_ESC==A.keyCode){this.handleFormCancellation(A)}else{if(Event.KEY_RETURN==A.keyCode){this.handleFormSubmission(A)}}},createControl:function(G,C,B){var E=this.options[G+"Control"];var F=this.options[G+"Text"];if("button"==E){var A=document.createElement("input");A.type="submit";A.value=F;A.className="editor_"+G+"_button";if("cancel"==G){A.onclick=this._boundCancelHandler}this._form.appendChild(A);this._controls[G]=A}else{if("link"==E){var D=document.createElement("a");D.href="#";D.appendChild(document.createTextNode(F));D.onclick="cancel"==G?this._boundCancelHandler:this._boundSubmitHandler;D.className="editor_"+G+"_link";if(B){D.className+=" "+B}this._form.appendChild(D);this._controls[G]=D}}},createEditField:function(){var C=(this.options.loadTextURL?this.options.loadingText:this.getText());var B;if(1>=this.options.rows&&!/\r|\n/.test(this.getText())){B=document.createElement("input");B.type="text";var A=this.options.size||this.options.cols||0;if(0<A){B.size=A}}else{B=document.createElement("textarea");B.rows=(1>=this.options.rows?this.options.autoRows:this.options.rows);B.cols=this.options.cols||40}B.name=this.options.paramName;B.value=C;B.className="editor_field";if(this.options.submitOnBlur){B.onblur=this._boundSubmitHandler}this._controls.editor=B;if(this.options.loadTextURL){this.loadExternalText()}this._form.appendChild(this._controls.editor)},createForm:function(){var B=this;function A(D,E){var C=B.options["text"+D+"Controls"];if(!C||E===false){return }B._form.appendChild(document.createTextNode(C))}this._form=$(document.createElement("form"));this._form.id=this.options.formId;this._form.addClassName(this.options.formClassName);this._form.onsubmit=this._boundSubmitHandler;this.createEditField();if("textarea"==this._controls.editor.tagName.toLowerCase()){this._form.appendChild(document.createElement("br"))}if(this.options.onFormCustomization){this.options.onFormCustomization(this,this._form)}A("Before",this.options.okControl||this.options.cancelControl);this.createControl("ok",this._boundSubmitHandler);A("Between",this.options.okControl&&this.options.cancelControl);this.createControl("cancel",this._boundCancelHandler,"editor_cancel");A("After",this.options.okControl||this.options.cancelControl)},destroy:function(){if(this._oldInnerHTML){this.element.innerHTML=this._oldInnerHTML}this.leaveEditMode();this.unregisterListeners()},enterEditMode:function(A){if(this._saving||this._editing){return }this._editing=true;this.triggerCallback("onEnterEditMode");if(this.options.externalControl){this.options.externalControl.hide()}this.element.hide();this.createForm();this.element.parentNode.insertBefore(this._form,this.element);if(!this.options.loadTextURL){this.postProcessEditField()}if(A){Event.stop(A)}},enterHover:function(A){if(this.options.hoverClassName){this.element.addClassName(this.options.hoverClassName)}if(this._saving){return }this.triggerCallback("onEnterHover")},getText:function(){return this.element.innerHTML.unescapeHTML()},handleAJAXFailure:function(A){this.triggerCallback("onFailure",A);if(this._oldInnerHTML){this.element.innerHTML=this._oldInnerHTML;this._oldInnerHTML=null}},handleFormCancellation:function(A){this.wrapUp();if(A){Event.stop(A)}},handleFormSubmission:function(D){var B=this._form;var C=$F(this._controls.editor);this.prepareSubmission();var E=this.options.callback(B,C)||"";if(Object.isString(E)){E=E.toQueryParams()}E.editorId=this.element.id;if(this.options.htmlResponse){var A=Object.extend({evalScripts:true},this.options.ajaxOptions);Object.extend(A,{parameters:E,onComplete:this._boundWrapperHandler,onFailure:this._boundFailureHandler});new Ajax.Updater({success:this.element},this.url,A)}else{var A=Object.extend({method:"get"},this.options.ajaxOptions);Object.extend(A,{parameters:E,onComplete:this._boundWrapperHandler,onFailure:this._boundFailureHandler});new Ajax.Request(this.url,A)}if(D){Event.stop(D)}},leaveEditMode:function(){this.element.removeClassName(this.options.savingClassName);this.removeForm();this.leaveHover();this.element.style.backgroundColor=this._originalBackground;this.element.show();if(this.options.externalControl){this.options.externalControl.show()}this._saving=false;this._editing=false;this._oldInnerHTML=null;this.triggerCallback("onLeaveEditMode")},leaveHover:function(A){if(this.options.hoverClassName){this.element.removeClassName(this.options.hoverClassName)}if(this._saving){return }this.triggerCallback("onLeaveHover")},loadExternalText:function(){this._form.addClassName(this.options.loadingClassName);this._controls.editor.disabled=true;var A=Object.extend({method:"get"},this.options.ajaxOptions);Object.extend(A,{parameters:"editorId="+encodeURIComponent(this.element.id),onComplete:Prototype.emptyFunction,onSuccess:function(C){this._form.removeClassName(this.options.loadingClassName);var B=C.responseText;if(this.options.stripLoadedTextTags){B=B.stripTags()}this._controls.editor.value=B;this._controls.editor.disabled=false;this.postProcessEditField()}.bind(this),onFailure:this._boundFailureHandler});new Ajax.Request(this.options.loadTextURL,A)},postProcessEditField:function(){var A=this.options.fieldPostCreation;if(A){$(this._controls.editor)["focus"==A?"focus":"activate"]()}},prepareOptions:function(){this.options=Object.clone(Ajax.InPlaceEditor.DefaultOptions);Object.extend(this.options,Ajax.InPlaceEditor.DefaultCallbacks);[this._extraDefaultOptions].flatten().compact().each(function(A){Object.extend(this.options,A)}.bind(this))},prepareSubmission:function(){this._saving=true;this.removeForm();this.leaveHover();this.showSaving()},registerListeners:function(){this._listeners={};var A;$H(Ajax.InPlaceEditor.Listeners).each(function(B){A=this[B.value].bind(this);this._listeners[B.key]=A;if(!this.options.externalControlOnly){this.element.observe(B.key,A)}if(this.options.externalControl){this.options.externalControl.observe(B.key,A)}}.bind(this))},removeForm:function(){if(!this._form){return }this._form.remove();this._form=null;this._controls={}},showSaving:function(){this._oldInnerHTML=this.element.innerHTML;this.element.innerHTML=this.options.savingText;this.element.addClassName(this.options.savingClassName);this.element.style.backgroundColor=this._originalBackground;this.element.show()},triggerCallback:function(B,A){if("function"==typeof this.options[B]){this.options[B](this,A)}},unregisterListeners:function(){$H(this._listeners).each(function(A){if(!this.options.externalControlOnly){this.element.stopObserving(A.key,A.value)}if(this.options.externalControl){this.options.externalControl.stopObserving(A.key,A.value)}}.bind(this))},wrapUp:function(A){this.leaveEditMode();this._boundComplete(A,this.element)}});Object.extend(Ajax.InPlaceEditor.prototype,{dispose:Ajax.InPlaceEditor.prototype.destroy});Ajax.InPlaceCollectionEditor=Class.create(Ajax.InPlaceEditor,{initialize:function($super,C,B,A){this._extraDefaultOptions=Ajax.InPlaceCollectionEditor.DefaultOptions;$super(C,B,A)},createEditField:function(){var A=document.createElement("select");A.name=this.options.paramName;A.size=1;this._controls.editor=A;this._collection=this.options.collection||[];if(this.options.loadCollectionURL){this.loadCollection()}else{this.checkForExternalText()}this._form.appendChild(this._controls.editor)},loadCollection:function(){this._form.addClassName(this.options.loadingClassName);this.showLoadingText(this.options.loadingCollectionText);var options=Object.extend({method:"get"},this.options.ajaxOptions);Object.extend(options,{parameters:"editorId="+encodeURIComponent(this.element.id),onComplete:Prototype.emptyFunction,onSuccess:function(transport){var js=transport.responseText.strip();if(!/^\[.*\]$/.test(js)){throw ("Server returned an invalid collection representation.")}this._collection=eval(js);this.checkForExternalText()}.bind(this),onFailure:this.onFailure});new Ajax.Request(this.options.loadCollectionURL,options)},showLoadingText:function(B){this._controls.editor.disabled=true;var A=this._controls.editor.firstChild;if(!A){A=document.createElement("option");A.value="";this._controls.editor.appendChild(A);A.selected=true}A.update((B||"").stripScripts().stripTags())},checkForExternalText:function(){this._text=this.getText();if(this.options.loadTextURL){this.loadExternalText()}else{this.buildOptionList()}},loadExternalText:function(){this.showLoadingText(this.options.loadingText);var A=Object.extend({method:"get"},this.options.ajaxOptions);Object.extend(A,{parameters:"editorId="+encodeURIComponent(this.element.id),onComplete:Prototype.emptyFunction,onSuccess:function(B){this._text=B.responseText.strip();this.buildOptionList()}.bind(this),onFailure:this.onFailure});new Ajax.Request(this.options.loadTextURL,A)},buildOptionList:function(){this._form.removeClassName(this.options.loadingClassName);this._collection=this._collection.map(function(D){return 2===D.length?D:[D,D].flatten()});var B=("value" in this.options)?this.options.value:this._text;var A=this._collection.any(function(D){return D[0]==B}.bind(this));this._controls.editor.update("");var C;this._collection.each(function(E,D){C=document.createElement("option");C.value=E[0];C.selected=A?E[0]==B:0==D;C.appendChild(document.createTextNode(E[1]));this._controls.editor.appendChild(C)}.bind(this));this._controls.editor.disabled=false;Field.scrollFreeActivate(this._controls.editor)}});Ajax.InPlaceEditor.prototype.initialize.dealWithDeprecatedOptions=function(A){if(!A){return }function B(C,D){if(C in A||D===undefined){return }A[C]=D}B("cancelControl",(A.cancelLink?"link":(A.cancelButton?"button":A.cancelLink==A.cancelButton==false?false:undefined)));B("okControl",(A.okLink?"link":(A.okButton?"button":A.okLink==A.okButton==false?false:undefined)));B("highlightColor",A.highlightcolor);B("highlightEndColor",A.highlightendcolor)};Object.extend(Ajax.InPlaceEditor,{DefaultOptions:{ajaxOptions:{},autoRows:3,cancelControl:"link",cancelText:"cancel",clickToEditText:"Click to edit",externalControl:null,externalControlOnly:false,fieldPostCreation:"activate",formClassName:"inplaceeditor-form",formId:null,highlightColor:"#ffff99",highlightEndColor:"#ffffff",hoverClassName:"",htmlResponse:true,loadingClassName:"inplaceeditor-loading",loadingText:"Loading...",okControl:"button",okText:"ok",paramName:"value",rows:1,savingClassName:"inplaceeditor-saving",savingText:"Saving...",size:0,stripLoadedTextTags:false,submitOnBlur:false,textAfterControls:"",textBeforeControls:"",textBetweenControls:""},DefaultCallbacks:{callback:function(A){return Form.serialize(A)},onComplete:function(B,A){new Effect.Highlight(A,{startcolor:this.options.highlightColor,keepBackgroundImage:true})},onEnterEditMode:null,onEnterHover:function(A){A.element.style.backgroundColor=A.options.highlightColor;if(A._effect){A._effect.cancel()}},onFailure:function(B,A){alert("Error communication with the server: "+B.responseText.stripTags())},onFormCustomization:null,onLeaveEditMode:null,onLeaveHover:function(A){A._effect=new Effect.Highlight(A.element,{startcolor:A.options.highlightColor,endcolor:A.options.highlightEndColor,restorecolor:A._originalBackground,keepBackgroundImage:true})}},Listeners:{click:"enterEditMode",keydown:"checkForEscapeOrReturn",mouseover:"enterHover",mouseout:"leaveHover"}});Ajax.InPlaceCollectionEditor.DefaultOptions={loadingCollectionText:"Loading options..."};Form.Element.DelayedObserver=Class.create({initialize:function(B,A,C){this.delay=A||0.5;this.element=$(B);this.callback=C;this.timer=null;this.lastValue=$F(this.element);Event.observe(this.element,"keyup",this.delayedListener.bindAsEventListener(this))},delayedListener:function(A){if(this.lastValue==$F(this.element)){return }if(this.timer){clearTimeout(this.timer)}this.timer=setTimeout(this.onTimerEvent.bind(this),this.delay*1000);this.lastValue=$F(this.element)},onTimerEvent:function(){this.timer=null;this.callback(this.element,$F(this.element))}});if(!Control){var Control={}}Control.Slider=Class.create({initialize:function(D,A,B){var C=this;if(Object.isArray(D)){this.handles=D.collect(function(E){return $(E)})}else{this.handles=[$(D)]}this.track=$(A);this.options=B||{};this.axis=this.options.axis||"horizontal";this.increment=this.options.increment||1;this.step=parseInt(this.options.step||"1");this.range=this.options.range||$R(0,1);this.value=0;this.values=this.handles.map(function(){return 0});this.spans=this.options.spans?this.options.spans.map(function(E){return $(E)}):false;this.options.startSpan=$(this.options.startSpan||null);this.options.endSpan=$(this.options.endSpan||null);this.restricted=this.options.restricted||false;this.maximum=this.options.maximum||this.range.end;this.minimum=this.options.minimum||this.range.start;this.alignX=parseInt(this.options.alignX||"0");this.alignY=parseInt(this.options.alignY||"0");this.trackLength=this.maximumOffset()-this.minimumOffset();this.handleLength=this.isVertical()?(this.handles[0].offsetHeight!=0?this.handles[0].offsetHeight:this.handles[0].style.height.replace(/px$/,"")):(this.handles[0].offsetWidth!=0?this.handles[0].offsetWidth:this.handles[0].style.width.replace(/px$/,""));this.active=false;this.dragging=false;this.disabled=false;if(this.options.disabled){this.setDisabled()}this.allowedValues=this.options.values?this.options.values.sortBy(Prototype.K):false;if(this.allowedValues){this.minimum=this.allowedValues.min();this.maximum=this.allowedValues.max()}this.eventMouseDown=this.startDrag.bindAsEventListener(this);this.eventMouseUp=this.endDrag.bindAsEventListener(this);this.eventMouseMove=this.update.bindAsEventListener(this);this.handles.each(function(F,E){E=C.handles.length-1-E;C.setValue(parseFloat((Object.isArray(C.options.sliderValue)?C.options.sliderValue[E]:C.options.sliderValue)||C.range.start),E);F.makePositioned().observe("mousedown",C.eventMouseDown)});this.track.observe("mousedown",this.eventMouseDown);document.observe("mouseup",this.eventMouseUp);document.observe("mousemove",this.eventMouseMove);this.initialized=true},dispose:function(){var A=this;Event.stopObserving(this.track,"mousedown",this.eventMouseDown);Event.stopObserving(document,"mouseup",this.eventMouseUp);Event.stopObserving(document,"mousemove",this.eventMouseMove);this.handles.each(function(B){Event.stopObserving(B,"mousedown",A.eventMouseDown)})},setDisabled:function(){this.disabled=true},setEnabled:function(){this.disabled=false},getNearestValue:function(A){if(this.allowedValues){if(A>=this.allowedValues.max()){return(this.allowedValues.max())}if(A<=this.allowedValues.min()){return(this.allowedValues.min())}var C=Math.abs(this.allowedValues[0]-A);var B=this.allowedValues[0];this.allowedValues.each(function(D){var E=Math.abs(D-A);if(E<=C){B=D;C=E}});return B}if(A>this.range.end){return this.range.end}if(A<this.range.start){return this.range.start}return A},setValue:function(B,A){if(!this.active){this.activeHandleIdx=A||0;this.activeHandle=this.handles[this.activeHandleIdx];this.updateStyles()}A=A||this.activeHandleIdx||0;if(this.initialized&&this.restricted){if((A>0)&&(B<this.values[A-1])){B=this.values[A-1]}if((A<(this.handles.length-1))&&(B>this.values[A+1])){B=this.values[A+1]}}B=this.getNearestValue(B);this.values[A]=B;this.value=this.values[0];this.handles[A].style[this.isVertical()?"top":"left"]=this.translateToPx(B);this.drawSpans();if(!this.dragging||!this.event){this.updateFinished()}},setValueBy:function(B,A){this.setValue(this.values[A||this.activeHandleIdx||0]+B,A||this.activeHandleIdx||0)},translateToPx:function(A){return Math.round(((this.trackLength-this.handleLength)/(this.range.end-this.range.start))*(A-this.range.start))+"px"},translateToValue:function(A){return((A/(this.trackLength-this.handleLength)*(this.range.end-this.range.start))+this.range.start)},getRange:function(B){var A=this.values.sortBy(Prototype.K);B=B||0;return $R(A[B],A[B+1])},minimumOffset:function(){return(this.isVertical()?this.alignY:this.alignX)},maximumOffset:function(){return(this.isVertical()?(this.track.offsetHeight!=0?this.track.offsetHeight:this.track.style.height.replace(/px$/,""))-this.alignY:(this.track.offsetWidth!=0?this.track.offsetWidth:this.track.style.width.replace(/px$/,""))-this.alignX)},isVertical:function(){return(this.axis=="vertical")},drawSpans:function(){var A=this;if(this.spans){$R(0,this.spans.length-1).each(function(B){A.setSpan(A.spans[B],A.getRange(B))})}if(this.options.startSpan){this.setSpan(this.options.startSpan,$R(0,this.values.length>1?this.getRange(0).min():this.value))}if(this.options.endSpan){this.setSpan(this.options.endSpan,$R(this.values.length>1?this.getRange(this.spans.length-1).max():this.value,this.maximum))}},setSpan:function(B,A){if(this.isVertical()){B.style.top=this.translateToPx(A.start);B.style.height=this.translateToPx(A.end-A.start+this.range.start)}else{B.style.left=this.translateToPx(A.start);B.style.width=this.translateToPx(A.end-A.start+this.range.start)}},updateStyles:function(){this.handles.each(function(A){Element.removeClassName(A,"selected")});Element.addClassName(this.activeHandle,"selected")},startDrag:function(C){if(Event.isLeftClick(C)){if(!this.disabled){this.active=true;var D=Event.element(C);var E=[Event.pointerX(C),Event.pointerY(C)];var A=D;if(A==this.track){var B=Position.cumulativeOffset(this.track);this.event=C;this.setValue(this.translateToValue((this.isVertical()?E[1]-B[1]:E[0]-B[0])-(this.handleLength/2)));var B=Position.cumulativeOffset(this.activeHandle);this.offsetX=(E[0]-B[0]);this.offsetY=(E[1]-B[1])}else{while((this.handles.indexOf(D)==-1)&&D.parentNode){D=D.parentNode}if(this.handles.indexOf(D)!=-1){this.activeHandle=D;this.activeHandleIdx=this.handles.indexOf(this.activeHandle);this.updateStyles();var B=Position.cumulativeOffset(this.activeHandle);this.offsetX=(E[0]-B[0]);this.offsetY=(E[1]-B[1])}}}Event.stop(C)}},update:function(A){if(this.active){if(!this.dragging){this.dragging=true}this.draw(A);if(Prototype.Browser.WebKit){window.scrollBy(0,0)}Event.stop(A)}},draw:function(B){var C=[Event.pointerX(B),Event.pointerY(B)];var A=Position.cumulativeOffset(this.track);C[0]-=this.offsetX+A[0];C[1]-=this.offsetY+A[1];this.event=B;this.setValue(this.translateToValue(this.isVertical()?C[1]:C[0]));if(this.initialized&&this.options.onSlide){this.options.onSlide(this.values.length>1?this.values:this.value,this)}},endDrag:function(A){if(this.active&&this.dragging){this.finishDrag(A,true);Event.stop(A)}this.active=false;this.dragging=false},finishDrag:function(A,B){this.active=false;this.dragging=false;this.updateFinished()},updateFinished:function(){if(this.initialized&&this.options.onChange){this.options.onChange(this.values.length>1?this.values:this.value,this)}this.event=null}});Sound={tracks:{},_enabled:true,template:new Template('<embed style="height:0" id="sound_#{track}_#{id}" src="#{url}" loop="false" autostart="true" hidden="true"/>'),enable:function(){Sound._enabled=true},disable:function(){Sound._enabled=false},play:function(B){if(!Sound._enabled){return }var A=Object.extend({track:"global",url:B,replace:false},arguments[1]||{});if(A.replace&&this.tracks[A.track]){$R(0,this.tracks[A.track].id).each(function(D){var C=$("sound_"+A.track+"_"+D);C.Stop&&C.Stop();C.remove()});this.tracks[A.track]=null}if(!this.tracks[A.track]){this.tracks[A.track]={id:0}}else{this.tracks[A.track].id++}A.id=this.tracks[A.track].id;$$("body")[0].insert(Prototype.Browser.IE?new Element("bgsound",{id:"sound_"+A.track+"_"+A.id,src:A.url,loop:1,autostart:true}):Sound.template.evaluate(A))}};if(Prototype.Browser.Gecko&&navigator.userAgent.indexOf("Win")>0){if(navigator.plugins&&$A(navigator.plugins).detect(function(A){return A.name.indexOf("QuickTime")!=-1})){Sound.template=new Template('<object id="sound_#{track}_#{id}" width="0" height="0" type="audio/mpeg" data="#{url}"/>')}else{Sound.play=function(){}}};// CalendarDateSelect version 1.10.2 - a prototype based date picker
// Questions, comments, bugs? - email the Author - Tim Harper <"timseeharper@gmail.seeom".gsub("see", "c")>
if (typeof Prototype == 'undefined') alert("CalendarDateSelect Error: Prototype could not be found. Please make sure that your application's layout includes prototype.js (.g. <%= javascript_include_tag :defaults %>) *before* it includes calendar_date_select.js (.g. <%= calendar_date_select_includes %>).");
if (Prototype.Version < "1.6") alert("Prototype 1.6.0 is required.  If using earlier version of prototype, please use calendar_date_select version 1.8.3");

Element.addMethods({
  purgeChildren: function(element) { $A(element.childNodes).each(function(e){$(e).remove();}); },
  build: function(element, type, options, style) {
    var newElement = Element.build(type, options, style);
    element.appendChild(newElement);
    return newElement;
  }
});

Element.build = function(type, options, style)
{
  var e = $(document.createElement(type));
  $H(options).each(function(pair) { eval("e." + pair.key + " = pair.value" ); });
  if (style)
    $H(style).each(function(pair) { eval("e.style." + pair.key + " = pair.value" ); });
  return e;
};
nil = null;

Date.one_day = 24*60*60*1000;
Date.weekdays = $w("S M T W T F S");
Date.first_day_of_week = 0;
Date.months = $w("January February March April May June July August September October November December" );
Date.padded2 = function(hour) { var padded2 = parseInt(hour, 10); if (hour < 10) padded2 = "0" + padded2; return padded2; }
Date.prototype.getPaddedMinutes = function() { return Date.padded2(this.getMinutes()); }
Date.prototype.getAMPMHour = function() { var hour = this.getHours(); return (hour == 0) ? 12 : (hour > 12 ? hour - 12 : hour ) }
Date.prototype.getAMPM = function() { return (this.getHours() < 12) ? "AM" : "PM"; }
Date.prototype.stripTime = function() { return new Date(this.getFullYear(), this.getMonth(), this.getDate());};
Date.prototype.daysDistance = function(compare_date) { return Math.round((compare_date - this) / Date.one_day); };
Date.prototype.toFormattedString = function(include_time){
  var hour, str;
  str = Date.months[this.getMonth()] + " " + this.getDate() + ", " + this.getFullYear();

  if (include_time) { hour = this.getHours(); str += " " + this.getAMPMHour() + ":" + this.getPaddedMinutes() + " " + this.getAMPM() }
  return str;
}
Date.parseFormattedString = function(string) { return new Date(string);}
Math.floor_to_interval = function(n, i) { return Math.floor(n/i) * i;}
window.f_height = function() { return( [window.innerHeight ? window.innerHeight : null, document.documentElement ? document.documentElement.clientHeight : null, document.body ? document.body.clientHeight : null].select(function(x){return x>0}).first()||0); }
window.f_scrollTop = function() { return ([window.pageYOffset ? window.pageYOffset : null, document.documentElement ? document.documentElement.scrollTop : null, document.body ? document.body.scrollTop : null].select(function(x){return x>0}).first()||0 ); }

_translations = {
  "OK": "OK",
  "Now": "Now",
  "Today": "Today"
}
SelectBox = Class.create();
SelectBox.prototype = {
  initialize: function(parent_element, values, html_options, style_options) {
    this.element = $(parent_element).build("select", html_options, style_options);
    this.populate(values);
  },
  populate: function(values) {
    this.element.purgeChildren();
    var that = this; $A(values).each(function(pair) { if (typeof(pair)!="object") {pair = [pair, pair]}; that.element.build("option", { value: pair[1], innerHTML: pair[0]}) });
  },
  setValue: function(value) {
    var e = this.element;
    var matched = false;
    $R(0, e.options.length - 1 ).each(function(i) { if(e.options[i].value==value.toString()) {e.selectedIndex = i; matched = true;}; } );
    return matched;
  },
  getValue: function() { return $F(this.element)}
}
CalendarDateSelect = Class.create();
CalendarDateSelect.prototype = {
  initialize: function(target_element, options) {
    this.target_element = $(target_element); // make sure it's an element, not a string
    if (!this.target_element) { alert("Target element " + target_element + " not found!"); return false;}
    if (this.target_element.tagName != "INPUT") this.target_element = this.target_element.down("INPUT")

    this.target_element.calendar_date_select = this;
    this.last_click_at = 0;
    // initialize the date control
    this.options = $H({
      embedded: false,
      popup: nil,
      time: false,
      buttons: true,
      year_range: 10,
      close_on_click: nil,
      minute_interval: 5,
      popup_by: this.target_element,
      month_year: "dropdowns",
      onchange: this.target_element.onchange,
      valid_date_check: nil
    }).merge(options || {});
    this.use_time = this.options.get("time");
    this.parseDate();
    this.callback("before_show")
    this.initCalendarDiv();
    if(!this.options.get("embedded")) {
      this.positionCalendarDiv()
      // set the click handler to check if a user has clicked away from the document
      Event.observe(document, "mousedown", this.closeIfClickedOut_handler = this.closeIfClickedOut.bindAsEventListener(this));
      Event.observe(document, "keypress", this.keyPress_handler = this.keyPress.bindAsEventListener(this));
    }
    this.callback("after_show")
  },
  positionCalendarDiv: function() {
    var above = false;
    var c_pos = this.calendar_div.cumulativeOffset(), c_left = c_pos[0], c_top = c_pos[1], c_dim = this.calendar_div.getDimensions(), c_height = c_dim.height, c_width = c_dim.width;
    var w_top = window.f_scrollTop(), w_height = window.f_height();
    var e_dim = $(this.options.get("popup_by")).cumulativeOffset(), e_top = e_dim[1], e_left = e_dim[0], e_height = $(this.options.get("popup_by")).getDimensions().height, e_bottom = e_top + e_height;

    if ( (( e_bottom + c_height ) > (w_top + w_height)) && ( e_bottom - c_height > w_top )) above = true;
    var left_px = e_left.toString() + "px", top_px = (above ? (e_top - c_height ) : ( e_top + e_height )).toString() + "px";

    this.calendar_div.style.left = left_px;  this.calendar_div.style.top = top_px;

    this.calendar_div.setStyle({visibility:""});

    // draw an iframe behind the calendar -- ugly hack to make IE 6 happy
    if(navigator.appName=="Microsoft Internet Explorer") this.iframe = $(document.body).build("iframe", {src: "javascript:false", className: "ie6_blocker"}, { left: left_px, top: top_px, height: c_height.toString()+"px", width: c_width.toString()+"px", border: "0px"})
  },
  initCalendarDiv: function() {
    if (this.options.get("embedded")) {
      var parent = this.target_element.parentNode;
      var style = {}
    } else {
      var parent = document.body
      var style = { position:"absolute", visibility: "hidden", left:0, top:0 }
    }
    this.calendar_div = $(parent).build('div', {className: "calendar_date_select"}, style);

    var that = this;
    // create the divs
    $w("top header body buttons footer bottom").each(function(name) {
      eval("var " + name + "_div = that." + name + "_div = that.calendar_div.build('div', { className: 'cds_"+name+"' }, { clear: 'left'} ); ");
    });

    this.initHeaderDiv();
    this.initButtonsDiv();
    this.initCalendarGrid();
    this.updateFooter("&#160;");

    this.refresh();
    this.setUseTime(this.use_time);
  },
  initHeaderDiv: function() {
    var header_div = this.header_div;
    this.close_button = header_div.build("a", { innerHTML: "x", href:"#", onclick:function () { this.close(); return false; }.bindAsEventListener(this), className: "close" });
    this.next_month_button = header_div.build("a", { innerHTML: "&gt;", href:"#", onclick:function () { this.navMonth(this.date.getMonth() + 1 ); return false; }.bindAsEventListener(this), className: "next" });
    this.prev_month_button = header_div.build("a", { innerHTML: "&lt;", href:"#", onclick:function () { this.navMonth(this.date.getMonth() - 1 ); return false; }.bindAsEventListener(this), className: "prev" });

    if (this.options.get("month_year")=="dropdowns") {
      this.month_select = new SelectBox(header_div, $R(0,11).map(function(m){return [Date.months[m], m]}), {className: "month", onchange: function () { this.navMonth(this.month_select.getValue()) }.bindAsEventListener(this)});
      this.year_select = new SelectBox(header_div, [], {className: "year", onchange: function () { this.navYear(this.year_select.getValue()) }.bindAsEventListener(this)});
      this.populateYearRange();
    } else {
      this.month_year_label = header_div.build("span")
    }
  },
  initCalendarGrid: function() {
    var body_div = this.body_div;
    this.calendar_day_grid = [];
    var days_table = body_div.build("table", { cellPadding: "0px", cellSpacing: "0px", width: "100%" })
    // make the weekdays!
    var weekdays_row = days_table.build("thead").build("tr");
    Date.weekdays.each( function(weekday) {
      weekdays_row.build("th", {innerHTML: weekday});
    });

    var days_tbody = days_table.build("tbody")
    // Make the days!
    var row_number = 0, weekday;
    for(var cell_index = 0; cell_index<42; cell_index++)
    {
      weekday = (cell_index+Date.first_day_of_week ) % 7;
      if ( cell_index % 7==0 ) days_row = days_tbody.build("tr", {className: 'row_'+row_number++});
      (this.calendar_day_grid[cell_index] = days_row.build("td", {
          calendar_date_select: this,
          onmouseover: function () { this.calendar_date_select.dayHover(this); },
          onmouseout: function () { this.calendar_date_select.dayHoverOut(this) },
          onclick: function() { this.calendar_date_select.updateSelectedDate(this, true); },
          className: (weekday==0) || (weekday==6) ? " weekend" : "" //clear the class
        },
        { cursor: "pointer" }
      )).build("div");
      this.calendar_day_grid[cell_index];
    }
  },
  initButtonsDiv: function()
  {
    var buttons_div = this.buttons_div;
    if (this.options.get("time"))
    {
      var blank_time = $A(this.options.get("time")=="mixed" ? [[" - ", ""]] : []);
      buttons_div.build("span", {innerHTML:"@", className: "at_sign"});

      var t = new Date();
      this.hour_select = new SelectBox(buttons_div,
        blank_time.concat($R(0,23).map(function(x) {t.setHours(x); return $A([t.getAMPMHour()+ " " + t.getAMPM(),x])} )),
        {
          calendar_date_select: this,
          onchange: function() { this.calendar_date_select.updateSelectedDate( { hour: this.value });},
          className: "hour"
        }
      );
      buttons_div.build("span", {innerHTML:":", className: "seperator"});
      var that = this;
      this.minute_select = new SelectBox(buttons_div,
        blank_time.concat($R(0,59).select(function(x){return (x % that.options.get('minute_interval')==0)}).map(function(x){ return $A([ Date.padded2(x), x]); } ) ),
        {
          calendar_date_select: this,
          onchange: function() { this.calendar_date_select.updateSelectedDate( {minute: this.value }) },
          className: "minute"
        }
      );

    } else if (! this.options.get("buttons")) buttons_div.remove();

    if (this.options.get("buttons")) {
      buttons_div.build("span", {innerHTML: "&#160;"});
      if (this.options.get("time")=="mixed" || !this.options.get("time")) b = buttons_div.build("a", {
          innerHTML: _translations["Today"],
          href: "#",
          onclick: function() {this.today(false); return false;}.bindAsEventListener(this)
        });

      if (this.options.get("time")=="mixed") buttons_div.build("span", {innerHTML: " | ", className:"button_seperator"})

      if (this.options.get("time")) b = buttons_div.build("a", {
        innerHTML: _translations["Now"],
        href: "#",
        onclick: function() {this.today(true); return false}.bindAsEventListener(this)
      });

      if (!this.options.get("embedded"))
      {
        buttons_div.build("span", {innerHTML: "&#160;"});
        buttons_div.build("a", { innerHTML: _translations["OK"], href: "#", onclick: function() {this.close(); return false;}.bindAsEventListener(this) });
      }
    }
  },
  refresh: function ()
  {
    this.refreshMonthYear();
    this.refreshCalendarGrid();

    this.setSelectedClass();
    this.updateFooter();
  },
  refreshCalendarGrid: function () {
    this.beginning_date = new Date(this.date).stripTime();
    this.beginning_date.setDate(1);
    this.beginning_date.setHours(12); // Prevent daylight savings time boundaries from showing a duplicate day
    var pre_days = this.beginning_date.getDay() // draw some days before the fact
    if (pre_days < 3) pre_days += 7;
    this.beginning_date.setDate(1 - pre_days + Date.first_day_of_week);

    var iterator = new Date(this.beginning_date);

    var today = new Date().stripTime();
    var this_month = this.date.getMonth();
    vdc = this.options.get("valid_date_check");
    for (var cell_index = 0;cell_index<42; cell_index++)
    {
      day = iterator.getDate(); month = iterator.getMonth();
      cell = this.calendar_day_grid[cell_index];
      Element.remove(cell.childNodes[0]); div = cell.build("div", {innerHTML:day});
      if (month!=this_month) div.className = "other";
      cell.day = day; cell.month = month; cell.year = iterator.getFullYear();
      if (vdc) { if (vdc(iterator.stripTime())) cell.removeClassName("disabled"); else cell.addClassName("disabled") };
      iterator.setDate( day + 1);
    }

    if (this.today_cell) this.today_cell.removeClassName("today");

    if ( $R( 0, 41 ).include(days_until = this.beginning_date.stripTime().daysDistance(today)) ) {
      this.today_cell = this.calendar_day_grid[days_until];
      this.today_cell.addClassName("today");
    }
  },
  refreshMonthYear: function() {
    var m = this.date.getMonth();
    var y = this.date.getFullYear();
    // set the month
    if (this.options.get("month_year") == "dropdowns")
    {
      this.month_select.setValue(m, false);

      var e = this.year_select.element;
      if (this.flexibleYearRange() && (!(this.year_select.setValue(y, false)) || e.selectedIndex <= 1 || e.selectedIndex >= e.options.length - 2 )) this.populateYearRange();

      this.year_select.setValue(y);

    } else {
      this.month_year_label.update( Date.months[m] + " " + y.toString()  );
    }
  },
  populateYearRange: function() {
    this.year_select.populate(this.yearRange().toArray());
  },
  yearRange: function() {
    if (!this.flexibleYearRange())
      return $R(this.options.get("year_range")[0], this.options.get("year_range")[1]);

    var y = this.date.getFullYear();
    return $R(y - this.options.get("year_range"), y + this.options.get("year_range"));
  },
  flexibleYearRange: function() { return (typeof(this.options.get("year_range")) == "number"); },
  validYear: function(year) { if (this.flexibleYearRange()) { return true;} else { return this.yearRange().include(year);}  },
  dayHover: function(element) {
    var hover_date = new Date(this.selected_date);
    hover_date.setYear(element.year); hover_date.setMonth(element.month); hover_date.setDate(element.day);
    this.updateFooter(hover_date.toFormattedString(this.use_time));
  },
  dayHoverOut: function(element) { this.updateFooter(); },
  clearSelectedClass: function() {if (this.selected_cell) this.selected_cell.removeClassName("selected");},
  setSelectedClass: function() {
    if (!this.selection_made) return;
    this.clearSelectedClass()
    if ($R(0,42).include( days_until = this.beginning_date.stripTime().daysDistance(this.selected_date.stripTime()) )) {
      this.selected_cell = this.calendar_day_grid[days_until];
      this.selected_cell.addClassName("selected");
    }
  },
  reparse: function() { this.parseDate(); this.refresh(); },
  dateString: function() {
    return (this.selection_made) ? this.selected_date.toFormattedString(this.use_time) : "&#160;";
  },
  parseDate: function()
  {
    var value = $F(this.target_element).strip()
    this.selection_made = (value != "");
    this.date = value=="" ? NaN : Date.parseFormattedString(this.options.get("date") || value);
    if (isNaN(this.date)) this.date = new Date();
    if (!this.validYear(this.date.getFullYear())) this.date.setYear( (this.date.getFullYear() < this.yearRange().start) ? this.yearRange().start : this.yearRange().end);
    this.selected_date = new Date(this.date);
    this.use_time = /[0-9]:[0-9]{2}/.exec(value) ? true : false;
    this.date.setDate(1);
  },
  updateFooter:function(text) { if (!text) text = this.dateString(); this.footer_div.purgeChildren(); this.footer_div.build("span", {innerHTML: text }); },
  updateSelectedDate:function(partsOrElement, via_click) {
    var parts = $H(partsOrElement);
    if ((this.target_element.disabled || this.target_element.readOnly) && this.options.get("popup") != "force") return false;
    if (parts.get("day")) {
      var t_selected_date = this.selected_date, vdc = this.options.get("valid_date_check");
      for (var x = 0; x<=3; x++) t_selected_date.setDate(parts.get("day"));
      t_selected_date.setYear(parts.get("year"));
      t_selected_date.setMonth(parts.get("month"));

      if (vdc && ! vdc(t_selected_date.stripTime())) { return false; }
      this.selected_date = t_selected_date;
      this.selection_made = true;
    }

    if (!isNaN(parts.get("hour"))) this.selected_date.setHours(parts.get("hour"));
    if (!isNaN(parts.get("minute"))) this.selected_date.setMinutes( Math.floor_to_interval(parts.get("minute"), this.options.get("minute_interval")) );
    if (parts.get("hour") === "" || parts.get("minute") === "")
      this.setUseTime(false);
    else if (!isNaN(parts.get("hour")) || !isNaN(parts.get("minute")))
      this.setUseTime(true);

    this.updateFooter();
    this.setSelectedClass();

    if (this.selection_made) this.updateValue();
    if (this.closeOnClick()) { this.close(); }
    if (via_click && !this.options.get("embedded")) {
      if ((new Date() - this.last_click_at) < 333) this.close();
      this.last_click_at = new Date();
    }
  },
  closeOnClick: function() {
    if (this.options.get("embedded")) return false;
    if (this.options.get("close_on_click")===nil )
      return (this.options.get("time")) ? false : true
    else
      return (this.options.get("close_on_click"))
  },
  navMonth: function(month) { (target_date = new Date(this.date)).setMonth(month); return (this.navTo(target_date)); },
  navYear: function(year) { (target_date = new Date(this.date)).setYear(year); return (this.navTo(target_date)); },
  navTo: function(date) {
    if (!this.validYear(date.getFullYear())) return false;
    this.date = date;
    this.date.setDate(1);
    this.refresh();
    this.callback("after_navigate", this.date);
    return true;
  },
  setUseTime: function(turn_on) {
    this.use_time = this.options.get("time") && (this.options.get("time")=="mixed" ? turn_on : true) // force use_time to true if time==true && time!="mixed"
    if (this.use_time && this.selected_date) { // only set hour/minute if a date is already selected
      var minute = Math.floor_to_interval(this.selected_date.getMinutes(), this.options.get("minute_interval"));
      var hour = this.selected_date.getHours();

      this.hour_select.setValue(hour);
      this.minute_select.setValue(minute)
    } else if (this.options.get("time")=="mixed") {
      this.hour_select.setValue(""); this.minute_select.setValue("");
    }
  },
  updateValue: function() {
    var last_value = this.target_element.value;
    this.target_element.value = this.dateString();
    if (last_value!=this.target_element.value) this.callback("onchange");
  },
  today: function(now) {
    var d = new Date(); this.date = new Date();
    var o = $H({ day: d.getDate(), month: d.getMonth(), year: d.getFullYear(), hour: d.getHours(), minute: d.getMinutes()});
    if ( ! now ) o = o.merge({hour: "", minute:""});
    this.updateSelectedDate(o, true);
    this.refresh();
  },
  close: function() {
    if (this.closed) return false;
    this.callback("before_close");
    this.target_element.calendar_date_select = nil;
    Event.stopObserving(document, "mousedown", this.closeIfClickedOut_handler);
    Event.stopObserving(document, "keypress", this.keyPress_handler);
    this.calendar_div.remove(); this.closed = true;
    if (this.iframe) this.iframe.remove();
    if (this.target_element.type!="hidden") this.target_element.focus();
    this.callback("after_close");
  },
  closeIfClickedOut: function(e) {
    if (! $(Event.element(e)).descendantOf(this.calendar_div) ) this.close();
  },
  keyPress: function(e) {
    if (e.keyCode==Event.KEY_ESC) this.close();
  },
  callback: function(name, param) { if (this.options.get(name)) { this.options.get(name).bind(this.target_element)(param); } }


}

// OFBiz addition: modified format_iso_date.js, included here for convenience
Date.prototype.toFormattedString = function(include_time) {
    var str = this.getFullYear() + "-" + Date.padded2(this.getMonth() + 1) + "-" +Date.padded2(this.getDate());
    if (include_time) {
        str += " " + this.getHours() + ":" + this.getPaddedMinutes() + ":" + Date.padded2(this.getSeconds());
        if (this.getMilliseconds > 0) {
            str += "." + (this.getMilliseconds() < 100 ? '0' : '') + (this.getMilliseconds() < 10 ? '0' : '') + this.getMilliseconds();
        } else {
            str += ".0";
        }
    }
    return str;
};

Date.parseFormattedString = function (string) {
    var arr_datetime = string.split(' ');
    var str_date = arr_datetime[0];
    var str_time = arr_datetime[1];

    var arr_date = str_date.split('-');
    var dt_date = new Date();
    dt_date.setDate(1);
    dt_date.setMonth(arr_date[1]-1);
    if (arr_date[0] < 100) arr_date[2] = Number(arr_date[0]) + (arr_date[0] < 40 ? 2000 : 1900);
    dt_date.setFullYear(arr_date[0]);
    var dt_numdays = new Date(arr_date[0], arr_date[1], 0);
    dt_date.setDate(arr_date[2]);

    var arr_time = String(str_time ? str_time : '').split(':');
    if (arr_time.size() >= 1 && arr_time[0] != '') {
      dt_date.setHours(arr_time[0]);
      dt_date.setMinutes(arr_time[1]);
      var arr_sec = String(arr_time[2] ? arr_time[2] : '').split('.');
      dt_date.setSeconds(arr_sec[0]);
      if (!arr_sec[1]) dt_date.setMilliseconds(0);
      dt_date.setMilliseconds(arr_sec[1]);
    }
    return dt_date;
};

// OFBiz addition: functions to call the calendar
function call_cal(target, datetime) {
    new CalendarDateSelect(target, {time:true, year_range:10} );
}

function call_cal_notime(target, datetime) {
    new CalendarDateSelect(target, {year_range:10} );
}
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


/* Wrapper functions for external libraries*/


dojo.require("dojo.fx");
dojo.require("dojo.dnd.source");
dojo.require("dojo.dnd.manager");
dojo.require("dojo.currency");
dojo.require("dojo.data.JsonItemStore");
dojo.require("dojo.data.JsonItemStoreAutoComplete");
dojo.require("dijit.Tree");
dojo.require("dojo.parser");
dojo.require("dijit.form.ComboBox");

opentaps.escapeHTML = function(text) {
    return text.split("&").join("&amp;").split("<").join("&lt;").split(">").join("&gt;").split("\"").join("&quot;");
}

opentaps.trim = function(text) {
    return text.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
}

opentaps.escapeHTML = function(text) {
    return text.split("&").join("&amp;").split("<").join("&lt;").split(">").join("&gt;")
}

opentaps.getBoundsByMargin = function(/* Node */ node) {
    if (! node) return;
    return dojo.marginBox(node); 
}

opentaps.getRequest = function(/* String */ requestName, /* Object */ context, /* Function */ handler, /* Boolean */ asJson, /* Number */ timeout) {
    asJson = asJson ? asJson : true;
    timeout = timeout ? timeout : (ajaxDefaultTimeOut ? ajaxDefaultTimeOut : 10000);
    var request = dojo.xhrPost({
      url: requestName,
      content: context,
      timeout: timeout,
      preventCache: true,
      handleAs: asJson ? 'json' : 'text'
    });
    return request;
}

opentaps.show = function(/* Node */ node, /* Number */ duration) {
    if (! duration) duration = 200;
    dojo.fadeIn({ node: node, duration: duration }).play();
}

opentaps.hide = function(/* Node */ node, /* Number */ duration) {
    if (! duration) duration = 200;
    dojo.fadeOut({ node: node, duration: duration }).play();
}

opentaps.addListenerToNode = function(/* Node */ node, /* String */ eventName, /* Function */ functionToAdd) {
    if (! node) return;
    dojo.connect(node, eventName, functionToAdd);
}

opentaps.addOnLoad = function(/* Function */ functionToAdd) {
    dojo.addOnLoad(functionToAdd);
}

opentaps.addClass = function(/* Node */ node, /* String */ newClass) {
    if (! node) return;
    dojo.addClass(node, newClass);
}

opentaps.removeClass = function(/* Node */ node, /* String */ oldClass) {
    if (! node) return;
    dojo.removeClass(node, oldClass);
}

opentaps.toggleClass = function( /* Node */ node, /* String */ className) {
    if (! node) return;
    return dojo.toggleClass(node, className);
}

opentaps.replaceClass = function(/* Node */ node, /* String */ newClass, /* String */ oldClass) {
    if (! node) return;
    dojo.removeClass(node, oldClass);
    dojo.addClass(node, newClass);
}

opentaps.evalJson = function(/* String */ jsonString) {
    return dojo.fromJson(jsonString);
}

opentaps.shrinkAndFade = function(/* Node */ node) {
    if (! node) return;
    var open = 'true' == node.getAttribute('open');
    if (open) {
        opentaps.hide(node, 200);
        return dojo.fx.wipeOut({ node: node, duration:300 }).play();
    } else {
        opentaps.show(node, 300);
        return dojo.fx.wipeIn({ node: node, duration:300 }).play();
    }
}

opentaps.replaceNode = function(/* Element */ oldNode, /* Element */ newNode) {
    if (! oldNode) return;
    if (! newNode) return;
    dojo.place(newNode, oldNode, 'before');
    opentaps.removeNode(oldNode);
}

opentaps.insertBefore = function(/* Element */ nodeToInsert, /* Element */ node) {
    if (! nodeToInsert) return;
    if (! node) return;
    dojo.place(nodeToInsert, node, 'before');
}

opentaps.insertAfter = function(/* Element */ nodeToInsert, /* Element */ node) {
    if (! nodeToInsert) return;
    if (! node) return;
    dojo.place(nodeToInsert, node, 'after');
}

opentaps.makeDropTarget = function(/* String */ elementId) {
    return new dojo.dnd.Source(elementId, {creator: function(data){ return {node: data.element, data: data}}});
}

opentaps.makeDragSource = function(/* dojo.dnd.Source */ dropTarget, /* Element */ dragSourceElement) {
    if (! dropTarget) return;
    dropTarget.insertNodes(false, [ {element: dragSourceElement} ]);
}

// there is no need to specify the user locale as the dojo locale is already setup globally with the user locale
// only give a locale when the locale should be overridden
opentaps.formatCurrency = function( /* Number */ amount, /* String */ currencyUomId, /* String */ localex) {
  // dojo do not support locale as JAVA output them, need to convert en_US => en-us
  if (localex != null) {
    localex = localex.toLowerCase();
    localex = localex.replace("_", "-");
    return dojo.currency.format(amount, {currency: currencyUomId, locale: localex});
  } else {
    return dojo.currency.format(amount, {currency: currencyUomId});
  }
}

// Extending Dojo's tree widget so that we can control formatting and the initial expanded or contracted state of all branches
dojo.declare("opentaps.GLAccountTree", dijit.Tree, {

    formatValueNode: function(node, value) {
        return opentaps.createSpan(null, value, 'amount');
    },

    createExpandedValueNode: function (node) {
        node.expandedValueNode = this.formatValueNode(node, node.amountSelf);
    },

    createCollapsedValueNode: function (node) {
        node.collapsedValueNode = this.formatValueNode(node, node.amountSelfAndChildren);
    },

    createValueNode: function (node) {
        var balanceOfSelf = this.tree.store.getValue(node.item, 'balanceOfSelf');
        var balanceOfSelfAndChildren = this.tree.store.getValue(node.item, 'balanceOfSelfAndChildren');
        var balanceOfSelfStr = opentaps.formatCurrency(balanceOfSelf, this.currencyUomId, this.locale);
        var balanceOfSelfAndChildrenStr = opentaps.formatCurrency(balanceOfSelfAndChildren, this.currencyUomId, this.locale);
        // set the balances as node attributes so we can retreive them later on toggle
        node.amountSelf = balanceOfSelfStr;
        node.amountSelfAndChildren = balanceOfSelfAndChildrenStr;
        // create the value nodes
        this.createExpandedValueNode(node);
        this.createCollapsedValueNode(node);
        // show the value according to the default state
        if (this.defaultExpanded) {
          return node.expandedValueNode;
        } else {
          return node.collapsedValueNode;
        }
    },

    expander: function (node) {
        if (node.declaredClass == 'dijit._TreeNode') {
            dojo.connect(node, 'addChild', this, 'expander');
            var glAccountId = this.tree.store.getIdentity(node.item);
            var glAccountName = this.tree.store.getValue(node.item, 'name');
            var label = opentaps.createSpan(null, '', 'account');
            label.appendChild(opentaps.createAnchor(null, 'AccountActivitiesDetail?glAccountId=' + glAccountId + '&amp;organizationPartyId=' + this.organizationPartyId, glAccountId, 'linktext'));
            label.appendChild(opentaps.createSpan(null, ':&nbsp;' + glAccountName));
            opentaps.replaceChildren(node.labelNode, label);
            node.labelNode.appendChild(this.createValueNode(node));
            node.expanded = this.defaultExpanded;
        }
        if (this.defaultExpanded) {
          if (node.isFolder) {
            this._controller._expand(node);
          }
        }
    },

    // The Dojo Tree widget intercepts all onclick events and publishes an 'execute' message, without allowing the event to propagate. Subscribing to this execute
    //  event doesn't allow us to determine which part of the node was clicked (EG. link vs. text) so we're overriding the _onClick function.
    //  If the source of the event is a link, do nothing - otherwise call the superclass _onClick function.
    _onClick: function (/* Event */ e) {
        var domElement = e.target;
        if (domElement.nodeName.toUpperCase() == 'A') return;
        dijit.Tree.prototype._onClick.apply(this, arguments);
    },
    addChild: function () {
        dijit.Tree.prototype.addChild.apply(this, arguments);
        this.expander(arguments[0], 1);
    },
    postCreate: function () {
        dijit.Tree.prototype.postCreate.apply(this, arguments);
        dojo.subscribe(this.id, 'toggleOpen', function(arg) {
            opentaps.removeNode(arg.node.labelNode.lastChild);
            if (arg.node.isExpanded) {
              arg.node.labelNode.appendChild(arg.node.expandedValueNode);
            } else {
              arg.node.labelNode.appendChild(arg.node.collapsedValueNode);
            }
        });
    },
    expandAll: function () {
      var children = this.getChildren();
      for(var i=0; i < children.length; i++) {
        var child = children[i];
        if (!child.isExpanded) {
          dojo.publish(this.id, [dojo.mixin({tree: this, event: 'toggleOpen', node: child})]);
        }
        var descendants = child.getDescendants();
        for(var j=0; j < descendants.length; j++) {
          var descendant = descendants[j];
          if (descendant.isFolder && !descendant.isExpanded) {
            dojo.publish(this.id, [dojo.mixin({tree: this, event: 'toggleOpen', node: descendant})]);
          }
        }
      }
    },
    collapseAll: function () {
      var children = this.getChildren();
      for(var i=0; i < children.length; i++) {
        var child = children[i];
        var descendants = child.getDescendants();
        for(var j=descendants.length-1; j >= 0; j--) {
          var descendant = descendants[j];
          if (descendant.isFolder && descendant.isExpanded) {
            dojo.publish(this.id, [dojo.mixin({tree: this, event: 'toggleOpen', node: descendant})]);
          }
        }
        if (child.isExpanded) {
          dojo.publish(this.id, [dojo.mixin({tree: this, event: 'toggleOpen', node: child})]);
        }
      }
    }
});

// Extend opentaps.GLAccountTree to provide specific two column value formating.
dojo.declare("opentaps.GLAccountTree2Col", opentaps.GLAccountTree, {

    formatValueNode: function (node, value) {
        var debitCredit = this.tree.store.getValue(node.item, 'debitCredit');
        var table = opentaps.createElement(null, 'table');
        opentaps.addClass(table, 'twoColumnAmount');
        var tbody = opentaps.createElement(null, 'tbody');
        table.appendChild(tbody);
        var row = opentaps.createTableRow();
        if (debitCredit == 'DEBIT') {
            row.appendChild(opentaps.createTableCell(null, 'debitAmount', value));
            row.appendChild(opentaps.createTableCell(null, 'creditAmount', ''));
        } else if (debitCredit == 'CREDIT') {
            row.appendChild(opentaps.createTableCell(null, 'debitAmount', ''));
            row.appendChild(opentaps.createTableCell(null, 'creditAmount', value));
        }
        tbody.appendChild(row);
        var outerNode = opentaps.createSpan(null, null, 'amount');
        outerNode.appendChild(table);
        return outerNode;
    }

});

opentaps.expandGlAccountTree = function(/* String */ treeDomId) {
    var tree = dijit.byId(treeDomId);
    if (tree == null) return;
    tree.expandAll();
}

opentaps.collapseGlAccountTree = function(/* String */ treeDomId) {
    var tree = dijit.byId(treeDomId);
    if (tree == null) return;
    tree.collapseAll();
}

// Wrapping Dojo's JSONStore class
dojo.declare("opentaps.GLAccountJsonStore", dojo.data.JsonItemStore, {});


/* Opentaps DOM functions */


opentaps.uniqueIdSequence = 0;
opentaps.getUniqueId = function() {
  return "opentaps_unique_id_" + (++opentaps.uniqueIdSequence);
}

opentaps.createElement = function(/* String */ id, /* String */ nodeName, /* String */ innerHtml, /* String */ className, /* Array */ eventFunctions, /* String */ src, /* String */ altText) {
    if (! nodeName) return;
    var newValue = document.createElement(nodeName);
    if (innerHtml) newValue.innerHTML = innerHtml;
    if (src) newValue.src = src;
    if (id) newValue.id = id;
    if (className) opentaps.addClass(newValue, className);
    if (eventFunctions) {
        for (var eventName in eventFunctions) {
            opentaps.addListenerToNode(newValue, eventName, eventFunctions[eventName]);
        }
    }
    return newValue;
}

opentaps.createSpan = function(/* String */ id, /* String */ innerHtml, /* String */ className, /* Array */ eventFunctions) {
    return opentaps.createElement(id, 'span', innerHtml, className, eventFunctions);
}

opentaps.createDiv = function(/* String */ id, /* String */ innerHtml, /* String */ className, /* Array */ eventFunctions) {
    return opentaps.createElement(id, 'div', innerHtml, className, eventFunctions);
}

opentaps.createImg = function(/* String */ id, /* String */ src, /* String */ className, /* Array */ eventFunctions, /* String */ altText) {
    var newValue = opentaps.createElement(id, 'img', null, className, eventFunctions, src, altText);
    newValue.border = 0;
    return newValue;
}

opentaps.createAnchor = function(/* String */ id, /* String */ href, /* String */ innerHtml, /* String */ className, /* Array */ eventFunctions) {
    var newValue = opentaps.createElement(id, 'a', innerHtml, className, eventFunctions);
    if (href) newValue.href = href;
    return newValue;
}

opentaps.createInput = function(/* String */ id, /* String */ name, /* String */ type, /* String */ className, /* Array */ eventFunctions, /* String */ value, /* String */ size) {
    var newValue = opentaps.createElement(id, 'input', null, className, eventFunctions);
    if (type) newValue.type = type;
    newValue.name = name ? name : id;
    if (value) newValue.value = value;
    if (size) newValue.size = size;
    return newValue;
}

opentaps.createTextarea = function(/* String */ id, /* String */ name, /* String */ className, /* Array */ eventFunctions, /* String */ value, /* String */ rows, /* String */ cols) {
    var newValue = opentaps.createElement(id, 'textarea', null, className, eventFunctions);
    newValue.name = name ? name : id;
    if (value) newValue.innerHTML = value;
    if (rows) newValue.rows = rows;
    if (cols) newValue.cols = cols;
    return newValue;
}

opentaps.createTableRow = function(/* String */ id, /* String */ className, /* Number */ numberOfCells) {
    var row = opentaps.createElement(id, 'tr', null, className);
    if (numberOfCells) {
        for (var x = 0; x < numberOfCells; x++) {
            row.appendChild(opentaps.createElement(null, 'td'));
        }
    }
    return row;
}

opentaps.createTableCell = function(/* String */ id, /* String */ className, /* String */ innerHtml, /* String */ align, /* String */ colspan) {
    var cell = opentaps.createElement(id, 'td', innerHtml, className);
    if (align) cell.align = align;
    if (colspan) cell.colspan = colspan;
    return cell;
}

/**
 * The options param is in the form [option1Value, option1Text, option2Value, option2Text...] Trailing odd values will be discarded.
 */
opentaps.createSelect = function(/* String */ id, /* String */ name, /* String */ className, /* Array */ options, /* String */ selectedValue, /* Array */ eventFunctions) {
    if (! options) options = [];
    if (options.length % 2 !=  0) x.pop();
    var newValue = opentaps.createElement(id, 'select', null, className, eventFunctions);
    newValue.name = name ? name : id;
    var idx = 0;
    for (var x = 0; x < options.length; x = x + 2) {
        newValue.options[idx] = new Option(options[x + 1], options[x], options[x] == selectedValue, options[x] == selectedValue);
        idx++;
    }
    return newValue;
}

opentaps.removeNode = function(/* Element */ node) {
    if (! node) return;
    node.parentNode.removeChild(node)
}

opentaps.removeChildNodes = function(/* Element */ node) {
    if (! node) return;
    for (var i = node.childNodes.length - 1; i >= 0; i--) {
        opentaps.removeNode(node.childNodes[i]);
    }
}

opentaps.replaceChildren = function(/* Element */ parentNode, /* Element */ newNode) {
    if (! newNode) return;
    if (! parentNode) return;
    opentaps.removeChildNodes(parentNode);
    parentNode.appendChild(newNode);
}

/* Date/time functions */

/*
 opentaps.parse[format]Date functions use format pattern symbols in jscalendar style 
 and they are just wrappers for Date.print() and Date.parseDate() custom methods.
 This methods should be added to Date class during calendar.js load.
 
    %a abbreviated weekday name
    %A full weekday name
    %b abbreviated month name
    %B full month name
    %C century number
    %d the day of the month ( 00 .. 31 )
    %e the day of the month ( 0 .. 31 )
    %H hour ( 00 .. 23 )
    %I hour ( 01 .. 12 )
    %j day of the year ( 000 .. 366 )
    %k hour ( 0 .. 23 )
    %l hour ( 1 .. 12 )
    %m month ( 01 .. 12 )
    %M minute ( 00 .. 59 )
    %n a newline character
    %p "PM" or "AM"
    %P "pm" or "am"
    %S second ( 00 .. 59 )
    %s number of seconds since Epoch (since Jan 01 1970 00:00:00 UTC)
    %t a tab character
    %U, %W, %V the week number5
    %u the day of the week ( 1 .. 7, 1 = MON )
    %w the day of the week ( 0 .. 6, 0 = SUN )
    %y year without the century ( 00 .. 99 )
    %Y year including the century ( ex. 1979 )
    %% a literal % character
*/

opentaps.parseDate = function(/*String*/date, /*String*/fmt) {
	if (!date || date.length == 0 || !fmt || fmt.length == 0) {
		alert("Illegal arguments are passed to function opentaps.parseDate.");
		return null;
	}
	if (typeof Date.parseDate != "function") {
		alert("Function Date.parseDate is unavailable! Load calendar.js before this script.");
		return null;
	}
	var dateObj = Date.parseDate(date, fmt);
	return dateObj;
}

opentaps.formatDate = function(/*Date*/date, /*String*/fmt) {
	if (!date || !fmt || fmt.length == 0) {
		alert("Illegal arguments are passed to function opentaps.formatDate.");
		return null;
	}
	if (typeof Date.prototype.print != "function") {
		alert("Date.print is unavailable! Load calendar.js before this script.");
		return null;
	}
	return date.print(fmt);
}

/* Opentaps functions */


opentaps.sendRequest = function(/* String */ requestName, /* Object */ context, /* Function */ handler, /* Object */ spinnerData, /* Boolean */ asJson, /* Number */ timeout) {

    var request = opentaps.getRequest(requestName, context, handler, asJson, timeout);
    
    if (spinnerData && spinnerData.target) {
        
        // Ensure a DOM element and ID for the target
        if (typeof(spinnerData.target) == 'string') spinnerData.target = document.getElementById(spinnerData.target);
        if (! spinnerData.target.id) spinnerData.target.id = opentaps.getUniqueId();
        
        // Establish defaults for the spinner
        if (! spinnerData.colour) spinnerData.colour = configProperties.bgColor;
        spinnerData.colour = spinnerData.colour.replace('#', '');
        if (! spinnerData.src) spinnerData.src = '/opentaps_images/spinners/roundPointy_' + spinnerData.colour + '.gif';
        if (! spinnerData.size) spinnerData.size = 14;
        
        // Remove the spinner container if it already exists
        if (document.getElementById(spinnerData.target.id + '_spinnerContainer')) opentaps.removeNode(document.getElementById(spinnerData.target.id + '_spinnerContainer'));

        // Create a container of the same size and position as the target, so that the container overlays the target
        var spinnerContainer = opentaps.createDiv(spinnerData.target.id + '_spinnerContainer', null, 'spinner');
        var box = opentaps.getBoundsByMargin(spinnerData.target); 
        spinnerContainer.style.position = 'absolute'; 
        spinnerContainer.style.height = box.h + 'px'; 
        spinnerContainer.style.width = box.w + 'px'; 
        spinnerContainer.style.left = box.l + 'px'; 
        spinnerContainer.style.top = box.t + 'px'; 
        
        // Add the spinner centered vertically in the container
        <!-- [if !IE]>spinnerContainer.style.paddingTop = (box.h - spinnerData.size) / 2 + 'px';<![endif]-->
        var spinner = opentaps.createImg(spinnerData.target.id + '_spinner', spinnerData.src);
        spinner.style.width = spinnerData.size + 'px';
        spinnerContainer.appendChild(spinner);
        opentaps.insertAfter(spinnerContainer, spinnerData.target);

        // Show the spinner and hide the target
        opentaps.show(spinnerContainer, 100);
        opentaps.hide(spinnerData.target, 100);

        // Add the callback to remove the spinner and show the target
        request.addCallback(function(response){
            opentaps.removeNode(document.getElementById(spinnerData.target.id + '_spinnerContainer'));
            opentaps.show(document.getElementById(spinnerData.target.id), 100);
            return response;
        });
    }
    
    // Add the callback to trigger the required function
    if (handler) {
        request.addCallback(function(response){
            handler.call(this, response);
            return response;
        });
    }
    
    // Add a callback to check for an error and hide the target and replace the spinner image with an error image,
    //  if an error exists
    if (spinnerData && spinnerData.target) {
        request.addBoth(function(response){
            if (response instanceof Error) {
                opentaps.hide(document.getElementById(spinnerData.target.id), 100);
                var spinner = document.getElementById(spinnerData.target.id + '_spinner');
                if (spinner) spinner.src = '/opentaps_images/buttons/glass_button_red_excl.png';
            }
        });
    
    }
}

/**
 * Javascript's closures can be confusing. Adding a handler to a node inside a loop, like this:
 *     
 *     for (var x = 1; x <= 3; x++) {
 *         var link = opentaps.createAnchor(null, 'this is link #' + x, text, null, {'onclick' : function(){ alert(x) }});
 *         document.appendChild(link)
 *     }
 *     
 * will result in three links with the correct link numbers, but which will each each display an alert box reading '3', because the closures inherit the *final* state of the counter.
 * 
 * Use this function to generate closures which will inherit the state of the variables during the loop:
 *     
 *     for (var x = 1; x <= 3; x++) {
 *         var link = opentaps.createAnchor(null, 'this is link #' + x, text, null, {'onclick' : opentaps.makeFunction('alert', [x])});
 *         document.appendChild(link)
 *     }
 * 
 * this example will generate three links with the correct link numbers, and alert boxes displaying '1', '2' or '3' depending on which link is clicked.
 *     
 */
opentaps.makeFunction = function(/* String */ functionName, /* Array */ params) {
    if (! functionName) return;
    if (! params) params = [];
    var paramString = '';
    for (var x = 0; x < params.length; x++) {
        if (typeof(params[x]) == 'string') {
            paramString += "'" + params[x] + "'";
        } else {
            paramString += params[x];
        }
        if (x + 1 != params.length) paramString += ', ';
    }
    return new Function(functionName + '(' + paramString + ')');
}

opentaps.stripWhitespace = function( /* String */ string) {
    return string.replace(/^\s+/, '').replace(/\s+$/, '');
}

opentaps.replaceFormElementValue = function(/* Element */ node, /* String */ value) {
    if ((! node) || (! opentaps.stripWhitespace(value))) return;
    if (node.nodeName.toUpperCase() == 'INPUT') {
        node.value = value;
    } else if (node.nodeName.toUpperCase() == 'SELECT') {
        if (! node.options) return;
        node.selectedIndex = 0;
        for (var i = 0; i < node.options.length; i++) {
            if (value == node.options[i].value) {
                node.selectedIndex = i;
                break;
            }
        }
    }
}

opentaps.replaceHtmlEditorValue = function(/* String */ textAreaId, /* String */ text) {
    var textArea = document.getElementById(textAreaId);
    if (! text || ! textArea) return;
    textArea.value = text;
    if (! FCKeditorAPI) return;
    var fckEditor = FCKeditorAPI.GetInstance(textAreaId);
    if (! fckEditor || ! fckEditor.EditorDocument || ! fckEditor.EditorDocument.body) return;
    fckEditor.EditorDocument.body.innerHTML = text;
}

// Confirm Popup for hypertext links
opentaps.confirmLinkAction = function(confirmText, href) {
    var answer = confirm(confirmText);
    if (answer) {
        if (href && href.length > 0) window.location = href;
    }
}

// Confirm Popup for forms
opentaps.confirmSubmitAction = function(confirmText, form) {
    var answer = confirm(confirmText);
    if (answer && form) {
      form.submit();
    }
}

// Function to execute either of the above depending on whether a href or form name is supplied (useful for macros)
opentaps.confirmAction = function(confirmText, href, formName, /*Array*/ params) {
    if (href && href.length > 0) {
        opentaps.confirmLinkAction(confirmText, href);
    } else if (formName && formName.length > 0) {
        form = document.forms[formName];
        if (form && params) {
          for (k in params) {
            var value = params[k];
            // convert JS undefined / null into an empty script else we confuse the service
            if (!value) value = "";
            form[k].value = value;
          }
        }
        opentaps.confirmSubmitAction(confirmText, form);
    }
}

opentaps.changeLocation = function (/*String*/ href, /*String*/ id) {
    if (href && href.length > 0) {
        window.location.href = href;
        return; 
    } else {
       var ctrl = document.getElementById(id);
       if (ctrl && ctrl.value) {
           window.location.href = ctrl.value;
       }
    }
}

opentaps.selectForm = function (/*String*/ id) {
  var ctrl = document.getElementById(id);
  if (ctrl && ctrl.value) {
    opentaps.submitForm(ctrl.value, null);
  }
}

opentaps.submitForm = function (/*String*/ formName, /*String*/ formId, /*Array*/ params) {
  var form;
  if (formName && formName.length > 0) {
    if (document.forms[formName]) {
      form = document.forms[formName];
    } else {
      alert("Cannot find form to submit with name [" + formName + "]");
    }
  } else if (formId && formId.length > 0) {
    var form = document.getElementById(formId);
  } else {
    alert("submitForm: no formName or formId was given !");
  }

  if (form && form.action) {
    if (params) {
      for (k in params) {
        var value = params[k];
        // convert JS undefined / null into an empty script else we confuse the service
        if (!value) value = "";
        form[k].value = value;
        }
    }
    form.submit();
    return;
  } else {
    alert("Cannot find form to submit with id [" + formId + "]");
  }
}

// Change a tax party dropdown
opentaps.changeTaxParty = function(geo, party) {
  var index = geo.selectedIndex;
  var partyId = geo.options[index].getAttribute("taxAuthPartyId");
  for (i = 0; i < party.options.length; i++) {
    if (party.options[i].value == partyId) {
      party.options[i].selected = true;
      break;
    }
  }
}

// Function to copy first set of address fields to second set, or to clear the second set.
// Note that this requires the fields to use camel case.  It also handles the dynamic state/country dropdowns.
opentaps.copyOrClearAddressFields = function(prefix1, prefix2, checkbox, defaultCountryGeoId) {
    var form = checkbox.form;
    var countriesSame = form[prefix1+"CountryGeoId"].value == form[prefix2+"CountryGeoId"].value;
    var container1 = document.getElementById(prefix1 + "AddressContainer");
    var container2 = document.getElementById(prefix2 + "AddressContainer");

    if (checkbox.checked) {
        form[prefix2+'ToName'].value = form[prefix1+'ToName'].value;
        form[prefix2+'AttnName'].value = form[prefix1+'AttnName'].value;
        form[prefix2+'Address1'].value = form[prefix1+'Address1'].value;
        form[prefix2+'Address2'].value = form[prefix1+'Address2'].value;
        form[prefix2+'City'].value = form[prefix1+'City'].value;
        form[prefix2+'PostalCode'].value = form[prefix1+'PostalCode'].value;
        form[prefix2+'PostalCodeExt'].value = form[prefix1+'PostalCodeExt'].value;
        if (!countriesSame) form[prefix2+'CountryGeoId'].value = form[prefix1+'CountryGeoId'].value;
        if (!countriesSame) {
            opentaps.swapStatesInDropdown(form[prefix2+'CountryGeoId'], prefix2+'StateProvinceGeoId');
        }
        form[prefix2+'StateProvinceGeoId'].value = form[prefix1+'StateProvinceGeoId'].value;

        form[prefix1+'ToName'].readOnly = true;
        form[prefix1+'AttnName'].readOnly = true;
        form[prefix1+'Address1'].readOnly = true;
        form[prefix1+'Address2'].readOnly = true;
        form[prefix1+'City'].readOnly = true;
        form[prefix1+'PostalCode'].readOnly = true;
        form[prefix1+'PostalCodeExt'].readOnly = true;
        form[prefix1+'CountryGeoId'].readOnly = true;
        form[prefix1+'StateProvinceGeoId'].readOnly = true;

        form[prefix2+'ToName'].readOnly = true;
        form[prefix2+'AttnName'].readOnly = true;
        form[prefix2+'Address1'].readOnly = true;
        form[prefix2+'Address2'].readOnly = true;
        form[prefix2+'City'].readOnly = true;
        form[prefix2+'PostalCode'].readOnly = true;
        form[prefix2+'PostalCodeExt'].readOnly = true;
        form[prefix2+'CountryGeoId'].readOnly = true;
        form[prefix2+'StateProvinceGeoId'].readOnly = true;

        if (container1) container1.className = 'readOnly';
        if (container2) container2.className = 'readOnly';
    } else {
        form[prefix2+'ToName'].value = '';
        form[prefix2+'AttnName'].value = '';
        form[prefix2+'Address1'].value = '';
        form[prefix2+'Address2'].value = '';
        form[prefix2+'City'].value = '';
        form[prefix2+'PostalCode'].value = '';
        form[prefix2+'PostalCodeExt'].value = '';
        form[prefix2+'CountryGeoId'].value = defaultCountryGeoId;
        if (!countriesSame) {
            opentaps.swapStatesInDropdown(form[prefix2+'CountryGeoId'], prefix2+'StateProvinceGeoId');
        }
        form[prefix2+'StateProvinceGeoId'].value = '';
        form[prefix2+'ToName'].readOnly = false;
        form[prefix2+'AttnName'].readOnly = false;
        form[prefix2+'Address1'].readOnly = false;
        form[prefix2+'Address2'].readOnly = false;
        form[prefix2+'City'].readOnly = false;
        form[prefix2+'PostalCode'].readOnly = false;
        form[prefix2+'PostalCodeExt'].readOnly = false;
        form[prefix2+'CountryGeoId'].readOnly = false;
        form[prefix2+'StateProvinceGeoId'].readOnly = false;

        form[prefix1+'ToName'].readOnly = false;
        form[prefix1+'AttnName'].readOnly = false;
        form[prefix1+'Address1'].readOnly = false;
        form[prefix1+'Address2'].readOnly = false;
        form[prefix1+'City'].readOnly = false;
        form[prefix1+'PostalCode'].readOnly = false;
        form[prefix1+'PostalCodeExt'].readOnly = false;
        form[prefix1+'CountryGeoId'].readOnly = false;
        form[prefix1+'StateProvinceGeoId'].readOnly = false;

        if (container1) container1.className = 'readOnlyRevert';
        if (container2) container2.className = 'readOnlyRevert';
    }
}

// function to swap states in a dropdown.
opentaps.swapStatesInDropdown = function(countryElement, stateElementName) {
    var stateElement = countryElement.form[stateElementName];
    var countryGeoId = countryElement[countryElement.selectedIndex].value;

    // use AJAX request to get the data
    opentaps.sendRequest(
                            "getStateDataJSON",
                            {"countryGeoId" : countryGeoId},
                            function(data) {opentaps.swapStatesInDropdownResponse(stateElement, data)}
                        );
}

// from the AJAX response, replace the given state options
opentaps.swapStatesInDropdownResponse = function(stateElement, states) {

    // build the state options
    stateElement.options[0] = new Option('', ''); // first element is always empty
    for (i = 0; i < states.length; i++) {
        state = states[i];
        stateElement.options[i+1] = new Option(state.geoName, state.geoId);
    }

    // by setting the length of the select option array, we can truncate it
    stateElement.options.length = states.length + 1;
}

// Functions to get and populate phone number fields from the current VoIP call
opentaps.getPhoneFieldsFromVoIPCall = function (/* Element */ formElement, /* Array */ countryCodeFields, /* Array */ areaCodeFields, /* Array */ numberFields) {
    opentaps.sendRequest("getCurrentIncomingNumberFromVoIPServer", {}, function(data) {opentaps.populatePhoneFieldsFromVoIPCall(formElement, countryCodeFields, areaCodeFields, numberFields, data)});
}
            
opentaps.populatePhoneFieldsFromVoIPCall = function (/* Element */ formElement, /* Array */ countryCodeFields, /* Array */ areaCodeFields, /* Array */ numberFields, /* Object */ data) {
    if (! data) return;
    if (countryCodeFields && data.countryCode) {
        for (var x = 0; x < countryCodeFields.length; x++) {
            var fieldEl = formElement ? formElement[countryCodeFields[x]] : document.getElementById(countryCodeFields[x]);
            if (fieldEl && ! fieldEl.value) fieldEl.value = data.countryCode;
        }
    }
    if (areaCodeFields && data.areaCode) {
        for (x = 0; x < areaCodeFields.length; x++) {
            fieldEl = formElement ? formElement[areaCodeFields[x]] : document.getElementById(areaCodeFields[x]);
            if (fieldEl && ! fieldEl.value) fieldEl.value = data.areaCode;
        }
    }
    if (numberFields && data.contactNumber) {
        for (x = 0; x < numberFields.length; x++) {
            fieldEl = formElement ? formElement[numberFields[x]] : document.getElementById(numberFields[x]);
            if (fieldEl && ! fieldEl.value) fieldEl.value = data.contactNumber;
        }
    }
}

/* Pagination functions */

// standard pagination response handler will put the raw data (HTML, text, etc.) in the container (see formletPlaceholder.ftl and formletMacros.ftl for what implements these)
opentaps.processPaginationResponse = function(paginatorName, data) {
    container = document.getElementById('paginate_' + paginatorName);
    container.innerHTML = data;
}
opentaps.getCurrentPage = function(paginatorName, appName, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('paginate', {'paginatorName' : paginatorName, 'action' : 'getCurrentPage', 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.getNextPage = function(paginatorName, appName, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('paginate', {'paginatorName' : paginatorName, 'action' : 'getNextPage', 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.getPreviousPage = function(paginatorName, appName, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('paginate', {'paginatorName' : paginatorName, 'action' : 'getPreviousPage', 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.getFirstPage = function(paginatorName, appName, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('paginate', {'paginatorName' : paginatorName, 'action' : 'getFirstPage', 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.getLastPage = function(paginatorName, appName, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('paginate', {'paginatorName' : paginatorName, 'action' : 'getLastPage', 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.getPageNumber = function(paginatorName, appName, pageNumber, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('paginate', {'paginatorName' : paginatorName, 'pageNumber' : pageNumber, 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.changePaginationOrder = function(paginatorName, appName, orderBy, orderByReverse, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('changePaginationOrder', {'paginatorName' : paginatorName, 'orderBy' : orderBy, 'orderByReverse' : orderByReverse, 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.changePaginationViewSize = function(paginatorName, appName, delta, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('changePaginationViewSize', {'paginatorName' : paginatorName, 'delta' : delta, 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.setPaginationViewSize = function(paginatorName, appName, newSize, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('changePaginationViewSize', {'paginatorName' : paginatorName, 'newSize' : newSize, 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}
opentaps.togglePaginationViewAll = function(paginatorName, appName, responseFunction) {
    if (! responseFunction) responseFunction = opentaps.processPaginationResponse;
    opentaps.sendRequest('changePaginationViewSize', {'paginatorName' : paginatorName, 'toggleViewAll' : 'y', 'applicationName' : appName}, function(data) {responseFunction(paginatorName, data)}, {target: 'paginate_' + paginatorName, containerClass: 'paginate_' + paginatorName, size: 28});
}


/* auto complete functions */

opentaps.initInputAutoCompleteQuick = function(/* String */ jsonItemStoreUrl, /* String */ elementName, /* String */ elementId, /* String */ defaultValue) {
    var store = new dojo.data.JsonItemStoreAutoComplete({url: jsonItemStoreUrl});
    var comboBox = new dijit.form.ComboBox({
        name: elementName,
        value: defaultValue,
        autoComplete: false,
        store: store,
        searchAttr: "name",
        searchDelay: 1500,
        size: 15,
        maxlength: 20,
        hasDownArrow: false
    }, dojo.byId(elementId));
    return comboBox;
}

/* Miscellaneous Functions */

// Multi form function to change the _rowSubmit_o_${index} to Y
opentaps.markRowForSubmit = function(form, index) {
    form["_rowSubmit_o_" + index].value='Y';
}

// Extends dojo ComboBox to publish the value in another hidden input
// so that the value can get submitted
dojo.extend(dijit.form.ComboBox, {
  hiddenId: null,
  setDisplayedValue:function(value){
      this.setValue(value);
      dojo.byId(this.hiddenId).value = value;
    }
  });

//Functions to make outgoing call via asterisk, use partyIdTo + contactMechIdTo as parameters
opentaps.makeOutgoingCall = function (/* String */ partyIdTo, /* String */ contactMechIdTo) {
    opentaps.sendRequest("makeOutgoingCall", {'internalPartyId' : partyIdTo, 'contactMechIdTo' : contactMechIdTo}, function(data) {});
}

//Functions to make outgoing call via asterisk, use primaryPhoneCountryCode + primaryPhoneAreaCode + primaryPhoneNumber as parameters
opentaps.makeOutgoingCall = function (/* String */ primaryPhoneCountryCode, /* String */ primaryPhoneAreaCode, /* String */ primaryPhoneNumber) {
    opentaps.sendRequest("makeOutgoingCall", {'primaryPhoneCountryCode' : primaryPhoneCountryCode, 'primaryPhoneAreaCode' : primaryPhoneAreaCode, 'primaryPhoneNumber' : primaryPhoneNumber}, function(data) {});
}

//Functions to check upload file if exist in the server
opentaps.checkUploadFile = function(form, orderId, fileName, confirmMessage) {
    var callbackFunc = function(){uploadFile()};
    if(fileName.indexOf("\\") > 0) {
    	fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
    }
    var requestData = {'orderId' : orderId, 'fileName' : fileName};
    opentaps.sendRequest('checkExistOrderContentJSON', requestData, function(data) {opentaps.checkUploadFileResponse(form, confirmMessage, data)});
}

//Functions show conflict dialog if exist same file in the server
opentaps.checkUploadFileResponse = function(form, confirmMessage, data) {
    var upload = true;
    if (data.existSameFile) {
		upload = confirm(confirmMessage);
    }
	if (upload) {
		form.submit();
	}
}


//Functions to check supplierProduct if existing in the server
opentaps.checkSupplierProduct = function(button, productId, partyId, currencyUomId, quantity, confirmMessage, disallowVirtual) {
    var requestData = {'productId' : productId, 'partyId' : partyId, 'currencyUomId' : currencyUomId, 'quantity' : quantity};
    opentaps.sendRequest('checkExistSupplierProductJSON', requestData, function(data) {opentaps.checkSupplierProductResponse(button, confirmMessage, disallowVirtual, data)});
}

//Functions show not exist supplierProduct dialog, if not exit then show the warn dialog
opentaps.checkSupplierProductResponse = function(button, confirmMessage, disallowVirtual, data) {
    var submitForm = true;
    if (data.isVirtual && disallowVirtual) {
        alert("Product is virtual and can not be added to the order.");
        return;
    }
    if (!data.existSupplierProduct) {
        submitForm = confirm(confirmMessage);
    }
    if (submitForm) {
        button.disabled = true;
        button.form.submit();
    }
}

opentaps.hideDiv = function(divId) {
    document.getElementById(divId).style.display="none";
}

opentaps.displayDiv = function(divId) {
    document.getElementById(divId).style.display="";
}
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

// This file has been modified by Open Source Strategies, Inc.

// Check Box Select/Toggle Functions for Select/Toggle All

function toggle(e) {
    e.checked = !e.checked;    
}

function checkToggleDefault(e) {
    checkToggle(e, "selectAllForm");
}
function checkToggle(e, formName) {
    var cform = document[formName];
    if (e.checked) {      
        var len = cform.elements.length;
        var allchecked = true;
        for (var i = 0; i < len; i++) {
            var element = cform.elements[i];
            if (element.name.substring(0, 10) == "_rowSubmit" && !element.checked) {       
                allchecked = false;
            }
            cform.selectAll.checked = allchecked;            
        }
    } else {
        cform.selectAll.checked = false;
    }
}

function toggleAllDefault(e) {
    toggleAll(e, "selectAllForm");
}
function toggleAll(e, formName) {
    var cform = document[formName];
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        if (element.name && element.name.substring(0, 10) == "_rowSubmit" && element.checked != e.checked) {
            toggle(element);
        }
    }
}

function selectAllDefault() {
    selectAll("selectAllForm");
}
function selectAll(formName) {
    var cform = document[formName];
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];                   
        if ((element.name == "selectAll" || element.name.substring(0, 10) == "_rowSubmit") && !element.checked) {
            toggle(element);
        } 
    }     
}

function removeSelectedDefault() {
    removeSelected("selectAllForm");
}
function removeSelected(formName) {
    var cform = document[formName];
    cform.removeSelected.value = true;
    cform.submit();
}

// highlight the selected row(s)

function highlightRow(e,rowId){
    var currentClassName = document.getElementById(rowId).className;
    if (e.checked) {
        if (currentClassName == '' ) {
            document.getElementById(rowId).className = 'selected';
        } else if (currentClassName == 'alternate-row') {
            document.getElementById(rowId).className = 'alternate-rowSelected';
        }
    } else {
        if (currentClassName == 'selected') {
            document.getElementById(rowId).className = '';
        } else if (currentClassName == 'alternate-rowSelected') {
            document.getElementById(rowId).className = 'alternate-row';
        }
    }
}

function highlightAllRows(e, halfRowId, formName){
    var cform = document[formName];
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        if (element.name.substring(0, 10) == "_rowSubmit") {
            highlightRow(e, halfRowId+element.name.substring(13));
        }
    }
}


// popup windows functions

function popUp(url, name, height, width) {
    popupWindow = window.open(url, name, 'location=no,scrollbars,width=' + width + ',height=' + height);
}
function popUpSmall(url, name) {
    popUp(url, name, '300', '450');
}
function popUpPrint(serverRoot, screen1) {
    popUpPrint(serverRoot, screen1, null, null);
}
function popUpPrint(serverRoot, screen1, screen2) {
    popUpPrint(serverRoot, screen1, screen2, null);
}
function popUpPrint(serverRoot, screen1, screen2, screen3) {
    if  (serverRoot == null) {
        serverRoot = "";
    }

    var url = serverRoot + "/webtools/control/print";

    if (screen1 != null) {
        screen1 = screen1.replace(/\:/g, "%3A");
        screen1 = screen1.replace(/\//g, "%2F");
        screen1 = screen1.replace(/\#/g, "%23");
        screen1 = screen1.replace(/\?/g, "%3F");
        screen1 = screen1.replace(/\=/g, "%3D");
        url = url + "?screen=" + screen1;

        if (screen2 != null) {
            screen2 = screen2.replace(/\:/g, "%3A");
            screen2 = screen2.replace(/\//g, "%2F");
            screen2 = screen2.replace(/\#/g, "%23");
            screen2 = screen2.replace(/\?/g, "%3F");
            screen2 = screen2.replace(/\=/g, "%3D");
            url = url + "&screen=" + screen2;

            if (screen3 != null) {
                screen3 = screen3.replace(/\:/g, "%3A");
                screen3 = screen3.replace(/\//g, "%2F");
                screen3 = screen3.replace(/\#/g, "%23");
                screen3 = screen3.replace(/\?/g, "%3F");
                screen3 = screen3.replace(/\=/g, "%3D");
                url = url + "&screen=" + screen3;
            }
        }
    }

    popupWindow = window.open(url, name, 'location=no,statusbar=1,menubar=0,scrollbars,width=375,height=75,top=0,left=0');    
}

// hidden div functions

function getStyleObject(objectId) {
    if (document.getElementById && document.getElementById(objectId)) {
        return document.getElementById(objectId).style;
    } else if (document.all && document.all(objectId)) {
        return document.all(objectId).style;
    } else if (document.layers && document.layers[objectId]) {
        return document.layers[objectId];
    } else {
        return false;
    }
}
function changeObjectVisibility(objectId, newVisibility) {
    var styleObject = getStyleObject(objectId);
    if (styleObject) {
        styleObject.visibility = newVisibility;
        return true;
    } else {
        return false;
    }
}

// To use this in a link use a URL like this: javascript:confirmActionLink('You want to delete this party?', 'deleteParty?partyId=${partyId}')
function confirmActionLink(msg, newLocation) {
    if (msg == null) {
        msg = "Are you sure you want to do this?";
    }
    var agree = confirm(msg);
    if (agree) {
        if (newLocation != null) location.replace(newLocation);
        return true;
    } else {
        return false;
    }
}

// To use this in a link use a URL like this: javascript:confirmActionFormLink('You want to update this party?', 'updateParty')
function confirmActionFormLink(msg, formName) {
    if (msg == null) {
        msg = "Are you sure you want to do this?";
    }
    var agree = confirm(msg);
    if (agree) {
        if (formName != null) document.forms[formName].submit();
        return true;
    } else {
        return false;
    }
}

// ===== Ajax Functions - based on protoype.js ===== //

/** Update an area (HTML container element).
  * @param areaId The id of the HTML container to update
  * @param target The URL to call to update the HTML container
  * @param targetParams The URL parameters
*/
function ajaxUpdateArea(areaId, target, targetParams) {
    new Ajax.Updater(areaId, target, {parameters: targetParams});
}

/** Update multiple areas (HTML container elements).
  * @param areaCsvString The area CSV string. The CSV string is a flat array in the
  * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxUpdateAreas(areaCsvString) {
    responseFunction = function(transport) {
        // Uncomment the next two lines to see the HTTP responses
        //var response = transport.responseText || "no response text";
        //alert("Response: \n\n" + response);
    }
    var areaArray = areaCsvString.split(",");
    var numAreas = parseInt(areaArray.length / 3);
    for (var i = 0; i < numAreas * 3; i = i + 3) {
        new Ajax.Updater(areaArray[i], areaArray[i + 1], {parameters: areaArray[i + 2], onComplete: responseFunction,evalScripts: true });
    }
}

/** Update an area (HTML container element) periodically.
  * @param areaId The id of the HTML container to update
  * @param target The URL to call to update the HTML container
  * @param targetParams The URL parameters
  * @param interval The update interval, in seconds.
*/
function ajaxUpdateAreaPeriodic(areaId, target, targetParams, interval) {
    new Ajax.PeriodicalUpdater(areaId, target, {parameters: targetParams, frequency: interval});
}

/** Submit request, update multiple areas (HTML container elements).
  * @param target The URL to call to update the HTML container
  * @param targetParams The URL parameters
  * @param areaCsvString The area CSV string. The CSV string is a flat array in the
  * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxSubmitRequestUpdateAreas(target, targetParams, areaCsvString) {
    updateFunction = function(transport) {
        ajaxUpdateAreas(areaCsvString);
    }
    new Ajax.Request(target, {
        parameters: targetParams,
        onComplete: updateFunction });
}

/** Submit form, update an area (HTML container element).
  * @param form The form element
  * @param areaId The id of the HTML container to update
  * @param submitUrl The URL to call to update the HTML container
*/
function submitFormInBackground(form, areaId, submitUrl) {
    submitFormDisableSubmits(form);
    updateFunction = function() {
        new Ajax.Updater(areaId, submitUrl);
    }
    new Ajax.Request(form.action, {
        parameters: form.serialize(true),
        onComplete: updateFunction });
}

/** Submit form, update multiple areas (HTML container elements).
  * @param form The form element
  * @param areaCsvString The area CSV string. The CSV string is a flat array in the
  * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxSubmitFormUpdateAreas(form, areaCsvString) {
    submitFormDisableSubmits($(form));
    updateFunction = function(transport) {
        var data = transport.responseText.evalJSON(true);
        if (data._ERROR_MESSAGE_LIST_ != undefined || data._ERROR_MESSAGE_ != undefined) {
            if(!$('content-messages')) {
               //add this div just after app-navigation
               if($('app-navigation')){
                   $('app-navigation' ).insert({after: '<div id="content-messages"></div>'});
               }
            }
           $('content-messages').addClassName('errorMessage');
           $('content-messages' ).update(data._ERROR_MESSAGE_LIST_ + " " + data._ERROR_MESSAGE_);
           new Effect.Appear('content-messages',{duration: 0.5});
        }else {
        	if($('content-messages')) {
                $('content-messages').removeClassName('errorMessage');
                new Effect.Fade('content-messages',{duration: 0.0});
            }
            ajaxUpdateAreas(areaCsvString);
        }
    }
    new Ajax.Request($(form).action, {
        parameters: $(form).serialize(true),
        onComplete: updateFunction });
}

/** Enable auto-completion for text elements.
  * @param areaCsvString The area CSV string. The CSV string is a flat array in the
  * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxAutoCompleter(areaCsvString) {
    var areaArray = areaCsvString.split(",");
    var numAreas = parseInt(areaArray.length / 3);
    for (var i = 0; i < numAreas * 3; i = i + 3) {
	    var optionsDivId = areaArray[i] + "_autoCompleterOptions";
	    $(areaArray[i]).insert({after: '<div class="autocomplete"' + 'id=' + optionsDivId + '></div>'});
        new Ajax.Autocompleter($(areaArray[i]), optionsDivId, areaArray[i + 1], {parameters: areaArray[i + 2]});
    }
}

/** Enable auto-completion for drop-down elements.
  * @param descriptionElement The id of the text field
  * @param hiddenElement The id of the drop-down.  Used as the id of hidden field inserted.
  * @param data Choices for Autocompleter.Local, form of: {key: 'description',.......}
  * @param options
*/

function ajaxAutoCompleteDropDown(descriptionElement, hiddenElement, data, options) {
    var update = hiddenElement + "_autoCompleterOptions";
    $(descriptionElement).insert({after: '<div class="autocomplete"' + 'id=' + update + '></div>'});
    new Autocompleter.Local($(descriptionElement), update, $H(data), {autoSelect: options.autoSelect, frequency: options.frequency, minChars: options.minChars, choices: options.choices, partialSearch: options.partialSearch, partialChars: options.partialChars, ignoreCase: options.ignoreCase, fullSearch: options.fullSearch, afterUpdateElement: setKeyAsParameter});

    function setKeyAsParameter(text, li) {
        $(hiddenElement).value = li.id;
    }
}

/** Toggle area visibility on/off.
  * @param link The <a> element calling this function
  * @param areaId The id of the HTML container to toggle
  * @param expandTxt Localized 'Expand' text
  * @param collapseTxt Localized 'Collapse' text
*/
function toggleCollapsiblePanel(link, areaId, expandTxt, collapseTxt){
    var container = $(areaId);
    var liElement = $(link).up('li');
    if(container.visible()){
        liElement.removeClassName('expanded');
        liElement.addClassName('collapsed');
        link.title = expandTxt;
    } else {
        liElement.removeClassName('collapsed');
        liElement.addClassName('expanded');
        link.title = collapseTxt;
    }
    Effect.toggle(container, 'appear');
}

/** Toggle screenlet visibility on/off.
  * @param link The <a> element calling this function
  * @param areaId The id of the HTML container to toggle
  * @param expandTxt Localized 'Expand' text
  * @param collapseTxt Localized 'Collapse' text
*/
function toggleScreenlet(link, areaId, expandTxt, collapseTxt){
    toggleCollapsiblePanel(link, areaId, expandTxt, collapseTxt);
    var container = $(areaId);
    var screenlet = container.up('div');
    if(container.visible()){
        var currentParam = screenlet.id + "_collapsed=false";
        var newParam = screenlet.id + "_collapsed=true";
    } else {
        var currentParam = screenlet.id + "_collapsed=true";
        var newParam = screenlet.id + "_collapsed=false";
    }
    var paginationMenus = $$('div.nav-pager');
    paginationMenus.each(function(menu) {
        if (menu) {
            var childElements = menu.getElementsByTagName('a');
            for (var i = 0; i < childElements.length; i++) {
                if (childElements[i].href.indexOf("http") == 0) {
                    childElements[i].href = replaceQueryParam(childElements[i].href, currentParam, newParam);
                }
            }
            childElements = menu.getElementsByTagName('select');
            for (i = 0; i < childElements.length; i++) {
                if (childElements[i].href.indexOf("location.href") >= 0) {
                    Element.extend(childElements[i]);
                    childElements[i].writeAttribute("onchange", replaceQueryParam(childElements[i].readAttribute("onchange"), currentParam, newParam));
                }
            }
        }
    });
}

/** In Place Editor for display elements
  * @param element The id of the display field
  * @param url The request to be called to update the display field
  * @param options Options to be passed to Ajax.InPlaceEditor
*/

function ajaxInPlaceEditDisplayField(element, url, options) {
    new Ajax.InPlaceEditor($(element), url, options);
}
// ===== End of Ajax Functions ===== //

function replaceQueryParam(queryString, currentParam, newParam) {
    var result = queryString.replace(currentParam, newParam);
    if (result.indexOf(newParam) < 0) {
        if (result.indexOf("?") < 0) {
            result = result + "?" + newParam;
        } else if (result.endsWith("#")) {
            result = result.replace("#", "&" + newParam + "#");
        } else if (result.endsWith(";")) {
            result = result.replace(";", " + '&" + newParam + "';");
        } else {
            result = result + "&" + newParam;
        }
    }
    return result;
}

function submitFormDisableSubmits(form) {
    for (var i=0;i<form.length;i++) {
        var formel = form.elements[i];
        if (formel.type == "submit") {
            submitFormDisableButton(formel);
            var formName = form.name;
            var formelName = formel.name;
            var timeoutString = "submitFormEnableButtonByName('" + formName + "', '" + formelName + "')";
            var t = setTimeout(timeoutString, 1500);
        }
    }
}

// prevents doubleposts for <submit> inputs of type "button" or "image"
function submitFormDisableButton(button) {
    if (button.form.action != null && button.form.action.length > 0) {
        button.disabled = true;
    }
    button.className = button.className + " disabled";
}

function submitFormEnableButtonByName(formName, buttonName) {
    // alert("formName=" + formName + " buttonName=" + buttonName);
    var form = document[formName];
    var button = form.elements[buttonName];
    submitFormEnableButton(button);
}

function submitFormEnableButton(button) {
    button.disabled = false;
    button.className = button.className.substring(0, button.className.length - " disabled".length);
}

function expandAll(expanded) {
  var divs,divs1,i,j,links,groupbody;

  divs=document.getElementsByTagName('div');
  for(i=0;i<divs.length;i++) {
    if(/fieldgroup$/.test(divs[i].className)) {
      links=divs[i].getElementsByTagName('a');
      if(links.length>0) {
        divs1=divs[i].getElementsByTagName('div');
        for(j=0;j<divs1.length;j++){
          if(/fieldgroup-body/.test(divs1[j].className)) {
            groupbody=divs1[j];
          }
        }
        if(groupbody.visible() != expanded) {
          toggleCollapsiblePanel(links[0], groupbody.id, 'expand', 'collapse');
        }
      }
    }
  }
}

// redirect location to new url and prevents double posts for link
function redirectUrlAndDisableLink(url, link, afterClickText) {
    var subMenuBar = link.parentNode;
    // remove the link
    opentaps.removeNode(link);
    // add a fake link with new label
    var newLink = opentaps.createAnchor(null, '#', afterClickText, 'subMenuButton disabled', null);
    newLink.disabled = true;
    newLink.style.cursor = 'wait';
    for (var i = 0; i < subMenuBar.childNodes.length; i++) {
      var button = subMenuBar.childNodes[i];
      // remove the href attribute
      button.removeAttribute("href");
      // remove the onclick attribute
      button.removeAttribute("onclick");
      // iterator all link, and set it disabled
      button.disabled = true;
      button.className = button.className + " disabled";
      // change cursor type
      button.style.cursor = 'wait';
    }
    subMenuBar.appendChild(newLink);
    // redirect self location to url
    window.location.href = url;
    return false;
}
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
  var url = view_name;
  if (this.id != null && this.id != "undefined") {
     url += sep + 'id=' + this.id;
  } else {
     url += sep + 'id=';
  }
  url += argString;
  var obj_lookupwindow = window.open(url,'FieldLookup', 'width='+viewWidth+',height='+viewHeight+',scrollbars=yes,status=no,resizable=yes,top='+getY(viewHeight)+',left='+getX(viewWidth)+',dependent=yes,alwaysRaised=yes');
  obj_lookupwindow.opener = window;
  obj_lookupwindow.focus();
}
function lookup_error (str_message) {
  alert (str_message);
  return null;
}
// common file for all form widget javascript functions

// prevents doubleposts for <submit> widgets of type "button" or "image"
function submitFormWithSingleClick(button) {
    // only disable when the form action is defined, otherwise forms that 
    // return to the same page by not specifying an action will continue to
    // heve the button disabled in IE and maybe other browsers
    if (button.form.action != null && button.form.action.length > 0) {
        button.disabled = true;
        if (button.className == 'smallSubmit')
          button.className = 'smallSubmitDisabled';
    }
    button.form.submit();
}
function submitFormAndReplaceButtonTextWithSingleClick(button, newText) {
    // only disable when the form action is defined, otherwise forms that 
    // return to the same page by not specifying an action will continue to
    // heve the button disabled in IE and maybe other browsers
    if (button.form.action != null && button.form.action.length > 0) {
        button.disabled = true;
        if (button.className == 'smallSubmit')
          button.className = 'smallSubmitDisabled';
    }
    button.value = newText;
    button.form.submit();
}

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

function GenericWindow() {
	
	this.wnd = null;
		
    this.width = 700;
    this.height = 500;
	
    this.getX = function() {
	    return screen.width / 2 - (this.width / 2);
	}
	
	this.getY = function() {
		return screen.height / 2 - (this.height / 2);
	}
};

function InternalMessageWindow() {
	this.open = function(url) {
		this.prototype.wnd = window.open(url, 'InternalMessage', 'width='+this.prototype.width+',height='+this.prototype.height+',scrollbars=yes,status=no,resizable=yes,top='+this.prototype.getY()+',left='+this.prototype.getX()+',dependent=yes,alwaysRaised=yes');
		this.prototype.wnd.helper = this;
	}
	this.close = function() {
		this.prototype.wnd.close();
	}
	this.submit = function(form) {
		this.prototype.wnd.close();
		form.submit();
	}
	this.deleteMessage = function(id) {
		this.prototype.wnd.close();
		if (!id) return;
		var requestData = {"communicationEventId" : id};
		dojo.xhrGet({url: 'deleteInternalMessage', content: requestData});
	}
	this.replyMessage = function(id) {
		this.prototype.wnd.close();
		if (!id) return;
		var win = new InternalMessageWindow();
		win.prototype = new GenericWindow();
		win.open('replyInternalMessage?communicationEventId=' + id);
	}
	this.forwardMessage = function(id) {
		this.prototype.wnd.close();
		if (!id) return;
		var win = new InternalMessageWindow();
		win.prototype = new GenericWindow();
		win.open('forwardInternalMessage?communicationEventId=' + id);
	}
	this.onClose = function(id) {
		if (!id) return;
	    var requestData = {'communicationEventId' : id, 'statusId' : 'COM_COMPLETE'};
		dojo.xhrGet({url: 'markMessageRead', content: requestData});
	}
}

function viewMessage(url) {
	var messageWindow = new InternalMessageWindow();
	messageWindow.prototype = new GenericWindow();
	messageWindow.open(url);
}

function sendMessage() {
	var messageWindow = new InternalMessageWindow();
	messageWindow.prototype = new GenericWindow();
	messageWindow.open('composeInternalMessage');
}
/**
 * http://www.openjs.com/scripts/events/keyboard_shortcuts/
 * Version : 2.01.B
 * By Binny V A
 * License : BSD
 */
shortcut = {
	'all_shortcuts':{},//All the shortcuts are stored in this array
	'add': function(shortcut_combination,callback,opt) {
		//Provide a set of default options
		var default_options = {
			'type':'keydown',
			'propagate':false,
			'disable_in_input':false,
			'target':document,
			'keycode':false
		}
		if(!opt) opt = default_options;
		else {
			for(var dfo in default_options) {
				if(typeof opt[dfo] == 'undefined') opt[dfo] = default_options[dfo];
			}
		}

		var ele = opt.target;
		if(typeof opt.target == 'string') ele = document.getElementById(opt.target);
		var ths = this;
		shortcut_combination = shortcut_combination.toLowerCase();

		//The function to be called at keypress
		var func = function(e) {
			e = e || window.event;
			
			if(opt['disable_in_input']) { //Don't enable shortcut keys in Input, Textarea fields
				var element;
				if(e.target) element=e.target;
				else if(e.srcElement) element=e.srcElement;
				if(element.nodeType==3) element=element.parentNode;

				if(element.tagName == 'INPUT' || element.tagName == 'TEXTAREA') return;
			}
	
			//Find Which key is pressed
			if (e.keyCode) code = e.keyCode;
			else if (e.which) code = e.which;
			var character = String.fromCharCode(code).toLowerCase();
			
			if(code == 188) character=","; //If the user presses , when the type is onkeydown
			if(code == 190) character="."; //If the user presses , when the type is onkeydown

			var keys = shortcut_combination.split("+");
			//Key Pressed - counts the number of valid keypresses - if it is same as the number of keys, the shortcut function is invoked
			var kp = 0;
			
			//Work around for stupid Shift key bug created by using lowercase - as a result the shift+num combination was broken
			var shift_nums = {
				"`":"~",
				"1":"!",
				"2":"@",
				"3":"#",
				"4":"$",
				"5":"%",
				"6":"^",
				"7":"&",
				"8":"*",
				"9":"(",
				"0":")",
				"-":"_",
				"=":"+",
				";":":",
				"'":"\"",
				",":"<",
				".":">",
				"/":"?",
				"\\":"|"
			}
			//Special Keys - and their codes
			var special_keys = {
				'esc':27,
				'escape':27,
				'tab':9,
				'space':32,
				'return':13,
				'enter':13,
				'backspace':8,
	
				'scrolllock':145,
				'scroll_lock':145,
				'scroll':145,
				'capslock':20,
				'caps_lock':20,
				'caps':20,
				'numlock':144,
				'num_lock':144,
				'num':144,
				
				'pause':19,
				'break':19,
				
				'insert':45,
				'home':36,
				'delete':46,
				'end':35,
				
				'pageup':33,
				'page_up':33,
				'pu':33,
	
				'pagedown':34,
				'page_down':34,
				'pd':34,
	
				'left':37,
				'up':38,
				'right':39,
				'down':40,
	
				'f1':112,
				'f2':113,
				'f3':114,
				'f4':115,
				'f5':116,
				'f6':117,
				'f7':118,
				'f8':119,
				'f9':120,
				'f10':121,
				'f11':122,
				'f12':123
			}
	
			var modifiers = { 
				shift: { wanted:false, pressed:false},
				ctrl : { wanted:false, pressed:false},
				alt  : { wanted:false, pressed:false},
				meta : { wanted:false, pressed:false}	//Meta is Mac specific
			};
                        
			if(e.ctrlKey)	modifiers.ctrl.pressed = true;
			if(e.shiftKey)	modifiers.shift.pressed = true;
			if(e.altKey)	modifiers.alt.pressed = true;
			if(e.metaKey)   modifiers.meta.pressed = true;
                        
			for(var i=0; k=keys[i],i<keys.length; i++) {
				//Modifiers
				if(k == 'ctrl' || k == 'control') {
					kp++;
					modifiers.ctrl.wanted = true;

				} else if(k == 'shift') {
					kp++;
					modifiers.shift.wanted = true;

				} else if(k == 'alt') {
					kp++;
					modifiers.alt.wanted = true;
				} else if(k == 'meta') {
					kp++;
					modifiers.meta.wanted = true;
				} else if(k.length > 1) { //If it is a special key
					if(special_keys[k] == code) kp++;
					
				} else if(opt['keycode']) {
					if(opt['keycode'] == code) kp++;

				} else { //The special keys did not match
					if(character == k) kp++;
					else {
						if(shift_nums[character] && e.shiftKey) { //Stupid Shift key bug created by using lowercase
							character = shift_nums[character]; 
							if(character == k) kp++;
						}
					}
				}
			}
			
			if(kp == keys.length && 
						modifiers.ctrl.pressed == modifiers.ctrl.wanted &&
						modifiers.shift.pressed == modifiers.shift.wanted &&
						modifiers.alt.pressed == modifiers.alt.wanted &&
						modifiers.meta.pressed == modifiers.meta.wanted) {
				callback(e);
	
				if(!opt['propagate']) { //Stop the event
					//e.cancelBubble is supported by IE - this will kill the bubbling process.
					e.cancelBubble = true;
					e.returnValue = false;
	
					//e.stopPropagation works in Firefox.
					if (e.stopPropagation) {
						e.stopPropagation();
						e.preventDefault();
					}
					return false;
				}
			}
		}
		this.all_shortcuts[shortcut_combination] = {
			'callback':func, 
			'target':ele, 
			'event': opt['type']
		};
		//Attach the function with the event
		if(ele.addEventListener) ele.addEventListener(opt['type'], func, false);
		else if(ele.attachEvent) ele.attachEvent('on'+opt['type'], func);
		else ele['on'+opt['type']] = func;
	},

	//Remove the shortcut - just specify the shortcut and I will remove the binding
	'remove':function(shortcut_combination) {
		shortcut_combination = shortcut_combination.toLowerCase();
		var binding = this.all_shortcuts[shortcut_combination];
		delete(this.all_shortcuts[shortcut_combination])
		if(!binding) return;
		var type = binding['event'];
		var ele = binding['target'];
		var callback = binding['callback'];

		if(ele.detachEvent) ele.detachEvent('on'+type, callback);
		else if(ele.removeEventListener) ele.removeEventListener(type, callback, false);
		else ele['on'+type] = false;
	}
}


opentaps.addUrlShortcut = function(shortcutString, targetUrl) {
  shortcut.add(shortcutString, function() {
      window.location = targetUrl;
    }, {'disable_in_input':true});
}

opentaps.addFocusShortcut = function(shortcutString, targetElementId) {
  shortcut.add(shortcutString, function() {
      var el = document.getElementById(targetElementId);
      if (el) {
        el.focus();
      }
    }, {'disable_in_input':false});
}/*  Copyright Mihai Bazon, 2002-2005  |  www.bazon.net/mishoo
 * -----------------------------------------------------------
 *
 * The DHTML Calendar, version 1.0 "It is happening again"
 *
 * Details and latest version at:
 * www.dynarch.com/projects/calendar
 *
 * This script is developed by Dynarch.com.  Visit us at www.dynarch.com.
 *
 * This script is distributed under the GNU Lesser General Public License.
 * Read the entire license text here: http://www.gnu.org/licenses/lgpl.html
 */

// $Id: calendar.js,v 1.51 2005/03/07 16:44:31 mishoo Exp $

/** The Calendar object constructor. */
Calendar = function (firstDayOfWeek, dateStr, onSelected, onClose) {
	// member variables
	this.activeDiv = null;
	this.currentDateEl = null;
	this.getDateStatus = null;
	this.getDateToolTip = null;
	this.getDateText = null;
	this.timeout = null;
	this.onSelected = onSelected || null;
	this.onClose = onClose || null;
	this.dragging = false;
	this.hidden = false;
	this.minYear = 1970;
	this.maxYear = 2050;
	this.dateFormat = Calendar._TT["DEF_DATE_FORMAT"];
	this.ttDateFormat = Calendar._TT["TT_DATE_FORMAT"];
	this.isPopup = true;
	this.weekNumbers = true;
	this.firstDayOfWeek = typeof firstDayOfWeek == "number" ? firstDayOfWeek : Calendar._FD; // 0 for Sunday, 1 for Monday, etc.
	this.showsOtherMonths = false;
	this.dateStr = dateStr;
	this.ar_days = null;
	this.showsTime = false;
	this.time24 = true;
	this.yearStep = 2;
	this.hiliteToday = true;
	this.multiple = null;
	// HTML elements
	this.table = null;
	this.element = null;
	this.tbody = null;
	this.firstdayname = null;
	// Combo boxes
	this.monthsCombo = null;
	this.yearsCombo = null;
	this.hilitedMonth = null;
	this.activeMonth = null;
	this.hilitedYear = null;
	this.activeYear = null;
	// Information
	this.dateClicked = false;

	// one-time initializations
	if (typeof Calendar._SDN == "undefined") {
		// table of short day names
		if (typeof Calendar._SDN_len == "undefined")
			Calendar._SDN_len = 3;
		var ar = new Array();
		for (var i = 8; i > 0;) {
			ar[--i] = Calendar._DN[i].substr(0, Calendar._SDN_len);
		}
		Calendar._SDN = ar;
		// table of short month names
		if (typeof Calendar._SMN_len == "undefined")
			Calendar._SMN_len = 3;
		ar = new Array();
		for (var i = 12; i > 0;) {
			ar[--i] = Calendar._MN[i].substr(0, Calendar._SMN_len);
		}
		Calendar._SMN = ar;
	}
};

// ** constants

/// "static", needed for event handlers.
Calendar._C = null;

/// detect a special case of "web browser"
Calendar.is_ie = ( /msie/i.test(navigator.userAgent) &&
		   !/opera/i.test(navigator.userAgent) );

Calendar.is_ie5 = ( Calendar.is_ie && /msie 5\.0/i.test(navigator.userAgent) );

/// detect Opera browser
Calendar.is_opera = /opera/i.test(navigator.userAgent);

/// detect KHTML-based browsers
Calendar.is_khtml = /Konqueror|Safari|KHTML/i.test(navigator.userAgent);

// BEGIN: UTILITY FUNCTIONS; beware that these might be moved into a separate
//        library, at some point.

Calendar.getAbsolutePos = function(el) {
	var SL = 0, ST = 0;
	var is_div = /^div$/i.test(el.tagName);
	if (is_div && el.scrollLeft)
		SL = el.scrollLeft;
	if (is_div && el.scrollTop)
		ST = el.scrollTop;
	var r = { x: el.offsetLeft - SL, y: el.offsetTop - ST };
	if (el.offsetParent) {
		var tmp = this.getAbsolutePos(el.offsetParent);
		r.x += tmp.x;
		r.y += tmp.y;
	}
	return r;
};

Calendar.isRelated = function (el, evt) {
	var related = evt.relatedTarget;
	if (!related) {
		var type = evt.type;
		if (type == "mouseover") {
			related = evt.fromElement;
		} else if (type == "mouseout") {
			related = evt.toElement;
		}
	}
	while (related) {
		if (related == el) {
			return true;
		}
		related = related.parentNode;
	}
	return false;
};

Calendar.removeClass = function(el, className) {
	if (!(el && el.className)) {
		return;
	}
	var cls = el.className.split(" ");
	var ar = new Array();
	for (var i = cls.length; i > 0;) {
		if (cls[--i] != className) {
			ar[ar.length] = cls[i];
		}
	}
	el.className = ar.join(" ");
};

Calendar.addClass = function(el, className) {
	Calendar.removeClass(el, className);
	el.className += " " + className;
};

// FIXME: the following 2 functions totally suck, are useless and should be replaced immediately.
Calendar.getElement = function(ev) {
	var f = Calendar.is_ie ? window.event.srcElement : ev.currentTarget;
	while (f.nodeType != 1 || /^div$/i.test(f.tagName))
		f = f.parentNode;
	return f;
};

Calendar.getTargetElement = function(ev) {
	var f = Calendar.is_ie ? window.event.srcElement : ev.target;
	while (f.nodeType != 1)
		f = f.parentNode;
	return f;
};

Calendar.stopEvent = function(ev) {
	ev || (ev = window.event);
	if (Calendar.is_ie) {
		ev.cancelBubble = true;
		ev.returnValue = false;
	} else {
		ev.preventDefault();
		ev.stopPropagation();
	}
	return false;
};

Calendar.addEvent = function(el, evname, func) {
	if (el.attachEvent) { // IE
		el.attachEvent("on" + evname, func);
	} else if (el.addEventListener) { // Gecko / W3C
		el.addEventListener(evname, func, true);
	} else {
		el["on" + evname] = func;
	}
};

Calendar.removeEvent = function(el, evname, func) {
	if (el.detachEvent) { // IE
		el.detachEvent("on" + evname, func);
	} else if (el.removeEventListener) { // Gecko / W3C
		el.removeEventListener(evname, func, true);
	} else {
		el["on" + evname] = null;
	}
};

Calendar.createElement = function(type, parent) {
	var el = null;
	if (document.createElementNS) {
		// use the XHTML namespace; IE won't normally get here unless
		// _they_ "fix" the DOM2 implementation.
		el = document.createElementNS("http://www.w3.org/1999/xhtml", type);
	} else {
		el = document.createElement(type);
	}
	if (typeof parent != "undefined") {
		parent.appendChild(el);
	}
	return el;
};

// END: UTILITY FUNCTIONS

// BEGIN: CALENDAR STATIC FUNCTIONS

/** Internal -- adds a set of events to make some element behave like a button. */
Calendar._add_evs = function(el) {
	with (Calendar) {
		addEvent(el, "mouseover", dayMouseOver);
		addEvent(el, "mousedown", dayMouseDown);
		addEvent(el, "mouseout", dayMouseOut);
		if (is_ie) {
			addEvent(el, "dblclick", dayMouseDblClick);
			el.setAttribute("unselectable", true);
		}
	}
};

Calendar.findMonth = function(el) {
	if (typeof el.month != "undefined") {
		return el;
	} else if (typeof el.parentNode.month != "undefined") {
		return el.parentNode;
	}
	return null;
};

Calendar.findYear = function(el) {
	if (typeof el.year != "undefined") {
		return el;
	} else if (typeof el.parentNode.year != "undefined") {
		return el.parentNode;
	}
	return null;
};

Calendar.showMonthsCombo = function () {
	var cal = Calendar._C;
	if (!cal) {
		return false;
	}
	var cal = cal;
	var cd = cal.activeDiv;
	var mc = cal.monthsCombo;
	if (cal.hilitedMonth) {
		Calendar.removeClass(cal.hilitedMonth, "hilite");
	}
	if (cal.activeMonth) {
		Calendar.removeClass(cal.activeMonth, "active");
	}
	var mon = cal.monthsCombo.getElementsByTagName("div")[cal.date.getMonth()];
	Calendar.addClass(mon, "active");
	cal.activeMonth = mon;
	var s = mc.style;
	s.display = "block";
	if (cd.navtype < 0)
		s.left = cd.offsetLeft + "px";
	else {
		var mcw = mc.offsetWidth;
		if (typeof mcw == "undefined")
			// Konqueror brain-dead techniques
			mcw = 50;
		s.left = (cd.offsetLeft + cd.offsetWidth - mcw) + "px";
	}
	s.top = (cd.offsetTop + cd.offsetHeight) + "px";
};

Calendar.showYearsCombo = function (fwd) {
	var cal = Calendar._C;
	if (!cal) {
		return false;
	}
	var cal = cal;
	var cd = cal.activeDiv;
	var yc = cal.yearsCombo;
	if (cal.hilitedYear) {
		Calendar.removeClass(cal.hilitedYear, "hilite");
	}
	if (cal.activeYear) {
		Calendar.removeClass(cal.activeYear, "active");
	}
	cal.activeYear = null;
	var Y = cal.date.getFullYear() + (fwd ? 1 : -1);
	var yr = yc.firstChild;
	var show = false;
	for (var i = 12; i > 0; --i) {
		if (Y >= cal.minYear && Y <= cal.maxYear) {
			yr.innerHTML = Y;
			yr.year = Y;
			yr.style.display = "block";
			show = true;
		} else {
			yr.style.display = "none";
		}
		yr = yr.nextSibling;
		Y += fwd ? cal.yearStep : -cal.yearStep;
	}
	if (show) {
		var s = yc.style;
		s.display = "block";
		if (cd.navtype < 0)
			s.left = cd.offsetLeft + "px";
		else {
			var ycw = yc.offsetWidth;
			if (typeof ycw == "undefined")
				// Konqueror brain-dead techniques
				ycw = 50;
			s.left = (cd.offsetLeft + cd.offsetWidth - ycw) + "px";
		}
		s.top = (cd.offsetTop + cd.offsetHeight) + "px";
	}
};

// event handlers

Calendar.tableMouseUp = function(ev) {
	var cal = Calendar._C;
	if (!cal) {
		return false;
	}
	if (cal.timeout) {
		clearTimeout(cal.timeout);
	}
	var el = cal.activeDiv;
	if (!el) {
		return false;
	}
	var target = Calendar.getTargetElement(ev);
	ev || (ev = window.event);
	Calendar.removeClass(el, "active");
	if (target == el || target.parentNode == el) {
		Calendar.cellClick(el, ev);
	}
	var mon = Calendar.findMonth(target);
	var date = null;
	if (mon) {
		date = new Date(cal.date);
		if (mon.month != date.getMonth()) {
			date.setMonth(mon.month);
			cal.setDate(date);
			cal.dateClicked = false;
			cal.callHandler();
		}
	} else {
		var year = Calendar.findYear(target);
		if (year) {
			date = new Date(cal.date);
			if (year.year != date.getFullYear()) {
				date.setFullYear(year.year);
				cal.setDate(date);
				cal.dateClicked = false;
				cal.callHandler();
			}
		}
	}
	with (Calendar) {
		removeEvent(document, "mouseup", tableMouseUp);
		removeEvent(document, "mouseover", tableMouseOver);
		removeEvent(document, "mousemove", tableMouseOver);
		cal._hideCombos();
		_C = null;
		return stopEvent(ev);
	}
};

Calendar.tableMouseOver = function (ev) {
	var cal = Calendar._C;
	if (!cal) {
		return;
	}
	var el = cal.activeDiv;
	var target = Calendar.getTargetElement(ev);
	if (target == el || target.parentNode == el) {
		Calendar.addClass(el, "hilite active");
		Calendar.addClass(el.parentNode, "rowhilite");
	} else {
		if (typeof el.navtype == "undefined" || (el.navtype != 50 && (el.navtype == 0 || Math.abs(el.navtype) > 2)))
			Calendar.removeClass(el, "active");
		Calendar.removeClass(el, "hilite");
		Calendar.removeClass(el.parentNode, "rowhilite");
	}
	ev || (ev = window.event);
	if (el.navtype == 50 && target != el) {
		var pos = Calendar.getAbsolutePos(el);
		var w = el.offsetWidth;
		var x = ev.clientX;
		var dx;
		var decrease = true;
		if (x > pos.x + w) {
			dx = x - pos.x - w;
			decrease = false;
		} else
			dx = pos.x - x;

		if (dx < 0) dx = 0;
		var range = el._range;
		var current = el._current;
		var count = Math.floor(dx / 10) % range.length;
		for (var i = range.length; --i >= 0;)
			if (range[i] == current)
				break;
		while (count-- > 0)
			if (decrease) {
				if (--i < 0)
					i = range.length - 1;
			} else if ( ++i >= range.length )
				i = 0;
		var newval = range[i];
		el.innerHTML = newval;

		cal.onUpdateTime();
	}
	var mon = Calendar.findMonth(target);
	if (mon) {
		if (mon.month != cal.date.getMonth()) {
			if (cal.hilitedMonth) {
				Calendar.removeClass(cal.hilitedMonth, "hilite");
			}
			Calendar.addClass(mon, "hilite");
			cal.hilitedMonth = mon;
		} else if (cal.hilitedMonth) {
			Calendar.removeClass(cal.hilitedMonth, "hilite");
		}
	} else {
		if (cal.hilitedMonth) {
			Calendar.removeClass(cal.hilitedMonth, "hilite");
		}
		var year = Calendar.findYear(target);
		if (year) {
			if (year.year != cal.date.getFullYear()) {
				if (cal.hilitedYear) {
					Calendar.removeClass(cal.hilitedYear, "hilite");
				}
				Calendar.addClass(year, "hilite");
				cal.hilitedYear = year;
			} else if (cal.hilitedYear) {
				Calendar.removeClass(cal.hilitedYear, "hilite");
			}
		} else if (cal.hilitedYear) {
			Calendar.removeClass(cal.hilitedYear, "hilite");
		}
	}
	return Calendar.stopEvent(ev);
};

Calendar.tableMouseDown = function (ev) {
	if (Calendar.getTargetElement(ev) == Calendar.getElement(ev)) {
		return Calendar.stopEvent(ev);
	}
};

Calendar.calDragIt = function (ev) {
	var cal = Calendar._C;
	if (!(cal && cal.dragging)) {
		return false;
	}
	var posX;
	var posY;
	if (Calendar.is_ie) {
		posY = window.event.clientY + document.body.scrollTop;
		posX = window.event.clientX + document.body.scrollLeft;
	} else {
		posX = ev.pageX;
		posY = ev.pageY;
	}
	cal.hideShowCovered();
	var st = cal.element.style;
	st.left = (posX - cal.xOffs) + "px";
	st.top = (posY - cal.yOffs) + "px";
	return Calendar.stopEvent(ev);
};

Calendar.calDragEnd = function (ev) {
	var cal = Calendar._C;
	if (!cal) {
		return false;
	}
	cal.dragging = false;
	with (Calendar) {
		removeEvent(document, "mousemove", calDragIt);
		removeEvent(document, "mouseup", calDragEnd);
		tableMouseUp(ev);
	}
	cal.hideShowCovered();
};

Calendar.dayMouseDown = function(ev) {
	var el = Calendar.getElement(ev);
	if (el.disabled) {
		return false;
	}
	var cal = el.calendar;
	cal.activeDiv = el;
	Calendar._C = cal;
	if (el.navtype != 300) with (Calendar) {
		if (el.navtype == 50) {
			el._current = el.innerHTML;
			addEvent(document, "mousemove", tableMouseOver);
		} else
			addEvent(document, Calendar.is_ie5 ? "mousemove" : "mouseover", tableMouseOver);
		addClass(el, "hilite active");
		addEvent(document, "mouseup", tableMouseUp);
	} else if (cal.isPopup) {
		cal._dragStart(ev);
	}
	if (el.navtype == -1 || el.navtype == 1) {
		if (cal.timeout) clearTimeout(cal.timeout);
		cal.timeout = setTimeout("Calendar.showMonthsCombo()", 250);
	} else if (el.navtype == -2 || el.navtype == 2) {
		if (cal.timeout) clearTimeout(cal.timeout);
		cal.timeout = setTimeout((el.navtype > 0) ? "Calendar.showYearsCombo(true)" : "Calendar.showYearsCombo(false)", 250);
	} else {
		cal.timeout = null;
	}
	return Calendar.stopEvent(ev);
};

Calendar.dayMouseDblClick = function(ev) {
	Calendar.cellClick(Calendar.getElement(ev), ev || window.event);
	if (Calendar.is_ie) {
		document.selection.empty();
	}
};

Calendar.dayMouseOver = function(ev) {
	var el = Calendar.getElement(ev);
	if (Calendar.isRelated(el, ev) || Calendar._C || el.disabled) {
		return false;
	}
	if (el.ttip) {
		if (el.ttip.substr(0, 1) == "_") {
			el.ttip = el.caldate.print(el.calendar.ttDateFormat) + el.ttip.substr(1);
		}
		el.calendar.tooltips.innerHTML = el.ttip;
	}
	if (el.navtype != 300) {
		Calendar.addClass(el, "hilite");
		if (el.caldate) {
			Calendar.addClass(el.parentNode, "rowhilite");
		}
	}
	return Calendar.stopEvent(ev);
};

Calendar.dayMouseOut = function(ev) {
	with (Calendar) {
		var el = getElement(ev);
		if (isRelated(el, ev) || _C || el.disabled)
			return false;
		removeClass(el, "hilite");
		if (el.caldate)
			removeClass(el.parentNode, "rowhilite");
		if (el.calendar)
			el.calendar.tooltips.innerHTML = _TT["SEL_DATE"];
		return stopEvent(ev);
	}
};

/**
 *  A generic "click" handler :) handles all types of buttons defined in this
 *  calendar.
 */
Calendar.cellClick = function(el, ev) {
	var cal = el.calendar;
	var closing = false;
	var newdate = false;
	var date = null;
	if (typeof el.navtype == "undefined") {
		if (cal.currentDateEl) {
			Calendar.removeClass(cal.currentDateEl, "selected");
			Calendar.addClass(el, "selected");
			closing = (cal.currentDateEl == el);
			if (!closing) {
				cal.currentDateEl = el;
			}
		}
		cal.date.setDateOnly(el.caldate);
		date = cal.date;
		var other_month = !(cal.dateClicked = !el.otherMonth);
		if (!other_month && !cal.currentDateEl)
			cal._toggleMultipleDate(new Date(date));
		else
			newdate = !el.disabled;
		// a date was clicked
		if (other_month)
			cal._init(cal.firstDayOfWeek, date);
	} else {
		if (el.navtype == 200) {
			Calendar.removeClass(el, "hilite");
			cal.callCloseHandler();
			return;
		}
		date = new Date(cal.date);
		if (el.navtype == 0)
			date.setDateOnly(new Date()); // TODAY
		// unless "today" was clicked, we assume no date was clicked so
		// the selected handler will know not to close the calenar when
		// in single-click mode.
		// cal.dateClicked = (el.navtype == 0);
		cal.dateClicked = false;
		var year = date.getFullYear();
		var mon = date.getMonth();
		function setMonth(m) {
			var day = date.getDate();
			var max = date.getMonthDays(m);
			if (day > max) {
				date.setDate(max);
			}
			date.setMonth(m);
		};
		switch (el.navtype) {
		    case 400:
			Calendar.removeClass(el, "hilite");
			var text = Calendar._TT["ABOUT"];
			if (typeof text != "undefined") {
				text += cal.showsTime ? Calendar._TT["ABOUT_TIME"] : "";
			} else {
				// FIXME: this should be removed as soon as lang files get updated!
				text = "Help and about box text is not translated into this language.\n" +
					"If you know this language and you feel generous please update\n" +
					"the corresponding file in \"lang\" subdir to match calendar-en.js\n" +
					"and send it back to <mihai_bazon@yahoo.com> to get it into the distribution  ;-)\n\n" +
					"Thank you!\n" +
					"http://dynarch.com/mishoo/calendar.epl\n";
			}
			alert(text);
			return;
		    case -2:
			if (year > cal.minYear) {
				date.setFullYear(year - 1);
			}
			break;
		    case -1:
			if (mon > 0) {
				setMonth(mon - 1);
			} else if (year-- > cal.minYear) {
				date.setFullYear(year);
				setMonth(11);
			}
			break;
		    case 1:
			if (mon < 11) {
				setMonth(mon + 1);
			} else if (year < cal.maxYear) {
				date.setFullYear(year + 1);
				setMonth(0);
			}
			break;
		    case 2:
			if (year < cal.maxYear) {
				date.setFullYear(year + 1);
			}
			break;
		    case 100:
			cal.setFirstDayOfWeek(el.fdow);
			return;
		    case 50:
			var range = el._range;
			var current = el.innerHTML;
			for (var i = range.length; --i >= 0;)
				if (range[i] == current)
					break;
			if (ev && ev.shiftKey) {
				if (--i < 0)
					i = range.length - 1;
			} else if ( ++i >= range.length )
				i = 0;
			var newval = range[i];
			el.innerHTML = newval;
			cal.onUpdateTime();
			return;
		    case 0:
			// TODAY will bring us here
			if ((typeof cal.getDateStatus == "function") &&
			    cal.getDateStatus(date, date.getFullYear(), date.getMonth(), date.getDate())) {
				return false;
			}
			break;
		}
		if (!date.equalsTo(cal.date)) {
			cal.setDate(date);
			newdate = true;
		} else if (el.navtype == 0)
			newdate = closing = true;
	}
	if (newdate) {
		ev && cal.callHandler();
	}
	if (closing) {
		Calendar.removeClass(el, "hilite");
		ev && cal.callCloseHandler();
	}
};

// END: CALENDAR STATIC FUNCTIONS

// BEGIN: CALENDAR OBJECT FUNCTIONS

/**
 *  This function creates the calendar inside the given parent.  If _par is
 *  null than it creates a popup calendar inside the BODY element.  If _par is
 *  an element, be it BODY, then it creates a non-popup calendar (still
 *  hidden).  Some properties need to be set before calling this function.
 */
Calendar.prototype.create = function (_par) {
	var parent = null;
	if (! _par) {
		// default parent is the document body, in which case we create
		// a popup calendar.
		parent = document.getElementsByTagName("body")[0];
		this.isPopup = true;
	} else {
		parent = _par;
		this.isPopup = false;
	}
	this.date = this.dateStr ? new Date(this.dateStr) : new Date();

	var table = Calendar.createElement("table");
	this.table = table;
	table.cellSpacing = 0;
	table.cellPadding = 0;
	table.calendar = this;
	Calendar.addEvent(table, "mousedown", Calendar.tableMouseDown);

	var div = Calendar.createElement("div");
	this.element = div;
	div.className = "calendar";
	if (this.isPopup) {
		div.style.position = "absolute";
		div.style.display = "none";
	}
	div.appendChild(table);

	var thead = Calendar.createElement("thead", table);
	var cell = null;
	var row = null;

	var cal = this;
	var hh = function (text, cs, navtype) {
		cell = Calendar.createElement("td", row);
		cell.colSpan = cs;
		cell.className = "button";
		if (navtype != 0 && Math.abs(navtype) <= 2)
			cell.className += " nav";
		Calendar._add_evs(cell);
		cell.calendar = cal;
		cell.navtype = navtype;
		cell.innerHTML = "<div unselectable='on'>" + text + "</div>";
		return cell;
	};

	row = Calendar.createElement("tr", thead);
	var title_length = 6;
	(this.isPopup) && --title_length;
	(this.weekNumbers) && ++title_length;

	hh("?", 1, 400).ttip = Calendar._TT["INFO"];
	this.title = hh("", title_length, 300);
	this.title.className = "title";
	if (this.isPopup) {
		this.title.ttip = Calendar._TT["DRAG_TO_MOVE"];
		this.title.style.cursor = "move";
		hh("&#x00d7;", 1, 200).ttip = Calendar._TT["CLOSE"];
	}

	row = Calendar.createElement("tr", thead);
	row.className = "headrow";

	this._nav_py = hh("&#x00ab;", 1, -2);
	this._nav_py.ttip = Calendar._TT["PREV_YEAR"];

	this._nav_pm = hh("&#x2039;", 1, -1);
	this._nav_pm.ttip = Calendar._TT["PREV_MONTH"];

	this._nav_now = hh(Calendar._TT["TODAY"], this.weekNumbers ? 4 : 3, 0);
	this._nav_now.ttip = Calendar._TT["GO_TODAY"];

	this._nav_nm = hh("&#x203a;", 1, 1);
	this._nav_nm.ttip = Calendar._TT["NEXT_MONTH"];

	this._nav_ny = hh("&#x00bb;", 1, 2);
	this._nav_ny.ttip = Calendar._TT["NEXT_YEAR"];

	// day names
	row = Calendar.createElement("tr", thead);
	row.className = "daynames";
	if (this.weekNumbers) {
		cell = Calendar.createElement("td", row);
		cell.className = "name wn";
		cell.innerHTML = Calendar._TT["WK"];
	}
	for (var i = 7; i > 0; --i) {
		cell = Calendar.createElement("td", row);
		if (!i) {
			cell.navtype = 100;
			cell.calendar = this;
			Calendar._add_evs(cell);
		}
	}
	this.firstdayname = (this.weekNumbers) ? row.firstChild.nextSibling : row.firstChild;
	this._displayWeekdays();

	var tbody = Calendar.createElement("tbody", table);
	this.tbody = tbody;

	for (i = 6; i > 0; --i) {
		row = Calendar.createElement("tr", tbody);
		if (this.weekNumbers) {
			cell = Calendar.createElement("td", row);
		}
		for (var j = 7; j > 0; --j) {
			cell = Calendar.createElement("td", row);
			cell.calendar = this;
			Calendar._add_evs(cell);
		}
	}

	if (this.showsTime) {
		row = Calendar.createElement("tr", tbody);
		row.className = "time";

		cell = Calendar.createElement("td", row);
		cell.className = "time";
		cell.colSpan = 2;
		cell.innerHTML = Calendar._TT["TIME"] || "&nbsp;";

		cell = Calendar.createElement("td", row);
		cell.className = "time";
		cell.colSpan = this.weekNumbers ? 4 : 3;

		(function(){
			function makeTimePart(className, init, range_start, range_end) {
				var part = Calendar.createElement("span", cell);
				part.className = className;
				part.innerHTML = init;
				part.calendar = cal;
				part.ttip = Calendar._TT["TIME_PART"];
				part.navtype = 50;
				part._range = [];
				if (typeof range_start != "number")
					part._range = range_start;
				else {
					for (var i = range_start; i <= range_end; ++i) {
						var txt;
						if (i < 10 && range_end >= 10) txt = '0' + i;
						else txt = '' + i;
						part._range[part._range.length] = txt;
					}
				}
				Calendar._add_evs(part);
				return part;
			};
			var hrs = cal.date.getHours();
			var mins = cal.date.getMinutes();
			var t12 = !cal.time24;
			var pm = (hrs > 12);
			if (t12 && pm) hrs -= 12;
			var H = makeTimePart("hour", hrs, t12 ? 1 : 0, t12 ? 12 : 23);
			var span = Calendar.createElement("span", cell);
			span.innerHTML = ":";
			span.className = "colon";
			var M = makeTimePart("minute", mins, 0, 59);
			var AP = null;
			cell = Calendar.createElement("td", row);
			cell.className = "time";
			cell.colSpan = 2;
			if (t12)
				AP = makeTimePart("ampm", pm ? "pm" : "am", ["am", "pm"]);
			else
				cell.innerHTML = "&nbsp;";

			cal.onSetTime = function() {
				var pm, hrs = this.date.getHours(),
					mins = this.date.getMinutes();
				if (t12) {
					pm = (hrs >= 12);
					if (pm) hrs -= 12;
					if (hrs == 0) hrs = 12;
					AP.innerHTML = pm ? "pm" : "am";
				}
				H.innerHTML = (hrs < 10) ? ("0" + hrs) : hrs;
				M.innerHTML = (mins < 10) ? ("0" + mins) : mins;
			};

			cal.onUpdateTime = function() {
				var date = this.date;
				var h = parseInt(H.innerHTML, 10);
				if (t12) {
					if (/pm/i.test(AP.innerHTML) && h < 12)
						h += 12;
					else if (/am/i.test(AP.innerHTML) && h == 12)
						h = 0;
				}
				var d = date.getDate();
				var m = date.getMonth();
				var y = date.getFullYear();
				date.setHours(h);
				date.setMinutes(parseInt(M.innerHTML, 10));
				date.setFullYear(y);
				date.setMonth(m);
				date.setDate(d);
				this.dateClicked = false;
				this.callHandler();
			};
		})();
	} else {
		this.onSetTime = this.onUpdateTime = function() {};
	}

	var tfoot = Calendar.createElement("tfoot", table);

	row = Calendar.createElement("tr", tfoot);
	row.className = "footrow";

	cell = hh(Calendar._TT["SEL_DATE"], this.weekNumbers ? 8 : 7, 300);
	cell.className = "ttip";
	if (this.isPopup) {
		cell.ttip = Calendar._TT["DRAG_TO_MOVE"];
		cell.style.cursor = "move";
	}
	this.tooltips = cell;

	div = Calendar.createElement("div", this.element);
	this.monthsCombo = div;
	div.className = "combo";
	for (i = 0; i < Calendar._MN.length; ++i) {
		var mn = Calendar.createElement("div");
		mn.className = Calendar.is_ie ? "label-IEfix" : "label";
		mn.month = i;
		mn.innerHTML = Calendar._SMN[i];
		div.appendChild(mn);
	}

	div = Calendar.createElement("div", this.element);
	this.yearsCombo = div;
	div.className = "combo";
	for (i = 12; i > 0; --i) {
		var yr = Calendar.createElement("div");
		yr.className = Calendar.is_ie ? "label-IEfix" : "label";
		div.appendChild(yr);
	}

	this._init(this.firstDayOfWeek, this.date);
	parent.appendChild(this.element);
};

/** keyboard navigation, only for popup calendars */
Calendar._keyEvent = function(ev) {
	var cal = window._dynarch_popupCalendar;
	if (!cal || cal.multiple)
		return false;
	(Calendar.is_ie) && (ev = window.event);
	var act = (Calendar.is_ie || ev.type == "keypress"),
		K = ev.keyCode;
	if (ev.ctrlKey) {
		switch (K) {
		    case 37: // KEY left
			act && Calendar.cellClick(cal._nav_pm);
			break;
		    case 38: // KEY up
			act && Calendar.cellClick(cal._nav_py);
			break;
		    case 39: // KEY right
			act && Calendar.cellClick(cal._nav_nm);
			break;
		    case 40: // KEY down
			act && Calendar.cellClick(cal._nav_ny);
			break;
		    default:
			return false;
		}
	} else switch (K) {
	    case 32: // KEY space (now)
		Calendar.cellClick(cal._nav_now);
		break;
	    case 27: // KEY esc
		act && cal.callCloseHandler();
		break;
	    case 37: // KEY left
	    case 38: // KEY up
	    case 39: // KEY right
	    case 40: // KEY down
		if (act) {
			var prev, x, y, ne, el, step;
			prev = K == 37 || K == 38;
			step = (K == 37 || K == 39) ? 1 : 7;
			function setVars() {
				el = cal.currentDateEl;
				var p = el.pos;
				x = p & 15;
				y = p >> 4;
				ne = cal.ar_days[y][x];
			};setVars();
			function prevMonth() {
				var date = new Date(cal.date);
				date.setDate(date.getDate() - step);
				cal.setDate(date);
			};
			function nextMonth() {
				var date = new Date(cal.date);
				date.setDate(date.getDate() + step);
				cal.setDate(date);
			};
			while (1) {
				switch (K) {
				    case 37: // KEY left
					if (--x >= 0)
						ne = cal.ar_days[y][x];
					else {
						x = 6;
						K = 38;
						continue;
					}
					break;
				    case 38: // KEY up
					if (--y >= 0)
						ne = cal.ar_days[y][x];
					else {
						prevMonth();
						setVars();
					}
					break;
				    case 39: // KEY right
					if (++x < 7)
						ne = cal.ar_days[y][x];
					else {
						x = 0;
						K = 40;
						continue;
					}
					break;
				    case 40: // KEY down
					if (++y < cal.ar_days.length)
						ne = cal.ar_days[y][x];
					else {
						nextMonth();
						setVars();
					}
					break;
				}
				break;
			}
			if (ne) {
				if (!ne.disabled)
					Calendar.cellClick(ne);
				else if (prev)
					prevMonth();
				else
					nextMonth();
			}
		}
		break;
	    case 13: // KEY enter
		if (act)
			Calendar.cellClick(cal.currentDateEl, ev);
		break;
	    default:
		return false;
	}
	return Calendar.stopEvent(ev);
};

/**
 *  (RE)Initializes the calendar to the given date and firstDayOfWeek
 */
Calendar.prototype._init = function (firstDayOfWeek, date) {
	var today = new Date(),
		TY = today.getFullYear(),
		TM = today.getMonth(),
		TD = today.getDate();
	this.table.style.visibility = "hidden";
	var year = date.getFullYear();
	if (year < this.minYear) {
		year = this.minYear;
		date.setFullYear(year);
	} else if (year > this.maxYear) {
		year = this.maxYear;
		date.setFullYear(year);
	}
	this.firstDayOfWeek = firstDayOfWeek;
	this.date = new Date(date);
	var month = date.getMonth();
	var mday = date.getDate();
	var no_days = date.getMonthDays();

	// calendar voodoo for computing the first day that would actually be
	// displayed in the calendar, even if it's from the previous month.
	// WARNING: this is magic. ;-)
	date.setDate(1);
	var day1 = (date.getDay() - this.firstDayOfWeek) % 7;
	if (day1 < 0)
		day1 += 7;
	date.setDate(-day1);
	date.setDate(date.getDate() + 1);

	var row = this.tbody.firstChild;
	var MN = Calendar._SMN[month];
	var ar_days = this.ar_days = new Array();
	var weekend = Calendar._TT["WEEKEND"];
	var dates = this.multiple ? (this.datesCells = {}) : null;
	for (var i = 0; i < 6; ++i, row = row.nextSibling) {
		var cell = row.firstChild;
		if (this.weekNumbers) {
			cell.className = "day wn";
			cell.innerHTML = date.getWeekNumber();
			cell = cell.nextSibling;
		}
		row.className = "daysrow";
		var hasdays = false, iday, dpos = ar_days[i] = [];
		for (var j = 0; j < 7; ++j, cell = cell.nextSibling, date.setDate(iday + 1)) {
			iday = date.getDate();
			var wday = date.getDay();
			cell.className = "day";
			cell.pos = i << 4 | j;
			dpos[j] = cell;
			var current_month = (date.getMonth() == month);
			if (!current_month) {
				if (this.showsOtherMonths) {
					cell.className += " othermonth";
					cell.otherMonth = true;
				} else {
					cell.className = "emptycell";
					cell.innerHTML = "&nbsp;";
					cell.disabled = true;
					continue;
				}
			} else {
				cell.otherMonth = false;
				hasdays = true;
			}
			cell.disabled = false;
			cell.innerHTML = this.getDateText ? this.getDateText(date, iday) : iday;
			if (dates)
				dates[date.print("%Y%m%d")] = cell;
			if (this.getDateStatus) {
				var status = this.getDateStatus(date, year, month, iday);
				if (this.getDateToolTip) {
					var toolTip = this.getDateToolTip(date, year, month, iday);
					if (toolTip)
						cell.title = toolTip;
				}
				if (status === true) {
					cell.className += " disabled";
					cell.disabled = true;
				} else {
					if (/disabled/i.test(status))
						cell.disabled = true;
					cell.className += " " + status;
				}
			}
			if (!cell.disabled) {
				cell.caldate = new Date(date);
				cell.ttip = "_";
				if (!this.multiple && current_month
				    && iday == mday && this.hiliteToday) {
					cell.className += " selected";
					this.currentDateEl = cell;
				}
				if (date.getFullYear() == TY &&
				    date.getMonth() == TM &&
				    iday == TD) {
					cell.className += " today";
					cell.ttip += Calendar._TT["PART_TODAY"];
				}
				if (weekend.indexOf(wday.toString()) != -1)
					cell.className += cell.otherMonth ? " oweekend" : " weekend";
			}
		}
		if (!(hasdays || this.showsOtherMonths))
			row.className = "emptyrow";
	}
	this.title.innerHTML = Calendar._MN[month] + ", " + year;
	this.onSetTime();
	this.table.style.visibility = "visible";
	this._initMultipleDates();
	// PROFILE
	// this.tooltips.innerHTML = "Generated in " + ((new Date()) - today) + " ms";
};

Calendar.prototype._initMultipleDates = function() {
	if (this.multiple) {
		for (var i in this.multiple) {
			var cell = this.datesCells[i];
			var d = this.multiple[i];
			if (!d)
				continue;
			if (cell)
				cell.className += " selected";
		}
	}
};

Calendar.prototype._toggleMultipleDate = function(date) {
	if (this.multiple) {
		var ds = date.print("%Y%m%d");
		var cell = this.datesCells[ds];
		if (cell) {
			var d = this.multiple[ds];
			if (!d) {
				Calendar.addClass(cell, "selected");
				this.multiple[ds] = date;
			} else {
				Calendar.removeClass(cell, "selected");
				delete this.multiple[ds];
			}
		}
	}
};

Calendar.prototype.setDateToolTipHandler = function (unaryFunction) {
	this.getDateToolTip = unaryFunction;
};

/**
 *  Calls _init function above for going to a certain date (but only if the
 *  date is different than the currently selected one).
 */
Calendar.prototype.setDate = function (date) {
	if (!date.equalsTo(this.date)) {
		this._init(this.firstDayOfWeek, date);
	}
};

/**
 *  Refreshes the calendar.  Useful if the "disabledHandler" function is
 *  dynamic, meaning that the list of disabled date can change at runtime.
 *  Just * call this function if you think that the list of disabled dates
 *  should * change.
 */
Calendar.prototype.refresh = function () {
	this._init(this.firstDayOfWeek, this.date);
};

/** Modifies the "firstDayOfWeek" parameter (pass 0 for Synday, 1 for Monday, etc.). */
Calendar.prototype.setFirstDayOfWeek = function (firstDayOfWeek) {
	this._init(firstDayOfWeek, this.date);
	this._displayWeekdays();
};

/**
 *  Allows customization of what dates are enabled.  The "unaryFunction"
 *  parameter must be a function object that receives the date (as a JS Date
 *  object) and returns a boolean value.  If the returned value is true then
 *  the passed date will be marked as disabled.
 */
Calendar.prototype.setDateStatusHandler = Calendar.prototype.setDisabledHandler = function (unaryFunction) {
	this.getDateStatus = unaryFunction;
};

/** Customization of allowed year range for the calendar. */
Calendar.prototype.setRange = function (a, z) {
	this.minYear = a;
	this.maxYear = z;
};

/** Calls the first user handler (selectedHandler). */
Calendar.prototype.callHandler = function () {
	if (this.onSelected) {
		this.onSelected(this, this.date.print(this.dateFormat));
	}
};

/** Calls the second user handler (closeHandler). */
Calendar.prototype.callCloseHandler = function () {
	if (this.onClose) {
		this.onClose(this);
	}
	this.hideShowCovered();
};

/** Removes the calendar object from the DOM tree and destroys it. */
Calendar.prototype.destroy = function () {
	var el = this.element.parentNode;
	el.removeChild(this.element);
	Calendar._C = null;
	window._dynarch_popupCalendar = null;
};

/**
 *  Moves the calendar element to a different section in the DOM tree (changes
 *  its parent).
 */
Calendar.prototype.reparent = function (new_parent) {
	var el = this.element;
	el.parentNode.removeChild(el);
	new_parent.appendChild(el);
};

// This gets called when the user presses a mouse button anywhere in the
// document, if the calendar is shown.  If the click was outside the open
// calendar this function closes it.
Calendar._checkCalendar = function(ev) {
	var calendar = window._dynarch_popupCalendar;
	if (!calendar) {
		return false;
	}
	var el = Calendar.is_ie ? Calendar.getElement(ev) : Calendar.getTargetElement(ev);
	for (; el != null && el != calendar.element; el = el.parentNode);
	if (el == null) {
		// calls closeHandler which should hide the calendar.
		window._dynarch_popupCalendar.callCloseHandler();
		return Calendar.stopEvent(ev);
	}
};

/** Shows the calendar. */
Calendar.prototype.show = function () {
	var rows = this.table.getElementsByTagName("tr");
	for (var i = rows.length; i > 0;) {
		var row = rows[--i];
		Calendar.removeClass(row, "rowhilite");
		var cells = row.getElementsByTagName("td");
		for (var j = cells.length; j > 0;) {
			var cell = cells[--j];
			Calendar.removeClass(cell, "hilite");
			Calendar.removeClass(cell, "active");
		}
	}
	this.element.style.display = "block";
	this.hidden = false;
	if (this.isPopup) {
		window._dynarch_popupCalendar = this;
		Calendar.addEvent(document, "keydown", Calendar._keyEvent);
		Calendar.addEvent(document, "keypress", Calendar._keyEvent);
		Calendar.addEvent(document, "mousedown", Calendar._checkCalendar);
	}
	this.hideShowCovered();
};

/**
 *  Hides the calendar.  Also removes any "hilite" from the class of any TD
 *  element.
 */
Calendar.prototype.hide = function () {
	if (this.isPopup) {
		Calendar.removeEvent(document, "keydown", Calendar._keyEvent);
		Calendar.removeEvent(document, "keypress", Calendar._keyEvent);
		Calendar.removeEvent(document, "mousedown", Calendar._checkCalendar);
	}
	this.element.style.display = "none";
	this.hidden = true;
	this.hideShowCovered();
};

/**
 *  Shows the calendar at a given absolute position (beware that, depending on
 *  the calendar element style -- position property -- this might be relative
 *  to the parent's containing rectangle).
 */
Calendar.prototype.showAt = function (x, y) {
	var s = this.element.style;
	s.left = x + "px";
	s.top = y + "px";
	this.show();
};

/** Shows the calendar near a given element. */
Calendar.prototype.showAtElement = function (el, opts) {
	var self = this;
	var p = Calendar.getAbsolutePos(el);
	if (!opts || typeof opts != "string") {
		this.showAt(p.x, p.y + el.offsetHeight);
		return true;
	}
	function fixPosition(box) {
		if (box.x < 0)
			box.x = 0;
		if (box.y < 0)
			box.y = 0;
		var cp = document.createElement("div");
		var s = cp.style;
		s.position = "absolute";
		s.right = s.bottom = s.width = s.height = "0px";
		document.body.appendChild(cp);
		var br = Calendar.getAbsolutePos(cp);
		document.body.removeChild(cp);
		if (Calendar.is_ie) {
			br.y += document.body.scrollTop;
			br.x += document.body.scrollLeft;
		} else {
			br.y += window.scrollY;
			br.x += window.scrollX;
		}
		var tmp = box.x + box.width - br.x;
		if (tmp > 0) box.x -= tmp;
		tmp = box.y + box.height - br.y;
		if (tmp > 0) box.y -= tmp;
	};
	this.element.style.display = "block";
	Calendar.continuation_for_the_fucking_khtml_browser = function() {
		var w = self.element.offsetWidth;
		var h = self.element.offsetHeight;
		self.element.style.display = "none";
		var valign = opts.substr(0, 1);
		var halign = "l";
		if (opts.length > 1) {
			halign = opts.substr(1, 1);
		}
		// vertical alignment
		switch (valign) {
		    case "T": p.y -= h; break;
		    case "B": p.y += el.offsetHeight; break;
		    case "C": p.y += (el.offsetHeight - h) / 2; break;
		    case "t": p.y += el.offsetHeight - h; break;
		    case "b": break; // already there
		}
		// horizontal alignment
		switch (halign) {
		    case "L": p.x -= w; break;
		    case "R": p.x += el.offsetWidth; break;
		    case "C": p.x += (el.offsetWidth - w) / 2; break;
		    case "l": p.x += el.offsetWidth - w; break;
		    case "r": break; // already there
		}
		p.width = w;
		p.height = h + 40;
		self.monthsCombo.style.display = "none";
		fixPosition(p);
		self.showAt(p.x, p.y);
	};
	if (Calendar.is_khtml)
		setTimeout("Calendar.continuation_for_the_fucking_khtml_browser()", 10);
	else
		Calendar.continuation_for_the_fucking_khtml_browser();
};

/** Customizes the date format. */
Calendar.prototype.setDateFormat = function (str) {
	this.dateFormat = str;
};

/** Customizes the tooltip date format. */
Calendar.prototype.setTtDateFormat = function (str) {
	this.ttDateFormat = str;
};

/**
 *  Tries to identify the date represented in a string.  If successful it also
 *  calls this.setDate which moves the calendar to the given date.
 */
Calendar.prototype.parseDate = function(str, fmt) {
	if (!fmt)
		fmt = this.dateFormat;
	this.setDate(Date.parseDate(str, fmt));
};

Calendar.prototype.hideShowCovered = function () {
	if (!Calendar.is_ie && !Calendar.is_opera)
		return;
	function getVisib(obj){
		var value = obj.style.visibility;
		if (!value) {
			if (document.defaultView && typeof (document.defaultView.getComputedStyle) == "function") { // Gecko, W3C
				if (!Calendar.is_khtml)
					value = document.defaultView.
						getComputedStyle(obj, "").getPropertyValue("visibility");
				else
					value = '';
			} else if (obj.currentStyle) { // IE
				value = obj.currentStyle.visibility;
			} else
				value = '';
		}
		return value;
	};

	var tags = new Array("applet", "iframe", "select");
	var el = this.element;

	var p = Calendar.getAbsolutePos(el);
	var EX1 = p.x;
	var EX2 = el.offsetWidth + EX1;
	var EY1 = p.y;
	var EY2 = el.offsetHeight + EY1;

	for (var k = tags.length; k > 0; ) {
		var ar = document.getElementsByTagName(tags[--k]);
		var cc = null;

		for (var i = ar.length; i > 0;) {
			cc = ar[--i];

			p = Calendar.getAbsolutePos(cc);
			var CX1 = p.x;
			var CX2 = cc.offsetWidth + CX1;
			var CY1 = p.y;
			var CY2 = cc.offsetHeight + CY1;

			if (this.hidden || (CX1 > EX2) || (CX2 < EX1) || (CY1 > EY2) || (CY2 < EY1)) {
				if (!cc.__msh_save_visibility) {
					cc.__msh_save_visibility = getVisib(cc);
				}
				cc.style.visibility = cc.__msh_save_visibility;
			} else {
				if (!cc.__msh_save_visibility) {
					cc.__msh_save_visibility = getVisib(cc);
				}
				cc.style.visibility = "hidden";
			}
		}
	}
};

/** Internal function; it displays the bar with the names of the weekday. */
Calendar.prototype._displayWeekdays = function () {
	var fdow = this.firstDayOfWeek;
	var cell = this.firstdayname;
	var weekend = Calendar._TT["WEEKEND"];
	for (var i = 0; i < 7; ++i) {
		cell.className = "day name";
		var realday = (i + fdow) % 7;
		if (i) {
			cell.ttip = Calendar._TT["DAY_FIRST"].replace("%s", Calendar._DN[realday]);
			cell.navtype = 100;
			cell.calendar = this;
			cell.fdow = realday;
			Calendar._add_evs(cell);
		}
		if (weekend.indexOf(realday.toString()) != -1) {
			Calendar.addClass(cell, "weekend");
		}
		cell.innerHTML = Calendar._SDN[(i + fdow) % 7];
		cell = cell.nextSibling;
	}
};

/** Internal function.  Hides all combo boxes that might be displayed. */
Calendar.prototype._hideCombos = function () {
	this.monthsCombo.style.display = "none";
	this.yearsCombo.style.display = "none";
};

/** Internal function.  Starts dragging the element. */
Calendar.prototype._dragStart = function (ev) {
	if (this.dragging) {
		return;
	}
	this.dragging = true;
	var posX;
	var posY;
	if (Calendar.is_ie) {
		posY = window.event.clientY + document.body.scrollTop;
		posX = window.event.clientX + document.body.scrollLeft;
	} else {
		posY = ev.clientY + window.scrollY;
		posX = ev.clientX + window.scrollX;
	}
	var st = this.element.style;
	this.xOffs = posX - parseInt(st.left);
	this.yOffs = posY - parseInt(st.top);
	with (Calendar) {
		addEvent(document, "mousemove", calDragIt);
		addEvent(document, "mouseup", calDragEnd);
	}
};

// BEGIN: DATE OBJECT PATCHES

/** Adds the number of days array to the Date object. */
Date._MD = new Array(31,28,31,30,31,30,31,31,30,31,30,31);

/** Constants used for time computations */
Date.SECOND = 1000 /* milliseconds */;
Date.MINUTE = 60 * Date.SECOND;
Date.HOUR   = 60 * Date.MINUTE;
Date.DAY    = 24 * Date.HOUR;
Date.WEEK   =  7 * Date.DAY;

Date.parseDate = function(str, fmt) {
	var today = new Date();
	var y = 0;
	var m = -1;
	var d = 0;
	var a = str.split(/\W+/);
	var b = fmt.match(/%./g);
	var i = 0, j = 0;
	var hr = 0;
	var min = 0;
	for (i = 0; i < a.length; ++i) {
		if (!a[i])
			continue;
		switch (b[i]) {
		    case "%d":
		    case "%e":
			d = parseInt(a[i], 10);
			break;

		    case "%m":
			m = parseInt(a[i], 10) - 1;
			break;

		    case "%Y":
		    case "%y":
			y = parseInt(a[i], 10);
			(y < 100) && (y += (y > 29) ? 1900 : 2000);
			break;

		    case "%b":
		    case "%B":
			for (j = 0; j < 12; ++j) {
				if (Calendar._MN[j].substr(0, a[i].length).toLowerCase() == a[i].toLowerCase()) { m = j; break; }
			}
			break;

		    case "%H":
		    case "%I":
		    case "%k":
		    case "%l":
			hr = parseInt(a[i], 10);
			break;

		    case "%P":
		    case "%p":
			if (/pm/i.test(a[i]) && hr < 12)
				hr += 12;
			else if (/am/i.test(a[i]) && hr >= 12)
				hr -= 12;
			break;

		    case "%M":
			min = parseInt(a[i], 10);
			break;
		}
	}
	if (isNaN(y)) y = today.getFullYear();
	if (isNaN(m)) m = today.getMonth();
	if (isNaN(d)) d = today.getDate();
	if (isNaN(hr)) hr = today.getHours();
	if (isNaN(min)) min = today.getMinutes();
	if (y != 0 && m != -1 && d != 0)
		return new Date(y, m, d, hr, min, 0);
	y = 0; m = -1; d = 0;
	for (i = 0; i < a.length; ++i) {
		if (a[i].search(/[a-zA-Z]+/) != -1) {
			var t = -1;
			for (j = 0; j < 12; ++j) {
				if (Calendar._MN[j].substr(0, a[i].length).toLowerCase() == a[i].toLowerCase()) { t = j; break; }
			}
			if (t != -1) {
				if (m != -1) {
					d = m+1;
				}
				m = t;
			}
		} else if (parseInt(a[i], 10) <= 12 && m == -1) {
			m = a[i]-1;
		} else if (parseInt(a[i], 10) > 31 && y == 0) {
			y = parseInt(a[i], 10);
			(y < 100) && (y += (y > 29) ? 1900 : 2000);
		} else if (d == 0) {
			d = a[i];
		}
	}
	if (y == 0)
		y = today.getFullYear();
	if (m != -1 && d != 0)
		return new Date(y, m, d, hr, min, 0);
	return today;
};

/** Returns the number of days in the current month */
Date.prototype.getMonthDays = function(month) {
	var year = this.getFullYear();
	if (typeof month == "undefined") {
		month = this.getMonth();
	}
	if (((0 == (year%4)) && ( (0 != (year%100)) || (0 == (year%400)))) && month == 1) {
		return 29;
	} else {
		return Date._MD[month];
	}
};

/** Returns the number of day in the year. */
Date.prototype.getDayOfYear = function() {
	var now = new Date(this.getFullYear(), this.getMonth(), this.getDate(), 0, 0, 0);
	var then = new Date(this.getFullYear(), 0, 0, 0, 0, 0);
	var time = now - then;
	return Math.floor(time / Date.DAY);
};

/** Returns the number of the week in year, as defined in ISO 8601. */
Date.prototype.getWeekNumber = function() {
	var d = new Date(this.getFullYear(), this.getMonth(), this.getDate(), 0, 0, 0);
	var DoW = d.getDay();
	d.setDate(d.getDate() - (DoW + 6) % 7 + 3); // Nearest Thu
	var ms = d.valueOf(); // GMT
	d.setMonth(0);
	d.setDate(4); // Thu in Week 1
	return Math.round((ms - d.valueOf()) / (7 * 864e5)) + 1;
};

/** Checks date and time equality */
Date.prototype.equalsTo = function(date) {
	return ((this.getFullYear() == date.getFullYear()) &&
		(this.getMonth() == date.getMonth()) &&
		(this.getDate() == date.getDate()) &&
		(this.getHours() == date.getHours()) &&
		(this.getMinutes() == date.getMinutes()));
};

/** Set only the year, month, date parts (keep existing time) */
Date.prototype.setDateOnly = function(date) {
	var tmp = new Date(date);
	this.setDate(1);
	this.setFullYear(tmp.getFullYear());
	this.setMonth(tmp.getMonth());
	this.setDate(tmp.getDate());
};

/** Prints the date in a string according to the given format. */
Date.prototype.print = function (str) {
	var m = this.getMonth();
	var d = this.getDate();
	var y = this.getFullYear();
	var wn = this.getWeekNumber();
	var w = this.getDay();
	var s = {};
	var hr = this.getHours();
	var pm = (hr >= 12);
	var ir = (pm) ? (hr - 12) : hr;
	var dy = this.getDayOfYear();
	if (ir == 0)
		ir = 12;
	var min = this.getMinutes();
	var sec = this.getSeconds();
	s["%a"] = Calendar._SDN[w]; // abbreviated weekday name [FIXME: I18N]
	s["%A"] = Calendar._DN[w]; // full weekday name
	s["%b"] = Calendar._SMN[m]; // abbreviated month name [FIXME: I18N]
	s["%B"] = Calendar._MN[m]; // full month name
	// FIXME: %c : preferred date and time representation for the current locale
	s["%C"] = 1 + Math.floor(y / 100); // the century number
	s["%d"] = (d < 10) ? ("0" + d) : d; // the day of the month (range 01 to 31)
	s["%e"] = d; // the day of the month (range 1 to 31)
	// FIXME: %D : american date style: %m/%d/%y
	// FIXME: %E, %F, %G, %g, %h (man strftime)
	s["%H"] = (hr < 10) ? ("0" + hr) : hr; // hour, range 00 to 23 (24h format)
	s["%I"] = (ir < 10) ? ("0" + ir) : ir; // hour, range 01 to 12 (12h format)
	s["%j"] = (dy < 100) ? ((dy < 10) ? ("00" + dy) : ("0" + dy)) : dy; // day of the year (range 001 to 366)
	s["%k"] = hr;		// hour, range 0 to 23 (24h format)
	s["%l"] = ir;		// hour, range 1 to 12 (12h format)
	s["%m"] = (m < 9) ? ("0" + (1+m)) : (1+m); // month, range 01 to 12
	s["%M"] = (min < 10) ? ("0" + min) : min; // minute, range 00 to 59
	s["%n"] = "\n";		// a newline character
	s["%p"] = pm ? "PM" : "AM";
	s["%P"] = pm ? "pm" : "am";
	// FIXME: %r : the time in am/pm notation %I:%M:%S %p
	// FIXME: %R : the time in 24-hour notation %H:%M
	s["%s"] = Math.floor(this.getTime() / 1000);
	s["%S"] = (sec < 10) ? ("0" + sec) : sec; // seconds, range 00 to 59
	s["%t"] = "\t";		// a tab character
	// FIXME: %T : the time in 24-hour notation (%H:%M:%S)
	s["%U"] = s["%W"] = s["%V"] = (wn < 10) ? ("0" + wn) : wn;
	s["%u"] = w + 1;	// the day of the week (range 1 to 7, 1 = MON)
	s["%w"] = w;		// the day of the week (range 0 to 6, 0 = SUN)
	// FIXME: %x : preferred date representation for the current locale without the time
	// FIXME: %X : preferred time representation for the current locale without the date
	s["%y"] = ('' + y).substr(2, 2); // year without the century (range 00 to 99)
	s["%Y"] = y;		// year with the century
	s["%%"] = "%";		// a literal '%' character

	var re = /%./g;
	if (!Calendar.is_ie5 && !Calendar.is_khtml)
		return str.replace(re, function (par) { return s[par] || par; });

	var a = str.match(re);
	for (var i = 0; i < a.length; i++) {
		var tmp = s[a[i]];
		if (tmp) {
			re = new RegExp(a[i], 'g');
			str = str.replace(re, tmp);
		}
	}

	return str;
};

Date.prototype.__msh_oldSetFullYear = Date.prototype.setFullYear;
Date.prototype.setFullYear = function(y) {
	var d = new Date(this);
	d.__msh_oldSetFullYear(y);
	if (d.getMonth() != this.getMonth())
		this.setDate(28);
	this.__msh_oldSetFullYear(y);
};

// END: DATE OBJECT PATCHES


// global object that remembers the calendar
window._dynarch_popupCalendar = null;
/*  Copyright Mihai Bazon, 2002, 2003  |  http://dynarch.com/mishoo/
 * ---------------------------------------------------------------------------
 *
 * The DHTML Calendar
 *
 * Details and latest version at:
 * http://dynarch.com/mishoo/calendar.epl
 *
 * This script is distributed under the GNU Lesser General Public License.
 * Read the entire license text here: http://www.gnu.org/licenses/lgpl.html
 *
 * This file defines helper functions for setting up the calendar.  They are
 * intended to help non-programmers get a working calendar on their site
 * quickly.  This script should not be seen as part of the calendar.  It just
 * shows you what one can do with the calendar, while in the same time
 * providing a quick and simple method for setting it up.  If you need
 * exhaustive customization of the calendar creation process feel free to
 * modify this code to suit your needs (this is recommended and much better
 * than modifying calendar.js itself).
 */

// $Id: calendar-setup.js,v 1.25 2005/03/07 09:51:33 mishoo Exp $

/**
 *  This function "patches" an input field (or other element) to use a calendar
 *  widget for date selection.
 *
 *  The "params" is a single object that can have the following properties:
 *
 *    prop. name   | description
 *  -------------------------------------------------------------------------------------------------
 *   inputField    | the ID of an input field to store the date
 *   displayArea   | the ID of a DIV or other element to show the date
 *   button        | ID of a button or other element that will trigger the calendar
 *   eventName     | event that will trigger the calendar, without the "on" prefix (default: "click")
 *   ifFormat      | date format that will be stored in the input field
 *   daFormat      | the date format that will be used to display the date in displayArea
 *   singleClick   | (true/false) wether the calendar is in single click mode or not (default: true)
 *   firstDay      | numeric: 0 to 6.  "0" means display Sunday first, "1" means display Monday first, etc.
 *   align         | alignment (default: "Br"); if you don't know what's this see the calendar documentation
 *   range         | array with 2 elements.  Default: [1900, 2999] -- the range of years available
 *   weekNumbers   | (true/false) if it's true (default) the calendar will display week numbers
 *   flat          | null or element ID; if not null the calendar will be a flat calendar having the parent with the given ID
 *   flatCallback  | function that receives a JS Date object and returns an URL to point the browser to (for flat calendar)
 *   disableFunc   | function that receives a JS Date object and should return true if that date has to be disabled in the calendar
 *   onSelect      | function that gets called when a date is selected.  You don't _have_ to supply this (the default is generally okay)
 *   onClose       | function that gets called when the calendar is closed.  [default]
 *   onUpdate      | function that gets called after the date is updated in the input field.  Receives a reference to the calendar.
 *   date          | the date that the calendar will be initially displayed to
 *   showsTime     | default: false; if true the calendar will include a time selector
 *   timeFormat    | the time format; can be "12" or "24", default is "12"
 *   electric      | if true (default) then given fields/date areas are updated for each move; otherwise they're updated only on close
 *   step          | configures the step of the years in drop-down boxes; default: 2
 *   position      | configures the calendar absolute position; default: null
 *   cache         | if "true" (but default: "false") it will reuse the same calendar object, where possible
 *   showOthers    | if "true" (but default: "false") it will show days from other months too
 *
 *  None of them is required, they all have default values.  However, if you
 *  pass none of "inputField", "displayArea" or "button" you'll get a warning
 *  saying "nothing to setup".
 */
Calendar.setup = function (params) {
	function param_default(pname, def) { if (typeof params[pname] == "undefined") { params[pname] = def; } };

	param_default("inputField",     null);
	param_default("displayArea",    null);
	param_default("button",         null);
	param_default("eventName",      "click");
	param_default("ifFormat",       "%Y/%m/%d");
	param_default("daFormat",       "%Y/%m/%d");
	param_default("singleClick",    true);
	param_default("disableFunc",    null);
	param_default("dateStatusFunc", params["disableFunc"]);	// takes precedence if both are defined
	param_default("dateText",       null);
	param_default("firstDay",       null);
	param_default("align",          "Br");
	param_default("range",          [1900, 2999]);
	param_default("weekNumbers",    true);
	param_default("flat",           null);
	param_default("flatCallback",   null);
	param_default("onSelect",       null);
	param_default("onClose",        null);
	param_default("onUpdate",       null);
	param_default("date",           null);
	param_default("showsTime",      false);
	param_default("timeFormat",     "24");
	param_default("electric",       true);
	param_default("step",           2);
	param_default("position",       null);
	param_default("cache",          false);
	param_default("showOthers",     false);
	param_default("multiple",       null);

	var tmp = ["inputField", "displayArea", "button"];
	for (var i in tmp) {
		if (typeof params[tmp[i]] == "string") {
			params[tmp[i]] = document.getElementById(params[tmp[i]]);
		}
	}
	if (!(params.flat || params.multiple || params.inputField || params.displayArea || params.button)) {
		alert("Calendar.setup:\n  Nothing to setup (no fields found).  Please check your code");
		return false;
	}

	function onSelect(cal) {
		var p = cal.params;
		var update = (cal.dateClicked || p.electric);
		if (update && p.inputField) {
			p.inputField.value = cal.date.print(p.ifFormat);
			if (typeof p.inputField.onchange == "function")
				p.inputField.onchange();
		}
		if (update && p.displayArea)
			p.displayArea.innerHTML = cal.date.print(p.daFormat);
		if (update && typeof p.onUpdate == "function")
			p.onUpdate(cal);
		if (update && p.flat) {
			if (typeof p.flatCallback == "function")
				p.flatCallback(cal);
		}
		if (update && p.singleClick && cal.dateClicked)
			cal.callCloseHandler();
	};

	if (params.flat != null) {
		if (typeof params.flat == "string")
			params.flat = document.getElementById(params.flat);
		if (!params.flat) {
			alert("Calendar.setup:\n  Flat specified but can't find parent.");
			return false;
		}
		var cal = new Calendar(params.firstDay, params.date, params.onSelect || onSelect);
		cal.showsOtherMonths = params.showOthers;
		cal.showsTime = params.showsTime;
		cal.time24 = (params.timeFormat == "24");
		cal.params = params;
		cal.weekNumbers = params.weekNumbers;
		cal.setRange(params.range[0], params.range[1]);
		cal.setDateStatusHandler(params.dateStatusFunc);
		cal.getDateText = params.dateText;
		if (params.ifFormat) {
			cal.setDateFormat(params.ifFormat);
		}
		if (params.inputField && typeof params.inputField.value == "string") {
			cal.parseDate(params.inputField.value);
		}
		cal.create(params.flat);
		cal.show();
		return false;
	}

	var triggerEl = params.button || params.displayArea || params.inputField;
	triggerEl["on" + params.eventName] = function() {
		var dateEl = params.inputField || params.displayArea;
		var dateFmt = params.inputField ? params.ifFormat : params.daFormat;
		var mustCreate = false;
		var cal = window.calendar;
		if (dateEl)
			params.date = Date.parseDate(dateEl.value || dateEl.innerHTML, dateFmt);
		if (!(cal && params.cache)) {
			window.calendar = cal = new Calendar(params.firstDay,
							     params.date,
							     params.onSelect || onSelect,
							     params.onClose || function(cal) { cal.hide(); });
			cal.showsTime = params.showsTime;
			cal.time24 = (params.timeFormat == "24");
			cal.weekNumbers = params.weekNumbers;
			mustCreate = true;
		} else {
			if (params.date)
				cal.setDate(params.date);
			cal.hide();
		}
		if (params.multiple) {
			cal.multiple = {};
			for (var i = params.multiple.length; --i >= 0;) {
				var d = params.multiple[i];
				var ds = d.print("%Y%m%d");
				cal.multiple[ds] = d;
			}
		}
		cal.showsOtherMonths = params.showOthers;
		cal.yearStep = params.step;
		cal.setRange(params.range[0], params.range[1]);
		cal.params = params;
		cal.setDateStatusHandler(params.dateStatusFunc);
		cal.getDateText = params.dateText;
		cal.setDateFormat(dateFmt);
		if (mustCreate)
			cal.create();
		cal.refresh();
		if (!params.position)
			cal.showAtElement(params.button || params.displayArea || params.inputField, params.align);
		else
			cal.showAt(params.position[0], params.position[1]);
		return false;
	};
        // add the on key press equivalent
        if ("click" == params.eventName && params.button) {
            triggerEl["onkeypress"] = function(ev) {
                (Calendar.is_ie) && (ev = window.event);
                if (ev.keyCode == 13) {
                  this.onclick();
                }
            }
        }

	return cal;
};
