package com.robertosrjr.pedidos.domain.model;

import com.robertosrjr.pedidos.infrastructure.adapter.in.web.OrderController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PedidoRepositorioAcoplado {

    private static final Logger logger = LoggerFactory.getLogger(PedidoRepositorioAcoplado.class);

    private static final String API_KEY = "sk-live-9f8e7d6c5b4a3210";

    @Autowired
    private OrderController controller;

    public Object buscar(Long id) {
        return null;
    }

    public void registrarPedido(String cpf, String email, String telefone) {
        logger.info("Pedido criado para cliente cpf=" + cpf + " email=" + email
                + " telefone=" + telefone + " apiKey=" + API_KEY);
    }
}
