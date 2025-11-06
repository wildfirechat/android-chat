# Push Notification Instructions
1. Currently integrated with Xiaomi, Huawei, and Meizu push services. Developers can integrate other push services on their own. On Xiaomi, Huawei, and Meizu phones, each uses their own push service; other phones use Xiaomi push.
2. The keys in the project are for testing purposes only. Developers need to apply for their own keys.

## Xiaomi Push
1. Xiaomi supports pass-through push and notification bar push. Currently, VoIP-related messages use pass-through push, and other messages use notification bar push. The differences between pass-through and notification bar push are as follows:

  |            | Pass-through Push                       | Notification Bar Push                   |
  | ---------- | ------------------------------ | ---------------------------- |
  | Allow Auto-start | No notification in notification bar, but wakes up app   | Notification appears in notification bar and wakes up app   |
  | Prohibit Auto-start | No notification in notification bar, and doesn't wake up app | Notification appears in notification bar, but doesn't wake up app |

2. When auto-start is allowed and notification bar push is used, notifications may appear duplicated in the notification bar

## Huawei Push

todo

## Meizu Push

todo


