package start.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import start.dto.request.ApproveRequestDTO;
import start.entity.User;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@EnableAutoConfiguration
public class EmailService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private JavaMailSender javaMailSender;
    public void sendMailTemplate(User user){
        try{
            Context context = new Context();
            context.setVariable("name", user.getName());
            context.setVariable("email", "http://mycremo.art/confirm-success?id="+user.getId()+"&email="+user.getEmail());
            String text = templateEngine.process("emailtemplate", context);
            // Creating a simple mail message
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            // Setting up necessary details
            mimeMessageHelper.setFrom("admin@gmail.com");
            mimeMessageHelper.setTo(user.getEmail());
            mimeMessageHelper.setText(text, true);
            mimeMessageHelper.setSubject("Verify Account");
            javaMailSender.send(mimeMessage);
        }catch (MessagingException messagingException){
            messagingException.printStackTrace();
        }
    }

    public void sendMail(User user, String subject, String description){

        try{
            // Creating a simple mail message
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            // Setting up necessary details
            mimeMessageHelper.setFrom("admin@gmail.com");
            mimeMessageHelper.setTo(user.getEmail());
            mimeMessageHelper.setText(description);
            mimeMessageHelper.setSubject(subject);
            javaMailSender.send(mimeMessage);
        }catch (MessagingException messagingException){
            messagingException.printStackTrace();
        }
    }


}
