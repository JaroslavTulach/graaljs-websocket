(function (jvm) {

    var EventBase = {
        bubbles: false,
        cancelable: false
    };

    var WebSocket = (function () {

        function WebSocket(url, protocols) {
            this.state_ = WebSocket.CONNECTING;

            this.url_ = url;

            if (typeof protocols === "string") {
                protocols = [protocols];
            }
            this.protocols_ = protocols;

            this.connection_ = jvm(
                'new-web-socket-connection',
                url,
                protocols,
                this._handle_open.bind(this),
                this._handle_close.bind(this),
                this._handle_error.bind(this),
                this._handle_message.bind(this)
            );
        }

        //
        // Constants
        //

        WebSocket.CONNECTING = 0;
        WebSocket.OPEN = 1;
        WebSocket.CLOSING = 2;
        WebSocket.CLOSED = 3;

        //
        // Properties
        //

        Object.defineProperty(WebSocket.prototype, "url", {
            get: function () {
                return this.url_;
            },
            enumerable: false,
            configurable: true
        });

        Object.defineProperty(WebSocket.prototype, "protocol", {
            get: function () {
                return this.protocols_ ? this.protocols_[0] : '';
            },
            enumerable: false,
            configurable: true
        });

        Object.defineProperty(WebSocket.prototype, "readyState", {
            get: function () {
                return this.state_;
            },
            enumerable: false,
            configurable: true
        });

        Object.defineProperty(WebSocket.prototype, "binaryType", {
            value: "arraybuffer",
            writable: true,
            enumerable: false,
            configurable: true
        });

        //
        // Listeners
        //

        Object.defineProperty(WebSocket.prototype, "onopen", {
            get: function () {
                return jvm('get-on-listener', 'open');
            },
            set: function (listener) {
                jvm('set-on-listener', 'open', listener);
            },
            enumerable: false,
            configurable: true
        });

        Object.defineProperty(WebSocket.prototype, "onclose", {
            get: function () {
                return jvm('get-on-listener', 'close');
            },
            set: function (listener) {
                jvm('set-on-listener', 'close', listener);
            },
            enumerable: false,
            configurable: true
        });

        Object.defineProperty(WebSocket.prototype, "onmessage", {
            get: function () {
                return jvm('get-on-listener', 'message');
            },
            set: function (listener) {
                jvm('set-on-listener', 'message', listener);
            },
            enumerable: false,
            configurable: true
        });

        Object.defineProperty(WebSocket.prototype, "onerror", {
            get: function () {
                return jvm('get-on-listener', 'error');
            },
            set: function (listener) {
                jvm('set-on-listener', 'error', listener);
            },
            enumerable: false,
            configurable: true
        });


        //
        // Methods
        //

        WebSocket.prototype.send = function (data) {
            if (typeof data.valueOf() === 'string') {
                jvm('web-socket-send-text', this.connection_, data);
            } else {
                jvm('web-socket-send-binary', this.connection_, data);
            }
        };

        WebSocket.prototype.close = function (code, reason) {
            this.state_ = WebSocket.CLOSING;
            if (code === undefined) {
                jvm('web-socket-terminate', this.connection_);
            } else {
                jvm('web-socket-close', this.connection_, code, reason);
            }
        }

        //
        // EventTarget
        //

        WebSocket.prototype.addEventListener = function (type, listener) {
            jvm('add-event-listener', type, listener);
        };

        WebSocket.prototype.removeEventListener = function (type, listener) {
            jvm('remove-event-listener', type, listener);
        };

        WebSocket.prototype.dispatchEvent = function (event) {
            var listeners = jvm('get-event-listeners', event.type);

            event.target = this;
            // event.timeStamp = TODO

            for (const listener of listeners) {
                try {
                    listener(event);
                } catch (e) {
                };
            };
        };

        WebSocket.prototype._handle_open = function () {
            this.state_ = WebSocket.OPEN;
            this.dispatchEvent({
                ...EventBase,
                type: 'open'
            });
        };

        WebSocket.prototype._handle_close = function (code, reason) {
            this.state_ = WebSocket.CLOSED;
            this.dispatchEvent({
                ...EventBase,
                type: 'close',
                code: code,
                reason: reason,
                wasClean: true
            });
        };

        WebSocket.prototype._handle_error = function (message) {
            this.dispatchEvent({
                ...EventBase,
                type: 'error',
                message: message
            });
        };

        WebSocket.prototype._handle_message = function (data) {
            this.dispatchEvent({
                ...EventBase,
                type: 'message',
                data: data
            });
        };

        return WebSocket;
    }());

    globalThis.WebSocket = WebSocket;

    globalThis.WebSocketServer = function (config) {
        debugger;
        var webSocketServerData = jvm(null, 'new-web-socket-server-data', config);
        var wss = {
            on: function (type, callback) {
                var webSocketData = jvm(webSocketServerData, type, callback);
                var ws = {
                    on: function (type, callback) {
                        jvm(webSocketData, type, callback);
                    },
                    send: function (msg) {
                        jvm(webSocketData, "send", msg);
                    }
                }
                callback(ws);
            }
        };
        return wss;
    };
})
