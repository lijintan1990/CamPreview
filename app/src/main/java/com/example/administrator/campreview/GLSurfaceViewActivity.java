package com.example.administrator.campreview;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

public class GLSurfaceViewActivity extends Activity {
    private static final String TAG = "GLSurfacePreview";
    int mCameraId;
    Camera mCamera;
    GLSurfaceView mglSurfaceView;

    int vertexShader;
    int fragmentShader;
    int mShaderProgram;

    //不做任何变换下的原始矩阵
    private float[] transformMatrix= {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //setContentView(R.layout.activity_glsurface_view);
        InitGLSurfaceView();

        setContentView(mglSurfaceView);
    }

    private void InitGLSurfaceView() {
        mglSurfaceView = new GLSurfaceView(this);
        //配置Opengl ES版本
        mglSurfaceView.setEGLContextClientVersion(2);
        mglSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                initCamera();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int i, int i1) {
                mDataBuffer = createBuffer(vertexData);
                vertexShader = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER);
                fragmentShader = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
                mShaderProgram = linkProgram(vertexShader, fragmentShader);

                mOESTextureId = CreateOESTextureObject();
                initSurfaceTexture();
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                if (mSurfaceTexture != null){
                    //更新纹理图像
                    mSurfaceTexture.updateTexImage();
                    //获取外部纹理的矩阵，用来确定纹理的采样位置，没有此矩阵可能导致图像翻转等问题
                    mSurfaceTexture.getTransformMatrix(transformMatrix);
                    Draw();
                }
            }
        });
    }

    /**
     * 适配相机旋转
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        //前置
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }
        //后置
        else {
            result = (info.orientation - degrees + 360) % 360;
        }
        //orientationDegree = result;
        camera.setDisplayOrientation(result);
    }

    private void initCamera() {
        mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        mCamera = Camera.open(mCameraId);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.set("orientation", "portrait");
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setPreviewSize(1280, 720);
        mCamera.setDisplayOrientation(90);
        setCameraDisplayOrientation(this, mCameraId, mCamera);
        mCamera.setParameters(parameters);
    }

    /*
    GLES 相关代码
     */
    private static final String VERTEX_SHADER = "" +
            //顶点坐标
            "attribute vec4 aPosition;\n" +
            //纹理矩阵
            "uniform mat4 uTextureMatrix;\n" +
            //自己定义的纹理坐标
            "attribute vec4 aTextureCoordinate;\n" +
            //传给片段着色器的纹理坐标
            "varying vec2 vTextureCoord;\n" +
            "void main()\n" +
            "{\n" +
            //根据自己定义的纹理坐标和纹理矩阵求取传给片段着色器的纹理坐标
            "  vTextureCoord = (uTextureMatrix * aTextureCoordinate).xy;\n" +
            "  gl_Position = aPosition;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER = "" +
            //使用外部纹理必须支持此扩展
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            //外部纹理采样器
            "uniform samplerExternalOES uTextureSampler;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() \n" +
            "{\n" +
            //获取此纹理（预览图像）对应坐标的颜色值
            "  vec4 vCameraColor = texture2D(uTextureSampler, vTextureCoord);\n" +
            //求此颜色的灰度值
            "  float fGrayColor = (0.3*vCameraColor.r + 0.59*vCameraColor.g + 0.11*vCameraColor.b);\n" +
            //将此灰度值作为输出颜色的RGB值，这样就会变成黑白滤镜
            "  gl_FragColor = vec4(fGrayColor, fGrayColor, fGrayColor, 1.0);\n" +
            "}\n";

    //每行前两个值为顶点坐标，后两个为纹理坐标
    private static final float[] vertexData = {
            -1f,  1f,  0f,  1f,
            -1f, -1f,  0f,  0f,
            1f,  1f,  1f,  1f,
            1f, -1f,  1f,  0f
    };
    int aPositionLocation;
    int aTextureCoordLocation;
    int uTextureMatrixLocation;
    int uTextureSamplerLocation;
    FloatBuffer mDataBuffer;

    private SurfaceTexture mSurfaceTexture;
    private int mOESTextureId;

    public FloatBuffer createBuffer(float[] vertexData) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(vertexData, 0, vertexData.length).position(0);
        return buffer;
    }

    void Draw(){
        //获取Shader中定义的变量在program中的位置
        aPositionLocation = glGetAttribLocation(mShaderProgram, "aPosition");
        aTextureCoordLocation = glGetAttribLocation(mShaderProgram, "aTextureCoordinate");
        uTextureMatrixLocation = glGetUniformLocation(mShaderProgram, "uTextureMatrix");
        uTextureSamplerLocation = glGetUniformLocation(mShaderProgram, "uTextureSampler");

        //激活纹理单元0
        glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定外部纹理到纹理单元0
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextureId);
        //将此纹理单元床位片段着色器的uTextureSampler外部纹理采样器
        glUniform1i(uTextureSamplerLocation, 0);

        //将纹理矩阵传给片段着色器
        glUniformMatrix4fv(uTextureMatrixLocation, 1, false, transformMatrix, 0);

        //将顶点和纹理坐标传给顶点着色器
        if (mDataBuffer != null) {
            //顶点坐标从位置0开始读取
            mDataBuffer.position(0);
            //使能顶点属性
            glEnableVertexAttribArray(aPositionLocation);
            //顶点坐标每次读取两个顶点值，之后间隔16（每行4个值 * 4个字节）的字节继续读取两个顶点值
            glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT, false, 16, mDataBuffer);

            //纹理坐标从位置2开始读取
            mDataBuffer.position(2);
            glEnableVertexAttribArray(aTextureCoordLocation);
            //纹理坐标每次读取两个顶点值，之后间隔16（每行4个值 * 4个字节）的字节继续读取两个顶点值
            glVertexAttribPointer(aTextureCoordLocation, 2, GL_FLOAT, false, 16, mDataBuffer);
        }

        //绘制两个三角形（6个顶点）
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    public static int CreateOESTextureObject() {
        int [] tex = new int[1];
        //生成一个纹理
        GLES20.glGenTextures(1, tex, 0);
        //将此纹理绑定到外部纹理上
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        //设置纹理过滤参数
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        //解除纹理绑定
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return tex[0];
    }

    public static void checkGLError(String op){
        int error;
        //错误代码不为0, 就打印错误日志, 并抛出异常
        if( (error = GLES20.glGetError()) != GLES20.GL_NO_ERROR ){
            Log.e(TAG, op + ": glError " + error);
        }
    }

    //加载着色器，GL_VERTEX_SHADER代表生成顶点着色器，GL_FRAGMENT_SHADER代表生成片段着色器
    public int loadShader(int type, String shaderSource) {
        //创建Shader
        int shader = glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Create Shader Failed!" + glGetError());
        }
        //加载Shader代码
        glShaderSource(shader, shaderSource);
        //编译Shader
        glCompileShader(shader);
        return shader;
    }

    //将两个Shader链接至program中
    public int linkProgram(int verShader, int fragShader) {
        //创建program
        int program = glCreateProgram();
        if (program == 0) {
            throw new RuntimeException("Create Program Failed!" + glGetError());
        }
        //附着顶点和片段着色器
        glAttachShader(program, verShader);
        glAttachShader(program, fragShader);
        //链接program
        glLinkProgram(program);
        //告诉OpenGL ES使用此program
        glUseProgram(program);
        return program;
    }

    public boolean initSurfaceTexture() {
        //根据外部纹理ID创建SurfaceTexture
        mSurfaceTexture = new SurfaceTexture(mOESTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(
                new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        //有数据来了，请求opengl进行渲染
                        mglSurfaceView.requestRender();
                    }
                }
        );
        // 将此SurfaceTexture作为相机预览输出
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        mCamera.startPreview();
        return true;
    }
}
