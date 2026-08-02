# User Registration And SMS Verification Design

Date: 2026-08-02

## Goal

Add an explicit normal-user registration flow. A user registers with a phone number, verification code, and nickname. Successful registration creates one row in the existing `users` table and returns a JWT so the user is logged in immediately.

The verification-code implementation supports two providers:

- `mock`: local development mode. The backend generates and stores a real expiring code and also returns it in the API response so the flow can be tested without an SMS account.
- `aliyun`: real SMS mode. The backend sends the code through Alibaba Cloud SMS and never returns the code to the frontend.

Changing between providers is configuration-only. The registration and login business logic does not depend on a provider implementation.

## Scope

Included:

- Explicit phone registration endpoint and frontend form.
- Registration verification-code endpoint.
- Database-backed verification-code lifecycle.
- Existing phone login changed to require an existing registered user.
- Login verification codes use the same lifecycle as registration codes.
- Mock and Alibaba Cloud SMS sender implementations.
- Focused backend tests and frontend build verification.

Excluded:

- Password login.
- Real-name identity verification.
- CAPTCHA integration.
- Redis. MySQL is sufficient for the current graduation-project traffic level.
- Changes to administrator authentication.

## User Experience

The login page contains three tabs:

1. `手机号登录`
2. `用户注册`
3. `管理员登录`

Registration fields:

- Phone number.
- Verification code.
- Nickname.

The current scope accepts mainland China mobile numbers in the standard 11-digit format. International phone-number registration is outside this change.

The user requests a registration code, enters the code and nickname, and submits. On success, the frontend stores the returned JWT using the existing Pinia auth store and navigates to the homepage.

Login remains phone plus verification code. An unregistered phone is rejected with a clear message directing the user to register first. Existing rows in `users` are treated as registered accounts.

## API Design

### Request Registration Code

```http
POST /api/auth/user/register/code
Content-Type: application/json

{
  "phone": "13800138000"
}
```

The response uses the existing `PhoneCodeResponse`. In `mock` mode, `data.code` contains the generated code. In `aliyun` mode, `data.code` is null or empty.

### Register User

```http
POST /api/auth/user/register
Content-Type: application/json

{
  "phone": "13800138000",
  "code": "123456",
  "nickname": "小厨"
}
```

The response uses the existing `AuthResponse` and contains the new user ID, display name, role, and JWT.

### Existing Login Endpoints

`POST /api/auth/user/code` issues a login-purpose code only when the phone already exists and is enabled.

`POST /api/auth/user/login` verifies a login-purpose code and no longer creates a user automatically.

All four normal-user authentication endpoints remain public in Spring Security.

## Verification-Code Persistence

Add a Flyway migration for `phone_verification_codes`:

```text
id                BIGINT primary key
phone             VARCHAR(32), indexed
purpose           VARCHAR(32): REGISTER or LOGIN
code_hash         VARCHAR(255)
expires_at        DATETIME
attempt_count     INT default 0
max_attempts      INT default 5
consumed_at       DATETIME nullable
created_at        DATETIME
```

Only a hash of the code is stored. The latest unconsumed code for the same phone and purpose is used for verification.

Rules:

- Code length: six numeric digits.
- Expiry: five minutes.
- Resend interval: sixty seconds for the same phone and purpose.
- Maximum verification attempts: five.
- Issuing a new code invalidates every older unconsumed code for the same phone and purpose.
- A successful verification sets `consumed_at` and the code cannot be reused.
- Registration codes and login codes cannot be used interchangeably.

## Backend Components

`VerificationCodeService` owns code generation, rate checks, persistence, verification attempts, expiry, and consumption.

Code issuance first validates the account rule and resend interval, then calls the active sender. A successful send invalidates older codes and stores the new hash. A provider failure does not create a usable verification-code row.

`SmsCodeSender` is the provider boundary:

```text
SmsCodeSender
  MockSmsCodeSender
  AliyunSmsCodeSender
```

`MockSmsCodeSender` does not contact an external service. `AliyunSmsCodeSender` uses the official Alibaba Cloud SMS Java SDK and is created only when `app.sms.provider=aliyun`.

`AuthService` owns registration and login account rules:

- Register rejects an existing phone.
- Register consumes a valid `REGISTER` code and inserts the user in one transaction.
- Login rejects a missing or disabled user.
- Login consumes a valid `LOGIN` code and updates `last_login_at`.

The existing unique constraint on `users.phone` remains the final concurrency guard against duplicate registration.

## Configuration

Default local configuration:

```yaml
app:
  sms:
    provider: ${SMS_PROVIDER:mock}
    code-expiry: 5m
    resend-interval: 60s
    max-attempts: 5
```

Alibaba Cloud mode additionally reads:

```text
ALIBABA_CLOUD_ACCESS_KEY_ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET
ALIYUN_SMS_SIGN_NAME
ALIYUN_SMS_TEMPLATE_CODE
```

Credentials are never placed in source code, committed configuration, API responses, or logs. The local backend can call Alibaba Cloud SMS over the public internet; deployment to an Alibaba Cloud server is not required.

## Error Handling

The API returns the existing response envelope and maps these cases to clear client messages:

- Phone already registered.
- Phone not registered.
- Verification code invalid.
- Verification code expired.
- Verification code already used.
- Too many verification attempts.
- Code requested too frequently.
- User account disabled.
- SMS provider unavailable or rejected the request.

The frontend maps these messages to Chinese notifications. It does not expose provider credentials or raw provider errors.

## Testing

Backend tests are written first and must demonstrate the red-green cycle for:

- Registration code issuance in mock mode.
- Successful registration writes exactly one user row and returns JWT.
- Duplicate registration is rejected.
- Login for an unregistered phone is rejected.
- Wrong, expired, consumed, purpose-mismatched, and over-attempted codes are rejected.
- Registration and login code resend limits are enforced.
- Disabled users cannot request a login code or log in.
- Public endpoint security rules remain correct.
- Alibaba Cloud sender configuration fails clearly when required values are missing.

Frontend verification covers the three-tab form behavior through implementation review and `npm run build`. No frontend test framework is added as part of this focused change.

Full backend verification:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

## Migration And Compatibility

Flyway creates only the new verification-code table. Existing users and administrator records are preserved. Existing user rows can immediately request login codes. The old fixed `MOCK_LOGIN_CODE` behavior is removed and replaced by generated database-backed codes in the same focused change.
