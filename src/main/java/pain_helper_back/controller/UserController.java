package pain_helper_back.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.dto.UserLoginRequestDTO;
import pain_helper_back.dto.UserRegisterRequestDTO;
import pain_helper_back.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegisterRequestDTO request) {
        String result = userService.registerUser(request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginRequestDTO request) {
        String result = userService.loginUser(request);
        return ResponseEntity.ok(Map.of("message", result));
    }
}
