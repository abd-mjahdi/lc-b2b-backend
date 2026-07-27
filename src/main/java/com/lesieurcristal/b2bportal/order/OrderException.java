package com.lesieurcristal.b2bportal.order;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrderException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public OrderException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static OrderException notFound(String orderNumber) {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Commande introuvable: " + orderNumber);
    }

    public static OrderException accessDenied() {
        return new OrderException(HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED", "Accès non autorisé à cette commande");
    }

    public static OrderException noCustomerAssociated() {
        return new OrderException(HttpStatus.BAD_REQUEST, "NO_CUSTOMER_ASSOCIATED", "Aucun numéro client associé à cet utilisateur");
    }
}
