import * as Y from 'yjs';
import { WebsocketProvider } from 'y-websocket';

globalThis.Buffer = require('buffer').Buffer;

const doc = new Y.Doc();
const wsProvider = new WebsocketProvider('ws://[::1]:1234', 'my-roomname', doc);
wsProvider.on('status', event => {
  print(event.status);
});

const yarray = doc.getArray('my-array');
yarray.observe(event => {
  console.log('yarray was modified:', yarray.toArray());
});
// every time a local or remote client modifies yarray, the observer is called
yarray.push(['val']);
