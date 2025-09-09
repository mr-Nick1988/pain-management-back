package pain_helper_back.service;


import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pain_helper_back.dto.UserLoginRequestDTO;
import pain_helper_back.dto.UserRegisterRequestDTO;
import pain_helper_back.entity.User;
import pain_helper_back.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;



    public String registerUser(UserRegisterRequestDTO request) {
        System.out.println("Email: " + request.getEmail());
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            System.out.println("User exists, no save");
            return "User with this email already exists";
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(request.getPassword());
        System.out.println("Saving...");
        userRepository.save(user);
        System.out.println("Saved");
        return "User registered successfully";
    }

    public String loginUser(UserLoginRequestDTO request) {
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty()) {
            return "User not found";
        }
        if (!user.get().getPassword().equals(request.getPassword())) {
            return "Incorrect password";
        }
        return "User logged in successfully";
    }
}
