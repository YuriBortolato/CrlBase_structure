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

import java.math.BigDecimal; // <--- Importante
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner  {

    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final UnidadeRepository unidadeRepository;
    private final PerfilAcessoRepository perfilAcessoRepository;

    @Override
    public void run(String... args) {
        if (funcionarioRepository.count() == 0) {

            // CRIAR UNIDADE MATRIZ
            Unidade matriz;
            if (unidadeRepository.count() == 0) {
                matriz = Unidade.builder()
                        .grupoEconomicoId(1L)
                        .nomeFantasia("Matriz - Sede")
                        .documentoNumero("00000000000191")
                        .tipoDocumento(TipoDocumento.CNPJ)
                        .logradouro("Rua do Sistema")
                        .numero("1000")
                        .cidade("Maringá")
                        .uf("PR")
                        .cep("87000-000")
                        .emailContato("admin@sistema.com")
                        .build();
                unidadeRepository.save(matriz);
                System.out.println("✅ Unidade Matriz criada.");
            } else {
                matriz = unidadeRepository.findAll().get(0);
            }

            // CRIAR PERFIL DE ACESSO
            PerfilAcesso perfilAdmin;
            if (perfilAcessoRepository.count() == 0) {
                perfilAdmin = PerfilAcesso.builder()
                        .nome("ADMINISTRADOR")
                        .descricao("Acesso total ao sistema")
                        .tipo(TipoPerfil.INTERNO)
                        .build();
                perfilAcessoRepository.save(perfilAdmin);
                System.out.println("✅ Perfil de Acesso ADMIN criado.");
            } else {
                perfilAdmin = perfilAcessoRepository.findAll().get(0);
            }

            // DADOS DO ADMIN
            String adminEmail = "pato@sistema.com".toLowerCase();
            String adminCpf = "25285178908";
            String adminLogin = "admin";
            String adminTelefone = "4488239541";
            String adminNome = "Administrador do Sistema";
            String adminSenhaPlana = "admin123";
            String senhaCripto = passwordEncoder.encode(adminSenhaPlana);

            // PIN PADRÃO: 1234
            String pinHash = passwordEncoder.encode("1234");

            // CRIAR FUNCIONÁRIO ADMIN (Com PIN e Limites)
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
                    .perfilAcesso(perfilAdmin)
                    .ativo(true)
                    .pinHash(pinHash)
                    .limiteCrediario(new BigDecimal("10000.00")) // Limite alto para testes
                    .valorComissaoAcumulado(BigDecimal.ZERO)
                    .build();

            funcionarioRepository.save(admin);

            // CRIAR O CLIENTE ESPELHO DO ADMIN (Com Limite)
            if (!clienteRepository.existsByCpf(adminCpf) && !clienteRepository.existsByEmail(adminEmail)) {
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
                        .limiteCredito(new BigDecimal("10000.00"))
                        .build();

                clienteRepository.save(clienteAdmin);

                System.out.println("✅ Usuário administrador criado: login=admin | senha=admin123 | PIN=1234");
            }
        }
    }
}