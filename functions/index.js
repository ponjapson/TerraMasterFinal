const functions = require('firebase-functions');
const admin = require("firebase-admin");

admin.initializeApp();

const messageNotification = require('./messageNotification');

exports.messageNotification = messageNotification.messageNotification;
