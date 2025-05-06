package org.example.graphics.shadow;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;

/**
 * Zarządza Framebuffer Object (FBO) i teksturą sześcienną (Cube Map)
 * do przechowywania mapy głębi dla cieni rzucanych przez SpotLight (lub PointLight).
 */
public class SpotLightShadowMap {

    public static final int SHADOW_MAP_WIDTH = 1024; // Rozdzielczość mapy cieni (można dostosować)
    public static final int SHADOW_MAP_HEIGHT = 1024;

    private final int depthMapFBO;
    private final int depthCubeMapTexture; // ID tekstury cube mapy głębi

    public SpotLightShadowMap() throws Exception {
        // 1. Utwórz Framebuffer Object (FBO)
        depthMapFBO = glGenFramebuffers();

        // 2. Utwórz teksturę Cube Map dla głębi
        depthCubeMapTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthCubeMapTexture);

        for (int i = 0; i < 6; ++i) {
            // GL_TEXTURE_CUBE_MAP_POSITIVE_X + i daje kolejne ściany: +X, -X, +Y, -Y, +Z, -Z
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_DEPTH_COMPONENT,
                    SHADOW_MAP_WIDTH, SHADOW_MAP_HEIGHT, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE); // Dla cube mapy potrzebny też WRAP_R

        // 3. Dołącz teksturę cube mapy jako załącznik głębi do FBO
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        // Dołączamy całą cube mapę, a potem w pętli renderowania będziemy wybierać konkretną ścianę
        // za pomocą glFramebufferTexture2D z odpowiednim celem (np. GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)
        // LUB używamy glFramebufferTexture, która jest bardziej generyczna (ale może być mniej wspierana na starym sprzęcie)
        // Na razie zostawimy to tak, będziemy dołączać konkretne ściany w pętli renderowania.
        // Alternatywnie, można by użyć glFramebufferTextureLayer dla każdej ściany,
        // ale uprośćmy i dołączajmy dynamicznie przed renderowaniem każdej ściany.
        // Na razie wystarczy samo powiązanie FBO, konkretna ściana będzie dołączana w metodzie `bindForWritingToFace`.

        // Określ, że nie będziemy renderować do żadnego bufora koloru
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        // Sprawdź kompletność FBO (bez dołączonej tekstury jeszcze nie będzie kompletny)
        // Sprawdzenie kompletności wykonamy po dołączeniu pierwszej ściany.
        // if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
        //     glBindFramebuffer(GL_FRAMEBUFFER, 0); // Odwiąż przed rzuceniem wyjątku
        //     throw new Exception("Could not create FrameBuffer for SpotLight shadow map. Status: " + glCheckFramebufferStatus(GL_FRAMEBUFFER));
        // }

        glBindFramebuffer(GL_FRAMEBUFFER, 0); // Odwiąż FBO
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0); // Odwiąż teksturę
    }

    /**
     * Wiąże FBO i konkretną ścianę cube mapy jako cel renderowania.
     * @param faceIndex Indeks ściany (0 do 5, odpowiadający GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)
     */
    public void bindForWritingToFace(int faceIndex) {
        if (faceIndex < 0 || faceIndex > 5) {
            throw new IllegalArgumentException("Invalid face index for cube map: " + faceIndex);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        // Dołącz konkretną ścianę cube mapy do FBO
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_CUBE_MAP_POSITIVE_X + faceIndex, depthCubeMapTexture, 0);

        // Sprawdź kompletność FBO po dołączeniu ściany (ważne przy pierwszym użyciu)
        // Można to robić tylko raz, np. w konstruktorze po dołączeniu pierwszej ściany, ale dla bezpieczeństwa
        int fboStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (fboStatus != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("SpotLightShadowMap FBO not complete! Status: " + fboStatus + " for face " + faceIndex);
            // Można rzucić wyjątek, jeśli to krytyczne
            // throw new RuntimeException("SpotLightShadowMap FBO not complete! Status: " + fboStatus);
        }

        glViewport(0, 0, SHADOW_MAP_WIDTH, SHADOW_MAP_HEIGHT); // Ustaw viewport na rozmiar mapy cieni
        glClear(GL_DEPTH_BUFFER_BIT); // Wyczyść bufor głębi
    }

    public void unbindAfterWriting(int windowWidth, int windowHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, windowWidth, windowHeight); // Przywróć oryginalny viewport
    }

    public int getDepthCubeMapTexture() {
        return depthCubeMapTexture;
    }

    public void cleanup() {
        glDeleteFramebuffers(depthMapFBO);
        glDeleteTextures(depthCubeMapTexture);
    }

    /**
     * Zwraca tablicę 6 macierzy widoku (view matrices) dla każdej ściany cube mapy,
     * wygenerowanych z perspektywy światła.
     * @param lightPosition Pozycja światła.
     * @return Tablica 6 macierzy widoku.
     */
    public static Matrix4f[] getCubeMapViewMatrices(Vector3f lightPosition) {
        Matrix4f[] lightViews = new Matrix4f[6];
        // Kolejność zgodna z GL_TEXTURE_CUBE_MAP_POSITIVE_X do GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
        // +X (Right)
        lightViews[0] = new Matrix4f().lookAt(lightPosition, new Vector3f(lightPosition).add(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f));
        // -X (Left)
        lightViews[1] = new Matrix4f().lookAt(lightPosition, new Vector3f(lightPosition).add(-1.0f, 0.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f));
        // +Y (Top)
        lightViews[2] = new Matrix4f().lookAt(lightPosition, new Vector3f(lightPosition).add(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f));
        // -Y (Bottom)
        lightViews[3] = new Matrix4f().lookAt(lightPosition, new Vector3f(lightPosition).add(0.0f, -1.0f, 0.0f), new Vector3f(0.0f, 0.0f, -1.0f));
        // +Z (Front/Back - zależy od konwencji)
        lightViews[4] = new Matrix4f().lookAt(lightPosition, new Vector3f(lightPosition).add(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, -1.0f, 0.0f));
        // -Z (Front/Back)
        lightViews[5] = new Matrix4f().lookAt(lightPosition, new Vector3f(lightPosition).add(0.0f, 0.0f, -1.0f), new Vector3f(0.0f, -1.0f, 0.0f));
        return lightViews;
    }

    /**
     * Zwraca macierz projekcji perspektywicznej dla renderowania do cube mapy.
     * FOV wynosi 90 stopni, aby pokryć każdą ścianę sześcianu.
     * @param nearPlane Bliska płaszczyzna odcięcia.
     * @param farPlane Daleka płaszczyzna odcięcia (zasięg światła).
     * @return Macierz projekcji.
     */
    public static Matrix4f getCubeMapProjectionMatrix(float nearPlane, float farPlane) {
        // Aspect ratio jest 1.0, bo renderujemy do kwadratowej ściany cube mapy
        return new Matrix4f().perspective((float)Math.toRadians(90.0f), 1.0f, nearPlane, farPlane);
    }
}