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
exports.onBookingStatusChange = functions.firestore
    .document('bookings/{bookingId}')
    .onUpdate(async (change, context) => {
        const bookingData = change.after.data();
        const bookingId = context.params.bookingId;

        //okay nani
        // Check if the status has changed to 'Confirmed'
        if (bookingData.status === 'Surveyor Confirmed Waiting for quotation') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            // Retrieve the client's FCM token (since client needs to be notified)
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The surveyor has confirmed the booking request. Waiting for quotation.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Surveyor_Confirmed_Waiting_for_quotation');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        //mana ni
        } else if (bookingData.status === 'Waiting for landowners confirmation') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The surveyor has entered the quotation. Please check it.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Waiting_for_landowners_confirmation');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        } else if (bookingData.status === 'pending_payment') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const surveyorDoc = await db.collection('users').doc(bookedUserId).get();
            const bookedFcmToken = surveyorDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The landowner has confirmed. Waiting for payment.`;
        
            // Send the notification to the client
            if (bookedFcmToken) {
                await sendNotification(bookedFcmToken, title, message, bookedUserId, landOwnerUserId, 'pending_payment');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        } else if (bookingData.status === 'payment_submitted') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const surveyorDoc = await db.collection('users').doc(bookedUserId).get();
            const bookedFcmToken = surveyorDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The landowner has paid. Please confirm.`;
        
            // Send the notification to the client
            if (bookedFcmToken) {
                await sendNotification(bookedFcmToken, title, message, bookedUserId, landOwnerUserId, 'payment_submitted');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        }
        else if (bookingData.status === 'professional edit details') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The surveyor has edited the details. Please confirm.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'professional edit details');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        } 
        else if (bookingData.status === 'landowner edit details') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const surveyorDoc = await db.collection('users').doc(bookedUserId).get();
            const bookedFcmToken = surveyorDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The landowner has edited the details. Please confirm.`;
        
            // Send the notification to the client
            if (bookedFcmToken) {
                await sendNotification(bookedFcmToken, title, message, bookedUserId, landOwnerUserId, 'landowner edit details');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        } else if (bookingData.status === 'Surveyor Confirmed') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The surveyor has confirmed. Please verify.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Surveyor Confirmed');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        } 
        else if (bookingData.status === 'processor edit details') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The processor has edited the details. Please confirm.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'processor edit details');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        } 
        else if (bookingData.status === 'landowner edit detail') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const surveyorDoc = await db.collection('users').doc(bookedUserId).get();
            const bookedFcmToken = surveyorDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The landowner has edited the details. Please confirm.`;
        
            // Send the notification to the client
            if (bookedFcmToken) {
                await sendNotification(bookedFcmToken, title, message, bookedUserId, landOwnerUserId, 'landowner edit detail');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        }

        else if (bookingData.status === 'Waiting for processor document verification') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            // Fetch the surveyor's FCM token (booked user)
            const surveyorDoc = await db.collection('users').doc(bookedUserId).get();
            const bookedFcmToken = surveyorDoc.data()?.fcmToken;
        
            // Fetch the landowner's FCM token
            const landOwnerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landOwnerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The booking has been confirmed, and we are now waiting for processor verification.`;
        
            // Send the notification to the booked user (surveyor)
            if (bookedFcmToken) {
                await sendNotification(bookedFcmToken, title, message, bookedUserId, landOwnerUserId, 'verification');
            } else {
                console.error('FCM token not found for booked user:', bookedUserId);
            }
        
            // Send the notification to the landowner
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'verification');
            } else {
                console.error('FCM token not found for landowner:', landOwnerUserId);
            }
        }
        else if (bookingData.status === 'Verified') {
            const landOwnerUserId = bookingData.landOwnerUserId;
            const bookedUserId = bookingData.bookedUserId; 
        
            
            const landownerDoc = await db.collection('users').doc(landOwnerUserId).get();
            const landOwnerFcmToken = landownerDoc.data()?.fcmToken;
        
            // Prepare the notification message
            const title = 'Booking Confirmed';
            const message = `The processor has verified the document. Please submit it to the office.`;
        
            // Send the notification to the client
            if (landOwnerFcmToken) {
                await sendNotification(landOwnerFcmToken, title, message, landOwnerUserId, bookedUserId, 'Verified');
            } else {
                console.error('FCM token not found for landowner:', landOwnerFcmToken);
            }
        
        }
        
    });