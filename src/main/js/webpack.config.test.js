const path = require('path');

module.exports = {
  mode: 'none',
  entry: './all.test.js',
  output: {
    filename: 'all-tests.js',
    path: path.resolve(__dirname, '../../../target/test-classes'),
  },
};
