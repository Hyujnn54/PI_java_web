package Services.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating cover letters locally without API calls
 * This provides a professional template-based approach
 */
public class LocalCoverLetterService {

    /**
     * Generate a professional cover letter using local template
     * No external API calls required
     */
    public static String generateCoverLetter(String candidateName, String email, String phone,
                                            String jobTitle, String companyName, String experience,
                                            String education, List<String> skills, String cvContent) {
        try {
            StringBuilder coverLetter = new StringBuilder();

            // Date
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

            // Header with date
            coverLetter.append(date).append("\n\n");

            // Greeting
            coverLetter.append("Dear Hiring Manager,\n\n");

            // Opening paragraph - Express interest and position
            coverLetter.append("I am writing to express my strong interest in the ")
                    .append(jobTitle)
                    .append(" position at ")
                    .append(companyName)
                    .append(". With my background in ")
                    .append(education)
                    .append(" and ")
                    .append(experience)
                    .append(", I am confident that I can make a significant contribution to your team.\n\n");

            // Middle paragraph - Highlight skills
            if (skills != null && !skills.isEmpty()) {
                coverLetter.append("Throughout my career, I have developed strong expertise in the following areas:\n");
                for (String skill : skills) {
                    coverLetter.append("• ").append(skill).append("\n");
                }
                coverLetter.append("\nThese skills have enabled me to consistently deliver high-quality results and exceed expectations in my professional roles.\n\n");
            }

            // Add relevant CV highlights if available
            if (cvContent != null && !cvContent.isEmpty() && cvContent.length() > 50) {
                coverLetter.append("As detailed in my resume, my professional experience includes diverse projects and accomplishments that align well with the requirements of this position. ");
                coverLetter.append("I have demonstrated expertise in applying my technical and professional skills to solve complex challenges and deliver measurable results.\n\n");
            }

            // Connection paragraph
            coverLetter.append("I am particularly drawn to ")
                    .append(companyName)
                    .append(" because of your commitment to excellence and innovation in the industry. ")
                    .append("I am excited about the opportunity to contribute my expertise and grow professionally within your esteemed organization. ")
                    .append("I am confident that my skills, experience, and dedication make me an ideal candidate for this position.\n\n");

            // Closing paragraph - Call to action
            coverLetter.append("I would welcome the opportunity to discuss how my qualifications align with your team's needs. ")
                    .append("Please feel free to contact me at ")
                    .append(phone)
                    .append(" or ")
                    .append(email)
                    .append(" at your earliest convenience.\n\n");

            // Sign off
            coverLetter.append("Thank you for considering my application. I look forward to the possibility of contributing to ")
                    .append(companyName)
                    .append(" and making a positive impact on your organization.\n\n");

            coverLetter.append("Sincerely,\n\n");
            coverLetter.append(candidateName);

            return coverLetter.toString();

        } catch (Exception e) {
            System.err.println("Error generating cover letter: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}


