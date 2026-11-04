CREATE TABLE funcionarios (
                              cpf             VARCHAR(11) PRIMARY KEY,
                              nome            VARCHAR(150) NOT NULL,
                              numero_telefone VARCHAR(20),
                              email           VARCHAR(254),
                              salario         NUMERIC(12, 2) NOT NULL
);