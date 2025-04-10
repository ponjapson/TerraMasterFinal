const functions = require('firebase-functions');
const admin = require("firebase-admin");

admin.initializeApp();

const messageNotification = require('./messageNotification');
const createPaymentIntent = require('./createPaymentIntent');
const onVerifiedUserNotification = require('./onVerifiedUserNotification');
const newProcessorBookingRequest = require('./newProcessorBookingRequest');
const newSurveyorBookingRequest = require('./newSurveyorBookingRequest');
const onBookingStatusChange = require('./onBookingStatusChange');
const onBookingDocumentStatusChange = require('./onBookingDocumentStatusChange');

exports.messageNotification = messageNotification.messageNotification;
exports.createPaymentIntent = createPaymentIntent.createPaymentIntent;
exports.onVerifiedUserNotification = onVerifiedUserNotification.onVerifiedUserNotification;
exports.newProcessorBookingRequest = newProcessorBookingRequest.newProcessorBookingRequest;
exports.newSurveyorBookingRequest = newSurveyorBookingRequest.newSurveyorBookingRequest;
exports.onBookingStatusChange = onBookingStatusChange.onBookingStatusChange;
exports.onBookingDocumentStatusChange = onBookingDocumentStatusChange.onBookingDocumentStatusChange;
