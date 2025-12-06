package com.larlew.controller;

import com.larlew.api.UsersApi;
import com.larlew.model.User;
import com.larlew.model.UserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class UsersApiDelegateImpl implements UsersApi {

    private final List<User> users = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public ResponseEntity<User> createUser(UserRequest userRequest) {
        User user = new User();
        user.setId(nextId++);
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setCreatedAt(OffsetDateTime.now());
        
        users.add(user);
        
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<User> getUserById(Long userId) {
        Optional<User> user = users.stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst();
        
        return user.map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(users);
    }
}
