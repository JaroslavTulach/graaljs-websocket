const wss = new WebSocketServer({ host: 'localhost', port: 1234 })
const setupWSConnection = require('./utils.js').setupWSConnection

wss.onconnect = (socket, url) => setupWSConnection(socket, url)

wss.start()
