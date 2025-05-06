package org.example.graphics;

import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.example.exception.ResourceLoadException;      // Import nowych wyjątków
import org.example.exception.ResourceNotFoundException; // Import nowych wyjątków
import java.nio.file.NoSuchFileException;                // Import dla specyficznego błędu pliku

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.GL_FALSE;

public class ShaderProgram {

    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private final Map<String, Integer> uniforms;
    private boolean linked = false;

    public ShaderProgram() throws ResourceLoadException {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new ResourceLoadException("Could not create Shader program object");
        }
        uniforms = new HashMap<>();
    }

    public void createVertexShader(String shaderCode) throws ResourceLoadException {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws ResourceLoadException {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) throws ResourceLoadException {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new ResourceLoadException("Error creating shader object. Type: " + shaderType);
        }
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            String infoLog = glGetShaderInfoLog(shaderId, 1024);
            glDeleteShader(shaderId); // Posprzątaj nieudany shader
            throw new ResourceLoadException("Error compiling Shader code (Type: " + shaderType + "): " + infoLog);
        }
        glAttachShader(programId, shaderId);
        return shaderId;
    }

    public void link() throws ResourceLoadException {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String infoLog = glGetProgramInfoLog(programId, 1024);
            linked = false;
            detachShaders(); // Spróbuj posprzątać
            throw new ResourceLoadException("Error linking Shader program: " + infoLog);
        } else {
            linked = true;
        }
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader program: " + glGetProgramInfoLog(programId, 1024));
        }
        detachShaders(); // Odłącz po udanym linkowaniu
    }

    private void detachShaders() {
        if (programId != 0) {
            if (vertexShaderId != 0) {
                glDetachShader(programId, vertexShaderId);
                glDeleteShader(vertexShaderId);
                vertexShaderId = 0;
            }
            if (fragmentShaderId != 0) {
                glDetachShader(programId, fragmentShaderId);
                glDeleteShader(fragmentShaderId);
                fragmentShaderId = 0;
            }
        }
    }

    public void createUniform(String uniformName) throws ResourceLoadException { // Zmieniono z Exception
        if (!linked) {
            // Rzucanie wyjątku, jeśli próbuje się stworzyć uniform przed linkowaniem
            throw new ResourceLoadException("Cannot create uniform '" + uniformName + "': Shader program is not linked yet.");
        }
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            System.err.println("Warning: Could not find uniform location: '" + uniformName + "' (or it might be unused/optimized out)");
            // Nie rzucaj błędu, bo to może być zamierzone (optymalizacja)
        }
        uniforms.put(uniformName, uniformLocation);
    }

    private int getUniformLocation(String uniformName) {
        Integer location = uniforms.get(uniformName);
        // Zwróć -1 lub null, jeśli nie znaleziono, ale nie rzucaj błędu tutaj
        return (location != null) ? location : -1;
    }

    public void setUniform(String uniformName, Matrix4f value) {
        int location = getUniformLocation(uniformName);
        if (location != -1) { // Ustawiaj tylko, jeśli uniform istnieje
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16); value.get(fb);
                glUniformMatrix4fv(location, false, fb);
            }
        } // else { System.err.println("DEBUG: Uniform '" + uniformName + "' not set (location not found)."); } // Opcjonalny debug
    }
    public void setUniform(String uniformName, Vector3f value) { int location = getUniformLocation(uniformName); if (location != -1) glUniform3f(location, value.x, value.y, value.z); }
    public void setUniform(String uniformName, float value) { int location = getUniformLocation(uniformName); if (location != -1) glUniform1f(location, value); }
    public void setUniform(String uniformName, int value) { int location = getUniformLocation(uniformName); if (location != -1) glUniform1i(location, value); }

    public void bind() { glUseProgram(programId); }
    public void unbind() { glUseProgram(0); }

    public void cleanup() {
        unbind();
        // detachShaders(); // Już wywołane po link()
        if (programId != 0) { glDeleteProgram(programId); }
        linked = false;
    }

    public boolean isLinked() { return linked; }

    public static String loadShaderSource(String filePath) throws ResourceNotFoundException, ResourceLoadException {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (NoSuchFileException e) {
            throw new ResourceNotFoundException("Shader file not found: " + filePath, e);
        } catch (IOException e) {
            throw new ResourceLoadException("Failed to read shader file: " + filePath, e);
        }
    }
}