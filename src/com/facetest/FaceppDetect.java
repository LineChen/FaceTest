package com.facetest;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

/**
 * 工具类
 * 
 * @author Administrator
 * 
 */
public class FaceppDetect {

	public interface Callback {
		void success(JSONObject result);

		void error(FaceppParseException exception);
	}

	/**
	 * 发送请求，得到结果
	 * 
	 * @param bm
	 * @param callback
	 */
	public static void detect(final Bitmap bm, final Callback callback) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpRequests requests = new HttpRequests(ConstantValues.KEY,
							ConstantValues.SECRET, true, true);
					Bitmap bmSmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
							bm.getHeight());
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					
					byte[] arrays = stream.toByteArray();
					
					PostParameters params = new PostParameters();
					params.setImg(arrays);
					JSONObject json = requests.detectionDetect(params);
					
					Log.i("--", json.toString());
					
					if(callback != null){
						callback.success(json);
					}
					
				} catch (FaceppParseException e) {
					e.printStackTrace();
					if(callback != null){
						callback.error(e);
					}
				}
			}
		}).start();
	}

}









