package com.gomech.service;

import com.gomech.model.Client;
import com.gomech.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repository;

    public Client save(Client client) {
        return repository.save(client);
    }

    public List<Client> listAll() {
        return repository.findAll();
    }


    public Optional<Client> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Client update(Long id, Client updatedClient) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        client.setName(updatedClient.getName());
        client.setDocument(updatedClient.getDocument());
        client.setEmail(updatedClient.getEmail());
        client.setPhone(updatedClient.getPhone());
        return repository.save(client);
    }
}
