package org.example.repository;

import org.example.model.RefreshToken;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying // Обязательно: говорит Spring, что это запрос на изменение/удаление
    @Transactional // Обязательно: выполнение должно быть в транзакции
    void deleteByUser(User user); // Изменили int на void, чтобы избежать ошибки Null
}