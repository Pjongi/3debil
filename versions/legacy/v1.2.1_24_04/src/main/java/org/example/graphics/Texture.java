package org.example.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.example.exception.ResourceLoadException;      // Import nowych wyjątków
import org.example.exception.ResourceNotFoundException; // Import nowych wyjątków
import org.example.util.ResourceLoader;                 // Import klasy narzędziowej

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
     * Ładuje teksturę z pliku w classpath.
     * @param classpathResourcePath Ścieżka do pliku obrazu.
     * @throws ResourceNotFoundException Jeśli plik nie zostanie znaleziony.
     * @throws ResourceLoadException Jeśli wystąpi błąd podczas ładowania lub przetwarzania obrazu.
     */
    public Texture(String classpathResourcePath)
            throws ResourceNotFoundException, ResourceLoadException {

        ByteBuffer imageBuffer = null;
        ByteBuffer fileData = null;

        try {
            // 1. Wczytaj plik obrazu używając ResourceLoader
            fileData = ResourceLoader.ioResourceToByteBuffer(classpathResourcePath); // Może rzucić ResourceNotFoundException lub IOException
            // System.out.println("DEBUG: Loaded image file data for: " + classpathResourcePath);

            // 2. Dekoduj obrazek z pamięci używając STB
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1); IntBuffer h = stack.mallocInt(1); IntBuffer channels = stack.mallocInt(1);

                stbi_set_flip_vertically_on_load(true); // Często potrzebne dla OpenGL
                imageBuffer = stbi_load_from_memory(fileData, w, h, channels, 4); // Wymuś RGBA
                if (imageBuffer == null) {
                    throw new ResourceLoadException("Failed to load texture using STB: " + classpathResourcePath + " - " + stbi_failure_reason());
                }
                this.width = w.get(0); this.height = h.get(0);
            }

            // 3. Wygeneruj i skonfiguruj teksturę OpenGL
            this.textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR); // Filtracja z mipmapami
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);             // Filtracja liniowa przy powiększeniu

            // Prześlij dane obrazu do OpenGL
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
            // Wygeneruj mipmapy
            glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, 0); // Odwiąż teksturę
            System.out.println("Loaded texture: " + classpathResourcePath + " (" + width + "x" + height + ")");

        } catch (IOException e) { // Złap IO z ResourceLoader (inny niż NotFound)
            throw new ResourceLoadException("IO error loading texture file: " + classpathResourcePath, e);
            // ResourceNotFoundException i ResourceLoadException są propagowane
        } finally {
            // 4. Zwolnij pamięć buforów
            if (imageBuffer != null) {
                stbi_image_free(imageBuffer); // Ważne, aby zwolnić pamięć alokowaną przez STB
            }
            if (fileData != null) {
                MemoryUtil.memFree(fileData); // Zawsze zwalniaj bufor z ResourceLoader
                // System.out.println("DEBUG: Freed image file buffer for: " + classpathResourcePath);
            }
        }
    }

    /**
     * Tworzy teksturę programowo z dostarczonych danych (np. domyślna biała).
     * @param width Szerokość tekstury.
     * @param height Wysokość tekstury.
     * @param data ByteBuffer z danymi pikseli w formacie RGBA.
     */
    public Texture(int width, int height, ByteBuffer data) {
        this.width = width;
        this.height = height;
        this.textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        // Uproszczona filtracja dla programowo tworzonych tekstur (bez mipmap)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        // Rzadko potrzebne, zwykle wystarczy związać inną teksturę lub 0
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTextureId() { return textureId; }
}