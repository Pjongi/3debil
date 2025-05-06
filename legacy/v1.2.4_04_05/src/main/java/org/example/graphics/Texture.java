package org.example.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.example.exception.ResourceLoadException;
import org.example.exception.ResourceNotFoundException;
import org.example.util.ResourceLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int textureId;
    private int width;
    private int height;

    /**
     * Ładuje teksturę z pliku obrazu.
     * Próbuje załadować z systemu plików, a następnie z classpath.
     * @param resourcePath Ścieżka do pliku obrazu.
     * @throws ResourceNotFoundException Jeśli plik nie zostanie znaleziony.
     * @throws ResourceLoadException Jeśli wystąpi błąd podczas ładowania lub przetwarzania obrazu.
     */
    public Texture(String resourcePath)
            throws ResourceNotFoundException, ResourceLoadException {

        ByteBuffer imageBuffer = null;
        ByteBuffer fileData = null;

        try {
            // 1. Wczytaj plik obrazu używając ResourceLoader
            fileData = ResourceLoader.ioResourceToByteBuffer(resourcePath);

            // 2. Dekoduj obrazek z pamięci używając STB
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                stbi_set_flip_vertically_on_load(true); // Dla OpenGL
                imageBuffer = stbi_load_from_memory(fileData, w, h, channels, 4); // Wymuś RGBA
                if (imageBuffer == null) {
                    throw new ResourceLoadException("Failed to load texture using STB: " + resourcePath + " - " + stbi_failure_reason());
                }
                this.width = w.get(0);
                this.height = h.get(0);
            }

            // 3. Wygeneruj i skonfiguruj teksturę OpenGL
            this.textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR); // Filtracja trójliniowa
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // Filtracja dwuliniowa

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);

            glGenerateMipmap(GL_TEXTURE_2D); // Generuj mipmapy

            glBindTexture(GL_TEXTURE_2D, 0); // Odwiąż teksturę
            System.out.println("Loaded texture: " + resourcePath + " (" + width + "x" + height + ")");

        } catch (IOException e) {
            if (e instanceof ResourceNotFoundException) throw (ResourceNotFoundException) e;
            throw new ResourceLoadException("IO error loading texture resource: " + resourcePath, e);
        } catch (Exception e) {
            if (e instanceof ResourceLoadException) throw (ResourceLoadException) e;
            if (e instanceof ResourceNotFoundException) throw (ResourceNotFoundException) e;
            throw new ResourceLoadException("Failed to load or process texture: " + resourcePath, e);
        } finally {
            if (imageBuffer != null) stbi_image_free(imageBuffer);
            if (fileData != null) MemoryUtil.memFree(fileData);
        }
    }

    /**
     * Tworzy teksturę programowo z dostarczonych danych (np. domyślna biała).
     * @param width Szerokość tekstury.
     * @param height Wysokość tekstury.
     * @param data ByteBuffer z danymi pikseli w formacie RGBA (powinien być flipped).
     * @param generateMipmaps Czy generować mipmapy (wpływa na filtrowanie).
     */
    public Texture(int width, int height, ByteBuffer data, boolean generateMipmaps) { // Konstruktor z boolean
        if (width <= 0 || height <= 0 || data == null || !data.hasRemaining()) {
            throw new IllegalArgumentException("Invalid parameters for programmatic texture creation.");
        }
        this.width = width;
        this.height = height;
        this.textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        if (generateMipmaps) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Tworzy teksturę programowo z dostarczonych danych (domyślnie bez mipmap).
     * @param width Szerokość tekstury.
     * @param height Wysokość tekstury.
     * @param data ByteBuffer z danymi pikseli w formacie RGBA (powinien być flipped).
     */
    public Texture(int width, int height, ByteBuffer data) { // Konstruktor bez boolean
        this(width, height, data, false); // Wywołaj wersję z boolean generateMipmaps = false
    }

    /**
     * Aktywuje jednostkę teksturującą i wiąże tę teksturę.
     * @param textureUnit Indeks jednostki teksturującej (0, 1, 2, ...).
     */
    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Odwiązuje teksturę 2D od aktualnie aktywnej jednostki teksturującej.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Zwalnia zasoby OpenGL powiązane z teksturą. */
    public void cleanup() {
        glDeleteTextures(textureId);
    }

    // --- Gettery ---
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTextureId() { return textureId; }
}