package cn.wildfire.chat.kit.third.location.ui.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.Circle;
import com.tencent.mapsdk.raster.model.CircleOptions;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;

import butterknife.BindView;
import cn.wildfire.chat.kit.third.location.ui.base.BaseActivity;
import cn.wildfire.chat.kit.third.location.ui.presenter.MyLocationAtPresenter;
import cn.wildfire.chat.kit.third.location.ui.view.IMyLocationAtView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;

/**
 * @创建者 CSDN_LQR
 * @描述
 */
public class ShowLocationActivity extends BaseActivity<IMyLocationAtView, MyLocationAtPresenter> implements IMyLocationAtView, TencentLocationListener, SensorEventListener {

    private TencentMap mTencentMap;
    private double mLat;
    private double mLong;

    @BindView(R2.id.confirmButton)
    Button mBtnToolbarSend;
    @BindView(R2.id.rlMap)
    RelativeLayout mRlMap;
    @BindView(R2.id.map)
    MapView mMap;
    @BindView(R2.id.ibShowLocation)
    ImageButton mIbShowLocation;
    private Marker myLocation;
    private Circle accuracy;
    private TencentLocationManager mLocationManager;
    private TencentLocationRequest mLocationRequest;

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
        mTencentMap.setZoom(16);
        mLocationManager = TencentLocationManager.getInstance(this);
        mLocationRequest = TencentLocationRequest.create();

        Marker marker = mTencentMap.addMarker(new MarkerOptions()
                .position(new LatLng(mLat, mLong))
                .title(title)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory
                        .defaultMarker())
                .draggable(false));
        marker.showInfoWindow();
    }

    @Override
    public void initListener() {
        mBtnToolbarSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        mIbShowLocation.setOnClickListener(v -> requestLocationUpdate());

    }

    private void requestLocationUpdate() {
        //开启定位
        int error = mLocationManager.requestLocationUpdates(mLocationRequest, ShowLocationActivity.this);
        switch (error) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                break;
        }
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
        if (isFinishing()) {
            return;
        }
        if (i == tencentLocation.ERROR_OK) {
            LatLng latLng = new LatLng(tencentLocation.getLatitude(), tencentLocation.getLongitude());
            if (myLocation == null) {
                myLocation = mTencentMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.arm)).anchor(0.5f, 0.8f));
            }
            if (accuracy == null) {
                accuracy = mTencentMap.addCircle(new CircleOptions().center(latLng).radius(tencentLocation.getAccuracy()).fillColor(0x440000ff).strokeWidth(0f));
            }
            myLocation.setPosition(latLng);
            accuracy.setCenter(latLng);
            accuracy.setRadius(tencentLocation.getAccuracy());
            mTencentMap.animateTo(latLng);
            mTencentMap.setZoom(16);
            //取消定位
            mLocationManager.removeUpdates(this);
        } else {
        }

    }

    @Override
    public void onStatusUpdate(String s, int i, String s1) {
        if (isFinishing()) {
            return;
        }
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
