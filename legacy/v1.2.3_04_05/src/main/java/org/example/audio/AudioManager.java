package org.example.audio;

import java.io.IOException;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import org.example.exception.ResourceLoadException;      // Import nowych wyjątków
import org.example.exception.ResourceNotFoundException; // Import nowych wyjątków
import org.example.util.ResourceLoader;                 // Import klasy narzędziowej
import org.example.util.WavLoader;                      // Import klasy narzędziowej

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.*;

public class AudioManager {

    private long device = NULL;
    private long context = NULL;
    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private final List<Integer> sources = new ArrayList<>();
    private final Listener listener;

    public AudioManager() {
        listener = new Listener();
    }

    public void init() throws Exception { // Pozostawiamy Exception dla błędów inicjalizacji OpenAL
        this.device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        if (!deviceCaps.OpenALC10) {
            alcCloseDevice(device);
            throw new IllegalStateException("OpenAL 1.0 is not supported by the device.");
        }

        this.context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to create OpenAL context.");
        }
        if (!alcMakeContextCurrent(context)) {
            alcDestroyContext(context);
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to make OpenAL context current.");
        }
        AL.createCapabilities(deviceCaps);

        System.out.println("OpenAL Initialized: Vendor=" + alGetString(AL_VENDOR) + ", Version=" + alGetString(AL_VERSION) + ", Renderer=" + alGetString(AL_RENDERER));
        listener.setPosition(0, 0, 0);
        listener.setVelocity(0, 0, 0);
        listener.setOrientation(0, 0, -1, 0, 1, 0);
    }

    /**
     * Ładuje plik dźwiękowy WAV z classpath, używając WavLoader, i tworzy bufor OpenAL.
     *
     * @param resourcePath Ścieżka do pliku WAV w classpath.
     * @return ID utworzonego bufora OpenAL.
     * @throws ResourceNotFoundException Jeśli plik WAV nie zostanie znaleziony.
     * @throws ResourceLoadException Jeśli wystąpi błąd podczas parsowania WAV lub tworzenia bufora OpenAL.
     */
    public int loadSound(String resourcePath)
            throws ResourceNotFoundException, ResourceLoadException {

        String cacheKey = resourcePath;
        if (soundBuffers.containsKey(cacheKey)) {
            return soundBuffers.get(cacheKey);
        }

        int bufferPointer = -1;
        ByteBuffer wavFileBuffer = null;

        try {
            // 1. Wczytaj plik WAV używając ResourceLoader
            wavFileBuffer = ResourceLoader.ioResourceToByteBuffer(resourcePath); // Może rzucić ResourceNotFoundException lub IOException

            // 2. Sparsuj dane WAV używając WavLoader
            WavLoader.WavInfo wavInfo = WavLoader.load(wavFileBuffer); // Może rzucić ResourceLoadException
            System.out.println("Parsed WAV: " + resourcePath + " - Format: " + wavInfo.format +
                    ", Channels: " + wavInfo.channels + ", SampleRate: " + wavInfo.sampleRate +
                    ", Bits/Sample: " + wavInfo.bitsPerSample + ", PCM Size: " + wavInfo.pcmData.remaining());

            // 3. Utwórz bufor OpenAL
            bufferPointer = alGenBuffers();
            int alError = alGetError();
            if (alError != AL_NO_ERROR) {
                throw new ResourceLoadException("OpenAL error after alGenBuffers: " + alError);
            }

            // 4. Załaduj dane PCM do bufora OpenAL
            alBufferData(bufferPointer, wavInfo.format, wavInfo.pcmData, wavInfo.sampleRate);
            alError = alGetError();
            if (alError != AL_NO_ERROR) {
                System.err.println("AL Error during alBufferData: " + alError + " (Format: " + wavInfo.format +
                        ", SampleRate: " + wavInfo.sampleRate + ", DataSize: " + wavInfo.pcmData.remaining() + ")");
                alDeleteBuffers(bufferPointer); // Spróbuj posprzątać
                throw new ResourceLoadException("OpenAL error during alBufferData: " + alError);
            }

            // 5. Dodaj do mapy i zwróć ID
            soundBuffers.put(cacheKey, bufferPointer);
            System.out.println("Loaded sound (WAV via Loader): " + resourcePath + " (Buffer ID: " + bufferPointer + ")");
            return bufferPointer;

        } catch (IOException e) { // Złap IO z ResourceLoader (inny niż NotFound)
            throw new ResourceLoadException("IO error loading sound file: " + resourcePath, e);
            // ResourceNotFoundException i ResourceLoadException są propagowane
        } finally {
            // 6. ZAWSZE zwalniaj bufor wczytany przez ResourceLoader
            if (wavFileBuffer != null) {
                MemoryUtil.memFree(wavFileBuffer);
                // System.out.println("DEBUG: Freed WAV file buffer for: " + resourcePath); // Opcjonalne
            }
        }
    }

    // Metody createSource, getListener, cleanup bez istotnych zmian
    public SoundSource createSource(boolean loop, boolean relative) {
        int sourcePointer = alGenSources();
        int error = alGetError();
        if (error != AL_NO_ERROR) throw new RuntimeException("OpenAL error generating source: " + error);
        alSourcei(sourcePointer, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        alSourcei(sourcePointer, AL_SOURCE_RELATIVE, relative ? AL_TRUE : AL_FALSE);
        if (!relative) {
            alSourcef(sourcePointer, AL_ROLLOFF_FACTOR, 1.0f);
            alSourcef(sourcePointer, AL_REFERENCE_DISTANCE, 6.0f);
            alSourcef(sourcePointer, AL_MAX_DISTANCE, 50.0f);
        }
        sources.add(sourcePointer);
        return new SoundSource(sourcePointer);
    }

    public Listener getListener() {
        return listener;
    }

    // W AudioManager.java
    public void cleanup() {
        System.out.println("AudioManager Cleanup: Starting...");
        // Sprzątanie źródeł
        System.out.println("AudioManager Cleanup: Deleting sources...");
        for (int sourceId : sources) {
            if (alIsSource(sourceId)) {
                alSourceStop(sourceId);
                alDeleteSources(sourceId);
            }
        }
        sources.clear();
        System.out.println("AudioManager Cleanup: Sources deleted.");

        // Sprzątanie buforów
        System.out.println("AudioManager Cleanup: Deleting buffers...");
        for (int bufferId : soundBuffers.values()) {
            if (alIsBuffer(bufferId)) {
                alDeleteBuffers(bufferId);
            }
        }
        soundBuffers.clear();
        System.out.println("AudioManager Cleanup: Buffers deleted.");

        // Sprzątanie kontekstu
        if (context != NULL) {
            System.out.println("AudioManager Cleanup: Detaching context...");
            if (!alcMakeContextCurrent(NULL)) System.err.println("AudioManager Cleanup Warning: Failed to detach context.");
            System.out.println("AudioManager Cleanup: Destroying context...");
            alcDestroyContext(context);
            context = NULL;
            System.out.println("AudioManager Cleanup: Context destroyed.");
        } else {
            System.out.println("AudioManager Cleanup: Context was already NULL.");
        }

        // Sprzątanie urządzenia
        if (device != NULL) {
            System.out.println("AudioManager Cleanup: Closing device...");
            if (!alcCloseDevice(device)) {
                System.err.println("AudioManager Cleanup Error: Failed to close OpenAL device.");
            }
            device = NULL;
            System.out.println("AudioManager Cleanup: Device closed.");
        } else {
            System.out.println("AudioManager Cleanup: Device was already NULL.");
        }
        System.out.println("AudioManager Cleanup: Finished.");
    }
}