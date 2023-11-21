const path = require('path');

module.exports = {
  mode: 'none',
  entry: './all.js',
  output: {
    filename: 'all-y-websocket.js',
    path: path.resolve(__dirname, '../../../target/classes'),
  },
};
