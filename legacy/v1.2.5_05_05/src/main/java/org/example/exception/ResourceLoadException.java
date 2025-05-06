package org.example.exception;

/**
 * Wyjątek rzucany, gdy wystąpi błąd podczas ładowania, parsowania
 * lub przetwarzania zasobu (np. błąd formatu pliku, błąd kompilacji shadera).
 */
public class ResourceLoadException extends Exception {
    public ResourceLoadException(String message) {
        super(message);
    }

    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}