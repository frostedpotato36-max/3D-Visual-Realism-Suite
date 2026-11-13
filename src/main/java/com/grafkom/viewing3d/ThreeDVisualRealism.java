package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import java.nio.*;
import java.nio.file.*;
import java.io.IOException;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** Interactive OpenGL 3.3 demo for the Computer Graphics week-11 assignment. */
public class ThreeDVisualRealism {
    private long window;
    private int width=1100,height=720;
    private Shader sceneShader, depthShader;
    private Mesh sphere, floor;
    private int texture, shadowFbo, shadowTexture;
    private boolean blinn=true, smooth=true, depth=true, culling=false, useTexture=true, shadows=true, linear=true;
    private float yaw=0.35f,pitch=0.18f,cameraDistance=6.0f;
    private boolean dragging=false; private double lastX,lastY;
    private final Matrix4f projection=new Matrix4f();
    private final Vector3f lightPos=new Vector3f(-3.0f,5.0f,2.0f);
    private static final int SHADOW_SIZE=2048;

    // Variabel tambahan untuk menghitung FPS
    private int currentFps = 0;

    public static void main(String[] args){new ThreeDVisualRealism().run();}
    private void run(){
        initWindow(); initResources(); loop(); cleanup();
    }
    private void initWindow(){
        if(!glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3); glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE); glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE);
        window=glfwCreateWindow(width,height,"3D Visual Realism Suite | Grafika Komputer Minggu 11",0,0);
        if(window==0) throw new RuntimeException("Tidak dapat membuat window OpenGL 3.3");
        glfwMakeContextCurrent(window); 
        glfwSwapInterval(1); // Ubah ke 0 jika ingin V-Sync OFF (FPS tidak mentok 60)
        GL.createCapabilities();
        glfwSetFramebufferSizeCallback(window,(w,nw,nh)->{width=Math.max(1,nw);height=Math.max(1,nh);glViewport(0,0,width,height);});
        glfwSetKeyCallback(window,(w,key,scancode,action,mods)->{
            if(action==GLFW_PRESS){
                switch(key){
                    case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window,true); 
                    case GLFW_KEY_B -> blinn=!blinn;
                    case GLFW_KEY_F -> smooth=!smooth; 
                    case GLFW_KEY_Z -> depth=!depth; 
                    case GLFW_KEY_C -> culling=!culling;
                    case GLFW_KEY_T -> useTexture=!useTexture; 
                    case GLFW_KEY_M -> linear=!linear; 
                    case GLFW_KEY_H -> shadows=!shadows;
                }
                updateTitle(); // Langsung update title saat tombol ditekan
            }
        });
        glfwSetMouseButtonCallback(window,(w,button,action,mods)->{if(button==GLFW_MOUSE_BUTTON_LEFT) dragging=action==GLFW_PRESS;});
        glfwSetCursorPosCallback(window,(w,x,y)->{if(dragging){yaw+=(float)(x-lastX)*0.006f;pitch+=(float)(y-lastY)*0.006f;pitch=Math.max(-1.45f,Math.min(1.45f,pitch));}lastX=x;lastY=y;});
        glfwSetScrollCallback(window,(w,x,y)->cameraDistance=Math.max(2.5f,Math.min(12f,cameraDistance-(float)y*0.35f)));
        glEnable(GL_DEPTH_TEST); glClearColor(0.075f,0.085f,0.11f,1f);
    }
    private void initResources(){
        sceneShader=new Shader("/shaders/realism.vert","/shaders/realism.frag");
        depthShader=new Shader("/shaders/depth.vert","/shaders/depth.frag");
        sphere=Mesh.sphere(40,28); floor=Mesh.plane(); texture=createCheckerTexture(); createShadowMap();
    }
    private void loop(){
        double lastTime = glfwGetTime();
        int frameCount = 0;

        while(!glfwWindowShouldClose(window)){
            // Hitung FPS setiap 1 detik
            double currentTime = glfwGetTime();
            frameCount++;
            if (currentTime - lastTime >= 1.0) {
                currentFps = frameCount;
                frameCount = 0;
                lastTime = currentTime;
                updateTitle();
            }

            glEnable(GL_DEPTH_TEST); if(depth)glEnable(GL_DEPTH_TEST);else glDisable(GL_DEPTH_TEST);
            if(culling){glEnable(GL_CULL_FACE);glCullFace(GL_BACK);glFrontFace(GL_CCW);}else glDisable(GL_CULL_FACE);
            Vector3f camera=new Vector3f((float)(cameraDistance*Math.cos(pitch)*Math.sin(yaw)),(float)(cameraDistance*Math.sin(pitch)),(float)(cameraDistance*Math.cos(pitch)*Math.cos(yaw)));
            Matrix4f view=new Matrix4f().lookAt(camera,new Vector3f(0,0.35f,0),new Vector3f(0,1,0));
            projection.identity().perspective((float)Math.toRadians(45),(float)width/height,0.1f,100f);
            Matrix4f lightView=new Matrix4f().lookAt(lightPos,new Vector3f(0,0,0),new Vector3f(0,1,0));
            Matrix4f lightProj=new Matrix4f().ortho(-7,7,-7,7,0.1f,20f); Matrix4f lightSpace=new Matrix4f(lightProj).mul(lightView);
            if(shadows){glViewport(0,0,SHADOW_SIZE,SHADOW_SIZE);glBindFramebuffer(GL_FRAMEBUFFER,shadowFbo);glClear(GL_DEPTH_BUFFER_BIT);depthShader.use();depthShader.mat4("lightSpaceMatrix",lightSpace);drawSceneDepth();glBindFramebuffer(GL_FRAMEBUFFER,0);}
            glViewport(0,0,width,height);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
            sceneShader.use();sceneShader.mat4("view",view);sceneShader.mat4("projection",projection);sceneShader.mat4("lightSpaceMatrix",lightSpace);
            sceneShader.vec3("lightPos",lightPos);sceneShader.vec3("viewPos",camera);sceneShader.vec3("objectColor",new Vector3f(0.82f,0.48f,0.22f));
            sceneShader.integer("useBlinnPhong",blinn?1:0);sceneShader.integer("smoothShading",smooth?1:0);sceneShader.integer("useTexture",useTexture?1:0);sceneShader.integer("useShadows",shadows?1:0);
            sceneShader.integer("diffuseTexture",0);sceneShader.integer("shadowMap",1);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,shadowTexture);
            Matrix4f model=new Matrix4f().translate(0,0.05f,0).rotateY((float)glfwGetTime()*0.35f).scale(1.15f);
            sceneShader.mat4("model",model);sphere.draw();
            sceneShader.vec3("objectColor",new Vector3f(0.48f,0.52f,0.58f));sceneShader.integer("useTexture",0);
            sceneShader.mat4("model",new Matrix4f().translate(0,-1.18f,0).scale(5,1,5));floor.draw();
            glfwSwapBuffers(window);glfwPollEvents();
            glBindTexture(GL_TEXTURE_2D,texture);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,linear?GL_LINEAR_MIPMAP_LINEAR:GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,linear?GL_LINEAR:GL_NEAREST);
        }
    }

    // Method terpisah untuk memperbarui teks pada Window Title
    private void updateTitle() {
        glfwSetWindowTitle(window, String.format(
            "3D Visual Realism Suite | FPS: %d | B:%s F:%s Z:%b C:%b T:%b M:%s H:%b",
            currentFps,
            (blinn ? "Blinn-Phong" : "Phong"),
            (smooth ? "Smooth" : "Flat"),
            depth,
            culling,
            useTexture,
            (linear ? "Linear" : "Nearest"),
            shadows
        ));
    }

    private void drawSceneDepth(){depthShader.mat4("model",new Matrix4f().translate(0,0.05f,0).scale(1.15f));sphere.draw();depthShader.mat4("model",new Matrix4f().translate(0,-1.18f,0).scale(5,1,5));floor.draw();}
    private int createCheckerTexture(){
        int id=glGenTextures();glBindTexture(GL_TEXTURE_2D,id);ByteBuffer data=BufferUtils.createByteBuffer(4*4*4);
        for(int y=0;y<4;y++)for(int x=0;x<4;x++){boolean a=((x+y)&1)==0;data.put((byte)(a?210:90)).put((byte)(a?130:55)).put((byte)(a?65:30)).put((byte)255);}data.flip();
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,4,4,0,GL_RGBA,GL_UNSIGNED_BYTE,data);glGenerateMipmap(GL_TEXTURE_2D);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_REPEAT);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_REPEAT);return id;
    }
    private void createShadowMap(){shadowFbo=glGenFramebuffers();shadowTexture=glGenTextures();glBindTexture(GL_TEXTURE_2D,shadowTexture);glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT,SHADOW_SIZE,SHADOW_SIZE,0,GL_DEPTH_COMPONENT,GL_FLOAT,(ByteBuffer)null);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_BORDER);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_BORDER);FloatBuffer border=BufferUtils.createFloatBuffer(4).put(new float[]{1,1,1,1});border.flip();glTexParameterfv(GL_TEXTURE_2D,GL_TEXTURE_BORDER_COLOR,border);glBindFramebuffer(GL_FRAMEBUFFER,shadowFbo);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,shadowTexture,0);glDrawBuffer(GL_NONE);glReadBuffer(GL_NONE);if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Shadow framebuffer tidak lengkap");glBindFramebuffer(GL_FRAMEBUFFER,0);}
    private void cleanup(){if(sphere!=null)sphere.cleanup();if(floor!=null)floor.cleanup();if(sceneShader!=null)sceneShader.cleanup();if(depthShader!=null)depthShader.cleanup();glDeleteTextures(texture);glDeleteTextures(shadowTexture);glDeleteFramebuffers(shadowFbo);glfwDestroyWindow(window);glfwTerminate();}

    private static class Shader{
        int id; Shader(String vertex,String fragment){id=glCreateProgram();int v=compile(vertex,GL_VERTEX_SHADER),f=compile(fragment,GL_FRAGMENT_SHADER);glAttachShader(id,v);glAttachShader(id,f);glLinkProgram(id);if(glGetProgrami(id,GL_LINK_STATUS)==GL_FALSE)throw new RuntimeException("Shader link: "+glGetProgramInfoLog(id));glDeleteShader(v);glDeleteShader(f);}
        int compile(String path,int type){String s;try(var in=ThreeDVisualRealism.class.getResourceAsStream(path)){if(in==null)throw new IOException("Missing "+path);s=new String(in.readAllBytes());}catch(IOException e){throw new RuntimeException(e);}int sh=glCreateShader(type);glShaderSource(sh,s);glCompileShader(sh);if(glGetShaderi(sh,GL_COMPILE_STATUS)==GL_FALSE)throw new RuntimeException(path+": "+glGetShaderInfoLog(sh));return sh;}
        void use(){glUseProgram(id);}int loc(String n){return glGetUniformLocation(id,n);}void mat4(String n,Matrix4fc m){try(MemoryStack stack=MemoryStack.stackPush()){glUniformMatrix4fv(loc(n),false,m.get(stack.mallocFloat(16)));}}void vec3(String n,Vector3fc v){glUniform3f(loc(n),v.x(),v.y(),v.z());}void integer(String n,int v){glUniform1i(loc(n),v);}void cleanup(){glDeleteProgram(id);}
    }
    private static class Mesh{
        int vao,vbo,ebo,count;Mesh(float[] vertices,int[] indices){count=indices.length;vao=glGenVertexArrays();vbo=glGenBuffers();ebo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glBufferData(GL_ARRAY_BUFFER,vertices,GL_STATIC_DRAW);glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);int stride=8*Float.BYTES;glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0);glEnableVertexAttribArray(0);glVertexAttribPointer(1,3,GL_FLOAT,false,stride,3L*Float.BYTES);glEnableVertexAttribArray(1);glVertexAttribPointer(2,2,GL_FLOAT,false,stride,6L*Float.BYTES);glEnableVertexAttribArray(2);glBindVertexArray(0);}
        void draw(){glBindVertexArray(vao);glDrawElements(GL_TRIANGLES,count,GL_UNSIGNED_INT,0);glBindVertexArray(0);}void cleanup(){glDeleteVertexArrays(vao);glDeleteBuffers(vbo);glDeleteBuffers(ebo);}
        static Mesh sphere(int sectors,int stacks){java.util.ArrayList<Float> v=new java.util.ArrayList<>();java.util.ArrayList<Integer> ix=new java.util.ArrayList<>();for(int i=0;i<=stacks;i++){double phi=Math.PI*i/stacks;for(int j=0;j<=sectors;j++){double theta=2*Math.PI*j/sectors;float x=(float)(Math.sin(phi)*Math.cos(theta)),y=(float)Math.cos(phi),z=(float)(Math.sin(phi)*Math.sin(theta));v.add(x);v.add(y);v.add(z);v.add(x);v.add(y);v.add(z);v.add((float)j/sectors);v.add((float)i/stacks);}}for(int i=0;i<stacks;i++)for(int j=0;j<sectors;j++){int a=i*(sectors+1)+j,b=a+sectors+1;ix.add(a);ix.add(b);ix.add(a+1);ix.add(b);ix.add(b+1);ix.add(a+1);}return new Mesh(toFloats(v),toInts(ix));}
        static Mesh plane(){float[] v={-1,0,-1,0,1,0,0,0, 1,0,-1,0,1,0,1,0, 1,0,1,0,1,0,1,1, -1,0,1,0,1,0,0,1};return new Mesh(v,new int[]{0,2,1,0,3,2});}
        static float[] toFloats(java.util.List<Float> a){float[] r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}static int[] toInts(java.util.List<Integer>a){int[]r=new int[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i);return r;}
    }
}