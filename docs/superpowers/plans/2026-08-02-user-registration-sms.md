# User Registration And SMS Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add explicit phone registration that persists users to MySQL, replace the fixed mock login code with expiring database-backed codes, and support configuration-based mock or Alibaba Cloud SMS delivery.

**Architecture:** A focused `verification` package owns code generation, hashing, persistence, expiry, attempts, and SMS delivery. `AuthService` keeps account rules and calls the verification service for `REGISTER` and `LOGIN` purposes. The Vue login page adds a registration tab and continues to store the returned JWT through the existing auth store.

**Tech Stack:** Java 17, Spring Boot 3.3.6, MyBatis Plus, Flyway, MySQL/H2, Spring Security, Alibaba Cloud SMS Java SDK 4.6.0, JUnit 5, MockMvc, Vue 3, Element Plus, Axios.

**Git rule:** Do not create any implementation commit or push unless the user explicitly approves it. Commit commands below are checkpoints to run only after that approval.

---

## File Map

Create:

- `backend/src/main/resources/db/migration/V4__add_phone_verification_codes.sql`: verification-code persistence schema.
- `backend/src/main/java/com/example/food/auth/dto/PhoneRegistrationRequest.java`: registration request validation.
- `backend/src/main/java/com/example/food/auth/verification/VerificationCodePurpose.java`: `REGISTER` and `LOGIN` purpose enum.
- `backend/src/main/java/com/example/food/auth/verification/PhoneVerificationCode.java`: MyBatis entity.
- `backend/src/main/java/com/example/food/auth/verification/PhoneVerificationCodeMapper.java`: database access.
- `backend/src/main/java/com/example/food/auth/verification/SmsProperties.java`: provider, timing, and Alibaba Cloud configuration.
- `backend/src/main/java/com/example/food/auth/verification/SmsSendResult.java`: provider result with optional mock code.
- `backend/src/main/java/com/example/food/auth/verification/SmsCodeSender.java`: sender interface.
- `backend/src/main/java/com/example/food/auth/verification/MockSmsCodeSender.java`: local sender.
- `backend/src/main/java/com/example/food/auth/verification/AliyunSmsCodeSender.java`: real sender using the official SDK.
- `backend/src/main/java/com/example/food/auth/verification/VerificationCodeService.java`: lifecycle service.
- `backend/src/test/java/com/example/food/auth/verification/VerificationCodeServiceTest.java`: database-backed lifecycle tests.
- `backend/src/test/java/com/example/food/auth/verification/AliyunSmsCodeSenderTest.java`: configuration validation tests.

Modify:

- `backend/pom.xml`: add Alibaba Cloud SMS SDK.
- `backend/src/main/resources/application.yml`: replace fixed mock code with `app.sms` configuration.
- `backend/src/test/resources/application-test.yml`: deterministic mock-provider test configuration.
- `backend/src/main/java/com/example/food/auth/dto/PhoneCodeRequest.java`: mainland phone validation.
- `backend/src/main/java/com/example/food/auth/dto/PhoneLoginRequest.java`: mainland phone and six-digit code validation.
- `backend/src/main/java/com/example/food/auth/AuthService.java`: explicit registration and existing-user login rules.
- `backend/src/main/java/com/example/food/auth/AuthController.java`: registration endpoints and database-backed code issuance.
- `backend/src/main/java/com/example/food/security/SecurityConfig.java`: permit the two registration endpoints.
- `backend/src/test/java/com/example/food/auth/AuthControllerTest.java`: registration persistence and login boundary tests.
- `backend/src/test/java/com/example/food/auth/AuthServiceTest.java`: adjust constructor and account-rule tests.
- `backend/src/test/java/com/example/food/auth/AuthSecurityTest.java`: public endpoint coverage.
- `frontend/src/api/auth.js`: registration API methods.
- `frontend/src/views/LoginView.vue`: registration tab, code requests, countdowns, and Chinese messages.
- `README.md`: local mock and future Alibaba Cloud configuration.

Do not modify the unrelated existing whitespace change in `backend/src/main/java/com/example/food/FoodApplication.java`.

---

### Task 1: Add Verification-Code Schema And Mapping

**Files:**

- Create: `backend/src/main/resources/db/migration/V4__add_phone_verification_codes.sql`
- Create: `backend/src/main/java/com/example/food/auth/verification/VerificationCodePurpose.java`
- Create: `backend/src/main/java/com/example/food/auth/verification/PhoneVerificationCode.java`
- Create: `backend/src/main/java/com/example/food/auth/verification/PhoneVerificationCodeMapper.java`
- Test: `backend/src/test/java/com/example/food/FoodApplicationTest.java`

- [ ] **Step 1: Add a failing migration assertion**

Extend `FoodApplicationTest` to inject `JdbcTemplate` and assert the table exists:

```java
@Autowired
private JdbcTemplate jdbcTemplate;

@Test
void phoneVerificationCodeTableIsCreated() {
    Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'phone_verification_codes'",
            Integer.class
    );
    assertThat(count).isEqualTo(1);
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml -Dtest=FoodApplicationTest test
```

Expected: FAIL because `phone_verification_codes` does not exist.

- [ ] **Step 3: Add migration and mapping types**

Create `V4__add_phone_verification_codes.sql`:

```sql
CREATE TABLE phone_verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    consumed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phone_verification_codes_lookup
    ON phone_verification_codes(phone, purpose, created_at);
```

Create `VerificationCodePurpose`:

```java
package com.example.food.auth.verification;

public enum VerificationCodePurpose {
    REGISTER,
    LOGIN
}
```

Create the entity and mapper with these exact fields and signatures:

```java
package com.example.food.auth.verification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("phone_verification_codes")
public class PhoneVerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String purpose;
    private String codeHash;
    private LocalDateTime expiresAt;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

```java
package com.example.food.auth.verification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PhoneVerificationCodeMapper extends BaseMapper<PhoneVerificationCode> {
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Task 1 command again. Expected: PASS.

- [ ] **Step 5: Review checkpoint**

Run `git diff --check` and verify only Task 1 files plus the pre-existing `FoodApplication.java` user change appear. Commit only after explicit approval.

---

### Task 2: Implement Database-Backed Verification Lifecycle In Mock Mode

**Files:**

- Create: `backend/src/main/java/com/example/food/auth/verification/SmsProperties.java`
- Create: `backend/src/main/java/com/example/food/auth/verification/SmsSendResult.java`
- Create: `backend/src/main/java/com/example/food/auth/verification/SmsCodeSender.java`
- Create: `backend/src/main/java/com/example/food/auth/verification/MockSmsCodeSender.java`
- Create: `backend/src/main/java/com/example/food/auth/verification/VerificationCodeService.java`
- Create: `backend/src/test/java/com/example/food/auth/verification/VerificationCodeServiceTest.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Write failing lifecycle tests**

Use `@SpringBootTest`, `@ActiveProfiles("test")`, the real mapper, and the configured mock sender. Cover these concrete cases:

```java
@Test
void issuedMockCodeCanBeConsumedOnce() {
    String code = service.issue("13900001001", VerificationCodePurpose.REGISTER).code();
    assertThat(code).matches("\\d{6}");

    service.verify("13900001001", VerificationCodePurpose.REGISTER, code);

    assertThatThrownBy(() -> service.verify("13900001001", VerificationCodePurpose.REGISTER, code))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Verification code already used");
}

@Test
void wrongPurposeCannotConsumeCode() {
    String code = service.issue("13900001002", VerificationCodePurpose.REGISTER).code();
    assertThatThrownBy(() -> service.verify("13900001002", VerificationCodePurpose.LOGIN, code))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Verification code invalid");
}
```

Also add tests that directly update the stored row to an expired time, request a second code inside the resend interval, enter five wrong codes, and verify a newly issued code invalidates the previous row.

- [ ] **Step 2: Run tests and verify RED**

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml -Dtest=VerificationCodeServiceTest test
```

Expected: test compilation fails because verification classes do not exist.

- [ ] **Step 3: Add configuration and sender boundary**

`SmsProperties` is a `@Component` and `@ConfigurationProperties(prefix = "app.sms")` with defaults:

```java
private String provider = "mock";
private Duration codeExpiry = Duration.ofMinutes(5);
private Duration resendInterval = Duration.ofSeconds(60);
private int maxAttempts = 5;
private Aliyun aliyun = new Aliyun();
```

The nested `Aliyun` properties are `accessKeyId`, `accessKeySecret`, `signName`, `templateCode`, and `endpoint` defaulting to `dysmsapi.aliyuncs.com`.

Create:

```java
public record SmsSendResult(String code) {
}

public interface SmsCodeSender {
    SmsSendResult send(String phone, String code);
}
```

`MockSmsCodeSender` uses:

```java
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsCodeSender implements SmsCodeSender {
    @Override
    public SmsSendResult send(String phone, String code) {
        return new SmsSendResult(code);
    }
}
```

- [ ] **Step 4: Implement minimal lifecycle service**

`issue(phone, purpose)` must:

1. Query the newest row for phone and purpose.
2. Reject when `createdAt + resendInterval` is after now.
3. Generate a six-digit code with `SecureRandom`.
4. Call `smsCodeSender.send(phone, code)`.
5. Mark every older unconsumed row for phone and purpose as consumed.
6. Insert a new row with `passwordEncoder.encode(code)`, expiry, zero attempts, and configured max attempts.
7. Return the sender's `SmsSendResult`.

`verify(phone, purpose, submittedCode)` must query the newest row for the exact purpose, then check in this order: missing, consumed, expired, attempts exhausted, hash match. A wrong hash increments `attempt_count`; a correct hash sets `consumed_at`.

Use exact exception messages from the design:

```text
Verification code invalid
Verification code already used
Verification code expired
Too many verification attempts
Code requested too frequently
```

- [ ] **Step 5: Replace fixed-code configuration**

Remove `app.auth.mock-code` from main and test YAML. Add:

```yaml
app:
  sms:
    provider: ${SMS_PROVIDER:mock}
    code-expiry: ${SMS_CODE_EXPIRY:5m}
    resend-interval: ${SMS_RESEND_INTERVAL:60s}
    max-attempts: ${SMS_MAX_ATTEMPTS:5}
    aliyun:
      access-key-id: ${ALIBABA_CLOUD_ACCESS_KEY_ID:}
      access-key-secret: ${ALIBABA_CLOUD_ACCESS_KEY_SECRET:}
      sign-name: ${ALIYUN_SMS_SIGN_NAME:}
      template-code: ${ALIYUN_SMS_TEMPLATE_CODE:}
      endpoint: ${ALIYUN_SMS_ENDPOINT:dysmsapi.aliyuncs.com}
```

The test profile uses `provider: mock`, `resend-interval: 60s`, and the same expiry/attempt defaults.

- [ ] **Step 6: Run lifecycle tests and verify GREEN**

Run the Task 2 command. Expected: all lifecycle tests PASS.

- [ ] **Step 7: Review checkpoint**

Inspect the diff for plaintext code persistence, code logging, broad refactors, and unbounded queries. Commit only after explicit approval.

---

### Task 3: Add Explicit Registration And Change Login Account Rules

**Files:**

- Create: `backend/src/main/java/com/example/food/auth/dto/PhoneRegistrationRequest.java`
- Modify: `backend/src/main/java/com/example/food/auth/dto/PhoneCodeRequest.java`
- Modify: `backend/src/main/java/com/example/food/auth/dto/PhoneLoginRequest.java`
- Modify: `backend/src/main/java/com/example/food/auth/AuthService.java`
- Modify: `backend/src/test/java/com/example/food/auth/AuthServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Add tests proving:

```java
@Test
void registerUserPersistsNicknameAndReturnsJwt() {
    String code = authService.issueRegistrationCode("13900002001").code();
    AuthResponse response = authService.registerUser(
            new PhoneRegistrationRequest("13900002001", code, "测试用户")
    );

    User stored = userMapper.selectOne(new QueryWrapper<User>().eq("phone", "13900002001"));
    assertThat(stored).isNotNull();
    assertThat(stored.getNickname()).isEqualTo("测试用户");
    assertThat(response.token()).isNotBlank();
}

@Test
void unknownPhoneCannotRequestLoginCode() {
    assertThatThrownBy(() -> authService.issueLoginCode("13900002002"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not registered");
}
```

Also cover duplicate registration and disabled-user login-code requests.

- [ ] **Step 2: Run tests and verify RED**

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml -Dtest=AuthServiceTest test
```

Expected: FAIL because registration methods and DTO do not exist.

- [ ] **Step 3: Add strict request validation**

Use `@Pattern(regexp = "^1[3-9]\\d{9}$")` for registration, code request, and login phone fields. Use `@Pattern(regexp = "^\\d{6}$")` for code fields. Create:

```java
public record PhoneRegistrationRequest(
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String code,
        @NotBlank @Size(max = 64) String nickname
) {
}
```

- [ ] **Step 4: Implement account rules in `AuthService`**

Replace the fixed `mockCode` dependency with `VerificationCodeService`.

Add:

```java
public SmsSendResult issueRegistrationCode(String phone)
public SmsSendResult issueLoginCode(String phone)
@Transactional(isolation = Isolation.READ_COMMITTED)
public AuthResponse registerUser(PhoneRegistrationRequest request)
```

Registration rejects an existing phone with `Phone already registered`, verifies `REGISTER`, inserts an enabled `USER` with timestamps, and returns the existing JWT response. Catch `DuplicateKeyException` and translate it to the same duplicate message.

Login first requires an existing enabled user, verifies `LOGIN`, updates `lastLoginAt`, and returns JWT. Remove the previous create-on-login path.

- [ ] **Step 5: Run service tests and verify GREEN**

Run the Task 3 command. Expected: PASS.

- [ ] **Step 6: Review checkpoint**

Confirm registration and code consumption are in one transaction and the unique phone constraint remains the concurrency guard. Commit only after explicit approval.

---

### Task 4: Expose Registration APIs And Security Rules

**Files:**

- Modify: `backend/src/main/java/com/example/food/auth/AuthController.java`
- Modify: `backend/src/main/java/com/example/food/security/SecurityConfig.java`
- Modify: `backend/src/test/java/com/example/food/auth/AuthControllerTest.java`
- Modify: `backend/src/test/java/com/example/food/auth/AuthSecurityTest.java`

- [ ] **Step 1: Write failing MockMvc tests**

Replace fixed-code assumptions with request-code-first flows. Add:

```java
@Test
void userCanRegisterAndDataIsSaved() throws Exception {
    String code = requestRegistrationCode("13900003001");

    mockMvc.perform(post("/api/auth/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"phone":"13900003001","code":"%s","nickname":"注册用户"}
                            """.formatted(code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("注册用户"))
            .andExpect(jsonPath("$.data.token").isNotEmpty());

    assertThat(userMapper.selectCount(
            new QueryWrapper<User>().eq("phone", "13900003001")
    )).isEqualTo(1L);
}
```

Add helpers that parse `$.data.code` with `ObjectMapper`. Cover duplicate phone, invalid phone, invalid nickname, unknown login phone, and successful login after registration.

Add security tests that all four user auth endpoints are public and `/api/auth/me` still requires JWT.

- [ ] **Step 2: Run controller/security tests and verify RED**

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml -Dtest=AuthControllerTest,AuthSecurityTest test
```

Expected: registration endpoint tests return 404 or fail to compile.

- [ ] **Step 3: Add controller endpoints**

Add to `AuthController`:

```java
@PostMapping("/user/register/code")
public ApiResponse<PhoneCodeResponse> issueRegistrationCode(@Valid @RequestBody PhoneCodeRequest request) {
    return ApiResponse.ok(new PhoneCodeResponse(authService.issueRegistrationCode(request.phone()).code()));
}

@PostMapping("/user/register")
public ApiResponse<AuthResponse> registerUser(@Valid @RequestBody PhoneRegistrationRequest request) {
    return ApiResponse.ok(authService.registerUser(request));
}
```

Change the existing `/user/code` endpoint to call `issueLoginCode` and return its optional mock code.

- [ ] **Step 4: Permit only the new public routes**

Add explicit POST matchers for `/api/auth/user/register/code` and `/api/auth/user/register`. Do not broaden access to other `/api/auth/**` routes.

- [ ] **Step 5: Run controller/security tests and verify GREEN**

Run the Task 4 command. Expected: PASS.

- [ ] **Step 6: Review checkpoint**

Confirm response envelopes and authorization behavior remain compatible. Commit only after explicit approval.

---

### Task 5: Add Alibaba Cloud SMS Provider

**Files:**

- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/food/auth/verification/AliyunSmsCodeSender.java`
- Create: `backend/src/test/java/com/example/food/auth/verification/AliyunSmsCodeSenderTest.java`

- [ ] **Step 1: Write failing configuration tests**

Construct `SmsProperties` with provider `aliyun` and assert each missing required property causes `IllegalArgumentException("Aliyun SMS configuration incomplete")`. Add one test where all fields are set and construction succeeds without sending a request.

- [ ] **Step 2: Run test and verify RED**

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml -Dtest=AliyunSmsCodeSenderTest test
```

Expected: test compilation fails because the sender does not exist.

- [ ] **Step 3: Add the official SDK dependency**

Add:

```xml
<aliyun.sms.version>4.6.0</aliyun.sms.version>
```

and:

```xml
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>dysmsapi20170525</artifactId>
    <version>${aliyun.sms.version}</version>
</dependency>
```

- [ ] **Step 4: Implement the conditional sender**

Annotate with:

```java
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun")
```

Validate `accessKeyId`, `accessKeySecret`, `signName`, and `templateCode` in the constructor. In `send`, build the official `Client`, create `SendSmsRequest` with phone, sign, template, and JSON template parameter `{"code":"123456"}` using `ObjectMapper`, then call `sendSms`.

Return `new SmsSendResult(null)` only when response body code is `OK`. Otherwise throw `ResponseStatusException(HttpStatus.BAD_GATEWAY, "短信发送失败，请稍后重试")`. Never log phone numbers, verification codes, AccessKeys, or provider response bodies.

- [ ] **Step 5: Run provider test and verify GREEN**

Run the Task 5 command. Expected: PASS without any network call.

- [ ] **Step 6: Review checkpoint**

Inspect dependency resolution and secret handling. Commit only after explicit approval.

---

### Task 6: Add The Chinese Registration UI

**Files:**

- Modify: `frontend/src/api/auth.js`
- Modify: `frontend/src/views/LoginView.vue`

- [ ] **Step 1: Add API functions**

```javascript
export function requestRegistrationCode(phone) {
  return http.post('/auth/user/register/code', { phone })
}

export function registerUser(phone, code, nickname) {
  return http.post('/auth/user/register', { phone, code, nickname })
}
```

- [ ] **Step 2: Add the registration tab and state**

Add an Element Plus tab named `register` between user and admin. Its visible labels are `手机号`, `验证码`, and `昵称`, with buttons `获取验证码` and `注册并登录`.

Add separate refs:

```javascript
const registrationPhone = ref('')
const registrationCode = ref('')
const registrationNickname = ref('')
const registrationCodeCountdown = ref(0)
```

Import `UserPlus`, `onBeforeUnmount`, `requestRegistrationCode`, and `registerUser`.

- [ ] **Step 3: Add request and submit handlers**

`requestRegisterCode()` validates the phone, calls the registration-code API, fills `registrationCode` only when `response.data.data.code` is present, displays `验证码已发送`, and starts a 60-second button countdown.

`submitRegistration()` validates all three fields, calls `registerUser`, stores auth with `auth.setAuth(response.data.data)`, and navigates to `/`.

Add Chinese mappings:

```javascript
'Phone already registered': '该手机号已经注册',
'User not registered': '该手机号尚未注册，请先注册',
'Verification code invalid': '验证码错误',
'Verification code expired': '验证码已过期，请重新获取',
'Verification code already used': '验证码已使用，请重新获取',
'Too many verification attempts': '验证码错误次数过多，请重新获取',
'Code requested too frequently': '验证码发送过于频繁，请稍后再试'
```

Clear countdown timers in `onBeforeUnmount`. Keep every visible string primarily Chinese.

- [ ] **Step 4: Build and verify**

```powershell
npm run build
```

Run from `frontend`. Expected: Vite build exits 0.

- [ ] **Step 5: Browser smoke test**

With backend on `7068` and frontend on `5173`, verify:

1. Registration tab is visible.
2. Mock code request fills a six-digit code.
3. Registration logs in and redirects home.
4. The new phone row exists in MySQL.
5. Logging out and requesting a login code succeeds for that phone.
6. An unknown phone receives the Chinese unregistered message.

- [ ] **Step 6: Review checkpoint**

Check desktop/mobile fit, Chinese copy, loading states, timer cleanup, and unrelated UI changes. Commit only after explicit approval.

---

### Task 7: Documentation, Full Verification, And Review

**Files:**

- Modify: `README.md`
- Review: all files changed by Tasks 1-6

- [ ] **Step 1: Update local and real SMS configuration docs**

Document that `SMS_PROVIDER=mock` is the default and returned codes are development-only. Add the Alibaba Cloud environment variable names without values and state that the AccessKey, sign, and template must be configured before switching to `aliyun`.

- [ ] **Step 2: Run full backend tests**

```powershell
mvn "-Dmaven.repo.local=D:\AI-Search-food\.m2" -f backend/pom.xml test
```

Expected: Maven exits 0 with zero test failures and zero errors.

- [ ] **Step 3: Run frontend build**

```powershell
npm run build
```

Run from `frontend`. Expected: Vite exits 0.

- [ ] **Step 4: Review the complete diff**

Run:

```powershell
git diff --check
git status --short
```

Review for requirement coverage, accidental API-key values, unrelated refactors, Flyway/MySQL compatibility, Chinese frontend copy, and missing tests. Preserve the pre-existing `FoodApplication.java` change without staging or reverting it.

- [ ] **Step 5: Ask for Git approval**

Report the test/build/browser evidence and ask whether to commit the implementation branch and push it to GitHub. Do not stage, commit, or push until the user explicitly approves.
