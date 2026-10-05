package com.robertosrjr.pedidos.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** Número do pedido exibido ao cliente: PED- seguido de 8 dígitos. */
public record OrderNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("PED-\\d{8}");

    public OrderNumber {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Número de pedido inválido: esperado PED-00000000");
        }
    }
}
