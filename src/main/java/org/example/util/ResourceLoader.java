package org.example.util;

import org.example.exception.ResourceNotFoundException;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Klasa narzędziowa do ładowania zasobów z classpath.
 */
public class ResourceLoader {

    /**
     * Wczytuje zasób z classpath jako bezpośredni ByteBuffer.
     * UWAGA: Pamięć dla zwróconego bufora musi zostać zwolniona przez wołającego
     * za pomocą MemoryUtil.memFree()!
     *
     * @param classpathResourcePath Ścieżka do zasobu względna do roota classpath
     *                              (np. "textures/stone.png", "models/bunny.obj").
     * @return Bezpośredni ByteBuffer zawierający dane zasobu, przygotowany do odczytu (flipped).
     * @throws ResourceNotFoundException Jeśli zasób nie zostanie znaleziony w classpath.
     * @throws IOException Jeśli wystąpi inny błąd odczytu.
     */
    public static ByteBuffer ioResourceToByteBuffer(String classpathResourcePath)
            throws ResourceNotFoundException, IOException {

        ByteBuffer buffer = null;

        // Użyj ClassLoadera do znalezienia zasobu
        // getResourceAsStream oczekuje ścieżki względnej do roota classpath
        try (InputStream source = ResourceLoader.class.getClassLoader().getResourceAsStream(classpathResourcePath)) {

            if (source == null) {
                throw new ResourceNotFoundException("Resource not found in classpath: " + classpathResourcePath);
            }

            // Wczytaj wszystkie bajty z InputStream (prostsze dla zasobów)
            byte[] bytes = source.readAllBytes(); // Dostępne od Java 9+

            // Zaalokuj bezpośredni bufor i skopiuj dane
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip(); // Przygotuj bufor do odczytu

            return buffer;

        } catch (IOException e) {
            // Jeśli bufor został częściowo zaalokowany przed błędem IO
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
            // Jeśli to ResourceNotFoundException rzucony wyżej, rzuć go dalej
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) e;
            }
            // Inne błędy IO
            throw new IOException("IOException reading resource from classpath: " + classpathResourcePath, e);
        } catch (NullPointerException e) {
            // getResourceAsStream zwraca null, co oznacza brak zasobu
            throw new ResourceNotFoundException("Resource not found in classpath: " + classpathResourcePath, e);
        }
        // Catch OutOfMemoryError jeśli readAllBytes zawiedzie dla dużych plików
        catch (OutOfMemoryError e) {
            if (buffer != null) MemoryUtil.memFree(buffer);
            throw new IOException("OutOfMemoryError reading resource: " + classpathResourcePath + ". Resource might be too large.", e);
        }
    }
}