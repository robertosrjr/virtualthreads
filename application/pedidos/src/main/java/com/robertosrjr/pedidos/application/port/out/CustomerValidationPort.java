package com.robertosrjr.pedidos.application.port.out;

import java.util.UUID;

public interface CustomerValidationPort {
	void validate(UUID customerId);
}
