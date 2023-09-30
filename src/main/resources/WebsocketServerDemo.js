//import { WebSocketServer } from 'ws';

const wss = new WebSocketServer({ port: 8090 });

wss.on('connection', function connection(ws) {
  ws.on('error', console.error);

  ws.on('message', function message(data) {
    console.log(`received: ${data}`);
  });

  ws.send('something');
});
