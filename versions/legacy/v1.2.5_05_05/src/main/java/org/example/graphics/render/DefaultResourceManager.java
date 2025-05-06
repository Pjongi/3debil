package org.example.graphics.render;

import org.example.exception.ResourceLoadException;
import org.example.graphics.Material;
import org.example.graphics.Texture;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Zarządza tworzeniem i udostępnianiem domyślnych zasobów renderowania,
 * takich jak domyślna tekstura (np. biały piksel) i domyślny materiał.
 */
public class DefaultResourceManager {

    private Texture defaultTexture;
    private Material defaultMaterial;

    public void init() throws ResourceLoadException {
        System.out.println("  DefaultResourceManager: Initializing...");
        try {
            // Tekstura (biały piksel)
            ByteBuffer whitePixel = null;
            try {
                whitePixel = MemoryUtil.memAlloc(4).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
                // Użyj konstruktora z boolean: nie generuj mipmap dla 1x1 tekstury
                defaultTexture = new Texture(1, 1, whitePixel, false);
                System.out.println("    Default texture created (ID: " + defaultTexture.getTextureId() + ").");
            } finally {
                if (whitePixel != null) MemoryUtil.memFree(whitePixel);
            }

            // Materiał
            defaultMaterial = new Material(); // Używa domyślnych kolorów
            System.out.println("    Default material created.");

        } catch (Exception e) {
            cleanup(); // Posprzątaj, jeśli coś się częściowo udało
            throw new ResourceLoadException("Failed to create default render resources", e);
        }
        System.out.println("  DefaultResourceManager: Initialized successfully.");
    }

    public Texture getDefaultTexture() {
        if (defaultTexture == null) {
            throw new IllegalStateException("Default texture accessed before initialization.");
        }
        return defaultTexture;
    }

    public Material getDefaultMaterial() {
        if (defaultMaterial == null) {
            throw new IllegalStateException("Default material accessed before initialization.");
        }
        return defaultMaterial;
    }

    public void cleanup() {
        System.out.println("  DefaultResourceManager: Cleaning up...");
        if (defaultTexture != null) {
            defaultTexture.cleanup();
            defaultTexture = null;
            System.out.println("    Default texture cleaned.");
        }
        defaultMaterial = null; // Materiał nie wymaga specjalnego cleanup GPU
        System.out.println("  DefaultResourceManager: Cleanup complete.");
    }
}