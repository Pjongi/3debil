package org.example.audio;

import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder; // Ważny import dla Little Endian
import java.nio.IntBuffer;
// ShortBuffer nie jest już bezpośrednio potrzebny w tej wersji, ale zostawiam
// import java.nio.ShortBuffer;
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

    public void init() throws Exception {
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

    // Metoda pomocnicza do wczytania zasobu jako ByteBuffer
    private ByteBuffer ioResourceToByteBuffer(String resource) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = AudioManager.class.getClassLoader().getResourceAsStream(resource)) {
            if (source == null) {
                throw new IOException("Resource not found in classpath: " + resource);
            }
            byte[] bytes = source.readAllBytes();
            buffer = MemoryUtil.memAlloc(bytes.length); // Alokuj Direct ByteBuffer
            buffer.put(bytes);
            buffer.flip(); // Przygotuj do odczytu
        }
        return buffer;
    }

    /**
     * Ładuje plik dźwiękowy WAV z classpath używając prostego, wbudowanego parsera.
     * Tworzy bufor OpenAL. Obsługuje PCM 8/16 bit, Mono/Stereo.
     *
     * @param resourcePath Ścieżka do pliku WAV w classpath (np. "audio/music.wav").
     * @return ID utworzonego bufora OpenAL lub -1 w przypadku błędu.
     */
    public int loadSound(String resourcePath) {
        String cacheKey = resourcePath;
        if (soundBuffers.containsKey(cacheKey)) {
            return soundBuffers.get(cacheKey);
        }

        int bufferPointer = -1;
        ByteBuffer wavDataBuffer = null; // Bufor na cały plik WAV
        ByteBuffer pcmData = null;      // Slice bufora zawierający tylko dane PCM

        try {
            // 1. Wczytaj cały plik WAV do bezpośredniego bufora
            wavDataBuffer = ioResourceToByteBuffer(resourcePath);
            wavDataBuffer.order(ByteOrder.LITTLE_ENDIAN); // Format WAV używa Little Endian

            // DEBUGOWE WYPISANIE PIERWSZYCH BAJTÓW:
            if (wavDataBuffer.remaining() >= 4) {
                System.out.printf("DEBUG: First 4 bytes of %s: %02X %02X %02X %02X (%c%c%c%c)%n",
                        resourcePath,
                        wavDataBuffer.get(0), wavDataBuffer.get(1),
                        wavDataBuffer.get(2), wavDataBuffer.get(3),
                        (char)wavDataBuffer.get(0), (char)wavDataBuffer.get(1),
                        (char)wavDataBuffer.get(2), (char)wavDataBuffer.get(3)
                );
            } else {
                System.out.println("DEBUG: File " + resourcePath + " has less than 4 bytes.");
            }

            // 2. Sprawdź nagłówek RIFF
            if (wavDataBuffer.remaining() < 12) throw new IOException("Invalid WAV file: Too short for RIFF header.");
            wavDataBuffer.position(0); // Upewnij się, że zaczynamy od początku

            // Sprawdź 'RIFF' bajt po bajcie
            byte b1 = wavDataBuffer.get(); byte b2 = wavDataBuffer.get();
            byte b3 = wavDataBuffer.get(); byte b4 = wavDataBuffer.get();
            if (b1 != 'R' || b2 != 'I' || b3 != 'F' || b4 != 'F') {
                System.err.printf("ERROR: Expected RIFF marker, but got bytes: %02X %02X %02X %02X (%c%c%c%c)%n",
                        b1, b2, b3, b4, (char)b1, (char)b2, (char)b3, (char)b4);
                throw new IOException("Invalid WAV file: Missing 'RIFF' marker (byte check failed).");
            }
            System.out.println("DEBUG: RIFF marker confirmed via byte check.");

            // Odczytaj chunkSize
            int chunkSize = wavDataBuffer.getInt();
            System.out.println("DEBUG: Read chunkSize = " + chunkSize + " (0x" + Integer.toHexString(chunkSize) + ")");
            System.out.println("DEBUG: Buffer position after reading chunkSize: " + wavDataBuffer.position());

            // Loguj następne 4 bajty PRZED próbą odczytu WAVE
            if (wavDataBuffer.remaining() >= 4) {
                System.out.printf("DEBUG: Next 4 bytes (should be WAVE): %02X %02X %02X %02X (%c%c%c%c)%n",
                        wavDataBuffer.get(wavDataBuffer.position() + 0),
                        wavDataBuffer.get(wavDataBuffer.position() + 1),
                        wavDataBuffer.get(wavDataBuffer.position() + 2),
                        wavDataBuffer.get(wavDataBuffer.position() + 3),
                        (char)wavDataBuffer.get(wavDataBuffer.position() + 0),
                        (char)wavDataBuffer.get(wavDataBuffer.position() + 1),
                        (char)wavDataBuffer.get(wavDataBuffer.position() + 2),
                        (char)wavDataBuffer.get(wavDataBuffer.position() + 3)
                );
            } else {
                System.out.println("DEBUG: Less than 4 bytes remaining before WAVE check.");
            }

            // Odczytaj WAVE marker
            int waveMarker = wavDataBuffer.getInt();
            System.out.println("DEBUG: Read marker for WAVE = 0x" + Integer.toHexString(waveMarker));

            // ******** POPRAWIONA STAŁA PORÓWNANIA DLA WAVE: ********
            if (waveMarker != 0x45564157) { // 'WAVE' - Little Endian (E=45, V=56, A=41, W=57)
                System.err.println("ERROR: Expected WAVE marker (Little Endian 0x45564157), but got 0x" + Integer.toHexString(waveMarker));
                throw new IOException("Invalid WAV file: Missing 'WAVE' marker.");
            }
            // ******************************************************
            System.out.println("DEBUG: WAVE marker confirmed.");


            // 3. Znajdź chunk "fmt " i "data"
            int format = -1;
            int channels = -1;
            int sampleRate = -1;
            int bitsPerSample = -1;
            int dataSize = -1;
            int dataOffset = -1;

            boolean fmtFound = false;
            boolean dataFound = false;

            while (wavDataBuffer.remaining() >= 8 && (!fmtFound || !dataFound)) {
                int chunkId = wavDataBuffer.getInt();
                int subChunkSize = wavDataBuffer.getInt();

                if (chunkId == 0x20746d66) { // "fmt " - POPRAWNA STAŁA (LITTLE ENDIAN)
                    if (subChunkSize < 16) throw new IOException("Invalid 'fmt ' chunk size.");
                    int fmtStartPos = wavDataBuffer.position();

                    int audioFormat = wavDataBuffer.getShort();
                    if (audioFormat != 1)
                        throw new UnsupportedOperationException("Unsupported WAV format: Only PCM (1) is supported. Found: " + audioFormat);
                    channels = wavDataBuffer.getShort();
                    sampleRate = wavDataBuffer.getInt();
                    int byteRate = wavDataBuffer.getInt();
                    int blockAlign = wavDataBuffer.getShort();
                    bitsPerSample = wavDataBuffer.getShort();

                    wavDataBuffer.position(fmtStartPos + subChunkSize); // Przesuń za chunk fmt
                    fmtFound = true;
                    System.out.println("Found 'fmt ': Channels=" + channels + ", SampleRate=" + sampleRate + ", BitsPerSample=" + bitsPerSample);

                } else if (chunkId == 0x61746164) { // "data" - POPRAWNA STAŁA (LITTLE ENDIAN)
                    dataSize = subChunkSize;
                    dataOffset = wavDataBuffer.position();
                    dataFound = true;
                    System.out.println("Found 'data': Size=" + dataSize + ", Offset=" + dataOffset);
                    // Przesuń pozycję za dane, aby kontynuować szukanie
                    if (wavDataBuffer.remaining() < dataSize) {
                        System.err.println("Warning: Not enough data remaining for 'data' chunk size " + dataSize);
                        dataSize = wavDataBuffer.remaining();
                    }
                    wavDataBuffer.position(dataOffset + dataSize);

                } else {
                    // Pomiń nieznany chunk
                    System.out.println("Skipping unknown chunk: ID=" + Integer.toHexString(chunkId) + ", Size=" + subChunkSize);
                    if (wavDataBuffer.remaining() < subChunkSize) {
                        System.err.println("Warning: Reached end of file while skipping chunk " + Integer.toHexString(chunkId));
                        break;
                    }
                    wavDataBuffer.position(wavDataBuffer.position() + subChunkSize);
                }
            }

            // 4. Sprawdź, czy znaleziono potrzebne chunki
            if (!fmtFound) throw new IOException("Invalid WAV file: 'fmt ' chunk not found.");
            if (!dataFound) throw new IOException("Invalid WAV file: 'data' chunk not found.");
            if (dataOffset == -1 || dataSize <= 0) throw new IOException("Invalid 'data' chunk info.");

            // 5. Określ format OpenAL
            if (channels == 1) {
                if (bitsPerSample == 8) format = AL_FORMAT_MONO8;
                else if (bitsPerSample == 16) format = AL_FORMAT_MONO16;
                else throw new UnsupportedOperationException("Unsupported bits per sample for mono: " + bitsPerSample);
            } else if (channels == 2) {
                if (bitsPerSample == 8) format = AL_FORMAT_STEREO8;
                else if (bitsPerSample == 16) format = AL_FORMAT_STEREO16;
                else throw new UnsupportedOperationException("Unsupported number of channels: " + channels);
            } else {
                throw new UnsupportedOperationException("Unsupported number of channels: " + channels);
            }

            // 6. Uzyskaj slice bufora z danymi PCM
            wavDataBuffer.position(dataOffset);
            wavDataBuffer.limit(dataOffset + dataSize);
            pcmData = wavDataBuffer.slice();
            System.out.println("PCM Data Slice created: position=" + pcmData.position() + ", limit=" + pcmData.limit() + ", capacity=" + pcmData.capacity());

            // 7. Utwórz bufor OpenAL i załaduj dane
            bufferPointer = alGenBuffers();
            int alError = alGetError();
            if (alError != AL_NO_ERROR) throw new RuntimeException("AL Error (genBuffers): " + alError);

            alBufferData(bufferPointer, format, pcmData, sampleRate);
            alError = alGetError();
            if (alError != AL_NO_ERROR) {
                alDeleteBuffers(bufferPointer);
                throw new RuntimeException("AL Error (bufferData): " + alError);
            }

            // 8. Dodaj do mapy i zwróć ID
            soundBuffers.put(cacheKey, bufferPointer);
            System.out.println("Loaded sound (WAV via Parser): " + resourcePath + " (Buffer ID: " + bufferPointer + ")");
            return bufferPointer;

        } catch (Exception e) {
            System.err.println("Error loading sound " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
            if (bufferPointer != -1 && alIsBuffer(bufferPointer)) {
                alDeleteBuffers(bufferPointer);
            }
            return -1; // Zwróć -1 w przypadku błędu
        } finally {
            // 9. Zwolnij pamięć głównego bufora WAV
            if (wavDataBuffer != null) {
                MemoryUtil.memFree(wavDataBuffer);
            }
        }
    }


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

    public void cleanup() {
        System.out.println("Cleaning up LWJGL AudioManager...");
        for (int sourceId : sources) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
        }
        sources.clear();
        System.out.println("AudioManager: Sources deleted.");
        for (int bufferId : soundBuffers.values()) {
            alDeleteBuffers(bufferId);
        }
        soundBuffers.clear();
        System.out.println("AudioManager: Buffers deleted.");

        if (context != NULL) {
            if (!alcMakeContextCurrent(NULL)) System.err.println("AudioManager: Failed to detach context.");
            alcDestroyContext(context);
            context = NULL;
        }
        System.out.println("AudioManager: Context destroyed.");

        if (device != NULL) {
            if (!alcCloseDevice(device)) {
                System.err.println("AudioManager: Failed to close OpenAL device.");
            }
            device = NULL;
        }
        System.out.println("AudioManager cleanup finished.");
    }
}