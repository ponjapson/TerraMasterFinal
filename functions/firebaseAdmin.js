const admin = require('firebase-admin');

// Check if the app has already been initialized
if (!admin.apps.length) {
    admin.initializeApp();
}

module.exports = admin;
