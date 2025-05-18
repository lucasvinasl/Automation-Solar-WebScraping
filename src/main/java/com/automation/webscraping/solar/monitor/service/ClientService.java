package com.automation.webscraping.solar.monitor.service;

import com.automation.webscraping.solar.monitor.model.Client;
import com.automation.webscraping.solar.monitor.repository.ClientRepository;
import org.openqa.selenium.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<Client> findAll(){
        List<Client> clients = clientRepository.findAll();
        if(clients.isEmpty()){
            throw new NotFoundException("Nenhum cliente encontrado");
        }
        return clients;
    }

}
