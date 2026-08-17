/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.third.location.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.third.location.ui.activity.ShowLocationActivity;

/**
 * 「看一条位置消息」整页：一张定住的地图 + 右下角「回到我的位置」。
 * <p>
 * 逐行搬自 {@link ShowLocationActivity}，那个类现在只是手机端的壳。入口是会话里的位置消息气泡，
 * 会话本身就在右栏，不迁的话点一下就整屏跳出去。
 * <p>
 * 改造前这一页继承的是位置包自带的那套 MVP 脚手架（{@code BaseActivity} / {@code BasePresenter} /
 * {@code IMyLocationAtView}），但它 {@code createPresenter()} 返回 null，{@code getRvPOI()} 返回 null
 * ——整套 MVP 在这一页上是空转的。脚手架随本次改造一并删掉，见
 * {@link MyLocationPageFragment} 的说明。
 * <p>
 * 同时丢掉的还有 {@code SensorEventListener}：{@code registerListener} 从来没被调用过，
 * {@code onSensorChanged} 的方法体也整段是注释。
 */
public class ShowLocationPageFragment extends Fragment implements WfcPage, TencentLocationListener {

    private static final String ARG_LAT = "Lat";
    private static final String ARG_LONG = "Long";
    private static final String ARG_TITLE = "title";

    private MapView mapView;
    private ImageButton showLocationButton;

    private TencentMap tencentMap;
    private TencentLocationManager locationManager;
    private TencentLocationRequest locationRequest;

    private Marker myLocation;
    private Circle accuracy;

    private double lat;
    private double lng;
    private String title;

    public static ShowLocationPageFragment fromIntent(Intent intent) {
        ShowLocationPageFragment fragment = new ShowLocationPageFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, intent.getDoubleExtra(ARG_LAT, 0));
        args.putDouble(ARG_LONG, intent.getDoubleExtra(ARG_LONG, 0));
        args.putString(ARG_TITLE, intent.getStringExtra(ARG_TITLE));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            lat = args.getDouble(ARG_LAT, 0);
            lng = args.getDouble(ARG_LONG, 0);
            title = args.getString(ARG_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.location_activity_show_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id.map);
        showLocationButton = view.findViewById(R.id.ibShowLocation);

        tencentMap = mapView.getMap();
        tencentMap.setCenter(new LatLng(lat, lng));
        tencentMap.setZoom(16);

        locationManager = TencentLocationManager.getInstance(requireContext());
        locationRequest = TencentLocationRequest.create();

        Marker marker = tencentMap.addMarker(new MarkerOptions()
            .position(new LatLng(lat, lng))
            .title(title)
            .anchor(0.5f, 0.5f)
            .icon(BitmapDescriptorFactory.defaultMarker())
            .draggable(false));
        marker.showInfoWindow();

        showLocationButton.setOnClickListener(v -> requestLocationUpdate());
    }

    private void requestLocationUpdate() {
        locationManager.requestLocationUpdates(locationRequest, this);
    }

    // ==================== WfcPage ====================

    /**
     * 标题是位置消息里带的地点名，跟着 intent 走。
     */
    @Nullable
    @Override
    public CharSequence pageTitle() {
        return title;
    }

    // ==================== TencentLocationListener ====================

    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int errorCode, String reason) {
        // 定位是异步回来的，视图可能已经销毁（右栏里本页被弹掉）
        if (getView() == null) {
            return;
        }
        if (errorCode != TencentLocation.ERROR_OK) {
            return;
        }
        LatLng latLng = new LatLng(tencentLocation.getLatitude(), tencentLocation.getLongitude());
        if (myLocation == null) {
            myLocation = tencentMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.arm))
                .anchor(0.5f, 0.8f));
        }
        if (accuracy == null) {
            accuracy = tencentMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(tencentLocation.getAccuracy())
                .fillColor(0x440000ff)
                .strokeWidth(0f));
        }
        myLocation.setPosition(latLng);
        accuracy.setCenter(latLng);
        accuracy.setRadius(tencentLocation.getAccuracy());
        tencentMap.animateTo(latLng);
        tencentMap.setZoom(16);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusUpdate(String name, int status, String desc) {
        // 改造前这里只是把状态码翻译成一个没人读的本地变量，原样保留「什么也不做」
    }

    // ==================== 地图生命周期 ====================

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // 改造前一次都没转发过地图的生命周期——Activity 整个销毁掉也就跟着没了。
        // 右栏里宿主 Activity 长期活着，本页出栈只销毁视图，不放地图就是真泄漏。
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        mapView = null;
        tencentMap = null;
        myLocation = null;
        accuracy = null;
        super.onDestroyView();
    }
}
