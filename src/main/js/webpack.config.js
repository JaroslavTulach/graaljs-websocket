const path = require('path');

module.exports = {
  mode: 'none',
  entry: './server.js',
  output: {
    filename: 'all-y-websocket.js',
    path: path.resolve(__dirname, '../../../target/classes'),
  },
};
