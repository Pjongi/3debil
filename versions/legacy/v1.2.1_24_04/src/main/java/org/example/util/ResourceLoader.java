package org.example.util;

import org.example.exception.ResourceNotFoundException; // Import nowego wyjątku
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Klasa narzędziowa do ładowania zasobów z classpath.
 */
public class ResourceLoader {

    /**
     * Wczytuje zasób z classpath jako bezpośredni ByteBuffer.
     * UWAGA: Pamięć dla zwróconego bufora musi zostać zwolniona przez wołającego
     * za pomocą MemoryUtil.memFree()!
     *
     * @param resourcePath Ścieżka do zasobu w classpath.
     * @return Bezpośredni ByteBuffer zawierający dane zasobu, przygotowany do odczytu (flipped).
     * @throws ResourceNotFoundException Jeśli zasób nie zostanie znaleziony.
     * @throws IOException Jeśli wystąpi inny błąd odczytu.
     */
    public static ByteBuffer ioResourceToByteBuffer(String resourcePath)
            throws ResourceNotFoundException, IOException {
        ByteBuffer buffer = null;

        try (InputStream source = ResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (source == null) {
                throw new ResourceNotFoundException("Resource not found in classpath: " + resourcePath);
            }
            byte[] bytes = source.readAllBytes();
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
            // Jeśli to już jest ResourceNotFoundException, rzuć dalej
            if (e instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException)e;
            }
            // Inne błędy IO pozostaw jako IOException
            throw e;
        }
    }
}