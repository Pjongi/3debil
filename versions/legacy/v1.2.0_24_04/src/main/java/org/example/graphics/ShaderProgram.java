package org.example.graphics; // Upewnij się, że pakiet jest poprawny!

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// --- WAŻNE IMPORTY STATYCZNE ---
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.GL_FALSE; // Potrzebne dla glUniformMatrix4fv

public class ShaderProgram {

    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private final Map<String, Integer> uniforms;
    private boolean linked = false; // Pole do śledzenia stanu linkowania

    public ShaderProgram() throws Exception {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create Shader program");
        }
        uniforms = new HashMap<>();
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() throws Exception {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            linked = false; // Linkowanie nie powiodło się
            // Detach shaders even if linking failed to avoid resource leaks if cleanup isn't called
            detachShaders();
            throw new Exception("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        } else {
            linked = true; // Linkowanie powiodło się
        }

        // Walidacja jest opcjonalna, ale dobra do debugowania
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        // Odłącz shadery po udanym linkowaniu (dobra praktyka)
        detachShaders();
    }

    // Metoda pomocnicza do odłączania shaderów
    private void detachShaders() {
        if (programId != 0) { // Check if program exists
            if (vertexShaderId != 0) {
                glDetachShader(programId, vertexShaderId);
                glDeleteShader(vertexShaderId); // Można też usunąć shader po odłączeniu
                vertexShaderId = 0;
            }
            if (fragmentShaderId != 0) {
                glDetachShader(programId, fragmentShaderId);
                glDeleteShader(fragmentShaderId); // Można też usunąć shader po odłączeniu
                fragmentShaderId = 0;
            }
        }
    }


    public void createUniform(String uniformName) throws Exception {
        // Pobierz lokalizację uniformu *po* zlinkowaniu programu
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            // To niekoniecznie błąd, uniform mógł zostać zoptymalizowany przez sterownik jeśli nie jest używany
            System.err.println("Warning: Could not find uniform: '" + uniformName + "' (or it might be unused/optimized out)");
            // Można rzucić wyjątek, jeśli uniform jest absolutnie wymagany:
            // throw new Exception("Could not find uniform: " + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }

    // --- Metody setUniform ---
    // Sprawdzaj czy uniform istnieje przed ustawieniem

    private int getUniformLocation(String uniformName) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            // Można rzucić wyjątek lub zalogować błąd, jeśli próbuje się ustawić nieistniejący uniform
            System.err.println("Error: Uniform '" + uniformName + "' does not exist or was not created.");
            return -1; // Zwróć -1 lub rzuć wyjątek
        }
        return location;
    }


    public void setUniform(String uniformName, Matrix4f value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                value.get(fb);
                glUniformMatrix4fv(location, false, fb);
            }
        }
    }

    public void setUniform(String uniformName, Vector3f value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            glUniform3f(location, value.x, value.y, value.z);
        }
    }

    public void setUniform(String uniformName, float value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    public void setUniform(String uniformName, int value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }

    // --- Kontrola programu ---

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        // Odłączanie i usuwanie shaderów jest teraz w detachShaders()
        // detachShaders(); // Powinno być już wywołane po link(), ale dla pewności można
        if (programId != 0) {
            glDeleteProgram(programId);
        }
        linked = false;
    }

    // Metoda sprawdzająca stan linkowania
    public boolean isLinked() {
        return linked;
    }

    // --- Metoda statyczna do ładowania kodu shadera z pliku ---
    public static String loadShaderSource(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}