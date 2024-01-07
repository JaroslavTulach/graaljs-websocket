import * as Y from 'yjs';
import { WebsocketProvider } from 'y-websocket';

globalThis.Buffer = require('buffer').Buffer;

/*
const ws = new WebSocket('ws://localhost:1234/');

ws.onopen = function (event) {
  console.log('JS ONOPEN', JSON.stringify(event));
};
ws.onclose = function(event) {
  console.log('JS ONCLOSE', JSON.stringify(event))
};
ws.onerror = function(event) {
  console.log('JS ONERROR', JSON.stringify(event));
};
ws.onmessage = function(event) {
  console.log('JS ONMESSAGE', JSON.stringify(event));
};

ws.send('hello!');
ws.send(new Uint8Array([1, 2, 3, 151, 10]));
ws.send('World');
*/


const doc = new Y.Doc();
const wsProvider = new WebsocketProvider('ws://[::1]:1234', 'my-roomname', doc);
wsProvider.on('status', event => {
  print(event.status);
});

const yarray = doc.getArray('my-array');
yarray.observe(event => {
  console.log('yarray was modified', JSON.stringify(event.target));
});
// every time a local or remote client modifies yarray, the observer is called
yarray.insert(0, ['val']);
