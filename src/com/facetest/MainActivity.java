package com.facetest;

import org.json.JSONArray;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facetest.FaceppDetect.Callback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private static final int PICK_CODE = 0x110;
	private ImageView iv_Photo;
	private Button bt_GetImage;
	private Button bt_Detect;
	private TextView tv_Tip;
	private View fl_Waiting;

	private String mCurrentPhotoStr;

	private Bitmap mPtotoImg;

	private Paint mPaint;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().hide();
		setContentView(R.layout.activity_main);

		initViews();

		initEvents();

		mPaint = new Paint();

	}

	private void initEvents() {
		bt_GetImage.setOnClickListener(this);
		bt_Detect.setOnClickListener(this);
	}

	private void initViews() {
		iv_Photo = (ImageView) findViewById(R.id.iv_Photo);
		bt_GetImage = (Button) findViewById(R.id.bt_GetImage);
		bt_Detect = (Button) findViewById(R.id.bt_Detect);
		tv_Tip = (TextView) findViewById(R.id.tv_Tip);

		fl_Waiting = findViewById(R.id.fl_Waiting);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == PICK_CODE) {
			if (intent != null) {
				Uri uri = intent.getData();
				// 得到图片的路径
				Cursor cursor = getContentResolver().query(uri, null, null,
						null, null);
				cursor.moveToFirst();

				int idx = cursor
						.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				mCurrentPhotoStr = cursor.getString(idx);
				cursor.close();
				Log.i("--", "mCurrentPhotoStr = " + mCurrentPhotoStr);

				resizePhoto();

				iv_Photo.setImageBitmap(mPtotoImg);
				tv_Tip.setText("点击识别==>");
			}
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}

	/**
	 * 压缩图片
	 */
	private void resizePhoto() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// 设置为TRUE后只会获取图片的高和宽
		BitmapFactory.decodeFile(mCurrentPhotoStr, options);

		double ratio = Math.max(options.outWidth * 1.0d / 1024f,
				options.outHeight * 1.0d / 1024f);
		options.inSampleSize = (int) Math.ceil(ratio);
		options.inJustDecodeBounds = false;
		mPtotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr, options);
	}

	private static final int MSG_SUCCESS = 0x111;
	private static final int MSG_ERROR = 0x112;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == MSG_SUCCESS) {
				fl_Waiting.setVisibility(View.GONE);
				JSONObject rs = (JSONObject) msg.obj;
				prepareRsBitmap(rs);// 重新绘制图片，加上消息气泡

				iv_Photo.setImageBitmap(mPtotoImg);

			} else if (msg.what == MSG_ERROR) {
				fl_Waiting.setVisibility(View.GONE);
				String errorMsg = (String) msg.obj;
				if (TextUtils.isEmpty(errorMsg)) {
					tv_Tip.setText("ERROR!");
				} else {
					tv_Tip.setText(errorMsg);
				}
			}
		}

	};

	private void prepareRsBitmap(JSONObject rs) {

		Bitmap bitmap = Bitmap.createBitmap(mPtotoImg.getWidth(),
				mPtotoImg.getHeight(), mPtotoImg.getConfig());
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(mPtotoImg, 0, 0, null);
		try {
			JSONArray faces = rs.getJSONArray("face");

			int faceCount = faces.length();
			tv_Tip.setText("发现:" + faceCount);

			for (int i = 0; i < faceCount; i++) {
				JSONObject face = faces.getJSONObject(i);
				JSONObject posObj = face.getJSONObject("position");

				// 脸部中心点
				float x = (float) posObj.getJSONObject("center").getDouble("x");
				float y = (float) posObj.getJSONObject("center").getDouble("y");

				// 脸部长和宽
				float w = (float) posObj.getDouble("width");
				float h = (float) posObj.getDouble("height");

				x = x / 100 * bitmap.getWidth();
				y = y / 100 * bitmap.getHeight();

				w = w / 100 * bitmap.getWidth();
				h = h / 100 * bitmap.getHeight();

				mPaint.setColor(0xffffffff);
				// 画box，(脸部)
				canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2,
						mPaint);
				canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2,
						mPaint);
				canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2,
						mPaint);
				canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2,
						mPaint);

				// 获得age gender

				int age = face.getJSONObject("attribute").getJSONObject("age")
						.getInt("value");
				String gender = face.getJSONObject("attribute")
						.getJSONObject("gender").getString("value");
				// 绘制气泡

				Bitmap ageBitmap = buildAgeBitmap(age, "male".equals(gender));

				int ageWidth = ageBitmap.getWidth();
				int ageHeight = ageBitmap.getHeight();

				if (bitmap.getWidth() < mPtotoImg.getWidth()
						&& bitmap.getHeight() < mPtotoImg.getHeight()) {
					float ratio = Math.max(
							bitmap.getWidth() * 1.0f / mPtotoImg.getWidth(),
							bitmap.getHeight() * 1.0f / mPtotoImg.getHeight());
					ageBitmap = Bitmap.createScaledBitmap(ageBitmap,
							(int) (ageWidth * ratio),
							(int) (ageHeight * ratio), false);
				}

				// 绘制气泡
				canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y
						- h / 2 - ageBitmap.getHeight() / 2, null);

				mPtotoImg = bitmap;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 得到年龄的bitmap
	 * 
	 * @param age
	 * @param equals
	 * @return
	 */
	private Bitmap buildAgeBitmap(int age, boolean isMale) {
		Bitmap bitmap = null;

		TextView tv = (TextView) findViewById(R.id.tv_age_and_gender);
		tv.setText(age + "");
		if (isMale) {
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.male), null, null, null);
		} else {
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources()
					.getDrawable(R.drawable.female), null, null, null);
		}

		// 从textview获得bitmap
		tv.setDrawingCacheEnabled(true);
		bitmap = Bitmap.createBitmap(tv.getDrawingCache());
		tv.destroyDrawingCache();
		return bitmap;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_GetImage:
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, PICK_CODE);
			break;
		case R.id.bt_Detect:
			fl_Waiting.setVisibility(View.VISIBLE);

			if (mCurrentPhotoStr != null && !mCurrentPhotoStr.equals("")) {
				resizePhoto();
			} else {
				mPtotoImg = BitmapFactory.decodeResource(getResources(),
						R.drawable.t4);
			}

			FaceppDetect.detect(mPtotoImg, new Callback() {

				@Override
				public void success(JSONObject result) {
					Message msg = Message.obtain();
					msg.what = MSG_SUCCESS;
					msg.obj = result;
					handler.sendMessage(msg);
				}

				@Override
				public void error(FaceppParseException exception) {
					Message msg = Message.obtain();
					msg.what = MSG_ERROR;
					msg.obj = exception.getErrorMessage();
					handler.sendMessage(msg);

				}
			});
			break;
		}
	}
}
