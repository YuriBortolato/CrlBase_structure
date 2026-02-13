package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.*;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import com.apirest.api.repository.PerfilAcessoRepository;
import com.apirest.api.repository.UnidadeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;
    private final UnidadeRepository unidadeRepository;
    private final PerfilAcessoRepository perfilAcessoRepository;

    private static final Set<Cargo> PERMISSAO_CRIAR_FUNCIONARIO = Set.of(
            Cargo.DONO, Cargo.GERENTE, Cargo.LIDER_VENDA, Cargo.ADMIN
    );

    private String limparCPF(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^\\d]", ""); // Remove tudo que não for dígito
    }

    @Transactional
    public FuncionarioResponseDTO criar(FuncionarioDTO dto) {
        log.info("Iniciando cadastro de funcionário. Nome: {}, Cargo: {}", dto.getNomeCompleto(), dto.getCargo());

        String cpfLimpo = limparCPF(dto.getCpf());

        // Validações básicas
        if (repository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("E-mail já cadastrado para outro funcionário.");

        if (repository.existsByCpf(cpfLimpo))
            throw new RuntimeException("CPF já cadastrado para outro funcionário.");

        if (repository.existsByLogin(dto.getLogin()))
            throw new RuntimeException("Login já cadastrado para outro funcionário.");

        // Verifica idade mínima
        if (dto.getDataNascimento() != null) {
            int idade = Period.between(dto.getDataNascimento(), LocalDate.now()).getYears();
            if (idade < 18) {
                throw new RuntimeException("O funcionário deve ter pelo menos 18 anos de idade.");
            }
        } else {
            throw new RuntimeException("Data de nascimento é obrigatória.");
        }

        Unidade unidade = null;
        if (dto.getIdUnidade() != null) {
            unidade = unidadeRepository.findById(dto.getIdUnidade())
                    .orElseThrow(() -> new RuntimeException("Unidade não encontrada com o ID: " + dto.getIdUnidade()));
        } else {
            throw new RuntimeException("É obrigatório informar a Unidade (Filial/Matriz) do funcionário.");
        }

        PerfilAcesso perfil = perfilAcessoRepository.findById(dto.getIdPerfilAcesso())
                .orElseThrow(() -> new RuntimeException("Perfil de Acesso não encontrado com ID: " + dto.getIdPerfilAcesso()));

        // Criptografa a senha
        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());

        // Cria a entidade Funcionario
        Funcionario funcionario = Funcionario.builder()
                .cargo(dto.getCargo())
                .nomeCompleto(dto.getNomeCompleto())
                .nomeRegistro(dto.getNomeRegistro())
                .cpf(cpfLimpo)
                .email(dto.getEmail())
                .dataNascimento(dto.getDataNascimento())
                .telefone(dto.getTelefone())
                .login(dto.getLogin())
                .senhaCriptografada(senhaCriptografada)
                .unidade(unidade)
                .perfilAcesso(perfil)
                .build();

        // Salva o funcionário
        Funcionario salvo = repository.save(funcionario);

        // Sincroniza com Cliente Espelho
        log.info("Sincronizando cliente espelho para o funcionário ID: {}", salvo.getIdFuncionario());

        try {
        Optional<Cliente> clienteExistenteOpt = clienteRepository.findByCpf(cpfLimpo);
        Cliente clienteEspelho;

        if (clienteExistenteOpt.isPresent()) {
            // Atualiza o cliente espelho existente
            log.info("Cliente já existente (CPF: {}). Vinculando ao funcionário.", cpfLimpo);
            clienteEspelho = clienteExistenteOpt.get();

            clienteEspelho.setNomeCompleto(salvo.getNomeCompleto());
            clienteEspelho.setEmail(salvo.getEmail());
            clienteEspelho.setTelefone(salvo.getTelefone());
            clienteEspelho.setLogin(salvo.getLogin());
            clienteEspelho.setSenhaCriptografada(salvo.getSenhaCriptografada()); // Atualiza a senha
            clienteEspelho.setFuncionarioOrigem(salvo); // Vincula ao funcionário criado
            clienteEspelho.setUnidadeOrigem(salvo.getUnidade()); // Atualiza a unidade de origem

        } else {
            // Cria um novo cliente espelho
            log.info("Cliente não encontrado. Criando novo cliente espelho.");
            clienteEspelho = Cliente.builder()
                    .nomeCompleto(salvo.getNomeCompleto())
                    .email(salvo.getEmail())
                    .cpf(salvo.getCpf())
                    .dataNascimento(salvo.getDataNascimento())
                    .telefone(salvo.getTelefone())
                    .login(salvo.getLogin())
                    .senhaCriptografada(salvo.getSenhaCriptografada())
                    .ativo(true)
                    .funcionarioOrigem(salvo) // Vincula ao funcionário criado
                    .unidadeOrigem(salvo.getUnidade()) // Define a unidade de origem
                    .limiteCredito(new java.math.BigDecimal("200.00")) // Limite de crédito padrão
                    .build();
        }
        clienteRepository.save(clienteEspelho);

        } catch (Exception e) {
            // Em caso de erro, loga e reverte a criação do funcionário
            log.error("Erro ao salvar cliente espelho: {}", e.getMessage());
            // Reverte a criação do funcionário em caso de falha
            throw new RuntimeException("Erro ao sincronizar dados: O E-mail ou Login já estão em uso por outra pessoa.");
        }

        log.info("Funcionário criado com sucesso. ID: {}", salvo.getIdFuncionario());

        return toResponseDTO(salvo);
    }

    public List<Funcionario> listarTodos() {
        return repository.findAllByAtivoTrue();
    }

    public Funcionario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));
    }

    @Transactional
    public FuncionarioResponseDTO atualizar(Long id, FuncionarioDTO dto) {
        log.info("Atualizando funcionário ID: {}", id);
        Funcionario funcionarioExistente = buscarPorId(id);

        String cpfFuncionario = funcionarioExistente.getCpf();

        // Verifica se o email ou login foram alterados
        boolean emailMudou = false;
        if (dto.getEmail() != null) {
            if (funcionarioExistente.getEmail() == null) {
                emailMudou = true;
            } else {
                emailMudou = !dto.getEmail().equalsIgnoreCase(funcionarioExistente.getEmail());
            }
        }
        // Verifica se o novo email já está em uso
        if (emailMudou && (repository.existsByEmail(dto.getEmail()) || clienteRepository.existsByEmail(dto.getEmail()))) {
            throw new RuntimeException("E-mail já cadastrado em outra conta.");
        }

        boolean loginMudou = false;
        if (dto.getLogin() != null) {
            if (funcionarioExistente.getLogin() == null) {
                loginMudou = true;
            } else {
                loginMudou = !dto.getLogin().equals(funcionarioExistente.getLogin());
            }
        }
        if (loginMudou && (repository.existsByLogin(dto.getLogin()) || clienteRepository.existsByLogin(dto.getLogin()))) {
            throw new RuntimeException("Login já cadastrado em outra conta.");
        }

        // Atualiza a unidade, se fornecida
        if (dto.getIdUnidade() != null) {
            Unidade novaUnidade = unidadeRepository.findById(dto.getIdUnidade())
                    .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));
            funcionarioExistente.setUnidade(novaUnidade);
        }

        // Atualiza os campos do funcionário
        funcionarioExistente.setCargo(dto.getCargo());
        funcionarioExistente.setNomeCompleto(dto.getNomeCompleto());
        funcionarioExistente.setNomeRegistro(dto.getNomeRegistro());
        funcionarioExistente.setEmail(dto.getEmail());
        funcionarioExistente.setTelefone(dto.getTelefone());
        funcionarioExistente.setLogin(dto.getLogin());

        String senhaCripto = null;
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            senhaCripto = passwordEncoder.encode(dto.getSenha());
            funcionarioExistente.setSenhaCriptografada(senhaCripto);
        }

        repository.save(funcionarioExistente);

        // Atualiza o Cliente Espelho correspondente
        Cliente clienteEspelho = clienteRepository.findByCpf(cpfFuncionario)
                .orElseThrow(() -> new RuntimeException("Inconsistência no sistema: Cliente espelho não encontrado para o CPF " + cpfFuncionario));

        clienteEspelho.setNomeCompleto(dto.getNomeCompleto());
        clienteEspelho.setEmail(dto.getEmail());
        clienteEspelho.setTelefone(dto.getTelefone());
        clienteEspelho.setLogin(dto.getLogin());

        if (funcionarioExistente.getUnidade() != null) {
            clienteEspelho.setUnidadeOrigem(funcionarioExistente.getUnidade());
        }

        if (senhaCripto != null) {
            clienteEspelho.setSenhaCriptografada(senhaCripto);
        }

        clienteRepository.save(clienteEspelho);

        log.info("Funcionário ID {} atualizado com sucesso.", id);
        return toResponseDTO(funcionarioExistente);
    }

    @Transactional
    public FuncionarioResponseDTO atualizarParcial(Long id, FuncionarioPatchDTO patchDto) {
        log.info("Atualização parcial do funcionário ID: {}", id);
        Funcionario funcionarioExistente = buscarPorId(id);
        String cpfFuncionario = funcionarioExistente.getCpf();

        Cliente clienteEspelho = clienteRepository.findByCpf(cpfFuncionario)
                .orElseThrow(() -> new RuntimeException("Inconsistência: Cliente espelho não encontrado."));

        if (patchDto.getCargo() != null) {
            funcionarioExistente.setCargo(patchDto.getCargo());
        }

        if (patchDto.getIdUnidade() != null) {
            Unidade novaUnidade = unidadeRepository.findById(patchDto.getIdUnidade())
                    .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));
            funcionarioExistente.setUnidade(novaUnidade);
            clienteEspelho.setUnidadeOrigem(novaUnidade);
        }

        if (patchDto.getNomeCompleto() != null && !patchDto.getNomeCompleto().isBlank()) {
            funcionarioExistente.setNomeCompleto(patchDto.getNomeCompleto());
            clienteEspelho.setNomeCompleto(patchDto.getNomeCompleto());
        }

        if (patchDto.getNomeRegistro() != null && !patchDto.getNomeRegistro().isBlank()) {
            funcionarioExistente.setNomeRegistro(patchDto.getNomeRegistro());
        }

        if (patchDto.getEmail() != null && !patchDto.getEmail().isBlank()) {
            boolean mudou = !patchDto.getEmail().equalsIgnoreCase(funcionarioExistente.getEmail());
            if (mudou && (repository.existsByEmail(patchDto.getEmail()) || clienteRepository.existsByEmail(patchDto.getEmail()))) {
                throw new RuntimeException("E-mail já cadastrado.");
            }
            funcionarioExistente.setEmail(patchDto.getEmail());
            clienteEspelho.setEmail(patchDto.getEmail());
        }

        if (patchDto.getTelefone() != null && !patchDto.getTelefone().isBlank()) {
            funcionarioExistente.setTelefone(patchDto.getTelefone());
            clienteEspelho.setTelefone(patchDto.getTelefone());
        }

        if (patchDto.getLogin() != null && !patchDto.getLogin().isBlank()) {
            boolean mudou = !patchDto.getLogin().equals(funcionarioExistente.getLogin());
            if (mudou && (repository.existsByLogin(patchDto.getLogin()) || clienteRepository.existsByLogin(patchDto.getLogin()))) {
                throw new RuntimeException("Login já cadastrado.");
            }
            funcionarioExistente.setLogin(patchDto.getLogin());
            clienteEspelho.setLogin(patchDto.getLogin());
        }

        if (patchDto.getSenha() != null && !patchDto.getSenha().isBlank()) {
            String senha = passwordEncoder.encode(patchDto.getSenha());
            funcionarioExistente.setSenhaCriptografada(senha);
            clienteEspelho.setSenhaCriptografada(senha);
        }

        if (patchDto.getAtivo() != null) {
            if (patchDto.getAtivo() != funcionarioExistente.isAtivo()) {
                funcionarioExistente.setAtivo(patchDto.getAtivo());
                clienteEspelho.setAtivo(patchDto.getAtivo());
            }
        }

        Funcionario salvo = repository.save(funcionarioExistente);
        clienteRepository.save(clienteEspelho);

        return toResponseDTO(salvo);
    }

    @Transactional
    public void atualizarPin(Long id, String novoPin) {
        // Busca o funcionário pelo ID
        Funcionario f = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));

        // Criptografa o novo PIN antes de salvar
        f.setPinHash(passwordEncoder.encode(novoPin));

        repository.save(f);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Desativando funcionário ID: {}", id);
        Funcionario funcionario = buscarPorId(id);

        if (!funcionario.isAtivo()) {
            throw new RuntimeException("Funcionário já está inativo.");
        }

        // Desativa o funcionário
        funcionario.setAtivo(false);
        repository.save(funcionario);

        // Desativa o cliente espelho correspondente
        clienteRepository.findByCpf(funcionario.getCpf()).ifPresent(c -> {
            c.setAtivo(false);
            clienteRepository.save(c);
        });

        log.info("Funcionário ID {} desativado com sucesso.", id);
    }

    // Atualiza o limite de crédito do funcionário e do cliente espelho
    @Transactional
    public void atualizarLimiteCredito(Long id, java.math.BigDecimal novoLimite) {
        log.info("Atualizando limite de crédito do funcionário ID: {} para R$ {}", id, novoLimite);

        // Atualiza na tabela de Funcionários
        Funcionario funcionario = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado."));

        funcionario.setLimiteCrediario(novoLimite);
        repository.save(funcionario);

        // Atualiza na tabela de Clientes (Para passar na validação da Venda)
        // Buscamos o cliente pelo CPF do funcionário (Cliente Espelho)
        Cliente clienteEspelho = clienteRepository.findByCpf(funcionario.getCpf())
                .orElseThrow(() -> new RuntimeException("Erro crítico: Cliente espelho não encontrado para este funcionário."));

        clienteEspelho.setLimiteCredito(novoLimite);
        clienteRepository.save(clienteEspelho);
    }

    // Converte entidade Funcionario para FuncionarioResponseDTO
    private FuncionarioResponseDTO toResponseDTO(Funcionario f) {
        return new FuncionarioResponseDTO(
                f.getIdFuncionario(),
                f.getCargo(),
                f.getNomeCompleto(),
                f.getNomeRegistro(),
                f.getCpf(),
                f.getEmail(),
                f.getTelefone(),
                f.getLogin(),
                f.getDataNascimento(),
                f.isAtivo()
        );
    }
}