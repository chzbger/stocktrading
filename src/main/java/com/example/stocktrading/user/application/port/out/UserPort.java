package com.example.stocktrading.user.application.port.out;

import com.example.stocktrading.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    boolean existsByUsername(String username);

    void deleteById(Long id);
}
