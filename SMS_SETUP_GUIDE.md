# 📱 SMS Notification Setup Guide - Talent Bridge

## ✅ What's Done

I've successfully implemented SMS notification functionality for your Talent Bridge application using **Twilio API**. Here's what's included:

### 📁 Files Created/Modified:

1. **`SMSService.java`** - Main SMS service class (similar to your EmailService)
2. **`sms.properties.template`** - Configuration template for Twilio credentials
3. **`pom.xml`** - Updated with Twilio SDK dependency
4. **`.gitignore`** - Updated to exclude sms.properties (keeps credentials safe)
5. **`InterviewReminderScheduler.java`** - Updated to send both Email + SMS reminders

---

## 🚀 How to Setup SMS Notifications

### Step 1: Create Twilio Account (FREE)

1. Go to: **https://www.twilio.com/try-twilio**
2. Sign up for a **free trial account**
3. Verify your email and phone number
4. You'll get:
   - **$15 free credit** (enough for ~500 SMS)
   - A **free trial phone number**

### Step 2: Get Your Twilio Credentials

After signing up:

1. Go to your **Twilio Console Dashboard**: https://console.twilio.com/
2. Find these 3 important values:
   - **Account SID** (looks like: `ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)
   - **Auth Token** (click to reveal, looks like: `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)
   - **Twilio Phone Number** (format: `+1234567890`)

### Step 3: Configure SMS in Your Project

1. **Copy the template file:**
   ```
   Copy: src/main/resources/sms.properties.template
   To:   src/main/resources/sms.properties
   ```

2. **Edit `sms.properties` and fill in your Twilio credentials:**

   ```properties
   # Twilio Account SID (from Twilio Console)
   twilio.account.sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   
   # Twilio Auth Token (from Twilio Console)
   twilio.auth.token=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   
   # Your Twilio Phone Number (format: +1234567890)
   twilio.phone.number=+1234567890
   
   # Test phone number for testing (YOUR PHONE, format: +1234567890)
   twilio.test.recipient=+33612345678
   
   # Enable SMS sending (set to true when ready)
   twilio.enabled=true
   ```

3. **Save the file** - It's already in .gitignore, so it won't be committed to Git

### Step 4: Reload Maven Dependencies

In your IDE (IntelliJ IDEA):
1. Right-click on **pom.xml**
2. Select **Maven → Reload Project**
3. Wait for dependencies to download (Twilio SDK ~10MB)

---

## 🧪 Testing SMS Service

### Test 1: Send a Simple Test SMS

Add this to your main class or create a test:

```java
public static void main(String[] args) {
    // Test SMS service
    SMSService.sendTestSMS();
}
```

**Expected Output:**
- ✅ If configured: "SMS sent successfully! Message SID: SMxxxx"
- ⚠️  If not configured: "SMS SIMULATION MODE" (shows what would be sent)

### Test 2: Test Interview Reminder SMS

```java
// Create a test interview
Interview testInterview = new Interview();
testInterview.setScheduledAt(LocalDateTime.now().plusDays(1));
testInterview.setDurationMinutes(45);
testInterview.setMode("ONLINE");
testInterview.setMeetingLink("https://meet.google.com/abc-def-ghi");

// Send SMS reminder
SMSService.sendInterviewReminder(testInterview, "+33612345678");
```

---

## 📋 SMS Features Implemented

### 1. **Interview Reminders (24h before)**
Automatically sent by `InterviewReminderScheduler`:
```
🎯 RAPPEL ENTRETIEN - Talent Bridge

📅 27/02/2026 à 10:00
⏱️ Durée: 45 min
📍 Mode: ONLINE
🔗 Lien: https://meet.google.com/abc-def

Bonne chance! 🍀
```

### 2. **Interview Status Updates**
```java
// Send status update SMS
SMSService.sendInterviewStatusUpdate("+33612345678", "ACCEPTED");
// ✅ Talent Bridge: Votre entretien a été ACCEPTÉ!
```

### 3. **Application Status Updates**
```java
// Send application status SMS
SMSService.sendApplicationStatusUpdate(
    "+33612345678", 
    "Développeur Java", 
    "En cours d'évaluation"
);
```

---

## 🔒 Security Notes

### ✅ What's Protected:
- ✅ `sms.properties` is in `.gitignore` (won't be committed)
- ✅ `sms.properties.template` is committed (safe, no credentials)
- ✅ Credentials are loaded at runtime only

### ⚠️ Important:
- **NEVER commit** `sms.properties` with real credentials
- **NEVER hardcode** credentials in code
- Use `sms.properties.template` to share configuration structure with team

---

## 💰 Twilio Free Trial Limits

### What You Get (FREE):
- ✅ **$15.50 credit** (expires after time)
- ✅ ~**500 SMS messages** (depending on country)
- ✅ **1 free phone number**

### Limitations:
- ⚠️  Can only send to **verified phone numbers** during trial
- ⚠️  Messages include "Sent from a Twilio trial account" prefix
- ⚠️  Need to upgrade for production use

### To Verify Phone Numbers (Trial):
1. Go to: https://console.twilio.com/us1/develop/phone-numbers/manage/verified
2. Click **"Add a new phone number"**
3. Enter the number you want to test (your phone)
4. Verify with the code sent to you

---

## 🎯 How It Works in Your App

### Automatic Reminders (Background):
The `InterviewReminderScheduler` runs in the background and:
1. ✅ Checks every 5 minutes for upcoming interviews
2. ✅ If interview is 24 hours away → sends Email + SMS
3. ✅ Tracks sent reminders (no spam)
4. ✅ Fetches candidate email and phone from database

### Manual SMS Sending (Optional):
You can manually send SMS from your UI:

```java
// Example: Send SMS when interview status changes
public void updateInterviewStatus(Long interviewId, String status) {
    // Update database...
    
    // Get candidate phone
    String phone = getCandidatePhone(interviewId);
    
    // Send SMS notification
    if (phone != null) {
        SMSService.sendInterviewStatusUpdate(phone, status);
    }
}
```

---

## 🐛 Troubleshooting

### Problem: "SMS SIMULATION MODE"
**Solution:** 
- Check if `sms.properties` exists
- Check if `twilio.enabled=true`
- Check if credentials are filled (not "YOUR_xxx")

### Problem: "Failed to send SMS - Authentication Error"
**Solution:**
- Verify Account SID and Auth Token are correct
- Copy-paste directly from Twilio Console (no spaces)

### Problem: "Unverified phone number"
**Solution:**
- During trial, you can only send to verified numbers
- Verify phone at: https://console.twilio.com/us1/develop/phone-numbers/manage/verified

### Problem: "Invalid phone number format"
**Solution:**
- Use international format: `+33612345678` (France)
- NOT: `0612345678` or `33612345678`

---

## 📊 What to Show Your Professor

This implementation demonstrates:

1. ✅ **External API Integration** (Twilio REST API)
2. ✅ **Multi-channel Communication** (Email + SMS)
3. ✅ **Configuration Management** (Properties files)
4. ✅ **Security Best Practices** (.gitignore sensitive data)
5. ✅ **Error Handling** (Graceful fallback to simulation mode)
6. ✅ **Phone Number Validation**
7. ✅ **Automated Background Tasks** (Scheduler integration)

---

## 🎓 Academic Project Tips

### For Your Report/Presentation:
1. **Show the code structure** (service layer, configuration)
2. **Demonstrate SMS sending** (live or screenshots)
3. **Explain why Twilio** (industry standard, easy integration)
4. **Show security measures** (credential protection)
5. **Discuss alternatives** (AWS SNS, Nexmo, etc.)

### Why This API Choice is Good:
- ✅ **Relevant to HR/Recruitment** (real-world use case)
- ✅ **Easy to demonstrate** (send SMS to your own phone)
- ✅ **Professional API** (used by Uber, Airbnb, WhatsApp)
- ✅ **Well documented** (shows research skills)
- ✅ **Complements existing features** (pairs with email)

---

## 📞 Next Steps

1. ✅ **Test in simulation mode** (no credentials needed)
2. ✅ **Sign up for Twilio** (5 minutes)
3. ✅ **Configure credentials** (copy template)
4. ✅ **Send test SMS** (verify it works)
5. ✅ **Integrate with UI** (optional buttons for manual send)
6. ✅ **Document for report** (screenshots, code samples)

---

## 🆘 Need Help?

- **Twilio Documentation:** https://www.twilio.com/docs/sms
- **Twilio Console:** https://console.twilio.com/
- **Support:** Twilio has excellent documentation and support

---

**Good luck with your project! 🚀**

If you have questions about the implementation, just ask!

