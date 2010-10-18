%%
%% Copyright (c) Open Source Strategies, Inc.
%% 
%% Opentaps is free software: you can redistribute it and/or modify it
%% under the terms of the GNU Affero General Public License as published
%% by the Free Software Foundation, either version 3 of the License, or
%% (at your option) any later version.
%%
%% Opentaps is distributed in the hope that it will be useful,
%% but WITHOUT ANY WARRANTY; without even the implied warranty of
%% MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
%% GNU Affero General Public License for more details.
%%
%% You should have received a copy of the GNU Affero General Public License
%% along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
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
