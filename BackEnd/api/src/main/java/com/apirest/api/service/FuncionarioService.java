package com.apirest.api.service;

import com.apirest.api.dto.*;
import com.apirest.api.entity.Cargo;
import com.apirest.api.entity.Cliente;
import com.apirest.api.entity.Funcionario;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.service.ClienteService;
import com.apirest.api.dto.ClienteDTO;
import com.apirest.api.repository.FuncionarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;

    private static final Set<Cargo> PERMISSAO_CRIAR_FUNCIONARIO = Set.of(
            Cargo.DONO, Cargo.GERENTE, Cargo.LIDER_VENDA, Cargo.ADMIN
    );

    private String limparCPF(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^\\d]", ""); // Remove tudo que não for dígito
    }

    public FuncionarioResponseDTO criar(FuncionarioDTO dto) {

        String cpfLimpo = limparCPF(dto.getCpf());

        // Validação de duplicidade
        if (repository.existsByEmail(dto.getEmail())|| clienteRepository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("E-mail já cadastrado.");
        if (repository.existsByCpf(cpfLimpo)|| clienteRepository.existsByCpf(cpfLimpo))
            throw new RuntimeException("CPF já cadastrado.");

        if (repository.existsByLogin(dto.getLogin())|| clienteRepository.existsByLogin(dto.getLogin()))
            throw new RuntimeException("Login já cadastrado.");

        if (dto.getDataNascimento() != null) {
            int idade = Period.between(dto.getDataNascimento(), LocalDate.now()).getYears();
            if (idade < 18) {
                throw new RuntimeException("O funcionário deve ter pelo menos 18 anos de idade.");
            }
        } else {
            // Data de nascimento é obrigatória
            throw new RuntimeException("Data de nascimento é obrigatória.");
        }

        // Criptografa a senha
        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());

        Funcionario funcionario = Funcionario.builder()
                .cargo(dto.getCargo())
                .nomeCompleto(dto.getNomeCompleto())
                .nomeRegistro(dto.getNomeRegistro())
                .cpf(dto.getCpf())
                .email(dto.getEmail())
                .dataNascimento(dto.getDataNascimento())
                .telefone(dto.getTelefone())
                .login(dto.getLogin())
                .senhaCriptografada(senhaCriptografada)
                .build();

        Funcionario salvo = repository.save(funcionario);

        // Cria o cliente correspondente
        ClienteDTO clienteDtoParaFuncionario = new ClienteDTO();
        clienteDtoParaFuncionario.setNomeCompleto(salvo.getNomeCompleto());
        clienteDtoParaFuncionario.setDataNascimento(salvo.getDataNascimento());
        clienteDtoParaFuncionario.setEmail(salvo.getEmail());
        clienteDtoParaFuncionario.setCpf(salvo.getCpf());
        clienteDtoParaFuncionario.setTelefone(salvo.getTelefone());
        clienteDtoParaFuncionario.setLogin(salvo.getLogin());
        clienteDtoParaFuncionario.setSenha(dto.getSenha()); // Usa a mesma senha do funcionário

        // Tenta criar o cliente e captura possíveis exceções
        try {
            clienteService.criarClienteDeFuncionario(clienteDtoParaFuncionario);
        } catch (Exception e) {
            // Se falhar, lança uma exceção e desfaz a criação do funcionário
            throw new RuntimeException("Falha ao criar o cliente correspondente para o funcionário.", e);
        }

        // Retorna o DTO de resposta
        return new FuncionarioResponseDTO(
                salvo.getIdFuncionario(),
                salvo.getCargo(),
                salvo.getNomeCompleto(),
                salvo.getNomeRegistro(),
                salvo.getCpf(),
                salvo.getEmail(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.getDataNascimento(),
                salvo.isAtivo()
        );
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
        Funcionario funcionarioExistente = buscarPorId(id);

        String cpfFuncionario = funcionarioExistente.getCpf();

        boolean emailMudou = false;
        if (dto.getEmail() != null) {
            if (funcionarioExistente.getEmail() == null) {
                emailMudou = true;
            } else {
                emailMudou = !dto.getEmail().equalsIgnoreCase(funcionarioExistente.getEmail());
            }
        }
        if (emailMudou && repository.existsByEmail(dto.getEmail()) || clienteRepository.existsByEmail(dto.getEmail())) {
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
        if (loginMudou && repository.existsByLogin(dto.getLogin())|| clienteRepository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Login já cadastrado em outra conta.");
        }

        // Data de nascimento e CPF não são atualizados
        funcionarioExistente.setCargo(dto.getCargo());
        funcionarioExistente.setNomeCompleto(dto.getNomeCompleto());
        funcionarioExistente.setNomeRegistro(dto.getNomeRegistro());
        funcionarioExistente.setEmail(dto.getEmail());
        funcionarioExistente.setTelefone(dto.getTelefone());
        funcionarioExistente.setLogin(dto.getLogin());

        // Atualiza a senha somente se uma nova senha for fornecida
        String senhaCripto = null;
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            senhaCripto = passwordEncoder.encode(dto.getSenha());
            funcionarioExistente.setSenhaCriptografada(senhaCripto);
        }

        // Busca o cliente-espelho correspondente
        Cliente clienteEspelho = clienteRepository.findByCpf(cpfFuncionario)
                .orElseThrow(() -> new RuntimeException("Falha de consistência: O cliente-espelho do funcionário com CPF " + cpfFuncionario + " não foi encontrado."));

        // Atualiza os dados do cliente-espelho
        clienteEspelho.setNomeCompleto(dto.getNomeCompleto());
        clienteEspelho.setEmail(dto.getEmail());
        clienteEspelho.setTelefone(dto.getTelefone());
        clienteEspelho.setLogin(dto.getLogin());
        if (senhaCripto != null) {
            clienteEspelho.setSenhaCriptografada(senhaCripto);
        }

        // Salva as alterações no funcionário e no cliente-espelho
        repository.save(funcionarioExistente);
        clienteRepository.save(clienteEspelho);

        Funcionario salvo = repository.save(funcionarioExistente);

        return new FuncionarioResponseDTO(
                salvo.getIdFuncionario(),
                salvo.getCargo(),
                salvo.getNomeCompleto(),
                salvo.getNomeRegistro(),
                salvo.getCpf(),
                salvo.getEmail(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.getDataNascimento(),
                salvo.isAtivo()
        );
    }


    @Transactional
    public FuncionarioResponseDTO atualizarParcial(Long id, FuncionarioPatchDTO patchDto) {
        Funcionario funcionarioExistente = buscarPorId(id);

        // Obtém o CPF do funcionário antes de qualquer modificação
        String cpfFuncionario = funcionarioExistente.getCpf();

        // Busca o cliente-espelho correspondente
        Cliente clienteEspelho = clienteRepository.findByCpf(cpfFuncionario)
                .orElseThrow(() -> new RuntimeException("Falha de consistência: O cliente-espelho do funcionário com CPF " + cpfFuncionario + " não foi encontrado."));


        if (patchDto.getCargo() != null) {
            funcionarioExistente.setCargo(patchDto.getCargo());
        }

        if (patchDto.getNomeCompleto() != null && !patchDto.getNomeCompleto().isBlank()) {
            funcionarioExistente.setNomeCompleto(patchDto.getNomeCompleto());
            clienteEspelho.setNomeCompleto(patchDto.getNomeCompleto()); // Sincroniza
        }

        if (patchDto.getNomeRegistro() != null && !patchDto.getNomeRegistro().isBlank()) {
            funcionarioExistente.setNomeRegistro(patchDto.getNomeRegistro()); // Corrigido (era setAnoRegistro)
        }

        if (patchDto.getEmail() != null && !patchDto.getEmail().isBlank()) {

            boolean emailMudouParcial = (funcionarioExistente.getEmail() == null) || !patchDto.getEmail().equalsIgnoreCase(funcionarioExistente.getEmail());
            if (emailMudouParcial && (repository.existsByEmail(patchDto.getEmail()) || clienteRepository.existsByEmail(patchDto.getEmail()))) {
                throw new RuntimeException("E-mail já cadastrado em outra conta.");
            }
            // Atualiza o email tanto no funcionário quanto no cliente-espelho
            funcionarioExistente.setEmail(patchDto.getEmail());
            clienteEspelho.setEmail(patchDto.getEmail());
        }

        // Verifica e atualiza o telefone
        if (patchDto.getTelefone() != null && !patchDto.getTelefone().isBlank()) {
            funcionarioExistente.setTelefone(patchDto.getTelefone());
            clienteEspelho.setTelefone(patchDto.getTelefone());
        }

        // Verifica e atualiza o login
        if (patchDto.getLogin() != null && !patchDto.getLogin().isBlank()) {

            boolean loginMudouParcial = (funcionarioExistente.getLogin() == null) || !patchDto.getLogin().equals(funcionarioExistente.getLogin());
            if (loginMudouParcial && (repository.existsByLogin(patchDto.getLogin()) || clienteRepository.existsByLogin(patchDto.getLogin()))) {
                throw new RuntimeException("Login já cadastrado em outra conta.");
            }
            funcionarioExistente.setLogin(patchDto.getLogin());
            clienteEspelho.setLogin(patchDto.getLogin()); // Sincroniza
        }

        if (patchDto.getSenha() != null && !patchDto.getSenha().isBlank()) {
            String senhaCripto = passwordEncoder.encode(patchDto.getSenha());
            funcionarioExistente.setSenhaCriptografada(senhaCripto);
            clienteEspelho.setSenhaCriptografada(senhaCripto); // Sincroniza
        }

        Funcionario salvo = repository.save(funcionarioExistente);
        clienteRepository.save(clienteEspelho);

        return new FuncionarioResponseDTO(
                salvo.getIdFuncionario(),
                salvo.getCargo(),
                salvo.getNomeCompleto(),
                salvo.getNomeRegistro(),
                salvo.getCpf(),
                salvo.getEmail(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.getDataNascimento(),
                salvo.isAtivo()
        );
    }

    @Transactional
    public void deletar(Long id) {
        // Deleção lógica
        Funcionario funcionario = buscarPorId(id);
        if (!funcionario.isAtivo()) {
            throw new RuntimeException("Funcionário já está inativo.");
        }
        // Marca como inativo
        funcionario.setAtivo(false);
        // Salva a alteração
        repository.save(funcionario);
    }
}

