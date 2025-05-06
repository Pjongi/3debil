package org.example.audio;

import org.joml.Vector3f;
import static org.lwjgl.openal.AL10.*;

public class SoundSource {

    private final int sourceId;

    // Konstruktor powinien być publiczny, aby można było tworzyć instancje
    public SoundSource(int sourceId) {
        this.sourceId = sourceId;
    }

    public void setBuffer(int bufferId) {
        stop(); // Zatrzymaj przed zmianą bufora
        alSourcei(sourceId, AL_BUFFER, bufferId);
    }

    public void setPosition(Vector3f position) {
        alSource3f(sourceId, AL_POSITION, position.x, position.y, position.z);
    }

    public void setVelocity(Vector3f velocity) {
        alSource3f(sourceId, AL_VELOCITY, velocity.x, velocity.y, velocity.z);
    }

    public void setGain(float gain) {
        alSourcef(sourceId, AL_GAIN, gain);
    }

    public void setPitch(float pitch) {
        alSourcef(sourceId, AL_PITCH, pitch);
    }

    public void setLooping(boolean loop) {
        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
    }

    public void play() {
        alSourcePlay(sourceId);
    }

    public boolean isPlaying() {
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void pause() {
        alSourcePause(sourceId);
    }

    public void stop() {
        alSourceStop(sourceId);
    }

    // Cleanup dla pojedynczego źródła - wywoływane przez AudioManager.cleanup()
    // Nie powinno być publiczne, jeśli tylko AudioManager zarządza źródłami
    // LUB można zostawić publiczne, jeśli logika gry ma sama sprzątać źródła
    // Zostawmy na razie publiczne, ale pamiętajmy, że AudioManager też je sprząta.
    public void cleanup() {
        stop();
        alDeleteSources(sourceId);
    }

    public int getSourceId() { return sourceId; }
}