package com.research.gltexture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class DataHelper {
	private static final String TAG = "DataHelper";

	private static final boolean IS_KEEP_FILLED_BUFFER = true;

	private static final int INVALID_INT = -1;
	private static final int BUFFER_SIZE = 2;

	private static final int EMPTY_BUFFER_DONE = 0;
	private static final int FILL_BUFFER_DONE = 1;

	private static final int EMPTYING_BUFFER = 2;
	private static final int FILLING_BUFFER = 3;

	private int mWidth = INVALID_INT;
	private int mHeight = INVALID_INT;
	private int mLength = INVALID_INT;

	private boolean mIsInitialized = false;

	private int mOutputIndex = INVALID_INT;
	private int[] mStatuses = new int[BUFFER_SIZE];

	private Bitmap[] mBitmaps = new Bitmap[BUFFER_SIZE];

	/** RenderScript buffers **/
	private Allocation[] mInputs = new Allocation[BUFFER_SIZE];
	private Allocation[] mOutputs = new Allocation[BUFFER_SIZE];
	private RenderScript mRenderScript = null;
	private ScriptIntrinsicYuvToRGB mScript = null;

	public DataHelper() {
		for (int index = 0; index < BUFFER_SIZE; index++) {
			mInputs[index] = null;
			mBitmaps[index] = null;
			mOutputs[index] = null;

			mStatuses[index] = EMPTY_BUFFER_DONE;
		}
	}

	public void setWidth(int width) {
		mWidth = width;
	}

	public void setHeight(int height) {
		mHeight = height;
	}

	public void setLength(int length) {
		mLength = length;
	}

	public void setRenderScript(RenderScript rs) {
		mRenderScript = rs;
		mScript = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
	}

	public boolean isInitialized() {
		return mIsInitialized;
	}

	public synchronized void init() {
		if (mIsInitialized || INVALID_INT == mWidth || INVALID_INT == mHeight
				|| INVALID_INT == mLength || null == mRenderScript) {
			return;
		}

		Log.v("[" + mWidth + ", " + mHeight + "] " + mLength);

		mIsInitialized = true;

		for (int index = 0; index < BUFFER_SIZE; index++) {
			mInputs[index] = Allocation.createSized(mRenderScript,
					Element.U8(mRenderScript), mLength);
			mBitmaps[index] = Bitmap.createBitmap(mWidth, mHeight,
					Bitmap.Config.ARGB_8888 /* Element.U8_4 */);
			mOutputs[index] = Allocation.createFromBitmap(mRenderScript,
					mBitmaps[index]);
		}
	}

	public void input(byte[] buffer) {
		int index = 0;
		synchronized (mStatuses) {
			for (; index < BUFFER_SIZE; index++) {
				if (EMPTY_BUFFER_DONE == mStatuses[index]) {
					mStatuses[index] = FILLING_BUFFER;
					break;
				}
			}
		}

		if (BUFFER_SIZE == index) {
			Log.e(TAG, "Not empty buffer");

			if (IS_KEEP_FILLED_BUFFER) {
				// Check filled buffer.
				index = 0;
				synchronized (mStatuses) {
					for (; index < BUFFER_SIZE; index++) {
						if (FILL_BUFFER_DONE == mStatuses[index]) {
							mStatuses[index] = FILLING_BUFFER;
							break;
						}
					}
				}
				// XXX No need to check the index. Because there must
				// be a FILL_BUFFER_DOWN one when there is no empty one.
			} else {
				return;
			}
		}

		mInputs[index].copyFrom(buffer);
		mScript.setInput(mInputs[index]);
		mScript.forEach(mOutputs[index]);

		synchronized (mStatuses) {
			mStatuses[index] = FILL_BUFFER_DONE;
		}
	}

	public Bitmap getDummyOutputBuffer(Context ctx) {
		Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(),
				R.drawable.ic_launcher);
		return bm;
	}

	public Bitmap getOutputBuffer() {
		synchronized (mStatuses) {
			for (mOutputIndex = 0; mOutputIndex < BUFFER_SIZE; mOutputIndex++) {
				if (FILL_BUFFER_DONE == mStatuses[mOutputIndex]) {
					mStatuses[mOutputIndex] = EMPTYING_BUFFER;
					break;
				}
			}
		}

		if (INVALID_INT == mOutputIndex || BUFFER_SIZE == mOutputIndex) {
			mOutputIndex = INVALID_INT;
			return null;
		}

		return mBitmaps[mOutputIndex];
	}

	public void returnOutputBuffer() {
		if (INVALID_INT == mOutputIndex) {
			return;
		}

		synchronized (mStatuses) {
			if (IS_KEEP_FILLED_BUFFER) {
				mStatuses[mOutputIndex] = FILL_BUFFER_DONE;
			} else {
				mStatuses[mOutputIndex] = EMPTY_BUFFER_DONE;
			}
		}
	}

}
