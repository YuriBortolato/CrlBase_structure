package com.apirest.api.Config;

import com.apirest.api.entity.Funcionario;
import com.apirest.api.entity.Cargo;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner  {
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // verifica se já existe algum Administrador
        if (funcionarioRepository.count() == 0) {
            Funcionario admin = Funcionario.builder()
                    .nomeCompleto("Administrador do Sistema")
                    .login("admin")
                    .cpf("25285178908")
                    .email("pato@sistema.com")
                    .telefone("4488239541")
                    .senhaCriptografada(passwordEncoder.encode("admin123")) // senha padrão
                    .cargo(Cargo.ADMIN)
                    .build();

            funcionarioRepository.save(admin);
            System.out.println("✅ Usuário administrador criado: login=admin | senha=admin123");
        }
    }
}
