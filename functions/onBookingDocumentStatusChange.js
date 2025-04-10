const admin = require('firebase-admin');
const functions = require('firebase-functions');
const { sendNotification } = require('./notification'); 

if (!admin.apps.length) {
    admin.initializeApp();
} else {
    admin.app(); // Use the already initialized default app
}

const db = admin.firestore();

// Firestore trigger to watch the bookings collection for status updates
exports.onBookingDocumentStatusChange = functions.firestore
    .document('bookings/{bookingId}')
    .onUpdate(async (change, context) => {
        const bookingData = change.after.data();
        const bookingId = context.params.bookingId;

        // Check if the status has changed to 'Confirmed'
        if (bookingData.documentStatus === 'Prepare Blueprint') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            // Retrieve the client's FCM token (since client needs to be notified)
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Document Progress';
            const message = `Please prepare the blueprint for the next step in the process.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Prepare Blueprint');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        }

        if (bookingData.documentStatus === 'Submit Blueprint') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            // Retrieve the client's FCM token (since client needs to be notified)
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Document Progress';
            const message = `You can now pass the blueprint for the next step in the process.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Submit Blueprint');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        }
        if (bookingData.documentStatus === 'Follow-up Approval') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            // Retrieve the client's FCM token (since client needs to be notified)
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Document Progress';
            const message = `The document is now for follow-up approval`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Follow-up Approval');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        }
        if (bookingData.documentStatus === 'Ready to Claim') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            // Retrieve the client's FCM token (since client needs to be notified)
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Document Progress';
            const message = `The document is now ready for you to claim.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Ready to Claim');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        }
    });