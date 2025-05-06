package org.example.util;

import org.example.exception.ResourceLoadException; // Import nowego wyjątku

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.openal.AL10.*;

/**
 * Klasa narzędziowa do parsowania plików WAV (PCM).
 */
public class WavLoader {

    /**
     * Prosta klasa przechowująca dane załadowanego pliku WAV.
     */
    public static class WavInfo {
        public final int format; public final int channels; public final int sampleRate;
        public final int bitsPerSample; public final ByteBuffer pcmData;

        private WavInfo(int format, int channels, int sampleRate, int bitsPerSample, ByteBuffer pcmData) {
            this.format = format; this.channels = channels; this.sampleRate = sampleRate;
            this.bitsPerSample = bitsPerSample; this.pcmData = pcmData;
        }
    }

    /**
     * Parsuje dane pliku WAV z dostarczonego ByteBuffer.
     *
     * @param wavDataBuffer ByteBuffer zawierający dane pliku WAV (flipped).
     * @return Obiekt WavInfo.
     * @throws ResourceLoadException Jeśli plik ma nieprawidłowy format lub jest nieobsługiwany.
     */
    public static WavInfo load(ByteBuffer wavDataBuffer) throws ResourceLoadException {
        if (wavDataBuffer == null || !wavDataBuffer.hasRemaining()) {
            throw new ResourceLoadException("Input ByteBuffer for WAV parsing is null or empty.");
        }

        try {
            wavDataBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // -- Parsowanie nagłówka RIFF --
            if (wavDataBuffer.remaining() < 12) throw new IOException("Invalid WAV file: Too short for RIFF header.");
            wavDataBuffer.position(0);
            byte b1 = wavDataBuffer.get(); byte b2 = wavDataBuffer.get(); byte b3 = wavDataBuffer.get(); byte b4 = wavDataBuffer.get();
            if (b1 != 'R' || b2 != 'I' || b3 != 'F' || b4 != 'F') throw new IOException("Invalid WAV file: Missing 'RIFF' marker.");
            int chunkSize = wavDataBuffer.getInt();
            int waveMarker = wavDataBuffer.getInt();
            if (waveMarker != 0x45564157) throw new IOException("Invalid WAV file: Missing 'WAVE' marker.");

            // -- Wyszukiwanie chunków 'fmt ' i 'data' --
            int format = -1; int channels = -1; int sampleRate = -1; int bitsPerSample = -1;
            int dataSize = -1; int dataOffset = -1; boolean fmtFound = false; boolean dataFound = false;

            while (wavDataBuffer.remaining() >= 8 && (!fmtFound || !dataFound)) {
                int chunkId = wavDataBuffer.getInt(); int subChunkSize = wavDataBuffer.getInt();
                if (chunkId == 0x20746d66) { // "fmt "
                    if (subChunkSize < 16) throw new IOException("Invalid 'fmt ' chunk size.");
                    int fmtStartPos = wavDataBuffer.position(); int audioFormat = wavDataBuffer.getShort();
                    if (audioFormat != 1) throw new UnsupportedOperationException("Unsupported WAV audio format: Only PCM (1) is supported. Found: " + audioFormat);
                    channels = wavDataBuffer.getShort(); sampleRate = wavDataBuffer.getInt(); wavDataBuffer.getInt(); wavDataBuffer.getShort(); bitsPerSample = wavDataBuffer.getShort();
                    wavDataBuffer.position(fmtStartPos + subChunkSize); fmtFound = true;
                } else if (chunkId == 0x61746164) { // "data"
                    dataSize = subChunkSize; dataOffset = wavDataBuffer.position(); dataFound = true;
                    if (wavDataBuffer.remaining() < dataSize) { dataSize = wavDataBuffer.remaining();}
                    wavDataBuffer.position(dataOffset + dataSize);
                } else { // Skip unknown chunk
                    if (wavDataBuffer.remaining() < subChunkSize) { break; }
                    wavDataBuffer.position(wavDataBuffer.position() + subChunkSize);
                }
            }
            if (!fmtFound) throw new IOException("Invalid WAV file: 'fmt ' chunk not found.");
            if (!dataFound) throw new IOException("Invalid WAV file: 'data' chunk not found.");
            if (dataOffset == -1 || dataSize <= 0) throw new IOException("Invalid 'data' chunk info.");

            // -- Określenie formatu OpenAL --
            if (channels == 1) { if (bitsPerSample == 8) format = AL_FORMAT_MONO8; else if (bitsPerSample == 16) format = AL_FORMAT_MONO16; else throw new UnsupportedOperationException("Unsupported bits per sample for mono: " + bitsPerSample);}
            else if (channels == 2) { if (bitsPerSample == 8) format = AL_FORMAT_STEREO8; else if (bitsPerSample == 16) format = AL_FORMAT_STEREO16; else throw new UnsupportedOperationException("Unsupported number of channels: " + channels);}
            else { throw new UnsupportedOperationException("Unsupported number of channels: " + channels);}

            // -- Utworzenie slice'a z danymi PCM --
            wavDataBuffer.position(dataOffset); wavDataBuffer.limit(dataOffset + dataSize);
            ByteBuffer pcmData = wavDataBuffer.slice();

            return new WavInfo(format, channels, sampleRate, bitsPerSample, pcmData);

        } catch (IOException | UnsupportedOperationException | IllegalArgumentException e) { // Złap potencjalne błędy
            throw new ResourceLoadException("Failed to parse WAV data: " + e.getMessage(), e);
        }
    }
}