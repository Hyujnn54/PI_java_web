# Feedback System - Quick Guide

## Visual Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    INTERVIEW CARD                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  📅 Interview #123                      [SCHEDULED]          │
│                                                              │
│  📅 Date & Time    ⏱ Duration    🎯 Mode                    │
│  Feb 20, 2026      60 min        ONLINE                     │
│  14:00                                                       │
│                                                              │
│  🔗 Meeting Link: https://meet.zoom.us/xyz123               │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│  RECRUITER VIEW - No Feedback:                              │
│  [📋 Create Feedback]                                        │
│                                                              │
│  RECRUITER VIEW - Feedback Exists:                          │
│  [👁 View]  [✏ Update]  [🗑 Delete]                         │
│                                                              │
│  CANDIDATE VIEW - No Feedback:                              │
│  Pending Review                                             │
│                                                              │
│  CANDIDATE VIEW - Feedback Exists:                          │
│  [ACCEPTED] or [REJECTED]                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              FEEDBACK PANEL (Bottom of Screen)               │
├─────────────────────────────────────────────────────────────┤
│  Interview Feedback                                          │
│                                                              │
│  Overall Score:    [75___________]  ✓ ACCEPTED              │
│                                                              │
│  Comments:         ┌───────────────────────────────────┐    │
│                    │ Candidate showed excellent        │    │
│                    │ technical skills and clear        │    │
│                    │ communication. Recommended for    │    │
│                    │ the position.                     │    │
│                    └───────────────────────────────────┘    │
│                                                              │
│           [💾 Save Feedback]  [✓ Accept]  [✗ Reject]        │
│                      [🗑️ Delete]  [Cancel]                  │
└─────────────────────────────────────────────────────────────┘
```

## Button Actions Explained

### CREATE FEEDBACK Button (Green)
- Appears when: No feedback exists for the interview
- Action: Opens the feedback panel with empty fields
- Available to: Recruiters only

### VIEW Button (Blue)
- Appears when: Feedback exists
- Action: Opens a dialog showing score and comments (read-only)
- Available to: Recruiters only

### UPDATE Button (Orange)
- Appears when: Feedback exists
- Action: Opens the feedback panel with existing data pre-filled
- Available to: Recruiters only

### DELETE Button (Red)
- Appears when: Feedback exists
- Action: Prompts for confirmation, then deletes the feedback
- Available to: Recruiters only

### SAVE FEEDBACK Button
- In feedback panel
- Action: Creates new or updates existing feedback
- Validates score is 0-100

### ACCEPT CANDIDATE Button
- In feedback panel
- Action: Auto-fills score as 80 and saves
- Quick action for positive results

### REJECT CANDIDATE Button
- In feedback panel
- Action: Auto-fills score as 40 and saves
- Quick action for negative results

### DELETE Button (in panel)
- In feedback panel
- Only visible when updating existing feedback
- Action: Deletes the feedback after confirmation

### CANCEL Button
- In feedback panel
- Action: Closes the panel without saving

## Score Indicators

While typing the score, you'll see live indicators:

- **Score >= 70**: ✓ ACCEPTED (green)
- **Score 50-69**: ⚠ BORDERLINE (orange)
- **Score < 50**: ✗ REJECTED (red)

## Keyboard Shortcuts

- Press Enter in score field: Focus moves to comments
- Press Escape: Cancel and close panel (when implemented)

## Developer Notes

### Key Methods in InterviewManagementController:

1. `createFeedback(Interview)` - Opens panel for new feedback
2. `viewFeedback(Interview)` - Shows feedback in dialog
3. `updateFeedback(Interview)` - Opens panel with existing data
4. `deleteFeedbackForInterview(Interview)` - Deletes after confirmation
5. `handleSaveFeedback()` - Validates and saves feedback
6. `handleAcceptCandidate()` - Quick accept with score 80
7. `handleRejectCandidate()` - Quick reject with score 40

### Database Integration:

- Uses `InterviewFeedbackService` for all CRUD operations
- One feedback per interview (1:1 relationship)
- Score range: 0-100 (integer)
- Comments: Text field (optional but recommended)

### Styling Classes:

All buttons use inline styles matching the app theme:
- Primary blue: #5BA3F5
- Success green: #28a745
- Warning orange: #f0ad4e
- Danger red: #dc3545

