# Email Setup Guide - Free Options

## 1. Gmail SMTP (Recommended - 100% Free)

### Setup Steps:

1. **Enable 2-Factor Authentication** in your Google account
   - Go to: https://myaccount.google.com/security
   - Enable "2-Step Verification"

2. **Generate App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)" 
   - Name it: "EduScheduler"
   - Copy the generated password (16 characters)

3. **Update application.properties**:
   ```properties
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-app-password
   
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   
   app.base-url=https://your-domain.com
   ```

4. **Limits**:
   - Free tier: 500 emails/day
   - Perfect for small to medium apps

---

## 2. Resend.com (Developer-Friendly, Free Tier)

### Setup Steps:

1. **Sign up** at https://resend.com (free tier: 3000 emails/month)

2. **Get API Key** from dashboard

3. **Add to application.properties**:
   ```properties
   spring.mail.host=smtp.resend.com
   spring.mail.port=587
   spring.mail.username=resend
   spring.mail.password=your-api-key
   
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   ```

4. **Limits**:
   - Free tier: 3000 emails/month
   - 100 emails/day

---

## 3. SendGrid (Popular, Free Tier)

### Setup Steps:

1. **Sign up** at https://sendgrid.com (free tier: 100 emails/day)

2. **Create API Key** in dashboard

3. **Update application.properties**:
   ```properties
   spring.mail.host=smtp.sendgrid.net
   spring.mail.port=587
   spring.mail.username=apikey
   spring.mail.password=your-sendgrid-api-key
   
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   ```

4. **Limits**:
   - Free tier: 100 emails/day
   - 40,000 emails/month (first 30 days)

---

## Quick Test

After configuration, test with:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "birthDate": "2000-01-01",
    "role": "STUDENT"
  }'
```

Check console for email link (development mode) or check inbox (production mode).

---

## Development vs Production

- **Development Mode**: Emails logged to console (no configuration needed)
- **Production Mode**: Uncomment SMTP settings in `application.properties`

The code automatically detects if SMTP is configured and uses appropriate method!

