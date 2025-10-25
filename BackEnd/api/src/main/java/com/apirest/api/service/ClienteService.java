package com.apirest.api.service;

import com.apirest.api.dto.ClienteDTO;
import com.apirest.api.dto.ClientePatchDTO;
import com.apirest.api.dto.ClienteResponseDTO;
import com.apirest.api.entity.Cliente;
import com.apirest.api.repository.ClienteRepository;
import com.apirest.api.repository.FuncionarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    // metodo para limpar CPF (remover pontos e traços)
    private String limparCPF(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^\\d]", "");
    }

    // criar novo cliente
    @Transactional
    public ClienteResponseDTO criar(ClienteDTO dto) {
        // limpa CPF
        String cpfLimpo = limparCPF(dto.getCpf());
        // validação de email
        if (repository.existsByEmail(dto.getEmail()) || funcionarioRepository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email já cadastrado.");
        // validação de CPF
        if (repository.existsByCpf(cpfLimpo) || funcionarioRepository.existsByCpf(cpfLimpo))
            throw new RuntimeException("CPF já cadastrado.");
        // validação de login
        if (repository.existsByLogin(dto.getLogin()) || funcionarioRepository.existsByLogin(dto.getLogin()))
            throw new RuntimeException("Login já cadastrado.");

        // criptografa senha
        String senhaHash = passwordEncoder.encode(dto.getSenha());

        Cliente cliente = Cliente.builder()
                .nomeCompleto(dto.getNomeCompleto())
                .email(dto.getEmail())
                .cpf(dto.getCpf())
                .telefone(dto.getTelefone())
                .login(dto.getLogin())
                .senhaCriptografada(senhaHash)
                .ativo(true)
                .build();

        Cliente salvo = repository.save(cliente);
        // retorna DTO de resposta
        return new ClienteResponseDTO(
                salvo.getIdCliente(),
                salvo.getNomeCompleto(),
                salvo.getEmail(),
                salvo.getCpf(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.isAtivo()
        );
    }

    @Transactional
    public Cliente criarClienteDeFuncionario(ClienteDTO dto) {

        String senhaHash = passwordEncoder.encode(dto.getSenha());

        Cliente cliente = Cliente.builder()
                .nomeCompleto(dto.getNomeCompleto())
                .email(dto.getEmail())
                .cpf(dto.getCpf())
                .telefone(dto.getTelefone())
                .login(dto.getLogin())
                .senhaCriptografada(senhaHash)
                .ativo(true) // Funcionário nasce como cliente ativo
                .build();

        return repository.save(cliente);
    }

    // listar todos clientes ativos
    public List<Cliente> listarTodos() {
        return repository.findAllByAtivoTrue();
    }

    // buscar cliente por ID
    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID " + id));
    }

    // Atualizar cliente completo
    @Transactional
    public ClienteResponseDTO atualizar(Long id, ClienteDTO dto) {
        // Busca o cliente existente no banco de dados
        Cliente clienteExistente = buscarPorId(id);

        // Verifica se o email foi alterado
        boolean emailMudou = false;
        if (dto.getEmail() != null) {
            if (clienteExistente.getEmail() == null) {
                emailMudou = true;
            } else {
                emailMudou = !dto.getEmail().equalsIgnoreCase(clienteExistente.getEmail());
            }
        }
        if (emailMudou && repository.existsByEmail(dto.getEmail())|| funcionarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado em outra conta.");
        }
        // Verifica se o login foi alterado
        boolean loginMudou = false;
        if (dto.getLogin() != null) {
            if (clienteExistente.getLogin() == null) {
                loginMudou = true;
            } else {
                loginMudou = !dto.getLogin().equals(clienteExistente.getLogin());
            }
        }
        if (loginMudou && repository.existsByLogin(dto.getLogin())|| funcionarioRepository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Login já cadastrado em outra conta.");
        }
        clienteExistente.setNomeCompleto(dto.getNomeCompleto());
        clienteExistente.setEmail(dto.getEmail());
        clienteExistente.setTelefone(dto.getTelefone());
        clienteExistente.setLogin(dto.getLogin());

        // Atualiza a senha somente se uma nova senha for fornecida
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            clienteExistente.setSenhaCriptografada(passwordEncoder.encode(dto.getSenha()));
        }
        // Salva as alterações no banco de dados
        Cliente salvo = repository.save(clienteExistente);

        // Retorna o DTO de resposta atualizado
        return new ClienteResponseDTO(
                salvo.getIdCliente(),
                salvo.getNomeCompleto(),
                salvo.getEmail(),
                salvo.getCpf(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.isAtivo()
        );
    }

    // Atualização parcial do cliente
    @Transactional
    public ClienteResponseDTO atualizarParcial(Long id, ClientePatchDTO patchDto) {
        // Busca o cliente existente no banco de dados
        Cliente clienteExistente = buscarPorId(id);
        // Aplica as atualizações parciais
        if (patchDto.getNomeCompleto() != null && !patchDto.getNomeCompleto().isBlank()) {
            clienteExistente.setNomeCompleto(patchDto.getNomeCompleto());
        }
        // Atualiza o email se fornecido
        if (patchDto.getEmail() != null && !patchDto.getEmail().isBlank()) {
            boolean emailMudouParcial = false;
            if (clienteExistente.getEmail() == null) {
                emailMudouParcial = true;
            } else {
                emailMudouParcial = !patchDto.getEmail().equalsIgnoreCase(clienteExistente.getEmail());
            }

            if (emailMudouParcial && repository.existsByEmail(patchDto.getEmail()) || funcionarioRepository.existsByEmail(patchDto.getEmail())) {
                throw new RuntimeException("E-mail já cadastrado em outra conta.");
            }
            clienteExistente.setEmail(patchDto.getEmail());
        }
        // Atualiza o telefone se fornecido
        if (patchDto.getTelefone() != null && !patchDto.getTelefone().isBlank()) {
            clienteExistente.setTelefone(patchDto.getTelefone());
        }
        // Atualiza o login se fornecido
        if (patchDto.getLogin() != null && !patchDto.getLogin().isBlank()) {
            boolean loginMudouParcial = false;
            if (clienteExistente.getLogin() == null) {
                loginMudouParcial = true;
            } else {
                loginMudouParcial = !patchDto.getLogin().equals(clienteExistente.getLogin());
            }

            if (loginMudouParcial && repository.existsByLogin(patchDto.getLogin()) || funcionarioRepository.existsByLogin(patchDto.getLogin())) {
                throw new RuntimeException("Login já cadastrado em outra conta.");
            }
            clienteExistente.setLogin(patchDto.getLogin());
        }
        // Atualiza a senha se fornecida
        if (patchDto.getSenha() != null && !patchDto.getSenha().isBlank()) {
            clienteExistente.setSenhaCriptografada(passwordEncoder.encode(patchDto.getSenha()));
        }
        // Salva as alterações no banco de dados
        Cliente salvo = repository.save(clienteExistente);

        // Retorna o DTO de resposta atualizado
        return new ClienteResponseDTO(
                salvo.getIdCliente(),
                salvo.getNomeCompleto(),
                salvo.getEmail(),
                salvo.getCpf(),
                salvo.getTelefone(),
                salvo.getLogin(),
                salvo.isAtivo()
        );
    }

    // Deletar cliente (marcar como inativo)
    @Transactional
    public void deletar(Long id) {
        // Busca o cliente pelo ID
        Cliente cliente = buscarPorId(id);
        // Verifica se o cliente já está inativo
        if (!cliente.isAtivo()) {
            throw new RuntimeException("Cliente já está inativo.");
        }
        // Marca o cliente como inativo
        cliente.setAtivo(false);
        repository.save(cliente);
    }
}

