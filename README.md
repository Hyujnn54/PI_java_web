# Talent Bridge - Recruitment Management System

A JavaFX-based recruitment management application with automated email and SMS notifications.

## Features

- ✅ Interview Management
- ✅ Application Tracking  
- ✅ Job Offer Management
- ✅ Automated Email Reminders (24h before interviews)
- ✅ Automated SMS Reminders (24h before interviews)
- ✅ Interview Feedback System

## Technologies

- Java 21
- JavaFX
- MySQL Database
- Email (Gmail SMTP)
- SMS (SMSMobileAPI)

## Configuration

### Database
Configure in `Utils/MyDatabase.java`

### Email
1. Copy `email.properties.template` to `email.properties`
2. Configure Gmail credentials

### SMS  
1. Copy `sms.properties.template` to `sms.properties`
2. Add SMSMobileAPI credentials:
   - `sms.api.url` - API endpoint
   - `sms.api.key` - Your API key
   - `sms.enabled=true`

## Running

Run `org.example.MainFX` as the main class.

## Automated Reminders

The system automatically sends email and SMS reminders 24 hours before scheduled interviews.

