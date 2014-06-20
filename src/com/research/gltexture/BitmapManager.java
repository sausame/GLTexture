package com.research.gltexture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public class BitmapManager {
	private static final String TAG = "BitmapManager";

	private static final boolean IS_KEEP_FILLED_BUFFER = false;

	private static final int INVALID_INT = -1;
	private static final int BUFFER_SIZE = 2;

	private static final int EMPTY_BUFFER_DONE = 0;
	private static final int FILL_BUFFER_DONE = 1;

	private static final int EMPTYING_BUFFER = 2;
	private static final int FILLING_BUFFER = 3;

	private boolean mIsInitialized = false;

	private int mInputIndex = INVALID_INT;
	private int mOutputIndex = INVALID_INT;
	private int[] mStatuses = new int[BUFFER_SIZE];

	private Bitmap[] mBitmaps = new Bitmap[BUFFER_SIZE];
	private Bitmap mDstBitmap = null;
	private Canvas mDstCanvas = null;

	private Rect mSrcRect = null;
	private Rect mDstRect = null;

	public BitmapManager() {
		for (int index = 0; index < BUFFER_SIZE; index++) {
			mBitmaps[index] = null;
			mStatuses[index] = EMPTY_BUFFER_DONE;
		}
	}

	public void setSrcSize(int width, int height) {
		mSrcRect = new Rect();
		mSrcRect.set(0, 0, width, height);
	}

	public void setDstSize(int width, int height) {
		mDstRect = new Rect();
		mDstRect.set(0, 0, width, height);
	}

	public boolean isInitialized() {
		return mIsInitialized;
	}

	public synchronized void init() {
		if (mIsInitialized || null == mSrcRect || null == mDstRect) {
			return;
		}

		Log.v(TAG, "[" + mSrcRect.width() + ", " + mSrcRect.height() + "] -> ["
				+ mDstRect.width() + ", " + mDstRect.height() + "]");

		mIsInitialized = true;

		for (int index = 0; index < BUFFER_SIZE; index++) {
			mBitmaps[index] = Bitmap
					.createBitmap(mSrcRect.width(), mSrcRect.height(),
							Bitmap.Config.ARGB_8888 /* Element.U8_4 */);
		}

		mDstBitmap = Bitmap.createScaledBitmap(mBitmaps[0], mDstRect.width(),
				mDstRect.height(), false);
//		mDstBitmap = mBitmaps[0].copy(mBitmaps[0].getConfig(), true);
		mDstCanvas = new Canvas(mDstBitmap);
	}

	public Bitmap getInputBuffer() {
		synchronized (mStatuses) {
			if (BUFFER_SIZE == ++mInputIndex) {
				mInputIndex = 0;
			}

			if (EMPTY_BUFFER_DONE == mStatuses[mInputIndex]) {
				mStatuses[mInputIndex] = FILLING_BUFFER;
			} else {
				if (BUFFER_SIZE == ++mInputIndex) {
					mInputIndex = 0;
				}

				if (EMPTY_BUFFER_DONE == mStatuses[mInputIndex]) {
					mStatuses[mInputIndex] = FILLING_BUFFER;
				} else if (IS_KEEP_FILLED_BUFFER) {
					// Check filled buffer.

					// Log.v(TAG, "Checking buffer: " + mStatuses[0] + ", " +
					// mStatuses[1]);
					for (mInputIndex = 0; mInputIndex < BUFFER_SIZE; mInputIndex++) {
						if (FILL_BUFFER_DONE == mStatuses[mInputIndex]) {
							mStatuses[mInputIndex] = FILLING_BUFFER;
							break;
						}
					}

					if (BUFFER_SIZE == mInputIndex) {
						mInputIndex = INVALID_INT;
						// Log.e(TAG, "No empty buffer: " + mStatuses[0] + ", "
						// +
						// mStatuses[1]);
						return null;
					}
				}
			}
		}

		display(true);

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
			if (BUFFER_SIZE == ++ mOutputIndex) {
				mOutputIndex = 0;
			}

			if (FILL_BUFFER_DONE == mStatuses[mOutputIndex]) {
				mStatuses[mOutputIndex] = EMPTYING_BUFFER;
			} else {
				if (BUFFER_SIZE == ++ mOutputIndex) {
					mOutputIndex = 0;
				}

				if (FILL_BUFFER_DONE == mStatuses[mOutputIndex]) {
					mStatuses[mOutputIndex] = EMPTYING_BUFFER;
				} else {
					mOutputIndex = INVALID_INT;
					// Log.e(TAG, "No filled buffer: " + mStatuses[0] + ", " +
					// mStatuses[1]);
					return mDstBitmap;
				}
			}
		}

		display(false);

		mDstCanvas.drawBitmap(mBitmaps[mOutputIndex], mSrcRect, mDstRect, null);
//		mDstCanvas.drawBitmap(mBitmaps[mOutputIndex], 0, 0, null);

		return mDstBitmap;//mBitmaps[mOutputIndex];
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
	public final void display(boolean in) {
		String str0 = (0 == mInputIndex) ? "X" : ((0 == mOutputIndex) ? "O"
				: " ");
		String str1 = (1 == mInputIndex) ? "X" : ((1 == mOutputIndex) ? "O"
				: " ");

		Log.v(TAG, (in ? "-->   " : "<--   ") + str0 + "   " + str1);
	}

	// -----------------------------------------------------------------------
	// Dummy
	// -----------------------------------------------------------------------
	private int mCount = 0;

	private void fillBitmap(Bitmap bm, int color) {
		int width = bm.getWidth();
		int height = bm.getHeight();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (x == y) {
					if (0 == x || width - 1 == x || height - 1 == y) {
						bm.setPixel(x, y, 0xffff0000);
					} else {
						bm.setPixel(x, y, 0xffffffff);
					}
				} else {
					bm.setPixel(x, y, color);
				}
			}
		}
	}

	public void dummyInput(Context ctx) {
		Bitmap bm = getInputBuffer();

		if (null != bm) {
			mCount++;

			int color;
			switch ((mCount / 10) % 3) {
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

			fillBitmap(bm, color);

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
