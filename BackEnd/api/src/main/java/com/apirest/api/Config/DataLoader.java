package com.apirest.api.Config;

import com.apirest.api.entity.*;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import com.apirest.api.repository.PerfilAcessoRepository;
import com.apirest.api.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner  {
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final UnidadeRepository unidadeRepository;

    @Override
    public void run(String... args) {
        // verifica se já existe algum Administrador
        if (funcionarioRepository.count() == 0) {

            Unidade matriz;
            if (unidadeRepository.count() == 0) {
                // <--- CORREÇÃO AQUI: Campos batendo com a entidade Unidade --->
                matriz = Unidade.builder()
                        .grupoEconomicoId(1L) // Campo obrigatório (@NotNull)
                        .nomeFantasia("Matriz - Sede") // Nome correto do campo
                        .documentoNumero("00000000000191") // Campo obrigatório (@NotBlank)
                        .tipoDocumento(TipoDocumento.CNPJ) // Assumindo que você tem esse Enum criado
                        .logradouro("Rua do Sistema")
                        .numero("1000")
                        .cidade("Maringá")
                        .uf("PR")
                        .cep("87000-000")
                        .emailContato("admin@sistema.com")
                        //.ativo(true) <--- Removi pois sua entidade Unidade NÃO tem esse campo no código que você mandou
                        .build();

                unidadeRepository.save(matriz);
                System.out.println("✅ Unidade Matriz criada automaticamente.");
            } else {
                matriz = unidadeRepository.findAll().get(0);
            }

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
                    .nomeRegistro("Admin")
                    .dataNascimento(LocalDate.of(1990, 1, 1))
                    .unidade(matriz)
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
                        .dataNascimento(LocalDate.of(1990, 1, 1))
                        .funcionarioOrigem(admin)
                        .unidadeOrigem(matriz)
                        .build();

                clienteRepository.save(clienteAdmin);

                System.out.println("✅ Usuário administrador criado: login=admin | senha=admin123");
            }
        }
    }
}
