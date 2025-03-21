const functions = require('firebase-functions');
const admin = require('./firebaseAdmin');
const { sendNotification } = require('./notification'); // Ensure notification.js is in the same directory


// Function to fetch user data including FCM token
async function getUserData(userId) {
    try {
        const userSnapshot = await admin.firestore().collection('users').doc(userId).get();
        if (userSnapshot.exists) {
            return userSnapshot.data();
        }
        console.error(`User with ID ${userId} not found.`);
        return null;
    } catch (error) {
        console.error('Error fetching user data:', error);
        return null;
    }
}

// Firestore trigger for new message creation
exports.messageNotification = functions.firestore
    .document('messages/{messageId}')
    .onCreate(async (snap, context) => {
        const messageData = snap.data();
        const { senderId, chatRoomId, text } = messageData;

        // Fetch chat room and participant data
        const chatRoom = await admin.firestore().collection('chats').doc(chatRoomId).get();
        if (!chatRoom.exists) {
            console.error('Chat room not found');
            return;
        }

        // Identify the recipient
        const participants = chatRoom.data().participants;
        const recipientId = participants.find(id => id !== senderId);
        if (!recipientId) {
            console.error('Recipient not found');
            return;
        }

        // Fetch sender and recipient data
        const senderData = await getUserData(senderId);
        const recipientData = await getUserData(recipientId);
        if (!senderData || !recipientData) return;

        const senderName = `${senderData.first_name} ${senderData.last_name}`;
        const recipientFcmToken = recipientData.fcmToken;

        // Notification message content
        const notificationMessage = `${senderName} sent you a message: ${text}`;

        // Send push notification if recipient's FCM token is available
        if (recipientFcmToken) {
            try {
                await sendNotification(
                    recipientFcmToken,
                    'New Message',
                    notificationMessage,
                    recipientId,
                    senderId,  
                    'message'
                );
                console.log(`Notification sent to ${recipientId}`);
            } catch (error) {
                console.error('Error sending notification:', error);
            }
        } else {
            console.error(`FCM token not found for user ${recipientId}.`);
        }

        // Optionally save notification record in Firestore
        try {
            await admin.firestore().collection('notifications').add({
                recipientId: recipientId,
                senderId: senderId,
                message: notificationMessage,
                type: 'message',
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                isRead: false
            });
            console.log(`Notification record saved for message from ${senderId} to ${recipientId}`);
        } catch (error) {
            console.error('Error saving notification:', error);
        }
    });