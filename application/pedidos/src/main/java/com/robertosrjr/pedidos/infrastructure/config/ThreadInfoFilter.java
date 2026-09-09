package com.robertosrjr.pedidos.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ThreadInfoFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		boolean isVirtual = Thread.currentThread().isVirtual();
		String threadType = isVirtual ? "virtual" : "platform";
		response.addHeader("X-Thread-Type", threadType);
		filterChain.doFilter(request, response);
	}
}
