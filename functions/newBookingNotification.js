const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { sendNotification } = require('./notification');  // Assuming your functions are in notifications.js

if (!admin.apps.length) {
    admin.initializeApp();
} else {
    admin.app(); // Use the already initialized default app
}

// Firestore trigger to listen for new bookings
exports.newBookingNotification = functions.firestore
    .document('bookings/{bookingId}')
    .onCreate(async (snapshot, context) => {
        const bookingData = snapshot.data();
        const bookingId = context.params.bookingId;

        // Check if status is "new"
        if (bookingData.status === 'new') {
            const landownerId = bookingData.landOwnerUserId; // Assuming landownerId is stored in the booking data
            const professionalId = bookingData.bookedUserId; // Assuming professionalId is stored in the booking data

            try {
                // Retrieve the FCM token of the professional
                const profesionalDoc = await admin.firestore().collection('users').doc(professionalId).get();
                const professionalFcmToken = profesionalDoc.data()?.fcmToken;

                if (professionalFcmToken) {
                    // Prepare the notification message
                    const title = 'New Booking Request';
                    const message = `You have a new booking request. Please check it out!`;

                    // Send the notification
                    await sendNotification(professionalFcmToken, title, message, professionalId, landownerId, 'booking_request');

                    
                } else {
                    console.error('FCM token not found for professional:', profesionalDoc);
                }

            } catch (error) {
                console.error(`Error handling new booking notification for booking ID ${bookingId}:`, error);
            }
        }
    });