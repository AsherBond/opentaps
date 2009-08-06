var FCKMyCombo_command = function() {} 
FCKMyCombo_command.prototype.Execute = function( itemText , itemLabel ) {  
    if ( itemText != "" ) {
        FCK.InsertHtml( itemText );
    }
}
FCKMyCombo_command.prototype.GetState = function() {
    return;
}

FCKCommands.RegisterCommand( 'InsertTag' , new FCKMyCombo_command() ) ; 


var FCKToolbarMyCombo = function( tooltip , style ) {
    this.Command = FCKCommands.GetCommand( 'InsertTag' );
    this.CommandName = 'InsertTag';
    this.Label = this.GetLabel();
    this.Tooltip = tooltip ? tooltip : this.Label;
    this.Style = style;
};
FCKToolbarMyCombo.prototype = new FCKToolbarSpecialCombo;
FCKToolbarMyCombo.prototype.GetLabel = function() {
    return FCKConfig.insertTagsLabel;
};

FCKToolbarMyCombo.prototype.CreateItems = function( A ) {
    for ( var i = 0; i < FCKConfig.tags.length; i++ ) {
        this._Combo.AddItem( FCKConfig.tags[i].tag , FCKConfig.tags[i].description ) ;
    }
}

FCKToolbarItems.RegisterItem( 'InsertTag' , new FCKToolbarMyCombo( FCKConfig.insertTagsLabel, FCK_TOOLBARITEM_ICONTEXT ) ) ;
