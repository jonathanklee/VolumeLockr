# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.7.x   | :white_check_mark: |
| < 1.7   | :x:                |

## Reporting a Vulnerability

VolumeLockr is made for the Android platform and does not use any network permissions.
However, if you discover a security vulnerability, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

Contact the maintainer directly via:
- Email: See the maintainer's GitHub profile for contact information
- GitHub: Report via [private vulnerability reporting](https://github.com/xunnv/VolumeLockr/security/advisories/new)

### What to include

- Description of the vulnerability
- Steps to reproduce
- Affected version(s)
- Potential impact
- Suggested fix (if any)

### Response timeline

- Acknowledgment within 48 hours
- Status update within 7 days
- Resolution timeline depends on severity

## Security Model

VolumeLockr operates with the following permissions:
- **Notification access**: Required for the foreground service notification
- **Do Not Disturb access**: Required for ringer mode detection
- **RECEIVE_BOOT_COMPLETED**: Required for auto-start on boot

No network, storage, or background location permissions are used. The app does not collect, transmit, or store any user data off-device.