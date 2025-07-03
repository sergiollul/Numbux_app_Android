const admin = require("firebase-admin");
admin.initializeApp();

const {onValueUpdated} = require("firebase-functions/v2/database");

exports.syncBlockingToggle = onValueUpdated(
    {
      ref: "/rooms/{roomId}/blocking_enabled",
      region: "europe-west1",
    },
    async (event) => {
      const newVal = event.data.after.val();
      const roomId = event.params.roomId;

      const payload = {
        data: {
          toggle: String(newVal),
          roomId: roomId,
        },
        android: {
          priority: "high",
        },
        apns: {
          headers: {
            "apns-push-type": "background",
            "apns-priority": "5",
          },
          payload: {
            aps: {"content-available": 1},
          },
        },
      };

      return admin.messaging().sendToTopic(
          `blocking-updates-${roomId}`,
          payload,
      );
    },
);
