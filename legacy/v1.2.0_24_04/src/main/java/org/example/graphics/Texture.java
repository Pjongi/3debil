package org.example.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil; // Potrzebny do zwalniania bufora pliku

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int textureId;
    private int width; // Usuń final, bo ustawiamy w loadFromFile/constructor
    private int height;// Usuń final, bo ustawiamy w loadFromFile/constructor

    // Konstruktor ładujący z pliku w classpath
    public Texture(String classpathResourcePath) throws Exception {
        ByteBuffer imageBuffer = null;
        ByteBuffer fileData = null; // Bufor na dane z pliku

        try {
            // Wczytaj zasób z classpath do ByteBuffer
            fileData = ioResourceToByteBuffer(classpathResourcePath);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                // Dekoduj obrazek z pamięci
                // stbi_set_flip_vertically_on_load(true); // Może być potrzebne
                imageBuffer = stbi_load_from_memory(fileData, w, h, channels, 4); // Wymuś RGBA
                if (imageBuffer == null) {
                    throw new Exception("Failed to load texture from memory: " + classpathResourcePath + " - " + stbi_failure_reason());
                }

                this.width = w.get(0);
                this.height = h.get(0);
            }

            // Wygeneruj ID tekstury i załaduj dane do OpenGL
            this.textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
            glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, 0);
            System.out.println("Loaded texture: " + classpathResourcePath);

        } finally {
            // Zwolnij pamięć buforów
            if (imageBuffer != null) {
                stbi_image_free(imageBuffer);
            }
            if (fileData != null) {
                MemoryUtil.memFree(fileData); // Zwolnij bufor pliku
            }
        }
    }

    // Konstruktor dla tekstury tworzonej programowo (np. domyślna biała)
    public Texture(int width, int height, ByteBuffer data) {
        this.width = width;
        this.height = height;
        this.textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // Metoda pomocnicza do wczytania zasobu jako ByteBuffer
    private ByteBuffer ioResourceToByteBuffer(String resource) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = Texture.class.getClassLoader().getResourceAsStream(resource)) {
            if (source == null) {
                throw new IOException("Resource not found in classpath: " + resource);
            }
            byte[] bytes = source.readAllBytes();
            // Alokuj bezpośredni ByteBuffer poza stosem!
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
        }
        return buffer; // Pamiętaj o zwolnieniu przez MemoryUtil.memFree()!
    }


    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTextureId() { return textureId; }
}