package com.apirest.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "venda_evidencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_venda", nullable = false)
    private Venda venda;

    @Column(name = "assinatura_base64", columnDefinition = "TEXT")
    private String assinaturaBase64;

    @Column(name = "foto_comprovante_base64", columnDefinition = "TEXT")
    private String fotoComprovanteBase64;

    @Column(name = "data_registro")
    private LocalDateTime dataRegistro;
}