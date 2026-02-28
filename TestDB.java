import Services.joboffers.JobOfferService;
import Models.joboffers.JobOffer;
import java.util.List;

public class TestDB {
    public static void main(String[] args) throws Exception {
        JobOfferService service = new JobOfferService();
        List<JobOffer> offers = service.getAllJobOffers();
        if (offers.isEmpty()) {
            System.out.println("No offers found.");
        } else {
            JobOffer latest = offers.get(0);
            System.out.println("ID: " + latest.getId());
            System.out.println("Title: " + latest.getTitle());
            System.out.println("Description: [" + latest.getDescription() + "]");
        }
    }
}
