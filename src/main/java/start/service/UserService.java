package start.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import start.dto.request.UserRequestDTO;
import start.dto.response.UserResponseDTO;
import start.entity.User;
import start.repository.ArtworkRepository;
import start.repository.UserRepository;
import start.utils.AccountUtils;

import java.util.UUID;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountUtils accountUtils;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EmailService emailService;


    public User loginGoogle (String token) {
        try{
            FirebaseToken decodeToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String email = decodeToken.getEmail();
             User user = userRepository.findByEmail(email);
            return user;
        } catch (FirebaseAuthException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public UserResponseDTO getCreatorDetail(UUID id) {
        User user = userRepository.findUserById(id);
        UserResponseDTO UserResponseDTO = new UserResponseDTO();
        UserResponseDTO.setId(user.getId());
        UserResponseDTO.setName(user.getName());
        UserResponseDTO.setUsername(user.getUsername());
        UserResponseDTO.setEmail(user.getEmail());
        UserResponseDTO.setPostQuantity(user.getPostQuantity());
        UserResponseDTO.setAvt(user.getAvt());
        UserResponseDTO.setActive(user.isActive());
        UserResponseDTO.setRole(user.getRole());
        UserResponseDTO.setArtworks(user.getArtworks());
        return UserResponseDTO;
    }


    public User editProfile(UserRequestDTO userRequestDTO) {
          User user = accountUtils.getCurrentUser();
          
          user.setName(userRequestDTO.getName());
          user.setAvt(userRequestDTO.getAvt());

          if(!user.getEmail().toLowerCase().trim().equals(userRequestDTO.getEmail().toLowerCase().trim())){
              User checkMail = userRepository.findByEmail(userRequestDTO.getEmail());
              if(checkMail == null){
                  user.setActive(false);
                  user.setEmail(userRequestDTO.getEmail());
                  Runnable r = new Runnable() {
                      @Override
                      public void run() {emailService.sendMailTemplate(user);
                      }

                  };
                  new Thread(r).start();
              }else{
                  throw new DataIntegrityViolationException("Duplicate Email");
              }
          }


          String password = passwordEncoder.encode(userRequestDTO.getOldPassword());
          if(password.equals(user.getPassword())){
                  user.setPassword(passwordEncoder.encode(userRequestDTO.getNewPassword()));
          }
            return userRepository.save(user);
    }
}
