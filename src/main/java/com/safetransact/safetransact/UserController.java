package com.safetransact.safetransact;

import com.safetransact.safetransact.dto.UserRequest;
import com.safetransact.safetransact.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setBalance(userRequest.getBalance());
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);

        UserResponse response = new UserResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getBalance(),
                saved.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBalance(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }
}