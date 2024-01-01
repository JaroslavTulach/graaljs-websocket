import { WebsocketProvider } from './node_modules/y-websocket/dist/y-websocket.cjs'
import * as Y from './node_modules/yjs/dist/yjs.cjs'

globalThis.Buffer = require('buffer').Buffer;

const doc = new Y.Doc();
const wsProvider = new WebsocketProvider('ws://localhost:1234', 'my-roomname', doc);
wsProvider.on('status', event => {
  print(event.status);
});
