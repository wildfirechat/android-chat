package cn.wildfire.chat.kit.third.location.ui.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import cn.wildfire.chat.kit.third.location.ui.base.BaseActivity;
import cn.wildfire.chat.kit.third.location.ui.presenter.MyLocationAtPresenter;
import cn.wildfire.chat.kit.third.location.ui.view.IMyLocationAtView;
import cn.wildfirechat.chat.R;

/**
 * @创建者 CSDN_LQR
 * @描述
 */
public class ShowLocationActivity extends BaseActivity<IMyLocationAtView, MyLocationAtPresenter> implements IMyLocationAtView, TencentLocationListener, SensorEventListener {

    private TencentMap mTencentMap;
    private double mLat;
    private double mLong;

    @Bind(R.id.confirmButton)
    Button mBtnToolbarSend;
    @Bind(R.id.rlMap)
    RelativeLayout mRlMap;
    @Bind(R.id.map)
    MapView mMap;
    @Bind(R.id.ibShowLocation)
    ImageButton mIbShowLocation;

    @Override
    public void initView() {
        mBtnToolbarSend.setVisibility(View.VISIBLE);
        mTencentMap = mMap.getMap();
        mBtnToolbarSend.setVisibility(View.INVISIBLE);
    }

    @Override
    public void initData() {
        mLat = getIntent().getDoubleExtra("Lat", 0);
        mLong = getIntent().getDoubleExtra("Long", 0);
        String title = getIntent().getStringExtra("title");
        setToolbarTitle(title);
        mTencentMap.setCenter(new LatLng(mLat, mLong));
    }

    @Override
    public void initListener() {
        mBtnToolbarSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    @Override
    protected MyLocationAtPresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.location_activity_show_location;
    }

    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {

    }

    @Override
    public void onStatusUpdate(String s, int i, String s1) {
        String desc = "";
        switch (i) {
            case STATUS_DENIED:
                desc = "权限被禁止";
                break;
            case STATUS_DISABLED:
                desc = "模块关闭";
                break;
            case STATUS_ENABLED:
                desc = "模块开启";
                break;
            case STATUS_GPS_AVAILABLE:
                desc = "GPS可用，代表GPS开关打开，且搜星定位成功";
                break;
            case STATUS_GPS_UNAVAILABLE:
                desc = "GPS不可用，可能 gps 权限被禁止或无法成功搜星";
                break;
            case STATUS_LOCATION_SWITCH_OFF:
                desc = "位置信息开关关闭，在android M系统中，此时禁止进行wifi扫描";
                break;
            case STATUS_UNKNOWN:
                break;
            default:
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        if (myLocation != null) {
//            myLocation.setRotation(event.values[0]);
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public RecyclerView getRvPOI() {
        return null;
    }
}
