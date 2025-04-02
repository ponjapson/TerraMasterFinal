const functions = require('firebase-functions');
const admin = require("firebase-admin");

admin.initializeApp();

const messageNotification = require('./messageNotification');
const newBookingNotification = require('./newBookingNotification')
const createPaymentIntent = require('./createPaymentIntent')

exports.messageNotification = messageNotification.messageNotification;
exports.newBookingNotification = newBookingNotification.newBookingNotification;
exports.createPaymentIntent = createPaymentIntent.createPaymentIntent;
