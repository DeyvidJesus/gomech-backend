package com.gomech.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Setter
@Getter
@Entity
public class Cliente {
    private String nome;

    private String documento;

    private String telefone;

    private String email;

    private String endereco;

    private Date dataNascimento;

    private String observacoes;

//    @Getter
//    @Setter
//    private List<Veiculo> veiculos;

    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Cliente() {}

    public Cliente(int id, String nome, String documento, String telefone, String email, String endereco, Date dataNascimento, String observacoes) {
        this.id = (long) id;
        this.nome = nome;
        this.documento = documento;
        this.telefone = telefone;
        this.email = email;
        this.endereco = endereco;
        this.dataNascimento = dataNascimento;
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return "Cliente{" + "id=" + id + ", nome='" + nome + '\'' + ", telefone='" + telefone + '\'' + '}';
    }

}
