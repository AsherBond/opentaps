/*
 * Copyright (c) 2006 - 2007 Open Source Strategies, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 * 
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
