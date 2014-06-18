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
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;

public class RWBitmapManager {
	private static final String TAG = "RWBitmapManager";

	private static final boolean IS_KEEP_FILLED_BUFFER = true;

	private static final int INVALID_INT = -1;
	private static final int BUFFER_SIZE = 2;

	private static final int EMPTY_BUFFER_DONE = 0;
	private static final int FILL_BUFFER_DONE = 1;

	private static final int EMPTYING_BUFFER = 2;
	private static final int FILLING_BUFFER = 3;

	private int mWidth = INVALID_INT;
	private int mHeight = INVALID_INT;

	private boolean mIsInitialized = false;

	private int mInputIndex = INVALID_INT;
	private int mOutputIndex = INVALID_INT;
	private int[] mStatuses = new int[BUFFER_SIZE];

	private Bitmap[] mBitmaps = new Bitmap[BUFFER_SIZE];

	public RWBitmapManager() {
		for (int index = 0; index < BUFFER_SIZE; index++) {
			mBitmaps[index] = null;
			mStatuses[index] = EMPTY_BUFFER_DONE;
		}
	}

	public void setWidth(int width) {
		mWidth = width;
	}

	public void setHeight(int height) {
		mHeight = height;
	}

	public boolean isInitialized() {
		return mIsInitialized;
	}

	public synchronized void init() {
		if (mIsInitialized || INVALID_INT == mWidth || INVALID_INT == mHeight) {
			return;
		}

		Log.v(TAG, "[" + mWidth + ", " + mHeight + "]");

		mIsInitialized = true;

		for (int index = 0; index < BUFFER_SIZE; index++) {
			mBitmaps[index] = Bitmap.createBitmap(mWidth, mHeight,
					Bitmap.Config.ARGB_8888 /* Element.U8_4 */);
		}
	}

	public Bitmap getInputBuffer() {
		synchronized (mStatuses) {
			for (mInputIndex = 0; mInputIndex < BUFFER_SIZE; mInputIndex++) {
				if (EMPTY_BUFFER_DONE == mStatuses[mInputIndex]) {
					mStatuses[mInputIndex] = FILLING_BUFFER;
					break;
				}
			}

			// Check filled buffer.
			if (BUFFER_SIZE == mInputIndex && IS_KEEP_FILLED_BUFFER) {
				// Log.v(TAG, "Checking buffer: " + mStatuses[0] + ", " +
				// mStatuses[1]);
				for (mInputIndex = 0; mInputIndex < BUFFER_SIZE; mInputIndex++) {
					if (FILL_BUFFER_DONE == mStatuses[mInputIndex]) {
						mStatuses[mInputIndex] = FILLING_BUFFER;
						break;
					}
				}
			}
		}

		if (BUFFER_SIZE == mInputIndex) {
			mInputIndex = INVALID_INT;
			// Log.e(TAG, "No empty buffer: " + mStatuses[0] + ", " +
			// mStatuses[1]);
			return null;
		}

		return mBitmaps[mInputIndex];
	}

	public void returnInputBuffer() {
		if (INVALID_INT == mInputIndex) {
			return;
		}

		synchronized (mStatuses) {
			mStatuses[mInputIndex] = FILL_BUFFER_DONE;
		}
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

		if (BUFFER_SIZE == mOutputIndex) {
			mOutputIndex = INVALID_INT;
			// Log.e(TAG, "No filled buffer: " + mStatuses[0] + ", " +
			// mStatuses[1]);
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

	// -----------------------------------------------------------------------
	// Dummy
	// -----------------------------------------------------------------------
	private int mCount = 0;

	private void dummyFillBitmap(Bitmap bm, int color) {
		int width = bm.getWidth();
		int height = bm.getHeight();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				bm.setPixel(x, y, color);
			}
		}
	}

	public void dummyInput(Context ctx) {
		Bitmap bm = getInputBuffer();
		
		if (null != bm) {
			mCount ++;

			int color;
			switch ((mCount / 20) % 3) {
				case 1:
					color = 0xffff0000;
					break;
				case 2:
					color = 0xff00ff00;
					break;
				default:
					color = 0xff0000ff;
					break;
			}

			dummyFillBitmap(bm, color);

			returnInputBuffer();
		}
	}

	public Bitmap getDummyOutputBuffer(Context ctx) {
		int resId;

		mCount++;
		if (0 == (mCount / 20) % 2) {
			resId = R.drawable.ali;
		} else {
			resId = R.drawable.ic_launcher;
		}

		Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), resId);

		return bm;
	}

}
