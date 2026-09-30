package com.robertosrjr.pedidos.domain.model;

import org.springframework.stereotype.Component;

/** Gerador de número de pedido anotado com Spring no domínio (deve ser bloqueado). */
@Component
public class OrderNumberGenerator {

    private long sequence;

    public synchronized OrderNumber next() {
        sequence++;
        return new OrderNumber(String.format("PED-%08d", sequence));
    }
}
