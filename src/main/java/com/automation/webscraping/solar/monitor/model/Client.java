package com.automation.webscraping.solar.monitor.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode
public class Client {
    /*
    Informações Relevantes de Início
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @ManyToOne
    @JoinColumn(name = "inverter_manufacturer_id", nullable = false)
    private InverterManufacturer inverterManufacturer;

    /*
    Isso vai ser implementado depois
    */
    /*
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zip;
     */

}
