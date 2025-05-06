package org.example.audio;

import org.joml.Vector3f;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.openal.AL10.*;

public class Listener {

    // Konstruktor domyślny jest wystarczający
    public Listener() {
        // Domyślne wartości ustawiane w AudioManager.init()
    }

    public void setPosition(float x, float y, float z) {
        alListener3f(AL_POSITION, x, y, z);
    }
    public void setPosition(Vector3f position) {
        setPosition(position.x, position.y, position.z);
    }


    public void setVelocity(float x, float y, float z) {
        alListener3f(AL_VELOCITY, x, y, z);
    }
    public void setVelocity(Vector3f velocity) {
        setVelocity(velocity.x, velocity.y, velocity.z);
    }

    /**
     * Ustawia orientację słuchacza.
     * @param at Wektor kierunku "w przód" (kierunek patrzenia).
     * @param up Wektor kierunku "w górę".
     */
    public void setOrientation(Vector3f at, Vector3f up) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer data = stack.mallocFloat(6);
            data.put(at.x).put(at.y).put(at.z);
            data.put(up.x).put(up.y).put(up.z);
            data.flip();
            alListenerfv(AL_ORIENTATION, data);
        }
    }
    // Przeciążona metoda dla wygody
    public void setOrientation(float atX, float atY, float atZ, float upX, float upY, float upZ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer data = stack.mallocFloat(6);
            data.put(atX).put(atY).put(atZ);
            data.put(upX).put(upY).put(upZ);
            data.flip();
            alListenerfv(AL_ORIENTATION, data);
        }
    }
}