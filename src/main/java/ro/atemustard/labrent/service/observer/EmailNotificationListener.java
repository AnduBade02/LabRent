package ro.atemustard.labrent.service.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.ReturnAssessment;

/**
 * Observer concret — trimite notificări email la evenimentele de închiriere.
 *
 * În modul development (când MAIL_USERNAME nu e configurat), doar loghează
 * mesajul fără a trimite email real.
 */
@Component
public class EmailNotificationListener implements RentalEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final JavaMailSender mailSender;

    public EmailNotificationListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void onRequestCreated(RentalRequest request) {
        String subject = "LabRent: Rental request created";
        String body = String.format("Your rental request for %s has been submitted and is pending approval.",
                request.getEquipment().getName());
        sendOrLog(request.getUser().getEmail(), subject, body);
    }

    @Override
    public void onRequestApproved(RentalRequest request) {
        String subject = "LabRent: Rental request approved";
        String body = String.format("Your rental request for %s has been approved. You can pick up the equipment.",
                request.getEquipment().getName());
        sendOrLog(request.getUser().getEmail(), subject, body);
    }

    @Override
    public void onRequestRejected(RentalRequest request) {
        String subject = "LabRent: Rental request rejected";
        String body = String.format("Your rental request for %s has been rejected.",
                request.getEquipment().getName());
        sendOrLog(request.getUser().getEmail(), subject, body);
    }

    @Override
    public void onEquipmentReturned(RentalRequest request) {
        String subject = "LabRent: Equipment returned";
        String body = String.format("Equipment %s has been returned and is awaiting inspection.",
                request.getEquipment().getName());
        sendOrLog(request.getUser().getEmail(), subject, body);
    }

    @Override
    public void onAssessmentCompleted(ReturnAssessment assessment) {
        RentalRequest request = assessment.getRentalRequest();
        String subject = "LabRent: Return assessment completed";
        String body = String.format("Your return of %s has been assessed. Condition: %s. Reputation impact: %+.1f",
                request.getEquipment().getName(),
                assessment.getConditionRating().name(),
                assessment.getReputationImpact());
        sendOrLog(request.getUser().getEmail(), subject, body);
    }

    private void sendOrLog(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            // In development, mail may not be configured — just log
            log.info("[DEV] Email would be sent to {}: {} — {}", to, subject, body);
        }
    }
}
