#version 330 core
layout (location = 0) in vec2 aPos; // Pozycja 2D w Normalized Device Coordinates (NDC)

void main() {
    // Rysujemy bezpośrednio w NDC, Z=0.0 aby był na wierzchu (gdy test głębi wyłączony)
    gl_Position = vec4(aPos.x, aPos.y, 0.0, 1.0);
}