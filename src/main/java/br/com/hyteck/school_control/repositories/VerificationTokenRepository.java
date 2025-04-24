package br.com.hyteck.school_control.repositories;

import br.com.hyteck.school_control.models.auth.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
  Optional<VerificationToken> findByToken(String token);

  Optional<VerificationToken> findByUser_Id(String userId); // Para reenviar token, por exemplo

  void deleteByUser_Id(String userId); // Para limpar token antigo ao gerar novo
}