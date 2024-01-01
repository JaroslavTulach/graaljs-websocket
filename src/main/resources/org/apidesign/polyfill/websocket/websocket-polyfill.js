(function (jvm) {

    globalThis.WebSocket = function(config) {
        debugger;
        return {};
    };

    globalThis.WebSocketServer = function(config) {
        var webSocketServerData = jvm(null, 'new-web-socket-server-data', config);
        var wss = {
            on : function(type, callback) {
               var webSocketData = jvm(webSocketServerData, type, callback);
               var ws = {
                  on : function(type, callback) {
                    jvm(webSocketData, type, callback);
                  },
                  send : function(msg) {
                    jvm(webSocketData, "send", msg);
                  }
               }
               callback(ws);
            }
        };
        return wss;
    };
})
