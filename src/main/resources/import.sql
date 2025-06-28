-- Table creation is handled by Hibernate based on entity annotations
INSERT INTO inverter_manufacturer(id, name, portal_url) VALUES (1, 'GROWATT', 'https://server.growatt.com/?lang=pt');
INSERT INTO inverter_manufacturer(id, name, portal_url) VALUES (2, 'SOLIS', 'https://soliscloud.com/#/homepage');
INSERT INTO inverter_manufacturer(id, name, portal_url) VALUES (3, 'SUNGROW', 'https://isolarcloud.com/#/login');

-- INSERT INTO client(name, username, password, inverter_manufacturer_id) VALUES ('Lucimar Alves de Brito Dias', 'lucimarbrito', 'solar123', 1);
-- INSERT INTO client(name, username, password, inverter_manufacturer_id) VALUES ('Rodrigo Assano/Francisca Elisabetes', 'anderson.alexandre@gmail.com', 'sol@r123', 2);
-- INSERT INTO client(name, username, password, inverter_manufacturer_id) VALUES ('Jackson Gomes de Oliveira', 'jakson117@live.com', 'Sol@r!23', 3);
--INSERT INTO client(name, username, password, inverter_manufacturer_id) VALUES ('Ethan Comércio Atacadista de Confecções Eireli', 'ethancomercio', 'solar123', 1);
