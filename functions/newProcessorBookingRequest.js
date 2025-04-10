const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { sendNotification } = require('./notification');  // Assuming your functions are in notifications.js

if (!admin.apps.length) {
    admin.initializeApp();
} else {
    admin.app(); // Use the already initialized default app
}

// Firestore trigger to listen for new bookings
exports.newProcessorBookingRequest = functions.firestore
    .document('bookings/{bookingId}')
    .onCreate(async (snapshot, context) => {
        const bookingData = snapshot.data();
        const bookingId = context.params.bookingId;

        // Check if status is "new"
        if (bookingData.status === 'new processor request') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 

            try {
                // Retrieve the FCM token of the client
                const bookedDoc = await admin.firestore().collection('users').doc(bookedUserId).get();
                const bookedFcmToken = bookedDoc.data()?.fcmToken;

                if (bookedFcmToken) {
                    // Prepare the notification message
                    const title = 'New Booking Request';
                    const message = `You have a new booking request. Please check it out!`;

                    // Send the notification
                    await sendNotification(bookedFcmToken, title, message, bookedUserId, landOwnerUserId, 'booking_request');

                    
                } else {
                    console.error('FCM token not found for bookedUser:', bookedUserId);
                }

            } catch (error) {
                console.error(`Error handling new booking notification for booking ID ${bookingId}:`, error);
            }
        }
    });