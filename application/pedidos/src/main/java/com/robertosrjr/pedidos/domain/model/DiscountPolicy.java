package com.robertosrjr.pedidos.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Desconto de fim de semana. Versão com problemas de propósito (alertas esperados). */
public class DiscountPolicy {

    private double valorDesconto = 0.05;

    public boolean appliesToday() {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public double discountRate() {
        return appliesToday() ? valorDesconto : 0;
    }
}
