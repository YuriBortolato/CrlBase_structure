package com.apirest.api.Config;

import com.apirest.api.entity.Funcionario;
import com.apirest.api.entity.Cargo;
import com.apirest.api.entity.Cliente;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner  {
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // verifica se já existe algum Administrador
        if (funcionarioRepository.count() == 0) {

            // Variáveis para evitar repetição
            String adminEmail = "pato@sistema.com".toLowerCase();
            String adminCpf = "25285178908";
            String adminLogin = "admin";
            String adminTelefone = "4488239541";
            String adminNome = "Administrador do Sistema";
            String adminSenhaPlana = "admin123";
            String senhaCripto = passwordEncoder.encode(adminSenhaPlana);


            Funcionario admin = Funcionario.builder()
                    .nomeCompleto(adminNome)
                    .login(adminLogin)
                    .cpf(adminCpf)
                    .email(adminEmail)
                    .telefone(adminTelefone)
                    .senhaCriptografada(senhaCripto)
                    .cargo(Cargo.ADMIN)
                    .build();

            funcionarioRepository.save(admin);

            if (!clienteRepository.existsByCpf(adminCpf) && !clienteRepository.existsByEmail(adminEmail) && !clienteRepository.existsByLogin(adminLogin)) {
                Cliente clienteAdmin = Cliente.builder()
                        .nomeCompleto(adminNome)
                        .login(adminLogin)
                        .cpf(adminCpf)
                        .email(adminEmail)
                        .telefone(adminTelefone)
                        .senhaCriptografada(senhaCripto)
                        .ativo(true)
                        .build();

                clienteRepository.save(clienteAdmin);

                System.out.println("✅ Usuário administrador criado: login=admin | senha=admin123");
            }
        }
    }
}
