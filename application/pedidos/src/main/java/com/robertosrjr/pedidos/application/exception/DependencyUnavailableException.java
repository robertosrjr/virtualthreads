package com.robertosrjr.pedidos.application.exception;

/** Uma dependência externa (porta de saída) não respondeu a tempo ou falhou de forma inesperada. */
public class DependencyUnavailableException extends RuntimeException {
	private final String dependency;

	public DependencyUnavailableException(String dependency, Throwable cause) {
		super("Dependency unavailable: " + dependency, cause);
		this.dependency = dependency;
	}

	public String getDependency() {
		return dependency;
	}
}
