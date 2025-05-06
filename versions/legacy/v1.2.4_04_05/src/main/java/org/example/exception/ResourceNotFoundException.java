package org.example.exception;

import java.io.IOException;

/**
 * Wyjątek rzucany, gdy nie można znaleźć zasobu (np. pliku).
 * Rozszerza IOException, aby zachować zgodność z operacjami plikowymi.
 */
public class ResourceNotFoundException extends IOException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}