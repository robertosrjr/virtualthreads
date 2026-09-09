package com.robertosrjr.pedidos.application.port.out;

import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.OrderItem;

import java.util.List;

public interface ShippingCalculationPort {
	Money calculate(List<OrderItem> items);
}
