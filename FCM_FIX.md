# Fix FCM (experimental)

- Controls: `switchFcmFix`, `persist.hchen.fcm.fix.enable`, default true.
- System scope only; boot hook on AMS.finishBooting. No GMS scope addition required.
- Primary/owner Android user only. Work/secondary profiles are not covered.
- At most one package-targeted MCS_HEARTBEAT request every 5 minutes, using monotonic time.
- Inexact idle-aware wakeup alarms can be delayed by Android/OEM policies. No busy loop,
  permanent wakelock, new app permission, token reset, GMS kill, network toggle, whitelist or AppOps writes.
- OFF cancels its own pending alarm when the root-authorized configuration broadcast arrives.
  Every tick also rereads the property; failed property writes preserve the UI choice for retry.
- Skip no validated network, absent/disabled/force-stopped GMS, and GMS on the user restriction list.
- No public cross-version FCM connected-state API is assumed. The heartbeat action is undocumented;
  GMS may ignore it, and a successful sendBroadcast is not proof of delivery or reconnect.
- Initial deployment requires installing APK, enabling system scope and rebooting.
  Then toggles are live when property synchronization succeeds; broadcast failure may require reboot.
- No claim of 0.1% battery overhead or universal instant notifications.

## Device acceptance (not yet executed)

Compare ON/OFF with the same ROM, GMS version, Wi-Fi/mobile and screen-off state for 40-60 min.
Inspect GMS diagnostics (*#*#426#*#* where supported), timestamps of sent/received high-priority
and normal messages, system/module logs, and battery wakeups. Test OFF cancellation, reboot
persistence, airplane mode, absent/disabled/force-stopped GMS, restriction-list precedence,
and UI closed. Separate heartbeat-request logs from actual GMS reconnect/delivery evidence.

Sources: https://firebase.google.com/docs/cloud-messaging/android-message-priority
https://developer.android.com/reference/android/app/AlarmManager
https://github.com/E3N-glotm/HyperOS-Millet-Guard (author-reported native MCS heartbeat behavior;
not a compatibility guarantee for this module/device).

The configuration app has no FCM worker, service, receiver, job or alarm. All periodic
FCM work belongs to the Xposed hook in system_server, even after the UI exits.
