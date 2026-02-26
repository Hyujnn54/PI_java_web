# 🚀 Quick Start - SMS Notifications

## ⚡ 2-Minute Setup (Test Mode)

Want to test SMS without Twilio? It works in **SIMULATION MODE** by default!

### 1. Run the Test
```java
// Run this class to test SMS
test.TestSMSService.main()
```

### 2. What You'll See
```
=== SMS SIMULATION MODE ===
⚠️  No SMS sent - Twilio not configured!
To: +33612345678
Message:
🎯 RAPPEL ENTRETIEN - Talent Bridge

📅 27/02/2026 à 14:30
⏱️ Durée: 45 min
📍 Mode: ONLINE
🔗 Lien: https://meet.google.com/abc-def-ghi

Bonne chance! 🍀
============================
```

**✅ This proves the code works!** (No Twilio account needed for testing)

---

## 📱 To Send Real SMS (5-Minute Setup)

### Step 1: Get Twilio Account (2 minutes)
1. Go to: https://www.twilio.com/try-twilio
2. Sign up (free, no credit card)
3. Verify your email and phone

### Step 2: Get Credentials (1 minute)
1. Login to: https://console.twilio.com/
2. Copy these 3 values:
   - **Account SID** (starts with `AC...`)
   - **Auth Token** (click eye icon to reveal)
   - **Phone Number** (format: `+1234567890`)

### Step 3: Configure (2 minutes)
1. Copy file: `sms.properties.template` → `sms.properties`
2. Paste your credentials
3. Set `twilio.enabled=true`
4. Save!

### Step 4: Test
```java
// Run test again - now it sends real SMS!
test.TestSMSService.main()
```

**✅ Check your phone!**

---

## 📋 What's Already Integrated

### ✅ Automatic Interview Reminders
Your app **already sends SMS automatically** 24h before interviews!

No extra code needed - it's integrated in `InterviewReminderScheduler`:
```java
// When interview is 24h away:
// 1. Sends Email ✉️
// 2. Sends SMS 📱
// 3. No spam (tracks sent)
```

### ✅ Manual SMS Methods Available

**Send interview status:**
```java
SMSService.sendInterviewStatusUpdate("+33612345678", "ACCEPTED");
```

**Send application status:**
```java
SMSService.sendApplicationStatusUpdate(
    "+33612345678", 
    "Développeur Java", 
    "Acceptée"
);
```

**Send custom interview reminder:**
```java
SMSService.sendInterviewReminder(interview, "+33612345678");
```

---

## 🎯 Phone Number Format

**✅ Correct:**
- `+33612345678` (France)
- `+14155551234` (USA)
- `+212612345678` (Morocco)

**❌ Wrong:**
- `0612345678` (missing +)
- `+33 6 12 34 56 78` (spaces)

---

## 💡 Pro Tips

### For Testing:
1. Test in **simulation mode first** (no account needed)
2. Then create Twilio account
3. Verify your phone number in Twilio
4. Test with your own phone

### For Demo/Presentation:
1. Show simulation mode output (proves concept)
2. Show real SMS on your phone (live demo)
3. Explain auto-reminders (24h before)
4. Show Twilio dashboard (sent messages log)

### For Production:
1. Upgrade Twilio account (remove trial limits)
2. Get dedicated phone number
3. Enable for all candidates
4. Monitor usage in Twilio console

---

## 🆘 Troubleshooting

### "SMS SIMULATION MODE" appears?
- **That's normal!** It means Twilio isn't configured yet
- The code still works, just doesn't send real SMS
- Good for testing without account

### Want to send real SMS?
- Follow "Step 2" above (5 minutes)
- Get free Twilio account
- Configure credentials

### "Invalid phone number"?
- Use format: `+33612345678`
- Include country code with `+`
- No spaces or dashes

---

## 📞 Support

- **Twilio Docs:** https://www.twilio.com/docs/sms/quickstart/java
- **Get Account:** https://www.twilio.com/try-twilio
- **Console:** https://console.twilio.com/

---

**That's it! Your SMS service is ready to use! 🎉**

