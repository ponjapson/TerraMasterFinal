const  functions = require('firebase-functions');
const admin = require('./firebaseAdmin');
const {sendNotification} = require('./notification');

exports.onVerifiedUserNotification = functions.firestore
    .document('users/{uid}')
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();

        if(before.status != 'Verified' && after.status == 'Verified'){

            const fcmToken = after.fcmToken;
            const notificationMessage = `Your account has been successfully verified. You can now access all features.`;
            const userId = after.uid

            if(fcmToken){
                try{
                    await sendNotification(
                        fcmToken,
                        'Account Verification',
                        notificationMessage, 
                        userId,
                        'System admin',
                        'Verification'
                    )
                }catch (error) {
                    console.error('Failed to send verification notification:', error);
                }
            }
        }
    })