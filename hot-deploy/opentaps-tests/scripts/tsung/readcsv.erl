%%
%% Copyright (c) 2006 - 2007 Open Source Strategies, Inc.
%% 
%% This program is free software; you can redistribute it and/or modify
%% it under the terms of the Honest Public License.
%% 
%% This program is distributed in the hope that it will be useful,
%% but WITHOUT ANY WARRANTY; without even the implied warranty of
%% MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
%% Honest Public License for more details.
%% 
%% You should have received a copy of the Honest Public License
%% along with this program; if not, write to Funambol,
%% 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
%%

-module(readcsv).
-export([user/1]).

%%----------------------------------------------------------------------
%% Func: user/1
%% Returns: a string with association of a user
%% and a password read from a file
%%----------------------------------------------------------------------

user(Pid)->
    {ok,Line} = ts_file_server:get_next_line(),
    [Username, Passwd] = string:tokens(Line,";"),
    "USERNAME=" ++ Username ++"&PASSWORD=" ++ Passwd.
