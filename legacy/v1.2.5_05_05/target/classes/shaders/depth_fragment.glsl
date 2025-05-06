#version 330 core
// Minimalny fragment shader dla depth pass
// Nic nie musi robić, głębia jest zapisywana automatycznie
void main()
{
    // gl_FragDepth = gl_FragCoord.z; // Zwykle niepotrzebne, jeśli nie modyfikujesz głębi
}