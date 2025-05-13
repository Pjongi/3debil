package org.example.ui;

import org.example.core.Window;
import org.example.util.ResourceLoader;
import org.lwjgl.nuklear.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

// Importy dla typów callbacków GLFW
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

public class NuklearGui {

    private static final int BUFFER_INITIAL_SIZE = 4 * 1024;
    public static final int MAX_VERTEX_BUFFER = 512 * 1024;
    public static final int MAX_ELEMENT_BUFFER = 128 * 1024;

    private final Window window;
    private long glfwWindowHandle;

    private NkContext ctx = NkContext.create();
    private NkUserFont default_font = NkUserFont.create();
    private NkBuffer cmds = NkBuffer.create();
    private NkDrawNullTexture null_texture = NkDrawNullTexture.create();

    private int vbo, vao, ebo;
    private int prog;
    private int vert_shdr;
    private int frag_shdr;
    private int uniform_tex;
    private int uniform_proj;

    private ByteBuffer ttfBuffer;

    private GLFWScrollCallback scrollCallback;
    private GLFWCharCallback charCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;

    private static final NkAllocator ALLOCATOR;
    private static final NkDrawVertexLayoutElement.Buffer VERTEX_LAYOUT;

    static {
        ALLOCATOR = NkAllocator.create()
                .alloc((handle, old, size) -> nmemAllocChecked(size))
                .mfree((handle, ptr) -> nmemFree(ptr));

        VERTEX_LAYOUT = NkDrawVertexLayoutElement.create(4)
                .position(0).attribute(NK_VERTEX_POSITION).format(NK_FORMAT_FLOAT).offset(0)
                .position(1).attribute(NK_VERTEX_TEXCOORD).format(NK_FORMAT_FLOAT).offset(8)
                .position(2).attribute(NK_VERTEX_COLOR).format(NK_FORMAT_R8G8B8A8).offset(16)
                .position(3).attribute(NK_VERTEX_ATTRIBUTE_COUNT).format(NK_FORMAT_COUNT).offset(0)
                .flip();
    }

    public NuklearGui(Window window) {
        this.window = window;
        this.glfwWindowHandle = window.getWindowHandle();
        try {
            this.ttfBuffer = ResourceLoader.ioResourceToByteBuffer("fonts/FiraSans.ttf");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TTF font for Nuklear: " + e.getMessage(), e);
        }
        createCallbacks();
    }

    public void init() {
        System.out.println("  NuklearGui: Initializing...");
        if (!nk_init(ctx, ALLOCATOR, null)) {
            throw new IllegalStateException("Failed to initialize Nuklear context");
        }
        setupFont();
        setupOpenGL();

        ctx.clip()
                .copy((handle, text, len) -> {
                    if (len == 0) return;
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        ByteBuffer str = stack.malloc(len + 1);
                        MemoryUtil.memCopy(text, MemoryUtil.memAddress(str), len);
                        str.put(len, (byte) 0);
                        glfwSetClipboardString(glfwWindowHandle, str);
                    }
                })
                .paste((handle, edit) -> {
                    long text = nglfwGetClipboardString(glfwWindowHandle);
                    if (text != NULL) {
                        nnk_textedit_paste(edit, text, nnk_strlen(text));
                    }
                });
        System.out.println("  NuklearGui: Initialized successfully.");
    }

    private void createCallbacks() {
        scrollCallback = GLFWScrollCallback.create((windowHandle, xoffset, yoffset) -> {
            if (ctx != null && ctx.input() != null) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    NkVec2 scroll = NkVec2.malloc(stack).x((float) xoffset).y((float) yoffset);
                    nk_input_scroll(ctx, scroll);
                }
            }
        });

        charCallback = GLFWCharCallback.create((windowHandle, codepoint) -> {
            if (ctx != null && ctx.input() != null) nk_input_unicode(ctx, codepoint);
        });

        keyCallback = GLFWKeyCallback.create((windowHandle, key, scancode, action, mods) -> {
            if (ctx == null || ctx.input() == null) return;
            boolean press = action == GLFW_PRESS;
            switch (key) {
                case GLFW_KEY_DELETE: nk_input_key(ctx, NK_KEY_DEL, press); break;
                case GLFW_KEY_ENTER: nk_input_key(ctx, NK_KEY_ENTER, press); break;
                case GLFW_KEY_TAB: nk_input_key(ctx, NK_KEY_TAB, press); break;
                case GLFW_KEY_BACKSPACE: nk_input_key(ctx, NK_KEY_BACKSPACE, press); break;
                case GLFW_KEY_UP: nk_input_key(ctx, NK_KEY_UP, press); break;
                case GLFW_KEY_DOWN: nk_input_key(ctx, NK_KEY_DOWN, press); break;
                case GLFW_KEY_HOME:
                    nk_input_key(ctx, NK_KEY_TEXT_START, press);
                    nk_input_key(ctx, NK_KEY_SCROLL_START, press);
                    break;
                case GLFW_KEY_END:
                    nk_input_key(ctx, NK_KEY_TEXT_END, press);
                    nk_input_key(ctx, NK_KEY_SCROLL_END, press);
                    break;
                case GLFW_KEY_PAGE_DOWN: nk_input_key(ctx, NK_KEY_SCROLL_DOWN, press); break;
                case GLFW_KEY_PAGE_UP: nk_input_key(ctx, NK_KEY_SCROLL_UP, press); break;
                case GLFW_KEY_LEFT_SHIFT:
                case GLFW_KEY_RIGHT_SHIFT:
                    nk_input_key(ctx, NK_KEY_SHIFT, press);
                    break;
                case GLFW_KEY_LEFT_CONTROL:
                case GLFW_KEY_RIGHT_CONTROL:
                    if (press) {
                        nk_input_key(ctx, NK_KEY_COPY, glfwGetKey(windowHandle, GLFW_KEY_C) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_PASTE, glfwGetKey(windowHandle, GLFW_KEY_P) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_CUT, glfwGetKey(windowHandle, GLFW_KEY_X) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_TEXT_UNDO, glfwGetKey(windowHandle, GLFW_KEY_Z) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_TEXT_REDO, glfwGetKey(windowHandle, GLFW_KEY_R) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_TEXT_WORD_LEFT, glfwGetKey(windowHandle, GLFW_KEY_LEFT) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_TEXT_WORD_RIGHT, glfwGetKey(windowHandle, GLFW_KEY_RIGHT) == GLFW_PRESS);
                    } else {
                        nk_input_key(ctx, NK_KEY_LEFT, glfwGetKey(windowHandle, GLFW_KEY_LEFT) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_RIGHT, glfwGetKey(windowHandle, GLFW_KEY_RIGHT) == GLFW_PRESS);
                        nk_input_key(ctx, NK_KEY_COPY, false);
                        nk_input_key(ctx, NK_KEY_PASTE, false);
                        nk_input_key(ctx, NK_KEY_CUT, false);
                        nk_input_key(ctx, NK_KEY_SHIFT, false);
                    }
                    break;
            }
        });

        cursorPosCallback = GLFWCursorPosCallback.create((windowHandle, xpos, ypos) -> {
            // System.out.println("NuklearGui cursorPosCallback: x=" + xpos + ", y=" + ypos); // ODDEBUGUJ W RAZIE POTRZEBY
            if (ctx != null && ctx.input() != null) nk_input_motion(ctx, (int) xpos, (int) ypos);
        });

        mouseButtonCallback = GLFWMouseButtonCallback.create((windowHandle, button, action, mods) -> {
            System.out.println("NuklearGui mouseButtonCallback: button=" + button + ", action=" + action + " (PRESS="+(action == GLFW_PRESS)+")");
            if (ctx == null || ctx.input() == null) {
                System.out.println("NuklearGui mouseButtonCallback: ctx or input is null! Cannot process button.");
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                DoubleBuffer cx = stack.mallocDouble(1);
                DoubleBuffer cy = stack.mallocDouble(1);
                glfwGetCursorPos(windowHandle, cx, cy);
                int x = (int) cx.get(0);
                int y = (int) cy.get(0);
                int nkButton;
                switch (button) {
                    case GLFW_MOUSE_BUTTON_RIGHT: nkButton = NK_BUTTON_RIGHT; break;
                    case GLFW_MOUSE_BUTTON_MIDDLE: nkButton = NK_BUTTON_MIDDLE; break;
                    default: nkButton = NK_BUTTON_LEFT;
                }
                nk_input_button(ctx, nkButton, x, y, action == GLFW_PRESS);
                System.out.println("NuklearGui: nk_input_button called with nkButton=" + nkButton + ", x=" + x + ", y=" + y + ", press=" + (action == GLFW_PRESS)); // DODATKOWY LOG
            }
        });
    }

    public void setActiveCallbacks() {
        if (glfwWindowHandle == 0) return;
        glfwSetScrollCallback(glfwWindowHandle, scrollCallback);
        glfwSetCharCallback(glfwWindowHandle, charCallback);
        glfwSetKeyCallback(glfwWindowHandle, keyCallback);
        glfwSetCursorPosCallback(glfwWindowHandle, cursorPosCallback);
        glfwSetMouseButtonCallback(glfwWindowHandle, mouseButtonCallback);
        System.out.println("NuklearGui: GUI input callbacks set.");
    }

    public void clearCallbacks() {
        if (glfwWindowHandle == 0) return;
        glfwSetScrollCallback(glfwWindowHandle, null);
        glfwSetCharCallback(glfwWindowHandle, null);
        glfwSetKeyCallback(glfwWindowHandle, null);
        glfwSetCursorPosCallback(glfwWindowHandle, null);
        glfwSetMouseButtonCallback(glfwWindowHandle, null);
        System.out.println("NuklearGui: GUI input callbacks cleared.");
    }

    private void setupFont() {
        final int FONT_HEIGHT = 18;
        final int BITMAP_W = 1024;
        final int BITMAP_H = 1024;
        int fontTexID = GL11.glGenTextures();
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(95);
        float scale, descent;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (!stbtt_InitFont(fontInfo, ttfBuffer)) {
                throw new IllegalStateException("Failed to initialize font information.");
            }
            scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);
            IntBuffer d = stack.mallocInt(1);
            stbtt_GetFontVMetrics(fontInfo, null, d, null);
            descent = d.get(0) * scale;
            ByteBuffer bitmap = MemoryUtil.memAlloc(BITMAP_W * BITMAP_H);
            STBTTPackContext pc = STBTTPackContext.malloc(stack);
            stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
            stbtt_PackSetOversampling(pc, 2, 2);
            stbtt_PackFontRange(pc, ttfBuffer, 0, FONT_HEIGHT, 32, cdata);
            stbtt_PackEnd(pc);
            ByteBuffer texture = MemoryUtil.memAlloc(BITMAP_W * BITMAP_H * 4);
            for (int i = 0; i < bitmap.capacity(); i++) {
                texture.putInt((bitmap.get(i) << 24) | 0x00FFFFFF);
            }
            texture.flip();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexID);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            MemoryUtil.memFree(texture);
            MemoryUtil.memFree(bitmap);
        }

        default_font
                .width((handle, h, text, len) -> {
                    float text_width = 0;
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        IntBuffer unicode = stack.mallocInt(1);
                        int glyph_len = nnk_utf_decode(text, MemoryUtil.memAddress(unicode), len);
                        int text_len = glyph_len;
                        if (glyph_len == 0) return 0;
                        IntBuffer advance = stack.mallocInt(1);
                        while (text_len <= len && glyph_len != 0) {
                            if (unicode.get(0) == NK_UTF_INVALID) break;
                            stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null);
                            text_width += advance.get(0) * scale;
                            glyph_len = nnk_utf_decode(text + text_len, MemoryUtil.memAddress(unicode), len - text_len);
                            text_len += glyph_len;
                        }
                    }
                    return text_width;
                })
                .height(FONT_HEIGHT)
                .query((handle, font_height, glyph, codepoint, next_codepoint) -> {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        FloatBuffer x = stack.floats(0.0f);
                        FloatBuffer y = stack.floats(0.0f);
                        STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
                        stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false);
                        NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);
                        ufg.width(q.x1() - q.x0());
                        ufg.height(q.y1() - q.y0());
                        ufg.offset().set(q.x0(), q.y0() + (FONT_HEIGHT + descent));
                        IntBuffer advance = stack.mallocInt(1);
                        stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);
                        ufg.xadvance(advance.get(0) * scale);
                        ufg.uv(0).set(q.s0(), q.t0());
                        ufg.uv(1).set(q.s1(), q.t1());
                    }
                })
                .texture(it -> it.id(fontTexID));
        nk_style_set_font(ctx, default_font);
    }

    private void setupOpenGL() {
        String NK_SHADER_VERSION = Platform.get() == Platform.MACOSX ? "#version 150\n" : "#version 330 core\n";
        String vertex_shader =
                NK_SHADER_VERSION +
                        "uniform mat4 ProjMtx;\n" +
                        "in vec2 Position;\n" +
                        "in vec2 TexCoord;\n" +
                        "in vec4 Color;\n" +
                        "out vec2 Frag_UV;\n" +
                        "out vec4 Frag_Color;\n" +
                        "void main() {\n" +
                        "   Frag_UV = TexCoord;\n" +
                        "   Frag_Color = Color;\n" +
                        "   gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" +
                        "}\n";
        String fragment_shader =
                NK_SHADER_VERSION +
                        "uniform sampler2D Texture;\n" +
                        "in vec2 Frag_UV;\n" +
                        "in vec4 Frag_Color;\n" +
                        "out vec4 Out_Color;\n" +
                        "void main(){\n" +
                        "   Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                        "}\n";

        nk_buffer_init(cmds, ALLOCATOR, BUFFER_INITIAL_SIZE);
        prog = GL20.glCreateProgram();
        vert_shdr = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        frag_shdr = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(vert_shdr, vertex_shader);
        GL20.glShaderSource(frag_shdr, fragment_shader);
        GL20.glCompileShader(vert_shdr);
        GL20.glCompileShader(frag_shdr);
        if (GL20.glGetShaderi(vert_shdr, GL20.GL_COMPILE_STATUS) != GL_TRUE) {
            throw new IllegalStateException("Failed to compile Nuklear vertex shader: " + GL20.glGetShaderInfoLog(vert_shdr));
        }
        if (GL20.glGetShaderi(frag_shdr, GL20.GL_COMPILE_STATUS) != GL_TRUE) {
            throw new IllegalStateException("Failed to compile Nuklear fragment shader: " + GL20.glGetShaderInfoLog(frag_shdr));
        }
        GL20.glAttachShader(prog, vert_shdr);
        GL20.glAttachShader(prog, frag_shdr);
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) != GL_TRUE) {
            throw new IllegalStateException("Failed to link Nuklear shader program: " + GL20.glGetProgramInfoLog(prog));
        }

        uniform_tex = GL20.glGetUniformLocation(prog, "Texture");
        uniform_proj = GL20.glGetUniformLocation(prog, "ProjMtx");
        int attrib_pos = GL20.glGetAttribLocation(prog, "Position");
        int attrib_uv = GL20.glGetAttribLocation(prog, "TexCoord");
        int attrib_col = GL20.glGetAttribLocation(prog, "Color");

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        ebo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL20.glEnableVertexAttribArray(attrib_pos);
        GL20.glEnableVertexAttribArray(attrib_uv);
        GL20.glEnableVertexAttribArray(attrib_col);
        GL20.glVertexAttribPointer(attrib_pos, 2, GL_FLOAT, false, 20, 0);
        GL20.glVertexAttribPointer(attrib_uv, 2, GL_FLOAT, false, 20, 8);
        GL20.glVertexAttribPointer(attrib_col, 4, GL_UNSIGNED_BYTE, true, 20, 16);

        int nullTexID = GL11.glGenTextures();
        null_texture.texture().id(nullTexID);
        null_texture.uv().set(0.5f, 0.5f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, nullTexID);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(0xFFFFFFFF));
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    public void beginInput() {
        if (ctx != null && ctx.input() != null) {
            nk_input_begin(ctx);
        }
    }

    public void endInput() {
        if (ctx != null && ctx.input() != null) {
            NkMouse mouse = ctx.input().mouse();

            // +++ DODATKOWE LOGI PRZED nk_input_end +++
            if (mouse.buttons(NK_BUTTON_LEFT).down() && mouse.buttons(NK_BUTTON_LEFT).clicked() == 0) {
                // Sprawdzamy .down() ORAZ .clicked() == 0 aby zobaczyć, czy przycisk jest wciśnięty, ale jeszcze nie "kliknięty"
                System.out.println("NuklearGui.endInput: NK_BUTTON_LEFT is currently DOWN. Pos: " + mouse.pos().x() + "," + mouse.pos().y());
            }
            if (mouse.buttons(NK_BUTTON_LEFT).clicked() != 0) {
                System.out.println("NuklearGui.endInput: NK_BUTTON_LEFT was CLICKED this frame according to Nuklear state. Click count: " + mouse.buttons(NK_BUTTON_LEFT).clicked());
            }
            // +++++++++++++++++++++++++++++++++++++++

            if (mouse.grab()) {
                glfwSetInputMode(glfwWindowHandle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            } else if (mouse.grabbed()) {
                float prevX = mouse.prev().x();
                float prevY = mouse.prev().y();
                glfwSetCursorPos(glfwWindowHandle, prevX, prevY);
                mouse.pos().x(prevX);
                mouse.pos().y(prevY);
            } else if (mouse.ungrab()) {
                glfwSetInputMode(glfwWindowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }
            nk_input_end(ctx); // Finalizuje input dla tej ramki
            // System.out.println("NuklearGui: nk_input_end() called.");
        }
    }

    public void renderGUI(int AA) {
        int width = window.getWidth();
        int height = window.getHeight();
        int display_width = width;
        int display_height = height;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetWindowSize(glfwWindowHandle, w, h);
            width = w.get(0);
            height = h.get(0);
            glfwGetFramebufferSize(glfwWindowHandle, w, h);
            display_width = w.get(0);
            display_height = h.get(0);
        }

        boolean blendEnabled = GL11.glIsEnabled(GL_BLEND);
        boolean cullFaceEnabled = GL11.glIsEnabled(GL_CULL_FACE);
        boolean depthTestEnabled = GL11.glIsEnabled(GL_DEPTH_TEST);
        boolean scissorTestEnabled = GL11.glIsEnabled(GL_SCISSOR_TEST);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL_VIEWPORT, viewport);

        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_SCISSOR_TEST);
        glActiveTexture(GL_TEXTURE0);

        glUseProgram(prog);
        glUniform1i(uniform_tex, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer M = stack.mallocFloat(16);
            M.put(0, 2.0f / width); M.put(5, -2.0f / height);
            M.put(10, -1.0f);       M.put(12, -1.0f);
            M.put(13, 1.0f);        M.put(15, 1.0f);
            M.put(1,0f); M.put(2,0f); M.put(3,0f);
            M.put(4,0f); M.put(6,0f); M.put(7,0f);
            M.put(8,0f); M.put(9,0f); M.put(11,0f);
            M.put(14,0f);
            glUniformMatrix4fv(uniform_proj, false, M);
        }
        glViewport(0, 0, display_width, display_height);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        glBufferData(GL_ARRAY_BUFFER, MAX_VERTEX_BUFFER, GL_STREAM_DRAW);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, MAX_ELEMENT_BUFFER, GL_STREAM_DRAW);

        ByteBuffer vertices = Objects.requireNonNull(glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, MAX_VERTEX_BUFFER, null));
        ByteBuffer elements = Objects.requireNonNull(glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, MAX_ELEMENT_BUFFER, null));
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NkConvertConfig config = NkConvertConfig.calloc(stack)
                    .vertex_layout(VERTEX_LAYOUT)
                    .vertex_size(20)
                    .vertex_alignment(4)
                    .tex_null(null_texture)
                    .circle_segment_count(22)
                    .curve_segment_count(22)
                    .arc_segment_count(22)
                    .global_alpha(1.0f)
                    .shape_AA(AA)
                    .line_AA(AA);
            NkBuffer vbuf = NkBuffer.malloc(stack);
            NkBuffer ebuf = NkBuffer.malloc(stack);
            nk_buffer_init_fixed(vbuf, vertices);
            nk_buffer_init_fixed(ebuf, elements);
            nk_convert(ctx, cmds, vbuf, ebuf, config);
        }
        glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
        glUnmapBuffer(GL_ARRAY_BUFFER);

        float fb_scale_x = (float) display_width / (float) width;
        float fb_scale_y = (float) display_height / (float) height;
        long offset = NULL;
        for (NkDrawCommand cmd = nk__draw_begin(ctx, cmds); cmd != null; cmd = nk__draw_next(cmd, cmds, ctx)) {
            if (cmd.elem_count() == 0) continue;
            glBindTexture(GL_TEXTURE_2D, cmd.texture().id());
            glScissor(
                    (int) (cmd.clip_rect().x() * fb_scale_x),
                    (int) ((height - (int) (cmd.clip_rect().y() + cmd.clip_rect().h())) * fb_scale_y),
                    (int) (cmd.clip_rect().w() * fb_scale_x),
                    (int) (cmd.clip_rect().h() * fb_scale_y)
            );
            glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);
            offset += cmd.elem_count() * 2L;
        }
        nk_clear(ctx);

        glUseProgram(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        if (!blendEnabled) glDisable(GL_BLEND);
        if (!scissorTestEnabled) glDisable(GL_SCISSOR_TEST);
        if (depthTestEnabled) glEnable(GL_DEPTH_TEST);
        if (cullFaceEnabled) glEnable(GL_CULL_FACE);
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    public void cleanup() {
        System.out.println("  NuklearGui: Cleaning up...");

        if (scrollCallback != null) scrollCallback.free();
        if (charCallback != null) charCallback.free();
        if (keyCallback != null) keyCallback.free();
        if (cursorPosCallback != null) cursorPosCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();

        if (default_font != null) {
            if (default_font.query() != null) default_font.query().free();
            if (default_font.width() != null) default_font.width().free();
            if (default_font.texture().id() != 0 && (null_texture == null || default_font.texture().id() != null_texture.texture().id())) {
                GL11.glDeleteTextures(default_font.texture().id());
            }
        }
        if (prog != 0) {
            if (vert_shdr != 0) GL20.glDetachShader(prog, vert_shdr);
            if (frag_shdr != 0) GL20.glDetachShader(prog, frag_shdr);
            if (vert_shdr != 0) GL20.glDeleteShader(vert_shdr);
            if (frag_shdr != 0) GL20.glDeleteShader(frag_shdr);
            GL20.glDeleteProgram(prog);
        }
        if (null_texture != null && null_texture.texture().id() != 0) {
            GL11.glDeleteTextures(null_texture.texture().id());
        }
        if (vbo != 0) GL15.glDeleteBuffers(vbo);
        if (ebo != 0) GL15.glDeleteBuffers(ebo);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (cmds != null) nk_buffer_free(cmds);
        if (ctx != null) nk_free(ctx);

        if (ALLOCATOR != null) {
            if (ALLOCATOR.alloc() != null) Objects.requireNonNull(ALLOCATOR.alloc()).free();
            if (ALLOCATOR.mfree() != null) Objects.requireNonNull(ALLOCATOR.mfree()).free();
        }
        System.out.println("  NuklearGui: Cleanup complete.");
    }

    public NkContext getContext() { return ctx; }
}