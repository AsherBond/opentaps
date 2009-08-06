if(!dojo._hasResource["dojox.layout.FloatingPane"]){
dojo._hasResource["dojox.layout.FloatingPane"] = true;
dojo.provide("dojox.layout.FloatingPane");

dojo.require("dijit.TitlePane");
dojo.require("dojo.dnd.move"); 

dojo.declare(
	"dojox.layout.FloatingPane",
	[dijit.TitlePane],
	null,{	
	// summary:
	//
	// most simple widget extension, ever. Makes a dijit.TitlePane float
	// and draggable by it's title
	// and over-rides onClick to onDblClick for wipeIn/Out of containerNode

	// closable: Boolean
	//	allow closure of this Node
	closable: true,

	// title: String
	//	title to put in titlebar
	title: null,

	// duration: Integer
	//	time is MS to spend toggling in/out node
	duration: 400,

	// animations for toggle
	_showAnim: null,
	_hideAnim: null, 

	templateString:"<div id=\"${id}\">\n\t<div dojoAttachEvent=\"ondblclick: _onTitleClick; onkeypress: _onTitleKey\" tabindex=\"0\" \n\t\t\twaiRole=\"button\" class=\"dijitTitlePaneTitle\" dojoAttachPoint=\"focusNode\">\n\t\t<span dojoAttachPoint=\"closeNode\" dojoAttachEvent=\"onclick: hide\" style=\"float:right; display:none; \" class=\"dijitDialogCloseIcon\"></span>\n\t\t<span dojoAttachPoint=\"titleNode\" class=\"dijitInlineBox dijitTitleNode\"></span>\n\t</div>\n\t<div dojoAttachPoint=\"containerNode\" waiRole=\"region\" tabindex=\"-1\" class=\"${contentClass}\"></div>\n</div>\n",

	postCreate: function() {
		this.title = this.label || this.title; 
		new dojo.dnd.Moveable(this.domNode,this.focusNode);
		dojox.layout.FloatingPane.superclass.postCreate.call(this);
	},

	// extend 		
	hide: function() {
		dojo.fadeOut({node:this.domNode, duration:this.duration}).play();
	},
	show: function() {
		dojo.fadeIn({node:this.domNode, duration:this.duration}).play();
	}

});

}
