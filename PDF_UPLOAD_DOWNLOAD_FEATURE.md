# PDF Upload & Download Feature

## How It Works

### 1. Candidate Uploads PDF During Application
- When candidate clicks "Apply Now" for a job offer
- Application form appears with new "Upload CV (PDF)" field
- Click "Browse" button to select PDF file from computer
- File is stored with application when submitted

### 2. PDF Storage
- PDF files are saved to: `uploads/applications/` directory
- Files are renamed with UUID + original name (e.g., `a1b2c3d4_resume.pdf`)
- Database stores only the file path (not the full PDF content)
- This keeps database lean and fast

### 3. Recruiter Downloads PDF
- When viewing application details, recruiter sees "Download CV" button
- Click to open PDF in default PDF viewer (Adobe Reader, Chrome, etc.)
- File is retrieved from disk and opened

### 4. Admin Can Also Download
- Admins see same "Download CV" button
- Can download any application's PDF

## Files Created/Modified

### New Files:
✅ `Services/FileService.java` - Handles all file operations
  - `uploadPDF(File)` - Upload PDF to disk
  - `downloadPDF(String)` - Get PDF file
  - `deletePDF(String)` - Delete PDF (auto-called when app deleted)
  - `fileExists(String)` - Check if file exists

### Modified Files:
✅ `Services/ApplicationService.java` - Added PDF upload support
  - `createWithPDF()` - Create application with PDF upload
  - `downloadPDF()` - Get PDF for download

✅ `Controllers/JobOffersController.java` - Updated apply form
  - Added FileChooser for PDF selection
  - "Browse" button to select file
  - Submit uploads PDF and saves application

✅ `Controllers/ApplicationsController.java` - Added download button
  - "Download CV" button for recruiters/admins
  - Opens PDF in default viewer
  - Error handling if file not found

## Step-by-Step Workflow

### For Candidates (Uploading):
```
1. Click "Apply Now" on job offer
2. Fill in: Phone + Cover Letter
3. Click "Browse" in "Upload CV" section
4. Select PDF from computer
5. Click OK to submit
6. PDF uploaded to disk, path saved in database
7. Application created successfully
```

### For Recruiters (Downloading):
```
1. Go to "Applications" tab
2. Click on application to view details
3. Scroll down to "CV Path" section
4. Click "Download CV" button
5. PDF opens in default viewer (Adobe, Chrome, etc.)
6. Can save/print the PDF
```

## Technical Details

### Directory Structure:
```
E:\pidev\PI_java_web\
├── uploads/
│   └── applications/
│       ├── a1b2c3d4_resume.pdf
│       ├── e5f6g7h8_cv.pdf
│       └── ...
```

### Database Storage:
```
job_application table:
- id: 1
- candidate_id: 1
- offer_id: 1
- cv_path: "uploads/applications/a1b2c3d4_resume.pdf"
- phone: "555-1234"
- cover_letter: "I am interested..."
- applied_at: 2026-02-14 10:30:00
- current_status: SUBMITTED
```

## Features

✅ **Secure**: UUID prevents file name collisions
✅ **Organized**: All PDFs in dedicated directory
✅ **Database Efficient**: Only path stored, not file content
✅ **User Friendly**: Browse button makes selection easy
✅ **Auto Cleanup**: PDF deleted when application deleted (cascade)
✅ **Error Handling**: Shows alerts for missing files
✅ **Viewer Integration**: Opens in system default PDF viewer

## Validation

- Only PDF files accepted
- File must exist before upload
- Size not limited (adjust if needed)
- Download button only for recruiters/admins
- Proper error messages if file not found

## To Use:

1. Candidate applies for job → upload PDF
2. PDF saved to disk automatically
3. Recruiter views application → sees "Download CV" button
4. Click to open PDF in default viewer
5. Can print/save as needed

Done! 🎉

