const admin = require('./firebaseAdmin'); // Import from firebaseAdmin.js
const db = admin.firestore(); // Ensure Firestore is initialized


// Function to save notification to Firestore
async function saveNotification(recipientId, senderId, message, type) {
    try {
        const notificationRef = db.collection('notifications').doc(); // Auto-generate notificationId
        const notificationData = {
            notificationId: notificationRef.id,
            recipientId: recipientId, // User ID of the recipient
            senderId: senderId,       // User ID of the sender
            message: message,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false, // Default value indicating the notification hasn't been read
            type: type
        };

        await notificationRef.set(notificationData);
        console.log(`Notification saved for recipient ID: ${recipientId}`);
    } catch (error) {
        console.error(`Error saving notification for recipient ID ${recipientId}: ${error.message}`);
    }
}

// Function to send the notification via Firebase Cloud Messaging
const sendNotification = async (fcmToken, title, message, recipientId, senderId, type) => {
    const payload = {
        notification: {
            title: title,
            body: message,
        },
        token: fcmToken,
    };

    try {
        // Send the notification
        await admin.messaging().send(payload);
        console.info(`Notification sent to ${fcmToken}: ${title}`);

        // Call saveNotification to save the notification after sending
        await saveNotification(recipientId, senderId, message, type); // Save notification in Firestore
    } catch (error) {
        console.error(`Error sending notification to ${fcmToken}: ${error.message}`);
    }
};

// Export both functions for global use
module.exports = { sendNotification, saveNotification };