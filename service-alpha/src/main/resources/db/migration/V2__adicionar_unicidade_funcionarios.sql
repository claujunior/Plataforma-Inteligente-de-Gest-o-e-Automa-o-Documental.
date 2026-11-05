ALTER TABLE funcionarios
    ADD CONSTRAINT uk_funcionarios_nome UNIQUE (nome),
    ADD CONSTRAINT uk_funcionarios_numero_telefone UNIQUE (numero_telefone),
    ADD CONSTRAINT uk_funcionarios_email UNIQUE (email);
