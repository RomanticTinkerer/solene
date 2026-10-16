*Bluetooth or Local Network. Must setup and link a device beforehand for sharing.*
  
Use Syncthing's protocol for syncing data. Make two profiles on startup (first setup) for "Who are you?"

- Male (For Cycle sharing)
- Female (Cycle Tracking)

(gender data is only for application feature availability purposes, it is not stored. We are not trying to offend any gender groups, thank you.)

---

# Developed plan (October 2026)

## What exists

The You tab has a "Partner Links" card that says "Coming soon". Nothing behind it.

## First-launch choice

The note above proposes Male / Female profiles, used only to decide which features appear. Suggestion: label the choice by **role** instead:

- **"I track my cycle"** → the full app.
- **"I follow a partner's cycle"** → partner mode.

The outcome is the same, and gender is still never stored. It also works for same-sex couples and for trans and non-binary users, so the disclaimer isn't needed. The choice can be changed later in Settings.

## Partner mode

A read-only view of whatever the tracker shares:

- cycle day and phase;
- next period estimate;
- fertile window (only if shared);
- selected symptoms, e.g. "cramps today" (only if shared);
- pregnancy week and summaries when pregnancy mode is on ([[Pregnancy Tracking (Detailed)]]).

Partner mode has no logging, predictions, or reminders of its own, except an optional "period expected in 3 days" notification from the shared estimate.

## Transport: borrow Syncthing's ideas, not its protocol

Syncthing's Block Exchange Protocol is built for syncing folders between many devices: block hashing, global discovery, relays, conflict resolution. Solene needs a one-way push of a snapshot of a few hundred bytes between two paired phones. Implementing the protocol would be most of the work for none of the need.

Take its two core ideas:

- **Device identity** = a key pair made on first use and kept in Android Keystore. The device ID is a hash of the public key.
- **Pairing**: both sides pin each other's ID. No accounts, no server.

### v0: QR snapshot (no networking at all)

1. The tracker taps "Share" and the phone shows a QR code containing the encrypted snapshot.
2. The partner scans it and partner mode updates.

This works anywhere, with no Wi-Fi, Bluetooth, or background service. It needs ZXing (Apache-2.0) to generate and decode. Avoid ML Kit, which is not FOSS and would block F-Droid.

Ship this first. Upgrade only when re-scanning gets annoying.

### v1: local network sync

- **Pairing:** scan a QR code once. It carries the device ID plus a one-time secret, and both sides store each other's public key.
- **Discovery:** Android's built-in NSD (mDNS) on the same Wi-Fi.
- **Connection:** mutual TLS with the pinned keys. The tracker pushes the latest snapshot, and the partner keeps only the latest one, encrypted at rest.
- **When:** whenever either app opens and the other is reachable, plus a "Sync now" button. No background service in v1.
- **Bluetooth:** only if local Wi-Fi proves unreliable in practice. BLE GATT is more code and flakier.

### Snapshot format

```json
{
  "v": 1,
  "generatedAt": "2026-10-09T18:00:00+05:30",
  "cycleDay": 12,
  "phase": "follicular",
  "nextPeriod": "2026-10-25",
  "fertileWindow": null,
  "symptoms": []
}
```

Fields the tracker didn't share are `null` or empty, never sent.

## Privacy and safety

- Sharing is set up and revoked **only on the tracker's phone**. The partner can't request data, change what's shared, or see history beyond the latest snapshot.
- **Per-field toggles.** The default shares only the next period estimate and the phase.
- Notes are never shared.
- A visible "Shared with {name}" indicator sits on the tracker's Home while sharing is on.
- **Revoking** deletes the pairing keys, so no further syncs happen. The UI says honestly that what was already shared stays on the partner's phone until replaced or removed.
- **Coercion risk:** partner tracking can be abused in controlling relationships. Keep it easy to turn off, never hidden, and never something the partner can switch on remotely.

## Milestones

1. Role choice at onboarding, plus the partner-mode shell.
2. v0 QR snapshot share.
3. Pairing keys and local network sync.
4. Per-field toggles and revoke.
5. Pregnancy fields.

## Risks

- **Multicast blocked:** many routers and guest networks block multicast, so NSD finds nothing. Keep the QR path as the fallback.
- **Reinstall:** reinstalling loses the Keystore key, so the devices must re-pair. Say so in the UI.
- **Version skew:** the snapshot carries `"v"`. A partner app that sees an unknown version shows "update Solene" instead of guessing.

