if(!dojo._hasResource["dojo.data.JsonItemStoreAutoComplete"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojo.data.JsonItemStoreAutoComplete"] = true;
dojo.provide("dojo.data.JsonItemStoreAutoComplete");

dojo.require("dojo.data.JsonItemStore");

dojo.declare("dojo.data.JsonItemStoreAutoComplete",
	dojo.data.JsonItemStore,
	
	{
    _fetchItems: function(  /* Object */ keywordArgs, 
                            /* Function */ findCallback, 
                            /* Function */ errorCallback){
        //  summary: 
        //      See dojo.data.util.simpleFetch.fetch()
        var self = this;
        var filter = function(requestArgs, arrayOfAllItems){
            var items = null;
            if(requestArgs.query){
                var ignoreCase = requestArgs.queryOptions ? requestArgs.queryOptions.ignoreCase : false; 
                items = [];

                //See if there are any string values that can be regexp parsed first to avoid multiple regexp gens on the
                //same value for each item examined.  Much more efficient.
                var regexpList = {};
                for(var key in requestArgs.query){
                    var value = requestArgs.query[key];
                    if(typeof value === "string"){
                        regexpList[key] = dojo.data.util.filter.patternToRegExp(value, ignoreCase);
                    }
                }

                for(var i = 0; i < arrayOfAllItems.length; ++i){
                    var match = true;
                    var candidateItem = arrayOfAllItems[i];
                    /*
                    for(var key in requestArgs.query) {
                        var value = requestArgs.query[key];
                        if (!self._containsValue(candidateItem, key, value, regexpList[key])){
                            match = false;
                        }
                    }
                    */
                    if(match){
                        items.push(candidateItem);
                    }
                }
                findCallback(items, requestArgs);
            }else{
                // We want a copy to pass back in case the parent wishes to sort the array.  We shouldn't allow resort 
                // of the internal list so that multiple callers can get listsand sort without affecting each other.
                if(self._arrayOfAllItems.length> 0){
                    items = self._arrayOfAllItems.slice(0,self._arrayOfAllItems.length); 
                }
                findCallback(items, requestArgs);
            }
        };

        //if(this._loadFinished){
            //filter(keywordArgs, this._arrayOfAllItems);
        //}else{
            
        // retrieve the user input
        var keyword = null;
            
        if (keywordArgs.query) {
            keyword = keywordArgs.query['name'];
        }
            
        if (keyword) {
            var pos = keyword.lastIndexOf('*');
            keyword = keyword.substr(0, pos);
        }
            
        //console.debug("keyword = " + keyword);
        
        if (keyword) {

            if(this._jsonFileUrl){
                var getArgs = {
                        url: self._jsonFileUrl + "?keyword=" + keyword,
                        handleAs: "json-comment-optional"
                    };
                var getHandler = dojo.xhrGet(getArgs);
                getHandler.addCallback(function(data){
                    // console.debug(dojo.toJson(data));
                    self._loadFinished = true;
                    try{
                        self._arrayOfAllItems = self._getItemsFromLoadedData(data);
                        filter(keywordArgs, self._arrayOfAllItems);
                    }catch(e){
                        errorCallback(e, keywordArgs);
                    }

                });
                getHandler.addErrback(function(error){
                    errorCallback(error, keywordArgs);
                });
            }else if(this._jsonData){
                try{
                    this._loadFinished = true;
                    this._arrayOfAllItems = this._getItemsFromLoadedData(this._jsonData);
                    this._jsonData = null;
                    filter(keywordArgs, this._arrayOfAllItems);
                }catch(e){
                    errorCallback(e, keywordArgs);
                }
            }else{
                errorCallback(new Error("dojo.data.JsonItemStoreAutoComplete: No JSON source data was provided as either URL or a nested Javascript object."), keywordArgs);
            }
        }
    }
});

//Mix in the simple fetch implementation to this class.
dojo.extend(dojo.data.JsonItemStoreAutoComplete,dojo.data.util.simpleFetch);


}
